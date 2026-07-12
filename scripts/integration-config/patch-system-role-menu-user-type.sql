-- Football system_role_menu schema patch for wd (RoleMenuDO.userType)
-- Required after RoleMenuDO gained user_type for permission checks (@PreAuthorize).
-- Idempotent: adds column/index only when missing.

SET @db = DATABASE();

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@db AND table_name='system_role_menu' AND column_name='user_type') = 0,
  'ALTER TABLE system_role_menu ADD COLUMN user_type tinyint NOT NULL DEFAULT 2 COMMENT ''用户类型(1-会员 2-管理员)'' AFTER menu_id',
  'SELECT ''system_role_menu.user_type already exists'' AS info'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=@db AND table_name='system_role_menu' AND index_name='idx_user_type') = 0,
  'CREATE INDEX idx_user_type ON system_role_menu (user_type)',
  'SELECT ''idx_user_type already exists'' AS info'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
