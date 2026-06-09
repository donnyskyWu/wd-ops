-- seed-base (S0)

INSERT INTO sys_tenant (id, name, status) VALUES
(1, 'default', 'ENABLED'),
(2, 'tenant-b', 'ENABLED');

INSERT INTO sys_user (id, tenant_id, username, nickname, status) VALUES
(1001, 1, 'oa-admin', '系统管理员', 'ENABLED'),
(2001, 2, 'tenantb-admin', '租户B管理员', 'ENABLED');

INSERT INTO sys_role (id, tenant_id, code, name) VALUES
(1, 1, 'OA_ADMIN', '系统管理员'),
(2, 2, 'TENANT_ADMIN', '租户管理员');

INSERT INTO sys_user_role (user_id, role_id) VALUES
(1001, 1),
(2001, 2);

INSERT INTO sys_user_token (user_id, token, status) VALUES
(1001, 'dev-token-oa-admin', 'ENABLED'),
(2001, 'dev-token-oa-tenantb', 'ENABLED');

INSERT INTO sys_dict_type (type, name, status) VALUES
('dict_platform_type', '平台类型', 'ENABLED'),
('dict_yes_no', '是否', 'ENABLED');

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_platform_type', '微信公众号', 'WECHAT_OFFICIAL', 1, 'ENABLED'),
('dict_platform_type', '抖音', 'DOUYIN', 2, 'ENABLED'),
('dict_yes_no', '是', 'YES', 1, 'ENABLED'),
('dict_yes_no', '否', 'NO', 2, 'ENABLED');

INSERT INTO oa_demo_item (id, tenant_id, name) VALUES
(1, 1, 'tenant-1-item'),
(2, 2, 'tenant-2-item');
