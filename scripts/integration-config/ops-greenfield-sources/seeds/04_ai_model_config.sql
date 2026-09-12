-- =============================================================================
-- Ops DB (shenyu-ops) — AI model config production init (ADR-053 / M8)
-- Fixes V43 legacy DashScope native API URL and V139 ADR alias model_id values.
-- Ops must configure api_key_encrypted via M8「AI模型配置」after deploy (no secrets here).
-- Version: 2026-08-26
-- =============================================================================
SET NAMES utf8mb4;

-- Clear default flag before assigning QWEN default (idempotent)
UPDATE oa_ai_model_config SET is_default = 0, updater = 'deploy-seed'
WHERE tenant_id = 1 AND deleted = 0 AND is_default = 1;

-- ----- QWEN (default · DashScope compatible-mode Chat Completions) -----
UPDATE oa_ai_model_config SET
  model_name = 'qwen3.7-max',
  model_id = 'qwen3.7-max',
  api_endpoint = 'https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions',
  max_tokens = COALESCE(max_tokens, 8192),
  temperature = COALESCE(temperature, 0.70),
  top_p = COALESCE(top_p, 0.90),
  is_default = 1,
  status = 'ENABLED',
  remark = '阿里通义千问 · DashScope Chat Completions（greenfield seed）',
  updater = 'deploy-seed'
WHERE tenant_id = 1 AND deleted = 0 AND model_type = 'QWEN'
  AND (
    model_id IN ('qwen') OR model_id IS NULL OR model_id = ''
    OR api_endpoint LIKE '%text-generation/generation%'
    OR (model_id = 'qwen3.7-max' AND api_endpoint NOT LIKE '%compatible-mode%')
  )
ORDER BY is_default DESC, id ASC
LIMIT 1;

INSERT INTO oa_ai_model_config
  (tenant_id, model_name, model_id, model_type, api_endpoint, max_tokens, timeout,
   is_default, conn_status, temperature, top_p, status, remark, creator, updater)
SELECT 1, 'qwen3.7-max', 'qwen3.7-max', 'QWEN',
  'https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions',
  8192, 60, 1, 'DISCONNECTED', 0.70, 0.90, 'ENABLED',
  '阿里通义千问 · DashScope Chat Completions（greenfield seed）', 'deploy-seed', 'deploy-seed'
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM oa_ai_model_config
  WHERE tenant_id = 1 AND deleted = 0 AND model_id = 'qwen3.7-max'
);

UPDATE oa_ai_model_config SET is_default = 1, updater = 'deploy-seed'
WHERE tenant_id = 1 AND deleted = 0 AND model_id = 'qwen3.7-max'
LIMIT 1;

-- ----- DEEPSEEK -----
UPDATE oa_ai_model_config SET
  model_name = 'DeepSeek-Chat',
  model_id = 'deepseek-chat',
  model_type = 'DEEPSEEK',
  api_endpoint = 'https://api.deepseek.com/v1/chat/completions',
  max_tokens = COALESCE(max_tokens, 8192),
  temperature = COALESCE(temperature, 0.70),
  top_p = COALESCE(top_p, 0.90),
  is_default = 0,
  status = 'ENABLED',
  remark = 'DeepSeek 对话模型 · Chat Completions（greenfield seed）',
  updater = 'deploy-seed'
WHERE tenant_id = 1 AND deleted = 0
  AND (model_type = 'DEEPSEEK' OR model_id IN ('deepseek', 'deepseek-chat'))
ORDER BY id ASC
LIMIT 1;

INSERT INTO oa_ai_model_config
  (tenant_id, model_name, model_id, model_type, api_endpoint, max_tokens, timeout,
   is_default, conn_status, temperature, top_p, status, remark, creator, updater)
SELECT 1, 'DeepSeek-Chat', 'deepseek-chat', 'DEEPSEEK',
  'https://api.deepseek.com/v1/chat/completions',
  8192, 60, 0, 'DISCONNECTED', 0.70, 0.90, 'ENABLED',
  'DeepSeek 对话模型 · Chat Completions（greenfield seed）', 'deploy-seed', 'deploy-seed'
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM oa_ai_model_config
  WHERE tenant_id = 1 AND deleted = 0 AND model_id = 'deepseek-chat'
);

-- ----- GLM (智谱) -----
UPDATE oa_ai_model_config SET
  model_name = 'glm-4',
  model_id = 'glm-4',
  model_type = 'GLM',
  api_endpoint = 'https://open.bigmodel.cn/api/paas/v4/chat/completions',
  max_tokens = COALESCE(max_tokens, 8192),
  temperature = COALESCE(temperature, 0.70),
  top_p = COALESCE(top_p, 0.90),
  is_default = 0,
  status = 'ENABLED',
  remark = '智谱 GLM-4 · Chat Completions（greenfield seed）',
  updater = 'deploy-seed'
WHERE tenant_id = 1 AND deleted = 0
  AND (model_type = 'GLM' OR model_id IN ('glm', 'glm-4'))
ORDER BY id ASC
LIMIT 1;

INSERT INTO oa_ai_model_config
  (tenant_id, model_name, model_id, model_type, api_endpoint, max_tokens, timeout,
   is_default, conn_status, temperature, top_p, status, remark, creator, updater)
SELECT 1, 'glm-4', 'glm-4', 'GLM',
  'https://open.bigmodel.cn/api/paas/v4/chat/completions',
  8192, 60, 0, 'DISCONNECTED', 0.70, 0.90, 'ENABLED',
  '智谱 GLM-4 · Chat Completions（greenfield seed）', 'deploy-seed', 'deploy-seed'
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM oa_ai_model_config
  WHERE tenant_id = 1 AND deleted = 0 AND model_id = 'glm-4'
);

-- ----- MOONSHOT / Kimi -----
UPDATE oa_ai_model_config SET
  model_name = 'moonshot-v1-32k',
  model_id = 'moonshot-v1-32k',
  model_type = 'MOONSHOT',
  api_endpoint = 'https://api.moonshot.cn/v1/chat/completions',
  max_tokens = COALESCE(max_tokens, 32768),
  temperature = COALESCE(temperature, 0.60),
  top_p = COALESCE(top_p, 0.90),
  is_default = 0,
  status = 'ENABLED',
  remark = '月之暗面 Moonshot · Chat Completions（greenfield seed）',
  updater = 'deploy-seed'
WHERE tenant_id = 1 AND deleted = 0
  AND (model_type IN ('MOONSHOT', 'KIMI') OR model_id IN ('kimi', 'moonshot-v1-32k'))
ORDER BY id ASC
LIMIT 1;

INSERT INTO oa_ai_model_config
  (tenant_id, model_name, model_id, model_type, api_endpoint, max_tokens, timeout,
   is_default, conn_status, temperature, top_p, status, remark, creator, updater)
SELECT 1, 'moonshot-v1-32k', 'moonshot-v1-32k', 'MOONSHOT',
  'https://api.moonshot.cn/v1/chat/completions',
  32768, 60, 0, 'DISCONNECTED', 0.60, 0.90, 'ENABLED',
  '月之暗面 Moonshot · Chat Completions（greenfield seed）', 'deploy-seed', 'deploy-seed'
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM oa_ai_model_config
  WHERE tenant_id = 1 AND deleted = 0 AND model_id = 'moonshot-v1-32k'
);
