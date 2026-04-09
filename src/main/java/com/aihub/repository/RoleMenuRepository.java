package com.aihub.repository;

import com.aihub.entity.RoleMenu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
/**
 * RoleMenuRepository interface。
 * <p>该类型承担当前文件的核心职责，建议结合同包相关类一起阅读。
 */

public interface RoleMenuRepository extends JpaRepository<RoleMenu, Long> {

    List<RoleMenu> findAllByRoleId(Long roleId);

    void deleteByRoleId(Long roleId);

    void deleteByMenuId(Long menuId);
}