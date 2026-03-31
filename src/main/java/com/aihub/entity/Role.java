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
@Table(name = "sys_role")
@TableName("sys_role")
public class Role {

    @Id
    @TableId(type = IdType.AUTO)
    private Long id;

    @Column(nullable = false, length = 30, unique = true)
    private String code;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 200)
    private String description;

    private Integer sort;

    @Column(columnDefinition = "TINYINT")
    private Integer status;

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