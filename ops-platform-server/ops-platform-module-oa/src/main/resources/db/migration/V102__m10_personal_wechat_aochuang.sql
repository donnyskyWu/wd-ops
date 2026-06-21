-- M10-AO-S-01: 个微表奥创设备绑定扩展（ADR-045 §1.3 · Q4/Q5）

ALTER TABLE oa_personal_wechat_account ADD COLUMN aochuang_wechat_account_id VARCHAR(64) NULL;
ALTER TABLE oa_personal_wechat_account ADD COLUMN aochuang_account_ref_id BIGINT NULL;
ALTER TABLE oa_personal_wechat_account ADD COLUMN aochuang_bind_status VARCHAR(32) NOT NULL DEFAULT 'UNBOUND';
ALTER TABLE oa_personal_wechat_account ADD COLUMN aochuang_nickname VARCHAR(200) NULL;
ALTER TABLE oa_personal_wechat_account ADD COLUMN aochuang_avatar VARCHAR(512) NULL;
ALTER TABLE oa_personal_wechat_account ADD COLUMN aochuang_is_alive SMALLINT NULL;
ALTER TABLE oa_personal_wechat_account ADD COLUMN last_device_sync_at TIMESTAMP NULL;
ALTER TABLE oa_personal_wechat_account ADD COLUMN last_friend_sync_at TIMESTAMP NULL;
ALTER TABLE oa_personal_wechat_account ADD COLUMN last_message_sync_at TIMESTAMP NULL;
ALTER TABLE oa_personal_wechat_account ADD COLUMN collect_status VARCHAR(32) NULL;

CREATE UNIQUE INDEX uk_oa_pwa_ao_device ON oa_personal_wechat_account (tenant_id, aochuang_wechat_account_id);

INSERT INTO sys_dict_type (type, name, status) VALUES
('dict_aochuang_bind_status', '奥创设备绑定状态', 'ENABLED')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_aochuang_bind_status', '未绑定', 'UNBOUND', 1, 'ENABLED'),
('dict_aochuang_bind_status', '自动绑定', 'AUTO', 2, 'ENABLED'),
('dict_aochuang_bind_status', '手工绑定', 'MANUAL', 3, 'ENABLED'),
('dict_aochuang_bind_status', '待绑定', 'PENDING', 4, 'ENABLED')
ON DUPLICATE KEY UPDATE label = VALUES(label);
