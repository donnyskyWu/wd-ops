-- V163: Drop clearly redundant objects in shenyu-ops (test/prod safe when SSOT = shenyu-system).
-- Inventory: docs/delivery/OPS-FOOTBALL-MERGE-CLEANUP-INVENTORY.md §3.1 / §3.3 / P0-4
--
-- KEEPS (master overlay still referenced by FootballOAuth2MasterTokenMapper @DS master):
--   system_users, system_role, system_menu, system_user_role, system_oauth2_access_token
--
-- DROPS:
--   - Manual backup tables from 2026-07-16 import
--   - Football demo sample tables (not OPS)
--   - Duplicate Football infra copied into shenyu-ops (dict/mail/sms/social/notify/logs/dept/post/tenant)
--   - Empty legacy standalone sys_* audit/dept/login tables

SET NAMES utf8mb4;
SET @ops_db = DATABASE();

-- ---------------------------------------------------------------------------
-- 1) Manual backup tables (pre-merge snapshots)
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS system_menu_backup_20260716;
DROP TABLE IF EXISTS system_role_menu_backup_20260716;

-- ---------------------------------------------------------------------------
-- 2) Football demo tables (sample data, not OPS domain)
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS football_demo01_contact;
DROP TABLE IF EXISTS football_demo02_category;
DROP TABLE IF EXISTS football_demo03_course;
DROP TABLE IF EXISTS football_demo03_grade;
DROP TABLE IF EXISTS football_demo03_student;

-- ---------------------------------------------------------------------------
-- 3) Duplicate Football system tables in shenyu-ops (SSOT = shenyu-system @DS system)
--    Dict reads via SystemDictAdapter @DS("system"); mail/sms/social not used on master DS.
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS system_dict_data;
DROP TABLE IF EXISTS system_dict_type;
DROP TABLE IF EXISTS system_mail_log;
DROP TABLE IF EXISTS system_mail_template;
DROP TABLE IF EXISTS system_mail_account;
DROP TABLE IF EXISTS system_sms_log;
DROP TABLE IF EXISTS system_sms_template;
DROP TABLE IF EXISTS system_sms_code;
DROP TABLE IF EXISTS system_sms_channel;
DROP TABLE IF EXISTS system_social_user_bind;
DROP TABLE IF EXISTS system_social_user;
DROP TABLE IF EXISTS system_social_client;
DROP TABLE IF EXISTS system_notify_message;
DROP TABLE IF EXISTS system_notify_template;
DROP TABLE IF EXISTS system_notice;
DROP TABLE IF EXISTS system_operate_log;
DROP TABLE IF EXISTS system_login_log;
DROP TABLE IF EXISTS system_dept;
DROP TABLE IF EXISTS system_post;
DROP TABLE IF EXISTS system_tenant_package;
DROP TABLE IF EXISTS system_tenant;
DROP TABLE IF EXISTS system_user_author;
DROP TABLE IF EXISTS system_user_data;
DROP TABLE IF EXISTS system_user_post;
DROP TABLE IF EXISTS system_oauth2_refresh_token;
DROP TABLE IF EXISTS system_oauth2_code;
DROP TABLE IF EXISTS system_oauth2_approve;
DROP TABLE IF EXISTS system_oauth2_client;

-- ---------------------------------------------------------------------------
-- 4) Empty legacy standalone tables (dev-token harness only; production/test use Football auth)
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS sys_audit_log;
DROP TABLE IF EXISTS sys_dept;
DROP TABLE IF EXISTS sys_login_log;

-- NOTE: sys_dict_* retained as OPS staging until dict ownership fully cut to Football ops scripts.
-- NOTE: sys_operation_log retained until OperationLogRecorder local write path is removed (CLEANUP P0-6).
-- NOTE: system_users/menu/role/user_role/oauth2_access_token on master kept for overlay fallback.
