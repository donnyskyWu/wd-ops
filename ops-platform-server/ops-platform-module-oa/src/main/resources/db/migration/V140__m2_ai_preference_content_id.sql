-- M2 AI 内容偏好总结：采纳时关联内容 ID（ADR-053 偏好持久化）

ALTER TABLE oa_ai_content_preference
  ADD COLUMN content_id BIGINT NULL COMMENT '关联内容 ID（采纳时写入）' AFTER source_session_id;
