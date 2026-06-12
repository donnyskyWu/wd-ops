-- V53: M10 采集状态 + 数据质量检查类型字典
-- Spec: GLOBAL-CONVENTIONS §2 / STATE-M10 §1.1 / API-M10 §2.2 / PRD-M10 §4.2
-- Fix: 采集任务/采集日志/数据质量页 DictSelect 报「字典 type 不存在」
-- 采集状态 5 态（BACKEND-DEV-PLAN ❶ 统一）：PENDING/RUNNING/SUCCESS/FAILED/PARTIAL
-- 质量检查 5 维（PRD-M10 §4.2.3）：完整性/准确性/一致性/时效性/唯一性

INSERT INTO sys_dict_type (type, name, status) VALUES
('dict_collect_status', '采集状态', 'ENABLED'),
('dict_quality_check_type', '数据质量检查类型', 'ENABLED')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_collect_status', '待执行', 'PENDING', 1, 'ENABLED'),
('dict_collect_status', '执行中', 'RUNNING', 2, 'ENABLED'),
('dict_collect_status', '成功', 'SUCCESS', 3, 'ENABLED'),
('dict_collect_status', '失败', 'FAILED', 4, 'ENABLED'),
('dict_collect_status', '部分成功', 'PARTIAL', 5, 'ENABLED'),
('dict_quality_check_type', '完整性', 'COMPLETENESS', 1, 'ENABLED'),
('dict_quality_check_type', '准确性', 'ACCURACY', 2, 'ENABLED'),
('dict_quality_check_type', '一致性', 'CONSISTENCY', 3, 'ENABLED'),
('dict_quality_check_type', '时效性', 'TIMELINESS', 4, 'ENABLED'),
('dict_quality_check_type', '唯一性', 'UNIQUENESS', 5, 'ENABLED')
ON DUPLICATE KEY UPDATE label = VALUES(label), sort = VALUES(sort);
