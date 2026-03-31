package com.aihub.dto.resource;

import com.aihub.common.enums.ResourceTypeEnum;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class GrantRequest {

    @NotNull(message = "用户 ID 不能为空")
    private Long userId;

    @NotNull(message = "资源类型不能为空")
    private ResourceTypeEnum resourceType;

    @NotEmpty(message = "资源 ID 列表不能为空")
    private List<Long> resourceIds;
}