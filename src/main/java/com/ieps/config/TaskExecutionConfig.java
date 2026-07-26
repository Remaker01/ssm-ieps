package com.ieps.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步任务与定时任务配置
 *
 * <p><b>异步下载链路的线程池配置：</b></p>
 * <ul>
 *   <li><b>{@code @EnableAsync}</b> — 启用 Spring 异步方法执行，使
 *       {@link DownloadTaskWorkerService#processDownloadTask(String)} 的 {@code @Async} 生效</li>
 *   <li><b>{@code @EnableScheduling}</b> — 启用定时任务，使
 *       {@link DownloadTaskService#cleanupExpiredTasks()} 的 {@code @Scheduled} 生效</li>
 *   <li><b>{@code downloadTaskExecutor}</b> — 下载任务的专属线程池，与业务请求线程池隔离，
 *       防止 ZIP 打包占满 Web 容器线程</li>
 * </ul>
 *
 * <p>线程池参数通过 {@code ieps.storage.cos.download-task-executor-pool-size} 配置（默认 2），
 * 队列容量固定 32，超过时由调用线程执行（天然反压）。</p>
 */
@Configuration
@EnableAsync
@EnableScheduling
public class TaskExecutionConfig {

    @Autowired
    private IepsCosProperties iepsCosProperties;

    /**
     * 下载任务异步执行线程池
     *
     * <p>专用于 {@link DownloadTaskWorkerService#processDownloadTask(String)} 的 ZIP 打包操作。
     * 核心/最大线程数相同（固定大小线程池），队列容量 32，
     * 应用关闭时等待正在执行的任务完成。</p>
     */
    @Bean("downloadTaskExecutor")
    public Executor downloadTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int poolSize = Math.max(2, iepsCosProperties.getDownloadTaskExecutorPoolSize());
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize);
        executor.setQueueCapacity(32);
        executor.setThreadNamePrefix("ieps-download-task-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }
}
