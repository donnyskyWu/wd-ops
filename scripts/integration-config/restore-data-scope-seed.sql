-- Idempotent restore for data-permission E2E (dev tokens + seed accounts)
USE wd;

-- Base tenants/users/tokens (V2)
INSERT IGNORE INTO sys_tenant (id, name, status) VALUES
(1, 'default', 'ENABLED'),
(2, 'tenant-b', 'ENABLED');

INSERT IGNORE INTO sys_user (id, tenant_id, username, nickname, status) VALUES
(1001, 1, 'oa-admin', 'OA Admin', 'ENABLED'),
(2001, 2, 'tenantb-admin', 'TenantB Admin', 'ENABLED');

INSERT IGNORE INTO sys_role (id, tenant_id, code, name) VALUES
(1, 1, 'OA_ADMIN', 'OA Admin'),
(2, 2, 'TENANT_ADMIN', 'Tenant Admin');

INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES
(1001, 1),
(2001, 2);

INSERT IGNORE INTO sys_user_token (user_id, token, status) VALUES
(1001, 'dev-token-oa-admin', 'ENABLED'),
(2001, 'dev-token-oa-tenantb', 'ENABLED');

-- Permissions for account APIs (V12 + V15 minimal)
INSERT IGNORE INTO sys_permission (id, code, name, module, creator, updater) VALUES
(1, 'ops:user:list', 'user list', 'M9', 'seed-restore', 'seed-restore'),
(5, 'ops:role:list', 'role list', 'M9', 'seed-restore', 'seed-restore'),
(10, 'ops:permission:list', 'perm list', 'M9', 'seed-restore', 'seed-restore'),
(11, 'ops:tenant:list', 'tenant list', 'M9', 'seed-restore', 'seed-restore'),
(12, 'ops:tenant:create', 'tenant create', 'M9', 'seed-restore', 'seed-restore'),
(13, 'ops:tenant:update', 'tenant update', 'M9', 'seed-restore', 'seed-restore'),
(14, 'ops:tenant:delete', 'tenant delete', 'M9', 'seed-restore', 'seed-restore'),
(15, 'ops:account:list', 'account list', 'M4', 'seed-restore', 'seed-restore'),
(16, 'ops:account:create', 'account create', 'M4', 'seed-restore', 'seed-restore');

INSERT IGNORE INTO sys_role_permission (role_id, permission_id) VALUES
(1, 1), (1, 5), (1, 10), (1, 11), (1, 12), (1, 13), (1, 14), (1, 15), (1, 16),
(3, 1), (3, 5), (3, 10), (3, 15),
(4, 1), (4, 15), (4, 16);

-- Auth seed (V15) — roles, operator, tokens
UPDATE sys_role SET data_scope = 'ALL' WHERE id IN (1, 2);

INSERT IGNORE INTO sys_role (id, tenant_id, code, name, status, data_scope, remark, creator, updater) VALUES
(3, 1, 'OPS_LEADER', 'Ops Leader', 'ENABLED', 'ALL', 'seed-auth', 'seed-auth', 'seed-auth'),
(4, 1, 'OPS_OPERATOR', 'Ops Operator', 'ENABLED', 'IP_GROUP', 'seed-auth ip-group scope', 'seed-auth', 'seed-auth'),
(5, 1, 'FINANCE', 'Finance', 'ENABLED', 'ALL', 'seed-auth', 'seed-auth', 'seed-auth'),
(6, 1, 'ip_group_leader', 'IP组长', 'ENABLED', 'SELF', 'seed · IP group leader role', 'seed-auth', 'seed-auth');

INSERT IGNORE INTO sys_user (id, tenant_id, username, nickname, status, position, ip_group_id, remark, creator, updater) VALUES
(1002, 1, 'oa-leader', 'Ops Leader', 'ENABLED', 'OPS_LEADER', NULL, 'seed-auth', 'seed-auth', 'seed-auth'),
(1003, 1, 'oa-operator', 'Ops Operator', 'ENABLED', 'OPERATOR', 9001, 'seed-auth ip-group 9001', 'seed-auth', 'seed-auth'),
(1004, 1, 'oa-finance', 'Finance', 'ENABLED', 'FINANCE', NULL, 'seed-auth', 'seed-auth', 'seed-auth'),
(1005, 1, 'oa-analyst', 'Analyst', 'ENABLED', 'OPERATOR', NULL, 'seed-auth', 'seed-auth', 'seed-auth');

INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES
(1002, 3),
(1002, 6),
(1003, 4),
(1003, 6),
(1004, 5),
(1005, 3);

INSERT IGNORE INTO sys_user_token (user_id, token, status) VALUES
(1002, 'dev-token-oa-leader', 'ENABLED'),
(1003, 'dev-token-oa-operator', 'ENABLED'),
(1004, 'dev-token-oa-finance', 'ENABLED'),
(1005, 'dev-token-oa-analyst', 'ENABLED');

-- Operator membership in IP group 9001
INSERT IGNORE INTO oa_ip_group_member (tenant_id, ip_group_id, user_id, position, is_leader, creator, updater)
VALUES (1, 9001, 1003, 'OPERATOR', 0, 'seed-restore', 'seed-restore');

-- Seed companies/realnames/phones if missing (minimal for account create)
INSERT IGNORE INTO oa_company (id, tenant_id, company_name, credit_code, industry, mp_capacity_standard, mp_registered_count, status, creator, updater)
VALUES
(9001, 1, 'SEED-Company-A', '91110000MA0SEED001', 'Internet', 20, 5, 'ENABLED', 'seed-restore', 'seed-restore'),
(9002, 1, 'SEED-Company-B', '91110000MA0SEED002', 'Media', 15, 3, 'ENABLED', 'seed-restore', 'seed-restore');

INSERT IGNORE INTO oa_realname (id, tenant_id, company_id, real_name, id_type, id_card_encrypted, phone_encrypted, gender, status, account_bound_count, creator, updater)
VALUES
(9001, 1, 9001, 'SEED-Zhang', 'ID_CARD', 'enc', 'enc', 'MALE', 'ENABLED', 2, 'seed-restore', 'seed-restore'),
(9002, 1, 9001, 'SEED-Li', 'ID_CARD', 'enc', 'enc', 'MALE', 'ENABLED', 2, 'seed-restore', 'seed-restore'),
(9003, 1, 9001, 'SEED-Wang', 'ID_CARD', 'enc', 'enc', 'MALE', 'ENABLED', 0, 'seed-restore', 'seed-restore');

INSERT IGNORE INTO oa_phone (id, tenant_id, realname_id, phone_number_encrypted, phone_number_hash, phone_code, phone_model, keeper_id, status, account_bound_count, creator, updater)
VALUES
(9001, 1, 9001, 'enc', 'hash1', 'SEED-PH-001', 'iPhone 15', 1001, 'ENABLED', 2, 'seed-restore', 'seed-restore'),
(9002, 1, 9002, 'enc', 'hash2', 'SEED-PH-002', 'iPhone 14', 1001, 'ENABLED', 2, 'seed-restore', 'seed-restore'),
(9003, 1, 9003, 'enc', 'hash3', 'SEED-PH-003', 'Huawei P60', 1001, 'ENABLED', 0, 'seed-restore', 'seed-restore');

-- Cleanup probe-created accounts (idempotent; realname 9003 rebinding)
DELETE FROM oa_account WHERE tenant_id = 1 AND external_account_id LIKE 'dy_op_9001_%';
UPDATE oa_realname SET account_bound_count = 0 WHERE tenant_id = 1 AND id = 9003;

-- Core platform accounts 9001-9010
INSERT IGNORE INTO oa_account (id, tenant_id, platform_type, account_type, account_name, external_account_id, company_id, realname_id, phone_id, phone_number_hash, status, creator, updater)
VALUES
(9001, 1, 'WECHAT_OFFICIAL', 'OFFICIAL_ACCOUNT', 'SEED-WX-A1', 'seed_mp_a1', 9001, 9001, 9001, 'hash1', 'NORMAL', 'seed-restore', 'seed-restore'),
(9002, 1, 'WECHAT_OFFICIAL', 'OFFICIAL_ACCOUNT', 'SEED-WX-A2', 'seed_mp_a2', 9001, 9002, 9002, 'hash2', 'NORMAL', 'seed-restore', 'seed-restore'),
(9003, 1, 'WECHAT_OFFICIAL', 'SERVICE_ACCOUNT',  'SEED-WX-A3', 'seed_mp_a3', 9001, 9001, 9001, 'hash1', 'NORMAL', 'seed-restore', 'seed-restore'),
(9004, 1, 'WECHAT_VIDEO',    'PERSONAL_ACCOUNT', 'SEED-WV-B1', 'seed_v_b1',  9002, 9002, 9002, 'hash2', 'NORMAL', 'seed-restore', 'seed-restore'),
(9005, 1, 'WECHAT_VIDEO',    'PERSONAL_ACCOUNT', 'SEED-WV-B2', 'seed_v_b2',  9002, 9002, 9002, 'hash2', 'NORMAL', 'seed-restore', 'seed-restore'),
(9006, 1, 'DOUYIN',          'PERSONAL_ACCOUNT', 'SEED-DY-1',  'seed_dy_1',  9001, 9001, 9001, 'hash1', 'NORMAL', 'seed-restore', 'seed-restore'),
(9007, 1, 'DOUYIN',          'PERSONAL_ACCOUNT', 'SEED-DY-2',  'seed_dy_2',  9001, 9002, 9002, 'hash2', 'NORMAL', 'seed-restore', 'seed-restore'),
(9008, 1, 'KUAISHOU',        'PERSONAL_ACCOUNT', 'SEED-KS-1',  'seed_ks_1',  9002, 9002, 9002, 'hash2', 'NORMAL', 'seed-restore', 'seed-restore'),
(9009, 1, 'XIAOHONGSHU',     'PERSONAL_ACCOUNT', 'SEED-XHS-1',  'seed_xhs1',  9002, 9002, 9002, 'hash2', 'NORMAL', 'seed-restore', 'seed-restore'),
(9010, 1, 'WECHAT_OFFICIAL', 'OFFICIAL_ACCOUNT', 'SEED-WX-B1', 'seed_mp_b1', 9002, 9002, 9002, 'hash2', 'NORMAL', 'seed-restore', 'seed-restore');

-- BR-006 IP group assignment + extra DOUYIN in group 9001 for list tests
UPDATE oa_account SET ip_group_id = 9001 WHERE tenant_id = 1 AND id IN (9001, 9002, 9003, 9004, 9005);
UPDATE oa_account SET ip_group_id = 9002 WHERE tenant_id = 1 AND id IN (9006, 9007, 9008, 9009, 9010);

INSERT IGNORE INTO oa_account (id, tenant_id, platform_type, account_type, account_name, external_account_id, company_id, realname_id, phone_id, phone_number_hash, status, ip_group_id, creator, updater)
VALUES
(9011, 1, 'DOUYIN', 'PERSONAL_ACCOUNT', 'SEED-DY-A3', 'seed_dy_a3', 9001, 9001, 9001, 'hash1', 'NORMAL', 9001, 'seed-restore', 'seed-restore'),
(9012, 1, 'DOUYIN', 'PERSONAL_ACCOUNT', 'SEED-DY-A4', 'seed_dy_a4', 9001, 9001, 9001, 'hash1', 'NORMAL', 9001, 'seed-restore', 'seed-restore'),
(9013, 1, 'DOUYIN', 'PERSONAL_ACCOUNT', 'SEED-DY-A5', 'seed_dy_a5', 9001, 9002, 9002, 'hash2', 'NORMAL', 9001, 'seed-restore', 'seed-restore');

UPDATE sys_user SET position = 'ADMIN', remark = 'seed-auth admin' WHERE id = 1001;

ALTER TABLE sys_user_token MODIFY id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE sys_user_role MODIFY id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE sys_role_permission MODIFY id BIGINT NOT NULL AUTO_INCREMENT;
