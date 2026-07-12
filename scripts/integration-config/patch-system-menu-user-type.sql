-- Football system_menu schema patch for wd (MenuDO.userType)
ALTER TABLE system_menu
  ADD COLUMN IF NOT EXISTS user_type tinyint NOT NULL DEFAULT 2 COMMENT '用户类型' AFTER always_show;
