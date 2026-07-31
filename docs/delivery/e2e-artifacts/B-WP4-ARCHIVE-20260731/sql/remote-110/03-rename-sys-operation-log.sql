-- B-WP4-ARCHIVE · Q1 #4 RENAME archive（同库；非 CREATE DATABASE）
-- Target: 110.42.49.224:3306/shenyu-ops (beta test)
-- Pre: backup-remote-110/shenyu-ops-q1-candidates-20260731.sql
-- Note: MySQL 5.7 · shenyu-ops@% 无 CREATE DATABASE → 同库 archive_* 前缀

USE `shenyu-ops`;

-- Move table out of live name (reads of shenyu-ops.sys_operation_log will fail if any remain)
RENAME TABLE `sys_operation_log` TO `archive_sys_operation_log`;
