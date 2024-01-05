package com.ieps.dto;

import com.ieps.pojo.Item;

import java.math.BigDecimal;

/**
 * Created by ljw
 */
public class ItemAdminDto extends Item {
    // 项目级别：2：校级；3：省区级；4：国家级 默认： 1  无
    private int itemLevel;
    // 项目类型：1：创新训练；2：创业训练；3：创业实践   默认：1
    private int itemType;
    // 项目简介，少于200个字
    private String summary;
    // 校级经费
    private BigDecimal collegeFunds;
    // 财政经费
    private BigDecimal governFunds;
    // 附件名
    private String fileName;
    // 身份标识：负责人/成员/指导老师/院内评委/院内评委组长/校内评委/校内评委组长
    private int identity;
    // 院级得分
    private float academyScore;
    // 校级得分
    private float collegeScore;
    // 省区级得分
    private float governScore;
    // 查询的年份
    private String itemYear;
    // 评审级别： 1：院级评审；2：校级评审；3：省区级评审；4：国家级评审
    private int reviewLevel;

    public int getItemLevel() {
        return itemLevel;
    }

    public void setItemLevel(int itemLevel) {
        this.itemLevel = itemLevel;
    }

    public int getItemType() {
        return itemType;
    }

    public void setItemType(int itemType) {
        this.itemType = itemType;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public BigDecimal getCollegeFunds() {
        return collegeFunds;
    }

    public void setCollegeFunds(BigDecimal collegeFunds) {
        this.collegeFunds = collegeFunds;
    }

    public BigDecimal getGovernFunds() {
        return governFunds;
    }

    public void setGovernFunds(BigDecimal governFunds) {
        this.governFunds = governFunds;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getIdentity() {
        return identity;
    }

    public void setIdentity(int identity) {
        this.identity = identity;
    }

    public float getAcademyScore() {
        return academyScore;
    }

    public void setAcademyScore(float academyScore) {
        this.academyScore = academyScore;
    }

    public float getCollegeScore() {
        return collegeScore;
    }

    public void setCollegeScore(float collegeScore) {
        this.collegeScore = collegeScore;
    }

    public float getGovernScore() {
        return governScore;
    }

    public void setGovernScore(float governScore) {
        this.governScore = governScore;
    }

    public String getItemYear() {
        return itemYear;
    }

    public void setItemYear(String itemYear) {
        this.itemYear = itemYear;
    }

    public int getReviewLevel() {
        return reviewLevel;
    }

    public void setReviewLevel(int reviewLevel) {
        this.reviewLevel = reviewLevel;
    }

    @Override
    public String toString() {
        return "ItemAdminDto{" +
                "itemLevel=" + itemLevel +
                ", itemType=" + itemType +
                ", summary='" + summary + '\'' +
                ", collegeFunds=" + collegeFunds +
                ", governFunds=" + governFunds +
                ", fileName='" + fileName + '\'' +
                ", identity=" + identity +
                ", academyScore=" + academyScore +
                ", collegeScore=" + collegeScore +
                ", governScore=" + governScore +
                ", itemYear='" + itemYear + '\'' +
                ", reviewLevel=" + reviewLevel +
                '}';
    }
}
