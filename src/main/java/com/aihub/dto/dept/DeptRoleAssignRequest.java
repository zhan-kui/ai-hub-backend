package com.aihub.dto.dept;

import lombok.Data;

import java.util.List;

@Data
public class DeptRoleAssignRequest {

    private List<Long> roleIds;
}
