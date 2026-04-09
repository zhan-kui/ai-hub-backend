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

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "chat_message")
@TableName("chat_message")
public class ChatMessage {

    @Id
    @TableId(type = IdType.AUTO)
    private Long id;

    @Column(name = "conversation_id", nullable = false)
    @TableField("conversation_id")
    private Long conversationId;

    @Column(name = "user_id", nullable = false)
    @TableField("user_id")
    private Long userId;

    @Column(name = "app_config_id")
    @TableField("app_config_id")
    private Long appConfigId;

    @Column(name = "dify_message_id", length = 100)
    @TableField("dify_message_id")
    private String difyMessageId;

    @Column(name = "dify_conversation_id", length = 100)
    @TableField("dify_conversation_id")
    private String difyConversationId;

    @Column(nullable = false, length = 20)
    private String role;

    @Column(columnDefinition = "TEXT")
    private String query;

    @Column(columnDefinition = "LONGTEXT")
    private String answer;

    @Column(name = "prompt_tokens")
    @TableField("prompt_tokens")
    private Integer promptTokens;

    @Column(name = "completion_tokens")
    @TableField("completion_tokens")
    private Integer completionTokens;

    @Column(name = "total_tokens")
    @TableField("total_tokens")
    private Integer totalTokens;

    @Column(name = "total_price", precision = 10, scale = 6)
    @TableField("total_price")
    private BigDecimal totalPrice;

    @Column(precision = 10, scale = 3)
    private BigDecimal latency;

    @Column(length = 20)
    private String feedback;

    @Column(columnDefinition = "TINYINT")
    @TableLogic
    private Boolean deleted;

    @Column(name = "created_at", updatable = false)
    @TableField("created_at")
    private LocalDateTime createdAt;

    // 增加思考过程字段配置 (推荐使用 text 类型防止思考过程过长装不下)
    @Column(name = "thought", columnDefinition = "TEXT")
    private String thought;
}