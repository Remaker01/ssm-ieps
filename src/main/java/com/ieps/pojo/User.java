package com.ieps.pojo;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;

/**
 * Created by ljw
 */
public class User implements Serializable {
    // 账号（学号/教师号）
    private String userNum;
    // 密码
    private String userPwd;
    // 新密码
    private String newPassword;
    // 确认密码/旧密码
    private String rePassword;
    // 用户状态：1是激活；2是禁用；
    private Integer userStatus;
    private Date createTime;
    private Date updateTime;
    // 当前用户角色
    private Integer roleId;
    // 角色集合
    private Set<Role> roleList;
    
    public User(String userNum, String userPwd,Integer userStatus, Date createTime, Date updateTime) {
        this.userNum = userNum;
        this.userPwd = userPwd;
        this.userStatus = userStatus;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }
    public User() {}

    public String getUserNum() {
        return userNum;
    }

    public void setUserNum(String userNum) {
        this.userNum = userNum;
    }

    public String getUserPwd() {
        return userPwd;
    }

    public void setUserPwd(String userPwd) {
        this.userPwd = userPwd;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getRePassword() {
        return rePassword;
    }

    public void setRePassword(String rePassword) {
        this.rePassword = rePassword;
    }

    public Integer getUserStatus() {
        return userStatus;
    }

    public void setUserStatus(Integer userStatus) {
        this.userStatus = userStatus;
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

    public Integer getRoleId() {
        return roleId;
    }

    public void setRoleId(Integer roleId) {
        this.roleId = roleId;
    }

    public Set<Role> getRoleList() {
        return roleList;
    }

    public void setRoleList(Set<Role> roleList) {
        this.roleList = roleList;
    }

}
