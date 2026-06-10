-- V34: 补 dict_perf_grade（绩效结果等级筛选）
INSERT INTO sys_dict_type (type, name, status) VALUES
('dict_perf_grade', '绩效等级', 'ENABLED')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_perf_grade', 'S（优秀）', 'S', 1, 'ENABLED'),
('dict_perf_grade', 'A（良好）', 'A', 2, 'ENABLED'),
('dict_perf_grade', 'B（合格）', 'B', 3, 'ENABLED'),
('dict_perf_grade', 'C（待改进）', 'C', 4, 'ENABLED'),
('dict_perf_grade', 'D（不合格）', 'D', 5, 'ENABLED');
