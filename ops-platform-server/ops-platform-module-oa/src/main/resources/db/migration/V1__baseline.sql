-- GATE-S0 baseline (MySQL 8 + H2 compatible)

CREATE TABLE IF NOT EXISTS sys_tenant (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(64)  NOT NULL,
    status      VARCHAR(32)  NOT NULL DEFAULT 'ENABLED',
    creator     VARCHAR(64)  DEFAULT 'system',
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater     VARCHAR(64)  DEFAULT 'system',
    update_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     SMALLINT     NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id   BIGINT       NOT NULL,
    username    VARCHAR(64)  NOT NULL,
    nickname    VARCHAR(64)  NOT NULL,
    status      VARCHAR(32)  NOT NULL DEFAULT 'ENABLED',
    creator     VARCHAR(64)  DEFAULT 'system',
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater     VARCHAR(64)  DEFAULT 'system',
    update_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     SMALLINT     NOT NULL DEFAULT 0,
    KEY idx_sys_user_tenant (tenant_id)
);

CREATE TABLE IF NOT EXISTS sys_user_token (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    token       VARCHAR(128) NOT NULL,
    status      VARCHAR(32)  NOT NULL DEFAULT 'ENABLED',
    creator     VARCHAR(64)  DEFAULT 'system',
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater     VARCHAR(64)  DEFAULT 'system',
    update_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     SMALLINT     NOT NULL DEFAULT 0,
    UNIQUE KEY uk_sys_user_token (token),
    KEY idx_sys_user_token_user (user_id)
);

CREATE TABLE IF NOT EXISTS sys_role (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id   BIGINT       NOT NULL,
    code        VARCHAR(64)  NOT NULL,
    name        VARCHAR(64)  NOT NULL,
    creator     VARCHAR(64)  DEFAULT 'system',
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater     VARCHAR(64)  DEFAULT 'system',
    update_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     SMALLINT     NOT NULL DEFAULT 0,
    KEY idx_sys_role_tenant (tenant_id)
);

CREATE TABLE IF NOT EXISTS sys_user_role (
    id          BIGINT   NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT   NOT NULL,
    role_id     BIGINT   NOT NULL,
    creator     VARCHAR(64) DEFAULT 'system',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sys_user_role (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS sys_dict_type (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    type        VARCHAR(64)  NOT NULL,
    name        VARCHAR(128) NOT NULL,
    status      VARCHAR(32)  NOT NULL DEFAULT 'ENABLED',
    creator     VARCHAR(64)  DEFAULT 'system',
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater     VARCHAR(64)  DEFAULT 'system',
    update_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     SMALLINT     NOT NULL DEFAULT 0,
    UNIQUE KEY uk_sys_dict_type (type)
);

CREATE TABLE IF NOT EXISTS sys_dict_data (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    dict_type   VARCHAR(64)  NOT NULL,
    label       VARCHAR(128) NOT NULL,
    dict_value  VARCHAR(128) NOT NULL,
    sort        INT          NOT NULL DEFAULT 0,
    status      VARCHAR(32)  NOT NULL DEFAULT 'ENABLED',
    creator     VARCHAR(64)  DEFAULT 'system',
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater     VARCHAR(64)  DEFAULT 'system',
    update_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     SMALLINT     NOT NULL DEFAULT 0,
    UNIQUE KEY uk_sys_dict_data (dict_type, dict_value),
    KEY idx_sys_dict_data_type (dict_type)
);

CREATE TABLE IF NOT EXISTS sys_audit_log (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id   BIGINT       NOT NULL,
    user_id     BIGINT       NULL,
    username    VARCHAR(64)  NULL,
    module      VARCHAR(64)  NOT NULL,
    action      VARCHAR(64)  NOT NULL,
    biz_id      VARCHAR(64)  NULL,
    content     TEXT         NULL,
    creator     VARCHAR(64)  DEFAULT 'system',
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_sys_audit_log_tenant (tenant_id)
);

CREATE TABLE IF NOT EXISTS oa_demo_item (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id   BIGINT       NOT NULL,
    name        VARCHAR(128) NOT NULL,
    creator     VARCHAR(64)  DEFAULT 'system',
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater     VARCHAR(64)  DEFAULT 'system',
    update_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     SMALLINT     NOT NULL DEFAULT 0,
    KEY idx_oa_demo_item_tenant (tenant_id)
);
