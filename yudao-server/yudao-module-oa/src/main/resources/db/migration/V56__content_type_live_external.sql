-- V56: 作品类型 dict_content_type 补齐 LIVE；外部作品表增加 content_type

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_content_type', '直播', 'LIVE', 3, 'ENABLED')
ON DUPLICATE KEY UPDATE label = VALUES(label), sort = VALUES(sort);

-- 历史 VIDEO 与短视频语义对齐
UPDATE oa_content SET content_type = 'SHORT_VIDEO' WHERE content_type = 'VIDEO';

ALTER TABLE oa_external_work ADD COLUMN content_type VARCHAR(20) NULL COMMENT '作品类型 dict_content_type' AFTER platform_type;

UPDATE oa_external_work SET content_type = 'SHORT_VIDEO'
WHERE content_type IS NULL
  AND platform_type IN ('DOUYIN', 'KUAISHOU', 'WECHAT_VIDEO', 'XIAOHONGSHU');

UPDATE oa_external_work SET content_type = 'ARTICLE'
WHERE content_type IS NULL
  AND platform_type IN ('WECHAT_OFFICIAL', 'WECHAT_SERVICE');

UPDATE oa_external_work SET content_type = 'SHORT_VIDEO'
WHERE content_type IS NULL;
