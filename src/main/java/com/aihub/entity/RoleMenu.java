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
/**
 * RoleMenu class。
 * <p>该类型承担当前文件的核心职责，建议结合同包相关类一起阅读。
 */

@Data
@Entity
@Table(name = "sys_role_menu")
@TableName("sys_role_menu")
public class RoleMenu {
    /**
     * 字段：id。
     * <p>用于承载当前对象的状态数据，具体业务语义请结合上下文与调用方理解。
     */

    @Id
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 字段：roleId。
     * <p>用于承载当前对象的状态数据，具体业务语义请结合上下文与调用方理解。
     */

    @Column(name = "role_id", nullable = false)
    @TableField("role_id")
    private Long roleId;
    /**
     * 字段：menuId。
     * <p>用于承载当前对象的状态数据，具体业务语义请结合上下文与调用方理解。
     */

    @Column(name = "menu_id", nullable = false)
    @TableField("menu_id")
    private Long menuId;
    /**
     * 字段：createdAt。
     * <p>用于承载当前对象的状态数据，具体业务语义请结合上下文与调用方理解。
     */

    @Column(name = "created_at", updatable = false)
    @TableField("created_at")
    private LocalDateTime createdAt;
}