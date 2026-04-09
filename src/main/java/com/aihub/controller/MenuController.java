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
/**
 * MenuController class。
 * <p>该类型承担当前文件的核心职责，建议结合同包相关类一起阅读。
 */

@RestController
@RequestMapping("/menus")
@RequiredArgsConstructor
@Tag(name = "菜单权限管理")
public class MenuController {
    /**
     * 字段：menuService。
     * <p>用于承载当前对象的状态数据，具体业务语义请结合上下文与调用方理解。
     */

    private final MenuService menuService;
    /**
     * 方法：tree。
     * <p>用于处理当前场景下的业务流程或数据转换逻辑。
     */

    @GetMapping("/tree")
    @Operation(summary = "当前用户菜单树")
    public R<List<MenuVO>> tree() {
        return R.ok(menuService.getCurrentUserMenuTree());
    }
    /**
     * 方法：list。
     * <p>用于处理当前场景下的业务流程或数据转换逻辑。
     */

    @GetMapping("/list")
    @Operation(summary = "全量菜单列表")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public R<List<MenuVO>> list() {
        return R.ok(menuService.listAllMenus());
    }
    /**
     * 方法：create。
     * <p>用于处理当前场景下的业务流程或数据转换逻辑。
     */

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
    /**
     * 方法：deleteByParam。
     * <p>用于处理当前场景下的业务流程或数据转换逻辑。
     */

    
    @DeleteMapping
    @Operation(summary = "删除菜单（兼容模式）")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public R<Void> deleteByParam(@RequestParam Long id) {
        menuService.delete(id);
        return R.ok();
    }
    /**
     * 方法：delete。
     * <p>用于处理当前场景下的业务流程或数据转换逻辑。
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除菜单")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public R<Void> delete(@PathVariable Long id) {
        menuService.delete(id);
        return R.ok();
    }
    /**
     * 方法：getRoleMenus。
     * <p>用于处理当前场景下的业务流程或数据转换逻辑。
     */

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