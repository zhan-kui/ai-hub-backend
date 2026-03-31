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
@Table(name = "app_config")
@TableName("app_config")
public class AppConfig {

    @Id
    @TableId(type = IdType.AUTO)
    private Long id;

    @Column(name = "app_name", nullable = false, length = 100)
    @TableField("app_name")
    private String appName;

    @Column(name = "app_code", nullable = false, length = 50, unique = true)
    @TableField("app_code")
    private String appCode;

    @Column(name = "app_type", nullable = false, length = 30)
    @TableField("app_type")
    private String appType;

    @Column(name = "dify_app_id", length = 100)
    @TableField("dify_app_id")
    private String difyAppId;

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