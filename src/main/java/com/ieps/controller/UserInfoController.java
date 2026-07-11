package com.ieps.controller;

import com.ieps.common.ServerResponse;
import com.ieps.pojo.User;
import com.ieps.pojo.UserInfo;
import com.ieps.service.CosStorageService;
import com.ieps.service.StoragePathService;
import com.ieps.service.UserInfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
/**
 * Created by ljw
 */
@Controller
public class UserInfoController {

    private static final Logger logger = LoggerFactory.getLogger(UserInfoController.class);

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private StoragePathService storagePathService;

    @Autowired
    private CosStorageService cosStorageService;

    private String resolveCurrentUserNum(HttpServletRequest request, String fallbackUserNum) {
        User user = (User) request.getAttribute(com.ieps.common.Const.REQUEST_CURRENT_USER);
        if (user != null && user.getUserNum() != null && !user.getUserNum().trim().isEmpty()) {
            return user.getUserNum();
        }
        return fallbackUserNum;
    }
    
    @RequestMapping({"/checkVerifyNum", "/checkVerifyNum.do"})
    @ResponseBody
    public ServerResponse<?> checkVerifyNum(String userNum, String verifyNum) {
        return userInfoService.checkVerifyNum(userNum, verifyNum);
    }
    
    @RequestMapping(value = {"/getVerifyCode", "/getVerifyCode.do"}, method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> getVerifyCode(String userNum, String verifyNum) {
        return userInfoService.getVerifyCode(userNum, verifyNum);
    }

    @RequestMapping(value = {"/checkVerifyCode", "/checkVerifyCode.do"}, method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> checkVerifyCode(String userNum, String verifyNum, String verifyCode) {
        return userInfoService.checkVerifyCode(userNum, verifyNum, verifyCode);
    }
    
    @RequestMapping({"/getUserInfo", "/getUserInfo.do"})
    @ResponseBody
    public ServerResponse<UserInfo> getUserInfo(String userNum, HttpServletRequest request) {
        String effectiveUserNum = resolveCurrentUserNum(request, userNum);
        if (effectiveUserNum == null || effectiveUserNum.trim().isEmpty()) {
            return ServerResponse.createByErrorMessage("登录状态已失效，请重新登录后再试！");
        }
        
        /*if (!user.getUserNum().equals(userNum)) {
            return ServerResponse.createByErrorMessage("安全检查不通过，用户已过时或不存在！");
        }*/
        
        return userInfoService.findByUserNum(effectiveUserNum);
    }
    
    @RequestMapping(value = {"/modifyUserInfo", "/modifyUserInfo.do"}, method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> modifyUserInfo(UserInfo userInfo, HttpServletRequest request) {
        String effectiveUserNum = resolveCurrentUserNum(request, userInfo.getUserNum());
        if (effectiveUserNum == null || effectiveUserNum.trim().isEmpty()) {
            return ServerResponse.createByErrorMessage("登录状态已失效，请重新登录后再试！");
        }
        userInfo.setUserNum(effectiveUserNum);
        
       /* if (!user.getUserNum().equals(userInfo.getUserNum())) {
            return ServerResponse.createByErrorMessage("安全检查不通过，用户已过时或不存在！");
        }*/
        
        return userInfoService.modifyUserInfo(userInfo);
    }
    
    
    @RequestMapping(value = {"/changeUserImg", "/changeUserImg.do"}, method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<?> changeUserImg(@RequestParam("file") MultipartFile file, String userNum,
                                        HttpServletRequest request) {
        String effectiveUserNum = resolveCurrentUserNum(request, userNum);
        if (effectiveUserNum == null || effectiveUserNum.trim().isEmpty()) {
            return ServerResponse.createByErrorMessage("登录状态已失效，请重新登录后再试！");
        }
    
        // if (!user.getUserNum().equals(userNum)) {
        //     return ServerResponse.createByErrorMessage("安全检查不通过，用户已过时或不存在！");
        // }
    
        if (file.isEmpty()) {
            logger.warn("Reject avatar upload because file was empty. userNum={}", effectiveUserNum);
            return ServerResponse.createByErrorMessage("上传文件为空，请检查配置！");
        }

        String fileName = file.getOriginalFilename();
        logger.info("Start avatar upload. userNum={}, originalFileName={}, size={}",
                effectiveUserNum, fileName, file.getSize());

        try {
            String objectKey = storagePathService.buildAvatarObjectKey(effectiveUserNum, fileName);
            cosStorageService.uploadMultipartFile(file, objectKey);

            UserInfo userInfo = new UserInfo();
            userInfo.setUserNum(effectiveUserNum);
            userInfo.setUserImg(objectKey);

            ServerResponse<?> response = userInfoService.modifyUserInfo(userInfo);
            if (response.getStatus() != 0) {
                logger.warn("Avatar upload persisted to COS but failed to update user profile. userNum={}, objectKey={}, message={}",
                        effectiveUserNum, objectKey, response.getMsg());
                return response;
            }

            logger.info("Avatar upload succeeded. userNum={}, originalFileName={}, objectKey={}",
                    effectiveUserNum, fileName, objectKey);
            return ServerResponse.createBySuccess("上传" + fileName + "成功！", objectKey);
        } catch (Exception e) {
            logger.error("Avatar upload failed. userNum={}, originalFileName={}", effectiveUserNum, fileName, e);
            return ServerResponse.createByErrorMessage("文件" + fileName + "上传异常，请重试！");
        }
    }
    
    
    @RequestMapping(value = {"/getUserInfoWithItemNum", "/getUserInfoWithItemNum.do"}, method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<?> getUserInfoWithItemNum(String itemNum, HttpServletRequest request) {
        User user = (User) request.getAttribute(com.ieps.common.Const.REQUEST_CURRENT_USER);
        
       /* if (!user.getUserNum().equals(userInfo.getUserNum())) {
            return ServerResponse.createByErrorMessage("安全检查不通过，用户已过时或不存在！");
        }*/
        
        return userInfoService.getUserInfoWithItemNum(itemNum);
    }
    
}
