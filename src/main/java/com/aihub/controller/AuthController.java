package com.aihub.controller;

import com.aihub.common.result.R;
import com.aihub.dto.auth.CaptchaResponse;
import com.aihub.dto.auth.LoginRequest;
import com.aihub.dto.auth.LoginResponse;
import com.aihub.service.AuthService;
import com.aihub.service.CaptchaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理")
public class AuthController {

    private final AuthService authService;
    private final CaptchaService captchaService;

    @GetMapping("/captcha/image")
    @Operation(summary = "获取图形验证码")
    public R<CaptchaResponse> imageCaptcha(HttpServletRequest request) {
        return R.ok(captchaService.generateImageCaptcha(resolveClientIp(request)));
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return R.ok(authService.login(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "用户登出")
    public R<Void> logout(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            authService.logout(authHeader.substring(7));
        }
        return R.ok();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}