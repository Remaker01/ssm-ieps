package com.ieps.pojo;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;

/**
 * Created by ljw
 */
public class Role implements Serializable {

    private Integer roleId;

    // 角色名
    private String roleName;

    // 角色描述
    private String roleDesc;

    private Date createTime;

    private Date updateTime;
    
    // 权限集合
    private Set<Perm> permList;

    public Integer getRoleId() {
        return roleId;
    }

    public void setRoleId(Integer roleId) {
        this.roleId = roleId;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getRoleDesc() {
        return roleDesc;
    }

    public void setRoleDesc(String roleDesc) {
        this.roleDesc = roleDesc;
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

    public Set<Perm> getPermList() {
        return permList;
    }

    public void setPermList(Set<Perm> permList) {
        this.permList = permList;
    }
}
