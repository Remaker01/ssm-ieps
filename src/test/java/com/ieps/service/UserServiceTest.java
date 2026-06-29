package com.ieps.service;

import com.ieps.common.ServerResponse;
import com.ieps.mapper.UserInfoMapper;
import com.ieps.mapper.UserMapper;
import com.ieps.mapper.UserRoleMapper;
import com.ieps.pojo.User;
import com.ieps.pojo.UserInfo;
import com.ieps.util.EncryptUtil;
import com.ieps.util.PasswordUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    private UserMapper userMapper;
    
    @Mock
    private UserInfoMapper userInfoMapper;
    
    @Mock
    private UserRoleMapper userRoleMapper;
    
    @InjectMocks
    private UserService userService;
    
    @Test
    void loginShouldReturnSuccessForHashedPassword() {
        User storedUser = new User();
        storedUser.setUserNum("20230001");
        storedUser.setUserPwd(PasswordUtil.hashPassword("Secret123"));
        storedUser.setUserStatus(1);
        
        when(userMapper.selectByUserNum("20230001")).thenReturn(storedUser);
        when(userMapper.selectRoleIdByUserNum("20230001")).thenReturn(200001);
        
        ServerResponse response = userService.login("20230001", "Secret123");
        User loginUser = (User) response.getData();
        
        assertEquals(0, response.getStatus());
        assertEquals("20230001", loginUser.getUserNum());
        assertEquals(200001, loginUser.getRoleId());
        assertNull(loginUser.getUserPwd());
    }
    
    @Test
    void loginShouldRejectLegacyAesPassword() {
        User storedUser = new User();
        storedUser.setUserNum("20230001");
        storedUser.setUserPwd(EncryptUtil.AESencode("Secret123", "123456"));
        storedUser.setUserStatus(1);
        
        when(userMapper.selectByUserNum("20230001")).thenReturn(storedUser);
        
        ServerResponse response = userService.login("20230001", "Secret123");
        
        assertEquals(1, response.getStatus());
        assertTrue(response.getMsg().contains("密码策略已升级"));
    }
    
    @Test
    void modifyPwdShouldHashNewPasswordBeforeUpdating() {
        User storedUser = new User();
        storedUser.setUserNum("20230001");
        storedUser.setUserPwd(PasswordUtil.hashPassword("OldPass123"));
        storedUser.setUserStatus(1);
        
        User changeRequest = new User();
        changeRequest.setUserNum("20230001");
        changeRequest.setUserPwd("OldPass123");
        changeRequest.setNewPassword("NewPass123");
        changeRequest.setRePassword("NewPass123");
        
        when(userMapper.selectByUserNum("20230001")).thenReturn(storedUser);
        when(userMapper.updatePwd(eq("20230001"), argThat(new PasswordHashMatcher("NewPass123")))).thenReturn(1);
        
        ServerResponse response = userService.modifyPwd(changeRequest);
        
        assertEquals(0, response.getStatus());
        verify(userMapper).updatePwd(eq("20230001"), argThat(new PasswordHashMatcher("NewPass123")));
    }
    
    @Test
    void registerShouldStorePasswordAsHash() {
        User user = new User();
        user.setUserNum("20230001");
        user.setUserPwd("Secret123!");
        user.setRePassword("Secret123!");
        
        UserInfo userInfo = new UserInfo();
        userInfo.setUserNum("20230001");
        
        when(userMapper.selectByUserNum("20230001")).thenReturn(null);
        when(userMapper.insertSelective(any(User.class))).thenReturn(1);
        when(userInfoMapper.insertSelective(any(UserInfo.class))).thenReturn(1);
        when(userRoleMapper.insertSelective(any())).thenReturn(1);
        
        ServerResponse response = userService.register(user, userInfo, 200001);
        
        assertEquals(0, response.getStatus());
        assertTrue(PasswordUtil.isPasswordHash(user.getUserPwd()));
        assertTrue(PasswordUtil.matches("Secret123!", user.getUserPwd()));
    }

    @Test
    void registerShouldRejectWeakPassword() {
        User user = new User();
        user.setUserNum("20230001");
        user.setUserPwd("12345678");
        user.setRePassword("12345678");

        UserInfo userInfo = new UserInfo();
        userInfo.setUserNum("20230001");

        ServerResponse response = userService.register(user, userInfo, 200001);

        assertEquals(1, response.getStatus());
        assertTrue(response.getMsg().contains("密码需为8-30位"));
    }

    @Test
    void forgetPwdShouldRejectWeakPassword() {
        User storedUser = new User();
        storedUser.setUserNum("20230001");

        when(userMapper.selectByUserNum("20230001")).thenReturn(storedUser);

        ServerResponse response = userService.forgetPwd("20230001", "12345678");

        assertEquals(1, response.getStatus());
        assertTrue(response.getMsg().contains("密码需为8-30位"));
    }
    
    private static class PasswordHashMatcher implements ArgumentMatcher<String> {
        
        private final String rawPassword;
        
        private PasswordHashMatcher(String rawPassword) {
            this.rawPassword = rawPassword;
        }
        
        @Override
        public boolean matches(String argument) {
            return PasswordUtil.isPasswordHash(argument) && PasswordUtil.matches(rawPassword, argument);
        }
    }
}
