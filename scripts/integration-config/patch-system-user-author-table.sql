-- Football system_user_author schema patch for wd (UserAuthorDO)
-- Required for 用户管理 → 分配权限 list-user-authors / assign-user-role.
-- Idempotent: creates table only when missing.

SET @db = DATABASE();

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=@db AND table_name='system_user_author') = 0,
  'CREATE TABLE system_user_author (
    id bigint NOT NULL AUTO_INCREMENT COMMENT ''自增编号'',
    user_id bigint NOT NULL COMMENT ''用户ID'',
    author_id bigint NOT NULL COMMENT ''作者ID'',
    creator varchar(64) DEFAULT '''' COMMENT ''创建者'',
    create_time datetime DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'',
    updater varchar(64) DEFAULT '''' COMMENT ''更新者'',
    update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'',
    deleted bit(1) DEFAULT b''0'' COMMENT ''是否删除'',
    tenant_id bigint NOT NULL DEFAULT 0 COMMENT ''租户编号'',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_author_id (author_id)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT ''用户和作者关联表''',
  'SELECT ''system_user_author already exists'' AS info'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
