-- seed-auth: 多用户/多角色/多 Token + 权限点 + 数据范围（BR-006）

ALTER TABLE sys_role ADD COLUMN data_scope VARCHAR(32) NOT NULL DEFAULT 'ALL';

UPDATE sys_role SET data_scope = 'ALL' WHERE id IN (1, 2);

-- 扩展权限点（租户 + 账号查询）
INSERT INTO sys_permission (id, code, name, module, creator, updater) VALUES
(11, 'oa:tenant:list', '租户查询', 'M9', 'seed-auth', 'seed-auth'),
(12, 'oa:tenant:create', '租户创建', 'M9', 'seed-auth', 'seed-auth'),
(13, 'oa:tenant:update', '租户更新', 'M9', 'seed-auth', 'seed-auth'),
(14, 'oa:tenant:delete', '租户删除', 'M9', 'seed-auth', 'seed-auth'),
(15, 'oa:account:list', '账号查询', 'M4', 'seed-auth', 'seed-auth')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- OA_ADMIN 补全租户 + 账号权限
INSERT INTO sys_role_permission (role_id, permission_id) VALUES
(1, 11), (1, 12), (1, 13), (1, 14), (1, 15);

-- tenant=1 业务角色
INSERT INTO sys_role (id, tenant_id, code, name, status, data_scope, remark, creator, updater) VALUES
(3, 1, 'OPS_LEADER', '运营组长', 'ENABLED', 'ALL', 'seed-auth', 'seed-auth', 'seed-auth'),
(4, 1, 'OPS_OPERATOR', '运营专员', 'ENABLED', 'IP_GROUP', 'seed-auth · IP组数据范围', 'seed-auth', 'seed-auth'),
(5, 1, 'FINANCE', '财务', 'ENABLED', 'ALL', 'seed-auth', 'seed-auth', 'seed-auth');

-- 运营组长：用户/角色/账号查询
INSERT INTO sys_role_permission (role_id, permission_id) VALUES
(3, 1), (3, 5), (3, 10), (3, 15);

-- 运营专员：用户查询 + 账号查询（无租户写权限）
INSERT INTO sys_role_permission (role_id, permission_id) VALUES
(4, 1), (4, 15);

-- 财务：用户查询
INSERT INTO sys_role_permission (role_id, permission_id) VALUES
(5, 1);

-- 租户 B 管理员：用户/角色查询
INSERT INTO sys_role_permission (role_id, permission_id) VALUES
(2, 1), (2, 5), (2, 10);

-- 扩展用户（5–8 人）
INSERT INTO sys_user (id, tenant_id, username, nickname, status, position, ip_group_id, remark, creator, updater) VALUES
(1002, 1, 'oa-leader', '运营组长', 'ENABLED', 'OPS_LEADER', NULL, 'seed-auth', 'seed-auth', 'seed-auth'),
(1003, 1, 'oa-operator', '运营专员', 'ENABLED', 'OPERATOR', 9001, 'seed-auth · IP组9001', 'seed-auth', 'seed-auth'),
(1004, 1, 'oa-finance', '财务', 'ENABLED', 'FINANCE', NULL, 'seed-auth', 'seed-auth', 'seed-auth'),
(1005, 1, 'oa-analyst', '数据分析', 'ENABLED', 'OPERATOR', NULL, 'seed-auth', 'seed-auth', 'seed-auth');

INSERT INTO sys_user_role (user_id, role_id) VALUES
(1002, 3),
(1003, 4),
(1004, 5),
(1005, 3);

INSERT INTO sys_user_token (user_id, token, status) VALUES
(1002, 'dev-token-oa-leader', 'ENABLED'),
(1003, 'dev-token-oa-operator', 'ENABLED'),
(1004, 'dev-token-oa-finance', 'ENABLED'),
(1005, 'dev-token-oa-analyst', 'ENABLED');

-- 完善管理员信息
UPDATE sys_user SET position = 'ADMIN', remark = 'seed-auth 系统管理员' WHERE id = 1001;
UPDATE sys_user SET remark = 'seed-auth 租户B管理员' WHERE id = 2001;

-- BR-006：SEED 账号按 IP 组分组（9001×5 / 9002×5）
UPDATE oa_account SET ip_group_id = 9001 WHERE tenant_id = 1 AND id IN (9001, 9002, 9003, 9004, 9005);
UPDATE oa_account SET ip_group_id = 9002 WHERE tenant_id = 1 AND id IN (9006, 9007, 9008, 9009, 9010);
