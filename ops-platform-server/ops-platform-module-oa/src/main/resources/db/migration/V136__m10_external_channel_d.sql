-- M10 Channel-D · GATE-EXT-P0（ADR-052 · 快手 user-videos E2E）

-- ========== 字典 ==========
-- sys_dict_data 在部分环境 id 无 AUTO_INCREMENT，须显式分配 id (V128/V134 同)
SET @next_data_id = (SELECT COALESCE(MAX(id), 0) FROM sys_dict_data);

INSERT INTO sys_dict_data (id, dict_type, label, dict_value, sort, status) VALUES
(@next_data_id + 1, 'dict_collect_method', '外部竞品', 'EXTERNAL', 4, 'ENABLED'),
(@next_data_id + 2, 'dict_collect_source', '统一采集-外部竞品', 'UNIFY_COLLECTOR_EXTERNAL', 10, 'ENABLED'),
(@next_data_id + 3, 'dict_collect_data_type', '快手竞品作品列表', 'EXT_KUAISHOU_USER_VIDEOS', 20, 'ENABLED')
ON DUPLICATE KEY UPDATE label = VALUES(label), sort = VALUES(sort), status = VALUES(status);

-- ========== 竞品账号快照 ==========
CREATE TABLE IF NOT EXISTS oa_external_account (
    id                  BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id           BIGINT        NOT NULL,
    collect_config_id   BIGINT        NOT NULL COMMENT 'FK oa_collect_config.id (scope=EXTERNAL)',
    platform_type       VARCHAR(32)   NOT NULL COMMENT 'dict_third_platform',
    external_user_id    VARCHAR(128)  NOT NULL COMMENT '平台 user_id / sec_uid 等',
    display_name        VARCHAR(128)  NULL,
    follower_count      BIGINT        NULL DEFAULT 0,
    work_count          INT           NULL DEFAULT 0,
    avatar_url          VARCHAR(500)  NULL,
    last_synced_at      TIMESTAMP     NULL,
    creator             VARCHAR(64)   DEFAULT 'system',
    create_time         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater             VARCHAR(64)   DEFAULT 'system',
    update_time         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             SMALLINT      NOT NULL DEFAULT 0,
    KEY idx_oa_external_account_tenant (tenant_id),
    KEY idx_oa_external_account_config (tenant_id, collect_config_id),
    UNIQUE KEY uk_oa_external_account_config (tenant_id, collect_config_id, deleted),
    UNIQUE KEY uk_oa_external_account_user (tenant_id, platform_type, external_user_id, deleted)
);

-- ========== 竞品粉丝日聚合 ==========
CREATE TABLE IF NOT EXISTS oa_external_follower_daily (
    id                  BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id           BIGINT        NOT NULL,
    external_account_id BIGINT        NOT NULL COMMENT 'FK oa_external_account.id',
    stat_date           DATE          NOT NULL,
    follower_count      BIGINT        NOT NULL DEFAULT 0,
    creator             VARCHAR(64)   DEFAULT 'system',
    create_time         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater             VARCHAR(64)   DEFAULT 'system',
    update_time         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             SMALLINT      NOT NULL DEFAULT 0,
    KEY idx_oa_external_follower_daily_tenant (tenant_id),
    UNIQUE KEY uk_oa_external_follower_daily (tenant_id, external_account_id, stat_date, deleted)
);

-- ========== 外部作品表增量 ==========
ALTER TABLE oa_external_work ADD COLUMN platform_work_id VARCHAR(128) NULL COMMENT '平台作品 ID（幂等 UK）';
ALTER TABLE oa_external_work ADD COLUMN collect_config_id BIGINT NULL COMMENT 'FK oa_collect_config.id';
ALTER TABLE oa_external_work ADD COLUMN comment_count INT NOT NULL DEFAULT 0 COMMENT '评论数';

ALTER TABLE oa_external_work
    ADD UNIQUE KEY uk_oa_external_work_platform_work (tenant_id, platform_type, platform_work_id);

ALTER TABLE oa_external_work
    MODIFY COLUMN account_id BIGINT NULL COMMENT 'FK oa_external_account.id（竞品账号快照）';

-- ========== 采集任务 Channel-D 字段 ==========
ALTER TABLE oa_collect_task ADD COLUMN collect_config_id BIGINT NULL COMMENT 'Channel-D FK oa_collect_config.id';
ALTER TABLE oa_collect_task ADD COLUMN credential_profile VARCHAR(64) NULL DEFAULT 'default' COMMENT '租户凭账号 profile';

ALTER TABLE oa_collect_task
    MODIFY COLUMN account_id BIGINT NULL COMMENT 'Channel-A 自有账号 oa_account.id；Channel-D 为 NULL';

-- ========== 租户级采集凭账号 ==========
CREATE TABLE IF NOT EXISTS oa_tenant_collector_credential (
    id                      BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id               BIGINT        NOT NULL,
    platform                VARCHAR(64)   NOT NULL COMMENT 'dict_third_platform',
    credential_profile      VARCHAR(64)   NOT NULL DEFAULT 'default',
    profile_name            VARCHAR(128)  NULL,
    cookie_encrypted        TEXT          NULL COMMENT 'AES-256',
    auth_token_encrypted    VARCHAR(512)  NULL COMMENT 'AES-256',
    expire_at               TIMESTAMP     NULL,
    conn_status             VARCHAR(20)   NOT NULL DEFAULT 'DISCONNECTED',
    status                  VARCHAR(32)   NOT NULL DEFAULT 'ENABLED',
    last_verified_at        TIMESTAMP     NULL,
    remark                  VARCHAR(512)  NULL,
    creator                 VARCHAR(64)   DEFAULT 'system',
    create_time             TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater                 VARCHAR(64)   DEFAULT 'system',
    update_time             TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                 SMALLINT      NOT NULL DEFAULT 0,
    KEY idx_oa_tenant_collector_cred_tenant (tenant_id),
    UNIQUE KEY uk_oa_tenant_collector_cred (tenant_id, platform, credential_profile, deleted)
);
