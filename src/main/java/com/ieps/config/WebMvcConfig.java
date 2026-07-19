package com.ieps.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

/**
 * Spring MVC 配置类
 * 替代旧的 springmvc.xml 配置
 *
 * @author ljw
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private DocsAccessInterceptor docsAccessInterceptor;

    /**
     * 配置视图解析器
     * 对应 springmvc.xml 中的 InternalResourceViewResolver 配置
     * prefix: /pages/, suffix: .html
     */
    @Bean
    public InternalResourceViewResolver viewResolver() {
        InternalResourceViewResolver resolver = new InternalResourceViewResolver();
        resolver.setPrefix("/pages/");
        resolver.setSuffix(".html");
        resolver.setOrder(0);
        return resolver;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(docsAccessInterceptor)
                .addPathPatterns("/api-docs/**", "/docs/**");
    }

    /**
     * 配置静态资源路径
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 兼容历史页面在不同入口下拼出的 /pages/**/static/** 资源地址
        registry.addResourceHandler(
                        "/pages/common/static/**",
                        "/pages/basic/static/**",
                        "/pages/admin/static/**",
                        "/pages/item/static/**",
                        "/pages/show/static/**")
                .addResourceLocations("classpath:/static/static/");
    }
}
