-- M10-API-S-01: Collector 双 ID 映射表 + oa_account 凭证扩展（ADR-047）

ALTER TABLE oa_account ADD COLUMN mp_token_encrypted VARCHAR(512) NULL COMMENT '公众号后台 Token AES-256';
ALTER TABLE oa_account ADD COLUMN auth_token_encrypted VARCHAR(512) NULL COMMENT '平台专用 Token AES-256';
ALTER TABLE oa_account ADD COLUMN field_mapping TEXT NULL COMMENT '账号级字段映射 JSON';
ALTER TABLE oa_account ADD COLUMN app_id VARCHAR(100) NULL COMMENT 'AppId 档案可选';
ALTER TABLE oa_account ADD COLUMN app_secret_encrypted VARCHAR(512) NULL COMMENT 'AppSecret AES-256 档案可选';

CREATE TABLE IF NOT EXISTS oa_collector_account_bind (
    id                      BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id               BIGINT       NOT NULL,
    oa_account_id           BIGINT       NOT NULL COMMENT '业务账号 oa_account.id',
    collector_account_id    VARCHAR(64)  NOT NULL COMMENT 'collector acc_xxx',
    platform_type           VARCHAR(64)  NOT NULL COMMENT '冗余平台类型',
    bind_status             VARCHAR(32)  NOT NULL DEFAULT 'PENDING' COMMENT 'dict_collector_bind_status',
    conn_status             VARCHAR(32)  NULL COMMENT 'dict_conn_status',
    last_bind_at            TIMESTAMP    NULL,
    last_health_check_at    TIMESTAMP    NULL,
    creator                 VARCHAR(64)  DEFAULT 'system',
    create_time             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater                 VARCHAR(64)  DEFAULT 'system',
    update_time             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                 SMALLINT     NOT NULL DEFAULT 0,
    UNIQUE KEY uk_oa_collector_bind_oa_account (tenant_id, oa_account_id),
    UNIQUE KEY uk_oa_collector_bind_collector_id (tenant_id, collector_account_id),
    KEY idx_oa_collector_bind_tenant (tenant_id),
    KEY idx_oa_collector_bind_platform (tenant_id, platform_type),
    KEY idx_oa_collector_bind_status (tenant_id, bind_status)
);

INSERT INTO sys_dict_type (type, name, status) VALUES
('dict_collector_bind_status', 'Collector 绑定状态', 'ENABLED')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_collector_bind_status', '已绑定', 'BOUND', 1, 'ENABLED'),
('dict_collector_bind_status', '待绑定', 'PENDING', 2, 'ENABLED'),
('dict_collector_bind_status', '绑定失败', 'FAILED', 3, 'ENABLED')
ON DUPLICATE KEY UPDATE label = VALUES(label);
