-- V131: Revise oa_author_ext PK + create oa_account_ext + author_id semantics (ADR-050/051)
-- Idempotent: safe if V130 applied and/or partial re-run

TRUNCATE TABLE oa_author_ext;

SET @has_ext_id := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_author_ext' AND COLUMN_NAME = 'id'
);
SET @sql_drop_id := IF(@has_ext_id > 0,
    'ALTER TABLE oa_author_ext DROP PRIMARY KEY, DROP COLUMN id',
    'SELECT 1');
PREPARE stmt FROM @sql_drop_id;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_primary_account := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_author_ext' AND COLUMN_NAME = 'primary_account_id'
);
SET @sql_rename_pa := IF(@has_primary_account > 0,
    'ALTER TABLE oa_author_ext CHANGE COLUMN primary_account_id primary_mp_account_id BIGINT NULL COMMENT ''primary wechat mp_account.id''',
    'SELECT 1');
PREPARE stmt FROM @sql_rename_pa;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE oa_author_ext
    MODIFY COLUMN author_user_id BIGINT NOT NULL COMMENT 'PK -> shenyu-member.author_user.id';

ALTER TABLE oa_author_ext
    MODIFY COLUMN sync_status VARCHAR(32) NOT NULL DEFAULT 'SYNCED' COMMENT 'SYNCED/ERROR';

SET @has_ext_pk := (
    SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_author_ext' AND CONSTRAINT_TYPE = 'PRIMARY KEY'
);
SET @sql_add_pk := IF(@has_ext_pk = 0,
    'ALTER TABLE oa_author_ext ADD PRIMARY KEY (author_user_id)',
    'SELECT 1');
PREPARE stmt FROM @sql_add_pk;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS oa_account_ext (
    id                  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id           BIGINT       NOT NULL COMMENT 'tenant_id',
    mp_account_id       BIGINT       NOT NULL COMMENT '-> shenyu-mp.mp_account.id',
    platform_type       VARCHAR(32)  NOT NULL DEFAULT 'WECHAT_OFFICIAL',
    company_id          BIGINT       NULL,
    realname_id         BIGINT       NULL,
    intermediary_id     BIGINT       NULL,
    ip_group_id         BIGINT       NULL COMMENT '-> oa_ip_group.id',
    phone_id            BIGINT       NULL,
    sim_card_id         BIGINT       NULL,
    cookie_encrypted    VARCHAR(512) NULL,
    trademark_name      VARCHAR(128) NULL,
    qualification_type  VARCHAR(32)  NULL,
    usage_status        VARCHAR(32)  NULL,
    admin_user_id       BIGINT       NULL COMMENT '-> system_users.id',
    sync_status         VARCHAR(32)  NOT NULL DEFAULT 'SYNCED',
    sync_error          VARCHAR(512) NULL,
    creator             VARCHAR(64)  DEFAULT 'system',
    create_time         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater             VARCHAR(64)  DEFAULT 'system',
    update_time         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             SMALLINT     NOT NULL DEFAULT 0,
    UNIQUE KEY uk_ext_mp (tenant_id, mp_account_id),
    KEY idx_ext_ip_group (tenant_id, ip_group_id)
) COMMENT='WeChat official account Ops extension (mp_account SSOT)';

ALTER TABLE oa_content
    MODIFY COLUMN author_id BIGINT NULL COMMENT '-> shenyu-member.author_user.id (ADR-050/051)';

ALTER TABLE oa_production_content
    MODIFY COLUMN author_id BIGINT NULL COMMENT '-> shenyu-member.author_user.id (ADR-050/051)';

ALTER TABLE oa_task
    MODIFY COLUMN author_id BIGINT NULL COMMENT '-> shenyu-member.author_user.id (ADR-050/051)';

ALTER TABLE oa_order_attribution
    MODIFY COLUMN author_id BIGINT NULL COMMENT '-> shenyu-member.author_user.id (ADR-050/051)';
