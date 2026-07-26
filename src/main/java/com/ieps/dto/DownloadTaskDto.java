package com.ieps.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

/**
 * 异步下载任务 DTO（返回给前端的数据视图）
 *
 * <p>相比实体 {@link com.ieps.pojo.DownloadTask}，增加了两个前端所需的字段：</p>
 * <ul>
 *   <li>{@code downloadPath} — 下载重定向路径（固定 {@code /downloadTasks/{taskId}/download}）</li>
 *   <li>{@code downloadUrl} — COS 预签名下载 URL（仅 SUCCESS 状态且未过期时填充）</li>
 * </ul>
 *
 * <p>前端轮询时根据 {@code status} 判断当前进度：
 * <ul>
 *   <li>{@code pending/running} — 继续轮询，不展示下载按钮</li>
 *   <li>{@code success} — 显示下载按钮，使用 {@code downloadUrl} 或 {@code downloadPath} 触发下载</li>
 *   <li>{@code failed} — 显示 {@code errorMessage} 中的错误提示</li>
 *   <li>{@code expired} — 提示用户重新创建下载任务</li>
 * </ul>
 */
public class DownloadTaskDto {

    /** 任务唯一标识 */
    private String taskId;

    /** 任务状态：pending / running / success / failed / expired */
    private String status;

    /** 待打包的文件数量 */
    private Integer fileCount;

    /** ZIP 包在 COS 中的对象键（内部使用，前端一般不需要） */
    private String zipObjectKey;

    /** ZIP 包文件名 */
    private String zipFileName;

    /** 打包失败时的错误信息 */
    private String errorMessage;

    /** 下载重定向路径（固定值：/downloadTasks/{taskId}/download） */
    private String downloadPath;

    /** COS 预签名下载 URL（仅 SUCCESS 且未过期时有效） */
    private String downloadUrl;

    /** 任务创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createdTime;

    /** 任务开始打包时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startedTime;

    /** 任务完成时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date finishedTime;

    /** 下载链接过期时间 */
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
