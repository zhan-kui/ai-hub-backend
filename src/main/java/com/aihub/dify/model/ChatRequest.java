package com.aihub.dify.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data
public class ChatRequest {

    @NotBlank(message = "消息不能为空")
    private String query;

    private String conversationId;

    private Map<String, Object> inputs;
}