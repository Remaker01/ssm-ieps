package com.ieps.controller;

import com.ieps.common.Const;
import com.ieps.common.ServerResponse;
import com.ieps.pojo.User;
import com.ieps.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by ljw
 */
@Controller
public class UserController {

    @Autowired
    private UserService userService;
    
    @RequestMapping(value = {"/forget-password", "/forgetPwd", "/forgetPwd.do"}, method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse forgetPwd(String userNum, String userPwd, String forgetPwdToken) {
        return userService.forgetPwd(userNum, userPwd, forgetPwdToken);
    }
    
    @RequestMapping(value = {"/modifyPwd", "/modifyPwd.do"}, method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse modifyPwd(User user, HttpServletRequest request) {
        User currentUser = (User) request.getAttribute(Const.REQUEST_CURRENT_USER);
        if (currentUser == null) {
            return ServerResponse.createByErrorMessage("登录状态已失效，请重新登录后再试！");
        }

        user.setUserNum(currentUser.getUserNum());
        return userService.modifyPwd(user);
    }
    
    @RequestMapping(value = {"/checkUser", "/checkUser.do"}, method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse checkUser(String userNum) {
        return userService.checkUser(userNum);
    }
    
    @RequestMapping(value = {"/getUserPwdWithUserNum", "/getUserPwdWithUserNum.do"}, method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse getUserPwdWithUserNum(String userNum) {
        return userService.getUserPwdWithUserNum(userNum);
    }
    
    
    
    
    
    
    
    
}
