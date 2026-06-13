-- M2: 内容审核二级可配置 + 系统参数 seed

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_param_category', '内容审核', 'CONTENT_REVIEW', 5, 'ENABLED'),
('dict_review_stage', '一级审核', 'FIRST_REVIEW', 1, 'ENABLED'),
('dict_review_stage', '二级审核', 'SECOND_REVIEW', 2, 'ENABLED'),
('dict_content_status', '待一级审核', 'PENDING_FIRST_REVIEW', 2, 'ENABLED'),
('dict_content_status', '待二级审核', 'PENDING_SECOND_REVIEW', 3, 'ENABLED')
ON DUPLICATE KEY UPDATE label = VALUES(label);

-- 历史终审队列并入二级审核
UPDATE oa_production_content SET status = 'PENDING_SECOND_REVIEW'
WHERE status = 'PENDING_FINAL_REVIEW' AND deleted = 0;

INSERT INTO sys_param (tenant_id, param_name, param_key, param_value, param_type, category, remark, creator, updater) VALUES
(1, '开启一级审核', 'content.review.level1.enabled', 'true', 'BOOLEAN', 'CONTENT_REVIEW', '关闭后提交审核将跳过一级审核', 'm2-v74', 'm2-v74'),
(1, '开启二级审核', 'content.review.level2.enabled', 'true', 'BOOLEAN', 'CONTENT_REVIEW', '关闭后一级通过后直接发布；两级均关闭则提交后直接发布', 'm2-v74', 'm2-v74'),
(1, '一级审核角色', 'content.review.level1.role', 'OPS_LEADER', 'STRING', 'CONTENT_REVIEW', '默认 IP 组长角色；也可匹配内容所属 IP 组的组长用户', 'm2-v74', 'm2-v74'),
(1, '二级审核角色', 'content.review.level2.role', 'DEPT_HEAD', 'STRING', 'CONTENT_REVIEW', '默认部门负责人角色', 'm2-v74', 'm2-v74')
ON DUPLICATE KEY UPDATE param_name = VALUES(param_name), remark = VALUES(remark);

INSERT INTO sys_role (id, tenant_id, code, name, status, data_scope, remark, creator, updater) VALUES
(6, 1, 'DEPT_HEAD', '部门负责人', 'ENABLED', 'ALL', 'm2-v74 内容二级审核默认角色', 'm2-v74', 'm2-v74')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO sys_user_role (user_id, role_id) VALUES
(1001, 6)
ON DUPLICATE KEY UPDATE user_id = user_id;
