package com.aihub.dto.user;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserInfoVO {

    private Long id;

    private String username;

    private String nickname;

    private String avatar;

    private String email;

    private String phone;

    private String roleCode;

    private String roleName;

    private Integer status;

    private LocalDateTime lastLoginAt;

    private LocalDateTime createdAt;
}