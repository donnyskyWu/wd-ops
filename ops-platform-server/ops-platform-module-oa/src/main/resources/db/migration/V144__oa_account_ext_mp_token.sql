-- M4: 公众号扩展表补齐 mp_token_encrypted（与 oa_account V119 对齐）

SET @has_mp_token := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_account_ext' AND COLUMN_NAME = 'mp_token_encrypted'
);
SET @sql_add_mp_token := IF(@has_mp_token = 0,
    'ALTER TABLE oa_account_ext ADD COLUMN mp_token_encrypted TEXT NULL COMMENT ''公众号后台 Token AES-256'' AFTER cookie_encrypted',
    'SELECT 1');
PREPARE stmt FROM @sql_add_mp_token;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
