package com.ruoyi.aigc.mapper;

import java.util.List;
import com.ruoyi.aigc.domain.Assets;
import org.apache.ibatis.annotations.Mapper;

/**
 * 资产 Mapper 接口
 */
@Mapper
public interface AssetsMapper {
    /**
     * 查询资产详情
     * @param id 资产ID
     * @return 资产信息
     */
    public Assets selectAssetsById(Long id);

    /**
     * 查询资产列表
     * @param assets 资产查询条件
     * @return 资产集合
     */
    public List<Assets> selectAssetsList(Assets assets);

    /**
     * 新增资产
     * @param assets 资产信息
     * @return 结果
     */
    public int insertAssets(Assets assets);

    /**
     * 修改资产
     * @param assets 资产信息
     * @return 结果
     */
    public int updateAssets(Assets assets);

    /**
     * 批量删除资产
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteAssetsByIds(Long[] ids);

    /**
     * 删除单个资产
     * @param id 资产ID
     * @return 结果
     */
    public int deleteAssetsById(Long id);
}