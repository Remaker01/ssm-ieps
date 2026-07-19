package com.ieps;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * IEPS - Innovation and Entrepreneurship Project Management System
 * Spring Boot 启动类
 *
 * @author ljw
 */
@SpringBootApplication
@MapperScan("com.ieps.mapper")
@EnableTransactionManagement
@EnableCaching
@ServletComponentScan
public class IepsApplication {

    public static void main(String[] args) {
        new SpringApplication(IepsApplication.class).run(args);
    }
}
