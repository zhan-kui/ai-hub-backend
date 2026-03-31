package com.aihub.controller;

import com.aihub.annotation.RequireResource;
import com.aihub.common.enums.ResourceTypeEnum;
import com.aihub.common.exception.BizException;
import com.aihub.common.result.R;
import com.aihub.config.DifyProperties;
import com.aihub.entity.AppConfig;
import com.aihub.repository.AppConfigRepository;
import com.aihub.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@Tag(name = "文件管理")
public class FileController {

    private final WebClient webClient;
    private final DifyProperties difyProperties;
    private final AppConfigRepository appConfigRepository;

    @PostMapping("/upload")
    @Operation(summary = "上传文件到 Dify（图片/文档）")
    @RequireResource(type = ResourceTypeEnum.APP, paramName = "appId")
    public R<Object> upload(@RequestParam Long appId,
                            @RequestParam("file") MultipartFile file) {
        Long userId = SecurityUtils.getCurrentUserId();
        AppConfig app = appConfigRepository.findById(appId)
                .orElseThrow(() -> new BizException(404, "应用不存在"));

        String baseUrl = (app.getDifyBaseUrl() != null && !app.getDifyBaseUrl().isBlank())
                ? app.getDifyBaseUrl() : difyProperties.getBaseUrl();

        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("file", file.getResource());
        bodyBuilder.part("user", "user-" + userId);

        Object result = webClient.mutate().baseUrl(baseUrl).build()
                .post()
                .uri("/files/upload")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + app.getDifyApiKey())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(bodyBuilder.build())
                .retrieve()
                .bodyToMono(Object.class)
                .block();

        return R.ok(result);
    }
}