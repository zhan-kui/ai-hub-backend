package com.aihub.dto.knowledge;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateKnowledgeBaseRequest {

    @NotBlank(message = "知识库名称不能为空")
    private String kbName;

    @NotBlank(message = "知识库编码不能为空")
    private String kbCode;

    @NotBlank(message = "Dify API Key 不能为空")
    private String difyApiKey;

    private String difyDatasetId;

    private String difyBaseUrl;

    private String description;

    private String icon;

    private String indexingTechnique;

    private String embeddingModel;

    private Integer sort;
}