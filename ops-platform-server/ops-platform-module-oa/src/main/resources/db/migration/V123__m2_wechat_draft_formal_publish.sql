-- M2 公众号发布拆分为「发布为草稿」与「正式发布」（Spec gap · ADR 待补）

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_content_status', '已发布草稿', 'PUBLISHED_DRAFT', 7, 'ENABLED'),
('dict_content_status', '已正式发布', 'FORMALLY_PUBLISHED', 8, 'ENABLED')
ON DUPLICATE KEY UPDATE label = VALUES(label), sort = VALUES(sort);

UPDATE sys_dict_data SET sort = 9 WHERE dict_type = 'dict_content_status' AND dict_value = 'PUBLISHED';
UPDATE sys_dict_data SET sort = 10 WHERE dict_type = 'dict_content_status' AND dict_value = 'UNPUBLISHED';
UPDATE sys_dict_data SET sort = 11 WHERE dict_type = 'dict_content_status' AND dict_value = 'COMPLETED';

ALTER TABLE oa_content_publish_record
    ADD COLUMN publish_id VARCHAR(128) NULL COMMENT 'freepublish submit 返回的 publish_id' AFTER external_id;
