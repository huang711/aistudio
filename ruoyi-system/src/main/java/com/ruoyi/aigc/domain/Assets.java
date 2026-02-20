package com.ruoyi.aigc.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 资产实体类 aigc_assets
 * * @author ruoyi
 */
public class Assets extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 所属工作空间ID */
    private Long workspaceId;

    /** 资产类型 (character-角色, scene-场景, item-道具) */
    private String type;

    /** 资产名称 */
    private String name;

    /** 资产描述 (或暂存的临时信息) */
    private String description;

    /** 关联存储文件ID (storage_files表主键) */
    private Long coverFileId;

    /** * 扩展字段：图片的完整 CDN URL
     * (该字段不在 assets 表中，是通过 storage_files 表左连接查询出来的)
     */
    private String url;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(Long workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getCoverFileId() {
        return coverFileId;
    }

    public void setCoverFileId(Long coverFileId) {
        this.coverFileId = coverFileId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("workspaceId", getWorkspaceId())
                .append("type", getType())
                .append("name", getName())
                .append("description", getDescription())
                .append("coverFileId", getCoverFileId())
                .append("url", getUrl())
                .append("createTime", getCreateTime())
                .append("updateTime", getUpdateTime())
                .toString();
    }
}