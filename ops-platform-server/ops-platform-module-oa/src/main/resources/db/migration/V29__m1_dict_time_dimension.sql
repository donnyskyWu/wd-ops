-- V29: 补 time_dimension 字典（spec: API-M1-运营管理 §4.2 timeDimension=DAY/WEEK/MONTH）
-- 用于：粉丝分析、人效盘点、运营管理时间维度切换
-- S-R2-Phase2: 修复"粉丝分析 字典 type 不存在：dict_time_dimension"

INSERT INTO sys_dict_type (type, name, status) VALUES
('dict_time_dimension', '时间维度', 'ENABLED');

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_time_dimension', '按日', 'DAY', 1, 'ENABLED'),
('dict_time_dimension', '按周', 'WEEK', 2, 'ENABLED'),
('dict_time_dimension', '按月', 'MONTH', 3, 'ENABLED');
