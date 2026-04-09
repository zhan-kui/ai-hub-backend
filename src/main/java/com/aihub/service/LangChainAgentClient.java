package com.aihub.service;

import com.aihub.common.exception.BizException;
import com.aihub.config.LangChainAgentProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI-compatible 模型调用客户端。
 * <p>
 * 当前实现面向 vLLM / OpenAI 兼容接口。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LangChainAgentClient {

    private final WebClient webClient;
    private final LangChainAgentProperties properties;
    private final ObjectMapper objectMapper;

    /**
     * 调用模型生成回复。
     */
    public String generate(String prompt) {
        return doChatCompletion(prompt, false, null);
    }

    /**
     * 流式调用模型并将 reasoning / content 透出给调用方。
     */
    public String stream(String prompt, StreamChunkHandler handler) {
        return doChatCompletion(prompt, true, handler);
    }

    private String doChatCompletion(String prompt, boolean stream, StreamChunkHandler handler) {
        if (!StringUtils.hasText(prompt)) {
            throw new BizException(400, "prompt 不能为空");
        }
        if (!StringUtils.hasText(properties.getBaseUrl())) {
            throw new BizException(500, "智能体模型 baseUrl 未配置");
        }
        if (!StringUtils.hasText(properties.getModelName())) {
            throw new BizException(500, "智能体模型 modelName 未配置");
        }

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", properties.getModelName());
        requestBody.put("messages", buildMessages(prompt));
        requestBody.put("stream", stream);
        requestBody.put("temperature", properties.getTemperature());
        requestBody.put("top_p", properties.getTopP());
        if (properties.getMaxTokens() != null && properties.getMaxTokens() > 0) {
            requestBody.put("max_tokens", properties.getMaxTokens());
        }

        if (!stream) {
            JsonNode response = webClient.mutate()
                    .baseUrl(properties.getBaseUrl())
                    .build()
                    .post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .headers(this::applyAuthHeader)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(timeout());

            if (response == null) {
                throw new BizException(500, "模型服务返回空响应");
            }

            String content = extractContent(response);
            logResponseDebug("non-stream", response.toString(), content);
            if (!StringUtils.hasText(content)) {
                throw new BizException(500, "模型服务未返回有效内容");
            }
            return content.trim();
        }

        StringBuilder reasoning = new StringBuilder();
        StringBuilder content = new StringBuilder();

        Mono<Void> streamWork = webClient.mutate()
                .baseUrl(properties.getBaseUrl())
                .build()
                .post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .headers(this::applyAuthHeader)
                .bodyValue(requestBody)
                .exchangeToMono(response -> {
                    MediaType responseType = response.headers().contentType().orElse(null);
                    log.debug("Agent SSE 响应状态: status={}, contentType={}", response.statusCode(), responseType);

                    if (!response.statusCode().is2xxSuccessful()) {
                        return response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> Mono.error(new BizException(
                                        500,
                                        "模型服务 HTTP 错误: status=" + response.statusCode()
                                                + ", body=" + truncateForLog(body)
                                )));
                    }

                    if (responseType != null && MediaType.TEXT_EVENT_STREAM.isCompatibleWith(responseType)) {
                        return response.bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
                                .doOnNext(event -> handleSseEvent(event, reasoning, content, handler))
                                .then();
                    }

                    return response.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .doOnNext(body -> handleRawPayload(body, reasoning, content, handler))
                            .then();
                });

        try {
            streamWork.block(timeout());
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof BizException bizException) {
                throw bizException;
            }
            throw new BizException(500, "模型流式请求失败: " + cause.getMessage());
        }

        String finalContent = content.toString().trim();
        if (!StringUtils.hasText(finalContent) && StringUtils.hasText(reasoning.toString())) {
            finalContent = reasoning.toString().trim();
        }
        if (!StringUtils.hasText(finalContent)) {
            throw new BizException(500, "模型服务未返回有效内容");
        }
        return finalContent;
    }

    private List<Map<String, String>> buildMessages(String prompt) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "user", "content", prompt));
        return messages;
    }

    private void applyAuthHeader(HttpHeaders headers) {
        if (StringUtils.hasText(properties.getApiKey())) {
            headers.setBearerAuth(properties.getApiKey().trim());
        }
    }

    private Duration timeout() {
        Integer timeoutMs = properties.getRequestTimeoutMs();
        return Duration.ofMillis(timeoutMs != null && timeoutMs > 0 ? timeoutMs : 120_000L);
    }

    private String extractContent(JsonNode response) {
        JsonNode choices = response.path("choices");
        if (choices.isArray() && choices.size() > 0) {
            JsonNode first = choices.get(0);
            String content = first.path("message").path("content").asText("");
            if (StringUtils.hasText(content)) {
                return content;
            }

            content = first.path("text").asText("");
            if (StringUtils.hasText(content)) {
                return content;
            }
        }
        String outputText = response.path("output_text").asText("");
        if (StringUtils.hasText(outputText)) {
            return outputText;
        }
        return response.path("content").asText("");
    }

    private void handleSseEvent(ServerSentEvent<String> event,
                                StringBuilder reasoning,
                                StringBuilder content,
                                StreamChunkHandler handler) {
        if (event == null) {
            return;
        }

        String eventName = event.event();
        String payload = event.data();
        log.debug("Agent SSE 事件: event={}, data={}", eventName, truncateForLog(payload));
        if (!StringUtils.hasText(payload) || "ping".equalsIgnoreCase(eventName)) {
            return;
        }

        handleRawPayload(payload, reasoning, content, handler);
    }

    private void handleRawPayload(String payload,
                                  StringBuilder reasoning,
                                  StringBuilder content,
                                  StreamChunkHandler handler) {
        if (!StringUtils.hasText(payload)) {
            return;
        }

        String raw = payload.trim();
        if (!StringUtils.hasText(raw) || "[DONE]".equals(raw)) {
            return;
        }

        if (raw.startsWith("data:")) {
            raw = raw.substring(5).trim();
        }
        if (!StringUtils.hasText(raw) || "[DONE]".equals(raw)) {
            return;
        }

        log.debug("Agent 模型原始载荷: {}", truncateForLog(raw));

        try {
            if (looksLikeJson(raw)) {
                JsonNode root = objectMapper.readTree(raw);
                appendFromJson(root, reasoning, content, handler);
            } else {
                appendContent(raw, content, handler);
            }
        } catch (Exception e) {
            log.warn("Agent 模型载荷解析失败，按纯文本兜底: {}", truncateForLog(raw));
            appendContent(raw, content, handler);
        }
    }

    private void appendFromJson(JsonNode root,
                                StringBuilder reasoning,
                                StringBuilder content,
                                StreamChunkHandler handler) {
        if (root == null) {
            return;
        }

        JsonNode choices = root.path("choices");
        if (choices.isArray() && choices.size() > 0) {
            JsonNode first = choices.get(0);
            JsonNode delta = first.path("delta");
            JsonNode message = first.path("message");

            String reasoningText = firstText(delta, "reasoning_content", "reasoning");
            if (!StringUtils.hasText(reasoningText)) {
                reasoningText = firstText(message, "reasoning_content", "reasoning");
            }

            String contentText = firstText(delta, "content");
            if (!StringUtils.hasText(contentText)) {
                contentText = firstText(message, "content");
            }

            if (StringUtils.hasText(reasoningText)) {
                appendReasoning(reasoningText, reasoning, handler);
            }
            if (StringUtils.hasText(contentText)) {
                appendContent(contentText, content, handler);
            }

            if (!StringUtils.hasText(reasoningText) && !StringUtils.hasText(contentText)) {
                String text = firstText(first, "text");
                if (StringUtils.hasText(text)) {
                    appendContent(text, content, handler);
                }
            }
            return;
        }

        String outputText = firstText(root, "output_text", "content", "answer", "text");
        if (StringUtils.hasText(outputText)) {
            appendContent(outputText, content, handler);
        }
    }

    private void appendReasoning(String delta,
                                 StringBuilder reasoning,
                                 StreamChunkHandler handler) {
        reasoning.append(delta);
        if (handler != null) {
            handler.onReasoningStart();
            handler.onReasoningDelta(delta);
        }
    }

    private void appendContent(String delta,
                               StringBuilder content,
                               StreamChunkHandler handler) {
        content.append(delta);
        if (handler != null) {
            handler.onContentStart();
            handler.onContentDelta(delta);
        }
    }

    private boolean looksLikeJson(String raw) {
        return (raw.startsWith("{") && raw.endsWith("}"))
                || (raw.startsWith("[") && raw.endsWith("]"));
    }

    private String firstText(JsonNode node, String... fieldNames) {
        if (node == null) {
            return null;
        }
        for (String fieldName : fieldNames) {
            String value = node.path(fieldName).asText("");
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private void logResponseDebug(String mode, String rawBody, String content) {
        log.debug("Agent {} 响应解析结果: content={}, raw={}",
                mode,
                truncateForLog(content),
                truncateForLog(rawBody));
    }

    private String truncateForLog(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String normalized = text.replace("\r", "\\r").replace("\n", "\\n");
        return normalized.length() > 2000 ? normalized.substring(0, 2000) + "..." : normalized;
    }

    /**
     * 流式回调。
     */
    public interface StreamChunkHandler {
        default void onReasoningStart() {
        }

        default void onReasoningDelta(String delta) {
        }

        default void onContentStart() {
        }

        default void onContentDelta(String delta) {
        }
    }
}
