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
@Table(name = "sys_dept")
@TableName("sys_dept")
public class Dept {

    @Id
    @TableId(type = IdType.AUTO)
    @Column(name = "id")
    @TableField("id")
    private Long id;

    @Column(name = "parent_id", nullable = false)
    @TableField("parent_id")
    private Long parentId;

    @Column(name = "dept_name", nullable = false, length = 100)
    @TableField("dept_name")
    private String deptName;

    @Column(name = "ancestors", length = 500)
    @TableField("ancestors")
    private String ancestors;

    @Column(name = "sort")
    @TableField("sort")
    private Integer sort;

    @Column(name = "leader", length = 50)
    @TableField("leader")
    private String leader;

    @Column(name = "phone", length = 20)
    @TableField("phone")
    private String phone;

    @Column(name = "email", length = 100)
    @TableField("email")
    private String email;

    @Column(name = "default_role_id")
    @TableField("default_role_id")
    private Long defaultRoleId;

    @Column(name = "status", columnDefinition = "TINYINT")
    @TableField("status")
    private Boolean status;

    @Column(name = "deleted", columnDefinition = "TINYINT")
    @TableField("deleted")
    @TableLogic
    private Boolean deleted;

    @Column(name = "created_at", updatable = false)
    @TableField("created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
