package com.aihub.service;

import com.aihub.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "aihub:token:blacklist:";

    private final StringRedisTemplate stringRedisTemplate;
    private final JwtUtil jwtUtil;

    /**
     * 将 token 加入黑名单（登出时调用）
     */
    public void addToBlacklist(String token) {
        long expireMillis = jwtUtil.getExpireMillis(token);
        if (expireMillis > 0) {
            stringRedisTemplate.opsForValue()
                    .set(BLACKLIST_PREFIX + token, "1", expireMillis, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * 检查 token 是否在黑名单中
     */
    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(BLACKLIST_PREFIX + token));
    }
}