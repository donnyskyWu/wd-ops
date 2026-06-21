-- M3: 考核模板支持多岗位（positions_json 替代 position 单值）

ALTER TABLE oa_perf_template
    ADD COLUMN positions_json TEXT NULL COMMENT 'positions json';

UPDATE oa_perf_template
SET positions_json = CONCAT('["', position, '"]')
WHERE positions_json IS NULL AND position IS NOT NULL AND position <> '';

UPDATE oa_perf_template
SET positions_json = '[]'
WHERE positions_json IS NULL;

ALTER TABLE oa_perf_template MODIFY positions_json TEXT NOT NULL;

ALTER TABLE oa_perf_template DROP INDEX idx_oa_perf_template_position;

ALTER TABLE oa_perf_template DROP COLUMN position;
