-- V35: 补 dict_industry（公司行业）
INSERT INTO sys_dict_type (type, name, status) VALUES
('dict_industry', '行业', 'ENABLED')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_industry', '新媒体', 'new_media', 1, 'ENABLED'),
('dict_industry', 'MCN', 'mcn', 2, 'ENABLED'),
('dict_industry', '文化传媒', 'media', 3, 'ENABLED'),
('dict_industry', '教育', 'education', 4, 'ENABLED'),
('dict_industry', '电商', 'ecommerce', 5, 'ENABLED');
