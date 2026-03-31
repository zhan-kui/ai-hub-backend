package com.aihub.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "dify.api")
public class DifyProperties {

    private String baseUrl = "http://localhost/v1";

    private int connectTimeout = 10000;

    private int readTimeout = 120000;

    private int maxInMemorySize = 10 * 1024 * 1024;
}