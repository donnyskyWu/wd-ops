-- M4 S-08: 个人账号（个微 / 企微）

CREATE TABLE IF NOT EXISTS oa_personal_wechat_account (
    id                      BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '个人微信账号ID',
    tenant_id               BIGINT       NOT NULL COMMENT '租户ID',
    account_name            VARCHAR(100) NOT NULL COMMENT '账号名称',
    wechat_id               VARCHAR(64)  NOT NULL COMMENT '微信号',
    phone_id                BIGINT       NULL COMMENT '绑定手机ID',
    api_url_encrypted       VARCHAR(512) NULL COMMENT 'API地址（加密）',
    app_id_encrypted        VARCHAR(256) NULL COMMENT 'AppID（加密）',
    app_secret_encrypted    VARCHAR(512) NULL COMMENT 'AppSecret（加密）',
    token_encrypted         VARCHAR(512) NULL COMMENT 'Token（加密）',
    status                  VARCHAR(32)  NOT NULL DEFAULT 'ENABLED' COMMENT '状态: ENABLED-启用, DISABLED-停用',
    creator                 VARCHAR(64)  DEFAULT 'system' COMMENT '创建人',
    create_time             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater                 VARCHAR(64)  DEFAULT 'system' COMMENT '更新人',
    update_time             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted                 SMALLINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除标志: 0-未删除, 1-已删除',
    UNIQUE KEY uk_oa_pwa_wechat (tenant_id, wechat_id),
    KEY idx_oa_pwa_tenant (tenant_id),
    KEY idx_oa_pwa_phone (tenant_id, phone_id)
) COMMENT='个人微信账号表';

CREATE TABLE IF NOT EXISTS oa_wework_account (
    id                      BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '企业微信账号ID',
    tenant_id               BIGINT       NOT NULL COMMENT '租户ID',
    account_name            VARCHAR(100) NOT NULL COMMENT '账号名称',
    corp_id                 VARCHAR(64)  NOT NULL COMMENT '企业ID',
    agent_id                VARCHAR(64)  NOT NULL COMMENT '应用ID',
    secret_encrypted        VARCHAR(512) NOT NULL COMMENT 'Secret（加密）',
    status                  VARCHAR(32)  NOT NULL DEFAULT 'ENABLED' COMMENT '状态: ENABLED-启用, DISABLED-停用',
    creator                 VARCHAR(64)  DEFAULT 'system' COMMENT '创建人',
    create_time             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater                 VARCHAR(64)  DEFAULT 'system' COMMENT '更新人',
    update_time             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted                 SMALLINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除标志: 0-未删除, 1-已删除',
    UNIQUE KEY uk_oa_wework_corp_agent (tenant_id, corp_id, agent_id),
    KEY idx_oa_wework_tenant (tenant_id)
) COMMENT='企业微信账号表';
