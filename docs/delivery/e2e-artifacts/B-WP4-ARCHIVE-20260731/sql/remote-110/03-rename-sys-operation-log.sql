-- B-WP4-ARCHIVE · Q1 #4 RENAME archive（确认无读后；非 DROP）
-- Target: 110.42.49.224:3306/shenyu-ops (beta test)
-- Pre: backup-remote-110/shenyu-ops-q1-candidates-20260731.sql
-- Note: MySQL 5.7 → utf8mb4_general_ci (not 8.0 utf8mb4_0900_ai_ci)
-- Requires privilege to CREATE DATABASE archive_wd (shenyu-ops@% may lack this)

CREATE DATABASE IF NOT EXISTS `archive_wd`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;

-- Move table out of application schema (reads of shenyu-ops.sys_operation_log will fail if any remain)
RENAME TABLE `shenyu-ops`.`sys_operation_log` TO `archive_wd`.`sys_operation_log`;
