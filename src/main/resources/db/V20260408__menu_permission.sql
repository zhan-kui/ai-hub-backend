USE aihub;

CREATE TABLE IF NOT EXISTS `sys_menu` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父级菜单 ID，0 表示根节点',
    `menu_name` VARCHAR(100) NOT NULL COMMENT '菜单名称',
    `menu_type` VARCHAR(20) NOT NULL COMMENT 'DIR=目录 MENU=菜单 BUTTON=按钮',
    `path` VARCHAR(255) DEFAULT NULL COMMENT '前端路由路径',
    `component` VARCHAR(255) DEFAULT NULL COMMENT '前端组件路径',
    `permission` VARCHAR(100) DEFAULT NULL COMMENT '权限标识',
    `icon` VARCHAR(100) DEFAULT NULL COMMENT '图标',
    `sort` INT DEFAULT 0 COMMENT '排序',
    `visible` TINYINT DEFAULT 1 COMMENT '1=显示 0=隐藏',
    `status` TINYINT DEFAULT 1 COMMENT '1=启用 0=禁用',
    `deleted` TINYINT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_parent_id` (`parent_id`),
    INDEX `idx_menu_type` (`menu_type`),
    INDEX `idx_permission` (`permission`)
) ENGINE=InnoDB COMMENT='系统菜单表';

CREATE TABLE IF NOT EXISTS `sys_role_menu` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `role_id` BIGINT NOT NULL,
    `menu_id` BIGINT NOT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_role_menu` (`role_id`, `menu_id`),
    INDEX `idx_role_id` (`role_id`),
    INDEX `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB COMMENT='角色菜单关联表';

INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `visible`, `status`) VALUES
(1, 0, '工作台', 'DIR', '/dashboard', 'Layout', NULL, 'House', 1, 1, 1),
(11, 1, '首页', 'MENU', '/dashboard/home', 'dashboard/Home', 'dashboard:view', 'DataBoard', 1, 1, 1),
(2, 0, 'AI能力', 'DIR', '/ai', 'Layout', NULL, 'Cpu', 2, 1, 1),
(21, 2, '应用管理', 'MENU', '/apps', 'app/AppList', 'app:list', 'Grid', 1, 1, 1),
(211, 21, '新增应用', 'BUTTON', NULL, NULL, 'app:create', NULL, 1, 0, 1),
(212, 21, '编辑应用', 'BUTTON', NULL, NULL, 'app:update', NULL, 2, 0, 1),
(213, 21, '删除应用', 'BUTTON', NULL, NULL, 'app:delete', NULL, 3, 0, 1),
(214, 21, '应用参数', 'BUTTON', NULL, NULL, 'app:parameters', NULL, 4, 0, 1),
(22, 2, '知识库管理', 'MENU', '/knowledge', 'knowledge/KnowledgeList', 'knowledge:list', 'Reading', 2, 1, 1),
(221, 22, '新增知识库', 'BUTTON', NULL, NULL, 'knowledge:create', NULL, 1, 0, 1),
(222, 22, '编辑知识库', 'BUTTON', NULL, NULL, 'knowledge:update', NULL, 2, 0, 1),
(223, 22, '删除知识库', 'BUTTON', NULL, NULL, 'knowledge:delete', NULL, 3, 0, 1),
(224, 22, '文档列表', 'BUTTON', NULL, NULL, 'knowledge:document:list', NULL, 4, 0, 1),
(225, 22, '新增文档(文本)', 'BUTTON', NULL, NULL, 'knowledge:document:create:text', NULL, 5, 0, 1),
(226, 22, '新增文档(文件)', 'BUTTON', NULL, NULL, 'knowledge:document:create:file', NULL, 6, 0, 1),
(227, 22, '更新文档(文本)', 'BUTTON', NULL, NULL, 'knowledge:document:update:text', NULL, 7, 0, 1),
(228, 22, '更新文档(文件)', 'BUTTON', NULL, NULL, 'knowledge:document:update:file', NULL, 8, 0, 1),
(229, 22, '删除文档', 'BUTTON', NULL, NULL, 'knowledge:document:delete', NULL, 9, 0, 1),
(2210, 22, '索引状态', 'BUTTON', NULL, NULL, 'knowledge:document:indexing', NULL, 10, 0, 1),
(2211, 22, '文档分段', 'BUTTON', NULL, NULL, 'knowledge:document:segments', NULL, 11, 0, 1),
(2212, 22, '检索测试', 'BUTTON', NULL, NULL, 'knowledge:retrieve', NULL, 12, 0, 1),
(23, 2, '智能聊天', 'MENU', '/chat', 'chat/ChatPanel', 'chat:stream', 'ChatRound', 3, 1, 1),
(231, 23, '停止生成', 'BUTTON', NULL, NULL, 'chat:stop', NULL, 1, 0, 1),
(232, 23, '消息反馈', 'BUTTON', NULL, NULL, 'chat:feedback', NULL, 2, 0, 1),
(233, 23, '建议问题', 'BUTTON', NULL, NULL, 'chat:suggested', NULL, 3, 0, 1),
(234, 23, '语音转文字', 'BUTTON', NULL, NULL, 'chat:audio', NULL, 4, 0, 1),
(24, 2, '会话管理', 'MENU', '/conversations', 'conversation/ConversationList', 'conversation:list', 'Comment', 4, 1, 1),
(241, 24, '消息列表', 'BUTTON', NULL, NULL, 'conversation:messages', NULL, 1, 0, 1),
(242, 24, '重命名会话', 'BUTTON', NULL, NULL, 'conversation:rename', NULL, 2, 0, 1),
(243, 24, '删除会话', 'BUTTON', NULL, NULL, 'conversation:delete', NULL, 3, 0, 1),
(3, 0, '系统管理', 'DIR', '/system', 'Layout', NULL, 'Setting', 3, 1, 1),
(31, 3, '用户管理', 'MENU', '/users', 'user/UserList', 'user:list', 'User', 1, 1, 1),
(311, 31, '创建用户', 'BUTTON', NULL, NULL, 'user:create', NULL, 1, 0, 1),
(312, 31, '更新用户', 'BUTTON', NULL, NULL, 'user:update', NULL, 2, 0, 1),
(313, 31, '修改密码', 'BUTTON', NULL, NULL, 'user:password', NULL, 3, 0, 1),
(314, 31, '变更状态', 'BUTTON', NULL, NULL, 'user:status', NULL, 4, 0, 1),
(315, 31, '分配角色', 'BUTTON', NULL, NULL, 'user:role', NULL, 5, 0, 1),
(316, 31, '删除用户', 'BUTTON', NULL, NULL, 'user:delete', NULL, 6, 0, 1),
(32, 3, '资源授权', 'MENU', '/resource-auth', 'resource/ResourceAuth', 'resource:grant', 'Key', 2, 1, 1),
(321, 32, '撤销授权', 'BUTTON', NULL, NULL, 'resource:revoke', NULL, 1, 0, 1),
(322, 32, '查看授权', 'BUTTON', NULL, NULL, 'resource:view', NULL, 2, 0, 1),
(33, 3, '菜单权限', 'MENU', '/menus', 'menu/MenuList', 'menu:list', 'Menu', 3, 1, 1),
(331, 33, '新增菜单', 'BUTTON', NULL, NULL, 'menu:create', NULL, 1, 0, 1),
(332, 33, '修改菜单', 'BUTTON', NULL, NULL, 'menu:update', NULL, 2, 0, 1),
(333, 33, '删除菜单', 'BUTTON', NULL, NULL, 'menu:delete', NULL, 3, 0, 1),
(334, 33, '查看角色菜单', 'BUTTON', NULL, NULL, 'menu:role:view', NULL, 4, 0, 1),
(335, 33, '分配角色菜单', 'BUTTON', NULL, NULL, 'menu:role:assign', NULL, 5, 0, 1),
(34, 3, '文件管理', 'MENU', '/files', 'file/FileUpload', 'file:upload', 'Upload', 4, 1, 1),
(35, 3, '系统监控', 'MENU', '/system/health', 'system/Health', 'system:health', 'Monitor', 5, 1, 1),
(36, 3, '个人中心', 'MENU', '/profile', 'user/Profile', 'user:me', 'Avatar', 6, 1, 1),
(361, 36, '更新个人资料', 'BUTTON', NULL, NULL, 'user:update:self', NULL, 1, 0, 1),
(362, 36, '修改个人密码', 'BUTTON', NULL, NULL, 'user:password:self', NULL, 2, 0, 1)
ON DUPLICATE KEY UPDATE
    parent_id = VALUES(parent_id),
    menu_name = VALUES(menu_name),
    menu_type = VALUES(menu_type),
    path = VALUES(path),
    component = VALUES(component),
    permission = VALUES(permission),
    icon = VALUES(icon),
    sort = VALUES(sort),
    visible = VALUES(visible),
    status = VALUES(status),
    deleted = 0;

DELETE rm
FROM sys_role_menu rm
         INNER JOIN sys_role r ON rm.role_id = r.id
WHERE r.code IN ('super_admin', 'admin', 'user');

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.id, m.id
FROM `sys_role` r
         JOIN `sys_menu` m ON m.deleted = 0
WHERE r.code = 'super_admin';

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.id, m.id
FROM `sys_role` r
         JOIN `sys_menu` m ON m.deleted = 0
WHERE r.code = 'admin'
  AND m.id NOT IN (33, 331, 332, 333, 334, 335, 315, 316);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.id, m.id
FROM `sys_role` r
         JOIN `sys_menu` m ON m.deleted = 0
WHERE r.code = 'user'
  AND m.id IN (
               1, 11,
               2, 21, 214,
               22, 224, 225, 226, 227, 228, 229, 2210, 2211, 2212,
               23, 231, 232, 233, 234,
               24, 241, 242, 243,
               3, 34, 35, 36, 361, 362
    );