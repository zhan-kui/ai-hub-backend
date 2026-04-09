package com.aihub.dto.agent;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * 智能体对话请求参数。
 * <p>
 * 该对象用于 `/agents/stream` 接口，支持 simple/memory/skill 三种 Agent。
 */
@Data
public class AgentChatRequest {

    /**
     * 用户输入问题。
     */
    @NotBlank(message = "消息不能为空")
    private String query;

    /**
     * 会话 ID。
     * <p>memory/skill 推荐传入固定 conversationId，以维持会话上下文。</p>
     */
    private String conversationId;

    /**
     * 可选的扩展输入。
     * <p>预留给后续 Prompt 变量注入、前端上下文透传等场景。</p>
     */
    private Map<String, Object> inputs;
}