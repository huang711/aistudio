package com.ruoyi.aigc.mapper;

import com.ruoyi.aigc.domain.StorageFiles;

public interface StorageFilesMapper {
    /** 插入物理文件记录，并返回自增主键ID */
    public int insertStorageFiles(StorageFiles storageFiles);

    /** 根据ID查询存储文件详情 */
    public StorageFiles selectStorageFilesById(Long id);

    /** 根据ID删除存储文件记录 */
    public int deleteStorageFilesById(Long id);
}