package com.aihub.dto.app;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AppConfigVO {

    private Long id;

    private String appName;

    private String appCode;

    private String appType;

    private String description;

    private String icon;

    private Integer sort;

    private Boolean enabled;

    private LocalDateTime createdAt;
}