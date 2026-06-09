-- V27: dict_author_type / dict_anchor_type 扩展（ADR-006）
-- 仅追加，不删改 V17 已有值（保留 ARTICLE，已被 V18 seed-ops 引用）

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_author_type', '直播',           'LIVE',         0, 'ENABLED'),
('dict_author_type', '直播+短视频',     'BOTH',         3, 'ENABLED'),
('dict_author_type', '图文',           'IMAGE_TEXT',   4, 'ENABLED');

-- 直播主播（V17 V18 中 oa_ip_group_anchor_rel.anchor_type 已用 LIVE，字典补齐）
INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_anchor_type', '直播', 'LIVE', 0, 'ENABLED')
ON DUPLICATE KEY UPDATE label = VALUES(label);
