package com.ruoyi.system.service;

import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import com.ruoyi.system.domain.AiModelPricing;

/**
 * 模型配置Service接口
 * * @author ruoyi
 * @date 2026-02-03
 */
public interface IAiModelPricingService
{
    /**
     * 查询模型配置
     * * @param id 模型配置主键
     * @return 模型配置
     */
    public AiModelPricing selectAiModelPricingById(Long id);

    /**
     * 查询模型配置列表（包含统计数据）
     * * @param aiModelPricing 模型配置
     * @return 模型配置集合
     */
    public List<AiModelPricing> selectAiModelPricingList(AiModelPricing aiModelPricing);

    /**
     * 新增模型配置
     * * @param aiModelPricing 模型配置
     * @return 结果
     */
    public int insertAiModelPricing(AiModelPricing aiModelPricing);

    /**
     * 修改模型配置
     * * @param aiModelPricing 模型配置
     * @return 结果
     */
    public int updateAiModelPricing(AiModelPricing aiModelPricing);

    /**
     * 批量删除模型配置
     * * @param ids 需要删除的模型配置主键集合
     * @return 结果
     */
    public int deleteAiModelPricingByIds(Long[] ids);

    /**
     * 删除模型配置信息
     * * @param id 模型配置主键
     * @return 结果
     */
    public int deleteAiModelPricingById(Long id);

    /**
     * 验证模型连接并返回响应内容（AI实验室功能）
     * * @param aiModelPricing 模型配置
     * @return AI响应的文本内容或报错信息
     */
    public String verifyModelConnection(AiModelPricing aiModelPricing);

    /**
     * 【核心新增】：根据消耗量计算任务的人民币成本和积分消耗
     * 用于其他模块调用后，自动计算并写入 aigc_tasks 表
     * * @param pricingId 模型配置ID
     * @param inputTokens 输入Token数
     * @param outputTokens 输出Token数
     * @param seconds 消耗秒数
     * @param count 调用次数
     * @return 包含 consumedCredits(积分) 和 realCostCny(6位精度成本) 的Map
     */
    public Map<String, BigDecimal> calculateTaskCost(Long pricingId, Integer inputTokens, Integer outputTokens, Integer seconds, Integer count);
}