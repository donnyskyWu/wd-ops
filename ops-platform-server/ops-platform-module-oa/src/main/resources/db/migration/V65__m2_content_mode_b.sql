-- S-13: 任务驱动内容编辑（模式 B）— dict_document_type + 扩展字段（ADR-016）

INSERT INTO sys_dict_type (type, name, status) VALUES
('dict_document_type', '文档类型', 'ENABLED')
ON DUPLICATE KEY UPDATE name = VALUES(name), status = VALUES(status);

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_document_type', '短视频文案', 'SHORT_VIDEO_SCRIPT', 1, 'ENABLED'),
('dict_document_type', '新号引流', 'NEW_ACCOUNT_TRAFFIC', 2, 'ENABLED'),
('dict_document_type', '赛后复盘', 'POST_MATCH_REVIEW', 3, 'ENABLED'),
('dict_document_type', '正式方案', 'OFFICIAL_PLAN', 4, 'ENABLED'),
('dict_document_type', '预热前瞻', 'PREHEAT_PREVIEW', 5, 'ENABLED');

ALTER TABLE oa_production_content ADD COLUMN document_type VARCHAR(50) NULL COMMENT 'dict_document_type, ARTICLE only';
ALTER TABLE oa_production_content ADD COLUMN ip_group_id BIGINT NULL COMMENT 'oa_ip_group';
ALTER TABLE oa_production_content ADD COLUMN author_id BIGINT NULL COMMENT 'oa_author';
ALTER TABLE oa_production_content ADD COLUMN generated_video_url VARCHAR(512) NULL COMMENT 'AI generated video';
ALTER TABLE oa_production_content ADD COLUMN final_video_url VARCHAR(512) NULL COMMENT 'uploaded or AI video';
