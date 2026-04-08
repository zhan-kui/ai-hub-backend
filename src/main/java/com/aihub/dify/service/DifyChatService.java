package com.aihub.dify.service;

import com.aihub.common.exception.BizException;
import com.aihub.config.DifyProperties;
import com.aihub.dify.model.ChatRequest;
import com.aihub.dify.model.DifyChatRequest;
import com.aihub.dify.model.DifyStreamEvent;
import com.aihub.entity.AppConfig;
import com.aihub.repository.AppConfigRepository;
import com.aihub.service.ChatHistoryService;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class DifyChatService {

    private final WebClient webClient;
    private final DifyProperties difyProperties;
    private final AppConfigRepository appConfigRepository;
    private final ObjectMapper objectMapper;
    private final ChatHistoryService chatHistoryService;

    public void streamChat(Long appId, Long userId, ChatRequest request, SseEmitter emitter) {
        AppConfig app = getAppConfig(appId);
        String baseUrl = resolveBaseUrl(app);

        DifyChatRequest difyRequest = DifyChatRequest.builder()
                .inputs(request.getInputs() != null ? request.getInputs() : Map.of())
                .query(request.getQuery())
                .user("abc-123" )
                .responseMode("streaming")
                .conversationId(request.getConversationId())
                .build();


        log.info("Dify SSE 请求: {}", difyRequest);


        StreamContext context = new StreamContext(appId, userId, request.getQuery());

        log.info("应用信息： {}" , app);
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
                            log.info("context历史聊天记录保存内容：{}",context);
                            saveHistorySafely(context, null);
                            emitter.complete();
                        }
                );
    }

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

    private void handleChunk(String chunk, SseEmitter emitter, StreamContext context) {
        if (!StringUtils.hasText(chunk)) {
            return;
        }

        String[] lines = chunk.replace("\r", "").split("\n");
        for (String line : lines) {
            String payload = line == null ? "" : line.trim();
            if (!StringUtils.hasText(payload) || payload.startsWith(":")) {
                continue;
            }
            if (payload.startsWith("event:")) {
                continue;
            }
            if (payload.startsWith("data:")) {
                payload = payload.substring(5).trim();
            }
            if (!StringUtils.hasText(payload) || "[DONE]".equals(payload)) {
                continue;
            }

            try {
                //  加上这一行，打印最原始的 Dify 返回的 JSON 字符串！
                log.info("原始 Dify SSE 数据: {}", payload);
                DifyStreamEvent event = objectMapper.readValue(payload, DifyStreamEvent.class);
                dispatchEvent(event, emitter, context);
            } catch (Exception ex) {
                log.debug("跳过无法解析的 SSE 片段: {}", payload);
            }
        }
    }

    private void dispatchEvent(DifyStreamEvent event, SseEmitter emitter, StreamContext context) {
        if (event == null || event.getEvent() == null) {
            return;
        }

        switch (event.getEvent()) {
            case "message" -> {
                if (StringUtils.hasText(event.getAnswer())) {
                    context.answerBuilder.append(event.getAnswer());
                }
                if (StringUtils.hasText(event.getConversationId())) {
                    context.difyConversationId = event.getConversationId();
                }
                if (StringUtils.hasText(event.getMessageId())) {
                    context.difyMessageId = event.getMessageId();
                }
                Map<String, Object> data = new HashMap<>();
                data.put("answer", event.getAnswer());
                data.put("conversationId", event.getConversationId());
                data.put("messageId", event.getMessageId());
                data.put("taskId", event.getTaskId());
                safeSend(emitter, "message", data);
            }
            case "message_end" -> {
                if (StringUtils.hasText(event.getConversationId())) {
                    context.difyConversationId = event.getConversationId();
                }
                if (StringUtils.hasText(event.getMessageId())) {
                    context.difyMessageId = event.getMessageId();
                }
                context.metadata = event.getMetadata();

                Map<String, Object> data = new HashMap<>();
                data.put("conversationId", event.getConversationId());
                data.put("messageId", event.getMessageId());
                data.put("metadata", event.getMetadata() != null ? event.getMetadata() : Map.of());
                safeSend(emitter, "message_end", data);

                saveHistorySafely(context, event.getMetadata());
            }
            case "message_replace" -> {
                context.answerBuilder.setLength(0);
                if (StringUtils.hasText(event.getAnswer())) {
                    context.answerBuilder.append(event.getAnswer());
                }
                safeSend(emitter, "message_replace", Map.of("answer", event.getAnswer()));
            }
            case "error" -> {
                String message = event.getMessage() != null ? event.getMessage() : "AI 服务异常";
                safeSend(emitter, "error", Map.of("message", message));
                saveHistorySafely(context, event.getMetadata());
                emitter.complete();
            }
            // 新增 Dify 工作流事件处理
            case "workflow_started", "node_started" -> {
                log.debug("Dify节点开始: {}", event.getEvent());
                // 可选：通知前端 AI 正在执行动作
                safeSend(emitter, "status", Map.of("status", "processing", "event", event.getEvent()));
            }
            case "workflow_finished", "node_finished" -> {
                log.debug("Dify节点结束: {}", event.getEvent());
            }
            case "tts_message", "tts_message_end" -> {
                // 如果开启了语音 TTS 会有这个
            }
            default -> log.debug("未知 Dify 事件: {}", event.getEvent());
        }
    }

    private void saveHistorySafely(StreamContext context, Map<String, Object> metadata) {
        log.error("保存聊天记录context: {}", context);
        if (!context.saved.compareAndSet(false, true)) {
            return;
        }
        try {
            chatHistoryService.saveRound(
                    context.appId,
                    context.userId,
                    context.query,
                    context.answerBuilder.toString(),
                    context.difyConversationId,
                    context.difyMessageId,
                    metadata != null ? metadata : context.metadata
            );
        } catch (Exception e) {
            log.warn("保存聊天记录失败: {}", e.getMessage());
        }
    }

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

    private static final class StreamContext {
        private final Long appId;
        private final Long userId;
        private final String query;
        private final StringBuilder answerBuilder = new StringBuilder();
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