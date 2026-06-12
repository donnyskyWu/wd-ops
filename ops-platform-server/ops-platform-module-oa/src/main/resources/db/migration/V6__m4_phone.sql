-- M4 S-04: 手机管理

CREATE TABLE IF NOT EXISTS oa_phone (
    id                      BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id               BIGINT       NOT NULL,
    realname_id             BIGINT       NULL,
    phone_number_encrypted  VARCHAR(128) NOT NULL,
    phone_number_hash       VARCHAR(64)  NOT NULL,
    phone_code              VARCHAR(32)  NULL,
    phone_model             VARCHAR(100) NULL,
    keeper_id               BIGINT       NULL,
    wechat_bound            VARCHAR(64)  NULL,
    status                  VARCHAR(32)  NOT NULL DEFAULT 'ENABLED',
    account_bound_count     INT          NOT NULL DEFAULT 0,
    creator                 VARCHAR(64)  DEFAULT 'system',
    create_time             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater                 VARCHAR(64)  DEFAULT 'system',
    update_time             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                 SMALLINT     NOT NULL DEFAULT 0,
    UNIQUE KEY uk_oa_phone_number (tenant_id, phone_number_hash),
    KEY idx_oa_phone_tenant (tenant_id),
    KEY idx_oa_phone_realname (tenant_id, realname_id)
);

INSERT INTO sys_dict_type (type, name, status) VALUES
('dict_phone_status', '手机状态', 'ENABLED');

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_phone_status', '在用', 'ENABLED', 1, 'ENABLED'),
('dict_phone_status', '停用', 'DISABLED', 2, 'ENABLED');
