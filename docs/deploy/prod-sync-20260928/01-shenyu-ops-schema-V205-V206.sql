-- =============================================================================
-- Production incremental deploy: shenyu-ops database
--
-- Purpose:
--   V205 — M4 持有人 holder_user_id + M2 任务 publish_account_id（ADR-056 / FR-M2-010）
--   V206 — M4 抖音/快手双状态 + 运营人 operator_user_id（ADR-078）
--
-- Flyway SSOT:
--   football-module-ops-server/src/main/resources/db/migration/
--     V205__m4_holder_m2_publish_account.sql
--     V206__oa_account_short_video_live_operator.sql
--
-- Pre-requisite:
--   Production shenyu-ops baseline at V204（或已通过 prod-incremental-shenyu-ops-V197-V204.sql）。
--
-- Date: 2026-09-28
--
-- Execution:
--   mysql -h<host> -u<user> -p --default-character-set=utf8mb4 shenyu-ops < 01-shenyu-ops-schema-V205-V206.sql
--
-- Idempotency:
--   列 / 索引均经 information_schema 检测后再动态 DDL；可重复执行。
--
-- Verify after run:
--   SELECT COLUMN_NAME FROM information_schema.COLUMNS
--     WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='oa_account'
--       AND COLUMN_NAME IN ('holder_user_id','short_video_status','live_status','operator_user_id');
--   SELECT COLUMN_NAME FROM information_schema.COLUMNS
--     WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='oa_work_task_assignment'
--       AND COLUMN_NAME='publish_account_id';
-- =============================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------------
-- V205: oa_account.holder_user_id
-- ---------------------------------------------------------------------------
SET @needs_v205_holder := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'oa_account'
      AND COLUMN_NAME = 'holder_user_id'
);

SET @sql_v205_holder := IF(@needs_v205_holder = 0,
    'ALTER TABLE oa_account ADD COLUMN holder_user_id BIGINT NULL COMMENT ''持有人 system_users.id'' AFTER admin_user_id',
    'SELECT ''V205 skip: oa_account.holder_user_id exists'' AS notice'
);
PREPARE stmt_v205_holder FROM @sql_v205_holder;
EXECUTE stmt_v205_holder;
DEALLOCATE PREPARE stmt_v205_holder;

SET @needs_idx_holder := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'oa_account'
      AND INDEX_NAME = 'idx_oa_account_holder'
);

SET @sql_idx_holder := IF(@needs_idx_holder = 0,
    'CREATE INDEX idx_oa_account_holder ON oa_account (tenant_id, holder_user_id)',
    'SELECT ''V205 skip: idx_oa_account_holder exists'' AS notice'
);
PREPARE stmt_idx_holder FROM @sql_idx_holder;
EXECUTE stmt_idx_holder;
DEALLOCATE PREPARE stmt_idx_holder;

-- ---------------------------------------------------------------------------
-- V205: oa_work_task_assignment.publish_account_id
-- ---------------------------------------------------------------------------
SET @needs_v205_publish := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'oa_work_task_assignment'
      AND COLUMN_NAME = 'publish_account_id'
);

SET @sql_v205_publish := IF(@needs_v205_publish = 0,
    'ALTER TABLE oa_work_task_assignment ADD COLUMN publish_account_id BIGINT NULL COMMENT ''发布平台账号 oa_account.id'' AFTER assignee_id',
    'SELECT ''V205 skip: oa_work_task_assignment.publish_account_id exists'' AS notice'
);
PREPARE stmt_v205_publish FROM @sql_v205_publish;
EXECUTE stmt_v205_publish;
DEALLOCATE PREPARE stmt_v205_publish;

SET @needs_idx_publish := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'oa_work_task_assignment'
      AND INDEX_NAME = 'idx_wt_assignment_publish_account'
);

SET @sql_idx_publish := IF(@needs_idx_publish = 0,
    'CREATE INDEX idx_wt_assignment_publish_account ON oa_work_task_assignment (tenant_id, publish_account_id)',
    'SELECT ''V205 skip: idx_wt_assignment_publish_account exists'' AS notice'
);
PREPARE stmt_idx_publish FROM @sql_idx_publish;
EXECUTE stmt_idx_publish;
DEALLOCATE PREPARE stmt_idx_publish;

-- ---------------------------------------------------------------------------
-- V206: oa_account.short_video_status, live_status, operator_user_id
-- ---------------------------------------------------------------------------
SET @needs_v206_sv := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'oa_account'
      AND COLUMN_NAME = 'short_video_status'
);

SET @sql_v206_sv := IF(@needs_v206_sv = 0,
    'ALTER TABLE oa_account ADD COLUMN short_video_status VARCHAR(128) NULL COMMENT ''短视频状态（Excel 原文，抖音/快手）'' AFTER password_encrypted',
    'SELECT ''V206 skip: short_video_status exists'' AS notice'
);
PREPARE stmt_v206_sv FROM @sql_v206_sv;
EXECUTE stmt_v206_sv;
DEALLOCATE PREPARE stmt_v206_sv;

SET @needs_v206_live := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'oa_account'
      AND COLUMN_NAME = 'live_status'
);

SET @sql_v206_live := IF(@needs_v206_live = 0,
    'ALTER TABLE oa_account ADD COLUMN live_status VARCHAR(128) NULL COMMENT ''直播状态（Excel 原文，抖音/快手）'' AFTER short_video_status',
    'SELECT ''V206 skip: live_status exists'' AS notice'
);
PREPARE stmt_v206_live FROM @sql_v206_live;
EXECUTE stmt_v206_live;
DEALLOCATE PREPARE stmt_v206_live;

SET @needs_v206_op := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'oa_account'
      AND COLUMN_NAME = 'operator_user_id'
);

SET @sql_v206_op := IF(@needs_v206_op = 0,
    'ALTER TABLE oa_account ADD COLUMN operator_user_id BIGINT NULL COMMENT ''运营人 Football system_users.id（Excel 运营人）'' AFTER ip_group_id',
    'SELECT ''V206 skip: operator_user_id exists'' AS notice'
);
PREPARE stmt_v206_op FROM @sql_v206_op;
EXECUTE stmt_v206_op;
DEALLOCATE PREPARE stmt_v206_op;

SET @needs_idx_operator := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'oa_account'
      AND INDEX_NAME = 'idx_oa_account_operator_user'
);

SET @sql_idx_operator := IF(@needs_idx_operator = 0,
    'CREATE INDEX idx_oa_account_operator_user ON oa_account (tenant_id, operator_user_id)',
    'SELECT ''V206 skip: idx_oa_account_operator_user exists'' AS notice'
);
PREPARE stmt_idx_operator FROM @sql_idx_operator;
EXECUTE stmt_idx_operator;
DEALLOCATE PREPARE stmt_idx_operator;

-- ---------------------------------------------------------------------------
-- Post-check (expect 4 + 1 rows, all OK)
-- ---------------------------------------------------------------------------
SELECT 'oa_account.holder_user_id' AS object_name,
       CASE WHEN COUNT(*) = 1 THEN 'OK' ELSE 'MISSING' END AS check_status
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_account' AND COLUMN_NAME = 'holder_user_id'
UNION ALL
SELECT 'oa_account.short_video_status',
       CASE WHEN COUNT(*) = 1 THEN 'OK' ELSE 'MISSING' END
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_account' AND COLUMN_NAME = 'short_video_status'
UNION ALL
SELECT 'oa_account.live_status',
       CASE WHEN COUNT(*) = 1 THEN 'OK' ELSE 'MISSING' END
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_account' AND COLUMN_NAME = 'live_status'
UNION ALL
SELECT 'oa_account.operator_user_id',
       CASE WHEN COUNT(*) = 1 THEN 'OK' ELSE 'MISSING' END
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_account' AND COLUMN_NAME = 'operator_user_id'
UNION ALL
SELECT 'oa_work_task_assignment.publish_account_id',
       CASE WHEN COUNT(*) = 1 THEN 'OK' ELSE 'MISSING' END
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_work_task_assignment' AND COLUMN_NAME = 'publish_account_id';
