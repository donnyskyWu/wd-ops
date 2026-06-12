-- M4 S-01: 公司管理

CREATE TABLE IF NOT EXISTS oa_company (
    id                      BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id               BIGINT       NOT NULL,
    company_name            VARCHAR(100) NOT NULL,
    credit_code             VARCHAR(18)  NOT NULL,
    industry                VARCHAR(40)  NULL,
    address                 VARCHAR(200) NULL,
    legal_name              VARCHAR(64)  NULL,
    legal_id_card_encrypted VARCHAR(128) NULL,
    mp_capacity_standard    INT          NOT NULL DEFAULT 0,
    mp_registered_count     INT          NOT NULL DEFAULT 0,
    status                  VARCHAR(32)  NOT NULL DEFAULT 'ENABLED',
    creator                 VARCHAR(64)  DEFAULT 'system',
    create_time             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater                 VARCHAR(64)  DEFAULT 'system',
    update_time             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                 SMALLINT     NOT NULL DEFAULT 0,
    UNIQUE KEY uk_oa_company_credit (tenant_id, credit_code),
    KEY idx_oa_company_tenant (tenant_id)
);

CREATE TABLE IF NOT EXISTS oa_company_expansion (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    company_id      BIGINT       NOT NULL,
    from_capacity   INT          NOT NULL,
    to_capacity     INT          NOT NULL,
    reason          VARCHAR(200) NOT NULL,
    operator_name   VARCHAR(64)  NULL,
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_oa_company_expansion_company (tenant_id, company_id)
);

INSERT INTO sys_dict_type (type, name, status) VALUES
('dict_company_status', '公司状态', 'ENABLED');

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_company_status', '启用', 'ENABLED', 1, 'ENABLED'),
('dict_company_status', '停用', 'DISABLED', 2, 'ENABLED');
