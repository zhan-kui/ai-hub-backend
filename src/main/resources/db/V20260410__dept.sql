USE aihub;

CREATE TABLE IF NOT EXISTS `sys_dept` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父部门ID，0=根节点',
    `dept_name` VARCHAR(100) NOT NULL,
    `ancestors` VARCHAR(500) DEFAULT '' COMMENT '祖级ID链，如 0,1,2',
    `sort` INT DEFAULT 0,
    `leader` VARCHAR(50) DEFAULT NULL COMMENT '负责人姓名',
    `phone` VARCHAR(20) DEFAULT NULL,
    `email` VARCHAR(100) DEFAULT NULL,
    `default_role_id` BIGINT DEFAULT NULL COMMENT '部门默认角色ID',
    `status` TINYINT DEFAULT 1 COMMENT '1=启用 0=禁用',
    `deleted` TINYINT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB COMMENT='部门表';

CREATE TABLE IF NOT EXISTS `sys_dept_role` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `dept_id` BIGINT NOT NULL,
    `role_id` BIGINT NOT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_dept_role` (`dept_id`, `role_id`),
    INDEX `idx_dept_id` (`dept_id`)
) ENGINE=InnoDB COMMENT='部门角色关联表';

ALTER TABLE `sys_user`
    ADD COLUMN `dept_id` BIGINT DEFAULT NULL COMMENT '所属部门ID' AFTER `role_id`;

INSERT INTO `sys_dept`
(`id`, `parent_id`, `dept_name`, `ancestors`, `sort`, `leader`, `phone`, `email`, `default_role_id`, `status`, `deleted`)
VALUES
    (1, 0, '总公司', '0', 0, NULL, NULL, NULL, NULL, 1, 0),
    (2, 1, '技术部', '0,1', 1, NULL, NULL, NULL, NULL, 1, 0)
ON DUPLICATE KEY UPDATE
    `parent_id` = VALUES(`parent_id`),
    `dept_name` = VALUES(`dept_name`),
    `ancestors` = VALUES(`ancestors`),
    `sort` = VALUES(`sort`),
    `leader` = VALUES(`leader`),
    `phone` = VALUES(`phone`),
    `email` = VALUES(`email`),
    `default_role_id` = VALUES(`default_role_id`),
    `status` = VALUES(`status`),
    `deleted` = VALUES(`deleted`);

INSERT INTO sys_menu(id, parent_id, menu_name, menu_type, path, component, permission, icon, sort, visible, status)
VALUES
    (37, 3, '部门管理', 'MENU', '/depts', 'dept/DeptList', 'dept:list', 'OfficeBuilding', 7, 1, 1),
    (371, 37, '新增部门', 'BUTTON', NULL, NULL, 'dept:create', NULL, 1, 0, 1),
    (372, 37, '修改部门', 'BUTTON', NULL, NULL, 'dept:update', NULL, 2, 0, 1),
    (373, 37, '删除部门', 'BUTTON', NULL, NULL, 'dept:delete', NULL, 3, 0, 1),
    (374, 37, '分配部门角色', 'BUTTON', NULL, NULL, 'dept:role:assign', NULL, 4, 0, 1)
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name);

-- super_admin 自动拥有全部菜单（已有逻辑 SELECT INSERT 会覆盖，无需额外处理）
-- admin 补充授权部门菜单查看（不含分配角色）
INSERT INTO sys_role_menu(role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
         JOIN sys_menu m
WHERE r.code = 'admin'
  AND m.id IN (37, 371, 372, 373)
ON DUPLICATE KEY UPDATE role_id = role_id;
