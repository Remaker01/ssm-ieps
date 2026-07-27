package com.ieps.event;

import org.springframework.context.ApplicationEvent;

/**
 * 异步下载任务创建事件
 * 发布后由 {@link com.ieps.service.DownloadTaskWorkerService} 异步订阅，
 * 触发 COS 多文件 ZIP 打包流程。
 */
public class DownloadTaskCreatedEvent extends ApplicationEvent {

    private final String taskId;

    public DownloadTaskCreatedEvent(Object source, String taskId) {
        super(source);
        this.taskId = taskId;
    }

    public String getTaskId() {
        return taskId;
    }
}
