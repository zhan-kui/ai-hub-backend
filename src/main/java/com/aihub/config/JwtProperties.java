package com.aihub.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secret;
    private Long expiration = 604800000L; // 7 天
    private String header = "Authorization";
    private String prefix = "Bearer ";
}