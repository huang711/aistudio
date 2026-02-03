package com.ruoyi.system.service.impl;

import java.util.*;
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

            // 根据厂商和类型构建请求体
            boolean isAliyun = endpoint.contains("aliyuncs.com");

            if ("video".equals(type)) {
                // 阿里地址通常是全路径，火山等地址若没拼 /tasks 则补全
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
                // 即使是阿里，如果没写全路径也要补全，或者直接让 finalUrl 等于 endpoint
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
                // 文本模型：修正补全逻辑
                // 只要路径里不包含 chat/completions 或 generation，就补全OpenAI兼容路径
                if (!endpoint.contains("chat/completions") && !endpoint.contains("generation")) {
                    finalUrl = buildUrl(endpoint, "chat/completions");
                }
                requestBody.put("messages", createSimpleMessage(userPrompt));
            }

            // Headers 处理
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey.trim());

            // 阿里生视频加这个Header触发异步
            if (isAliyun && "video".equals(type)) {
                headers.set("X-DashScope-Async", "enable");
            }

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            log.info("AI连接测试 - 最终URL: {}", finalUrl);

            ResponseEntity<Map> response = restTemplate.exchange(finalUrl, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();

                //解析结果
                if ("video".equals(type)) {
                    String taskId = null;
                    if (isAliyun) {
                        Map<String, Object> output = (Map<String, Object>) body.get("output");
                        if (output != null) taskId = (String) output.get("task_id");
                    } else {
                        taskId = (String) body.get("id");
                    }

                    if (taskId != null) {
                        log.info("任务提交成功，ID: {}，开始轮询...", taskId);
                        return pollVideoResult(endpoint, apiKey, taskId, isAliyun);
                    }
                }

                //图片解析
                if (body.containsKey("data")) {
                    List<Map<String, Object>> dataList = (List<Map<String, Object>>) body.get("data");
                    if (dataList != null && !dataList.isEmpty()) {
                        return "[图片]: " + dataList.get(0).get("url");
                    }
                }

                // 文本解析 OpenAI兼容格式/阿里 output
                if (body.containsKey("choices")) {
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
                    Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
                    return (String) msg.get("content");
                } else if (isAliyun && body.containsKey("output")) {
                    Map<String, Object> output = (Map<String, Object>) body.get("output");
                    return (String) output.get("text");
                }
            }
            return "连接成功，但未识别到结果结构";

        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            log.error("AI平台报错 ({}): {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ServiceException("平台返回错误: " + e.getStatusCode());
        } catch (Exception e) {
            log.error("AI连接异常: ", e);
            throw new ServiceException("连接异常: " + e.getMessage());
        }
    }

    private String pollVideoResult(String endpoint, String apiKey, String taskId, boolean isAliyun) throws InterruptedException {
        // 阿里查询地址是独立的，火山通常基于 endpoint
        String queryUrl = isAliyun ?
                "https://dashscope.aliyuncs.com/api/v1/tasks/" + taskId :
                buildUrl(endpoint, taskId);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey.trim());
        HttpEntity<Void> queryEntity = new HttpEntity<>(headers);

        for (int i = 1; i <= 30; i++) {
            Thread.sleep(10000);
            try {
                ResponseEntity<Map> resp = restTemplate.exchange(queryUrl, HttpMethod.GET, queryEntity, Map.class);
                Map<String, Object> resBody = resp.getBody();
                if (resBody == null) continue;

                if (isAliyun) {
                    Map<String, Object> output = (Map<String, Object>) resBody.get("output");
                    String status = (String) output.get("task_status");
                    if ("SUCCEEDED".equalsIgnoreCase(status)) {
                        return "[视频]: " + output.get("video_url");
                    } else if ("FAILED".equalsIgnoreCase(status)) {
                        return "视频生成失败: " + output.get("message");
                    }
                } else {
                    String status = (String) resBody.get("status");
                    if ("succeeded".equalsIgnoreCase(status)) {
                        Map<String, Object> data = (Map<String, Object>) resBody.get("data");
                        return "[视频]: " + data.get("url");
                    }
                }
            } catch (Exception e) {
                log.warn("轮询异常 (继续尝试): {}", e.getMessage());
            }
        }
        return "任务处理中，请稍后刷新。ID: " + taskId;
    }

    private String buildUrl(String base, String path) {
        return base.replaceAll("/+$", "") + "/" + path.replaceAll("^/+", "");
    }

    private String getDefaultPrompt(String type) {
        if ("video".equals(type)) return "一只可爱的熊猫在吃竹子";
        if ("image".equals(type)) return "美丽的风景画";
        return "你好";
    }

    private List<Map<String, Object>> createSimpleMessage(String text) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("role", "user");
        msg.put("content", text);
        return Collections.singletonList(msg);
    }

    @Override public AiModelPricing selectAiModelPricingById(Long id) { return aiModelPricingMapper.selectAiModelPricingById(id); }
    @Override public List<AiModelPricing> selectAiModelPricingList(AiModelPricing aiModelPricing) { return aiModelPricingMapper.selectAiModelPricingList(aiModelPricing); }
    @Override public int insertAiModelPricing(AiModelPricing aiModelPricing) { aiModelPricing.setCreateTime(DateUtils.getNowDate()); return aiModelPricingMapper.insertAiModelPricing(aiModelPricing); }
    @Override public int updateAiModelPricing(AiModelPricing aiModelPricing) { return aiModelPricingMapper.updateAiModelPricing(aiModelPricing); }
    @Override public int deleteAiModelPricingByIds(Long[] ids) { return aiModelPricingMapper.deleteAiModelPricingByIds(ids); }
    @Override public int deleteAiModelPricingById(Long id) { return aiModelPricingMapper.deleteAiModelPricingById(id); }
}