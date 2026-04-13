package com.aihub.controller;

import com.aihub.common.result.R;
import com.aihub.dto.dept.DeptRoleAssignRequest;
import com.aihub.dto.dept.DeptSaveRequest;
import com.aihub.dto.dept.DeptVO;
import com.aihub.service.DeptService;
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
@RequestMapping("/depts")
@RequiredArgsConstructor
@Tag(name = "部门管理")
public class DeptController {

    private final DeptService deptService;

    @GetMapping("/tree")
    @Operation(summary = "获取部门树")
    public R<List<DeptVO>> getDeptTree() {
        return R.ok(deptService.getDeptTree());
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取部门详情")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public R<DeptVO> getById(@PathVariable Long id) {
        return R.ok(deptService.getById(id));
    }

    @PostMapping("/create")
    @Operation(summary = "新增部门")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public R<DeptVO> create(@Valid @RequestBody DeptSaveRequest request) {
        return R.ok(deptService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改部门")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public R<DeptVO> update(@PathVariable Long id,
                            @Valid @RequestBody DeptSaveRequest request) {
        return R.ok(deptService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除部门")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public R<Void> delete(@PathVariable Long id) {
        deptService.delete(id);
        return R.ok();
    }

    @GetMapping("/{deptId}/roles")
    @Operation(summary = "获取部门绑定角色ID")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public R<List<Long>> getDeptRoleIds(@PathVariable Long deptId) {
        return R.ok(deptService.getDeptRoleIds(deptId));
    }

    @PostMapping("/{deptId}/roles")
    @Operation(summary = "分配部门角色")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public R<Void> assignDeptRoles(@PathVariable Long deptId,
                                   @RequestBody DeptRoleAssignRequest request) {
        deptService.assignDeptRoles(deptId, request);
        return R.ok();
    }
}
