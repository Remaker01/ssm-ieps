package com.ieps.mapper;

import com.ieps.pojo.DownloadTask;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 异步下载任务 Mapper 接口
 *
 * <p><b>异步下载流程中的数据访问层：</b></p>
 * <ul>
 *   <li>创建任务时 {@link #insertSelective(DownloadTask)} 插入 PENDING 状态记录</li>
 *   <li>异步执行时 {@link #updateByTaskIdSelective(DownloadTask)} 逐步更新状态</li>
 *   <li>前端轮询时 {@link #selectByTaskId(String)} 查询当前状态</li>
 *   <li>定时任务通过 {@link #selectSuccessTasksExpiredBefore(Date)} 和
 *       {@link #selectHistoryTasksBefore(Date)} 清理过期数据</li>
 * </ul>
 *
 * <p>对应数据库表：{@code ieps_download_task}</p>
 */
public interface DownloadTaskMapper {

    /** 插入下载任务记录（仅插入非空字段） */
    int insertSelective(DownloadTask record);

    /** 根据 taskId 查询下载任务 */
    DownloadTask selectByTaskId(String taskId);

    /** 根据 taskId 更新下载任务（仅更新非空字段，update_time 自动设置） */
    int updateByTaskIdSelective(DownloadTask record);

    /** 查询指定时间前已过期但未处理的 SUCCESS 任务（用于定时过期清理） */
    List<DownloadTask> selectSuccessTasksExpiredBefore(@Param("now") Date now);

    /** 查询指定时间前已完成的历史任务（用于定时历史清理） */
    List<DownloadTask> selectHistoryTasksBefore(@Param("finishedBefore") Date finishedBefore);

    /** 批量删除下载任务记录 */
    int batchDeleteByTaskIds(@Param("taskIds") String[] taskIds);
}
