package com.aihub.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.aihub.repository")
@EntityScan(basePackages = "com.aihub.entity")
public class JpaConfig {
    // JPA 只扫描 repository 包，避免与 MyBatis-Plus 的 Mapper 冲突
}