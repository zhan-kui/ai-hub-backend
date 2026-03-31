package com.aihub.dto.knowledge;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class KnowledgeBaseVO {

    private Long id;

    private String kbName;

    private String kbCode;

    private String description;

    private String icon;

    private Integer documentCount;

    private Long wordCount;

    private String indexingTechnique;

    private String embeddingModel;

    private Integer sort;

    private Boolean enabled;

    private LocalDateTime createdAt;
}