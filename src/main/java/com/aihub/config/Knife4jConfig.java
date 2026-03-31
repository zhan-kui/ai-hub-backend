package com.aihub.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI aihubOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("AiHub Backend API")
                        .description("AiHub 后端接口文档")
                        .version("1.0.0")
                        .contact(new Contact().name("aihub")))
                // 新增配置
                .components(new Components()
                        // 配置鉴权方式（HTTP Bearer）
                        .addSecuritySchemes("BearerToken", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                // 这里配置了 bearer，Swagger会自动加 Bearer 前缀
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)
                                .name("Authorization")
                        )
                )
                // 全局应用该鉴权配置（加上这行，页面上才会出 Authorize 按钮和接口旁边的小锁）
                .addSecurityItem(new SecurityRequirement().addList("BearerToken"));
    }

    @Bean
    public GroupedOpenApi defaultApi() {
        return GroupedOpenApi.builder()
                .group("default")
                .pathsToMatch("/**")
                .build();
    }
}