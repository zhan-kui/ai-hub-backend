package com.aihub.dify.service;

import com.aihub.common.exception.BizException;
import com.aihub.config.DifyProperties;
import com.aihub.entity.AppConfig;
import com.aihub.repository.AppConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DifyConversationService {

    private final WebClient webClient;
    private final DifyProperties difyProperties;
    private final AppConfigRepository appConfigRepository;

    public Object listConversations(Long appId, Long userId, Integer page, Integer limit) {
        AppConfig app = getAppConfig(appId);
        String baseUrl = resolveBaseUrl(app);

        return webClient.mutate().baseUrl(baseUrl).build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/conversations")
                        .queryParam("user", "user-" + userId)
                        .queryParam("limit", limit)
                        .queryParam("page", page)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + app.getDifyApiKey())
                .retrieve()
                .bodyToMono(Object.class)
                .block();
    }

    public Object listMessages(Long appId, Long userId,
                               String conversationId, String firstId, Integer limit) {
        AppConfig app = getAppConfig(appId);
        String baseUrl = resolveBaseUrl(app);

        return webClient.mutate().baseUrl(baseUrl).build()
                .get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/messages")
                            .queryParam("user", "user-" + userId)
                            .queryParam("conversation_id", conversationId)
                            .queryParam("limit", limit);
                    if (firstId != null && !firstId.isBlank()) {
                        uriBuilder.queryParam("first_id", firstId);
                    }
                    return uriBuilder.build();
                })
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + app.getDifyApiKey())
                .retrieve()
                .bodyToMono(Object.class)
                .block();
    }

    public Object rename(Long appId, Long userId,
                         String conversationId, String name, Boolean autoGenerate) {
        AppConfig app = getAppConfig(appId);
        String baseUrl = resolveBaseUrl(app);

        Map<String, Object> body = new HashMap<>();
        body.put("user", "user-" + userId);
        if (name != null && !name.isBlank()) {
            body.put("name", name);
        }
        body.put("auto_generate", autoGenerate != null && autoGenerate);

        return webClient.mutate().baseUrl(baseUrl).build()
                .post()
                .uri("/conversations/{conversationId}/name", conversationId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + app.getDifyApiKey())
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Object.class)
                .block();
    }

    public void delete(Long appId, Long userId, String conversationId) {
        AppConfig app = getAppConfig(appId);
        String baseUrl = resolveBaseUrl(app);

        webClient.mutate().baseUrl(baseUrl).build()
                .method(HttpMethod.DELETE)
                .uri("/conversations/{conversationId}", conversationId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + app.getDifyApiKey())
                .bodyValue(Map.of("user", "user-" + userId))
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    private AppConfig getAppConfig(Long appId) {
        return appConfigRepository.findById(appId)
                .orElseThrow(() -> new BizException(404, "应用不存在"));
    }

    private String resolveBaseUrl(AppConfig app) {
        return (app.getDifyBaseUrl() != null && !app.getDifyBaseUrl().isBlank())
                ? app.getDifyBaseUrl() : difyProperties.getBaseUrl();
    }
}