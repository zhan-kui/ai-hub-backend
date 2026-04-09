package com.aihub.mapper;

import com.aihub.entity.RoleMenu;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
/**
 * RoleMenuMapper interface。
 * <p>该类型承担当前文件的核心职责，建议结合同包相关类一起阅读。
 */

public interface RoleMenuMapper extends BaseMapper<RoleMenu> {

    int deleteByRoleId(@Param("roleId") Long roleId);

    int deleteByMenuId(@Param("menuId") Long menuId);

    int batchInsert(@Param("roleId") Long roleId,
                    @Param("menuIds") List<Long> menuIds);
}