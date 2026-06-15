-- M2 dict gaps: layout import job status + plan task extended statuses

INSERT INTO sys_dict_type (type, name, status) VALUES
('dict_layout_import_job_status', '版式导入任务状态', 'ENABLED')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_layout_import_job_status', '待处理', 'PENDING', 1, 'ENABLED'),
('dict_layout_import_job_status', '处理中', 'RUNNING', 2, 'ENABLED'),
('dict_layout_import_job_status', '成功', 'SUCCESS', 3, 'ENABLED'),
('dict_layout_import_job_status', '失败', 'FAILED', 4, 'ENABLED'),
('dict_sop_node_status', '计划草稿', 'PLAN_DRAFT', 8, 'ENABLED'),
('dict_sop_node_status', '已终止', 'TERMINATED', 9, 'ENABLED')
ON DUPLICATE KEY UPDATE label = VALUES(label);
