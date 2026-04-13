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

@Service
@RequiredArgsConstructor
public class MenuService {

    private static final Set<String> VALID_MENU_TYPES = Set.of("DIR", "MENU", "BUTTON");
    private static final Set<String> VALID_PLATFORMS = Set.of("pc", "h5", "all");
    private static final String DEFAULT_PLATFORM = "pc";

    private final MenuRepository menuRepository;
    private final RoleRepository roleRepository;
    private final RoleMenuRepository roleMenuRepository;
    private final UserRepository userRepository;
    private final MenuMapper menuMapper;
    private final RoleMenuMapper roleMenuMapper;

    public List<MenuVO> getCurrentUserMenuTree(String platform) {
        String normalizedPlatform = normalizePlatformOrDefault(platform);
        Long roleId = getCurrentRoleId();
        List<Menu> menus = SecurityUtils.isAdmin()
                ? menuMapper.selectAllEnabledMenusByPlatform(normalizedPlatform)
                : menuMapper.selectEnabledMenusByRoleIdAndPlatform(roleId, normalizedPlatform);

        List<MenuVO> voList = menus.stream().map(this::toVO).toList();
        return buildTree(voList);
    }

    public List<String> getCurrentUserPermissions(String platform) {
        String normalizedPlatform = normalizePlatformOrDefault(platform);
        Long roleId = getCurrentRoleId();
        List<String> rawPermissions = SecurityUtils.isAdmin()
                ? menuMapper.selectAllPermissionsByPlatform(normalizedPlatform)
                : menuMapper.selectPermissionsByRoleIdAndPlatform(roleId, normalizedPlatform);

        return rawPermissions.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .sorted()
                .toList();
    }

    public List<MenuVO> listAllMenus(String platform) {
        List<Menu> menus;
        if (!StringUtils.hasText(platform)) {
            menus = menuRepository.findAllByDeletedFalseOrderBySortAscIdAsc();
        } else {
            String normalizedPlatform = normalizePlatform(platform);
            List<String> platforms = "all".equals(normalizedPlatform)
                    ? List.of("all")
                    : List.of(normalizedPlatform, "all");
            menus = menuRepository.findAllByDeletedFalseAndPlatformInOrderBySortAscIdAsc(platforms);
        }

        return menus.stream()
                .map(this::toVO)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public MenuVO create(MenuSaveRequest request) {
        String menuType = normalizeMenuType(request.getMenuType());
        String platform = normalizePlatformOrDefault(request.getPlatform());
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
        menu.setPlatform(platform);
        menu.setDeleted(false);

        return toVO(menuRepository.save(menu));
    }

    @Transactional(rollbackFor = Exception.class)
    public MenuVO update(Long id, MenuSaveRequest request) {
        Menu menu = menuRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new BizException(404, "菜单不存在"));

        String menuType = normalizeMenuType(request.getMenuType());
        String platform = normalizePlatformOrDefault(request.getPlatform());
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
        menu.setPlatform(platform);

        return toVO(menuRepository.save(menu));
    }

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

    public List<Long> getRoleMenuIds(Long roleId) {
        ensureRoleExists(roleId);
        return roleMenuRepository.findAllByRoleId(roleId).stream()
                .map(RoleMenu::getMenuId)
                .distinct()
                .sorted()
                .toList();
    }

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

    private Long getCurrentRoleId() {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new BizException(404, "用户不存在"));
        return user.getRoleId();
    }

    private void ensureRoleExists(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new BizException(404, "角色不存在"));
        if (Boolean.TRUE.equals(role.getDeleted())) {
            throw new BizException(400, "角色已删除");
        }
    }

    private String normalizeMenuType(String menuType) {
        String normalized = menuType == null ? "" : menuType.trim().toUpperCase(Locale.ROOT);
        if (!VALID_MENU_TYPES.contains(normalized)) {
            throw new BizException(400, "菜单类型仅支持 DIR/MENU/BUTTON");
        }
        return normalized;
    }

    private String normalizePlatformOrDefault(String platform) {
        if (!StringUtils.hasText(platform)) {
            return DEFAULT_PLATFORM;
        }
        return normalizePlatform(platform);
    }

    private String normalizePlatform(String platform) {
        String normalized = platform.trim().toLowerCase(Locale.ROOT);
        if (!VALID_PLATFORMS.contains(normalized)) {
            throw new BizException(400, "平台类型不支持，仅支持 pc/h5/all");
        }
        return normalized;
    }

    private Long normalizeParentId(Long parentId) {
        return parentId == null || parentId <= 0 ? 0L : parentId;
    }

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

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

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
                .platform(menu.getPlatform())
                .createdAt(menu.getCreatedAt())
                .build();
    }

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
