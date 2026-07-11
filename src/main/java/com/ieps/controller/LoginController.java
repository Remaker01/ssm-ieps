package com.ieps.controller;

import com.ieps.common.Const;
import com.ieps.common.ServerResponse;
import com.ieps.pojo.RolePerm;
import com.ieps.pojo.User;
import com.ieps.pojo.UserInfo;
import com.ieps.service.RolePermService;
import com.ieps.service.UserService;
import com.ieps.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * Created by ljw
 */
@Controller
public class LoginController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private RolePermService rolePermService;

    @Autowired
    private JwtUtil jwtUtil;
    
    /**
     * 根路径默认进入公开首页
     * @return
     */
    @RequestMapping(value = {"/", "/home", "/goHome.do"}, method = RequestMethod.GET)
    public String root() {
        return "home";
    }

    /**
     * 通过userNum和userPwd登录系统
     * @param userNum
     * @param userPwd
     * @return JWT token
     */
    @RequestMapping(value = {"/login", "/login.do"}, method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<?> login(String userNum, String userPwd) {
   
        ServerResponse<User> response = userService.login(userNum, userPwd);
        
        if (response.isSuccess()) {
            // 生成 JWT Token 并返回
            User loginUser = response.getData();
            String token = jwtUtil.generateToken(loginUser);
            return ServerResponse.createBySuccess("登录成功，请尽情享用！", token);
        }
        
        return response;
    }
    
    /**
     * 根据roleId获取菜单
     * @param userNum
     * @param roleId
     * @param request
     * @return
     */
    @RequestMapping(value = {"/getMenu", "/getMenu.do"}, method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<?> getMenu(String userNum, Integer roleId, HttpServletRequest request) {
        User user = (User) request.getAttribute(Const.REQUEST_CURRENT_USER);
        Integer effectiveRoleId = roleId;
        if (user != null && user.getRoleId() != null) {
            effectiveRoleId = user.getRoleId();
        }
        if (effectiveRoleId == null) {
            return ServerResponse.createByErrorMessage("无法识别当前用户角色，请重新登录后再试！");
        }
    
        // if (user != null && !user.getUserNum().equals(userNum)) {
        //     return ServerResponse.createByErrorMessage("安全检查不通过，用户已过时或不存在！");
        // }
        
        ServerResponse<List<RolePerm>> perm = rolePermService.getMenu(effectiveRoleId);
        
        if (perm.getStatus() != 0) {
            return ServerResponse.createByErrorMessage(perm.getMsg());
        }
        
        return ServerResponse.createBySuccess(perm.getMsg(), perm.getData().get(0).getPermList());
    }
    
    /**
     * 注册用户信息
     * @param user
     * @param userInfo
     * @param roleId
     * @return
     */
    @RequestMapping(value = {"/register", "/register.do"}, method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> register(User user, UserInfo userInfo, int roleId) {
        return userService.register(user, userInfo, roleId);
    }
    
    /**
     * 根据userNum检查用户原始密码是否正确
     * @param userNum
     * @param userPwd
     * @param request
     * @return
     */
    @RequestMapping(value = {"/checkUserPwdWithUserNum", "/checkUserPwdWithUserNum.do"}, method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> checkUserPwdWithUserNum(String userNum, String userPwd, HttpServletRequest request) {
        User activeUser = (User) request.getAttribute(Const.REQUEST_CURRENT_USER);
        if (activeUser == null) {
            return ServerResponse.createByErrorMessage("登录状态已失效，请重新登录后再试！");
        }

        return userService.checkUserPwdWithUserNum(activeUser.getUserNum(), userPwd);
    }
    
    /**
     * 退出系统（无状态 JWT，前端清除 token 即可）
     * @return
     */
    @RequestMapping(value = {"/logout", "/logout.do"}, method = RequestMethod.GET)
    public String logout() {
        return "redirect:/login";
    }

    /**
     * 进入系统首页
     * @return
     */
    @RequestMapping(value = {"/index", "/goIndex.do"}, method = RequestMethod.GET)
    public String goIndex() {
        return "common/index";
    }
    
    /**
     * 登录页面
     * @return
     */
    @RequestMapping(value = {"/login", "/goLogin.do"}, method = RequestMethod.GET)
    public String goLogin() {
        return "common/login";
    }
    
    /**
     * 注册页面
     * @return
     */
    @RequestMapping(value = {"/register", "/goRegister.do"}, method = RequestMethod.GET)
    public String goRegister() {
        return "common/register";
    }
    
    /**
     * 忘记密码页面
     * @return
     */
    @RequestMapping(value = {"/forget-password", "/goForgetPwd.do"}, method = RequestMethod.GET)
    public String goForgotPwd() {
        return "common/forgetPwd";
    }
    //            首页
    // 重要通知详情
    @RequestMapping(value = {"/informs/detail", "/goShowInformDetail.do", "/goShowInfoemDetail.do"}, method = RequestMethod.GET)
    public String goShowInformDetail() {
        return "show/showInformDetail";
    }
    
    // 重要通知详情
    @RequestMapping(value = {"/informs", "/goShowInformMore.do"}, method = RequestMethod.GET)
    public String goShowInformMore() {
        return "show/showInformMore";
    }

    @RequestMapping(value = {"/items", "/goShowItemMore.do"}, method = RequestMethod.GET)
    public String goShowItemMore() {
        return "show/showItemMore";
    }
    
    @RequestMapping(value = {"/items/detail", "/goShowItemDetail.do"}, method = RequestMethod.GET)
    public String goShowItemDetail() {
        return "show/showItemDetail";
    }
    
    @RequestMapping(value = {"/downloads", "/goShowDownLoadMore.do"}, method = RequestMethod.GET)
    public String goShowDownLoadMore() {
        return "show/showDownLoadMore";
    }
}
