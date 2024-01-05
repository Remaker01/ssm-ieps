package com.ieps.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by ljw
 */
public class Item implements Serializable {

    private Integer itemId;

    // 项目编号：年+月+6位数字   2018+10+595036
    private String itemNum;

    // 项目名称
    private String itemName;
    
    // 项目负责人学号
    private String leaderNum;

    // 项目负责人姓名
    private String leaderName;
    
    // 指导老师教师号
    private String tutorNum;

    // 指导老师姓名
    private String tutorName;

    // 项目状态：1：申请中；2：立项评审；3：已立项；4：立项失败；5：中期检查; 6: 待结题；7：结题评审；8：结题成功；9：结题失败
    private Integer itemStatus;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date itemDate;

    private Date createTime;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date updateTime;

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    public String getItemNum() {
        return itemNum;
    }

    public void setItemNum(String itemNum) {
        this.itemNum = itemNum;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getLeaderNum() {
        return leaderNum;
    }

    public void setLeaderNum(String leaderNum) {
        this.leaderNum = leaderNum;
    }

    public String getLeaderName() {
        return leaderName;
    }

    public void setLeaderName(String leaderName) {
        this.leaderName = leaderName;
    }

    public String getTutorNum() {
        return tutorNum;
    }

    public void setTutorNum(String tutorNum) {
        this.tutorNum = tutorNum;
    }

    public String getTutorName() {
        return tutorName;
    }

    public void setTutorName(String tutorName) {
        this.tutorName = tutorName;
    }

    public Integer getItemStatus() {
        return itemStatus;
    }

    public void setItemStatus(Integer itemStatus) {
        this.itemStatus = itemStatus;
    }

    public Date getItemDate() {
        return itemDate;
    }

    public void setItemDate(Date itemDate) {
        this.itemDate = itemDate;
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
