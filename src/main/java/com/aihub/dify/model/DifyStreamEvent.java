package com.aihub.dify.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
public class DifyStreamEvent {

    private String event;

    private String answer;

    @JsonProperty("conversation_id")
    private String conversationId;

    @JsonProperty("message_id")
    private String messageId;

    @JsonProperty("task_id")
    private String taskId;

    private Map<String, Object> metadata;

    private Integer status;

    private String code;

    private String message;
}