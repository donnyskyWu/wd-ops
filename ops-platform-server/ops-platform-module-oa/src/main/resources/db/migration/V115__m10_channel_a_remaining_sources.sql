-- M10-API-S-06: Channel-A 剩余平台采集源字典

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_collect_source', '小红书 Cookie 采集', 'XIAOHONGSHU_OPEN_API', 8, 'ENABLED'),
('dict_collect_source', 'Bilibili Cookie 采集', 'BILIBILI_OPEN_API', 9, 'ENABLED')
ON DUPLICATE KEY UPDATE label = VALUES(label), sort = VALUES(sort);
