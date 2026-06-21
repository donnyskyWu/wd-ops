-- M10-AO-S-00: 奥创多账号子表（ADR-045 Q1）
CREATE TABLE IF NOT EXISTS oa_aocreate_account (
    id                   BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id            BIGINT       NOT NULL,
    aocreate_api_id      BIGINT       NOT NULL,
    account_name         VARCHAR(100) NOT NULL,
    aochuang_account_id  VARCHAR(64)  NOT NULL,
    status               VARCHAR(32)  NOT NULL DEFAULT 'ENABLED',
    last_device_sync_at  TIMESTAMP    NULL,
    conn_status          VARCHAR(32)  NULL,
    creator              VARCHAR(64)  DEFAULT 'system',
    create_time          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater              VARCHAR(64)  DEFAULT 'system',
    update_time          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted              SMALLINT     NOT NULL DEFAULT 0,
    UNIQUE KEY uk_oa_aocreate_account_tenant_ao_id (tenant_id, aochuang_account_id),
    KEY idx_oa_aocreate_account_api (tenant_id, aocreate_api_id)
);

-- 奥创连接诊断状态（ADR-045 conn_status）
INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_conn_status', '连接正常', 'OK', 3, 'ENABLED'),
('dict_conn_status', 'Token失效', 'TOKEN_FAIL', 4, 'ENABLED'),
('dict_conn_status', '权限不足', 'PERMISSION_DENIED', 5, 'ENABLED')
ON DUPLICATE KEY UPDATE label = VALUES(label);
