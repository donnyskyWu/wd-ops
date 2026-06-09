-- seed-base 字典基线（与 Flyway V2 同步，供手工灌库 / 文档引用）
-- 用法: mysql -u root -p oa_platform < db/init_dict.sql

INSERT IGNORE INTO sys_dict_type (type, name, status) VALUES
('dict_platform_type', '平台类型', 'ENABLED'),
('dict_account_type', '账号类型', 'ENABLED'),
('dict_account_status', '账号状态', 'ENABLED'),
('dict_realname_status', '实名人状态', 'ENABLED'),
('dict_phone_status', '手机状态', 'ENABLED'),
('dict_sim_status', '手机卡状态', 'ENABLED'),
('dict_company_status', '公司状态', 'ENABLED'),
('dict_yes_no', '是否', 'ENABLED');

INSERT IGNORE INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_platform_type', '微信公众号', 'WECHAT_OFFICIAL', 1, 'ENABLED'),
('dict_platform_type', '抖音', 'DOUYIN', 2, 'ENABLED'),
('dict_account_type', '企业号', 'ENTERPRISE', 1, 'ENABLED'),
('dict_account_type', '个人号', 'PERSONAL', 2, 'ENABLED'),
('dict_account_status', '正常', 'NORMAL', 1, 'ENABLED'),
('dict_account_status', '停用', 'DISABLED', 2, 'ENABLED'),
('dict_yes_no', '是', 'YES', 1, 'ENABLED'),
('dict_yes_no', '否', 'NO', 2, 'ENABLED');
