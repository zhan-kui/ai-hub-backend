package com.aihub.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "user_resource_auth")
@TableName("user_resource_auth")
public class UserResourceAuth {

    @Id
    @TableId(type = IdType.AUTO)
    private Long id;

    @Column(name = "user_id", nullable = false)
    @TableField("user_id")
    private Long userId;

    @Column(name = "resource_type", nullable = false, length = 30)
    @TableField("resource_type")
    private String resourceType;

    @Column(name = "resource_id", nullable = false)
    @TableField("resource_id")
    private Long resourceId;

    @Column(name = "granted_by")
    @TableField("granted_by")
    private Long grantedBy;

    @Column(name = "expire_at")
    @TableField("expire_at")
    private LocalDateTime expireAt;

    @Column(columnDefinition = "TINYINT")
    private Integer status;

    @Column(name = "created_at", updatable = false)
    @TableField("created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}