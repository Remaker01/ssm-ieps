package com.ieps.pojo;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by ljw
 */
public class UserItem implements Serializable {
    private Integer id;
    // 用户userNum
    private String userNum;
    // 项目编号
    private String itemNum;
    // 身份标识
    // 1：成员/2：负责人/3：指导老师/4：院内评委/5：院内评委组长/6：校内评委/7：校内评委组长
    private Integer identify;
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

    public Integer getIdentify() {
        return identify;
    }

    public void setIdentify(Integer identify) {
        this.identify = identify;
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
