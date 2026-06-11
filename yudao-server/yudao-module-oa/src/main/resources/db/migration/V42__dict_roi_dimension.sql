-- ROI 分析维度字典（M5 RoiAnalysis DictSelect）
INSERT INTO sys_dict_type (type, name, status) VALUES
('dict_roi_dimension', 'ROI分析维度', 'ENABLED')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_roi_dimension', 'IP组', 'IP_GROUP', 1, 'ENABLED'),
('dict_roi_dimension', '账号', 'ACCOUNT', 2, 'ENABLED'),
('dict_roi_dimension', '人员', 'PERSON', 3, 'ENABLED')
ON DUPLICATE KEY UPDATE label = VALUES(label), sort = VALUES(sort);
