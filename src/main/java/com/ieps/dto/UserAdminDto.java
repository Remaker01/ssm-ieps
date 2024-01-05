package com.ieps.dto;

import com.ieps.pojo.UserInfo;

import java.util.Arrays;

/**
 * Created by ljw
 */
public class UserAdminDto extends UserInfo {
    // 登录状态
    private Integer userStatus;
    // 用户项目级别
    // 身份标识：负责人/成员/指导老师/院内评委/院内评委组长/校内评委/校内评委组长
    private int identity;
    // 男性 male
    private int male;
    // 女性 female
    private int female;
    // 学院学生总数
    private int stuNum;
    // 批量注册
    // 账号
    private String[] userNums;
    // 姓名
    private String[] userNames;

    public Integer getUserStatus() {
        return userStatus;
    }

    public void setUserStatus(Integer userStatus) {
        this.userStatus = userStatus;
    }

    public int getIdentity() {
        return identity;
    }

    public void setIdentity(int identity) {
        this.identity = identity;
    }

    public int getMale() {
        return male;
    }

    public void setMale(int male) {
        this.male = male;
    }

    public int getFemale() {
        return female;
    }

    public void setFemale(int female) {
        this.female = female;
    }

    public int getStuNum() {
        return stuNum;
    }

    public void setStuNum(int stuNum) {
        this.stuNum = stuNum;
    }

    public String[] getUserNums() {
        return userNums;
    }

    public void setUserNums(String[] userNums) {
        this.userNums = userNums;
    }

    public String[] getUserNames() {
        return userNames;
    }

    public void setUserNames(String[] userNames) {
        this.userNames = userNames;
    }

    @Override
    public String toString() {
        return "UserAdminDto{" +
                "userStatus=" + userStatus +
                ", identity=" + identity +
                ", male=" + male +
                ", female=" + female +
                ", stuNum=" + stuNum +
                ", userNums=" + Arrays.toString(userNums) +
                ", userNames=" + Arrays.toString(userNames) +
                '}';
    }
}
