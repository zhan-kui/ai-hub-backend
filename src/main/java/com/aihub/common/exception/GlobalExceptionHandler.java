package com.aihub.common.exception;

import com.aihub.common.result.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public R<Void> handleBiz(BizException ex) {
        return R.fail(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleValid(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("参数校验失败");
        return R.fail(400, msg);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public R<Void> handleAccessDenied(AccessDeniedException ex) {
        return R.fail(403, "权限不足");
    }

    @ExceptionHandler(AuthenticationException.class)
    public R<Void> handleAuth(AuthenticationException ex) {
        return R.fail(401, "认证失败: " + ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception ex) {
        log.error("系统异常", ex);
        return R.fail(500, "系统异常，请稍后重试");
    }
}
