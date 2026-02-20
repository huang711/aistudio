package com.ruoyi.aigc.controller;

import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.aigc.domain.AigcTasks;
import com.ruoyi.aigc.domain.Assets;
import com.ruoyi.aigc.service.IAigcTasksService;
import com.ruoyi.aigc.service.IAigcAssetsService;
import com.ruoyi.system.domain.AiModelPricing;
import com.ruoyi.system.service.IAiModelPricingService;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * AI实验室工作台及全自动创作接口
 */
@RestController
@RequestMapping("/aigc/tasks")
public class AigcTasksController extends BaseController
{
    @Autowired
    private IAigcTasksService aigcTasksService;

    @Autowired
    private IAigcAssetsService assetsService;

    @Autowired
    private IAiModelPricingService aiModelPricingService;

    /**
     * 1. 查询任务历史列表
     */
    @PreAuthorize("@ss.hasPermi('aigc:tasks:list')")
    @GetMapping("/list")
    public TableDataInfo list(AigcTasks aigcTasks)
    {
        startPage();
        List<AigcTasks> list = aigcTasksService.selectAigcTasksList(aigcTasks);
        return getDataTable(list);
    }

    /**
     * 2. 获取任务详细信息
     */
    @PreAuthorize("@ss.hasPermi('aigc:tasks:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(aigcTasksService.selectAigcTasksById(id));
    }

    /**
     * 3. 提交剧本解析 (AI任务入库)
     */
    @PreAuthorize("@ss.hasPermi('aigc:tasks:add')")
    @Log(title = "AI任务解析", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AigcTasks aigcTasks)
    {
        if (aigcTasks.getWorkspaceId() == null) {
            aigcTasks.setWorkspaceId(1L);
        }
        aigcTasksService.insertAigcTasks(aigcTasks);
        return AjaxResult.success("任务已提交解析", aigcTasks);
    }

    /**
     * 4. 同步云端内容 (手动修改后的更新)
     */
    @PreAuthorize("@ss.hasPermi('aigc:tasks:edit')")
    @Log(title = "同步云端内容", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AigcTasks aigcTasks)
    {
        return toAjax(aigcTasksService.updateAigcTasks(aigcTasks));
    }

    /**
     * 5. 获取资产库列表
     */
    @GetMapping("/assets/list")
    public AjaxResult getAssetsList(Assets assets)
    {
        if (assets.getWorkspaceId() == null) assets.setWorkspaceId(1L);
        return success(assetsService.selectAssetsList(assets));
    }

    /**
     * 6. 新增资产元数据
     */
    @PreAuthorize("@ss.hasPermi('aigc:tasks:add')")
    @Log(title = "新增资产", businessType = BusinessType.INSERT)
    @PostMapping("/assets/add")
    public AjaxResult addAsset(@RequestBody Map<String, Object> params)
    {
        String name = params.get("name") != null ? params.get("name").toString() : "";
        String type = params.get("type") != null ? params.get("type").toString() : "character";
        if (name.trim().isEmpty()) return error("名称不能为空");

        Assets assets = new Assets();
        assets.setName(name);
        assets.setType(type);
        assets.setWorkspaceId(params.get("workspaceId") != null ? Long.valueOf(params.get("workspaceId").toString()) : 1L);

        return assetsService.insertAssets(assets) > 0 ? AjaxResult.success("资产入库成功", assets) : error("失败");
    }

    /**
     * 10. 【修复核心】绑定并保存生成的 AI 图片到资产
     * 逻辑：调用 Service 转存 OSS 并通过 cover_file_id 关联
     */
    @PreAuthorize("@ss.hasPermi('aigc:tasks:edit')")
    @Log(title = "绑定AI生图到资产", businessType = BusinessType.UPDATE)
    @PostMapping("/assets/updateImage")
    public AjaxResult updateAssetImage(@RequestBody Map<String, Object> params)
    {
        Object assetIdObj = params.get("assetId");
        Object imageUrlObj = params.get("imageUrl");

        if (assetIdObj == null || imageUrlObj == null) {
            return error("参数错误：资产ID或图片URL不能为空");
        }

        Long assetId = Long.valueOf(assetIdObj.toString());
        String imageUrl = imageUrlObj.toString();

        // 调用 Service 层新实现的转存并绑定逻辑
        int rows = assetsService.bindAiImageToAsset(assetId, imageUrl);

        return toAjax(rows);
    }

    /**
     * 7. 删除任务
     */
    @PreAuthorize("@ss.hasPermi('aigc:tasks:remove')")
    @Log(title = "删除AI任务", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(aigcTasksService.deleteAigcTasksByIds(ids));
    }

    /**
     * 8. 【真实美化】调用 LLM 接口美化提示词
     */
    @PreAuthorize("@ss.hasPermi('aigc:tasks:edit')")
    @PostMapping("/optimize-prompt")
    public AjaxResult optimizePrompt(@RequestBody Map<String, Object> params)
    {
        String text = (String) params.get("text");
        Object modelIdObj = params.get("usedModelId");
        if (modelIdObj == null) return error("未选择文本模型");

        Long modelId = Long.valueOf(modelIdObj.toString());
        AiModelPricing pricing = aiModelPricingService.selectAiModelPricingById(modelId);
        if (pricing == null) return error("后台模型配置不存在");

        pricing.setRemark("你是一位专业的绘画导演，请将以下剧本内容翻译并扩充为生图 Prompt，只返回结果：" + text);

        try {
            String aiResult = aiModelPricingService.verifyModelConnection(pricing);
            return AjaxResult.success("AI美化完成", aiResult);
        } catch (Exception e) {
            return error("美化失败: " + e.getMessage());
        }
    }

    /**
     * 9. 【真实生图】调用扩散模型生成画面
     */
    @PreAuthorize("@ss.hasPermi('aigc:tasks:draw')")
    @PostMapping("/draw")
    public AjaxResult drawImage(@RequestBody Map<String, Object> params)
    {
        String prompt = (String) params.get("prompt");
        Object modelIdObj = params.get("usedModelId");
        if (modelIdObj == null) return error("未选择生图模型");

        Long modelId = Long.valueOf(modelIdObj.toString());
        AiModelPricing pricing = aiModelPricingService.selectAiModelPricingById(modelId);
        if (pricing == null) return error("模型不存在");

        pricing.setType("image");
        pricing.setRemark(prompt);

        try {
            String result = aiModelPricingService.verifyModelConnection(pricing);
            String cleanUrl = result.replace("[图片]: ", "").trim();
            return AjaxResult.success("生图成功", cleanUrl);
        } catch (Exception e) {
            return error("生图引擎异常: " + e.getMessage());
        }
    }
}