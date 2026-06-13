-- Requirements 91/92/93: AI model types, IP member position dict, AI prompt content/document type

-- 91: Expand AI model type dictionary (domestic + international)
INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_ai_model_type', '通义千问', 'QWEN', 1, 'ENABLED'),
('dict_ai_model_type', '文心一言', 'ERNIE', 2, 'ENABLED'),
('dict_ai_model_type', '智谱 AI', 'GLM', 3, 'ENABLED'),
('dict_ai_model_type', 'DeepSeek', 'DEEPSEEK', 4, 'ENABLED'),
('dict_ai_model_type', 'Kimi', 'KIMI', 5, 'ENABLED'),
('dict_ai_model_type', '豆包', 'DOUBAO', 6, 'ENABLED'),
('dict_ai_model_type', 'OpenAI GPT', 'GPT', 7, 'ENABLED'),
('dict_ai_model_type', 'Claude', 'CLAUDE', 8, 'ENABLED'),
('dict_ai_model_type', 'Gemini', 'GEMINI', 9, 'ENABLED'),
('dict_ai_model_type', '月之暗面', 'MOONSHOT', 10, 'ENABLED')
ON DUPLICATE KEY UPDATE label = VALUES(label), sort = VALUES(sort);

-- 92: IP 组成员岗位（与 GLOBAL-CONVENTIONS dict_position 对齐）
INSERT INTO sys_dict_type (type, name, status) VALUES
('dict_position', '岗位', 'ENABLED')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_position', '运营', 'OPERATOR', 1, 'ENABLED'),
('dict_position', '编辑', 'EDITOR', 2, 'ENABLED'),
('dict_position', '主播', 'ANCHOR', 3, 'ENABLED'),
('dict_position', '销售', 'SALES', 4, 'ENABLED'),
('dict_position', '运营组长', 'OPS_LEADER', 5, 'ENABLED'),
('dict_position', '直播运营', 'LIVE_OPERATOR', 6, 'ENABLED')
ON DUPLICATE KEY UPDATE label = VALUES(label), sort = VALUES(sort);

-- 93: AI 提示词关联内容类型 / 文档类型
ALTER TABLE oa_ai_prompt_config ADD COLUMN content_type VARCHAR(64) NULL;
ALTER TABLE oa_ai_prompt_config ADD COLUMN document_type VARCHAR(64) NULL;
