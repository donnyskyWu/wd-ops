-- BLK-M2-008 / BLK-M2-007: SOP node instruction + attachment URLs (read-only JSON string)

ALTER TABLE oa_sop_node
    ADD COLUMN instruction_text VARCHAR(2000) NULL COMMENT '执行说明（任务执行页只读）' AFTER node_type,
    ADD COLUMN attachment_urls JSON NULL COMMENT '附件 [{name,url}] 只读，上传 API 未决' AFTER instruction_text;

UPDATE oa_sop_node
SET instruction_text = '根据赛事素材撰写短视频文案，注意品牌调性与赛事关键词。'
WHERE id = 9402;

UPDATE oa_sop_node
SET instruction_text = '将已审核内容发布至目标平台，并在交付说明中填写发布链接与发布时间。'
WHERE id = 9403;

UPDATE oa_sop_node
SET attachment_urls = JSON_ARRAY(
    JSON_OBJECT('name', '品牌规范示例.pdf', 'url', 'https://example.com/seed/m2/brand-guide.pdf')
)
WHERE id = 9402;
