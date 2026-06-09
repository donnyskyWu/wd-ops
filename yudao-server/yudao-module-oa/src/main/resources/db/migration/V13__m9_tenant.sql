-- M9-S-02: 租户管理表扩展

ALTER TABLE sys_tenant ADD COLUMN contact_name VARCHAR(64) NULL;
ALTER TABLE sys_tenant ADD COLUMN contact_phone VARCHAR(32) NULL;
ALTER TABLE sys_tenant ADD COLUMN contact_email VARCHAR(128) NULL;
ALTER TABLE sys_tenant ADD COLUMN expire_time TIMESTAMP NULL;
ALTER TABLE sys_tenant ADD COLUMN max_accounts INT NOT NULL DEFAULT 10;
ALTER TABLE sys_tenant ADD COLUMN remark VARCHAR(512) NULL;

CREATE UNIQUE INDEX uk_sys_tenant_name ON sys_tenant (name);

-- 租户状态字典（STATE-M9）
INSERT INTO sys_dict_type (type, name, status) VALUES
('dict_tenant_status', '租户状态', 'ENABLED')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_tenant_status', '正常', 'NORMAL', 1, 'ENABLED'),
('dict_tenant_status', '试用', 'TRIAL', 2, 'ENABLED'),
('dict_tenant_status', '已到期', 'EXPIRED', 3, 'ENABLED'),
('dict_tenant_status', '已停用', 'DISABLED', 4, 'ENABLED')
ON DUPLICATE KEY UPDATE label = VALUES(label);

-- 历史 seed 状态对齐 STATE-M9
UPDATE sys_tenant SET status = 'NORMAL' WHERE status = 'ENABLED';

UPDATE sys_tenant SET
  contact_name = '默认租户联系人',
  contact_phone = '13800138000',
  contact_email = 'admin@tenant1.local',
  expire_time = TIMESTAMP '2027-12-31 23:59:59',
  max_accounts = 100,
  remark = 'seed-base 默认租户'
WHERE id = 1;

UPDATE sys_tenant SET
  contact_name = '租户B联系人',
  contact_phone = '13800138099',
  contact_email = 'admin@tenant2.local',
  expire_time = TIMESTAMP '2027-12-31 23:59:59',
  max_accounts = 50,
  remark = 'seed-base 隔离租户'
WHERE id = 2;
