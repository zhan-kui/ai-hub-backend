package com.aihub.dto.dept;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeptSaveRequest {

    private Long parentId;

    @NotBlank(message = "部门名称不能为空")
    private String deptName;

    private Integer sort;

    private String leader;

    private String phone;

    private String email;

    private Long defaultRoleId;

    private Boolean status;
}
