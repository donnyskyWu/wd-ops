-- 任务执行页交付附件（ADR-001 本地文件存储，JSON 元数据）

ALTER TABLE oa_task
    ADD COLUMN deliverable_attachments_json VARCHAR(4000) NULL COMMENT '执行人上传的交付附件 JSON';
