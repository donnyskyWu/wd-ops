-- =============================================================================
-- shenyu-ops — V190 legacy sys_* cleanup for EXISTING local DB (manual / one-off)
-- Run AFTER ops-server JAR includes V190 Flyway migration, OR standalone before restart:
--   mysql -h HOST -u USER -p shenyu-ops < scripts/integration-config/drop_legacy_sys_harness_local.sql
--
-- Safe drops (Feign SSOT / dead code): sys_dict_*, sys_operation_log
-- Does NOT drop sys_user* / sys_role* / sys_metadata_* / sys_notification_event / sys_tenant
--   — still referenced by football-module-ops production code (see OPERATIONS-GUIDE.md)
-- =============================================================================
SET NAMES utf8mb4;

SELECT table_name AS before_drop
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN (
    'sys_dict_data', 'sys_dict_type', 'sys_operation_log',
    'sys_user', 'sys_user_token', 'sys_role', 'sys_metadata_entity'
  )
ORDER BY table_name;

DROP TABLE IF EXISTS sys_dict_data;
DROP TABLE IF EXISTS sys_dict_type;
DROP TABLE IF EXISTS sys_operation_log;

SELECT table_name AS after_drop_should_be_empty_for_dropped
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('sys_dict_data', 'sys_dict_type', 'sys_operation_log');

SELECT 'V190 manual drop complete' AS status;
