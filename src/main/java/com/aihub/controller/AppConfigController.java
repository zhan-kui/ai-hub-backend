package com.aihub.controller;

import com.aihub.common.enums.ResourceTypeEnum;
import com.aihub.common.result.R;
import com.aihub.dto.app.AppConfigVO;
import com.aihub.dto.app.CreateAppRequest;
import com.aihub.annotation.RequireResource;
import com.aihub.security.SecurityUtils;
import com.aihub.service.AppConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/apps")
@RequiredArgsConstructor
@Tag(name = "应用管理")
public class AppConfigController {

    private final AppConfigService appConfigService;

    @GetMapping("/list")
    @Operation(summary = "获取应用列表（按权限过滤）")
    public R<List<AppConfigVO>> list() {
        Long userId = SecurityUtils.getCurrentUserId();
        String roleCode = SecurityUtils.getCurrentRoleCode();
        return R.ok(appConfigService.listByPermission(userId, roleCode));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取应用详情")
    @RequireResource(type = ResourceTypeEnum.APP, paramName = "id")
    public R<AppConfigVO> detail(@PathVariable Long id) {
        return R.ok(appConfigService.getDetail(id));
    }

    @PostMapping("/create")
    @Operation(summary = "创建应用（管理员）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public R<AppConfigVO> create(@Valid @RequestBody CreateAppRequest request) {
        return R.ok(appConfigService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新应用（管理员）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public R<AppConfigVO> update(@PathVariable Long id,
                                 @Valid @RequestBody CreateAppRequest request) {
        return R.ok(appConfigService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除应用（管理员）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public R<Void> delete(@PathVariable Long id) {
        appConfigService.delete(id);
        return R.ok();
    }

    @GetMapping("/{id}/parameters")
    @Operation(summary = "获取 Dify 应用参数")
    @RequireResource(type = ResourceTypeEnum.APP, paramName = "id")
    public R<Object> getParameters(@PathVariable Long id) {
        return R.ok(appConfigService.getDifyParameters(id));
    }
}