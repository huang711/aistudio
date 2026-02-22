package com.ruoyi.aigc.controller;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.aigc.domain.Assets;
import com.ruoyi.aigc.service.IAigcAssetsService;

/**
 * 资产管理 Controller
 * * @author ruoyi
 */
@RestController
@RequestMapping("/aigc/assets")
public class AigcAssetsController extends BaseController {

    @Autowired
    private IAigcAssetsService aigcAssetsService;

    /**
     * 查询资产列表
     * 前端访问: GET /aigc/assets/list
     */
    @PreAuthorize("@ss.hasPermi('aigc:assets:list')")
    @GetMapping("/list")
    public TableDataInfo list(Assets assets) {
        startPage(); // 开启若依分页功能
        List<Assets> list = aigcAssetsService.selectAssetsList(assets);
        return getDataTable(list);
    }

    /**
     * 获取资产详情
     */
    @PreAuthorize("@ss.hasPermi('aigc:assets:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(aigcAssetsService.selectAssetsById(id));
    }

    /**
     * 新增资产 (修复点：补全此方法以对应前端的“点亮”/快速新增功能)
     * 前端访问: POST /aigc/assets/add
     */
    @PreAuthorize("@ss.hasPermi('aigc:assets:add')")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody Assets assets) {
        return toAjax(aigcAssetsService.insertAssets(assets));
    }

    /**
     * 图片转存并绑定资产 (修复点：对应前端 handleBindImage 的逻辑)
     * 前端访问: POST /aigc/assets/updateImage
     */
    @PreAuthorize("@ss.hasPermi('aigc:assets:edit')")
    @PostMapping("/updateImage")
    public AjaxResult updateImage(@RequestBody Map<String, Object> params) {
        try {
            Long assetId = Long.valueOf(params.get("assetId").toString());
            String imageUrl = params.get("imageUrl").toString();
            // 调用 Service 层处理：下载图片 -> 上传OSS -> 更新资产关联的 cover_file_id
            int result = aigcAssetsService.bindAiImageToAsset(assetId, imageUrl);
            return result > 0 ? AjaxResult.success("资产封面绑定成功") : AjaxResult.error("绑定失败");
        } catch (Exception e) {
            logger.error("转存图片异常", e);
            return AjaxResult.error("系统异常：" + e.getMessage());
        }
    }

    /**
     * 获取OSS存储统计状态
     */
    @PreAuthorize("@ss.hasPermi('aigc:assets:list')")
    @GetMapping("/storage/stat")
    public AjaxResult getStorageStat() {
        return AjaxResult.success(aigcAssetsService.getStorageStat());
    }

    /**
     * 资产上传入库（带物理文件上传）
     */
    @PreAuthorize("@ss.hasPermi('aigc:assets:add')")
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public AjaxResult upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam("type") String type,
            @RequestParam("workspaceId") Long workspaceId,
            @RequestParam(value = "name", required = false) String name
    ) {
        if (file == null || file.isEmpty()) {
            return AjaxResult.error("上传文件不能为空");
        }

        Assets assets = new Assets();
        assets.setType(type);
        assets.setWorkspaceId(workspaceId);
        assets.setName(name);

        try {
            int result = aigcAssetsService.insertAssetsWithFile(file, assets);
            return result > 0 ? AjaxResult.success("资产上传成功") : AjaxResult.error("资产上传失败");
        } catch (Exception e) {
            logger.error("资产上传异常", e);
            return AjaxResult.error("上传系统异常：" + e.getMessage());
        }
    }

    /**
     * 修改资产
     */
    @PreAuthorize("@ss.hasPermi('aigc:assets:edit')")
    @PutMapping
    public AjaxResult edit(@RequestBody Assets assets) {
        return toAjax(aigcAssetsService.updateAssets(assets));
    }

    /**
     * 删除资产
     */
    @PreAuthorize("@ss.hasPermi('aigc:assets:remove')")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(aigcAssetsService.deleteAssetsByIds(ids));
    }
}