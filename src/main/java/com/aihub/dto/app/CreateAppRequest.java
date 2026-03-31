package com.aihub.dto.app;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateAppRequest {

    @NotBlank(message = "应用名称不能为空")
    private String appName;

    @NotBlank(message = "应用编码不能为空")
    private String appCode;

    @NotBlank(message = "应用类型不能为空")
    private String appType;

    @NotBlank(message = "Dify API Key 不能为空")
    private String difyApiKey;

    private String difyAppId;

    private String difyBaseUrl;

    private String description;

    private String icon;

    private Integer sort;
}