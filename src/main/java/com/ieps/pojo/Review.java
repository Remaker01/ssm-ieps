package com.ieps.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by ljw
 */
public class Review implements Serializable {
    
    private Integer id;
    
    // 评委职工号
    private String userNum;
    
    // 项目编号
    private String itemNum;
    
    // 分数
    private Double reviewScore;
    
    // 评审意见
    private String reviewOption;
    
    // 评审类型（0：立项申请；1：中期检查；2：结题申请）
    private Integer reviewType;
    
    // 评审级别（0：指导老师评审；1：学院评审；2：学校评审）
    private Integer reviewLevel;
    
    // 评审时间
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date reviewTime;
    
    private Date createTime;
    
    private Date updateTime;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUserNum() {
        return userNum;
    }

    public void setUserNum(String userNum) {
        this.userNum = userNum;
    }

    public String getItemNum() {
        return itemNum;
    }

    public void setItemNum(String itemNum) {
        this.itemNum = itemNum;
    }

    public double getReviewScore() {
        return reviewScore;
    }

    public void setReviewScore(double reviewScore) {
        this.reviewScore = reviewScore;
    }

    public String getReviewOption() {
        return reviewOption;
    }

    public void setReviewOption(String reviewOption) {
        this.reviewOption = reviewOption;
    }

    public Integer getReviewType() {
        return reviewType;
    }

    public void setReviewType(Integer reviewType) {
        this.reviewType = reviewType;
    }

    public Integer getReviewLevel() {
        return reviewLevel;
    }

    public void setReviewLevel(Integer reviewLevel) {
        this.reviewLevel = reviewLevel;
    }

    public Date getReviewTime() {
        return reviewTime;
    }

    public void setReviewTime(Date reviewTime) {
        this.reviewTime = reviewTime;
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
