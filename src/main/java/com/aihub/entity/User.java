package com.aihub.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "sys_user")
@TableName("sys_user")
public class User {

    @Id
    @TableId(type = IdType.AUTO)
    private Long id;

    @Column(nullable = false, length = 50, unique = true)
    private String username;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(length = 100)
    private String nickname;

    @Column(length = 500)
    private String avatar;

    @Column(length = 100)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(name = "role_id", nullable = false)
    @TableField("role_id")
    private Long roleId;

    @Column(name = "dept_id")
    @TableField("dept_id")
    private Long deptId;

    @Column(columnDefinition = "TINYINT")
    private Integer status;

    @Column(name = "last_login_at")
    @TableField("last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "last_login_ip", length = 50)
    @TableField("last_login_ip")
    private String lastLoginIp;

    @Column(columnDefinition = "TINYINT")
    @TableLogic
    private Boolean deleted;

    @Column(name = "created_at", updatable = false)
    @TableField("created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
