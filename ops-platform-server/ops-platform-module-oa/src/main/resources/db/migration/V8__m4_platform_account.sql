-- M4 S-06: 平台账号强关联字段扩展
-- 为oa_account表添加公司、实名人、中介、账号类型、IP组和Cookie等关联字段

ALTER TABLE oa_account ADD COLUMN company_id BIGINT NULL COMMENT '所属公司ID';
ALTER TABLE oa_account ADD COLUMN realname_id BIGINT NULL COMMENT '实名人ID';
ALTER TABLE oa_account ADD COLUMN intermediary_id BIGINT NULL COMMENT '中介ID';
ALTER TABLE oa_account ADD COLUMN account_type VARCHAR(32) NULL COMMENT '账号类型: OFFICIAL_ACCOUNT-官方账号, PERSONAL_ACCOUNT-个人账号, SERVICE_ACCOUNT-服务号';
ALTER TABLE oa_account ADD COLUMN ip_group_id BIGINT NULL COMMENT 'IP组ID';
ALTER TABLE oa_account ADD COLUMN cookie_encrypted VARCHAR(512) NULL COMMENT 'Cookie（加密）';

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
