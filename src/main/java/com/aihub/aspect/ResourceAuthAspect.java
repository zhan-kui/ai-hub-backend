package com.aihub.aspect;

import com.aihub.annotation.RequireResource;
import com.aihub.common.exception.BizException;
import com.aihub.security.CustomUserDetails;
import com.aihub.security.SecurityUtils;
import com.aihub.service.ResourceAuthService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class ResourceAuthAspect {

    private final ResourceAuthService resourceAuthService;

    @Around("@annotation(requireResource)")
    public Object check(ProceedingJoinPoint point,
                        RequireResource requireResource) throws Throwable {
        CustomUserDetails user = SecurityUtils.getCurrentUser();

        Long resourceId = extractResourceId(point, requireResource.paramName());
        if (resourceId == null) {
            return point.proceed();
        }

        boolean allowed = resourceAuthService.hasPermission(
                user.getUserId(),
                user.getRoleCode(),
                requireResource.type(),
                resourceId
        );

        if (!allowed) {
            throw new BizException(403, "无权限操作此资源");
        }

        return point.proceed();
    }

    private Long extractResourceId(ProceedingJoinPoint point, String paramName) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        String[] names = signature.getParameterNames();
        Object[] args = point.getArgs();

        for (int i = 0; i < names.length; i++) {
            if (paramName.equals(names[i])) {
                Object value = args[i];
                if (value instanceof Long longValue) {
                    return longValue;
                }
                if (value instanceof Integer intValue) {
                    return intValue.longValue();
                }
                if (value instanceof String str && !str.isBlank()) {
                    try {
                        return Long.parseLong(str);
                    } catch (NumberFormatException ignored) {
                        return null;
                    }
                }
            }
        }
        return null;
    }
}