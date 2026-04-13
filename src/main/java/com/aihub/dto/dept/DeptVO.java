package com.aihub.dto.dept;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeptVO {

    private Long id;

    private Long parentId;

    private String deptName;

    private String ancestors;

    private Integer sort;

    private String leader;

    private String phone;

    private String email;

    private Long defaultRoleId;

    private String defaultRoleName;

    private Boolean status;

    private LocalDateTime createdAt;

    @Builder.Default
    private List<DeptVO> children = new ArrayList<>();
}
