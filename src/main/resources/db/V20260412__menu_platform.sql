USE aihub;

ALTER TABLE sys_menu
    ADD COLUMN platform VARCHAR(20) NOT NULL DEFAULT 'pc'
        COMMENT '所属平台：pc=管理后台 h5=移动端 all=全平台' AFTER status;

CREATE INDEX idx_platform ON sys_menu(platform);

-- 存量数据默认值已覆盖为 pc，如需强制回填可执行：
-- UPDATE sys_menu SET platform = 'pc' WHERE platform = 'pc';
