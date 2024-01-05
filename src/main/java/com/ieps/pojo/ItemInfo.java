package com.ieps.pojo;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by ljw
 */
public class ItemInfo implements Serializable {
    private Integer id;

    // 项目编号
    private String itemNum;
    
    // 项目级别：-1: 立项失败；0：校级；1：省区级；2：国家级
    private Integer itemLevel;

    // 项目类型：0：创新训练；1：创业训练；2：创业实践
    private Integer itemType;

    // 项目简介，少于200个字
    private String summary;

    // 校级经费
    private Double collegeFunds;

    // 财政经费
    private Double governFunds;

    private Date createTime;

    private Date updateTime;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getItemNum() {
        return itemNum;
    }

    public void setItemNum(String itemNum) {
        this.itemNum = itemNum;
    }

    public Integer getItemLevel() {
        return itemLevel;
    }

    public void setItemLevel(Integer itemLevel) {
        this.itemLevel = itemLevel;
    }

    public Integer getItemType() {
        return itemType;
    }

    public void setItemType(Integer itemType) {
        this.itemType = itemType;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Double getCollegeFunds() {
        return collegeFunds;
    }

    public void setCollegeFunds(Double collegeFunds) {
        this.collegeFunds = collegeFunds;
    }

    public Double getGovernFunds() {
        return governFunds;
    }

    public void setGovernFunds(Double governFunds) {
        this.governFunds = governFunds;
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
