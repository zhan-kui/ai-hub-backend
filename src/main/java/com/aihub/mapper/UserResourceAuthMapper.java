package com.aihub.mapper;

import com.aihub.entity.UserResourceAuth;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserResourceAuthMapper extends BaseMapper<UserResourceAuth> {

    boolean existsValidAuth(@Param("userId") Long userId,
                            @Param("resourceType") String resourceType,
                            @Param("resourceId") Long resourceId);

    List<Long> selectValidResourceIds(@Param("userId") Long userId,
                                      @Param("resourceType") String resourceType);

    int revokeByUserAndType(@Param("userId") Long userId,
                            @Param("resourceType") String resourceType);

    int upsertAuth(@Param("userId") Long userId,
                   @Param("resourceType") String resourceType,
                   @Param("resourceId") Long resourceId,
                   @Param("grantedBy") Long grantedBy);
}