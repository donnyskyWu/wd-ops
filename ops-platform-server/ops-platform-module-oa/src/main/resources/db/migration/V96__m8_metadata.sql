-- ADR-046: metadata entity & field for query condition rendering

CREATE TABLE IF NOT EXISTS sys_metadata_entity (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    entity_code     VARCHAR(64)  NOT NULL,
    entity_name     VARCHAR(128) NOT NULL,
    physical_table  VARCHAR(128) NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ENABLED',
    remark          VARCHAR(512) NULL,
    creator         VARCHAR(64)  DEFAULT 'system',
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         VARCHAR(64)  DEFAULT 'system',
    update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT     NOT NULL DEFAULT 0,
    UNIQUE KEY uk_sys_metadata_entity_code (tenant_id, entity_code),
    UNIQUE KEY uk_sys_metadata_entity_table (tenant_id, physical_table),
    KEY idx_sys_metadata_entity_tenant (tenant_id, status)
);

CREATE TABLE IF NOT EXISTS sys_metadata_field (
    id                    BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id             BIGINT       NOT NULL,
    entity_id             BIGINT       NOT NULL,
    field_code            VARCHAR(64)  NOT NULL,
    field_name            VARCHAR(128) NOT NULL,
    column_name           VARCHAR(128) NOT NULL,
    data_type             VARCHAR(32)  NOT NULL DEFAULT 'VARCHAR',
    query_condition_type  VARCHAR(32)  NOT NULL DEFAULT 'TEXT',
    dict_type             VARCHAR(64)  NULL,
    selector_config       JSON         NULL,
    sort                  INT          NOT NULL DEFAULT 0,
    creator               VARCHAR(64)  DEFAULT 'system',
    create_time           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater               VARCHAR(64)  DEFAULT 'system',
    update_time           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted               SMALLINT     NOT NULL DEFAULT 0,
    UNIQUE KEY uk_sys_metadata_field_code (tenant_id, entity_id, field_code),
    KEY idx_sys_metadata_field_entity (tenant_id, entity_id)
);

INSERT INTO sys_dict_type (type, name, status) VALUES
('dict_metadata_query_condition_type', '元数据查询条件类别', 'ENABLED'),
('dict_metadata_entity_status', '元数据实体状态', 'ENABLED')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_metadata_query_condition_type', '文本', 'TEXT', 10, 'ENABLED'),
('dict_metadata_query_condition_type', '数值', 'NUMBER', 20, 'ENABLED'),
('dict_metadata_query_condition_type', '日期', 'DATE', 30, 'ENABLED'),
('dict_metadata_query_condition_type', '日期范围', 'DATE_RANGE', 40, 'ENABLED'),
('dict_metadata_query_condition_type', '枚举字典', 'DICT', 50, 'ENABLED'),
('dict_metadata_query_condition_type', 'IP组选择', 'IP_GROUP_SELECT', 60, 'ENABLED'),
('dict_metadata_query_condition_type', '人员选择', 'USER_SELECT', 70, 'ENABLED'),
('dict_metadata_query_condition_type', '平台选择', 'PLATFORM_SELECT', 80, 'ENABLED'),
('dict_metadata_query_condition_type', '账号选择', 'ACCOUNT_SELECT', 90, 'ENABLED'),
('dict_metadata_query_condition_type', '赛事选择', 'COMPETITION_SELECT', 100, 'ENABLED'),
('dict_metadata_entity_status', '启用', 'ENABLED', 1, 'ENABLED'),
('dict_metadata_entity_status', '停用', 'DISABLED', 2, 'ENABLED')
ON DUPLICATE KEY UPDATE label = VALUES(label);

INSERT INTO sys_permission (id, code, name, module, creator, updater) VALUES
(59, 'oa:metadata:query', '元数据查询', 'M8', 'adr-046', 'adr-046'),
(60, 'oa:metadata:create', '元数据创建', 'M8', 'adr-046', 'adr-046'),
(61, 'oa:metadata:update', '元数据更新', 'M8', 'adr-046', 'adr-046'),
(62, 'oa:metadata:delete', '元数据删除', 'M8', 'adr-046', 'adr-046')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO sys_role_permission (role_id, permission_id) VALUES
(1, 59), (1, 60), (1, 61), (1, 62)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- Seed: oa_content entity for tenant 1
INSERT INTO sys_metadata_entity (tenant_id, entity_code, entity_name, physical_table, status, remark, creator, updater)
SELECT 1, 'content', '内容', 'oa_content', 'ENABLED', 'ADR-046 seed', 'system', 'system'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM sys_metadata_entity WHERE tenant_id = 1 AND entity_code = 'content'
);

INSERT INTO sys_metadata_field (tenant_id, entity_id, field_code, field_name, column_name, data_type, query_condition_type, dict_type, sort, creator, updater)
SELECT 1, e.id, 'account_id', '账号', 'account_id', 'BIGINT', 'ACCOUNT_SELECT', NULL, 10, 'system', 'system'
FROM sys_metadata_entity e WHERE e.tenant_id = 1 AND e.entity_code = 'content'
  AND NOT EXISTS (SELECT 1 FROM sys_metadata_field f WHERE f.tenant_id = 1 AND f.entity_id = e.id AND f.field_code = 'account_id')
UNION ALL
SELECT 1, e.id, 'platform_type', '平台', 'platform_type', 'VARCHAR', 'PLATFORM_SELECT', 'dict_platform_type', 20, 'system', 'system'
FROM sys_metadata_entity e WHERE e.tenant_id = 1 AND e.entity_code = 'content'
  AND NOT EXISTS (SELECT 1 FROM sys_metadata_field f WHERE f.tenant_id = 1 AND f.entity_id = e.id AND f.field_code = 'platform_type')
UNION ALL
SELECT 1, e.id, 'publish_time', '发布时间', 'publish_time', 'DATETIME', 'DATE_RANGE', NULL, 30, 'system', 'system'
FROM sys_metadata_entity e WHERE e.tenant_id = 1 AND e.entity_code = 'content'
  AND NOT EXISTS (SELECT 1 FROM sys_metadata_field f WHERE f.tenant_id = 1 AND f.entity_id = e.id AND f.field_code = 'publish_time')
UNION ALL
SELECT 1, e.id, 'title', '标题', 'title', 'VARCHAR', 'TEXT', NULL, 40, 'system', 'system'
FROM sys_metadata_entity e WHERE e.tenant_id = 1 AND e.entity_code = 'content'
  AND NOT EXISTS (SELECT 1 FROM sys_metadata_field f WHERE f.tenant_id = 1 AND f.entity_id = e.id AND f.field_code = 'title');
