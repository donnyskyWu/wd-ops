-- M4 S-05: 手机卡 + 跨平台账号聚合（最小 oa_account 表供 linked-accounts 查询）

CREATE TABLE IF NOT EXISTS oa_sim_card (
    id                      BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id               BIGINT       NOT NULL,
    phone_id                BIGINT       NULL,
    phone_number_encrypted  VARCHAR(128) NOT NULL,
    phone_number_hash       VARCHAR(64)  NOT NULL,
    is_primary              VARCHAR(8)   NOT NULL DEFAULT 'YES',
    operator                VARCHAR(32)  NOT NULL,
    assigned_user_id        BIGINT       NOT NULL,
    iccid_encrypted         VARCHAR(128) NULL,
    iccid_hash              VARCHAR(64)  NULL,
    package_name            VARCHAR(100) NULL,
    status                  VARCHAR(32)  NOT NULL DEFAULT 'ENABLED',
    account_bound_count     INT          NOT NULL DEFAULT 0,
    creator                 VARCHAR(64)  DEFAULT 'system',
    create_time             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater                 VARCHAR(64)  DEFAULT 'system',
    update_time             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                 SMALLINT     NOT NULL DEFAULT 0,
    UNIQUE KEY uk_oa_sim_phone (tenant_id, phone_number_hash),
    KEY idx_oa_sim_tenant (tenant_id),
    KEY idx_oa_sim_phone_id (tenant_id, phone_id),
    KEY idx_oa_sim_operator (tenant_id, operator)
);

CREATE TABLE IF NOT EXISTS oa_account (
    id                  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id           BIGINT       NOT NULL,
    platform_type       VARCHAR(32)  NOT NULL,
    account_name        VARCHAR(128) NOT NULL,
    external_account_id VARCHAR(64)  NULL,
    phone_id            BIGINT       NULL,
    phone_number_hash   VARCHAR(64)  NULL,
    sim_card_id         BIGINT       NULL,
    status              VARCHAR(32)  NOT NULL DEFAULT 'NORMAL',
    linked_at           TIMESTAMP    NULL,
    creator             VARCHAR(64)  DEFAULT 'system',
    create_time         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater             VARCHAR(64)  DEFAULT 'system',
    update_time         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             SMALLINT     NOT NULL DEFAULT 0,
    KEY idx_oa_account_tenant (tenant_id),
    KEY idx_oa_account_phone (tenant_id, phone_id),
    KEY idx_oa_account_phone_hash (tenant_id, phone_number_hash),
    KEY idx_oa_account_sim (tenant_id, sim_card_id),
    KEY idx_oa_account_platform (tenant_id, platform_type)
);

INSERT INTO sys_dict_type (type, name, status) VALUES
('dict_sim_operator', 'SIM运营商', 'ENABLED'),
('dict_sim_status', '手机卡状态', 'ENABLED'),
('dict_account_status', '账号状态', 'ENABLED');

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_sim_operator', '中国移动', 'MOBILE', 1, 'ENABLED'),
('dict_sim_operator', '中国联通', 'UNICOM', 2, 'ENABLED'),
('dict_sim_operator', '中国电信', 'TELECOM', 3, 'ENABLED'),
('dict_sim_status', '在用', 'ENABLED', 1, 'ENABLED'),
('dict_sim_status', '停用', 'DISABLED', 2, 'ENABLED'),
('dict_account_status', '正常', 'NORMAL', 1, 'ENABLED'),
('dict_account_status', '停用', 'DISABLED', 2, 'ENABLED'),
('dict_platform_type', '企业微信', 'WEWORK', 3, 'ENABLED');
