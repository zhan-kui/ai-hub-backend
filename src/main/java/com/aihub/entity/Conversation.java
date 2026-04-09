package com.aihub.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "conversation")
@TableName("conversation")
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 新增此行，解决 JPA 的 persist() 报错问题
    @TableId(type = IdType.AUTO)
    private Long id;

    @Column(name = "user_id", nullable = false)
    @TableField("user_id")
    private Long userId;

    @Column(name = "app_config_id")
    @TableField("app_config_id")
    private Long appConfigId;

    @Column(name = "dify_conversation_id", length = 100)
    @TableField("dify_conversation_id")
    private String difyConversationId;

    @Column(length = 255)
    private String title;

    @Column(columnDefinition = "TINYINT")
    private Integer status;

    @Column(columnDefinition = "TINYINT")
    @TableLogic
    private Boolean deleted;

    @Column(name = "created_at", updatable = false)
    @TableField("created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}