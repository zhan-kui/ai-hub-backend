package com.aihub.dto.menu;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
/**
 * MenuSaveRequest class。
 * <p>该类型承担当前文件的核心职责，建议结合同包相关类一起阅读。
 */

@Data
public class MenuSaveRequest {
    /**
     * 字段：parentId。
     * <p>用于承载当前对象的状态数据，具体业务语义请结合上下文与调用方理解。
     */

    private Long parentId;
    /**
     * 字段：menuName。
     * <p>用于承载当前对象的状态数据，具体业务语义请结合上下文与调用方理解。
     */

    @NotBlank(message = "菜单名称不能为空")
    private String menuName;
    /**
     * 字段：menuType。
     * <p>用于承载当前对象的状态数据，具体业务语义请结合上下文与调用方理解。
     */

    @NotBlank(message = "菜单类型不能为空")
    private String menuType;
    /**
     * 字段：path。
     * <p>用于承载当前对象的状态数据，具体业务语义请结合上下文与调用方理解。
     */

    private String path;
    /**
     * 字段：component。
     * <p>用于承载当前对象的状态数据，具体业务语义请结合上下文与调用方理解。
     */

    private String component;
    /**
     * 字段：permission。
     * <p>用于承载当前对象的状态数据，具体业务语义请结合上下文与调用方理解。
     */

    private String permission;
    /**
     * 字段：icon。
     * <p>用于承载当前对象的状态数据，具体业务语义请结合上下文与调用方理解。
     */

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

    private Boolean visible;
    /**
     * 字段：status。
     * <p>用于承载当前对象的状态数据，具体业务语义请结合上下文与调用方理解。
     */

    private Boolean status;

    private String platform;
}
