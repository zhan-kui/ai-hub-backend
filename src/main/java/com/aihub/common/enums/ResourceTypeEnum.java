package com.aihub.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResourceTypeEnum {
    APP("app", "Dify 应用"),
    KNOWLEDGE("knowledge", "Dify 知识库");

    private final String code;
    private final String name;

    public static ResourceTypeEnum fromCode(String code) {
        for (ResourceTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知资源类型: " + code);
    }
}
