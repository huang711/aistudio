package com.ruoyi.aigc.service.impl;

import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.AiModelPricing;
import com.ruoyi.system.service.IAiModelPricingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.aigc.mapper.AigcTasksMapper;
import com.ruoyi.aigc.domain.AigcTasks;
import com.ruoyi.aigc.service.IAigcTasksService;

/**
 * AI任务Service业务层处理 - 剧情片段卡片化解析版
 */
@Service
public class AigcTasksServiceImpl implements IAigcTasksService
{
    private static final Logger log = LoggerFactory.getLogger(AigcTasksServiceImpl.class);

    @Autowired
    private AigcTasksMapper aigcTasksMapper;

    @Autowired
    private IAiModelPricingService pricingService;

    @Override
    public int insertAigcTasks(AigcTasks aigcTasks)
    {
        // 1. 基础校验
        Long modelId = aigcTasks.getUsedModelId();
        if (modelId == null) throw new ServiceException("请选择 AI 模型");

        AiModelPricing originalPricing = pricingService.selectAiModelPricingById(modelId);
        if (originalPricing == null || originalPricing.getIsActive() == 0) {
            throw new ServiceException("模型不存在或已下架");
        }

        AiModelPricing tempExecConfig = new AiModelPricing();
        BeanUtils.copyProperties(originalPricing, tempExecConfig);

        // 2. 初始化基础数据
        aigcTasks.setModelNameSnapshot(tempExecConfig.getDisplayName());
        aigcTasks.setProviderSnapshot(tempExecConfig.getProvider());
        aigcTasks.setCreateTime(DateUtils.getNowDate());
        aigcTasks.setStatus(1L); // 运行中
        aigcTasks.setConsumedCredits(BigDecimal.ZERO);
        aigcTasks.setRealCostCny(BigDecimal.ZERO);

        // 补全数据库必填字段
        if (aigcTasks.getWorkspaceId() == null) aigcTasks.setWorkspaceId(1L);
        try {
            aigcTasks.setUserId(SecurityUtils.getUserId());
        } catch (Exception e) {
            aigcTasks.setUserId(1L);
        }

        // 3. 【核心改动】Prompt 策略升级：从“解析”转向“创作卡片拆解”
        String finalPrompt = aigcTasks.getInputContent();
        if ("script_parsing".equals(aigcTasks.getTaskType())) {
            finalPrompt = "你是一个电影分镜拆解专家。请将输入的剧本内容深度拆解为“原子化”的剧情片段卡片，以便后续独立进行视频创作。\n\n" +
                    "【执行要求】\n" +
                    "1. 提取资产：总结出所有不重复的人物、地点、道具。\n" +
                    "2. 剧情切分：将长剧本切分为多个短小的 script_flow 片段。每个片段字数控制在 100-300 字之间。不要过长！\n" +
                    "3. 独立性：确保每个片段都有明确的动作描述或对话，可以独立作为一段视频的素材。\n\n" +
                    "【必须遵守的 JSON 格式】\n" +
                    "{\n" +
                    "  \"assets\": { \"characters\": [], \"scenes\": [], \"props\": [] },\n" +
                    "  \"script_flow\": [\n" +
                    "    {\n" +
                    "      \"title\": \"片段简短标题(如：雨夜老宅闯入)\",\n" +
                    "      \"type\": \"SCENE|ACTION|DIALOGUE\",\n" +
                    "      \"content\": \"该片段的详细内容，适合创作编辑\",\n" +
                    "      \"role\": \"该片段涉及的主角\"\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}\n" +
                    "【禁令】严禁输出 ```json 等标签，只返回纯 JSON 对象。剧本内容如下：\n" +
                    aigcTasks.getInputContent();
        }

        try {
            log.info(">>>> 启动剧情片段化任务. 类型: {}", aigcTasks.getTaskType());
            tempExecConfig.setRemark(finalPrompt);

            // 发起请求
            String aiResult = pricingService.verifyModelConnection(tempExecConfig);
            if (aiResult == null || aiResult.trim().isEmpty()) throw new ServiceException("AI 响应为空");

            // 4. 清洗与截取
            String cleanedJson = aiResult.trim().replaceAll("(?i)```json", "").replaceAll("(?i)```", "").trim();
            int start = cleanedJson.indexOf("{");
            int end = cleanedJson.lastIndexOf("}");
            if (start != -1 && end != -1 && end > start) {
                cleanedJson = cleanedJson.substring(start, end + 1);
            } else {
                throw new ServiceException("AI 返回格式无法片段化，请重试");
            }

            aigcTasks.setOutputContent(cleanedJson);
            aigcTasks.setStatus(2L); // 成功

            // 5. 计费逻辑
            try {
                Map<String, BigDecimal> costMap = pricingService.calculateTaskCost(tempExecConfig.getId(), 0, 0, 0, 1);
                if (costMap != null) {
                    aigcTasks.setConsumedCredits(costMap.getOrDefault("consumedCredits", BigDecimal.ZERO));
                    aigcTasks.setRealCostCny(costMap.getOrDefault("realCostCny", BigDecimal.ZERO));
                }
            } catch (Exception ce) {
                log.warn("计费异常，跳过");
            }

        } catch (Exception e) {
            log.error(">>>> 解析异常: ", e);
            aigcTasks.setStatus(3L);
            aigcTasks.setOutputContent("解析失败: " + e.getMessage());
        }

        // 6. 存入数据库
        return aigcTasksMapper.insertAigcTasks(aigcTasks);
    }

    // 其他默认方法保持不变...
    @Override public AigcTasks selectAigcTasksById(Long id) { return aigcTasksMapper.selectAigcTasksById(id); }
    @Override public List<AigcTasks> selectAigcTasksList(AigcTasks aigcTasks) { return aigcTasksMapper.selectAigcTasksList(aigcTasks); }
    @Override public int updateAigcTasks(AigcTasks aigcTasks) { return aigcTasksMapper.updateAigcTasks(aigcTasks); }
    @Override public int deleteAigcTasksByIds(Long[] ids) { return aigcTasksMapper.deleteAigcTasksByIds(ids); }
    @Override public int deleteAigcTasksById(Long id) { return aigcTasksMapper.deleteAigcTasksById(id); }
}