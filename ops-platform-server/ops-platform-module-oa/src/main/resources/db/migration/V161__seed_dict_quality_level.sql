-- V161: Seed dict_quality_level (M10 数据质量等级)
-- Spec: GLOBAL-CONVENTIONS §2 / PRD-M10 §4.2.3 / API-M10 §2.2
-- Fix: 数据质量页 DictSelect 报「字典 type 不存在：dict_quality_level」
-- V53 seeded dict_quality_check_type but omitted dict_quality_level

SET NAMES utf8mb4;

SET @next_type_id = (SELECT COALESCE(MAX(id), 0) + 1 FROM sys_dict_type);

INSERT INTO sys_dict_type (id, type, name, status)
SELECT @next_type_id, 'dict_quality_level', '数据质量等级', 'ENABLED'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE type = 'dict_quality_level');

SET @next_data_id = (SELECT COALESCE(MAX(id), 0) FROM sys_dict_data);

INSERT INTO sys_dict_data (id, dict_type, label, dict_value, sort, status, color_type) VALUES
(@next_data_id + 1, 'dict_quality_level', '优', 'EXCELLENT', 1, 'ENABLED', 'success'),
(@next_data_id + 2, 'dict_quality_level', '良', 'GOOD', 2, 'ENABLED', 'primary'),
(@next_data_id + 3, 'dict_quality_level', '中', 'MEDIUM', 3, 'ENABLED', 'warning'),
(@next_data_id + 4, 'dict_quality_level', '差', 'POOR', 4, 'ENABLED', 'danger')
ON DUPLICATE KEY UPDATE label = VALUES(label), sort = VALUES(sort), color_type = VALUES(color_type);

-- shenyu-system sync: beta test DB has no cross-DB GRANT for shenyu-ops user.
-- Use scripts/integration-config/seed-ops-test-remote-dict.py (dual-connection) instead.
-- Local multidb with cross-DB grants: V158 bulk-sync covers dict_% → shenyu-system on next migrate path.
