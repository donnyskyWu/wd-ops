-- M4: 手机管理字段扩展 + 手机卡状态字典

ALTER TABLE oa_phone ADD COLUMN settings_screenshot_key VARCHAR(512) NULL;
ALTER TABLE oa_phone ADD COLUMN front_image_key VARCHAR(512) NULL;
ALTER TABLE oa_phone ADD COLUMN back_image_key VARCHAR(512) NULL;
ALTER TABLE oa_phone ADD COLUMN purchase_batch VARCHAR(64) NULL;
ALTER TABLE oa_phone ADD COLUMN purchase_date DATE NULL;
ALTER TABLE oa_phone ADD COLUMN purchase_time TIME NULL;
ALTER TABLE oa_phone ADD COLUMN handler_name VARCHAR(64) NULL;
ALTER TABLE oa_phone ADD COLUMN device_number VARCHAR(64) NULL;
ALTER TABLE oa_phone ADD COLUMN is_aochuang VARCHAR(8) NULL;
ALTER TABLE oa_phone ADD COLUMN phone_type VARCHAR(32) NULL;

INSERT INTO sys_dict_type (type, name, status) VALUES
('dict_phone_type', '手机类型', 'ENABLED');

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_phone_type', 'Android', 'ANDROID', 1, 'ENABLED'),
('dict_phone_type', 'iPhone', 'IPHONE', 2, 'ENABLED'),
('dict_sim_status', '损坏', 'DAMAGED', 3, 'ENABLED'),
('dict_sim_status', '丢失', 'LOST', 4, 'ENABLED');
