package com.aihub.controller;

import com.aihub.common.result.R;
import com.aihub.dto.menu.MenuSaveRequest;
import com.aihub.dto.menu.MenuVO;
import com.aihub.dto.menu.RoleMenuAssignRequest;
import com.aihub.service.MenuService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/menus")
@RequiredArgsConstructor
@Tag(name = "菜单权限管理")
public class MenuController {

    private final MenuService menuService;

    @GetMapping("/tree")
    @Operation(summary = "当前用户菜单树")
    public R<List<MenuVO>> tree(@RequestParam(required = false) String platform) {
        return R.ok(menuService.getCurrentUserMenuTree(platform));
    }

    @GetMapping("/permissions")
    @Operation(summary = "当前用户权限码")
    public R<List<String>> permissions(@RequestParam(required = false) String platform) {
        return R.ok(menuService.getCurrentUserPermissions(platform));
    }

    @GetMapping("/list")
    @Operation(summary = "全量菜单列表")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public R<List<MenuVO>> list(@RequestParam(required = false) String platform) {
        return R.ok(menuService.listAllMenus(platform));
    }

    @PostMapping
    @Operation(summary = "创建菜单")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public R<MenuVO> create(@Valid @RequestBody MenuSaveRequest request) {
        return R.ok(menuService.create(request));
    }

    @PutMapping
    @Operation(summary = "更新菜单（兼容模式）")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public R<MenuVO> updateByParam(@RequestParam Long id,
                                   @Valid @RequestBody MenuSaveRequest request) {
        return R.ok(menuService.update(id, request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新菜单")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public R<MenuVO> update(@PathVariable Long id,
                            @Valid @RequestBody MenuSaveRequest request) {
        return R.ok(menuService.update(id, request));
    }

    @DeleteMapping
    @Operation(summary = "删除菜单（兼容模式）")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public R<Void> deleteByParam(@RequestParam Long id) {
        menuService.delete(id);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除菜单")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public R<Void> delete(@PathVariable Long id) {
        menuService.delete(id);
        return R.ok();
    }

    @GetMapping("/role/{roleId}")
    @Operation(summary = "获取角色菜单 ID")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public R<List<Long>> getRoleMenus(@PathVariable Long roleId) {
        return R.ok(menuService.getRoleMenuIds(roleId));
    }

    @PostMapping("/role/{roleId}")
    @Operation(summary = "分配角色菜单")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public R<Void> assignRoleMenus(@PathVariable Long roleId,
                                   @RequestBody RoleMenuAssignRequest request) {
        menuService.assignRoleMenus(roleId, request);
        return R.ok();
    }
}
