-- V33: 补 dict_platform_type.ALL（API-M2 §1.1 SOP 模板 platformType=ALL）
INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status)
SELECT 'dict_platform_type', '全部', 'ALL', 0, 'ENABLED'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_dict_data
    WHERE dict_type = 'dict_platform_type' AND dict_value = 'ALL'
);
