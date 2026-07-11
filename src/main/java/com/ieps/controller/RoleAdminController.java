package com.ieps.controller;

import com.github.pagehelper.PageInfo;
import com.ieps.common.ServerResponse;
import com.ieps.pojo.Role;
import com.ieps.pojo.User;
import com.ieps.service.RoleAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by ljw
 */
@Controller
public class RoleAdminController {
    
    @Autowired
    private RoleAdminService roleAdminService;
    
    /**
     * 管理员查看角色信息列表
     * @param page
     * @param role
     * @param limit
     * @param session
     * @param userNum
     * @param roleAdminId
     * @return
     */
    @RequestMapping({"/getRoleListByAdmin", "/getRoleListByAdmin.do"})
    @ResponseBody
    public ServerResponse<PageInfo<Role>> getRoleListByAdmin(@RequestParam(value = "page", defaultValue = "1") int page, Role role,
                                             @RequestParam(value = "limit", defaultValue = "10") int limit,
                                             String userNum, int roleAdminId) {
        
        return roleAdminService.getRoleList(page, limit, userNum, roleAdminId, role);
    }
    
    /**
     * 批量删除角色
     * @param roleIds
     * @param userNum
     * @param roleAdminId
     * @param session
     * @return
     */
    @RequestMapping({"/batchRemoveRole", "/batchRemoveRole.do"})
    @ResponseBody
    public ServerResponse<String> batchRemoveRole(int[] roleIds, String userNum, int roleAdminId, HttpServletRequest request) {
        
        User user = (User) request.getAttribute(com.ieps.common.Const.REQUEST_CURRENT_USER);
    
        System.out.println(userNum + "   " +  roleAdminId);
        
        // if (!user.getUserNum().equals(userNum)) {
        //     return ServerResponse.createByErrorMessage("安全检查不通过，用户已过时或不存在！");
        // }
        
        return roleAdminService.batchRemoveRole(roleIds, roleAdminId);
    }
    
    /**
     * 根据角色id删除角色
     * @param roleId
     * @param userNum
     * @param roleAdminId
     * @param session
     * @return
     */
    @RequestMapping({"/removeRoleByRoleId", "/removeRoleByRoleId.do"})
    @ResponseBody
    public ServerResponse<String> removeRoleByRoleId(int roleId, String userNum, int roleAdminId, HttpServletRequest request) {
        
        User user = (User) request.getAttribute(com.ieps.common.Const.REQUEST_CURRENT_USER);
        
        System.out.println(userNum + "   " +  roleAdminId);
        
        // if (!user.getUserNum().equals(userNum)) {
        //     return ServerResponse.createByErrorMessage("安全检查不通过，用户已过时或不存在！");
        // }
        
        return roleAdminService.removeRoleByRoleId(roleId, roleAdminId);
    }
}
