-- M10: 采集任务「已停止」状态（启停调度）
INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status)
VALUES ('dict_collect_status', '已停止', 'STOPPED', 6, 'ENABLED')
ON DUPLICATE KEY UPDATE label = VALUES(label), sort = VALUES(sort), status = VALUES(status);
