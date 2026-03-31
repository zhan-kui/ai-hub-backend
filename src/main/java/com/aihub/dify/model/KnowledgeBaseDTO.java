package com.aihub.dify.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KnowledgeBaseDTO {

    private String id;

    private String name;

    private String description;

    private Integer documentCount;

    private Long wordCount;
}