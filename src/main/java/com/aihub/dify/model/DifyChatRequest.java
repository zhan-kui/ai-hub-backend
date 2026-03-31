package com.aihub.dify.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class DifyChatRequest {

    private Map<String, Object> inputs;

    private String query;

    private String user;

    @JsonProperty("response_mode")
    private String responseMode;

    @JsonProperty("conversation_id")
    private String conversationId;
}