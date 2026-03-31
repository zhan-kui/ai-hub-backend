package com.aihub.service;

import com.aihub.common.enums.ResourceTypeEnum;
import com.aihub.mapper.UserResourceAuthMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResourceAuthService {

    private static final String AUTH_CACHE_PREFIX = "aihub:auth:";

    private final UserResourceAuthMapper authMapper;
    private final StringRedisTemplate stringRedisTemplate;

    public boolean hasPermission(Long userId, String roleCode,
                                 ResourceTypeEnum type, Long resourceId) {
        if ("super_admin".equals(roleCode) || "admin".equals(roleCode)) {
            return true;
        }
        return authMapper.existsValidAuth(userId, type.getCode(), resourceId);
    }

    /**
     * @return null 表示全部可访问（管理员），空列表表示无权限。
     */
    public List<Long> getAuthorizedResourceIds(Long userId, String roleCode,
                                               ResourceTypeEnum type) {
        if ("super_admin".equals(roleCode) || "admin".equals(roleCode)) {
            return null;
        }
        return authMapper.selectValidResourceIds(userId, type.getCode());
    }

    @Transactional(rollbackFor = Exception.class)
    public void grant(Long userId, ResourceTypeEnum type,
                      List<Long> resourceIds, Long grantedBy) {
        authMapper.revokeByUserAndType(userId, type.getCode());

        if (resourceIds != null && !resourceIds.isEmpty()) {
            for (Long resourceId : resourceIds) {
                authMapper.upsertAuth(userId, type.getCode(), resourceId, grantedBy);
            }
        }

        clearAuthCache(userId);
    }

    public void clearAuthCache(Long userId) {
        stringRedisTemplate.delete(AUTH_CACHE_PREFIX + userId + ":app");
        stringRedisTemplate.delete(AUTH_CACHE_PREFIX + userId + ":knowledge");
    }
}