package com.ieps.service;

import cn.hutool.core.bean.BeanUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import java.util.ArrayList;
import com.ieps.common.Const;
import com.ieps.common.ServerResponse;
import com.ieps.dto.UserAdminDto;
import com.ieps.mapper.RoleMapper;
import com.ieps.mapper.UserInfoMapper;
import com.ieps.mapper.UserMapper;
import com.ieps.mapper.UserRoleMapper;
import com.ieps.pojo.Role;
import com.ieps.pojo.User;
import com.ieps.pojo.UserInfo;
import com.ieps.pojo.UserRole;
import com.ieps.util.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by ljw
 */
@Service
public class UserAdminService {
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private UserInfoMapper userInfoMapper;
    
    @Autowired
    private UserRoleMapper userRoleMapper;
    
    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private StorageService storageService;
    
    /**
     * 获取用户列表
     * @param pageNum
     * @param pageSize
     * @param userNum
     * @param roleId
     * @return
     */
    public ServerResponse<PageInfo<UserAdminDto>> getAllUserList(int pageNum, int pageSize, String userNum, int roleId) {
        
        PageHelper.startPage(pageNum, pageSize);
        
        List<UserAdminDto> userAdminDtoList = new ArrayList<>();
        
        if (Const.ROLEID_COLLEGE == roleId) {
            userAdminDtoList = userMapper.selectAllUser(userNum);
        } else if (Const.ROLEID_ACADEMY == roleId) {
            userAdminDtoList = userMapper.selectAllUserByUserNum(userNum);
        }
        
        if (userAdminDtoList.size() == 0) {
            return ServerResponse.createByErrorMessage("对不起，可能暂无数据，请稍候再试！");
        }
        decorateUserImages(userAdminDtoList);
        
        PageInfo<UserAdminDto> pageInfo = new PageInfo<>(userAdminDtoList);
        
        return ServerResponse.createBySuccess(pageInfo);
    }
    
    /**
     * 根据条件搜索用户列表
     * @param pageNum
     * @param pageSize
     * @param userNum
     * @param roleId
     * @param userInfo
     * @return
     */
    public ServerResponse<PageInfo<UserAdminDto>> searchUserAdminListWithCondition(int pageNum, int pageSize, String userNum, int roleId, UserInfo userInfo) {
        PageHelper.startPage(pageNum, pageSize);
        
        List<UserAdminDto> userAdminDtoList = new ArrayList<>();
        
        if (Const.ROLEID_COLLEGE == roleId) {
            userAdminDtoList = userMapper.selectAllUserWithCondition(userNum, userInfo);
        } else if (Const.ROLEID_ACADEMY == roleId) {
            userAdminDtoList = userMapper.selectAllUserByUserNumWithCondition(userNum, userInfo);
        }
        
        if (userAdminDtoList.size() == 0) {
            return ServerResponse.createByErrorMessage("对不起，可能暂无数据，请稍候再试！");
        }
        decorateUserImages(userAdminDtoList);
        
        PageInfo<UserAdminDto> pageInfo = new PageInfo<>(userAdminDtoList);
        
        return ServerResponse.createBySuccess(pageInfo);
    }
    
    /**
     * 根据userNum修改性别
     * @param userNum
     * @param sex
     * @return
     */
    public ServerResponse<String> modifySexWithUserNum(String userNum, Integer sex) {
        if (userInfoMapper.updateSexByUserNum(userNum, sex) > 0) {
            return ServerResponse.createBySuccessMessage("修改该用户性别成功！");
        }
        
        return ServerResponse.createByErrorMessage("修改该用户性别失败，请重新尝试！");
    }
    
    /**
     * 根据userNum修改用户状态
     * @param userNum
     * @param status
     * @return
     */
    public ServerResponse<String> modifyStatusWithUserNum(String userNum, Integer status) {
        if (userMapper.updateStatusByUserNum(userNum, status) > 0) {
            return ServerResponse.createBySuccessMessage("修改该用户状态成功！");
        }
        
        return ServerResponse.createByErrorMessage("修改该用户状态失败，请重新尝试！");
    }
    
    /**
     * 修改用户信息
     * @param userInfo
     * @return
     */
    public ServerResponse<String> modifyUserByUserNum(UserInfo userInfo) {
        if (userInfoMapper.updateByUserNumSelective(userInfo) > 0) {
            return ServerResponse.createBySuccessMessage("修改用户信息成功！");
        }
        
        return ServerResponse.createByErrorMessage("修改用户信息失败，请重新尝试！");
    }
    
    /**
     * 根据userNum删除用户
     * @param userNum
     * @return
     */
    public ServerResponse<String> removeUserByUserNum(String userNum) {
        
        if (userMapper.deleteByUserNum(userNum) > 0) {
            userMapper.deleteByUserNum(userNum);
            userInfoMapper.deleteByUserNum(userNum);
            userRoleMapper.deleteUserRoleByUserNum(new String[]{userNum});
            
            return ServerResponse.createBySuccessMessage("删除用户成功！");
        }
        
        return ServerResponse.createByErrorMessage("删除用户失败，请重新尝试！");
    }
    
    /**
     * 增加用户
     * @param userAdminDto
     * @param roleId
     * @return
     */
    public ServerResponse<String> addUserAdmin(UserAdminDto userAdminDto, int roleId) {
        
        User user = new User();
        user.setUserNum(userAdminDto.getUserNum());
        user.setUserPwd(PasswordUtil.hashPassword(Const.UNIFORM_USERPWD));
        user.setUserStatus(Const.UNIFORM_STATUS);
        
        UserInfo userInfo = new UserInfo();
        BeanUtil.copyProperties(userAdminDto, userInfo);
        userInfo.setUserImg(Const.UNIFORM_USERIMG);
        
        UserRole userRole = new UserRole();
        userRole.setUserNum(userAdminDto.getUserNum());
        userRole.setRoleId(roleId);
        
        if (userMapper.insertSelective(user) > 0) {
            if (userInfoMapper.insertSelective(userInfo) > 0) {
                if (userRoleMapper.insertSelective(userRole) > 0) {
                    return ServerResponse.createBySuccessMessage("添加用户成功！");
                }
            }
        }
        
        return ServerResponse.createByErrorMessage("添加用户失败，请重新尝试！");
    }
    
    /**
     * 批量删除用户
     * @param userNums
     * @return
     */
    public ServerResponse<String> batchRemoveUser(String[] userNums) {
        if (userMapper.deleteUserByUserNum(userNums) > 0) {
            userInfoMapper.deleteUserInfoByUserNum(userNums);
            userRoleMapper.deleteUserRoleByUserNum(userNums);
            
            return ServerResponse.createBySuccessMessage("批量删除用户成功！");
        }
        
        return ServerResponse.createByErrorMessage("批量删除用户失败，请重新尝试！");
    }
    
    /**
     * 通过用户名查找用户信息
     * @param userName
     * @return
     */
    public ServerResponse<UserInfo> getUserInfoByUserName(String userName) {
        UserInfo userInfo = userInfoMapper.selectUserInfoByUserName(userName);
        if (userInfo == null) {
            return ServerResponse.createByErrorMessage("抱歉，没有相关的用户信息，请重新输入！");
        }
        
        return ServerResponse.createBySuccess(userInfo);
    }
    
    /**
     * 获取当前管理员的权限角色
     * @param roleId
     * @return
     */
    @Cacheable("ieps-role")
    public ServerResponse<List<Role>> getAllRoleIdWithRoleIdByAdmin(Integer roleId) {
        List<Role> roleList = roleMapper.selectAllRoleWithRoleAdminId(roleId);
        if (roleList.size() == 0) {
            return ServerResponse.createByErrorMessage("抱歉，暂无数据，请稍后再试！");
        }
        
        return ServerResponse.createBySuccess(roleList);
    }
    
    /**
     * 批量添加用户
     * @param userAdminDto
     * @param roleId
     * @param roleIdAdmin
     * @return
     */
    public ServerResponse<String> batchAddUser(UserAdminDto userAdminDto, Integer roleId, int roleIdAdmin) {
        
        int count = 0;
        int successCount = 0;
        int failCount = 0;
        
        // 获得所有用户编号
        String[] userNums = userAdminDto.getUserNums();
        String[] userNames = userAdminDto.getUserNames();
        
        for (int i = 0; i < userNums.length; i++) {
            
            count++;
            
            User user = new User();
            user.setUserNum(userNums[i]);
            user.setUserPwd(PasswordUtil.hashPassword(Const.UNIFORM_USERPWD));
            user.setUserStatus(Const.UNIFORM_STATUS);
            
            UserInfo userInfo = new UserInfo();
            userInfo.setUserNum(userNums[i]);
            userInfo.setUserName(userNames[i]);
            if (userAdminDto.getAcademy() != null) {
                userInfo.setAcademy(userAdminDto.getAcademy());
            }
            if (userAdminDto.getUserImg() != null) {
                userInfo.setUserImg(userAdminDto.getUserImg());
            } else {
                userInfo.setUserImg(Const.UNIFORM_USERIMG);
            }
            
            UserRole userRole = new UserRole();
            userRole.setUserNum(userNums[i]);
            userRole.setRoleId(roleId);
            
            if (userMapper.selectByUserNum(userNums[i]) != null) {
                failCount++;
                continue;
            }
            
            if (userMapper.insertSelective(user) > 0) {
                if (userInfoMapper.insertSelective(userInfo) > 0) {
                    if (userRoleMapper.insertSelective(userRole) > 0) {
                        successCount++;
                    }
                }
            }
        }
        
        return ServerResponse.createBySuccessMessage("总共需要导入" + count + "条数据，导入成功" + successCount + "条，导入失败" + failCount + "条！");
    }

    private void decorateUserImages(List<UserAdminDto> userAdminDtoList) {
        for (UserAdminDto userAdminDto : userAdminDtoList) {
            userAdminDto.setUserImg(storageService.resolveUserImageUrl(userAdminDto.getUserImg()));
        }
    }
    
}
