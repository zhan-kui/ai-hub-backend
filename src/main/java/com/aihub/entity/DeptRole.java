package com.aihub.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "sys_dept_role")
@TableName("sys_dept_role")
public class DeptRole {

    @Id
    @TableId(type = IdType.AUTO)
    @Column(name = "id")
    @TableField("id")
    private Long id;

    @Column(name = "dept_id", nullable = false)
    @TableField("dept_id")
    private Long deptId;

    @Column(name = "role_id", nullable = false)
    @TableField("role_id")
    private Long roleId;

    @Column(name = "created_at", updatable = false)
    @TableField("created_at")
    private LocalDateTime createdAt;
}
