package com.ieps.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI 配置
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI iepsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("IEPS 大学生创新创业项目管理系统 API")
                        .description("创新与创业项目管理平台的 RESTful API 文档，涵盖用户管理、项目管理、"
                                + "评审管理、通知公告、文件管理等模块。")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("IEPS Dev Team")));
    }
}
