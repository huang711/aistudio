package com.ruoyi.aigc.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 物理存储文件实体类 storage_files
 */
public class StorageFiles extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String fileKey;
    private String cdnUrl;
    private Long sizeBytes; // 对应 SQL 里的 size_bytes
    private String mimeType;
    private Long workspaceId; // 对应 SQL 里的 workspace_id
    private Long uploaderId;  // 对应 SQL 里的 uploader_id

    // --- 以下是手动生成的 Getter 和 Setter ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFileKey() { return fileKey; }
    public void setFileKey(String fileKey) { this.fileKey = fileKey; }

    public String getCdnUrl() { return cdnUrl; }
    public void setCdnUrl(String cdnUrl) { this.cdnUrl = cdnUrl; }

    public Long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    // 就是因为少了下面这两个方法，Service 才会报错
    public Long getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(Long workspaceId) { this.workspaceId = workspaceId; }

    public Long getUploaderId() { return uploaderId; }
    public void setUploaderId(Long uploaderId) { this.uploaderId = uploaderId; }
}