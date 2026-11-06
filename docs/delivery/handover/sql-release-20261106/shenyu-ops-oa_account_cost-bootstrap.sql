-- v20261106: 本地/生产 shenyu-ops 缺 M5 基表 oa_account / oa_account_cost 时总 ROI 查询报 DataAccessException。
-- 幂等：CREATE IF NOT EXISTS + 条件 ALTER（与 sql-release-20261105 Section 03 对齐）。
-- 可选：新版 shenyu-ops-roi-minimal.sql 已含 01B+Section 03；仅「已跑旧 minimal、未含 01A/01B」时需单独执行本文件。
USE `shenyu-ops`;

CREATE TABLE IF NOT EXISTS oa_account (
    id                  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '账号ID',
    tenant_id           BIGINT       NOT NULL COMMENT '租户ID',
    platform_type       VARCHAR(32)  NOT NULL COMMENT 'DOUYIN/KUAISHOU/WECHAT_OFFICIAL 等',
    account_name        VARCHAR(128) NOT NULL COMMENT '账号名称',
    external_account_id VARCHAR(64)  NULL COMMENT '外部平台账号ID',
    phone_id            BIGINT       NULL,
    phone_number_hash   VARCHAR(64)  NULL,
    sim_card_id         BIGINT       NULL,
    status              VARCHAR(32)  NOT NULL DEFAULT 'NORMAL',
    linked_at           TIMESTAMP    NULL,
    creator             VARCHAR(64)  DEFAULT 'system',
    create_time         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater             VARCHAR(64)  DEFAULT 'system',
    update_time         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             SMALLINT     NOT NULL DEFAULT 0,
    company_id          BIGINT       NULL,
    realname_id         BIGINT       NULL,
    intermediary_id     BIGINT       NULL,
    account_type        VARCHAR(32)  NULL,
    ip_group_id         BIGINT       NULL,
    operator_user_id    BIGINT       NULL COMMENT '运营人 Football system_users.id',
    holder_user_id      BIGINT       NULL COMMENT '持有人 system_users.id',
    short_video_status  VARCHAR(128) NULL,
    live_status         VARCHAR(128) NULL,
    account_ban_json    JSON         NULL,
    video_ban_json      JSON         NULL,
    live_ban_json       JSON         NULL,
    author_user_id      BIGINT       NULL COMMENT '所属作者 author_user.id',
    password_encrypted  VARCHAR(512) NULL,
    app_id              VARCHAR(100) NULL,
    qualification_type  VARCHAR(32) NULL,
    cert_expiry_time    TIMESTAMP    NULL,
    admin_user_id       BIGINT       NULL,
    UNIQUE KEY uk_oa_account_platform_ext (tenant_id, platform_type, external_account_id),
    KEY idx_oa_account_tenant (tenant_id),
    KEY idx_oa_account_platform (tenant_id, platform_type),
    KEY idx_oa_account_operator_user (tenant_id, operator_user_id),
    KEY idx_oa_account_holder (tenant_id, holder_user_id),
    KEY idx_oa_account_tenant_author (tenant_id, author_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='跨平台账号聚合表';

CREATE TABLE IF NOT EXISTS oa_account_cost (
    id              BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT        NOT NULL,
    account_id      BIGINT        NOT NULL,
    cost_type       VARCHAR(32)   NOT NULL,
    amount          DECIMAL(16,2) NOT NULL,
    pay_method      VARCHAR(32)   NOT NULL,
    pay_date        DATE          NULL COMMENT 'Actual payment date; unknown until cashier registration',
    period          VARCHAR(32)   NOT NULL DEFAULT 'ONCE',
    remark          VARCHAR(500)  NULL,
    attachment_id   BIGINT        NULL,
    creator         VARCHAR(64)   DEFAULT 'system',
    create_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         VARCHAR(64)   DEFAULT 'system',
    update_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         SMALLINT      NOT NULL DEFAULT 0,
    KEY idx_oa_account_cost_tenant (tenant_id),
    KEY idx_oa_account_cost_account (tenant_id, account_id),
    KEY idx_oa_account_cost_date (tenant_id, pay_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='账号成本表';

SET @needs_cost_date := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_account_cost' AND COLUMN_NAME = 'cost_date'
);
SET @sql_cost_date := IF(@needs_cost_date = 0,
  'ALTER TABLE oa_account_cost ADD COLUMN cost_date DATE NULL COMMENT ''Recognized expense date; legacy records fall back to pay_date''',
  'SELECT ''skip: oa_account_cost.cost_date exists'' AS notice');
PREPARE stmt_cost_date FROM @sql_cost_date; EXECUTE stmt_cost_date; DEALLOCATE PREPARE stmt_cost_date;

SET @needs_source_type := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_account_cost' AND COLUMN_NAME = 'source_type'
);
SET @sql_source_type := IF(@needs_source_type = 0,
  'ALTER TABLE oa_account_cost ADD COLUMN source_type VARCHAR(16) NOT NULL DEFAULT ''MANUAL''',
  'SELECT ''skip: oa_account_cost.source_type exists'' AS notice');
PREPARE stmt_source_type FROM @sql_source_type; EXECUTE stmt_source_type; DEALLOCATE PREPARE stmt_source_type;

SET @needs_source_key := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_account_cost' AND COLUMN_NAME = 'source_key'
);
SET @sql_source_key := IF(@needs_source_key = 0,
  'ALTER TABLE oa_account_cost ADD COLUMN source_key CHAR(64) NULL',
  'SELECT ''skip: oa_account_cost.source_key exists'' AS notice');
PREPARE stmt_source_key FROM @sql_source_key; EXECUTE stmt_source_key; DEALLOCATE PREPARE stmt_source_key;

SET @needs_source_candidate := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_account_cost' AND COLUMN_NAME = 'source_candidate_id'
);
SET @sql_source_candidate := IF(@needs_source_candidate = 0,
  'ALTER TABLE oa_account_cost ADD COLUMN source_candidate_id CHAR(36) NULL',
  'SELECT ''skip: oa_account_cost.source_candidate_id exists'' AS notice');
PREPARE stmt_source_candidate FROM @sql_source_candidate; EXECUTE stmt_source_candidate; DEALLOCATE PREPARE stmt_source_candidate;

SET @needs_approval_business := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_account_cost' AND COLUMN_NAME = 'approval_business_id'
);
SET @sql_approval_business := IF(@needs_approval_business = 0,
  'ALTER TABLE oa_account_cost ADD COLUMN approval_business_id VARCHAR(128) NULL',
  'SELECT ''skip: oa_account_cost.approval_business_id exists'' AS notice');
PREPARE stmt_approval_business FROM @sql_approval_business; EXECUTE stmt_approval_business; DEALLOCATE PREPARE stmt_approval_business;

SET @needs_approval_instance := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_account_cost' AND COLUMN_NAME = 'approval_instance_id'
);
SET @sql_approval_instance := IF(@needs_approval_instance = 0,
  'ALTER TABLE oa_account_cost ADD COLUMN approval_instance_id VARCHAR(256) NULL',
  'SELECT ''skip: oa_account_cost.approval_instance_id exists'' AS notice');
PREPARE stmt_approval_instance FROM @sql_approval_instance; EXECUTE stmt_approval_instance; DEALLOCATE PREPARE stmt_approval_instance;

SET @needs_author_id := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_account_cost' AND COLUMN_NAME = 'author_id'
);
SET @sql_author_id := IF(@needs_author_id = 0,
  'ALTER TABLE oa_account_cost ADD COLUMN author_id BIGINT NULL',
  'SELECT ''skip: oa_account_cost.author_id exists'' AS notice');
PREPARE stmt_author_id FROM @sql_author_id; EXECUTE stmt_author_id; DEALLOCATE PREPARE stmt_author_id;

SET @needs_attr_ip := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_account_cost' AND COLUMN_NAME = 'attribution_ip_group_id'
);
SET @sql_attr_ip := IF(@needs_attr_ip = 0,
  'ALTER TABLE oa_account_cost ADD COLUMN attribution_ip_group_id BIGINT NULL',
  'SELECT ''skip: oa_account_cost.attribution_ip_group_id exists'' AS notice');
PREPARE stmt_attr_ip FROM @sql_attr_ip; EXECUTE stmt_attr_ip; DEALLOCATE PREPARE stmt_attr_ip;

SET @needs_payment_state := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_account_cost' AND COLUMN_NAME = 'payment_state'
);
SET @sql_payment_state := IF(@needs_payment_state = 0,
  'ALTER TABLE oa_account_cost ADD COLUMN payment_state VARCHAR(16) NOT NULL DEFAULT ''LEGACY''',
  'SELECT ''skip: oa_account_cost.payment_state exists'' AS notice');
PREPARE stmt_payment_state FROM @sql_payment_state; EXECUTE stmt_payment_state; DEALLOCATE PREPARE stmt_payment_state;

SET @needs_paid_ref := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_account_cost' AND COLUMN_NAME = 'paid_reference'
);
SET @sql_paid_ref := IF(@needs_paid_ref = 0,
  'ALTER TABLE oa_account_cost ADD COLUMN paid_reference VARCHAR(200) NULL',
  'SELECT ''skip: oa_account_cost.paid_reference exists'' AS notice');
PREPARE stmt_paid_ref FROM @sql_paid_ref; EXECUTE stmt_paid_ref; DEALLOCATE PREPARE stmt_paid_ref;

SET @needs_payment_note := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_account_cost' AND COLUMN_NAME = 'payment_note'
);
SET @sql_payment_note := IF(@needs_payment_note = 0,
  'ALTER TABLE oa_account_cost ADD COLUMN payment_note TEXT NULL',
  'SELECT ''skip: oa_account_cost.payment_note exists'' AS notice');
PREPARE stmt_payment_note FROM @sql_payment_note; EXECUTE stmt_payment_note; DEALLOCATE PREPARE stmt_payment_note;

SET @needs_pay_date_nullable := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_account_cost'
    AND COLUMN_NAME = 'pay_date' AND IS_NULLABLE = 'NO'
);
SET @sql_pay_date_nullable := IF(@needs_pay_date_nullable > 0,
  'ALTER TABLE oa_account_cost MODIFY COLUMN pay_date DATE NULL COMMENT ''Actual payment date; unknown until cashier registration''',
  'SELECT ''skip: oa_account_cost.pay_date already nullable'' AS notice');
PREPARE stmt_pay_date_nullable FROM @sql_pay_date_nullable; EXECUTE stmt_pay_date_nullable; DEALLOCATE PREPARE stmt_pay_date_nullable;

SET @needs_uq_source := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_account_cost' AND INDEX_NAME = 'uq_oa_cost_source_key'
);
SET @sql_uq_source := IF(@needs_uq_source = 0,
  'ALTER TABLE oa_account_cost ADD UNIQUE KEY uq_oa_cost_source_key(tenant_id,source_key)',
  'SELECT ''skip: uq_oa_cost_source_key exists'' AS notice');
PREPARE stmt_uq_source FROM @sql_uq_source; EXECUTE stmt_uq_source; DEALLOCATE PREPARE stmt_uq_source;

SET @needs_ix_cost_date := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_account_cost' AND INDEX_NAME = 'ix_oa_cost_date'
);
SET @sql_ix_cost_date := IF(@needs_ix_cost_date = 0,
  'ALTER TABLE oa_account_cost ADD KEY ix_oa_cost_date(tenant_id,cost_date,account_id)',
  'SELECT ''skip: ix_oa_cost_date exists'' AS notice');
PREPARE stmt_ix_cost_date FROM @sql_ix_cost_date; EXECUTE stmt_ix_cost_date; DEALLOCATE PREPARE stmt_ix_cost_date;
