-- B-WP4-ARCHIVE rollback (optional; Q3 = no hold window)
-- Target: 110.42.49.224:3306/shenyu-ops · same-schema archive_* → original names
-- Prefer restore from backup-remote-110/shenyu-ops-q1-candidates-20260731.sql if unsure.

USE `shenyu-ops`;

-- RENAME TABLE `archive_sys_operation_log` TO `sys_operation_log`;
-- RENAME TABLE `archive_sys_user` TO `sys_user`;
-- RENAME TABLE `archive_sys_user_token` TO `sys_user_token`;
-- RENAME TABLE `archive_sys_user_role` TO `sys_user_role`;
-- RENAME TABLE `archive_sys_role` TO `sys_role`;
-- RENAME TABLE `archive_sys_role_permission` TO `sys_role_permission`;
-- RENAME TABLE `archive_sys_permission` TO `sys_permission`;
-- RENAME TABLE `archive_sys_dict_type` TO `sys_dict_type`;
-- RENAME TABLE `archive_sys_dict_data` TO `sys_dict_data`;

-- Full restore example:
-- mysql -h 110.42.49.224 -P 3306 -u shenyu-ops -p shenyu-ops < backup-remote-110/shenyu-ops-q1-candidates-20260731.sql
