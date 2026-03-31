package com.aihub.common.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class R<T> {
    private Integer code;
    private String message;
    private T data;

    public static <T> R<T> ok(T data) {
        return R.<T>builder().code(200).message("success").data(data).build();
    }

    public static R<Void> ok() {
        return R.<Void>builder().code(200).message("success").build();
    }

    public static <T> R<T> fail(Integer code, String message) {
        return R.<T>builder().code(code).message(message).build();
    }
}