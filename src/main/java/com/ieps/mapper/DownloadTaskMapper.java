package com.ieps.mapper;

import com.ieps.pojo.DownloadTask;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface DownloadTaskMapper {

    int insertSelective(DownloadTask record);

    DownloadTask selectByTaskId(String taskId);

    int updateByTaskIdSelective(DownloadTask record);

    List<DownloadTask> selectSuccessTasksExpiredBefore(@Param("now") Date now);

    List<DownloadTask> selectHistoryTasksBefore(@Param("finishedBefore") Date finishedBefore);

    int batchDeleteByTaskIds(@Param("taskIds") String[] taskIds);
}
