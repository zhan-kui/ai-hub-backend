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
/**
 * Menu class。
 * <p>该类型承担当前文件的核心职责，建议结合同包相关类一起阅读。
 */

@Data
@Entity
@Table(name = "sys_menu")
@TableName("sys_menu")
public class Menu {
    /**
     * 字段：id。
     * <p>用于承载当前对象的状态数据，具体业务语义请结合上下文与调用方理解。
     */

    @Id
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 字段：parentId。
     * <p>用于承载当前对象的状态数据，具体业务语义请结合上下文与调用方理解。
     */

    @Column(name = "parent_id", nullable = false)
    @TableField("parent_id")
    private Long parentId;
    /**
     * 字段：menuName。
     * <p>用于承载当前对象的状态数据，具体业务语义请结合上下文与调用方理解。
     */

    @Column(name = "menu_name", nullable = false, length = 100)
    @TableField("menu_name")
    private String menuName;
    /**
     * 字段：menuType。
     * <p>用于承载当前对象的状态数据，具体业务语义请结合上下文与调用方理解。
     */

    @Column(name = "menu_type", nullable = false, length = 20)
    @TableField("menu_type")
    private String menuType;
    /**
     * 字段：path。
     * <p>用于承载当前对象的状态数据，具体业务语义请结合上下文与调用方理解。
     */

    @Column(length = 255)
    private String path;
    /**
     * 字段：component。
     * <p>用于承载当前对象的状态数据，具体业务语义请结合上下文与调用方理解。
     */

    @Column(length = 255)
    private String component;
    /**
     * 字段：permission。
     * <p>用于承载当前对象的状态数据，具体业务语义请结合上下文与调用方理解。
     */

    @Column(length = 100)
    private String permission;
    /**
     * 字段：icon。
     * <p>用于承载当前对象的状态数据，具体业务语义请结合上下文与调用方理解。
     */

    @Column(length = 100)
    private String icon;
    /**
     * 字段：sort。
     * <p>用于承载当前对象的状态数据，具体业务语义请结合上下文与调用方理解。
     */

    private Integer sort;
    /**
     * 字段：visible。
     * <p>用于承载当前对象的状态数据，具体业务语义请结合上下文与调用方理解。
     */

    @Column(columnDefinition = "TINYINT")
    private Boolean visible;
    /**
     * 字段：status。
     * <p>用于承载当前对象的状态数据，具体业务语义请结合上下文与调用方理解。
     */

    @Column(columnDefinition = "TINYINT")
    private Boolean status;

    @Column(name = "platform", nullable = false, length = 20)
    @TableField("platform")
    private String platform;
    /**
     * 字段：deleted。
     * <p>用于承载当前对象的状态数据，具体业务语义请结合上下文与调用方理解。
     */

    @Column(columnDefinition = "TINYINT")
    @TableLogic
    private Boolean deleted;
    /**
     * 字段：createdAt。
     * <p>用于承载当前对象的状态数据，具体业务语义请结合上下文与调用方理解。
     */

    @Column(name = "created_at", updatable = false)
    @TableField("created_at")
    private LocalDateTime createdAt;
    /**
     * 字段：updatedAt。
     * <p>用于承载当前对象的状态数据，具体业务语义请结合上下文与调用方理解。
     */

    @Column(name = "updated_at")
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
