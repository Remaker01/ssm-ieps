package com.ieps.service;

import org.springframework.context.annotation.Lazy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 异步下载任务执行器 —— 异步下载链路的"异步开关"
 *
 * <p><b>职责：</b></p>
 * <ul>
 *   <li>作为 {@link DownloadTaskService} 与 Spring {@code @Async} 之间的薄包装层</li>
 *   <li>将耗时的 ZIP 打包操作（从 COS 读取各文件 → 本地压缩 → 回传 COS）提交到独立线程池异步执行</li>
 * </ul>
 *
 * <p><b>为什么需要这个类？</b></p>
 * <p>如果将 {@code @Async} 直接放在 {@link DownloadTaskService} 的同级方法上，
 * 同 Service 内的调用（this.createDownloadTask → processDownloadTaskInternal）
 * 会使 {@code @Async} 失效（Spring AOP 代理限制）。因此拆出此类，
 * 使 {@link DownloadTaskController} 通过 {@link DownloadTaskWorkerService}
 * 调用时能正确触发异步代理。</p>
 *
 * <p><b>线程池：</b>使用 {@code downloadTaskExecutor} Bean，配置见
 * {@link com.ieps.config.TaskExecutionConfig}。默认核心/最大线程数为 2，队列容量 32。</p>
 */
@Service
public class DownloadTaskWorkerService {

    @Autowired
    @Lazy
    private DownloadTaskService downloadTaskService;

    /**
     * 异步执行下载任务的核心逻辑
     *
     * <p>此方法在 {@code downloadTaskExecutor} 线程池中执行，
     * 不会阻塞 HTTP 请求线程。执行时调用 {@link DownloadTaskService#processDownloadTaskInternal(String)}：</p>
     * <ol>
     *   <li>更新任务状态为 RUNNING</li>
     *   <li>从 COS 逐个读取源文件内容</li>
     *   <li>打包为本地 ZIP 文件</li>
     *   <li>将 ZIP 上传到 COS</li>
     *   <li>更新任务状态为 SUCCESS（含过期时间和 COS 路径）</li>
     *   <li>若异常则更新为 FAILED 并记录错误信息</li>
     * </ol>
     *
     * @param taskId 下载任务 ID
     */
    @Async("downloadTaskExecutor")
    public void processDownloadTask(String taskId) {
        downloadTaskService.processDownloadTaskInternal(taskId);
    }
}
