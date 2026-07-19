package com.ieps.service;

import com.ieps.common.ServerResponse;
import com.ieps.mapper.RolePermMapper;
import com.ieps.pojo.RolePerm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by ljw
 */
@Service
public class RolePermService {
    @Autowired
    private RolePermMapper rolePermMapper;
    
    @Cacheable(value = "ieps-role-perm", key = "#roleId")
    public ServerResponse<List<RolePerm>> getMenu(Integer roleId) {
        
        List<RolePerm> rolePermList = rolePermMapper.getPerm(roleId);
    
        if (rolePermList.size() == 0) {
            return ServerResponse.createByErrorMessage("加载菜单出错，请重试！");
        }
        return ServerResponse.createBySuccess("菜单加载完成，请尽情享用！", rolePermList);
    }
}
