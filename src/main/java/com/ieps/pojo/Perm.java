package com.ieps.pojo;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by ljw
 */
public class Perm implements Serializable {

    private Integer permId;

    // 权限名
    private String permName;
    
    // 权限类型：0：menu；1：permission
    private String permType;

    // url
    private String url;

    // 图标
    private String icon;
    
    // 父菜单id
    private Integer parentId;

    // 具体权限
    private String permCode;

    // 权限描述
    private String permDesc;

    private Date createTime;

    private Date updateTime;

    public Integer getPermId() {
        return permId;
    }

    public void setPermId(Integer permId) {
        this.permId = permId;
    }

    public String getPermName() {
        return permName;
    }

    public void setPermName(String permName) {
        this.permName = permName;
    }

    public String getPermType() {
        return permType;
    }

    public void setPermType(String permType) {
        this.permType = permType;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Integer getParentId() {
        return parentId;
    }

    public void setParentId(Integer parentId) {
        this.parentId = parentId;
    }

    public String getPermCode() {
        return permCode;
    }

    public void setPermCode(String permCode) {
        this.permCode = permCode;
    }

    public String getPermDesc() {
        return permDesc;
    }

    public void setPermDesc(String permDesc) {
        this.permDesc = permDesc;
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
    // private List<Role> roleList;
    
}
