-- B-WP4-ARCHIVE rollback (optional; Q3 = no hold window, but scripts retained)
-- Target: localhost:3306/wd ONLY
-- Prefer restore from backup/wd-q1-candidates-20260731.sql if unsure.

-- 1) Undo RENAME of sys_operation_log
-- RENAME TABLE `archive_wd`.`sys_operation_log` TO `wd`.`sys_operation_log`;

-- 2) Drop stop-write triggers
USE `wd`;
DROP TRIGGER IF EXISTS trg_bwp4_sys_user_bi;
DROP TRIGGER IF EXISTS trg_bwp4_sys_user_bu;
DROP TRIGGER IF EXISTS trg_bwp4_sys_user_bd;
DROP TRIGGER IF EXISTS trg_bwp4_sys_user_token_bi;
DROP TRIGGER IF EXISTS trg_bwp4_sys_user_token_bu;
DROP TRIGGER IF EXISTS trg_bwp4_sys_user_token_bd;
DROP TRIGGER IF EXISTS trg_bwp4_sys_role_bi;
DROP TRIGGER IF EXISTS trg_bwp4_sys_role_bu;
DROP TRIGGER IF EXISTS trg_bwp4_sys_role_bd;
DROP TRIGGER IF EXISTS trg_bwp4_sys_user_role_bi;
DROP TRIGGER IF EXISTS trg_bwp4_sys_user_role_bu;
DROP TRIGGER IF EXISTS trg_bwp4_sys_user_role_bd;
DROP TRIGGER IF EXISTS trg_bwp4_sys_role_permission_bi;
DROP TRIGGER IF EXISTS trg_bwp4_sys_role_permission_bu;
DROP TRIGGER IF EXISTS trg_bwp4_sys_role_permission_bd;
DROP TRIGGER IF EXISTS trg_bwp4_sys_permission_bi;
DROP TRIGGER IF EXISTS trg_bwp4_sys_permission_bu;
DROP TRIGGER IF EXISTS trg_bwp4_sys_permission_bd;
DROP TRIGGER IF EXISTS trg_bwp4_sys_dict_type_bi;
DROP TRIGGER IF EXISTS trg_bwp4_sys_dict_type_bu;
DROP TRIGGER IF EXISTS trg_bwp4_sys_dict_type_bd;
DROP TRIGGER IF EXISTS trg_bwp4_sys_dict_data_bi;
DROP TRIGGER IF EXISTS trg_bwp4_sys_dict_data_bu;
DROP TRIGGER IF EXISTS trg_bwp4_sys_dict_data_bd;
DROP TRIGGER IF EXISTS trg_bwp4_system_users_bi;
DROP TRIGGER IF EXISTS trg_bwp4_system_users_bu;
DROP TRIGGER IF EXISTS trg_bwp4_system_users_bd;

-- 3) Full table restore example:
-- mysql -h 127.0.0.1 -P 3306 -u root -proot wd < backup/wd-q1-candidates-20260731.sql
