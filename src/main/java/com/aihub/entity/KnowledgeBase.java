package com.aihub.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "knowledge_base")
@TableName("knowledge_base")
public class KnowledgeBase {

    @Id
    @TableId(type = IdType.AUTO)
    private Long id;

    @Column(name = "kb_name", nullable = false, length = 100)
    @TableField("kb_name")
    private String kbName;

    @Column(name = "kb_code", nullable = false, length = 50, unique = true)
    @TableField("kb_code")
    private String kbCode;

    @Column(name = "dify_dataset_id", nullable = false, length = 100)
    @TableField("dify_dataset_id")
    private String difyDatasetId;

    @Column(name = "dify_api_key", nullable = false, length = 255)
    @TableField("dify_api_key")
    private String difyApiKey;

    @Column(name = "dify_base_url", length = 500)
    @TableField("dify_base_url")
    private String difyBaseUrl;

    @Column(length = 500)
    private String description;

    @Column(length = 500)
    private String icon;

    @Column(name = "document_count")
    @TableField("document_count")
    private Integer documentCount;

    @Column(name = "word_count")
    @TableField("word_count")
    private Long wordCount;

    @Column(name = "indexing_technique", length = 30)
    @TableField("indexing_technique")
    private String indexingTechnique;

    @Column(name = "embedding_model", length = 100)
    @TableField("embedding_model")
    private String embeddingModel;

    private Integer sort;

    @Column(columnDefinition = "TINYINT")
    private Boolean enabled;

    @Column(columnDefinition = "TINYINT")
    @TableLogic
    private Boolean deleted;

    @Column(name = "created_by")
    @TableField("created_by")
    private Long createdBy;

    @Column(name = "created_at", updatable = false)
    @TableField("created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}