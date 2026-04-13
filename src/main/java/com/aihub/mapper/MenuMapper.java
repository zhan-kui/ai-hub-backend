package com.aihub.mapper;

import com.aihub.entity.Menu;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
/**
 * MenuMapper interface。
 * <p>该类型承担当前文件的核心职责，建议结合同包相关类一起阅读。
 */

public interface MenuMapper extends BaseMapper<Menu> {

    List<Menu> selectAllEnabledMenus();

    List<Menu> selectAllEnabledMenusByPlatform(@Param("platform") String platform);

    List<Menu> selectEnabledMenusByRoleId(@Param("roleId") Long roleId);

    List<Menu> selectEnabledMenusByRoleIdAndPlatform(@Param("roleId") Long roleId,
                                                      @Param("platform") String platform);

    List<String> selectAllPermissions();

    List<String> selectAllPermissionsByPlatform(@Param("platform") String platform);

    List<String> selectPermissionsByRoleId(@Param("roleId") Long roleId);

    List<String> selectPermissionsByRoleIdAndPlatform(@Param("roleId") Long roleId,
                                                       @Param("platform") String platform);
}
