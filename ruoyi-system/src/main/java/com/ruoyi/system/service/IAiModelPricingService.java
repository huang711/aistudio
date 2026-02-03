package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.AiModelPricing;

/**
 * 模型配置宽Service接口
 * * @author ruoyi
 * @date 2026-02-03
 */
public interface IAiModelPricingService
{
    /**
     * 查询模型配置宽
     * * @param id 模型配置宽主键
     * @return 模型配置宽
     */
    public AiModelPricing selectAiModelPricingById(Long id);

    /**
     * 查询模型配置宽列表
     * * @param aiModelPricing 模型配置宽
     * @return 模型配置宽集合
     */
    public List<AiModelPricing> selectAiModelPricingList(AiModelPricing aiModelPricing);

    /**
     * 新增模型配置宽
     * * @param aiModelPricing 模型配置宽
     * @return 结果
     */
    public int insertAiModelPricing(AiModelPricing aiModelPricing);

    /**
     * 修改模型配置宽
     * * @param aiModelPricing 模型配置宽
     * @return 结果
     */
    public int updateAiModelPricing(AiModelPricing aiModelPricing);

    /**
     * 批量删除模型配置宽
     * * @param ids 需要删除的模型配置宽主键集合
     * @return 结果
     */
    public int deleteAiModelPricingByIds(Long[] ids);

    /**
     * 删除模型配置宽信息
     * * @param id 模型配置宽主键
     * @return 结果
     */
    public int deleteAiModelPricingById(Long id);

    /**
     * 验证模型连接并返回响应内容
     * * @param aiModelPricing 模型配置
     * @return AI响应的文本内容或报错信息
     */
    public String verifyModelConnection(AiModelPricing aiModelPricing);
}