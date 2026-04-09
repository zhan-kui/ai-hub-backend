package com.aihub.dto.menu;

import lombok.Data;

import java.util.List;
/**
 * RoleMenuAssignRequest class。
 * <p>该类型承担当前文件的核心职责，建议结合同包相关类一起阅读。
 */

@Data
public class RoleMenuAssignRequest {
    /**
     * 字段：menuIds。
     * <p>用于承载当前对象的状态数据，具体业务语义请结合上下文与调用方理解。
     */

    private List<Long> menuIds;
}