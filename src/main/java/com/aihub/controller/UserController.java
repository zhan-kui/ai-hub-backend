package com.aihub.controller;

import com.aihub.common.result.R;
import com.aihub.dto.auth.RegisterUserRequest;
import com.aihub.dto.user.ChangePasswordRequest;
import com.aihub.dto.user.UpdateUserRequest;
import com.aihub.dto.user.UserInfoVO;
import com.aihub.service.UserService;
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
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "用户管理")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息")
    public R<UserInfoVO> me() {
        return R.ok(userService.getCurrentUserInfo());
    }

    @PutMapping("/me")
    @Operation(summary = "更新当前用户信息")
    public R<UserInfoVO> updateMe(@Valid @RequestBody UpdateUserRequest request) {
        return R.ok(userService.updateCurrentUser(request));
    }

    @PutMapping("/me/password")
    @Operation(summary = "修改密码")
    public R<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        return R.ok();
    }

    @GetMapping("/list")
    @Operation(summary = "用户列表（管理员）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public R<List<UserInfoVO>> list(@RequestParam(required = false) String keyword,
                                    @RequestParam(defaultValue = "1") Integer page,
                                    @RequestParam(defaultValue = "20") Integer size) {
        return R.ok(userService.listUsers(keyword, page, size));
    }

    @PostMapping("/create")
    @Operation(summary = "创建用户（管理员）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public R<UserInfoVO> create(@Valid @RequestBody RegisterUserRequest request) {
        return R.ok(userService.createUser(request));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "启用/禁用用户（管理员）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public R<Void> toggleStatus(@PathVariable Long id,
                                @RequestParam Integer status) {
        userService.toggleStatus(id, status);
        return R.ok();
    }

    @PutMapping("/{id}/role")
    @Operation(summary = "变更用户角色（超管）")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public R<Void> changeRole(@PathVariable Long id,
                              @RequestParam Long roleId) {
        userService.changeRole(id, roleId);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户（超管）")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public R<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return R.ok();
    }
}