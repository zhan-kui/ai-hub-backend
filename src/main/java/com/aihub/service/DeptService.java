package com.aihub.service;

import com.aihub.common.exception.BizException;
import com.aihub.dto.dept.DeptRoleAssignRequest;
import com.aihub.dto.dept.DeptSaveRequest;
import com.aihub.dto.dept.DeptVO;
import com.aihub.entity.Dept;
import com.aihub.entity.DeptRole;
import com.aihub.entity.Role;
import com.aihub.repository.DeptRepository;
import com.aihub.repository.DeptRoleRepository;
import com.aihub.repository.RoleRepository;
import com.aihub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeptService {

    private final DeptRepository deptRepository;
    private final DeptRoleRepository deptRoleRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    public List<DeptVO> getDeptTree() {
        List<Dept> depts = deptRepository.findAllByDeletedFalseOrderBySortAscIdAsc();
        if (depts.isEmpty()) {
            return List.of();
        }

        Set<Long> roleIds = depts.stream()
                .map(Dept::getDefaultRoleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> roleNameMap = roleRepository.findAllById(roleIds).stream()
                .filter(role -> !Boolean.TRUE.equals(role.getDeleted()))
                .collect(Collectors.toMap(Role::getId, Role::getName, (a, b) -> a));

        List<DeptVO> voList = depts.stream()
                .map(dept -> toVO(dept, roleNameMap.get(dept.getDefaultRoleId())))
                .toList();

        return buildTree(voList);
    }

    public DeptVO getById(Long id) {
        Dept dept = deptRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new BizException(404, "部门不存在"));

        String defaultRoleName = null;
        if (dept.getDefaultRoleId() != null) {
            defaultRoleName = roleRepository.findById(dept.getDefaultRoleId())
                    .filter(role -> !Boolean.TRUE.equals(role.getDeleted()))
                    .map(Role::getName)
                    .orElse(null);
        }
        return toVO(dept, defaultRoleName);
    }

    @Transactional(rollbackFor = Exception.class)
    public DeptVO create(DeptSaveRequest request) {
        Long parentId = normalizeParentId(request.getParentId());
        String ancestors = buildAncestors(parentId, null);

        Dept dept = new Dept();
        dept.setParentId(parentId);
        dept.setDeptName(request.getDeptName().trim());
        dept.setAncestors(ancestors);
        dept.setSort(request.getSort() != null ? request.getSort() : 0);
        dept.setLeader(trimToNull(request.getLeader()));
        dept.setPhone(trimToNull(request.getPhone()));
        dept.setEmail(trimToNull(request.getEmail()));
        dept.setDefaultRoleId(request.getDefaultRoleId());
        dept.setStatus(request.getStatus() != null ? request.getStatus() : true);
        dept.setDeleted(false);

        Dept saved = deptRepository.save(dept);
        String defaultRoleName = resolveRoleName(saved.getDefaultRoleId());
        return toVO(saved, defaultRoleName);
    }

    @Transactional(rollbackFor = Exception.class)
    public DeptVO update(Long id, DeptSaveRequest request) {
        Dept dept = deptRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new BizException(404, "部门不存在"));

        Long parentId = normalizeParentId(request.getParentId());
        if (Objects.equals(parentId, id)) {
            throw new BizException(400, "父部门不能为自己");
        }

        String ancestors = buildAncestors(parentId, id);
        dept.setParentId(parentId);
        dept.setDeptName(request.getDeptName().trim());
        dept.setAncestors(ancestors);
        if (request.getSort() != null) {
            dept.setSort(request.getSort());
        }
        dept.setLeader(trimToNull(request.getLeader()));
        dept.setPhone(trimToNull(request.getPhone()));
        dept.setEmail(trimToNull(request.getEmail()));
        dept.setDefaultRoleId(request.getDefaultRoleId());
        if (request.getStatus() != null) {
            dept.setStatus(request.getStatus());
        }

        Dept saved = deptRepository.save(dept);
        String defaultRoleName = resolveRoleName(saved.getDefaultRoleId());
        return toVO(saved, defaultRoleName);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Dept dept = deptRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new BizException(404, "部门不存在"));

        if (deptRepository.existsByParentIdAndDeletedFalse(id)) {
            throw new BizException(400, "请先删除子部门");
        }

        if (userRepository.existsByDeptIdAndDeletedFalse(id)) {
            throw new BizException(400, "部门下存在用户，无法删除");
        }

        dept.setDeleted(true);
        deptRepository.save(dept);
        deptRoleRepository.deleteByDeptId(id);
    }

    public List<Long> getDeptRoleIds(Long deptId) {
        ensureDeptExists(deptId);
        return deptRoleRepository.findAllByDeptId(deptId).stream()
                .map(DeptRole::getRoleId)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public void assignDeptRoles(Long deptId, DeptRoleAssignRequest request) {
        ensureDeptExists(deptId);
        List<Long> roleIds = request == null || request.getRoleIds() == null
                ? List.of()
                : request.getRoleIds().stream().filter(Objects::nonNull).distinct().toList();

        if (!roleIds.isEmpty()) {
            List<Role> roles = roleRepository.findAllById(roleIds).stream()
                    .filter(role -> !Boolean.TRUE.equals(role.getDeleted()))
                    .toList();
            if (roles.size() != roleIds.size()) {
                throw new BizException(400, "存在无效角色ID");
            }
        }

        deptRoleRepository.deleteByDeptId(deptId);
        if (roleIds.isEmpty()) {
            return;
        }

        List<DeptRole> records = roleIds.stream().map(roleId -> {
            DeptRole deptRole = new DeptRole();
            deptRole.setDeptId(deptId);
            deptRole.setRoleId(roleId);
            return deptRole;
        }).toList();
        deptRoleRepository.saveAll(records);
    }

    private void ensureDeptExists(Long deptId) {
        if (deptId == null) {
            throw new BizException(400, "部门ID不能为空");
        }
        if (deptRepository.findByIdAndDeletedFalse(deptId).isEmpty()) {
            throw new BizException(404, "部门不存在");
        }
    }

    private Long normalizeParentId(Long parentId) {
        return parentId == null || parentId <= 0 ? 0L : parentId;
    }

    private String buildAncestors(Long parentId, Long currentDeptId) {
        if (parentId == null || parentId == 0L) {
            return "0";
        }

        Dept parent = deptRepository.findByIdAndDeletedFalse(parentId)
                .orElseThrow(() -> new BizException(400, "父部门不存在"));

        if (currentDeptId != null && containsAncestor(parent.getAncestors(), currentDeptId)) {
            throw new BizException(400, "父部门不能是当前部门的子部门");
        }

        String parentAncestors = StringUtils.hasText(parent.getAncestors()) ? parent.getAncestors() : "0";
        return parentAncestors + "," + parentId;
    }

    private boolean containsAncestor(String ancestors, Long targetId) {
        if (!StringUtils.hasText(ancestors) || targetId == null) {
            return false;
        }
        String target = String.valueOf(targetId);
        return List.of(ancestors.split(",")).contains(target);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String resolveRoleName(Long roleId) {
        if (roleId == null) {
            return null;
        }
        return roleRepository.findById(roleId)
                .filter(role -> !Boolean.TRUE.equals(role.getDeleted()))
                .map(Role::getName)
                .orElse(null);
    }

    private DeptVO toVO(Dept dept, String defaultRoleName) {
        return DeptVO.builder()
                .id(dept.getId())
                .parentId(dept.getParentId())
                .deptName(dept.getDeptName())
                .ancestors(dept.getAncestors())
                .sort(dept.getSort())
                .leader(dept.getLeader())
                .phone(dept.getPhone())
                .email(dept.getEmail())
                .defaultRoleId(dept.getDefaultRoleId())
                .defaultRoleName(defaultRoleName)
                .status(dept.getStatus())
                .createdAt(dept.getCreatedAt())
                .build();
    }

    private List<DeptVO> buildTree(List<DeptVO> flatList) {
        Map<Long, DeptVO> nodeMap = flatList.stream()
                .collect(Collectors.toMap(DeptVO::getId, dept -> dept, (a, b) -> a, LinkedHashMap::new));

        List<DeptVO> roots = new ArrayList<>();
        for (DeptVO node : flatList) {
            Long parentId = node.getParentId();
            if (parentId == null || parentId == 0L || !nodeMap.containsKey(parentId)) {
                roots.add(node);
                continue;
            }
            nodeMap.get(parentId).getChildren().add(node);
        }

        sortTree(roots);
        return roots;
    }

    private void sortTree(List<DeptVO> nodes) {
        nodes.sort(Comparator
                .comparing((DeptVO dept) -> dept.getSort() == null ? Integer.MAX_VALUE : dept.getSort())
                .thenComparing(dept -> dept.getId() == null ? Long.MAX_VALUE : dept.getId()));

        for (DeptVO node : nodes) {
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                sortTree(node.getChildren());
            }
        }
    }
}
