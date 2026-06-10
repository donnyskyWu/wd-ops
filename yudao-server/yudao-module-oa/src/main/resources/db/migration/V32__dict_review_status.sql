-- V32: 补 dict_review_status（SOP 审核筛选用）
INSERT INTO sys_dict_type (type, name, status) VALUES
('dict_review_status', '审核状态', 'ENABLED')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_review_status', '待审核', 'PENDING', 1, 'ENABLED'),
('dict_review_status', '审核中', 'REVIEWING', 2, 'ENABLED'),
('dict_review_status', '已通过', 'APPROVED', 3, 'ENABLED'),
('dict_review_status', '已驳回', 'REJECTED', 4, 'ENABLED');
