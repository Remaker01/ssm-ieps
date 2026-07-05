package com.ieps.service;

import org.springframework.context.annotation.Lazy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class DownloadTaskWorkerService {

    @Autowired
    @Lazy
    private DownloadTaskService downloadTaskService;

    @Async("downloadTaskExecutor")
    public void processDownloadTask(String taskId) {
        downloadTaskService.processDownloadTaskInternal(taskId);
    }
}
