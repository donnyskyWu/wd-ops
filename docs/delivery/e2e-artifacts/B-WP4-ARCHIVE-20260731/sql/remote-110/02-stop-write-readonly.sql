-- B-WP4-ARCHIVE · Q1 停写只读（#1/#2/#3/#5/#7）
-- Target: 110.42.49.224:3306/shenyu-ops (beta test; OPS master = former wd)
-- MySQL 5.7.x — Effect: block INSERT/UPDATE/DELETE; SELECT still allowed
-- Does NOT touch §3.4 bridge columns (#6 暂不纳入)

USE `shenyu-ops`;

DELIMITER $$

-- ---- #1 sys_user ----
DROP TRIGGER IF EXISTS trg_bwp4_sys_user_bi$$
CREATE TRIGGER trg_bwp4_sys_user_bi BEFORE INSERT ON `sys_user`
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'B-WP4-ARCHIVE: shenyu-ops.sys_user is stop-write (read-only)';
END$$
DROP TRIGGER IF EXISTS trg_bwp4_sys_user_bu$$
CREATE TRIGGER trg_bwp4_sys_user_bu BEFORE UPDATE ON `sys_user`
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'B-WP4-ARCHIVE: shenyu-ops.sys_user is stop-write (read-only)';
END$$
DROP TRIGGER IF EXISTS trg_bwp4_sys_user_bd$$
CREATE TRIGGER trg_bwp4_sys_user_bd BEFORE DELETE ON `sys_user`
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'B-WP4-ARCHIVE: shenyu-ops.sys_user is stop-write (read-only)';
END$$

-- ---- #2 sys_user_token ----
DROP TRIGGER IF EXISTS trg_bwp4_sys_user_token_bi$$
CREATE TRIGGER trg_bwp4_sys_user_token_bi BEFORE INSERT ON `sys_user_token`
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'B-WP4-ARCHIVE: shenyu-ops.sys_user_token is stop-write (read-only)';
END$$
DROP TRIGGER IF EXISTS trg_bwp4_sys_user_token_bu$$
CREATE TRIGGER trg_bwp4_sys_user_token_bu BEFORE UPDATE ON `sys_user_token`
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'B-WP4-ARCHIVE: shenyu-ops.sys_user_token is stop-write (read-only)';
END$$
DROP TRIGGER IF EXISTS trg_bwp4_sys_user_token_bd$$
CREATE TRIGGER trg_bwp4_sys_user_token_bd BEFORE DELETE ON `sys_user_token`
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'B-WP4-ARCHIVE: shenyu-ops.sys_user_token is stop-write (read-only)';
END$$

-- ---- #3 legacy role tables ----
DROP TRIGGER IF EXISTS trg_bwp4_sys_role_bi$$
CREATE TRIGGER trg_bwp4_sys_role_bi BEFORE INSERT ON `sys_role`
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'B-WP4-ARCHIVE: shenyu-ops.sys_role is stop-write (read-only)';
END$$
DROP TRIGGER IF EXISTS trg_bwp4_sys_role_bu$$
CREATE TRIGGER trg_bwp4_sys_role_bu BEFORE UPDATE ON `sys_role`
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'B-WP4-ARCHIVE: shenyu-ops.sys_role is stop-write (read-only)';
END$$
DROP TRIGGER IF EXISTS trg_bwp4_sys_role_bd$$
CREATE TRIGGER trg_bwp4_sys_role_bd BEFORE DELETE ON `sys_role`
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'B-WP4-ARCHIVE: shenyu-ops.sys_role is stop-write (read-only)';
END$$

DROP TRIGGER IF EXISTS trg_bwp4_sys_user_role_bi$$
CREATE TRIGGER trg_bwp4_sys_user_role_bi BEFORE INSERT ON `sys_user_role`
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'B-WP4-ARCHIVE: shenyu-ops.sys_user_role is stop-write (read-only)';
END$$
DROP TRIGGER IF EXISTS trg_bwp4_sys_user_role_bu$$
CREATE TRIGGER trg_bwp4_sys_user_role_bu BEFORE UPDATE ON `sys_user_role`
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'B-WP4-ARCHIVE: shenyu-ops.sys_user_role is stop-write (read-only)';
END$$
DROP TRIGGER IF EXISTS trg_bwp4_sys_user_role_bd$$
CREATE TRIGGER trg_bwp4_sys_user_role_bd BEFORE DELETE ON `sys_user_role`
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'B-WP4-ARCHIVE: shenyu-ops.sys_user_role is stop-write (read-only)';
END$$

DROP TRIGGER IF EXISTS trg_bwp4_sys_role_permission_bi$$
CREATE TRIGGER trg_bwp4_sys_role_permission_bi BEFORE INSERT ON `sys_role_permission`
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'B-WP4-ARCHIVE: shenyu-ops.sys_role_permission is stop-write (read-only)';
END$$
DROP TRIGGER IF EXISTS trg_bwp4_sys_role_permission_bu$$
CREATE TRIGGER trg_bwp4_sys_role_permission_bu BEFORE UPDATE ON `sys_role_permission`
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'B-WP4-ARCHIVE: shenyu-ops.sys_role_permission is stop-write (read-only)';
END$$
DROP TRIGGER IF EXISTS trg_bwp4_sys_role_permission_bd$$
CREATE TRIGGER trg_bwp4_sys_role_permission_bd BEFORE DELETE ON `sys_role_permission`
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'B-WP4-ARCHIVE: shenyu-ops.sys_role_permission is stop-write (read-only)';
END$$

DROP TRIGGER IF EXISTS trg_bwp4_sys_permission_bi$$
CREATE TRIGGER trg_bwp4_sys_permission_bi BEFORE INSERT ON `sys_permission`
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'B-WP4-ARCHIVE: shenyu-ops.sys_permission is stop-write (read-only)';
END$$
DROP TRIGGER IF EXISTS trg_bwp4_sys_permission_bu$$
CREATE TRIGGER trg_bwp4_sys_permission_bu BEFORE UPDATE ON `sys_permission`
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'B-WP4-ARCHIVE: shenyu-ops.sys_permission is stop-write (read-only)';
END$$
DROP TRIGGER IF EXISTS trg_bwp4_sys_permission_bd$$
CREATE TRIGGER trg_bwp4_sys_permission_bd BEFORE DELETE ON `sys_permission`
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'B-WP4-ARCHIVE: shenyu-ops.sys_permission is stop-write (read-only)';
END$$

-- ---- #5 sys_dict_* ----
DROP TRIGGER IF EXISTS trg_bwp4_sys_dict_type_bi$$
CREATE TRIGGER trg_bwp4_sys_dict_type_bi BEFORE INSERT ON `sys_dict_type`
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'B-WP4-ARCHIVE: shenyu-ops.sys_dict_type is stop-write (read-only)';
END$$
DROP TRIGGER IF EXISTS trg_bwp4_sys_dict_type_bu$$
CREATE TRIGGER trg_bwp4_sys_dict_type_bu BEFORE UPDATE ON `sys_dict_type`
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'B-WP4-ARCHIVE: shenyu-ops.sys_dict_type is stop-write (read-only)';
END$$
DROP TRIGGER IF EXISTS trg_bwp4_sys_dict_type_bd$$
CREATE TRIGGER trg_bwp4_sys_dict_type_bd BEFORE DELETE ON `sys_dict_type`
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'B-WP4-ARCHIVE: shenyu-ops.sys_dict_type is stop-write (read-only)';
END$$

DROP TRIGGER IF EXISTS trg_bwp4_sys_dict_data_bi$$
CREATE TRIGGER trg_bwp4_sys_dict_data_bi BEFORE INSERT ON `sys_dict_data`
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'B-WP4-ARCHIVE: shenyu-ops.sys_dict_data is stop-write (read-only)';
END$$
DROP TRIGGER IF EXISTS trg_bwp4_sys_dict_data_bu$$
CREATE TRIGGER trg_bwp4_sys_dict_data_bu BEFORE UPDATE ON `sys_dict_data`
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'B-WP4-ARCHIVE: shenyu-ops.sys_dict_data is stop-write (read-only)';
END$$
DROP TRIGGER IF EXISTS trg_bwp4_sys_dict_data_bd$$
CREATE TRIGGER trg_bwp4_sys_dict_data_bd BEFORE DELETE ON `sys_dict_data`
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'B-WP4-ARCHIVE: shenyu-ops.sys_dict_data is stop-write (read-only)';
END$$

-- ---- #7 system_users overlay（停写；Mapper 保留见 Q4）----
DROP TRIGGER IF EXISTS trg_bwp4_system_users_bi$$
CREATE TRIGGER trg_bwp4_system_users_bi BEFORE INSERT ON `system_users`
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'B-WP4-ARCHIVE: shenyu-ops.system_users overlay is stop-write (read-only)';
END$$
DROP TRIGGER IF EXISTS trg_bwp4_system_users_bu$$
CREATE TRIGGER trg_bwp4_system_users_bu BEFORE UPDATE ON `system_users`
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'B-WP4-ARCHIVE: shenyu-ops.system_users overlay is stop-write (read-only)';
END$$
DROP TRIGGER IF EXISTS trg_bwp4_system_users_bd$$
CREATE TRIGGER trg_bwp4_system_users_bd BEFORE DELETE ON `system_users`
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'B-WP4-ARCHIVE: shenyu-ops.system_users overlay is stop-write (read-only)';
END$$

DELIMITER ;
