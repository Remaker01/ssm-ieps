package com.ieps.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

public class FileHub {
    
    private Integer id;
    
    // 文件类型：-1：通知类文件；其他的是项目文件 1 是普通通知
    private String typeNum;

    //  上传人
    private String userNum;
    
    // 所在学院
    private String academy;

    // 文件名
    private String fileName;

    // 文件大小
    private Integer fileSize;

    // 文件类型  -1：项目文件；0：普通通知文件；1：常用下载普通文件
    private Integer fileKind;
    
    // @DatetimeFormat是将String转换成Date，一般前台给后台传值时用
    // @JsonFormat(pattern="yyyy-MM-dd") 将Date转换成String 一般后台传值给前台时
    
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss" )
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss", timezone="GMT+8")
    private Date createTime;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd" )
    @JsonFormat(pattern="yyyy-MM-dd", timezone="GMT+8")
    private Date updateTime;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTypeNum() {
        return typeNum;
    }

    public void setTypeNum(String typeNum) {
        this.typeNum = typeNum;
    }

    public String getUserNum() {
        return userNum;
    }

    public void setUserNum(String userNum) {
        this.userNum = userNum;
    }

    public String getAcademy() {
        return academy;
    }

    public void setAcademy(String academy) {
        this.academy = academy;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Integer getFileSize() {
        return fileSize;
    }

    public void setFileSize(Integer fileSize) {
        this.fileSize = fileSize;
    }

    public Integer getFileKind() {
        return fileKind;
    }

    public void setFileKind(Integer fileKind) {
        this.fileKind = fileKind;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}