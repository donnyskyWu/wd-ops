-- M4 S-06: 平台账号强关联字段扩展

ALTER TABLE oa_account ADD COLUMN company_id BIGINT NULL;
ALTER TABLE oa_account ADD COLUMN realname_id BIGINT NULL;
ALTER TABLE oa_account ADD COLUMN intermediary_id BIGINT NULL;
ALTER TABLE oa_account ADD COLUMN account_type VARCHAR(32) NULL;
ALTER TABLE oa_account ADD COLUMN ip_group_id BIGINT NULL;
ALTER TABLE oa_account ADD COLUMN cookie_encrypted VARCHAR(512) NULL;

CREATE UNIQUE INDEX uk_oa_account_platform_ext ON oa_account (tenant_id, platform_type, external_account_id);

INSERT INTO sys_dict_type (type, name, status) VALUES
('dict_account_type', '账号类型', 'ENABLED');

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_account_type', '官方账号', 'OFFICIAL_ACCOUNT', 1, 'ENABLED'),
('dict_account_type', '个人账号', 'PERSONAL_ACCOUNT', 2, 'ENABLED'),
('dict_account_type', '服务号', 'SERVICE_ACCOUNT', 3, 'ENABLED'),
('dict_platform_type', '视频号', 'WECHAT_VIDEO', 4, 'ENABLED'),
('dict_platform_type', '快手', 'KUAISHOU', 5, 'ENABLED'),
('dict_platform_type', '小红书', 'XIAOHONGSHU', 6, 'ENABLED');
