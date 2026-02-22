package com.ruoyi.aigc.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * AI任务对象 aigc_tasks
 * 
 * @author ruoyi
 * @date 2026-02-09
 */
public class AigcTasks extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** $column.columnComment */
    private Long id;

    /** $column.columnComment */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private Long workspaceId;

    /** 扣费账户 */
    @Excel(name = "扣费账户")
    private Long userId;

    /** 类型(txt2img等) */
    @Excel(name = "类型(txt2img等)")
    private String taskType;

    /** 配置ID */
    @Excel(name = "配置ID")
    private Long usedModelId;

    /** 模型快照 */
    @Excel(name = "模型快照")
    private String modelNameSnapshot;

    /** 厂商快照 */
    @Excel(name = "厂商快照")
    private String providerSnapshot;

    /** 用户输入的 Prompt/原始文本 */
    @Excel(name = "用户输入的 Prompt/原始文本")
    private String inputContent;

    /** $column.columnComment */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private Long usageTokensInput;

    /** $column.columnComment */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private Long usageTokensOutput;

    /** AI 生成的最终文本结果 */
    @Excel(name = "AI 生成的最终文本结果")
    private String outputContent;

    /** $column.columnComment */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private Long usageSeconds;

    /** $column.columnComment */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private Long usageCount;

    /** 实扣积分 */
    @Excel(name = "实扣积分")
    private BigDecimal consumedCredits;

    /** 平台成本(￥) */
    @Excel(name = "平台成本(￥)")
    private BigDecimal realCostCny;

    /** 关联表 */
    @Excel(name = "关联表")
    private String targetTable;

    /** 关联ID */
    @Excel(name = "关联ID")
    private Long targetId;

    /** 状态(0排队/1执行/2完成/3失败) */
    @Excel(name = "状态(0排队/1执行/2完成/3失败)")
    private Long status;

    /** $column.columnComment */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private Long resultFileId;

    /** 错误详情 */
    @Excel(name = "错误详情")
    private String errorMsg;

    /** $column.columnComment */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private Long durationMs;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setWorkspaceId(Long workspaceId) 
    {
        this.workspaceId = workspaceId;
    }

    public Long getWorkspaceId() 
    {
        return workspaceId;
    }

    public void setUserId(Long userId) 
    {
        this.userId = userId;
    }

    public Long getUserId() 
    {
        return userId;
    }

    public void setTaskType(String taskType) 
    {
        this.taskType = taskType;
    }

    public String getTaskType() 
    {
        return taskType;
    }

    public void setUsedModelId(Long usedModelId) 
    {
        this.usedModelId = usedModelId;
    }

    public Long getUsedModelId() 
    {
        return usedModelId;
    }

    public void setModelNameSnapshot(String modelNameSnapshot) 
    {
        this.modelNameSnapshot = modelNameSnapshot;
    }

    public String getModelNameSnapshot() 
    {
        return modelNameSnapshot;
    }

    public void setProviderSnapshot(String providerSnapshot) 
    {
        this.providerSnapshot = providerSnapshot;
    }

    public String getProviderSnapshot() 
    {
        return providerSnapshot;
    }

    public void setInputContent(String inputContent) 
    {
        this.inputContent = inputContent;
    }

    public String getInputContent() 
    {
        return inputContent;
    }

    public void setUsageTokensInput(Long usageTokensInput) 
    {
        this.usageTokensInput = usageTokensInput;
    }

    public Long getUsageTokensInput() 
    {
        return usageTokensInput;
    }

    public void setUsageTokensOutput(Long usageTokensOutput) 
    {
        this.usageTokensOutput = usageTokensOutput;
    }

    public Long getUsageTokensOutput() 
    {
        return usageTokensOutput;
    }

    public void setOutputContent(String outputContent) 
    {
        this.outputContent = outputContent;
    }

    public String getOutputContent() 
    {
        return outputContent;
    }

    public void setUsageSeconds(Long usageSeconds) 
    {
        this.usageSeconds = usageSeconds;
    }

    public Long getUsageSeconds() 
    {
        return usageSeconds;
    }

    public void setUsageCount(Long usageCount) 
    {
        this.usageCount = usageCount;
    }

    public Long getUsageCount() 
    {
        return usageCount;
    }

    public void setConsumedCredits(BigDecimal consumedCredits) 
    {
        this.consumedCredits = consumedCredits;
    }

    public BigDecimal getConsumedCredits() 
    {
        return consumedCredits;
    }

    public void setRealCostCny(BigDecimal realCostCny) 
    {
        this.realCostCny = realCostCny;
    }

    public BigDecimal getRealCostCny() 
    {
        return realCostCny;
    }

    public void setTargetTable(String targetTable) 
    {
        this.targetTable = targetTable;
    }

    public String getTargetTable() 
    {
        return targetTable;
    }

    public void setTargetId(Long targetId) 
    {
        this.targetId = targetId;
    }

    public Long getTargetId() 
    {
        return targetId;
    }

    public void setStatus(Long status) 
    {
        this.status = status;
    }

    public Long getStatus() 
    {
        return status;
    }

    public void setResultFileId(Long resultFileId) 
    {
        this.resultFileId = resultFileId;
    }

    public Long getResultFileId() 
    {
        return resultFileId;
    }

    public void setErrorMsg(String errorMsg) 
    {
        this.errorMsg = errorMsg;
    }

    public String getErrorMsg() 
    {
        return errorMsg;
    }

    public void setDurationMs(Long durationMs) 
    {
        this.durationMs = durationMs;
    }

    public Long getDurationMs() 
    {
        return durationMs;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("workspaceId", getWorkspaceId())
            .append("userId", getUserId())
            .append("taskType", getTaskType())
            .append("usedModelId", getUsedModelId())
            .append("modelNameSnapshot", getModelNameSnapshot())
            .append("providerSnapshot", getProviderSnapshot())
            .append("inputContent", getInputContent())
            .append("usageTokensInput", getUsageTokensInput())
            .append("usageTokensOutput", getUsageTokensOutput())
            .append("outputContent", getOutputContent())
            .append("usageSeconds", getUsageSeconds())
            .append("usageCount", getUsageCount())
            .append("consumedCredits", getConsumedCredits())
            .append("realCostCny", getRealCostCny())
            .append("targetTable", getTargetTable())
            .append("targetId", getTargetId())
            .append("status", getStatus())
            .append("resultFileId", getResultFileId())
            .append("errorMsg", getErrorMsg())
            .append("durationMs", getDurationMs())
            .append("createTime", getCreateTime())
            .toString();
    }
}
