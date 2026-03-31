package com.aihub.security;

import com.aihub.common.exception.BizException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static CustomUserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !(authentication.getPrincipal() instanceof CustomUserDetails user)) {
            throw new BizException(401, "未登录");
        }
        return user;
    }

    public static Long getCurrentUserId() {
        return getCurrentUser().getUserId();
    }

    public static String getCurrentRoleCode() {
        return getCurrentUser().getRoleCode();
    }

    public static boolean isAdmin() {
        String roleCode = getCurrentRoleCode();
        return "super_admin".equals(roleCode) || "admin".equals(roleCode);
    }
}