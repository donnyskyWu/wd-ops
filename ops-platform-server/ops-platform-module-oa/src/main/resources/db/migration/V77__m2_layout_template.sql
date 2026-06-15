-- S-14: WeChat layout template library (FR-M2-005, ADR-019)

CREATE TABLE IF NOT EXISTS oa_wechat_layout_template (
    id                BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id         BIGINT       NOT NULL,
    template_name     VARCHAR(100) NOT NULL,
    description       VARCHAR(500) NULL,
    content_type      VARCHAR(20)  NOT NULL DEFAULT 'ARTICLE',
    document_type     VARCHAR(50)  NULL COMMENT 'dict_document_type, NULL = generic',
    layout_json       JSON         NOT NULL,
    layout_html       LONGTEXT     NULL,
    thumbnail_url     VARCHAR(512) NULL,
    source_type       VARCHAR(30)  NOT NULL DEFAULT 'MANUAL',
    source_url        VARCHAR(1024) NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    creator_user_id   BIGINT       NOT NULL,
    creator           VARCHAR(64)  DEFAULT 'system',
    create_time       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater           VARCHAR(64)  DEFAULT 'system',
    update_time       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted           SMALLINT     NOT NULL DEFAULT 0,
    KEY idx_oa_layout_tpl_tenant (tenant_id),
    KEY idx_oa_layout_tpl_status (tenant_id, status)
);

CREATE TABLE IF NOT EXISTS oa_layout_import_job (
    id                  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id           BIGINT       NOT NULL,
    source_type         VARCHAR(30)  NOT NULL,
    source_url          VARCHAR(1024) NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    preview_layout_json JSON         NULL,
    suggested_name      VARCHAR(100) NULL,
    error_message       VARCHAR(500) NULL,
    creator_user_id     BIGINT       NULL,
    creator             VARCHAR(64)  DEFAULT 'system',
    create_time         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater             VARCHAR(64)  DEFAULT 'system',
    update_time         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             SMALLINT     NOT NULL DEFAULT 0,
    KEY idx_oa_layout_job_tenant (tenant_id)
);

ALTER TABLE oa_production_content ADD COLUMN body_format VARCHAR(20) NOT NULL DEFAULT 'PLAIN';
ALTER TABLE oa_production_content ADD COLUMN layout_json JSON NULL;
ALTER TABLE oa_production_content ADD COLUMN layout_html LONGTEXT NULL;
ALTER TABLE oa_production_content ADD COLUMN layout_template_id BIGINT NULL;

INSERT INTO sys_dict_type (type, name, status) VALUES
('dict_content_body_format', 'Content body format', 'ENABLED'),
('dict_layout_template_status', 'Layout template status', 'ENABLED'),
('dict_layout_template_source', 'Layout template source', 'ENABLED')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_content_body_format', 'Plain text', 'PLAIN', 1, 'ENABLED'),
('dict_content_body_format', 'Layout', 'LAYOUT', 2, 'ENABLED'),
('dict_layout_template_status', '草稿', 'DRAFT', 1, 'ENABLED'),
('dict_layout_template_status', '已启用', 'ENABLED', 2, 'ENABLED'),
('dict_layout_template_status', '已停用', 'DISABLED', 3, 'ENABLED'),
('dict_layout_template_source', '手动创建', 'MANUAL', 1, 'ENABLED'),
('dict_layout_template_source', '链接导入', 'URL', 2, 'ENABLED'),
('dict_layout_template_source', 'Word 导入', 'DOCX', 3, 'ENABLED'),
('dict_layout_template_source', '粘贴导入', 'PASTE', 4, 'ENABLED')
ON DUPLICATE KEY UPDATE label = VALUES(label);

INSERT INTO sys_permission (id, code, name, module, creator, updater) VALUES
(43, 'oa:layout-template:list', 'Layout template list', 'M2', 's14', 's14'),
(44, 'oa:layout-template:create', 'Layout template create', 'M2', 's14', 's14'),
(45, 'oa:layout-template:update', 'Layout template update', 'M2', 's14', 's14'),
(46, 'oa:layout-template:delete', 'Layout template delete', 'M2', 's14', 's14'),
(47, 'oa:layout-template:import', 'Layout template import', 'M2', 's14', 's14')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO sys_role_permission (role_id, permission_id) VALUES
(1, 43), (1, 44), (1, 45), (1, 46), (1, 47);
