package com.ruoyi.system.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 模型配置宽对象 sys_ai_models_pricing
 * 
 * @author ruoyi
 * @date 2026-02-03
 */
public class AiModelPricing extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** $column.columnComment */
    private Long id;

    /** 商品名 */
    @Excel(name = "商品名")
    private String displayName;

    /** 模型代码 */
    @Excel(name = "模型代码")
    private String modelCode;

    /** 类型(text/image/video) */
    @Excel(name = "类型(text/image/video)")
    private String type;

    /** 厂商 */
    @Excel(name = "厂商")
    private String provider;

    /** API地址 */
    @Excel(name = "API地址")
    private String apiEndpoint;

    /** AK */
    @Excel(name = "AK")
    private String accessKeyId;

    /** SK */
    @Excel(name = "SK")
    private String secretAccessKey;

    /** 扩展配置 */
    @Excel(name = "扩展配置")
    private String extraConfig;

    /** 计费模式 */
    @Excel(name = "计费模式")
    private String billingMode;

    /** 按次积分 */
    @Excel(name = "按次积分")
    private BigDecimal pointsPerReq;

    /** 按Token积分 */
    @Excel(name = "按Token积分")
    private BigDecimal pointsPer1kTokens;

    /** 按秒积分 */
    @Excel(name = "按秒积分")
    private BigDecimal pointsPerSecond;

    /** 成本:单次 */
    @Excel(name = "成本:单次")
    private BigDecimal costPerReqCny;

    /** 成本:Token */
    @Excel(name = "成本:Token")
    private BigDecimal costPer1kCny;

    /** 成本:每秒 */
    @Excel(name = "成本:每秒")
    private BigDecimal costPerSecondCny;

    /** 上架状态 */
    @Excel(name = "上架状态")
    private Integer isActive;

    /** 排序 */
    @Excel(name = "排序")
    private Long sortOrder;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setDisplayName(String displayName) 
    {
        this.displayName = displayName;
    }

    public String getDisplayName() 
    {
        return displayName;
    }

    public void setModelCode(String modelCode) 
    {
        this.modelCode = modelCode;
    }

    public String getModelCode() 
    {
        return modelCode;
    }

    public void setType(String type) 
    {
        this.type = type;
    }

    public String getType() 
    {
        return type;
    }

    public void setProvider(String provider) 
    {
        this.provider = provider;
    }

    public String getProvider() 
    {
        return provider;
    }

    public void setApiEndpoint(String apiEndpoint) 
    {
        this.apiEndpoint = apiEndpoint;
    }

    public String getApiEndpoint() 
    {
        return apiEndpoint;
    }

    public void setAccessKeyId(String accessKeyId) 
    {
        this.accessKeyId = accessKeyId;
    }

    public String getAccessKeyId() 
    {
        return accessKeyId;
    }

    public void setSecretAccessKey(String secretAccessKey) 
    {
        this.secretAccessKey = secretAccessKey;
    }

    public String getSecretAccessKey() 
    {
        return secretAccessKey;
    }

    public void setExtraConfig(String extraConfig) 
    {
        this.extraConfig = extraConfig;
    }

    public String getExtraConfig() 
    {
        return extraConfig;
    }

    public void setBillingMode(String billingMode) 
    {
        this.billingMode = billingMode;
    }

    public String getBillingMode() 
    {
        return billingMode;
    }

    public void setPointsPerReq(BigDecimal pointsPerReq) 
    {
        this.pointsPerReq = pointsPerReq;
    }

    public BigDecimal getPointsPerReq() 
    {
        return pointsPerReq;
    }

    public void setPointsPer1kTokens(BigDecimal pointsPer1kTokens) 
    {
        this.pointsPer1kTokens = pointsPer1kTokens;
    }

    public BigDecimal getPointsPer1kTokens() 
    {
        return pointsPer1kTokens;
    }

    public void setPointsPerSecond(BigDecimal pointsPerSecond) 
    {
        this.pointsPerSecond = pointsPerSecond;
    }

    public BigDecimal getPointsPerSecond() 
    {
        return pointsPerSecond;
    }

    public void setCostPerReqCny(BigDecimal costPerReqCny) 
    {
        this.costPerReqCny = costPerReqCny;
    }

    public BigDecimal getCostPerReqCny() 
    {
        return costPerReqCny;
    }

    public void setCostPer1kCny(BigDecimal costPer1kCny) 
    {
        this.costPer1kCny = costPer1kCny;
    }

    public BigDecimal getCostPer1kCny() 
    {
        return costPer1kCny;
    }

    public void setCostPerSecondCny(BigDecimal costPerSecondCny) 
    {
        this.costPerSecondCny = costPerSecondCny;
    }

    public BigDecimal getCostPerSecondCny() 
    {
        return costPerSecondCny;
    }

    public void setIsActive(Integer isActive) 
    {
        this.isActive = isActive;
    }

    public Integer getIsActive() 
    {
        return isActive;
    }

    public void setSortOrder(Long sortOrder) 
    {
        this.sortOrder = sortOrder;
    }

    public Long getSortOrder() 
    {
        return sortOrder;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("displayName", getDisplayName())
            .append("modelCode", getModelCode())
            .append("type", getType())
            .append("provider", getProvider())
            .append("apiEndpoint", getApiEndpoint())
            .append("accessKeyId", getAccessKeyId())
            .append("secretAccessKey", getSecretAccessKey())
            .append("extraConfig", getExtraConfig())
            .append("billingMode", getBillingMode())
            .append("pointsPerReq", getPointsPerReq())
            .append("pointsPer1kTokens", getPointsPer1kTokens())
            .append("pointsPerSecond", getPointsPerSecond())
            .append("costPerReqCny", getCostPerReqCny())
            .append("costPer1kCny", getCostPer1kCny())
            .append("costPerSecondCny", getCostPerSecondCny())
            .append("isActive", getIsActive())
            .append("sortOrder", getSortOrder())
            .append("createTime", getCreateTime())
            .toString();
    }
}
