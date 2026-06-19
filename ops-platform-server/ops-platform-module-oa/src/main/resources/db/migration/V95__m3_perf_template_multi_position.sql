-- M3: 考核模板支持多岗位（positions_json 替代 position 单值）
-- Idempotent: safe when column/index already migrated (dev hot-reload / partial apply)

SET @has_positions_json := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_perf_template' AND COLUMN_NAME = 'positions_json'
);
SET @add_col_sql := IF(@has_positions_json = 0,
    'ALTER TABLE oa_perf_template ADD COLUMN positions_json TEXT NULL COMMENT ''positions json'' AFTER position',
    'SELECT 1');
PREPARE add_col FROM @add_col_sql;
EXECUTE add_col;
DEALLOCATE PREPARE add_col;

SET @has_position := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_perf_template' AND COLUMN_NAME = 'position'
);
SET @migrate_sql := IF(@has_position > 0,
    'UPDATE oa_perf_template SET positions_json = CONCAT(''["'', position, ''"]'') WHERE positions_json IS NULL AND position IS NOT NULL AND position <> ''''''',
    'SELECT 1');
PREPARE migrate_pos FROM @migrate_sql;
EXECUTE migrate_pos;
DEALLOCATE PREPARE migrate_pos;

UPDATE oa_perf_template
SET positions_json = '[]'
WHERE positions_json IS NULL;

ALTER TABLE oa_perf_template MODIFY positions_json TEXT NOT NULL;

SET @has_idx := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_perf_template' AND INDEX_NAME = 'idx_oa_perf_template_position'
);
SET @drop_idx_sql := IF(@has_idx > 0,
    'ALTER TABLE oa_perf_template DROP INDEX idx_oa_perf_template_position',
    'SELECT 1');
PREPARE drop_idx FROM @drop_idx_sql;
EXECUTE drop_idx;
DEALLOCATE PREPARE drop_idx;

SET @drop_col_sql := IF(@has_position > 0,
    'ALTER TABLE oa_perf_template DROP COLUMN position',
    'SELECT 1');
PREPARE drop_col FROM @drop_col_sql;
EXECUTE drop_col;
DEALLOCATE PREPARE drop_col;
