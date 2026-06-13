-- S-12: task-content link + dict_content_status COMPLETED (ADR-016)

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_content_status', '已完成', 'COMPLETED', 8, 'ENABLED');

ALTER TABLE oa_production_content ADD COLUMN task_id BIGINT NULL COMMENT 'linked task 0..1';
ALTER TABLE oa_production_content ADD COLUMN competition_id VARCHAR(64) NULL COMMENT 'competition scheduleId';

CREATE UNIQUE INDEX uk_production_content_task_id ON oa_production_content (task_id);
