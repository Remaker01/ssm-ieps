package com.ieps.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * 异步下载任务实体类，映射数据库表 {@code ieps_download_task}
 *
 * <p><b>状态流转：</b></p>
 * <pre>
 *                    ┌──→ FAILED
 *   PENDING ──→ RUNNING ──→ SUCCESS ──→ EXPIRED
 * </pre>
 *
 * <p>每条记录代表一次"一键打包下载"操作，包含从创建到过期完成的完整生命周期。
 * sourceFiles 字段存储换行分隔的源文件名列表。</p>
 *
 * @see com.ieps.enums.DownloadTaskStatus
 */
public class DownloadTask {

    /** 任务唯一标识（UUID，去横线后的 32 位字符串） */
    private String taskId;

    /** 发起下载任务的用户编号 */
    private String userNum;

    /** 任务状态：pending / running / success / failed / expired */
    private String status;

    /** 待打包的文件数量 */
    private Integer fileCount;

    /** 源文件列表（换行分隔） */
    private String sourceFiles;

    /** ZIP 包在 COS 中的对象键 */
    private String zipObjectKey;

    /** ZIP 包文件名 */
    private String zipFileName;

    /** 打包失败时的错误信息 */
    private String errorMessage;

    /** 任务创建时间 */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createdTime;

    /** 任务开始打包时间（PENDING → RUNNING 时） */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startedTime;

    /** 任务完成时间（SUCCESS / FAILED 时） */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date finishedTime;

    /** ZIP 下载链接过期时间 */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date expireTime;

    /** 记录最后更新时间 */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getUserNum() {
        return userNum;
    }

    public void setUserNum(String userNum) {
        this.userNum = userNum;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getFileCount() {
        return fileCount;
    }

    public void setFileCount(Integer fileCount) {
        this.fileCount = fileCount;
    }

    public String getSourceFiles() {
        return sourceFiles;
    }

    public void setSourceFiles(String sourceFiles) {
        this.sourceFiles = sourceFiles;
    }

    public String getZipObjectKey() {
        return zipObjectKey;
    }

    public void setZipObjectKey(String zipObjectKey) {
        this.zipObjectKey = zipObjectKey;
    }

    public String getZipFileName() {
        return zipFileName;
    }

    public void setZipFileName(String zipFileName) {
        this.zipFileName = zipFileName;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Date getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Date createdTime) {
        this.createdTime = createdTime;
    }

    public Date getStartedTime() {
        return startedTime;
    }

    public void setStartedTime(Date startedTime) {
        this.startedTime = startedTime;
    }

    public Date getFinishedTime() {
        return finishedTime;
    }

    public void setFinishedTime(Date finishedTime) {
        this.finishedTime = finishedTime;
    }

    public Date getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(Date expireTime) {
        this.expireTime = expireTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
