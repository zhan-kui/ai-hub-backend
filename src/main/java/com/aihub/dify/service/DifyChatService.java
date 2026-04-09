package com.aihub.dify.service;

import com.aihub.common.exception.BizException;
import com.aihub.config.DifyProperties;
import com.aihub.dify.model.ChatRequest;
import com.aihub.dify.model.DifyChatRequest;
import com.aihub.dify.model.DifyStreamEvent;
import com.aihub.entity.AppConfig;
import com.aihub.repository.AppConfigRepository;
import com.aihub.service.ChatHistoryService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

// Dify AI 对话服务
@Slf4j
@Service
@RequiredArgsConstructor
public class DifyChatService {

    private final WebClient webClient;
    private final DifyProperties difyProperties;
    private final AppConfigRepository appConfigRepository;
    private final ObjectMapper objectMapper;
    private final ChatHistoryService chatHistoryService;

    // 发起流式对话请求
    public void streamChat(Long appId, Long userId, ChatRequest request, SseEmitter emitter) {
        AppConfig app = getAppConfig(appId);
        String baseUrl = resolveBaseUrl(app);

        // 构建请求体
        DifyChatRequest difyRequest = DifyChatRequest.builder()
                .inputs(request.getInputs() != null ? request.getInputs() : Map.of())
                .query(request.getQuery())
                .user("abc-123" )
                .responseMode("streaming")
                .conversationId(request.getConversationId())
                .build();

        log.info("Dify SSE 请求: {}", difyRequest);

        // 初始化上下文，用来存拼接的聊天记录（包含思考和回复）
        StreamContext context = new StreamContext(appId, userId, request.getQuery());

        // 发起流式请求
        webClient.mutate().baseUrl(baseUrl).build()
                .post()
                .uri("/chat-messages")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + app.getDifyApiKey())
                .bodyValue(difyRequest)
                .retrieve()
                .bodyToFlux(String.class)
                .subscribe(
                        chunk -> handleChunk(chunk, emitter, context),
                        error -> {
                            log.error("Dify SSE 错误", error);
                            safeSend(emitter, "error", Map.of("message", "AI 服务异常"));
                            saveHistorySafely(context, null);
                            emitter.completeWithError(error);
                        },
                        () -> {
                            saveHistorySafely(context, null);
                            emitter.complete();
                        }
                );
    }

    // 停止生成
    public void stopGeneration(Long appId, Long userId, String taskId) {
        AppConfig app = getAppConfig(appId);
        String baseUrl = resolveBaseUrl(app);

        webClient.mutate().baseUrl(baseUrl).build()
                .post()
                .uri("/chat-messages/{taskId}/stop", taskId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + app.getDifyApiKey())
                .bodyValue(Map.of("user", "user-" + userId))
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    // 提交评价反馈
    public void feedback(Long appId, Long userId, String messageId, String rating) {
        AppConfig app = getAppConfig(appId);
        String baseUrl = resolveBaseUrl(app);

        Map<String, Object> body = new HashMap<>();
        body.put("user", "user-" + userId);
        body.put("rating", rating);

        webClient.mutate().baseUrl(baseUrl).build()
                .post()
                .uri("/messages/{messageId}/feedbacks", messageId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + app.getDifyApiKey())
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    // 获取建议问题
    public Object getSuggested(Long appId, Long userId, String messageId) {
        AppConfig app = getAppConfig(appId);
        String baseUrl = resolveBaseUrl(app);

        return webClient.mutate().baseUrl(baseUrl).build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/messages/{messageId}/suggested")
                        .queryParam("user", "user-" + userId)
                        .build(messageId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + app.getDifyApiKey())
                .retrieve()
                .bodyToMono(Object.class)
                .block();
    }

    // 音频转文本
    public Object audioToText(Long appId, Long userId, MultipartFile file) {
        AppConfig app = getAppConfig(appId);
        String baseUrl = resolveBaseUrl(app);

        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("file", file.getResource());
        bodyBuilder.part("user", "user-" + userId);

        return webClient.mutate().baseUrl(baseUrl).build()
                .post()
                .uri("/audio-to-text")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + app.getDifyApiKey())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(bodyBuilder.build())
                .retrieve()
                .bodyToMono(Object.class)
                .block();
    }

    // 处理 SSE 数据块
    private void handleChunk(String chunk, SseEmitter emitter, StreamContext context) {
        if (!StringUtils.hasText(chunk)) return;

        String[] lines = chunk.replace("\r", "").split("\n");
        for (String line : lines) {
            String payload = line == null ? "" : line.trim();
            if (!StringUtils.hasText(payload) || payload.startsWith(":")) continue;
            if (payload.startsWith("event:")) continue;
            if (payload.startsWith("data:")) payload = payload.substring(5).trim();
            if (!StringUtils.hasText(payload) || "[DONE]".equals(payload)) continue;

            try {
                DifyStreamEvent event = objectMapper.readValue(payload, DifyStreamEvent.class);
                dispatchRawEvent(payload, event, emitter, context);
            } catch (Exception ex) {
                log.debug("跳过无法解析的 SSE 片段: {}", payload);
            }
        }
    }

    // 直接返回 Dify 原始内容，通过解析 chunk_type 区分存库时的内容
    private void dispatchRawEvent(String rawPayload, DifyStreamEvent event, SseEmitter emitter, StreamContext context) {
        if (event == null || event.getEvent() == null) return;

        try {
            // 解析原始 JSON，用于读取 chunk_type 以及直接转成 JsonNode 返回给前端
            JsonNode rootNode = objectMapper.readTree(rawPayload);
            // 获取 chunk_type 字段（如果不存在默认为空字符串）
            String chunkType = rootNode.path("chunk_type").asText("");

            // 1. 拦截解析，维护对话上下文（用于保存历史记录）
            switch (event.getEvent()) {
                case "message" -> {
                    String answerContent = event.getAnswer();
                    if (StringUtils.hasText(answerContent)) {
                        // 判断是否为思考过程（包含 start / thought / end）
                        if ("thought".equals(chunkType) || "thought_start".equals(chunkType) || "thought_end".equals(chunkType)) {
                            context.thoughtBuilder.append(answerContent);
                        } else {
                            // 否则属于普通的回复内容 (chunk_type = text 或 为空)
                            context.answerBuilder.append(answerContent);
                        }
                    }
                    if (StringUtils.hasText(event.getConversationId())) context.difyConversationId = event.getConversationId();
                    if (StringUtils.hasText(event.getMessageId())) context.difyMessageId = event.getMessageId();
                }
                case "message_end" -> {
                    if (StringUtils.hasText(event.getConversationId())) context.difyConversationId = event.getConversationId();
                    if (StringUtils.hasText(event.getMessageId())) context.difyMessageId = event.getMessageId();
                    context.metadata = event.getMetadata();
                    // 结束时触发保存历史
                    saveHistorySafely(context, event.getMetadata());
                }
                case "message_replace" -> {
                    // 如果被替换，清空最终回复重新拼接
                    context.answerBuilder.setLength(0);
                    if (StringUtils.hasText(event.getAnswer())) context.answerBuilder.append(event.getAnswer());
                }
                case "error" -> {
                    saveHistorySafely(context, event.getMetadata());
                }
            }

            // 2. 原封不动返回前端
            emitter.send(SseEmitter.event()
                    .name(event.getEvent())
                    .data(rootNode));

            if ("error".equals(event.getEvent())) {
                emitter.complete();
            }
        } catch (Exception e) {
            log.warn("SSE 处理/发送失败: {}", e.getMessage());
        }
    }

    // 安全保存聊天记录
    private void saveHistorySafely(StreamContext context, Map<String, Object> metadata) {
        if (!context.saved.compareAndSet(false, true)) {
            return;
        }
        try {
            // 注意这里传递了两个参数：thoughtBuilder.toString() 和 answerBuilder.toString()
            chatHistoryService.saveRound(
                    context.appId,
                    context.userId,
                    context.query,
                    context.thoughtBuilder.toString(), // 将思考内容传过去
                    context.answerBuilder.toString(),  // 原有的回答内容
                    context.difyConversationId,
                    context.difyMessageId,
                    metadata != null ? metadata : context.metadata
            );
        } catch (Exception e) {
            log.warn("保存聊天记录失败: {}", e.getMessage());
        }
    }

    // 通用发包容错封装
    private void safeSend(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data));
        } catch (Exception e) {
            log.warn("SSE 发送失败: {}", e.getMessage());
        }
    }

    private AppConfig getAppConfig(Long appId) {
        return appConfigRepository.findById(appId)
                .orElseThrow(() -> new BizException(404, "应用不存在"));
    }

    private String resolveBaseUrl(AppConfig app) {
        return (app.getDifyBaseUrl() != null && !app.getDifyBaseUrl().isBlank())
                ? app.getDifyBaseUrl() : difyProperties.getBaseUrl();
    }

    // 内部类：流处理上下文记录器
    private static final class StreamContext {
        private final Long appId;
        private final Long userId;
        private final String query;
        private final StringBuilder thoughtBuilder = new StringBuilder(); // 新增：用来拼接思考过程
        private final StringBuilder answerBuilder = new StringBuilder();  // 最终的普通回复内容
        private final AtomicBoolean saved = new AtomicBoolean(false);
        private String difyConversationId;
        private String difyMessageId;
        private Map<String, Object> metadata;

        private StreamContext(Long appId, Long userId, String query) {
            this.appId = appId;
            this.userId = userId;
            this.query = query;
        }
    }
}