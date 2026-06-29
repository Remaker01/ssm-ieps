package com.ieps.config;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

/**
 * 验证码 Kaptcha 配置类
 * 替代 applicationContext.xml 中的 captchaProducer bean 配置
 *
 * @author ljw
 */
@Configuration
public class KaptchaConfig {

    @Bean
    public DefaultKaptcha captchaProducer() {
        DefaultKaptcha kaptcha = new DefaultKaptcha();
        Properties properties = new Properties();
        // 是否有边框 可选yes 或者 no
        properties.setProperty("kaptcha.border", "no");
        // 边框颜色
        properties.setProperty("kaptcha.border.color", "105,179,90");
        // 验证码文本字符颜色
        properties.setProperty("kaptcha.textproducer.font.color", "blue");
        // 验证码文本字符大小
        properties.setProperty("kaptcha.textproducer.font.size", "30");
        // 验证码图片的宽度 默认200
        properties.setProperty("kaptcha.image.width", "90");
        // 验证码图片的高度 默认50
        properties.setProperty("kaptcha.image.height", "40");
        // 验证码文本字符长度  默认为5
        properties.setProperty("kaptcha.textproducer.char.length", "4");
        // 验证码文本字体样式
        properties.setProperty("kaptcha.textproducer.font.names", "宋体,楷体,微软雅黑");

        Config config = new Config(properties);
        kaptcha.setConfig(config);
        return kaptcha;
    }
}
