package com.aihub.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RoleEnum {
    SUPER_ADMIN("super_admin", "超级管理员"),
    ADMIN("admin", "管理员"),
    USER("user", "普通用户");

    private final String code;
    private final String name;

    public boolean isAdmin() {
        return this == SUPER_ADMIN || this == ADMIN;
    }

    public static RoleEnum fromCode(String code) {
        for (RoleEnum role : values()) {
            if (role.getCode().equals(code)) {
                return role;
            }
        }
        throw new IllegalArgumentException("未知角色编码: " + code);
    }
}
