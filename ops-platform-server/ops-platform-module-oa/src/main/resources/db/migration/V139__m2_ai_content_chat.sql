-- M2 AI 内容对话生成（S-15 · ADR-053）：方案类型/主播风格字典、scheme_type 列、会话采纳表、提示词种子

SET @next_type_id = (SELECT COALESCE(MAX(id), 0) + 1 FROM sys_dict_type);

INSERT INTO sys_dict_type (id, type, name, status)
SELECT @next_type_id, 'dict_scheme_type', '赛事方案类型', 'ENABLED'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE type = 'dict_scheme_type');

SET @next_type_id2 = (SELECT COALESCE(MAX(id), 0) + 1 FROM sys_dict_type);

INSERT INTO sys_dict_type (id, type, name, status)
SELECT @next_type_id2, 'dict_anchor_style', '主播风格', 'ENABLED'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE type = 'dict_anchor_style');

SET @next_data_id = (SELECT COALESCE(MAX(id), 0) FROM sys_dict_data);

INSERT INTO sys_dict_data (id, dict_type, label, dict_value, sort, status) VALUES
(@next_data_id + 1, 'dict_scheme_type', '胜平负分析', 'WIN_DRAW_LOSE', 1, 'ENABLED'),
(@next_data_id + 2, 'dict_scheme_type', '让球分析', 'HANDICAP', 2, 'ENABLED'),
(@next_data_id + 3, 'dict_scheme_type', '大小球分析', 'OVER_UNDER', 3, 'ENABLED'),
(@next_data_id + 4, 'dict_scheme_type', '比分预测', 'SCORE_PREDICT', 4, 'ENABLED'),
(@next_data_id + 5, 'dict_scheme_type', '综合推荐', 'COMPREHENSIVE', 5, 'ENABLED'),
(@next_data_id + 6, 'dict_anchor_style', '激进型', 'aggressive', 1, 'ENABLED'),
(@next_data_id + 7, 'dict_anchor_style', '稳健型', 'conservative', 2, 'ENABLED'),
(@next_data_id + 8, 'dict_anchor_style', '数据型', 'data', 3, 'ENABLED'),
(@next_data_id + 9, 'dict_anchor_style', '情感型', 'emotional', 4, 'ENABLED'),
(@next_data_id + 10, 'dict_anchor_style', '综合分析型', 'comprehensive', 5, 'ENABLED')
ON DUPLICATE KEY UPDATE label = VALUES(label), sort = VALUES(sort), status = VALUES(status);

ALTER TABLE oa_production_content
  ADD COLUMN scheme_type VARCHAR(32) NULL COMMENT '赛事方案类型 dict_scheme_type' AFTER document_type;

CREATE TABLE IF NOT EXISTS oa_ai_content_session (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  session_id VARCHAR(64) NOT NULL COMMENT '前端会话 UUID',
  user_id BIGINT NOT NULL COMMENT '操作用户',
  model_key VARCHAR(32) NULL COMMENT '模型标识 qwen/deepseek/glm/kimi',
  round_count INT NOT NULL DEFAULT 0 COMMENT '对话轮次',
  last_content MEDIUMTEXT NULL COMMENT '最近一次 AI 回复',
  context_json JSON NULL COMMENT '上下文快照',
  creator VARCHAR(64) DEFAULT 'system',
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater VARCHAR(64) DEFAULT 'system',
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted SMALLINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_ai_content_session (tenant_id, session_id),
  KEY idx_ai_content_session_user (tenant_id, user_id)
) COMMENT='AI 内容对话会话';

CREATE TABLE IF NOT EXISTS oa_ai_content_adopt (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  session_id VARCHAR(64) NOT NULL COMMENT '会话 ID',
  user_id BIGINT NOT NULL COMMENT '操作用户',
  content_id BIGINT NULL COMMENT '关联内容 ID',
  model_key VARCHAR(32) NULL COMMENT '模型标识',
  scheme_type VARCHAR(32) NULL COMMENT '方案类型',
  content_length INT NULL COMMENT '采纳正文长度',
  creator VARCHAR(64) DEFAULT 'system',
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater VARCHAR(64) DEFAULT 'system',
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted SMALLINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_ai_content_adopt_session (tenant_id, session_id),
  KEY idx_ai_content_adopt_user (tenant_id, user_id)
) COMMENT='AI 内容方案采纳记录';

CREATE TABLE IF NOT EXISTS oa_ai_content_preference (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  user_id BIGINT NOT NULL COMMENT '用户 ID',
  author_id BIGINT NULL COMMENT '作者维度（可选）',
  summary_text TEXT NULL COMMENT '偏好总结文本',
  dimensions_json JSON NULL COMMENT '结构化偏好',
  source_session_id VARCHAR(64) NULL COMMENT '来源会话',
  is_updated_by_user TINYINT NOT NULL DEFAULT 0 COMMENT '是否用户手动修改',
  creator VARCHAR(64) DEFAULT 'system',
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater VARCHAR(64) DEFAULT 'system',
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted SMALLINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_ai_content_pref_user (tenant_id, user_id, author_id),
  KEY idx_ai_content_pref_tenant (tenant_id)
) COMMENT='AI 内容用户偏好总结';

INSERT INTO oa_ai_prompt_config
  (tenant_id, template_name, version, scene, content_type, prompt_content, variable_desc, temperature, status, remark)
SELECT 1, 'AI内容对话生成', 'v1', 'AI_CONTENT_CHAT', 'ARTICLE',
'你是一位专业的体育自媒体内容编辑，擅长撰写赛事分析方案。

【赛事信息】{{match_name}}
【作者/主播】{{author_name}}
【方案类型】{{scheme_type}}
{{#history_record}}【历史战绩】
{{history_record}}
{{/history_record}}
{{#anchor_style}}【主播风格】
{{anchor_style}}
{{/anchor_style}}
{{#product_description}}【产品定义说明】
{{product_description}}
{{/product_description}}
{{#preference_summary}}【用户偏好总结】
{{preference_summary}}
{{/preference_summary}}

要求：
1. 根据方案类型生成结构清晰的 Markdown 格式赛事方案
2. 语言风格符合主播特点
3. 包含核心推荐、分析要点，必要时使用表格
4. 仅输出方案正文，不要额外解释',
'{{match_name}}=赛事; {{author_name}}=作者; {{scheme_type}}=方案类型; {{history_record}}=历史战绩; {{anchor_style}}=主播风格; {{product_description}}=产品说明; {{preference_summary}}=偏好总结',
0.70, 'ENABLED', 'S-15 AI 内容对话生成系统提示词'
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM oa_ai_prompt_config
  WHERE tenant_id = 1 AND scene = 'AI_CONTENT_CHAT' AND deleted = 0
);

-- 为四模型映射补充 model_id 别名（幂等更新已有记录）
UPDATE oa_ai_model_config SET model_id = 'qwen', is_default = 1
WHERE tenant_id = 1 AND model_type = 'QWEN' AND deleted = 0
  AND (model_id IS NULL OR model_id = '') LIMIT 1;

UPDATE oa_ai_model_config SET model_id = 'glm'
WHERE tenant_id = 1 AND model_type = 'GLM' AND deleted = 0
  AND (model_id IS NULL OR model_id = '') LIMIT 1;

UPDATE oa_ai_model_config SET model_id = 'kimi'
WHERE tenant_id = 1 AND model_type = 'MOONSHOT' AND deleted = 0
  AND (model_id IS NULL OR model_id = '') LIMIT 1;

INSERT INTO oa_ai_model_config
  (tenant_id, model_name, model_id, model_type, api_endpoint, max_tokens, temperature, top_p, status, remark)
SELECT 1, 'DeepSeek-Chat', 'deepseek', 'DEEPSEEK', 'https://api.deepseek.com/v1/chat/completions', 8192, 0.70, 0.90, 'ENABLED', 'DeepSeek 对话模型（S-15）'
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM oa_ai_model_config WHERE tenant_id = 1 AND model_id = 'deepseek' AND deleted = 0
);
