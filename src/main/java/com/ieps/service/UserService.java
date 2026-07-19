package com.ieps.service;

import com.ieps.common.Const;
import com.ieps.common.ServerResponse;
import com.ieps.mapper.UserInfoMapper;
import com.ieps.mapper.UserMapper;
import com.ieps.mapper.UserRoleMapper;
import com.ieps.pojo.User;
import com.ieps.pojo.UserInfo;
import com.ieps.pojo.UserRole;
import com.ieps.util.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Created by ljw
 */
@Service
public class UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private UserInfoMapper userInfoMapper;
    
    @Autowired
    private UserRoleMapper userRoleMapper;
    
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    
    public ServerResponse<User> login(String userNum, String userPwd) {
        User storedUser = userMapper.selectByUserNum(userNum);
        if (storedUser == null) {
            return ServerResponse.createByErrorMessage("登录失败，请重新登录！");
        }
        
        if (storedUser.getUserStatus() == 0) {
            return ServerResponse.createByErrorMessage("对不起，该账号目前已经被锁定，请联系管理员激活！");
        }
        
        if (!PasswordUtil.isPasswordHash(storedUser.getUserPwd())) {
            return ServerResponse.createByErrorMessage("密码策略已升级，请先重置密码后再登录！");
        }
        
        if (!PasswordUtil.matches(userPwd, storedUser.getUserPwd())) {
            return ServerResponse.createByErrorMessage("登录失败，请重新登录！");
        }
        
        Integer roleId = userMapper.selectRoleIdByUserNum(userNum);
        if (roleId == null) {
            return ServerResponse.createByErrorMessage("登录失败，请联系管理员检查角色配置！");
        }
        
        User loginUser = new User();
        loginUser.setUserNum(storedUser.getUserNum());
        loginUser.setUserStatus(storedUser.getUserStatus());
        loginUser.setRoleId(roleId);
        
        return ServerResponse.createBySuccess("登录成功，请尽情享用！", loginUser);
    }
    
    public ServerResponse<User> getMenu(String userNum) {
        User user = userMapper.selectMenu(userNum);
        if (user == null) {
            return ServerResponse.createByErrorMessage("连接超时，请重新登录！");
        }
        return ServerResponse.createBySuccess("加载菜单完成，请尽情享受！", user);
    }
    
    // 注册
    public ServerResponse<String> register(User user, UserInfo userInfo, Integer roleId) {
        if (!user.getUserPwd().equals(user.getRePassword())) {
            return ServerResponse.createByErrorMessage("前后两次密码不一致，请重新输入！");
        }

        String passwordPolicyError = PasswordUtil.validatePasswordPolicy(user.getUserPwd());
        if (passwordPolicyError != null) {
            return ServerResponse.createByErrorMessage(passwordPolicyError);
        }
        
        if (userMapper.selectByUserNum(user.getUserNum()) != null) {
            return ServerResponse.createByErrorMessage("账号已注册，请重新输入！");
        }
        
        user.setUserPwd(PasswordUtil.hashPassword(user.getUserPwd()));
        
        int result = userMapper.insertSelective(user);
        if (result < 1) {
            return ServerResponse.createByErrorMessage("插入User用户表失败");
        }
    
        result = userInfoMapper.insertSelective(userInfo);
        if (result < 1) {
            return ServerResponse.createByErrorMessage("插入UserInfo用户信息表失败");
        }
        
        UserRole userRole = new UserRole();
        userRole.setRoleId(roleId);
        userRole.setUserNum(user.getUserNum());
        result = userRoleMapper.insertSelective(userRole);
        if (result < 1) {
            return ServerResponse.createByErrorMessage("插入UserRole用户信息表失败");
        }
        
        return ServerResponse.createBySuccessMessage("注册" + user.getUserNum() + "账号成功，返回首页！");
    }
    
    public ServerResponse<String> checkUserPwdWithUserNum(String userNum, String userPwd) {
        User user = userMapper.selectByUserNum(userNum);
        if (user == null) {
            return ServerResponse.createByErrorMessage("用户名与密码不匹配,请重新输入！");
        }
        
        if (!PasswordUtil.isPasswordHash(user.getUserPwd())) {
            return ServerResponse.createByErrorMessage("密码策略已升级，请先重置密码后再继续！");
        }
        
        if (!PasswordUtil.matches(userPwd, user.getUserPwd())) {
            return ServerResponse.createByErrorMessage("用户名与密码不匹配,请重新输入！");
        }
        
        return ServerResponse.createBySuccessMessage("用户名与密码匹配，请继续！");
    }
    
    
    public ServerResponse<String> forgetPwd(String userNum, String userPwd, String forgetPwdToken) {
        if (forgetPwdToken == null || forgetPwdToken.trim().isEmpty()) {
            return ServerResponse.createByErrorMessage("请先完成验证码校验后再重置密码！");
        }

        // 从 Redis 校验 forgetPwdToken
        String tokenKey = Const.REDIS_FORGET_PWD_TOKEN_PREFIX + userNum;
        String storedToken = stringRedisTemplate.opsForValue().get(tokenKey);
        if (storedToken == null || !storedToken.equals(forgetPwdToken.trim())) {
            return ServerResponse.createByErrorMessage("验证码校验已失效，请重新获取验证码！");
        }

        User user = userMapper.selectByUserNum(userNum);
        if (user == null) {
            stringRedisTemplate.delete(tokenKey);
            return ServerResponse.createByErrorMessage("用户不存在，请重新填写！");
        }

        String passwordPolicyError = PasswordUtil.validatePasswordPolicy(userPwd);
        if (passwordPolicyError != null) {
            return ServerResponse.createByErrorMessage(passwordPolicyError);
        }
        
        if (userMapper.updatePwd(userNum, PasswordUtil.hashPassword(userPwd)) < 1) {
            return ServerResponse.createByErrorMessage("重置密码失败，请重新填写！");
        }

        // 清除已使用的 token
        stringRedisTemplate.delete(tokenKey);
        
        return ServerResponse.createBySuccessMessage("重置密码成功，请使用新密码重新登录！");
    }
    
    public ServerResponse<String> modifyPwd(User user) {
        User storedUser = userMapper.selectByUserNum(user.getUserNum());
        if (storedUser == null) {
            return ServerResponse.createByErrorMessage("账号与旧密码不匹配，请重新填写！");
        }
        
        if (!PasswordUtil.isPasswordHash(storedUser.getUserPwd())) {
            return ServerResponse.createByErrorMessage("密码策略已升级，请先重置密码后再修改密码！");
        }
        
        if (!PasswordUtil.matches(user.getUserPwd(), storedUser.getUserPwd())) {
            return ServerResponse.createByErrorMessage("账号与旧密码不匹配，请重新填写！");
        }
        
        if (!user.getNewPassword().equals(user.getRePassword())) {
            return ServerResponse.createByErrorMessage("前后两次密码不一致，请重新填写！");
        }

        String passwordPolicyError = PasswordUtil.validatePasswordPolicy(user.getNewPassword());
        if (passwordPolicyError != null) {
            return ServerResponse.createByErrorMessage(passwordPolicyError);
        }
        
        if (userMapper.updatePwd(user.getUserNum(), PasswordUtil.hashPassword(user.getNewPassword())) < 1) {
            return ServerResponse.createByErrorMessage("修改密码失败，请重新填写！");
        }
        
        return ServerResponse.createBySuccessMessage("修改密码成功，请重新登录！");
    }
    
    public ServerResponse<?> checkUser(String userNum) {
        User user = userMapper.selectByUserNum(userNum);
        if (user == null) {
            return ServerResponse.createBySuccess();
        }
        
        return ServerResponse.createByErrorMessage("用户已存在，请重新填写账号！");
    }
    
    public ServerResponse<String> getUserPwdWithUserNum(String userNum) {
        return ServerResponse.createByErrorMessage("出于安全考虑，系统不再提供密码读取接口。");
    }

    /**
     * 根据用户编号获取用户基本信息（不含密码）
     * 用于 Token 刷新时重建 User 对象
     */
    public User getUserByUserNum(String userNum) {
        User user = userMapper.selectByUserNum(userNum);
        if (user == null) return null;
        User result = new User();
        result.setUserNum(user.getUserNum());
        result.setUserStatus(user.getUserStatus());
        Integer roleId = userMapper.selectRoleIdByUserNum(userNum);
        result.setRoleId(roleId);
        return result;
    }

    // ======================== Refresh Token 管理 ========================

    private static final String REDIS_REFRESH_PREFIX = "ieps:refresh:";

    /**
     * 存储 Refresh Token 到 Redis（以 jti 为 key）
     */
    public void storeRefreshToken(String userNum, String jti, long ttlSeconds) {
        String key = REDIS_REFRESH_PREFIX + jti;
        stringRedisTemplate.opsForValue().set(key, userNum, ttlSeconds, TimeUnit.SECONDS);
    }

    /**
     * 校验 Refresh Token 在 Redis 中是否有效
     * @return userNum 有效则返回用户编号，无效返回 null
     */
    public String validateRefreshTokenInRedis(String jti) {
        String key = REDIS_REFRESH_PREFIX + jti;
        return stringRedisTemplate.opsForValue().get(key);
    }

    /**
     * 删除 Refresh Token（退出登录 / token 轮换时调用）
     */
    public void revokeRefreshToken(String jti) {
        String key = REDIS_REFRESH_PREFIX + jti;
        stringRedisTemplate.delete(key);
    }
    
}
