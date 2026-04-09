package com.aihub.controller;

import com.aihub.dto.agent.AgentChatRequest;
import com.aihub.security.SecurityUtils;
import com.aihub.service.AgentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * LangChain4j 智能体入口控制器。
 * <p>
 * 统一对外提供 `/agents/stream` 流式接口，通过 `agentId` 路由到不同 Agent 实现：
 * <p>1) simple：无状态问答</p>
 * <p>2) memory：多轮记忆问答</p>
 * <p>3) skill：带 Tool 调用的智能体</p>
 */
@RestController
@RequestMapping("/agents")
@RequiredArgsConstructor
@Tag(name = "LangChain4j 智能体")
public class AgentController {

    /**
     * 智能体业务编排服务。
     */
    private final AgentService agentService;

    /**
     * 流式对话接口。
     * <p>
     * SSE 事件格式与现有 `/chat/stream` 保持一致，核心事件为：
     * <p>- thinking</p>
     * <p>- message</p>
     * <p>- message_end</p>
     * <p>- error</p>
     * <p>当使用 skill Agent 时，额外包含：</p>
     * <p>- tool_call</p>
     * <p>- tool_result</p>
     *
     * @param agentId 智能体类型：simple / memory / skill
     * @param request 对话请求参数
     * @return SSE 发射器
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "智能体流式对话（simple/memory/skill）")
    public SseEmitter stream(@RequestParam String agentId,
                             @Valid @RequestBody AgentChatRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        SseEmitter emitter = new SseEmitter(180_000L);
        emitter.onTimeout(emitter::complete);
        emitter.onError(ex -> emitter.complete());
        agentService.streamChat(agentId, userId, request, emitter);
        return emitter;
    }
}
