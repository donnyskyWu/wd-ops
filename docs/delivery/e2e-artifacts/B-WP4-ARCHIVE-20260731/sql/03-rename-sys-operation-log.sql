-- B-WP4-ARCHIVE · Q1 #4 RENAME archive（确认无读后；非 DROP）
-- Target: localhost:3306/wd ONLY
-- Pre: backup/wd-q1-candidates-20260731.sql

CREATE DATABASE IF NOT EXISTS `archive_wd`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

-- Move table out of application schema (reads of wd.sys_operation_log will fail if any remain)
RENAME TABLE `wd`.`sys_operation_log` TO `archive_wd`.`sys_operation_log`;
