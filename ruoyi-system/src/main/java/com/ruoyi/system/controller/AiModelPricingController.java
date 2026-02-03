package com.ruoyi.system.controller;

import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.AiModelPricing;
import com.ruoyi.system.service.IAiModelPricingService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 模型配置宽Controller
 * * @author ruoyi
 * @date 2026-02-03
 */
@RestController
@RequestMapping("/system/pricing")
public class AiModelPricingController extends BaseController
{
    @Autowired
    private IAiModelPricingService aiModelPricingService;

    /**
     * 查询模型配置宽列表
     */
    @PreAuthorize("@ss.hasPermi('system:pricing:list')")
    @GetMapping("/list")
    public TableDataInfo list(AiModelPricing aiModelPricing)
    {
        startPage();
        List<AiModelPricing> list = aiModelPricingService.selectAiModelPricingList(aiModelPricing);
        return getDataTable(list);
    }

    /**
     * 导出模型配置宽列表
     */
    @PreAuthorize("@ss.hasPermi('system:pricing:export')")
    @Log(title = "模型配置宽", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AiModelPricing aiModelPricing)
    {
        List<AiModelPricing> list = aiModelPricingService.selectAiModelPricingList(aiModelPricing);
        ExcelUtil<AiModelPricing> util = new ExcelUtil<AiModelPricing>(AiModelPricing.class);
        util.exportExcel(response, list, "模型配置宽数据");
    }

    /**
     * 获取模型配置宽详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:pricing:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(aiModelPricingService.selectAiModelPricingById(id));
    }

    /**
     * 新增模型配置宽
     */
    @PreAuthorize("@ss.hasPermi('system:pricing:add')")
    @Log(title = "模型配置宽", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AiModelPricing aiModelPricing)
    {
        return toAjax(aiModelPricingService.insertAiModelPricing(aiModelPricing));
    }

    /**
     * 修改模型配置宽
     */
    @PreAuthorize("@ss.hasPermi('system:pricing:edit')")
    @Log(title = "模型配置宽", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AiModelPricing aiModelPricing)
    {
        return toAjax(aiModelPricingService.updateAiModelPricing(aiModelPricing));
    }

    /**
     * 删除模型配置宽
     */
    @PreAuthorize("@ss.hasPermi('system:pricing:remove')")
    @Log(title = "模型配置宽", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(aiModelPricingService.deleteAiModelPricingByIds(ids));
    }

    /**
     * 测试模型连接
     * 修改功能：返回 AI 响应的具体内容
     */
    @PreAuthorize("@ss.hasPermi('system:pricing:edit')")
    @PostMapping("/testConnection")
    public AjaxResult testConnection(@RequestBody AiModelPricing aiModelPricing)
    {
        // 调用 service，此时返回的是 AI 的回复内容 (如 "hi") 或报错信息
        String aiResponse = aiModelPricingService.verifyModelConnection(aiModelPricing);

        // 使用 success(msg, data) 格式，将 AI 的内容放在 data 字段传回
        return AjaxResult.success("连接成功", aiResponse);
    }
}