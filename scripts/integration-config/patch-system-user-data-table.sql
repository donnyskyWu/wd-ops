-- Football system_user_data schema patch for wd (UserDataDO)
-- Required for 用户管理 → 分配权限 assign-user-role (下级数据权限清理/写入).
-- Idempotent: creates table only when missing.

SET @db = DATABASE();

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=@db AND table_name='system_user_data') = 0,
  'CREATE TABLE system_user_data (
    id bigint NOT NULL AUTO_INCREMENT COMMENT ''自增编号'',
    user_id bigint NOT NULL COMMENT ''用户ID'',
    user_id_union bigint NOT NULL COMMENT ''绑定用户ID'',
    creator varchar(64) DEFAULT '''' COMMENT ''创建者'',
    create_time datetime DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'',
    updater varchar(64) DEFAULT '''' COMMENT ''更新者'',
    update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'',
    deleted bit(1) DEFAULT b''0'' COMMENT ''是否删除'',
    tenant_id bigint NOT NULL DEFAULT 0 COMMENT ''租户编号'',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_user_id_union (user_id_union)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT ''用户关联数据表''',
  'SELECT ''system_user_data already exists'' AS info'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
