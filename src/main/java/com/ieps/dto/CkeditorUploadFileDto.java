package com.ieps.dto;

import java.io.Serializable;

/**
 * Created by ljw
 */
public class CkeditorUploadFileDto implements Serializable {
    // 状态码
    private Integer uploaded;
    // 文件名
    private String fileName;
    // 上传服务器的地址URL
    private String url;

    public Integer getUploaded() {
        return uploaded;
    }

    public CkeditorUploadFileDto setUploaded(Integer uploaded) {
        this.uploaded = uploaded;
        return this;
    }

    public String getFileName() {
        return fileName;
    }

    public CkeditorUploadFileDto setFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public CkeditorUploadFileDto setUrl(String url) {
        this.url = url;
        return this;
    }
}
