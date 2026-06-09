-- M9-S-01: 用户/角色/权限表扩展

ALTER TABLE sys_user ADD COLUMN email VARCHAR(128) NULL;
ALTER TABLE sys_user ADD COLUMN phone_encrypted VARCHAR(256) NULL;
ALTER TABLE sys_user ADD COLUMN phone_hash VARCHAR(64) NULL;
ALTER TABLE sys_user ADD COLUMN position VARCHAR(64) NULL;
ALTER TABLE sys_user ADD COLUMN ip_group_id BIGINT NULL;
ALTER TABLE sys_user ADD COLUMN remark VARCHAR(512) NULL;

CREATE UNIQUE INDEX uk_sys_user_tenant_username ON sys_user (tenant_id, username);

ALTER TABLE sys_role ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT 'ENABLED';
ALTER TABLE sys_role ADD COLUMN remark VARCHAR(512) NULL;

CREATE TABLE IF NOT EXISTS sys_permission (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(128) NOT NULL,
    name        VARCHAR(128) NOT NULL,
    module      VARCHAR(64)  NULL,
    creator     VARCHAR(64)  DEFAULT 'system',
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater     VARCHAR(64)  DEFAULT 'system',
    update_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     SMALLINT     NOT NULL DEFAULT 0,
    UNIQUE KEY uk_sys_permission_code (code)
);

CREATE TABLE IF NOT EXISTS sys_role_permission (
    id          BIGINT   NOT NULL AUTO_INCREMENT PRIMARY KEY,
    role_id     BIGINT   NOT NULL,
    permission_id BIGINT NOT NULL,
    creator     VARCHAR(64) DEFAULT 'system',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sys_role_permission (role_id, permission_id)
);

-- M9 字典基线
INSERT INTO sys_dict_type (type, name, status) VALUES
('dict_user_status', '用户状态', 'ENABLED'),
('dict_position', '岗位', 'ENABLED')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_user_status', '启用', 'ENABLED', 1, 'ENABLED'),
('dict_user_status', '停用', 'DISABLED', 2, 'ENABLED'),
('dict_user_status', '锁定', 'LOCKED', 3, 'ENABLED'),
('dict_position', '运营组长', 'OPS_LEADER', 1, 'ENABLED'),
('dict_position', '运营专员', 'OPERATOR', 2, 'ENABLED'),
('dict_position', '内容编辑', 'EDITOR', 3, 'ENABLED'),
('dict_position', '主播', 'ANCHOR', 4, 'ENABLED')
ON DUPLICATE KEY UPDATE label = VALUES(label);

-- 权限点注册
INSERT INTO sys_permission (id, code, name, module, creator, updater) VALUES
(1, 'oa:user:list', '用户查询', 'M9', 'm9-auth', 'm9-auth'),
(2, 'oa:user:create', '用户创建', 'M9', 'm9-auth', 'm9-auth'),
(3, 'oa:user:update', '用户更新', 'M9', 'm9-auth', 'm9-auth'),
(4, 'oa:user:delete', '用户删除', 'M9', 'm9-auth', 'm9-auth'),
(5, 'oa:role:list', '角色查询', 'M9', 'm9-auth', 'm9-auth'),
(6, 'oa:role:create', '角色创建', 'M9', 'm9-auth', 'm9-auth'),
(7, 'oa:role:update', '角色更新', 'M9', 'm9-auth', 'm9-auth'),
(8, 'oa:role:delete', '角色删除', 'M9', 'm9-auth', 'm9-auth'),
(9, 'oa:role:assign-permission', '角色授权', 'M9', 'm9-auth', 'm9-auth'),
(10, 'oa:permission:list', '权限点查询', 'M9', 'm9-auth', 'm9-auth');

-- OA_ADMIN 拥有全部 M9 权限
INSERT INTO sys_role_permission (role_id, permission_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 8), (1, 9), (1, 10);
