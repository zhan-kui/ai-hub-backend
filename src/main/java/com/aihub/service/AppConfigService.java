package com.aihub.service;

import com.aihub.common.enums.ResourceTypeEnum;
import com.aihub.common.exception.BizException;
import com.aihub.config.DifyProperties;
import com.aihub.dto.app.AppConfigVO;
import com.aihub.dto.app.CreateAppRequest;
import com.aihub.entity.AppConfig;
import com.aihub.repository.AppConfigRepository;
import com.aihub.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppConfigService {

    private final AppConfigRepository appConfigRepository;
    private final ResourceAuthService resourceAuthService;
    private final WebClient webClient;
    private final DifyProperties difyProperties;

    public List<AppConfigVO> listByPermission(Long userId, String roleCode) {
        List<Long> authorizedIds = resourceAuthService
                .getAuthorizedResourceIds(userId, roleCode, ResourceTypeEnum.APP);

        List<AppConfig> apps;
        if (authorizedIds == null) {
            apps = appConfigRepository.findAllByDeletedFalseAndEnabledTrueOrderBySortAsc();
        } else if (authorizedIds.isEmpty()) {
            return List.of();
        } else {
            apps = appConfigRepository.findAllByIdInAndDeletedFalseAndEnabledTrue(authorizedIds);
        }

        return apps.stream().map(this::toVO).toList();
    }

    public AppConfigVO getDetail(Long id) {
        AppConfig app = appConfigRepository.findById(id)
                .orElseThrow(() -> new BizException(404, "应用不存在"));
        return toVO(app);
    }

    public AppConfigVO create(CreateAppRequest request) {
        AppConfig app = new AppConfig();
        app.setAppName(request.getAppName());
        app.setAppCode(request.getAppCode());
        app.setAppType(request.getAppType());
        app.setDifyApiKey(request.getDifyApiKey());
        app.setDifyAppId(request.getDifyAppId());
        app.setDifyBaseUrl(request.getDifyBaseUrl());
        app.setDescription(request.getDescription());
        app.setIcon(request.getIcon());
        app.setSort(request.getSort() != null ? request.getSort() : 0);
        app.setEnabled(true);
        app.setDeleted(false);
        app.setCreatedBy(SecurityUtils.getCurrentUserId());
        return toVO(appConfigRepository.save(app));
    }

    public AppConfigVO update(Long id, CreateAppRequest request) {
        AppConfig app = appConfigRepository.findById(id)
                .orElseThrow(() -> new BizException(404, "应用不存在"));
        app.setAppName(request.getAppName());
        app.setAppType(request.getAppType());
        app.setDifyApiKey(request.getDifyApiKey());
        app.setDifyAppId(request.getDifyAppId());
        app.setDifyBaseUrl(request.getDifyBaseUrl());
        app.setDescription(request.getDescription());
        app.setIcon(request.getIcon());
        if (request.getSort() != null) {
            app.setSort(request.getSort());
        }
        return toVO(appConfigRepository.save(app));
    }

    public void delete(Long id) {
        AppConfig app = appConfigRepository.findById(id)
                .orElseThrow(() -> new BizException(404, "应用不存在"));
        app.setDeleted(true);
        appConfigRepository.save(app);
    }

    public Object getDifyParameters(Long id) {
        AppConfig app = appConfigRepository.findById(id)
                .orElseThrow(() -> new BizException(404, "应用不存在"));
        String baseUrl = resolveBaseUrl(app.getDifyBaseUrl());

        return webClient.mutate().baseUrl(baseUrl).build()
                .get()
                .uri("/parameters")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + app.getDifyApiKey())
                .retrieve()
                .bodyToMono(Object.class)
                .block();
    }

    private String resolveBaseUrl(String appBaseUrl) {
        return (appBaseUrl != null && !appBaseUrl.isBlank())
                ? appBaseUrl : difyProperties.getBaseUrl();
    }

    private AppConfigVO toVO(AppConfig app) {
        return AppConfigVO.builder()
                .id(app.getId())
                .appName(app.getAppName())
                .appCode(app.getAppCode())
                .appType(app.getAppType())
                .description(app.getDescription())
                .icon(app.getIcon())
                .sort(app.getSort())
                .enabled(app.getEnabled())
                .createdAt(app.getCreatedAt())
                .build();
    }
}