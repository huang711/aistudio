package com.ruoyi.aigc.service;

import java.util.List;
import com.ruoyi.aigc.domain.AigcTasks;

/**
 * AI任务Service接口
 * 
 * @author ruoyi
 * @date 2026-02-09
 */
public interface IAigcTasksService 
{
    /**
     * 查询AI任务
     * 
     * @param id AI任务主键
     * @return AI任务
     */
    public AigcTasks selectAigcTasksById(Long id);

    /**
     * 查询AI任务列表
     * 
     * @param aigcTasks AI任务
     * @return AI任务集合
     */
    public List<AigcTasks> selectAigcTasksList(AigcTasks aigcTasks);

    /**
     * 新增AI任务
     * 
     * @param aigcTasks AI任务
     * @return 结果
     */
    public int insertAigcTasks(AigcTasks aigcTasks);

    /**
     * 修改AI任务
     * 
     * @param aigcTasks AI任务
     * @return 结果
     */
    public int updateAigcTasks(AigcTasks aigcTasks);

    /**
     * 批量删除AI任务
     * 
     * @param ids 需要删除的AI任务主键集合
     * @return 结果
     */
    public int deleteAigcTasksByIds(Long[] ids);

    /**
     * 删除AI任务信息
     * 
     * @param id AI任务主键
     * @return 结果
     */
    public int deleteAigcTasksById(Long id);
}
