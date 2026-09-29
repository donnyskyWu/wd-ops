-- Quick verify on shenyu-ops (read-only)
USE `shenyu-ops`;

SELECT COLUMN_NAME, DATA_TYPE, COLUMN_COMMENT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'oa_account'
  AND COLUMN_NAME IN ('holder_user_id', 'short_video_status', 'live_status', 'operator_user_id')
ORDER BY ORDINAL_POSITION;

SELECT COLUMN_NAME, DATA_TYPE, COLUMN_COMMENT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'oa_work_task_assignment'
  AND COLUMN_NAME = 'publish_account_id';

SELECT version, success, installed_by, installed_on
FROM flyway_schema_history
WHERE version IN ('205', '206')
ORDER BY installed_rank;
