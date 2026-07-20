-- M2 AI 内容对话历史持久化（ADR-053）：按用户+作用域保存最近 10 轮对话

CREATE TABLE IF NOT EXISTS oa_ai_content_conversation (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  user_id BIGINT NOT NULL COMMENT '用户 ID',
  scope_key VARCHAR(64) NOT NULL COMMENT '作用域键 content:{id} / author:{id} / global',
  content_id BIGINT NULL COMMENT '关联内容 ID（编辑时）',
  author_id BIGINT NULL COMMENT '关联作者 ID（新建时）',
  conversation_json JSON NULL COMMENT '对话消息数组 [{role,content}]',
  round_count INT NOT NULL DEFAULT 0 COMMENT '对话轮次',
  source_session_id VARCHAR(64) NULL COMMENT '来源会话 ID',
  creator VARCHAR(64) DEFAULT 'system',
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater VARCHAR(64) DEFAULT 'system',
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted SMALLINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_ai_content_conv_scope (tenant_id, user_id, scope_key),
  KEY idx_ai_content_conv_tenant (tenant_id)
) COMMENT='AI 内容对话历史';
