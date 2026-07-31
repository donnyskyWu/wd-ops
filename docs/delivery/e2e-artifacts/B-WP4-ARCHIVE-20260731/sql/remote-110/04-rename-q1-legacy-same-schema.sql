-- B-WP4-ARCHIVE · Q1 #1/#3/#5 同库 RENAME（替代 stop-write trigger；无 SUPER）
-- Target: 110.42.49.224:3306/shenyu-ops
-- Pre: backup-remote-110/shenyu-ops-q1-candidates-20260731.sql
-- Policy: prefer RENAME TABLE ... TO archive_<name>; skip if source missing;
--         if RENAME fails (FK etc.) operator may DROP after logging (user-authorized).
-- SKIP: #6 §3.4 bridge; #7 system_users (active FootballOAuth2MasterTokenMapper overlay — see 00-README)

USE `shenyu-ops`;

-- ---- #1 sys_user* ----
RENAME TABLE `sys_user` TO `archive_sys_user`;
RENAME TABLE `sys_user_token` TO `archive_sys_user_token`;
RENAME TABLE `sys_user_role` TO `archive_sys_user_role`;

-- ---- #3 legacy role / permission ----
RENAME TABLE `sys_role` TO `archive_sys_role`;
RENAME TABLE `sys_role_permission` TO `archive_sys_role_permission`;
RENAME TABLE `sys_permission` TO `archive_sys_permission`;

-- ---- #5 sys_dict_* ----
RENAME TABLE `sys_dict_type` TO `archive_sys_dict_type`;
RENAME TABLE `sys_dict_data` TO `archive_sys_dict_data`;
