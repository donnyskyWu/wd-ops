-- M4 S-08: 个人账号（个微 / 企微）

CREATE TABLE IF NOT EXISTS oa_personal_wechat_account (
    id                      BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id               BIGINT       NOT NULL,
    account_name            VARCHAR(100) NOT NULL,
    wechat_id               VARCHAR(64)  NOT NULL,
    phone_id                BIGINT       NULL,
    api_url_encrypted       VARCHAR(512) NULL,
    app_id_encrypted        VARCHAR(256) NULL,
    app_secret_encrypted    VARCHAR(512) NULL,
    token_encrypted         VARCHAR(512) NULL,
    status                  VARCHAR(32)  NOT NULL DEFAULT 'ENABLED',
    creator                 VARCHAR(64)  DEFAULT 'system',
    create_time             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater                 VARCHAR(64)  DEFAULT 'system',
    update_time             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                 SMALLINT     NOT NULL DEFAULT 0,
    UNIQUE KEY uk_oa_pwa_wechat (tenant_id, wechat_id),
    KEY idx_oa_pwa_tenant (tenant_id),
    KEY idx_oa_pwa_phone (tenant_id, phone_id)
);

CREATE TABLE IF NOT EXISTS oa_wework_account (
    id                      BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id               BIGINT       NOT NULL,
    account_name            VARCHAR(100) NOT NULL,
    corp_id                 VARCHAR(64)  NOT NULL,
    agent_id                VARCHAR(64)  NOT NULL,
    secret_encrypted        VARCHAR(512) NOT NULL,
    status                  VARCHAR(32)  NOT NULL DEFAULT 'ENABLED',
    creator                 VARCHAR(64)  DEFAULT 'system',
    create_time             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater                 VARCHAR(64)  DEFAULT 'system',
    update_time             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                 SMALLINT     NOT NULL DEFAULT 0,
    UNIQUE KEY uk_oa_wework_corp_agent (tenant_id, corp_id, agent_id),
    KEY idx_oa_wework_tenant (tenant_id)
);
