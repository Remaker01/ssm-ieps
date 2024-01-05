package com.ieps.pojo;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Created by ljw
 */
public class RolePerm implements Serializable{

    private Integer id;

    // 角色Id
    private Integer roleId;

    // 权限Id
    private Integer permId;
    
    // 权限集合
    private List<Perm> permList;

    private Date createTime;

    private Date updateTime;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getRoleId() {
        return roleId;
    }

    public void setRoleId(Integer roleId) {
        this.roleId = roleId;
    }

    public Integer getPermId() {
        return permId;
    }

    public void setPermId(Integer permId) {
        this.permId = permId;
    }

    public List<Perm> getPermList() {
        return permList;
    }

    public void setPermList(List<Perm> permList) {
        this.permList = permList;
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

    public RolePerm(Integer id, Integer roleId, Integer permId, List<Perm> permList, Date createTime, Date updateTime) {
        this.id = id;
        this.roleId = roleId;
        this.permId = permId;
        this.permList = permList;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }
}
