package com.ruoyi.system.service.impl;

import java.util.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.ruoyi.system.mapper.AiModelPricingMapper;
import com.ruoyi.system.domain.AiModelPricing;
import com.ruoyi.system.service.IAiModelPricingService;

@Service
public class AiModelPricingServiceImpl implements IAiModelPricingService {
    private static final Logger log = LoggerFactory.getLogger(AiModelPricingServiceImpl.class);

    @Autowired
    private AiModelPricingMapper aiModelPricingMapper;

    @Autowired
    @Qualifier("aiRestTemplate")
    private RestTemplate restTemplate;

    /**
     * 【核心新增】：计算单次调用的费用和积分消耗
     * 对应你 SQL 中的 aigc_tasks 字段
     */
    @Override
    public Map<String, BigDecimal> calculateTaskCost(Long pricingId, Integer inputTokens, Integer outputTokens, Integer seconds, Integer count) {
        AiModelPricing pricing = aiModelPricingMapper.selectAiModelPricingById(pricingId);
        if (pricing == null) throw new ServiceException("模型配置不存在");

        BigDecimal consumedCredits = BigDecimal.ZERO;
        BigDecimal realCostCny = BigDecimal.ZERO;
        String mode = pricing.getBillingMode(); // req, token, second

        if ("token".equals(mode)) {
            // Token计费：总Tokens / 1000 * 1K单价
            int totalTokens = (inputTokens != null ? inputTokens : 0) + (outputTokens != null ? outputTokens : 0);
            BigDecimal tokenUnit = new BigDecimal(totalTokens).divide(new BigDecimal(1000), 10, RoundingMode.HALF_UP);
            consumedCredits = pricing.getPointsPer1kTokens().multiply(tokenUnit);
            realCostCny = pricing.getCostPer1kCny().multiply(tokenUnit);
        } else if ("second".equals(mode)) {
            // 按秒计费：秒数 * 单价
            BigDecimal secs = new BigDecimal(seconds != null ? seconds : 0);
            consumedCredits = pricing.getPointsPerSecond().multiply(secs);
            // 假设成本也是按秒，或者按次折算
            realCostCny = pricing.getCostPerReqCny().multiply(secs);
        } else {
            // 默认按次计费 (req)
            BigDecimal cnt = new BigDecimal(count != null ? count : 1);
            consumedCredits = pricing.getPointsPerReq().multiply(cnt);
            realCostCny = pricing.getCostPerReqCny().multiply(cnt);
        }

        Map<String, BigDecimal> result = new HashMap<>();
        // 积分保留2位，人民币成本保留6位（对应你SQL的decimal 12,6）
        result.put("consumedCredits", consumedCredits.setScale(2, RoundingMode.HALF_UP));
        result.put("realCostCny", realCostCny.setScale(6, RoundingMode.HALF_UP));
        return result;
    }

    /**
     * 【原有功能】：验证模型连接（AI实验室调用）
     */
    @Override
    public String verifyModelConnection(AiModelPricing pricing) {
        try {
            String type = pricing.getType();
            String endpoint = pricing.getApiEndpoint().trim();
            String modelCode = pricing.getModelCode();
            String apiKey = pricing.getAccessKeyId();
            String userPrompt = (pricing.getRemark() != null && !pricing.getRemark().isEmpty())
                    ? pricing.getRemark() : getDefaultPrompt(type);

            if (endpoint.isEmpty()) throw new ServiceException("API地址不能为空");
            if (apiKey == null || apiKey.trim().isEmpty()) throw new ServiceException("API Key不能为空");

            String finalUrl = endpoint;
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", modelCode);

            boolean isAliyun = endpoint.contains("aliyuncs.com");

            if ("video".equals(type)) {
                if (!isAliyun && !endpoint.contains("/tasks")) {
                    finalUrl = buildUrl(endpoint, "contents/generations/tasks");
                }
                if (isAliyun) {
                    Map<String, Object> input = new HashMap<>();
                    input.put("prompt", userPrompt);
                    requestBody.put("input", input);
                } else {
                    List<Map<String, Object>> contentList = new ArrayList<>();
                    Map<String, Object> textContent = new HashMap<>();
                    textContent.put("type", "text");
                    textContent.put("text", userPrompt);
                    contentList.add(textContent);
                    requestBody.put("content", contentList);
                }
            } else if ("image".equals(type)) {
                if (!endpoint.contains("images/generations") && !endpoint.contains("image-synthesis")) {
                    finalUrl = buildUrl(endpoint, isAliyun ? "services/aigc/text2image/image-synthesis" : "images/generations");
                }
                if (isAliyun) {
                    Map<String, Object> input = new HashMap<>();
                    input.put("prompt", userPrompt);
                    requestBody.put("input", input);
                } else {
                    requestBody.put("prompt", userPrompt);
                }
            } else {
                if (!endpoint.contains("chat/completions") && !endpoint.contains("generation")) {
                    finalUrl = buildUrl(endpoint, "chat/completions");
                }
                requestBody.put("messages", createSimpleMessage(userPrompt));
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey.trim());

            if (isAliyun && "video".equals(type)) {
                headers.set("X-DashScope-Async", "enable");
            }

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.exchange(finalUrl, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                if ("video".equals(type)) {
                    String taskId = null;
                    if (isAliyun) {
                        Map<String, Object> output = (Map<String, Object>) body.get("output");
                        if (output != null) taskId = (String) output.get("task_id");
                    } else {
                        taskId = (String) body.get("id");
                    }
                    if (taskId != null) return pollVideoResult(endpoint, apiKey, taskId, isAliyun);
                }
                if (body.containsKey("data")) {
                    List<Map<String, Object>> dataList = (List<Map<String, Object>>) body.get("data");
                    if (dataList != null && !dataList.isEmpty()) return "[图片]: " + dataList.get(0).get("url");
                }
                if (body.containsKey("choices")) {
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
                    Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
                    return (String) msg.get("content");
                } else if (isAliyun && body.containsKey("output")) {
                    Map<String, Object> output = (Map<String, Object>) body.get("output");
                    return (String) output.get("text");
                }
            }
            return "连接成功，未识别结果";
        } catch (Exception e) {
            log.error("AI连接异常: ", e);
            throw new ServiceException("连接异常: " + e.getMessage());
        }
    }

    // 内部辅助方法
    private String pollVideoResult(String endpoint, String apiKey, String taskId, boolean isAliyun) throws InterruptedException {
        String queryUrl = isAliyun ? "https://dashscope.aliyuncs.com/api/v1/tasks/" + taskId : buildUrl(endpoint, taskId);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey.trim());
        for (int i = 1; i <= 30; i++) {
            Thread.sleep(10000);
            try {
                ResponseEntity<Map> resp = restTemplate.exchange(queryUrl, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
                Map<String, Object> resBody = resp.getBody();
                if (resBody == null) continue;
                if (isAliyun) {
                    Map<String, Object> output = (Map<String, Object>) resBody.get("output");
                    String status = (String) output.get("task_status");
                    if ("SUCCEEDED".equalsIgnoreCase(status)) return "[视频]: " + output.get("video_url");
                } else {
                    String status = (String) resBody.get("status");
                    if ("succeeded".equalsIgnoreCase(status)) {
                        Map<String, Object> data = (Map<String, Object>) resBody.get("data");
                        return "[视频]: " + data.get("url");
                    }
                }
            } catch (Exception e) { log.warn("轮询异常: {}", e.getMessage()); }
        }
        return "处理中... ID: " + taskId;
    }

    private String buildUrl(String base, String path) { return base.replaceAll("/+$", "") + "/" + path.replaceAll("^/+", ""); }
    private String getDefaultPrompt(String type) { return "video".equals(type) ? "一只熊猫" : "image".equals(type) ? "风景" : "你好"; }
    private List<Map<String, Object>> createSimpleMessage(String text) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("role", "user");
        msg.put("content", text);
        return Collections.singletonList(msg);
    }

    // 基础 CRUD
    @Override public AiModelPricing selectAiModelPricingById(Long id) { return aiModelPricingMapper.selectAiModelPricingById(id); }

    /**
     * 【重点修改】：查询列表时，Mapper层将执行关联统计SQL
     */
    @Override public List<AiModelPricing> selectAiModelPricingList(AiModelPricing aiModelPricing) {
        return aiModelPricingMapper.selectAiModelPricingList(aiModelPricing);
    }

    @Override public int insertAiModelPricing(AiModelPricing aiModelPricing) { aiModelPricing.setCreateTime(DateUtils.getNowDate()); return aiModelPricingMapper.insertAiModelPricing(aiModelPricing); }
    @Override public int updateAiModelPricing(AiModelPricing aiModelPricing) { return aiModelPricingMapper.updateAiModelPricing(aiModelPricing); }
    @Override public int deleteAiModelPricingByIds(Long[] ids) { return aiModelPricingMapper.deleteAiModelPricingByIds(ids); }
    @Override public int deleteAiModelPricingById(Long id) { return aiModelPricingMapper.deleteAiModelPricingById(id); }
}