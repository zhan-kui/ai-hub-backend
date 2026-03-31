package com.aihub.service;

import com.aihub.config.JwtProperties;
import com.aihub.dto.auth.LoginRequest;
import com.aihub.dto.auth.LoginResponse;
import com.aihub.repository.UserRepository;
import com.aihub.security.CustomUserDetails;
import com.aihub.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final TokenBlacklistService tokenBlacklistService;
    private final UserRepository userRepository;
    private final CaptchaService captchaService;

    public LoginResponse login(LoginRequest request) {
        captchaService.verifyAndConsume(request.getCaptchaId(), request.getCaptchaCode());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String token = jwtUtil.generateToken(userDetails);

        userRepository.updateLastLoginTime(userDetails.getUserId(), LocalDateTime.now());

        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getExpiration() / 1000)
                .userId(userDetails.getUserId())
                .username(userDetails.getUsername())
                .roleCode(userDetails.getRoleCode())
                .build();
    }

    public void logout(String token) {
        tokenBlacklistService.addToBlacklist(token);
    }
}