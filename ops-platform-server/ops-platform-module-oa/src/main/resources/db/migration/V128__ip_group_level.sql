-- V128: IP 组等级字段 + dict_ip_group_level 字典 (S/A/B/C)
-- sys_dict_type / sys_dict_data 在部分环境 id 无 AUTO_INCREMENT，须显式分配 id

SET @col_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'oa_ip_group'
      AND COLUMN_NAME = 'level'
);
SET @ddl := IF(
    @col_exists = 0,
    'ALTER TABLE oa_ip_group ADD COLUMN level VARCHAR(8) NULL COMMENT ''IP组等级 dict_ip_group_level''',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @next_type_id = (SELECT COALESCE(MAX(id), 0) + 1 FROM sys_dict_type);

INSERT INTO sys_dict_type (id, type, name, status)
SELECT @next_type_id, 'dict_ip_group_level', 'IP组等级', 'ENABLED'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE type = 'dict_ip_group_level');

SET @next_data_id = (SELECT COALESCE(MAX(id), 0) FROM sys_dict_data);

INSERT INTO sys_dict_data (id, dict_type, label, dict_value, sort, status) VALUES
(@next_data_id + 1, 'dict_ip_group_level', 'S级', 'S', 1, 'ENABLED'),
(@next_data_id + 2, 'dict_ip_group_level', 'A级', 'A', 2, 'ENABLED'),
(@next_data_id + 3, 'dict_ip_group_level', 'B级', 'B', 3, 'ENABLED'),
(@next_data_id + 4, 'dict_ip_group_level', 'C级', 'C', 4, 'ENABLED')
ON DUPLICATE KEY UPDATE label = VALUES(label), sort = VALUES(sort), status = VALUES(status);
