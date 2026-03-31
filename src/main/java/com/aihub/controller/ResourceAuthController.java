package com.aihub.controller;

import com.aihub.common.enums.ResourceTypeEnum;
import com.aihub.common.result.R;
import com.aihub.dto.resource.GrantRequest;
import com.aihub.security.SecurityUtils;
import com.aihub.service.ResourceAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/resource-auth")
@RequiredArgsConstructor
@Tag(name = "资源授权管理")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public class ResourceAuthController {

    private final ResourceAuthService resourceAuthService;

    @PostMapping("/grant")
    @Operation(summary = "批量授权资源给用户")
    public R<Void> grant(@Valid @RequestBody GrantRequest request) {
        Long grantedBy = SecurityUtils.getCurrentUserId();
        resourceAuthService.grant(
                request.getUserId(),
                request.getResourceType(),
                request.getResourceIds(),
                grantedBy
        );
        return R.ok();
    }

    @PostMapping("/revoke")
    @Operation(summary = "撤销用户资源授权")
    public R<Void> revoke(@RequestParam Long userId,
                          @RequestParam ResourceTypeEnum resourceType) {
        resourceAuthService.grant(userId, resourceType, List.of(), SecurityUtils.getCurrentUserId());
        return R.ok();
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "查询用户已授权的资源 ID 列表")
    public R<Map<String, List<Long>>> getUserAuthorizations(@PathVariable Long userId) {
        Map<String, List<Long>> result = new HashMap<>();
        result.put("app", resourceAuthService.getAuthorizedResourceIds(
                userId, "user", ResourceTypeEnum.APP));
        result.put("knowledge", resourceAuthService.getAuthorizedResourceIds(
                userId, "user", ResourceTypeEnum.KNOWLEDGE));
        return R.ok(result);
    }
}