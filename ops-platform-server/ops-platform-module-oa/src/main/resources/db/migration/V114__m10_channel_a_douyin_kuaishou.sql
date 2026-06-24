-- M10-API-S-06: Channel-A KUAISHOU 采集源字典（DOUYIN 沿用 V103 DOUYIN_OPEN_API）

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_collect_source', '快手 Cookie 采集', 'KUAISHOU_OPEN_API', 7, 'ENABLED')
ON DUPLICATE KEY UPDATE label = VALUES(label), sort = VALUES(sort);
