package com.aihub.dify.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MessageVO {

    private String messageId;

    private String conversationId;

    private String query;

    private String answer;

    private LocalDateTime createdAt;
}