-- Align dict_perf_period with GLOBAL-CONVENTIONS (周/月/季/年/自定义)
SET @next_data_id = (SELECT COALESCE(MAX(id), 0) FROM sys_dict_data);

INSERT INTO sys_dict_data (id, dict_type, label, dict_value, sort, status) VALUES
(@next_data_id + 1, 'dict_perf_period', '周度', 'WEEK', 3, 'ENABLED'),
(@next_data_id + 2, 'dict_perf_period', '年度', 'YEAR', 4, 'ENABLED'),
(@next_data_id + 3, 'dict_perf_period', '自定义', 'CUSTOM', 5, 'ENABLED')
ON DUPLICATE KEY UPDATE label = VALUES(label), sort = VALUES(sort), status = VALUES(status);
