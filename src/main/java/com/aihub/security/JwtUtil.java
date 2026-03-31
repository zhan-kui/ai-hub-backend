package com.aihub.security;

import com.aihub.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties jwtProperties;
    private SecretKey secretKey;

    @PostConstruct
    private void init() {
        // 密钥只解析一次，避免每次调用都创建
        this.secretKey = Keys.hmacShaKeyFor(
                jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(CustomUserDetails userDetails) {
        Date now = new Date();
        Date expire = new Date(now.getTime() + jwtProperties.getExpiration());

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("uid", userDetails.getUserId())
                .claim("role", userDetails.getRoleCode())
                .issuedAt(now)
                .expiration(expire)
                .signWith(secretKey)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception ex) {
            return false;
        }
    }

    public String getUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public Long getUserId(String token) {
        return parseClaims(token).get("uid", Long.class);
    }

    public String getRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    /**
     * 获取 token 剩余有效期（毫秒），用于 Token 黑名单 TTL 设置
     */
    public long getExpireMillis(String token) {
        Claims claims = parseClaims(token);
        return claims.getExpiration().getTime() - System.currentTimeMillis();
    }
}