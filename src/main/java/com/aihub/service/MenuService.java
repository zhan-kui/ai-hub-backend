package com.aihub.service;

import com.aihub.common.exception.BizException;
import com.aihub.dto.menu.MenuSaveRequest;
import com.aihub.dto.menu.MenuVO;
import com.aihub.dto.menu.RoleMenuAssignRequest;
import com.aihub.entity.Menu;
import com.aihub.entity.Role;
import com.aihub.entity.RoleMenu;
import com.aihub.entity.User;
import com.aihub.mapper.MenuMapper;
import com.aihub.mapper.RoleMenuMapper;
import com.aihub.repository.MenuRepository;
import com.aihub.repository.RoleMenuRepository;
import com.aihub.repository.RoleRepository;
import com.aihub.repository.UserRepository;
import com.aihub.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
/**
 * MenuService class。
 * <p>该类型承担当前文件的核心职责，建议结合同包相关类一起阅读。
 */

@Service
@RequiredArgsConstructor
public class MenuService {
    /**
     * 字段：。
     * <p>用于承载当前对象的状态数据，具体业务语义请结合上下文与调用方理解。
     */

    private static final Set<String> VALID_MENU_TYPES = Set.of("DIR", "MENU", "BUTTON");
    /**
     * 字段：menuRepository。
     * <p>用于承载当前对象的状态数据，具体业务语义请结合上下文与调用方理解。
     */

    private final MenuRepository menuRepository;
    /**
     * 字段：roleRepository。
     * <p>用于承载当前对象的状态数据，具体业务语义请结合上下文与调用方理解。
     */
    private final RoleRepository roleRepository;
    /**
     * 字段：roleMenuRepository。
     * <p>用于承载当前对象的状态数据，具体业务语义请结合上下文与调用方理解。
     */
    private final RoleMenuRepository roleMenuRepository;
    /**
     * 字段：userRepository。
     * <p>用于承载当前对象的状态数据，具体业务语义请结合上下文与调用方理解。
     */
    private final UserRepository userRepository;
    /**
     * 字段：menuMapper。
     * <p>用于承载当前对象的状态数据，具体业务语义请结合上下文与调用方理解。
     */
    private final MenuMapper menuMapper;
    /**
     * 字段：roleMenuMapper。
     * <p>用于承载当前对象的状态数据，具体业务语义请结合上下文与调用方理解。
     */
    private final RoleMenuMapper roleMenuMapper;
    /**
     * 方法：getCurrentUserMenuTree。
     * <p>用于处理当前场景下的业务流程或数据转换逻辑。
     */

    public List<MenuVO> getCurrentUserMenuTree() {
        Long roleId = getCurrentRoleId();
        List<Menu> menus = SecurityUtils.isAdmin()
                ? menuMapper.selectAllEnabledMenus()
                : menuMapper.selectEnabledMenusByRoleId(roleId);

        List<MenuVO> voList = menus.stream().map(this::toVO).toList();
        return buildTree(voList);
    }
    /**
     * 方法：getCurrentUserPermissions。
     * <p>用于处理当前场景下的业务流程或数据转换逻辑。
     */

    public List<String> getCurrentUserPermissions() {
        Long roleId = getCurrentRoleId();
        List<String> rawPermissions = SecurityUtils.isAdmin()
                ? menuMapper.selectAllPermissions()
                : menuMapper.selectPermissionsByRoleId(roleId);

        return rawPermissions.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .sorted()
                .toList();
    }
    /**
     * 方法：listAllMenus。
     * <p>用于处理当前场景下的业务流程或数据转换逻辑。
     */

    public List<MenuVO> listAllMenus() {
        return menuRepository.findAllByDeletedFalseOrderBySortAscIdAsc().stream()
                .map(this::toVO)
                .toList();
    }
    /**
     * 方法：create。
     * <p>用于处理当前场景下的业务流程或数据转换逻辑。
     */

    @Transactional(rollbackFor = Exception.class)
    public MenuVO create(MenuSaveRequest request) {
        String menuType = normalizeMenuType(request.getMenuType());
        Long parentId = normalizeParentId(request.getParentId());
        validateParent(parentId, null);

        Menu menu = new Menu();
        menu.setParentId(parentId);
        menu.setMenuName(request.getMenuName().trim());
        menu.setMenuType(menuType);
        menu.setPath(trimToNull(request.getPath()));
        menu.setComponent(trimToNull(request.getComponent()));
        menu.setPermission(trimToNull(request.getPermission()));
        menu.setIcon(trimToNull(request.getIcon()));
        menu.setSort(request.getSort() != null ? request.getSort() : 0);
        menu.setVisible(request.getVisible() != null ? request.getVisible() : true);
        menu.setStatus(request.getStatus() != null ? request.getStatus() : true);
        menu.setDeleted(false);

        return toVO(menuRepository.save(menu));
    }
    /**
     * 方法：update。
     * <p>用于处理当前场景下的业务流程或数据转换逻辑。
     */

    @Transactional(rollbackFor = Exception.class)
    public MenuVO update(Long id, MenuSaveRequest request) {
        Menu menu = menuRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new BizException(404, "菜单不存在"));

        String menuType = normalizeMenuType(request.getMenuType());
        Long parentId = normalizeParentId(request.getParentId());
        if (Objects.equals(parentId, id)) {
            throw new BizException(400, "父级菜单不能为自身");
        }
        validateParent(parentId, id);

        menu.setParentId(parentId);
        menu.setMenuName(request.getMenuName().trim());
        menu.setMenuType(menuType);
        menu.setPath(trimToNull(request.getPath()));
        menu.setComponent(trimToNull(request.getComponent()));
        menu.setPermission(trimToNull(request.getPermission()));
        menu.setIcon(trimToNull(request.getIcon()));
        if (request.getSort() != null) {
            menu.setSort(request.getSort());
        }
        if (request.getVisible() != null) {
            menu.setVisible(request.getVisible());
        }
        if (request.getStatus() != null) {
            menu.setStatus(request.getStatus());
        }

        return toVO(menuRepository.save(menu));
    }
    /**
     * 方法：delete。
     * <p>用于处理当前场景下的业务流程或数据转换逻辑。
     */

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Menu menu = menuRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new BizException(404, "菜单不存在"));

        if (menuRepository.existsByParentIdAndDeletedFalse(id)) {
            throw new BizException(400, "请先删除子菜单后再删除当前菜单");
        }

        menu.setDeleted(true);
        menuRepository.save(menu);
        roleMenuMapper.deleteByMenuId(id);
    }
    /**
     * 方法：getRoleMenuIds。
     * <p>用于处理当前场景下的业务流程或数据转换逻辑。
     */

    public List<Long> getRoleMenuIds(Long roleId) {
        ensureRoleExists(roleId);
        return roleMenuRepository.findAllByRoleId(roleId).stream()
                .map(RoleMenu::getMenuId)
                .distinct()
                .sorted()
                .toList();
    }
    /**
     * 方法：assignRoleMenus。
     * <p>用于处理当前场景下的业务流程或数据转换逻辑。
     */

    @Transactional(rollbackFor = Exception.class)
    public void assignRoleMenus(Long roleId, RoleMenuAssignRequest request) {
        ensureRoleExists(roleId);

        List<Long> menuIds = request.getMenuIds() == null
                ? List.of()
                : request.getMenuIds().stream().filter(Objects::nonNull).distinct().toList();

        if (!menuIds.isEmpty()) {
            List<Menu> menus = menuRepository.findAllByIdInAndDeletedFalseOrderBySortAscIdAsc(menuIds);
            if (menus.size() != menuIds.size()) {
                throw new BizException(400, "存在无效菜单 ID");
            }
        }

        roleMenuMapper.deleteByRoleId(roleId);
        if (!menuIds.isEmpty()) {
            roleMenuMapper.batchInsert(roleId, menuIds);
        }
    }
    /**
     * 方法：getCurrentRoleId。
     * <p>用于处理当前场景下的业务流程或数据转换逻辑。
     */

    private Long getCurrentRoleId() {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new BizException(404, "用户不存在"));
        return user.getRoleId();
    }
    /**
     * 方法：ensureRoleExists。
     * <p>用于处理当前场景下的业务流程或数据转换逻辑。
     */

    private void ensureRoleExists(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new BizException(404, "角色不存在"));
        if (Boolean.TRUE.equals(role.getDeleted())) {
            throw new BizException(400, "角色已删除");
        }
    }
    /**
     * 方法：normalizeMenuType。
     * <p>用于处理当前场景下的业务流程或数据转换逻辑。
     */

    private String normalizeMenuType(String menuType) {
        String normalized = menuType == null ? "" : menuType.trim().toUpperCase(Locale.ROOT);
        if (!VALID_MENU_TYPES.contains(normalized)) {
            throw new BizException(400, "菜单类型仅支持 DIR/MENU/BUTTON");
        }
        return normalized;
    }
    /**
     * 方法：normalizeParentId。
     * <p>用于处理当前场景下的业务流程或数据转换逻辑。
     */

    private Long normalizeParentId(Long parentId) {
        return parentId == null || parentId <= 0 ? 0L : parentId;
    }
    /**
     * 方法：validateParent。
     * <p>用于处理当前场景下的业务流程或数据转换逻辑。
     */

    private void validateParent(Long parentId, Long currentId) {
        if (parentId == null || parentId == 0L) {
            return;
        }

        Menu parent = menuRepository.findByIdAndDeletedFalse(parentId)
                .orElseThrow(() -> new BizException(400, "父级菜单不存在"));
        if ("BUTTON".equals(parent.getMenuType())) {
            throw new BizException(400, "按钮类型菜单不能作为父级");
        }

        if (currentId != null) {
            Long cursor = parent.getParentId();
            while (cursor != null && cursor > 0) {
                if (Objects.equals(cursor, currentId)) {
                    throw new BizException(400, "父级菜单不能为当前菜单的子节点");
                }
                Menu cursorMenu = menuRepository.findByIdAndDeletedFalse(cursor).orElse(null);
                if (cursorMenu == null) {
                    break;
                }
                cursor = cursorMenu.getParentId();
            }
        }
    }
    /**
     * 方法：trimToNull。
     * <p>用于处理当前场景下的业务流程或数据转换逻辑。
     */

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
    /**
     * 方法：toVO。
     * <p>用于处理当前场景下的业务流程或数据转换逻辑。
     */

    private MenuVO toVO(Menu menu) {
        return MenuVO.builder()
                .id(menu.getId())
                .parentId(menu.getParentId())
                .menuName(menu.getMenuName())
                .menuType(menu.getMenuType())
                .path(menu.getPath())
                .component(menu.getComponent())
                .permission(menu.getPermission())
                .icon(menu.getIcon())
                .sort(menu.getSort())
                .visible(menu.getVisible())
                .status(menu.getStatus())
                .createdAt(menu.getCreatedAt())
                .build();
    }
    /**
     * 方法：buildTree。
     * <p>用于处理当前场景下的业务流程或数据转换逻辑。
     */

    private List<MenuVO> buildTree(List<MenuVO> flatList) {
        Map<Long, MenuVO> nodeMap = flatList.stream()
                .collect(Collectors.toMap(MenuVO::getId, menu -> menu, (a, b) -> a, LinkedHashMap::new));

        List<MenuVO> roots = new ArrayList<>();
        for (MenuVO node : flatList) {
            Long parentId = node.getParentId();
            if (parentId == null || parentId == 0L || !nodeMap.containsKey(parentId)) {
                roots.add(node);
                continue;
            }
            nodeMap.get(parentId).getChildren().add(node);
        }

        sortMenus(roots);
        return roots;
    }
    /**
     * 方法：sortMenus。
     * <p>用于处理当前场景下的业务流程或数据转换逻辑。
     */

    private void sortMenus(List<MenuVO> nodes) {
        nodes.sort(Comparator
                .comparing((MenuVO menu) -> menu.getSort() == null ? Integer.MAX_VALUE : menu.getSort())
                .thenComparing(menu -> menu.getId() == null ? Long.MAX_VALUE : menu.getId()));

        for (MenuVO node : nodes) {
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                sortMenus(node.getChildren());
            }
        }
    }
}