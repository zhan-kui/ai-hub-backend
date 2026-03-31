CREATE DATABASE IF NOT EXISTS aihub DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE aihub;

-- ========== 角色表 ==========
CREATE TABLE `sys_role` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `code` VARCHAR(30) NOT NULL UNIQUE COMMENT '角色编码：super_admin/admin/user',
    `name` VARCHAR(50) NOT NULL COMMENT '角色名称',
    `description` VARCHAR(200) DEFAULT NULL,
    `sort` INT DEFAULT 0 COMMENT '排序',
    `status` TINYINT DEFAULT 1 COMMENT '1=启用 0=禁用',
    `deleted` TINYINT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='角色表';

INSERT INTO `sys_role` (`code`, `name`, `description`, `sort`) VALUES
('super_admin', '超级管理员', '拥有所有权限，可管理所有用户和资源', 1),
('admin', '管理员', '可管理所有应用和知识库，可管理普通用户', 2),
('user', '普通用户', '仅可使用被授权的应用和知识库', 3);

-- ========== 用户表 ==========
CREATE TABLE `sys_user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '登录账号',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（BCrypt 加密）',
    `nickname` VARCHAR(100) DEFAULT NULL COMMENT '昵称',
    `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像 URL',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `role_id` BIGINT NOT NULL COMMENT '关联角色',
    `status` TINYINT DEFAULT 1 COMMENT '1=正常 0=禁用',
    `last_login_at` DATETIME DEFAULT NULL COMMENT '最后登录时间',
    `last_login_ip` VARCHAR(50) DEFAULT NULL COMMENT '最后登录 IP',
    `deleted` TINYINT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_role_id` (`role_id`),
    CONSTRAINT `fk_user_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role`(`id`)
) ENGINE=InnoDB COMMENT='用户表';

-- ========== 默认超级管理员（密码：admin123，BCrypt 加密） ==========
INSERT INTO `sys_user` (`username`, `password`, `nickname`, `role_id`, `status`) VALUES
('admin', '$2a$10$yQSpLMqRq6YT.FNp9jmt1u.gUS.Zh.TA5LcA6H707Q1pFbENd0nj6', '超级管理员',
 (SELECT `id` FROM `sys_role` WHERE `code` = 'super_admin'), 1);

-- ========== 应用配置表 ==========
CREATE TABLE `app_config` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `app_name` VARCHAR(100) NOT NULL COMMENT '应用显示名称',
    `app_code` VARCHAR(50) NOT NULL UNIQUE COMMENT '应用编码',
    `app_type` VARCHAR(30) NOT NULL COMMENT 'chat/chatflow/workflow/completion/agent',
    `dify_app_id` VARCHAR(100) DEFAULT NULL COMMENT 'Dify 应用 ID',
    `dify_api_key` VARCHAR(255) NOT NULL COMMENT 'Dify API Key',
    `dify_base_url` VARCHAR(500) DEFAULT NULL COMMENT '独立 Dify API 地址（为空则用全局配置）',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '应用描述',
    `icon` VARCHAR(500) DEFAULT NULL COMMENT '应用图标 URL',
    `sort` INT DEFAULT 0,
    `enabled` TINYINT DEFAULT 1,
    `deleted` TINYINT DEFAULT 0,
    `created_by` BIGINT DEFAULT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_app_type` (`app_type`)
) ENGINE=InnoDB COMMENT='Dify 应用配置表';

-- ========== 知识库配置表 ==========
CREATE TABLE `knowledge_base` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `kb_name` VARCHAR(100) NOT NULL COMMENT '知识库显示名称',
    `kb_code` VARCHAR(50) NOT NULL UNIQUE COMMENT '知识库编码',
    `dify_dataset_id` VARCHAR(100) NOT NULL COMMENT 'Dify dataset ID',
    `dify_api_key` VARCHAR(255) NOT NULL COMMENT 'Dify 知识库 API Key',
    `dify_base_url` VARCHAR(500) DEFAULT NULL COMMENT 'Dify API 地址',
    `description` VARCHAR(500) DEFAULT NULL,
    `icon` VARCHAR(500) DEFAULT NULL,
    `document_count` INT DEFAULT 0,
    `word_count` BIGINT DEFAULT 0,
    `indexing_technique` VARCHAR(30) DEFAULT NULL,
    `embedding_model` VARCHAR(100) DEFAULT NULL,
    `sort` INT DEFAULT 0,
    `enabled` TINYINT DEFAULT 1,
    `deleted` TINYINT DEFAULT 0,
    `created_by` BIGINT DEFAULT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_dify_dataset_id` (`dify_dataset_id`)
) ENGINE=InnoDB COMMENT='Dify 知识库配置表';

-- ========== 用户资源授权表 ==========
-- 注意：唯一索引不包含 deleted 字段，改用 status 字段标识是否生效
CREATE TABLE `user_resource_auth` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL COMMENT '用户 ID',
    `resource_type` VARCHAR(30) NOT NULL COMMENT '资源类型：app/knowledge',
    `resource_id` BIGINT NOT NULL COMMENT '资源 ID',
    `granted_by` BIGINT DEFAULT NULL COMMENT '授权人',
    `expire_at` DATETIME DEFAULT NULL COMMENT '授权过期时间（null=永不过期）',
    `status` TINYINT DEFAULT 1 COMMENT '1=生效 0=已撤销',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_user_resource` (`user_id`, `resource_type`, `resource_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_resource` (`resource_type`, `resource_id`)
) ENGINE=InnoDB COMMENT='用户资源授权表';

-- ========== 对话表 ==========
CREATE TABLE `conversation` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `app_config_id` BIGINT DEFAULT NULL COMMENT '关联应用',
    `dify_conversation_id` VARCHAR(100) DEFAULT NULL,
    `title` VARCHAR(255) DEFAULT NULL,
    `status` TINYINT DEFAULT 1 COMMENT '1=活跃 0=归档',
    `deleted` TINYINT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_app_config_id` (`app_config_id`),
    INDEX `idx_dify_conv` (`dify_conversation_id`)
) ENGINE=InnoDB COMMENT='对话表';

-- ========== 聊天消息表 ==========
CREATE TABLE `chat_message` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `conversation_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `app_config_id` BIGINT DEFAULT NULL COMMENT '关联应用',
    `dify_message_id` VARCHAR(100) DEFAULT NULL,
    `dify_conversation_id` VARCHAR(100) DEFAULT NULL,
    `role` VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT 'user/assistant',
    `query` TEXT DEFAULT NULL COMMENT '用户问题',
    `answer` LONGTEXT DEFAULT NULL COMMENT 'AI 完整回答',
    `prompt_tokens` INT DEFAULT 0,
    `completion_tokens` INT DEFAULT 0,
    `total_tokens` INT DEFAULT 0,
    `total_price` DECIMAL(10, 6) DEFAULT 0 COMMENT '费用',
    `latency` DECIMAL(10, 3) DEFAULT NULL COMMENT '响应耗时（秒）',
    `feedback` VARCHAR(20) DEFAULT NULL COMMENT 'like/dislike',
    `deleted` TINYINT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_conv_id` (`conversation_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_dify_msg` (`dify_message_id`)
) ENGINE=InnoDB COMMENT='聊天消息表';

