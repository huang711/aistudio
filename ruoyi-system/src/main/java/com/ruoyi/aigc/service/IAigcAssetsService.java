package com.ruoyi.aigc.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.aigc.domain.Assets;
import org.springframework.web.multipart.MultipartFile;

/**
 * 资产Service接口
 * * @author ruoyi
 */
public interface IAigcAssetsService {
    /**
     * 查询资产列表
     * * @param assets 资产信息
     * @return 资产集合
     */
    public List<Assets> selectAssetsList(Assets assets);

    /**
     * 查询资产详情
     * * @param id 资产ID
     * @return 资产信息
     */
    public Assets selectAssetsById(Long id);

    /**
     * 新增资产（纯数据入库）
     * * @param assets 资产信息
     * @return 结果
     */
    public int insertAssets(Assets assets);

    /**
     * 上传文件并新增资产
     * * @param file 物理文件
     * @param assets 资产信息
     * @return 结果
     */
    public int insertAssetsWithFile(MultipartFile file, Assets assets);

    /**
     * 修改资产信息
     * * @param assets 资产信息
     * @return 结果
     */
    public int updateAssets(Assets assets);

    /**
     * 批量删除资产
     * * @param ids 需要删除的资产ID
     * @return 结果
     */
    public int deleteAssetsByIds(Long[] ids);

    /**
     * 删除单个资产
     * * @param id 资产ID
     * @return 结果
     */
    public int deleteAssetsById(Long id);

    /**
     * 获取OSS存储统计信息（用量、文件数等）
     * * @return 统计Map
     */
    public Map<String, Object> getStorageStat();

    /**
     * 将网络图片URL转存至OSS并绑定到资产封面
     * * @param assetId 资产ID
     * @param imageUrl AI生成的网络图片URL
     * @return 结果
     */
    public int bindAiImageToAsset(Long assetId, String imageUrl);
}