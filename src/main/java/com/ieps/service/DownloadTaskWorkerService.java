package com.ieps.service;

import com.ieps.event.DownloadTaskCreatedEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 异步下载任务执行器 —— 事件驱动的异步开关
 *
 * <p><b>职责：</b></p>
 * <ul>
 *   <li>监听 {@link DownloadTaskCreatedEvent} 事件，触发耗时的 ZIP 打包操作</li>
 *   <li>将异步处理提交到独立线程池，不阻塞 HTTP 响应</li>
 * </ul>
 *
 * <p><b>事件驱动解耦：</b></p>
 * <p>{@link DownloadTaskService} 在创建任务后发布事件即返回，完全不感知 ZIP 打包的实现细节。
 * 该模式实现了类似消息队列的"发布-订阅"语义，但无需引入额外中间件。</p>
 *
 * <p><b>线程池：</b>使用 {@code downloadTaskExecutor} Bean，配置见
 * {@link com.ieps.config.TaskExecutionConfig}。核心线程 2，队列容量 32。</p>
 */
@Service
public class DownloadTaskWorkerService {

    @Autowired
    @Lazy
    private DownloadTaskService downloadTaskService;

    /**
     * 订阅下载任务创建事件，异步执行 ZIP 打包
     *
     * <p>{@code @Async} 确保不在发布线程（即 Tomcat 工作线程）中执行，
     * {@code @EventListener} 使 Spring 自动建立事件-订阅者的关联。</p>
     */
    @Async("downloadTaskExecutor")
    @EventListener
    public void onDownloadTaskCreated(DownloadTaskCreatedEvent event) {
        downloadTaskService.processDownloadTaskInternal(event.getTaskId());
    }
}
