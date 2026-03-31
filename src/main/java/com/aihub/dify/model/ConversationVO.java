package com.aihub.dify.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ConversationVO {

    private String conversationId;

    private String name;

    private Integer messageCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}