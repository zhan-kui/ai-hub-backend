package com.aihub.service;

import com.aihub.agent.skill.CalculatorSkill;
import com.aihub.agent.skill.UserCountSkill;
import com.aihub.agent.skill.WeatherSkill;
import com.aihub.common.exception.BizException;
import com.aihub.config.LangChainAgentProperties;
import com.aihub.dto.agent.AgentChatRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.input.PromptTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LangChain4j 智能体核心服务。
 * <p>
 * 该服务承担三类职责：
 * <p>1) 根据 agentId 路由到不同 Agent 策略（simple/memory/skill）</p>
 * <p>2) 统一把 Agent 输出转换为 SSE 事件（thinking / message / message_end / error）</p>
 * <p>3) 在 skill 模式下发送工具调用链事件（tool_call / tool_result）</p>
 * <p>
 * 说明：当前示例以“可运行、可演示”为目标，
 * 已引入 LangChain4j 的 PromptTemplate、ChatMemory、@Tool 能力，
 * 并接入 OpenAI-compatible 模型客户端。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService {

    /**
     * 数学表达式提取规则。
     */
    private static final Pattern CALC_PATTERN = Pattern.compile("(-?[0-9.]+(?:\\s*[+\\-*/]\\s*-?[0-9.]+)+)");

    /**
     * 天气问题中的城市提取规则。
     */
    private static final Pattern CITY_PATTERN = Pattern.compile("([\\u4e00-\\u9fa5]{2,10})天气");

    /**
     * simple Agent 提示词模板。
     */
    private static final PromptTemplate SIMPLE_TEMPLATE = PromptTemplate.from(
            "你是一个简洁的问答助手。请用中文给出清晰答案。\\n问题：{{query}}"
    );

    /**
     * memory Agent 提示词模板。
     */
    private static final PromptTemplate MEMORY_TEMPLATE = PromptTemplate.from(
            "你是一个带记忆的对话助手。\\n" +
                    "历史摘要：{{history}}\\n" +
                    "当前问题：{{query}}\\n" +
                    "请结合历史给出连贯回答。"
    );

    /**
     * 智能体配置参数。
     */
    private final LangChainAgentProperties properties;

    /**
     * 模型调用客户端。
     */
    private final LangChainAgentClient agentClient;

    /**
     * 天气 Skill。
     */
    private final WeatherSkill weatherSkill;

    /**
     * 数据库用户统计 Skill。
     */
    private final UserCountSkill userCountSkill;

    /**
     * 计算器 Skill。
     */
    private final CalculatorSkill calculatorSkill;

    /**
     * memory Agent 的会话记忆仓库。
     * <p>key 为 conversationId，value 为消息窗口记忆实例。</p>
     */
    private final Map<String, ChatMemory> memoryStore = new ConcurrentHashMap<>();

    /**
     * 启动流式会话。
     *
     * @param agentId 代理类型：simple / memory / skill
     * @param userId 当前用户 ID
     * @param request 请求体
     * @param emitter SSE 发射器
     */
    public void streamChat(String agentId,
                           Long userId,
                           AgentChatRequest request,
                           SseEmitter emitter) {
        String normalizedAgentId = normalizeAgentId(agentId);
        String conversationId = StringUtils.hasText(request.getConversationId())
                ? request.getConversationId().trim()
                : "conv_" + UUID.randomUUID().toString().replace("-", "");

        String messageId = "msg_" + UUID.randomUUID().toString().replace("-", "");
        String taskId = "task_" + UUID.randomUUID().toString().replace("-", "");

        Thread.startVirtualThread(() -> {
            try {
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("agentId", normalizedAgentId);
                metadata.put("userId", userId);
                metadata.put("timestamp", LocalDateTime.now().toString());
                metadata.put("modelName", properties.getModelName());
                metadata.put("modelBaseUrl", properties.getBaseUrl());

                AgentGeneration generation = switch (normalizedAgentId) {
                    case "simple" -> runSimpleAgent(conversationId, messageId, taskId, request.getQuery(), metadata, emitter);
                    case "memory" -> runMemoryAgent(conversationId, messageId, taskId, request.getQuery(), metadata, emitter);
                    case "skill" -> runSkillAgent(conversationId, messageId, taskId, request.getQuery(), metadata, emitter);
                    default -> throw new BizException(400, "不支持的 agentId: " + normalizedAgentId);
                };

                Map<String, Object> endData = new HashMap<>();
                endData.put("conversationId", conversationId);
                endData.put("messageId", messageId);
                endData.put("answer", generation.answer());
                endData.put("reasoning", generation.reasoning());
                endData.put("metadata", metadata);
                safeSend(emitter, "message_end", endData);
                emitter.complete();
            } catch (Exception e) {
                log.error("Agent SSE 处理异常", e);
                safeSend(emitter, "error", Map.of("message", e.getMessage()));
                emitter.completeWithError(e);
            }
        });
    }

    /**
     * 校验并标准化 agentId。
     */
    private String normalizeAgentId(String agentId) {
        if (!StringUtils.hasText(agentId)) {
            throw new BizException(400, "agentId 不能为空");
        }
        return agentId.trim().toLowerCase();
    }

    /**
     * simple Agent：无状态问答。
     */
    private AgentGeneration runSimpleAgent(String conversationId,
                                           String messageId,
                                           String taskId,
                                           String query,
                                           Map<String, Object> metadata,
                                           SseEmitter emitter) {
        if (!StringUtils.hasText(query)) {
            throw new BizException(400, "query 不能为空");
        }

        String promptText = SIMPLE_TEMPLATE.apply(Map.of("query", query.trim())).text();
        return streamPromptToEmitter("simple", conversationId, messageId, taskId, promptText, metadata, emitter);
    }

    /**
     * memory Agent：按 conversationId 维护会话记忆。
     */
    private AgentGeneration runMemoryAgent(String conversationId,
                                           String messageId,
                                           String taskId,
                                           String query,
                                           Map<String, Object> metadata,
                                           SseEmitter emitter) {
        if (!StringUtils.hasText(query)) {
            throw new BizException(400, "query 不能为空");
        }

        ChatMemory memory = memoryStore.computeIfAbsent(
                conversationId,
                id -> MessageWindowChatMemory.withMaxMessages(properties.getMemoryWindowSize())
        );

        List<ChatMessage> historyBefore = new ArrayList<>(memory.messages());

        int historyTurns = countTurns(historyBefore);
        String historyText = summarizeHistory(historyBefore);
        String promptText = MEMORY_TEMPLATE.apply(Map.of(
                "history", historyText,
                "query", query.trim()
        )).text();

        AgentGeneration generation = streamPromptToEmitter("memory", conversationId, messageId, taskId, promptText, metadata, emitter);

        memory.add(UserMessage.from(query.trim()));
        memory.add(AiMessage.from(generation.answer()));
        metadata.put("historyTurns", historyTurns + 1);
        metadata.put("memoryWindowSize", properties.getMemoryWindowSize());
        metadata.put("conversationId", conversationId);
        return generation;
    }

    /**
     * skill Agent：执行工具路由并发出 tool_call/tool_result 事件。
     * <p>
     * 当前规则：
     * <p>1) 命中天气意图 -> weatherSkill</p>
     * <p>2) 命中用户统计意图 -> userCountSkill</p>
     * <p>3) 命中计算意图 -> calculatorSkill</p>
     * <p>4) 未命中 -> 返回可用工具提示</p>
     */
    private AgentGeneration runSkillAgent(String conversationId,
                                          String messageId,
                                          String taskId,
                                          String query,
                                          Map<String, Object> metadata,
                                          SseEmitter emitter) {
        if (!StringUtils.hasText(query)) {
            throw new BizException(400, "query 不能为空");
        }

        String normalized = query.trim().toLowerCase();
        List<Map<String, Object>> toolTrace = new ArrayList<>();

        if (normalized.contains("天气") || normalized.contains("weather")) {
            String city = extractCity(query);
            Map<String, Object> call = toolCallData(conversationId, messageId, taskId, "weather", Map.of("city", city));
            safeSend(emitter, "tool_call", call);

            String result = weatherSkill.queryWeather(city);
            Map<String, Object> toolResult = toolResultData(conversationId, messageId, taskId, "weather", result);
            safeSend(emitter, "tool_result", toolResult);

            toolTrace.add(Map.of("tool", "weather", "arguments", Map.of("city", city), "result", result));
            metadata.put("tools", toolTrace);
            String answer = "【Skill Agent】已调用天气工具。\n" + result;
            streamByTypewriter(answer, conversationId, messageId, taskId, emitter);
            return new AgentGeneration(answer, null);
        }

        if (normalized.contains("用户数") || normalized.contains("多少用户") || normalized.contains("user count")) {
            Map<String, Object> call = toolCallData(conversationId, messageId, taskId, "db_user_count", Map.of());
            safeSend(emitter, "tool_call", call);

            String result = userCountSkill.queryUserCount();
            Map<String, Object> toolResult = toolResultData(conversationId, messageId, taskId, "db_user_count", result);
            safeSend(emitter, "tool_result", toolResult);

            toolTrace.add(Map.of("tool", "db_user_count", "arguments", Map.of(), "result", result));
            metadata.put("tools", toolTrace);
            String answer = "【Skill Agent】已调用数据库工具。\n" + result;
            streamByTypewriter(answer, conversationId, messageId, taskId, emitter);
            return new AgentGeneration(answer, null);
        }

        String expression = extractExpression(query);
        if (StringUtils.hasText(expression) || normalized.contains("计算")) {
            String finalExpr = StringUtils.hasText(expression) ? expression : query.replace("计算", "").trim();
            Map<String, Object> call = toolCallData(conversationId, messageId, taskId, "calculator", Map.of("expression", finalExpr));
            safeSend(emitter, "tool_call", call);

            String result = calculatorSkill.calculate(finalExpr);
            Map<String, Object> toolResult = toolResultData(conversationId, messageId, taskId, "calculator", result);
            safeSend(emitter, "tool_result", toolResult);

            toolTrace.add(Map.of("tool", "calculator", "arguments", Map.of("expression", finalExpr), "result", result));
            metadata.put("tools", toolTrace);
            String answer = "【Skill Agent】已调用计算器工具。\n" + result;
            streamByTypewriter(answer, conversationId, messageId, taskId, emitter);
            return new AgentGeneration(answer, null);
        }

        metadata.put("tools", toolTrace);
        String answer = "【Skill Agent】当前支持 3 个工具：天气查询、用户数量查询、表达式计算。\n" +
                "你可以尝试：\n" +
                "1) 北京天气怎么样\n" +
                "2) 系统当前有多少用户\n" +
                "3) 计算 (12.5+7.5)*3";
        streamByTypewriter(answer, conversationId, messageId, taskId, emitter);
        return new AgentGeneration(answer, null);
    }

    /**
     * 调用模型并将 reasoning / content 分别流式发送到 SSE。
     */
    private AgentGeneration streamPromptToEmitter(String agentLabel,
                                                  String conversationId,
                                                  String messageId,
                                                  String taskId,
                                                  String promptText,
                                                  Map<String, Object> metadata,
                                                  SseEmitter emitter) {
        StringBuilder reasoningBuilder = new StringBuilder();
        StringBuilder answerBuilder = new StringBuilder();

        String streamedAnswer = agentClient.stream(promptText, new LangChainAgentClient.StreamChunkHandler() {
            @Override
            public void onReasoningDelta(String delta) {
                if (!StringUtils.hasText(delta)) {
                    return;
                }
                reasoningBuilder.append(delta);
                safeSend(emitter, "thinking", Map.of(
                        "agentId", agentLabel,
                        "conversationId", conversationId,
                        "messageId", messageId,
                        "taskId", taskId,
                        "thinking", delta
                ));
            }

            @Override
            public void onContentDelta(String delta) {
                if (!StringUtils.hasText(delta)) {
                    return;
                }
                answerBuilder.append(delta);
                safeSend(emitter, "message", Map.of(
                        "agentId", agentLabel,
                        "conversationId", conversationId,
                        "messageId", messageId,
                        "taskId", taskId,
                        "answer", delta
                ));
            }
        });

        String answer = StringUtils.hasText(answerBuilder.toString())
                ? answerBuilder.toString()
                : streamedAnswer;
        if (!StringUtils.hasText(answer) && StringUtils.hasText(reasoningBuilder.toString())) {
            answer = reasoningBuilder.toString();
        }

        if (!StringUtils.hasText(answer)) {
            throw new BizException(500, "模型未返回有效内容");
        }

        metadata.put("answerLength", answer.length());
        metadata.put("reasoningLength", reasoningBuilder.length());
        return new AgentGeneration(answer, reasoningBuilder.toString());
    }

    /**
     * 构造 tool_call 事件数据。
     */
    private Map<String, Object> toolCallData(String conversationId,
                                             String messageId,
                                             String taskId,
                                             String toolName,
                                             Map<String, Object> args) {
        Map<String, Object> data = new HashMap<>();
        data.put("conversationId", conversationId);
        data.put("messageId", messageId);
        data.put("taskId", taskId);
        data.put("toolName", toolName);
        data.put("arguments", args);
        return data;
    }

    /**
     * 构造 tool_result 事件数据。
     */
    private Map<String, Object> toolResultData(String conversationId,
                                               String messageId,
                                               String taskId,
                                               String toolName,
                                               String result) {
        Map<String, Object> data = new HashMap<>();
        data.put("conversationId", conversationId);
        data.put("messageId", messageId);
        data.put("taskId", taskId);
        data.put("toolName", toolName);
        data.put("result", result);
        return data;
    }

    /**
     * 提取天气问题中的城市。
     */
    private String extractCity(String query) {
        Matcher matcher = CITY_PATTERN.matcher(query);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return properties.getDefaultCity();
    }

    /**
     * 从自然语言中提取数学表达式。
     */
    private String extractExpression(String query) {
        Matcher matcher = CALC_PATTERN.matcher(query.replace("计算", ""));
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * 统计历史中的用户发言轮次。
     */
    private int countTurns(List<ChatMessage> history) {
        int userCount = 0;
        for (ChatMessage message : history) {
            if (message instanceof UserMessage) {
                userCount++;
            }
        }
        return userCount;
    }

    /**
     * 生成历史摘要文本。
     */
    private String summarizeHistory(List<ChatMessage> history) {
        if (history.isEmpty()) {
            return "暂无历史对话";
        }

        List<String> lines = new ArrayList<>();
        for (ChatMessage message : history) {
            if (message instanceof UserMessage userMessage) {
                lines.add("用户: " + safeText(userMessage.singleText()));
            } else if (message instanceof AiMessage aiMessage) {
                lines.add("助手: " + safeText(aiMessage.text()));
            }
        }

        if (lines.isEmpty()) {
            return "暂无可读历史";
        }

        int start = Math.max(0, lines.size() - 6);
        return String.join(" | ", lines.subList(start, lines.size()));
    }

    /**
     * 截断过长文本，避免历史摘要过大。
     */
    private String safeText(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        return text.length() > 80 ? text.substring(0, 80) + "..." : text;
    }

    /**
     * 按字符逐步发送 message 事件，模拟打字机效果。
     */
    private void streamByTypewriter(String answer,
                                    String conversationId,
                                    String messageId,
                                    String taskId,
                                    SseEmitter emitter) {
        if (!StringUtils.hasText(answer)) {
            return;
        }

        int delay = properties.getStreamDelayMs() == null ? 25 : Math.max(0, properties.getStreamDelayMs());
        for (int i = 0; i < answer.length(); i++) {
            String chunk = String.valueOf(answer.charAt(i));
            Map<String, Object> data = new HashMap<>();
            data.put("answer", chunk);
            data.put("conversationId", conversationId);
            data.put("messageId", messageId);
            data.put("taskId", taskId);
            safeSend(emitter, "message", data);

            if (delay > 0) {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new BizException(500, "流式输出被中断");
                }
            }
        }
    }

    /**
     * 安全发送 SSE 事件。
     * <p>若连接中断，仅记录日志避免主流程崩溃。</p>
     */
    private void safeSend(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data));
        } catch (Exception e) {
            log.warn("Agent SSE 发送失败: {}", e.getMessage());
        }
    }

    /**
     * 模型生成结果。
     */
    private record AgentGeneration(String answer, String reasoning) {
    }
}
