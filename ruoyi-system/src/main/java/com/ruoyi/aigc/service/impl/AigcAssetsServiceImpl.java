package com.ruoyi.aigc.service.impl;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.BucketStat;
import com.aliyun.oss.model.ObjectMetadata;
import com.ruoyi.aigc.domain.Assets;
import com.ruoyi.aigc.domain.StorageFiles;
import com.ruoyi.aigc.mapper.AssetsMapper;
import com.ruoyi.aigc.mapper.StorageFilesMapper;
import com.ruoyi.aigc.service.IAigcAssetsService;
import com.ruoyi.common.config.AliyunOssConfig;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;

/**
 * 资产中心 Service 业务层处理 - 深度修复版
 * 解决了“存为新资产”无法显示图片的问题，实现了真正的自动转存逻辑。
 */
@Service
public class AigcAssetsServiceImpl implements IAigcAssetsService {

    private static final Logger log = LoggerFactory.getLogger(AigcAssetsServiceImpl.class);

    @Autowired
    private AssetsMapper assetsMapper;

    @Autowired
    private StorageFilesMapper storageFilesMapper;

    @Autowired
    private AliyunOssConfig ossConfig;

    private OSS createOssClient() {
        return new OSSClientBuilder().build(
                ossConfig.getEndpoint(),
                ossConfig.getAccessKeyId(),
                ossConfig.getAccessKeySecret()
        );
    }

    @Override
    public Map<String, Object> getStorageStat() {
        OSS ossClient = createOssClient();
        Map<String, Object> stat = new HashMap<>();
        try {
            BucketStat bucketStat = ossClient.getBucketStat(ossConfig.getBucketName());
            stat.put("usage", bucketStat.getStorageSize());
            stat.put("count", bucketStat.getObjectCount());
            stat.put("limit", 20L * 1024 * 1024 * 1024);
        } catch (Exception e) {
            log.error("获取OSS统计失败: {}", e.getMessage());
            stat.put("usage", 0L);
            stat.put("count", 0L);
            stat.put("limit", 20L * 1024 * 1024 * 1024);
        } finally {
            if (ossClient != null) ossClient.shutdown();
        }
        return stat;
    }

    @Override
    public Assets selectAssetsById(Long id) {
        return assetsMapper.selectAssetsById(id);
    }

    @Override
    public List<Assets> selectAssetsList(Assets assets) {
        return assetsMapper.selectAssetsList(assets);
    }

    @Override
    public int updateAssets(Assets assets) {
        assets.setUpdateTime(DateUtils.getNowDate());
        return assetsMapper.updateAssets(assets);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteAssetsByIds(Long[] ids) {
        OSS ossClient = createOssClient();
        try {
            for (Long id : ids) {
                Assets asset = assetsMapper.selectAssetsById(id);
                if (asset != null && asset.getCoverFileId() != null) {
                    StorageFiles fileInfo = storageFilesMapper.selectStorageFilesById(asset.getCoverFileId());
                    if (fileInfo != null) {
                        try {
                            ossClient.deleteObject(ossConfig.getBucketName(), fileInfo.getFileKey());
                        } catch (Exception e) {
                            log.error("OSS物理文件删除失败: {}", e.getMessage());
                        }
                        storageFilesMapper.deleteStorageFilesById(fileInfo.getId());
                    }
                }
            }
        } finally {
            if (ossClient != null) ossClient.shutdown();
        }
        return assetsMapper.deleteAssetsByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteAssetsById(Long id) {
        return deleteAssetsByIds(new Long[]{id});
    }

    /**
     * 重构逻辑：将“覆盖绑定”改为调用内部通用转存方法
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int bindAiImageToAsset(Long assetId, String imageUrl) {
        try {
            Assets asset = assetsMapper.selectAssetsById(assetId);
            if (asset == null) throw new RuntimeException("资产不存在");

            // 调用通用转存逻辑
            Long storageFileId = saveRemoteImageToStorage(imageUrl, asset.getWorkspaceId());

            // 更新资产关联
            Assets updateAsset = new Assets();
            updateAsset.setId(assetId);
            updateAsset.setCoverFileId(storageFileId);
            updateAsset.setUpdateTime(DateUtils.getNowDate());

            return assetsMapper.updateAssets(updateAsset);
        } catch (Exception e) {
            log.error("AI图片绑定失败: ", e);
            throw new RuntimeException("图片转存并关联失败: " + e.getMessage());
        }
    }

    /**
     * 核心改进：存为新资产时，自动检测并转存图片
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertAssets(Assets assets) {
        // 1. 如果前端传了临时 URL 字段，则先执行 OSS 转存
        if (StringUtils.isNotEmpty(assets.getUrl()) && assets.getUrl().startsWith("http")) {
            log.info("检测到新增资产携带图片URL，准备执行真实转存...");
            try {
                Long storageFileId = saveRemoteImageToStorage(assets.getUrl(), assets.getWorkspaceId());
                // 2. 将转存后的 ID 绑定给资产，这样列表就能显示了
                assets.setCoverFileId(storageFileId);
            } catch (Exception e) {
                log.error("新增资产时图片转存失败（跳过转存，继续保存文字）: {}", e.getMessage());
            }
        }

        assets.setCreateTime(DateUtils.getNowDate());
        return assetsMapper.insertAssets(assets);
    }

    /**
     * 私有通用逻辑：下载远程图片 -> 上传OSS -> 插入StorageFiles表 -> 返回文件ID
     * 这样可以保证不管是新增还是覆盖，逻辑完全对齐
     */
    private Long saveRemoteImageToStorage(String imageUrl, Long workspaceId) throws Exception {
        OSS ossClient = createOssClient();
        InputStream is = null;
        try {
            URL url = new URL(imageUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            is = conn.getInputStream();

            String fileKey = "assets/images/ai_" + UUID.randomUUID().toString() + ".png";
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType("image/png");
            metadata.setContentDisposition("inline");

            ossClient.putObject(ossConfig.getBucketName(), fileKey, is, metadata);

            StorageFiles sf = new StorageFiles();
            sf.setFileKey(fileKey);
            sf.setCdnUrl(ossConfig.getDomain() + "/" + fileKey);
            sf.setSizeBytes(Math.max(0L, (long) conn.getContentLength()));
            sf.setMimeType("image/png");
            sf.setWorkspaceId(workspaceId);
            sf.setUploaderId(SecurityUtils.getUserId());
            sf.setCreateTime(DateUtils.getNowDate());

            storageFilesMapper.insertStorageFiles(sf);
            return sf.getId(); // 返回自增ID
        } finally {
            if (is != null) is.close();
            if (ossClient != null) ossClient.shutdown();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertAssetsWithFile(MultipartFile file, Assets assets) {
        OSS ossClient = createOssClient();
        try {
            String originalFilename = file.getOriginalFilename();
            String suffix = originalFilename.contains(".") ? originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
            String businessDir = "script".equals(assets.getType()) ? "scripts" : "images";
            String fileKey = "assets/" + businessDir + "/" + UUID.randomUUID().toString() + suffix;

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());

            if ("script".equals(assets.getType())) {
                String fileName = (assets.getName() != null && !assets.getName().isEmpty())
                        ? assets.getName() + suffix : originalFilename;
                metadata.setContentDisposition("attachment; filename=\"" + URLEncoder.encode(fileName, "UTF-8") + "\"");
            } else {
                metadata.setContentDisposition("inline");
            }

            ossClient.putObject(ossConfig.getBucketName(), fileKey, file.getInputStream(), metadata);

            StorageFiles sf = new StorageFiles();
            sf.setFileKey(fileKey);
            sf.setCdnUrl(ossConfig.getDomain() + "/" + fileKey);
            sf.setSizeBytes(file.getSize());
            sf.setMimeType(file.getContentType());
            sf.setWorkspaceId(assets.getWorkspaceId());
            sf.setUploaderId(SecurityUtils.getUserId());
            sf.setCreateTime(DateUtils.getNowDate());
            storageFilesMapper.insertStorageFiles(sf);

            assets.setCoverFileId(sf.getId());
            if (assets.getName() == null || assets.getName().isEmpty()) {
                assets.setName(originalFilename.contains(".") ? originalFilename.substring(0, originalFilename.lastIndexOf(".")) : originalFilename);
            }
            assets.setCreateTime(DateUtils.getNowDate());

            return assetsMapper.insertAssets(assets);
        } catch (Exception e) {
            log.error("上传异常: ", e);
            throw new RuntimeException("上传失败: " + e.getMessage());
        } finally {
            if (ossClient != null) ossClient.shutdown();
        }
    }
}