package com.ieps.common;

/**
 * Created by ljw
 */
public class Const {
    
    // 统一注册头像
    public final static String UNIFORM_USERIMG = "/static/images/default.jpg";
    
    // 学校管理员  学校名称
    public final static String ACADEMY_COLLEGE = "桂林电子科技大学";
    
    //
    public final static String USERNUM_COLLEGE = "100001";
    
    // 学校管理员角色id
    public final static int ROLEID_COLLEGE = 200006;
    
    // 校内专家角色id
    public final static int ROLEID_COLLEGE_EXPERT = 200005;
    
    // 学院管理员角色id
    public final static int ROLEID_ACADEMY = 200004;
    
    // 院内专家角色id
    public final static int ROLEID_ACADEMY_EXPERT = 200003;
    
    // 指导老师角色id
    public final static int ROLEID_TUTOR = 200002;
    
    // 学生角色id
    public final static int ROLEID_STU = 200001;
    
    // 统一注册密码/重置密码
    public final static String UNIFORM_USERPWD = "Ieps@123";
    
    // 统一注册状态
    public final static int UNIFORM_STATUS = 1;

    // Request 属性 Key（JWT 认证后当前用户）
    public final static String REQUEST_CURRENT_USER = "currentUser";

    // 忘记密码验证码校验通过后的 Redis Key 前缀
    public final static String REDIS_FORGET_PWD_TOKEN_PREFIX = "ieps:forgetPwd:token:";

    // 忘记密码验证码校验通过后 Token 有效期（分钟）
    public final static long FORGET_PWD_VERIFIED_TIMEOUT_MINUTES = 10L;
    
    // 文件类型 file_kind  0：普通文件；1：常用下载文件；2：项目文件；
    public final static int ZERO_FILE_KIND = 0;
    public final static int FIRST_FILE_KIND = 1;
    public final static int SECOND_FILE_KIND = 2;
    public final static int THIRD_FILE_KIND = 3;
    public final static int FOURTH_FILE_KIND = 4;
    public final static int FIFTH_FILE_KIND = 5;
    
    // 结题成功状态
    // public final static int FINISH_ITEMSTATUS = 8;
    public final static String FINISH_ITEMSTATUS = "8";
    
}
