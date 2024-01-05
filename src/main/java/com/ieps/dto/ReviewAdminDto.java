package com.ieps.dto;

import com.ieps.pojo.Review;

/**
 * Created by ljw
 */
public class ReviewAdminDto extends Review {
    // 评审人姓名
    private String userName;
    // 评审类型
    private String reviewAdminType;
    // 评审级别
    private String reviewAdminLevel;
    // 评审文件名
    private String fileName;
    // 上传的文件类型
    private Integer fileKind;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getReviewAdminType() {
        return reviewAdminType;
    }

    public void setReviewAdminType(String reviewAdminType) {
        this.reviewAdminType = reviewAdminType;
    }

    public String getReviewAdminLevel() {
        return reviewAdminLevel;
    }

    public void setReviewAdminLevel(String reviewAdminLevel) {
        this.reviewAdminLevel = reviewAdminLevel;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Integer getFileKind() {
        return fileKind;
    }

    public void setFileKind(Integer fileKind) {
        this.fileKind = fileKind;
    }

    @Override
    public String toString() {
        return "ReviewAdminDto{" +
                "userName='" + userName + '\'' +
                ", reviewAdminType='" + reviewAdminType + '\'' +
                ", reviewAdminLevel='" + reviewAdminLevel + '\'' +
                ", fileName='" + fileName + '\'' +
                ", fileKind=" + fileKind +
                '}';
    }
}
