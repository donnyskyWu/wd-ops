-- M2 已发布内容转知识库（ADR-023）

ALTER TABLE oa_production_content
    ADD COLUMN transferred_to_knowledge TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已转知识库';

ALTER TABLE oa_production_content
    ADD COLUMN knowledge_id BIGINT NULL COMMENT '关联 oa_knowledge_base.id';

CREATE INDEX idx_oa_prod_content_knowledge ON oa_production_content (tenant_id, knowledge_id);

INSERT INTO sys_permission (id, code, name, module, creator, updater) VALUES
(49, 'oa:content:transfer-knowledge', '内容转知识库', 'M2', 's15', 's15')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO sys_role_permission (role_id, permission_id) VALUES
(1, 49), (3, 49), (4, 49)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);
