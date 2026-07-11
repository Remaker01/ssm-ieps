package com.ieps.service;

import com.ieps.common.Const;
import com.ieps.common.ServerResponse;
import com.ieps.config.IepsRedisProperties;
import com.ieps.dto.UserAdminDto;
import com.ieps.mapper.UserInfoMapper;
import com.ieps.pojo.UserInfo;
import com.ieps.util.MailUtil;
import com.ieps.util.miaodiyun.IndustrySMS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by ljw
 */
@Service
public class UserInfoService {
    
    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private IepsRedisProperties iepsRedisProperties;

    @Autowired
    private StorageService storageService;
    
    public ServerResponse<UserInfo> findByUserNum(String userNum) {
        if (userNum == null || userNum.isEmpty() || "undefined".equals(userNum)) {
            return ServerResponse.createByErrorMessage("用户不存在，请重新登录！");
        }
        
        UserInfo userInfo = userInfoMapper.selectByUserNum(userNum);
        
        if (userInfo == null) {
            return ServerResponse.createByErrorMessage("用户过时或不存在，请重新登录！");
        }
        
        userInfo.setUserNum(userNum);
        userInfo.setUserImg(storageService.resolveUserImageUrl(userInfo.getUserImg()));
        
        return ServerResponse.createBySuccess(userInfo);
    }
    
    public ServerResponse<String> modifyUserInfo(UserInfo userInfo) {
        int result = userInfoMapper.updateByUserNumSelective(userInfo);
        if (result == 0) {
            return ServerResponse.createByErrorMessage("你还没有修改任何信息");
        }
        
        return ServerResponse.createBySuccessMessage("恭喜你，修改成功！");
    }
    
    public ServerResponse<String> getVerifyCode(String userNum, String verifyNum) {
        ServerResponse<?> verifyNumCheck = checkVerifyNum(userNum, verifyNum);
        if (verifyNumCheck.getStatus() != 0) {
            return ServerResponse.createByErrorMessage(verifyNumCheck.getMsg());
        }

        String cooldownKey = buildCooldownKey(userNum);
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(cooldownKey))) {
            Long seconds = stringRedisTemplate.getExpire(cooldownKey, TimeUnit.SECONDS);
            long remainSeconds = seconds == null || seconds < 0 ? iepsRedisProperties.getCooldownSeconds() : seconds;
            return ServerResponse.createByErrorMessage("验证码发送过于频繁，请在" + remainSeconds + "秒后重试！");
        }

        String code = generateVerifyCode();

        // 邮箱验证
        if (verifyNum.indexOf('@') != -1) {
            try {
                MailUtil.send_mail(verifyNum, code);
            } catch (MessagingException e) {
                e.printStackTrace();
                return ServerResponse.createByErrorMessage("邮箱验证码发送失败，请稍后重试！");
            }
        } else {
            IndustrySMS.execute(verifyNum, code);
        }

        stringRedisTemplate.opsForValue().set(buildVerifyCodeKey(userNum), code,
                iepsRedisProperties.getTtlSeconds(), TimeUnit.SECONDS);
        stringRedisTemplate.opsForValue().set(cooldownKey, "1",
                iepsRedisProperties.getCooldownSeconds(), TimeUnit.SECONDS);
        stringRedisTemplate.delete(buildFailKey(userNum));

        return ServerResponse.createBySuccessMessage("验证码已发送，请及时输入验证");
    }
    
    public ServerResponse<?> checkVerifyNum(String userNum, String verifyNum) {
        int result = 0;
        
        if (verifyNum.lastIndexOf("@") != -1) {
            result = userInfoMapper.selectByUserNumAndEmail(userNum, verifyNum);
        }
        else {
            result = userInfoMapper.selectByUserNumAndPhotoNum(userNum, verifyNum);
        }
        
        if (result == 0) {
            return ServerResponse.createByErrorMessage("账号与手机号码或邮箱不匹配，请重新尝试！");
        }
        
        return ServerResponse.createBySuccess();
    }

    public ServerResponse<String> checkVerifyCode(String userNum, String verifyNum, String verifyCode) {
        if (verifyCode == null || verifyCode.trim().isEmpty()) {
            return ServerResponse.createByErrorMessage("请输入验证码后再继续！");
        }

        ServerResponse<?> verifyNumCheck = checkVerifyNum(userNum, verifyNum);
        if (verifyNumCheck.getStatus() != 0) {
            return ServerResponse.createByErrorMessage(verifyNumCheck.getMsg());
        }

        String codeKey = buildVerifyCodeKey(userNum);
        String cachedVerifyCode = stringRedisTemplate.opsForValue().get(codeKey);
        if (cachedVerifyCode == null || cachedVerifyCode.trim().isEmpty()) {
            return ServerResponse.createByErrorMessage("验证码已失效，请重新获取！");
        }

        if (!cachedVerifyCode.equals(verifyCode.trim())) {
            long failCount = stringRedisTemplate.opsForValue().increment(buildFailKey(userNum));
            if (failCount == 1L) {
                Long ttl = stringRedisTemplate.getExpire(codeKey, TimeUnit.SECONDS);
                if (ttl != null && ttl > 0) {
                    stringRedisTemplate.expire(buildFailKey(userNum), ttl, TimeUnit.SECONDS);
                }
            }

            if (failCount >= iepsRedisProperties.getMaxFailures()) {
                clearVerifyState(userNum);
                return ServerResponse.createByErrorMessage("验证码错误次数过多，请重新获取验证码！");
            }
            return ServerResponse.createByErrorMessage("验证码不正确，请重新输入！");
        }

        clearVerifyState(userNum);
        
        // 生成短时效的 forgetPwdToken 存入 Redis，替代 Session
        String forgetPwdToken = java.util.UUID.randomUUID().toString();
        String tokenKey = Const.REDIS_FORGET_PWD_TOKEN_PREFIX + userNum;
        stringRedisTemplate.opsForValue().set(tokenKey, forgetPwdToken,
                Const.FORGET_PWD_VERIFIED_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        
        return ServerResponse.createBySuccess("验证通过，请继续下一步！", forgetPwdToken);
    }
    
    public ServerResponse<List<UserAdminDto>> getUserInfoWithItemNum(String itemNum) {
        List<UserAdminDto> userAdminDtoList = userInfoMapper.selectUserInfoWithItemNum(itemNum);
        if (userAdminDtoList.size() == 0) {
            return ServerResponse.createByError();
        }
        
        return ServerResponse.createBySuccess(userAdminDtoList);
    }

    private String buildVerifyCodeKey(String userNum) {
        return iepsRedisProperties.getNamespace() + ":forgetPwd:" + userNum;
    }

    private String buildCooldownKey(String userNum) {
        return iepsRedisProperties.getNamespace() + ":cooldown:" + userNum;
    }

    private String buildFailKey(String userNum) {
        return iepsRedisProperties.getNamespace() + ":fail:" + userNum;
    }

    private void clearVerifyState(String userNum) {
        stringRedisTemplate.delete(buildVerifyCodeKey(userNum));
        stringRedisTemplate.delete(buildCooldownKey(userNum));
        stringRedisTemplate.delete(buildFailKey(userNum));
    }

    private String generateVerifyCode() {
        int random = (int) (Math.random() * 1000000);
        return String.format("%06d", random);
    }

}
