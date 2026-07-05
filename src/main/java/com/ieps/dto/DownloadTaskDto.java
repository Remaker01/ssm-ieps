package com.ieps.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

public class DownloadTaskDto {

    private String taskId;

    private String status;

    private Integer fileCount;

    private String zipObjectKey;

    private String zipFileName;

    private String errorMessage;

    private String downloadPath;

    private String downloadUrl;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createdTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startedTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date finishedTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date expireTime;

    public String getTaskId() {
        return taskId;
    }

    public DownloadTaskDto setTaskId(String taskId) {
        this.taskId = taskId;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public DownloadTaskDto setStatus(String status) {
        this.status = status;
        return this;
    }

    public Integer getFileCount() {
        return fileCount;
    }

    public DownloadTaskDto setFileCount(Integer fileCount) {
        this.fileCount = fileCount;
        return this;
    }

    public String getZipObjectKey() {
        return zipObjectKey;
    }

    public DownloadTaskDto setZipObjectKey(String zipObjectKey) {
        this.zipObjectKey = zipObjectKey;
        return this;
    }

    public String getZipFileName() {
        return zipFileName;
    }

    public DownloadTaskDto setZipFileName(String zipFileName) {
        this.zipFileName = zipFileName;
        return this;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public DownloadTaskDto setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }

    public String getDownloadPath() {
        return downloadPath;
    }

    public DownloadTaskDto setDownloadPath(String downloadPath) {
        this.downloadPath = downloadPath;
        return this;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public DownloadTaskDto setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
        return this;
    }

    public Date getCreatedTime() {
        return createdTime;
    }

    public DownloadTaskDto setCreatedTime(Date createdTime) {
        this.createdTime = createdTime;
        return this;
    }

    public Date getStartedTime() {
        return startedTime;
    }

    public DownloadTaskDto setStartedTime(Date startedTime) {
        this.startedTime = startedTime;
        return this;
    }

    public Date getFinishedTime() {
        return finishedTime;
    }

    public DownloadTaskDto setFinishedTime(Date finishedTime) {
        this.finishedTime = finishedTime;
        return this;
    }

    public Date getExpireTime() {
        return expireTime;
    }

    public DownloadTaskDto setExpireTime(Date expireTime) {
        this.expireTime = expireTime;
        return this;
    }
}
