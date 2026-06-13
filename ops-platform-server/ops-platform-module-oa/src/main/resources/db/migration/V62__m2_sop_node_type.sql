-- S-10: dict_sop_node_type 三值 + oa_sop_node.node_type（ADR-016）

INSERT INTO sys_dict_type (type, name, status) VALUES
('dict_sop_node_type', 'SOP节点类型', 'ENABLED')
ON DUPLICATE KEY UPDATE name = VALUES(name);

DELETE FROM sys_dict_data WHERE dict_type = 'dict_sop_node_type';

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_sop_node_type', '内容生成', 'CONTENT_GENERATION', 1, 'ENABLED'),
('dict_sop_node_type', '内容发布', 'CONTENT_PUBLISH', 2, 'ENABLED'),
('dict_sop_node_type', '普通节点', 'NORMAL', 3, 'ENABLED');

ALTER TABLE oa_sop_node
    ADD COLUMN node_type VARCHAR(30) NOT NULL DEFAULT 'NORMAL' COMMENT 'dict_sop_node_type' AFTER node_order;

UPDATE oa_sop_node SET node_type = 'CONTENT_GENERATION' WHERE id IN (9402, 9404);
UPDATE oa_sop_node SET node_type = 'CONTENT_PUBLISH' WHERE id = 9403;
