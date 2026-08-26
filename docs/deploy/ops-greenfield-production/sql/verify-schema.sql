-- =============================================================================
-- Ops DB (shenyu-ops) — Greenfield schema 验证
-- 版本: 2026-08-25 (V190/V191 sys_* cleanup)
-- Prerequisite: Step 2 `01-shenyu-ops-schema.sql` on empty shenyu-ops (not 03 seeds)
-- Target DB: mysql -h HOST -u USER -p shenyu-ops < sql/verify-schema.sql
-- Windows PS: Get-Content sql/verify-schema.sql -Raw | mysql -h HOST -u USER -p shenyu-ops
-- 期望: 所有 check_status = 'OK'；失败行 check_status = 'MISSING'
-- =============================================================================
SET NAMES utf8mb4;

-- ---- oa_* 核心业务表 ----
SELECT 'oa_task' AS object_name,
       CASE WHEN COUNT(*) = 1 THEN 'OK' ELSE 'MISSING' END AS check_status
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'oa_task';

SELECT 'oa_sop_template' AS object_name,
       CASE WHEN COUNT(*) = 1 THEN 'OK' ELSE 'MISSING' END AS check_status
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'oa_sop_template';

SELECT 'oa_sop_node' AS object_name,
       CASE WHEN COUNT(*) = 1 THEN 'OK' ELSE 'MISSING' END AS check_status
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'oa_sop_node';

SELECT 'oa_ip_group' AS object_name,
       CASE WHEN COUNT(*) = 1 THEN 'OK' ELSE 'MISSING' END AS check_status
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'oa_ip_group';

SELECT 'oa_ip_group_anchor_rel' AS object_name,
       CASE WHEN COUNT(*) = 1 THEN 'OK' ELSE 'MISSING' END AS check_status
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'oa_ip_group_anchor_rel';

SELECT 'oa_content' AS object_name,
       CASE WHEN COUNT(*) = 1 THEN 'OK' ELSE 'MISSING' END AS check_status
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'oa_content';

SELECT 'oa_work_task_sheet' AS object_name,
       CASE WHEN COUNT(*) = 1 THEN 'OK' ELSE 'MISSING' END AS check_status
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'oa_work_task_sheet';

SELECT 'oa_report_weekly_feedback' AS object_name,
       CASE WHEN COUNT(*) = 1 THEN 'OK' ELSE 'MISSING' END AS check_status
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'oa_report_weekly_feedback';

-- ---- sys_* 终态保留（配置 SSOT）----
SELECT 'sys_param' AS object_name,
       CASE WHEN COUNT(*) = 1 THEN 'OK' ELSE 'MISSING' END AS check_status
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'sys_param';

SELECT 'sys_message' AS object_name,
       CASE WHEN COUNT(*) = 1 THEN 'OK' ELSE 'MISSING' END AS check_status
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'sys_message';

-- ---- V190 DROP 验收（须不存在）----
SELECT 'no_sys_dict_type' AS object_name,
       CASE WHEN COUNT(*) = 0 THEN 'OK' ELSE 'MISSING' END AS check_status
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'sys_dict_type';

SELECT 'no_sys_dict_data' AS object_name,
       CASE WHEN COUNT(*) = 0 THEN 'OK' ELSE 'MISSING' END AS check_status
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'sys_dict_data';

SELECT 'no_sys_operation_log' AS object_name,
       CASE WHEN COUNT(*) = 0 THEN 'OK' ELSE 'MISSING' END AS check_status
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'sys_operation_log';

-- ---- V191 DROP 验收（须不存在）----
SELECT 'no_sys_tenant' AS object_name,
       CASE WHEN COUNT(*) = 0 THEN 'OK' ELSE 'MISSING' END AS check_status
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'sys_tenant';

SELECT 'no_sys_user' AS object_name,
       CASE WHEN COUNT(*) = 0 THEN 'OK' ELSE 'MISSING' END AS check_status
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'sys_user';

SELECT 'no_sys_user_token' AS object_name,
       CASE WHEN COUNT(*) = 0 THEN 'OK' ELSE 'MISSING' END AS check_status
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'sys_user_token';

SELECT 'no_sys_role' AS object_name,
       CASE WHEN COUNT(*) = 0 THEN 'OK' ELSE 'MISSING' END AS check_status
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'sys_role';

SELECT 'no_sys_permission' AS object_name,
       CASE WHEN COUNT(*) = 0 THEN 'OK' ELSE 'MISSING' END AS check_status
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'sys_permission';

-- ---- Flyway (information_schema gate: avoid 1146 before 01) ----
SET @ops_has_flyway = (
  SELECT COUNT(*)
  FROM information_schema.tables
  WHERE table_schema = DATABASE() AND table_name = 'flyway_schema_history'
);

SET @flyway_versions_sql = IF(
  @ops_has_flyway > 0,
  'SELECT CONCAT(''flyway_v'', version) AS object_name, CASE WHEN success = 1 THEN ''OK'' ELSE ''MISSING'' END AS check_status FROM flyway_schema_history WHERE version IN (''190'', ''191'') UNION ALL SELECT ''flyway_v190'' AS object_name, ''MISSING'' AS check_status WHERE NOT EXISTS (SELECT 1 FROM flyway_schema_history WHERE version = ''190'' AND success = 1) UNION ALL SELECT ''flyway_v191'' AS object_name, ''MISSING'' AS check_status WHERE NOT EXISTS (SELECT 1 FROM flyway_schema_history WHERE version = ''191'' AND success = 1)',
  'SELECT ''flyway_v190'' AS object_name, ''MISSING'' AS check_status UNION ALL SELECT ''flyway_v191'' AS object_name, ''MISSING'' AS check_status'
);
PREPARE flyway_versions_stmt FROM @flyway_versions_sql;
EXECUTE flyway_versions_stmt;
DEALLOCATE PREPARE flyway_versions_stmt;

SET @flyway_count_sql = IF(
  @ops_has_flyway > 0,
  'SELECT ''flyway_sql_migrations_186'' AS object_name, CASE WHEN COUNT(*) = 186 THEN ''OK'' ELSE ''MISSING'' END AS check_status FROM flyway_schema_history WHERE type = ''SQL'' AND success = 1',
  'SELECT ''flyway_sql_migrations_186'' AS object_name, ''MISSING'' AS check_status'
);
PREPARE flyway_count_stmt FROM @flyway_count_sql;
EXECUTE flyway_count_stmt;
DEALLOCATE PREPARE flyway_count_stmt;

-- ---- Football system_* overlay 须不存在（V163/V172；SSOT = shenyu-system）----
SELECT 'no_system_menu_overlay' AS object_name,
       CASE WHEN COUNT(*) = 0 THEN 'OK' ELSE 'MISSING' END AS check_status
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'system_menu';

SELECT 'no_system_users_overlay' AS object_name,
       CASE WHEN COUNT(*) = 0 THEN 'OK' ELSE 'MISSING' END AS check_status
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'system_users';

SELECT 'no_system_user_author_overlay' AS object_name,
       CASE WHEN COUNT(*) = 0 THEN 'OK' ELSE 'MISSING' END AS check_status
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'system_user_author';

SELECT 'no_system_user_data_overlay' AS object_name,
       CASE WHEN COUNT(*) = 0 THEN 'OK' ELSE 'MISSING' END AS check_status
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'system_user_data';

-- ---- 信息性：保留表（V191 未 DROP）----
-- sys_metadata_*     MetadataServiceImpl (M6)
-- sys_notification_event  NotificationServiceImpl 去重账本