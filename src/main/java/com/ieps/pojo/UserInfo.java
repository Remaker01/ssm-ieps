package com.ieps.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by ljw
 */
public class UserInfo implements Serializable {
    private Integer id;
    // 用户Id
    private String userNum;
    // 用户姓名
    private String userName;
    // 用户头像
    private String userImg;
    // 手机号码
    private String photoNum;
    // 邮箱
    private String email;
    // 职称：0：学生；1：助理研究员；2：讲师；3：高级实验师；4：副教授；5：教授
    private Integer title;
    // 性别
    private Integer sex;
    // 学院
    private String academy;
    // 年级
    private String grade;
    // 专业
    private String major;
    // 出生日期
    @JsonFormat(pattern="yyyy-MM-dd", timezone="GMT+8")
    @DateTimeFormat(pattern="yyyy-MM-dd")
    private Date birthDate;

    private Date createTime;

    private Date updateTime;

    public Integer getId() {
        return id;
    }

    public UserInfo setId(Integer id) {
        this.id = id;
        return this;
    }

    public String getUserNum() {
        return userNum;
    }

    public UserInfo setUserNum(String userNum) {
        this.userNum = userNum;
        return this;
    }

    public String getUserName() {
        return userName;
    }

    public UserInfo setUserName(String userName) {
        this.userName = userName;
        return this;
    }

    public String getUserImg() {
        return userImg;
    }

    public UserInfo setUserImg(String userImg) {
        this.userImg = userImg;
        return this;
    }

    public String getPhotoNum() {
        return photoNum;
    }

    public UserInfo setPhotoNum(String photoNum) {
        this.photoNum = photoNum;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public UserInfo setEmail(String email) {
        this.email = email;
        return this;
    }

    public Integer getTitle() {
        return title;
    }

    public void setTitle(Integer title) {
        this.title = title;
    }

    public Integer getSex() {
        return sex;
    }

    public UserInfo setSex(Integer sex) {
        this.sex = sex;
        return this;
    }

    public String getAcademy() {
        return academy;
    }

    public UserInfo setAcademy(String academy) {
        this.academy = academy;
        return this;
    }

    public String getGrade() {
        return grade;
    }

    public UserInfo setGrade(String grade) {
        this.grade = grade;
        return this;
    }

    public String getMajor() {
        return major;
    }

    public UserInfo setMajor(String major) {
        this.major = major;
        return this;
    }

    public Date getBirthDate() {
        return birthDate;
    }

    public UserInfo setBirthDate(Date birthDate) {
        this.birthDate = birthDate;
        return this;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public UserInfo setCreateTime(Date createTime) {
        this.createTime = createTime;
        return this;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public UserInfo setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
        return this;
    }
}
