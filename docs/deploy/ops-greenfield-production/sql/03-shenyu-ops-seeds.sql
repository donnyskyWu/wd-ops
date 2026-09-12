-- =============================================================================
-- shenyu-ops — business seeds (AI model + AI prompt + sys_param catalog + work-task SOP params)
-- Generated: 2026-08-28 by gen-ops-greenfield-sql.py — do not hand-edit
--
-- *** BEFORE RUNNING ***
-- 1. Run prerequisite verify queries (see pointer below) or OPERATIONS-GUIDE.md Step 4
-- 2. Edit {{WORK_TASK_DEFAULT_TEMPLATE_ID}} / {{WORK_TASK_DEFAULT_NODE_ID}}
-- 3. After seed: configure M8 AI model API keys via Admin UI (sql does not contain secrets)
--
-- Target DB: pass on mysql CLI, e.g. mysql -h HOST -u USER -p shenyu-ops < sql/03-shenyu-ops-seeds.sql
-- =============================================================================
SET NAMES utf8mb4;


-- =============================================================================
-- ===== 01_prerequisite_sop_verify.sql (reference only — not embedded) =====
-- Run verification SELECTs from source file before seed writes
-- =============================================================================

-- Prerequisite verify SQL:
--   scripts/integration-config/ops-greenfield-sources/seeds/01_prerequisite_sop_verify.sql
-- See OPERATIONS-GUIDE.md Step 4 for execution order.


-- =============================================================================
-- ===== 04_ai_model_config.sql =====
-- M8 AI model config — DashScope Chat Completions vendor slugs (ADR-053)
-- =============================================================================

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

-- =============================================================================
-- ===== 02_ai_prompt_work_task.sql =====
-- WORK_TASK_WIN_PREDICTION AI prompt (V181 §3)
-- =============================================================================

INSERT INTO oa_ai_prompt_config
  (tenant_id, template_name, version, scene, content_type, prompt_content, variable_desc, temperature, status, remark)
SELECT 1,
  '工作任务红黑预测抽取', 'v1', 'WORK_TASK_WIN_PREDICTION', 'ARTICLE',
'你是一位专业的足球赛果分析助手。请从以下任务正文中**抽取且仅抽取一条**全场胜负预测 outcome。

【赛事】{{match_name}}（competition_id={{competition_id}}）
【正文】
{{content_body}}

输出要求：
1. 仅输出一个 outcome 枚举值：HOME_WIN（主胜）/ DRAW（平局）/ AWAY_WIN（客胜）
2. 若正文无法判断明确单场预测，输出 UNKNOWN
3. 不要输出解释、标点或其他文字',
'{{match_name}}=赛事名称; {{competition_id}}=赛事ID; {{content_body}}=任务关联正文',
0.20, 'ENABLED', 'FR-M2-010 S-16 · ADR-072 赛后 Job 抽取预测'
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM oa_ai_prompt_config
  WHERE tenant_id = 1 AND scene = 'WORK_TASK_WIN_PREDICTION' AND deleted = 0
);

-- =============================================================================
-- ===== 05_sys_param.sql =====
-- M8/M9/M10 sys_param catalog (V52/V74/V167/V169/V170/V175/V177)
-- =============================================================================

INSERT INTO sys_param (tenant_id, param_name, param_key, param_value, param_type, category, remark, creator, updater)
SELECT 1, '数据采集间隔（秒）', 'collect.interval.seconds', '3600', 'NUMBER', 'COLLECT',
       '定时采集任务的时间间隔，单位：秒', 'deploy-seed', 'deploy-seed'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE tenant_id = 1 AND param_key = 'collect.interval.seconds' AND deleted = 0);

INSERT INTO sys_param (tenant_id, param_name, param_key, param_value, param_type, category, remark, creator, updater)
SELECT 1, '最大并发采集数', 'collect.max.concurrency', '10', 'NUMBER', 'COLLECT',
       '同时进行的采集任务最大数量', 'deploy-seed', 'deploy-seed'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE tenant_id = 1 AND param_key = 'collect.max.concurrency' AND deleted = 0);

INSERT INTO sys_param (tenant_id, param_name, param_key, param_value, param_type, category, remark, creator, updater)
SELECT 1, 'AI生成内容审核开关', 'ai.content.review.enabled', 'true', 'BOOLEAN', 'AI',
       '是否启用AI生成内容的自动审核流程', 'deploy-seed', 'deploy-seed'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE tenant_id = 1 AND param_key = 'ai.content.review.enabled' AND deleted = 0);

INSERT INTO sys_param (tenant_id, param_name, param_key, param_value, param_type, category, remark, creator, updater)
SELECT 1, 'AI内容对话走竞彩Author', 'ai.content.chat-via-jingcai', 'true', 'BOOLEAN', 'AI',
       'true=AI内容对话抽屉走 jingcai.article 异步任务；false=直连 M8 模型 Chat Completions（需 football.ai.scheme-generate-* 仍用于其他场景）', 'deploy-seed', 'deploy-seed'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE tenant_id = 1 AND param_key = 'ai.content.chat-via-jingcai' AND deleted = 0);

INSERT INTO sys_param (tenant_id, param_name, param_key, param_value, param_type, category, remark, creator, updater)
SELECT 1, '默认AI模型', 'ai.default.model', 'QWEN', 'STRING', 'AI',
       '系统默认使用的AI模型类型', 'deploy-seed', 'deploy-seed'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE tenant_id = 1 AND param_key = 'ai.default.model' AND deleted = 0);

INSERT INTO sys_param (tenant_id, param_name, param_key, param_value, param_type, category, remark, creator, updater)
SELECT 1, '数据保留天数', 'data.retention.days', '365', 'NUMBER', 'BASIC',
       '历史数据保留的天数，超过自动清理', 'deploy-seed', 'deploy-seed'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE tenant_id = 1 AND param_key = 'data.retention.days' AND deleted = 0);

INSERT INTO sys_param (tenant_id, param_name, param_key, param_value, param_type, category, remark, creator, updater)
SELECT 1, 'API请求超时时间（毫秒）', 'api.timeout.milliseconds', '30000', 'NUMBER', 'BASIC',
       '外部API请求的超时时间', 'deploy-seed', 'deploy-seed'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE tenant_id = 1 AND param_key = 'api.timeout.milliseconds' AND deleted = 0);

-- ----- V74 content review + V169 ADR-064 role codes -----
INSERT INTO sys_param (tenant_id, param_name, param_key, param_value, param_type, category, remark, creator, updater)
SELECT 1, '开启一级审核', 'content.review.level1.enabled', 'true', 'BOOLEAN', 'CONTENT_REVIEW',
       '关闭后提交审核将跳过一级审核', 'deploy-seed', 'deploy-seed'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE tenant_id = 1 AND param_key = 'content.review.level1.enabled' AND deleted = 0);

INSERT INTO sys_param (tenant_id, param_name, param_key, param_value, param_type, category, remark, creator, updater)
SELECT 1, '开启二级审核', 'content.review.level2.enabled', 'true', 'BOOLEAN', 'CONTENT_REVIEW',
       '关闭后一级通过后直接发布；两级均关闭则提交后直接发布', 'deploy-seed', 'deploy-seed'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE tenant_id = 1 AND param_key = 'content.review.level2.enabled' AND deleted = 0);

INSERT INTO sys_param (tenant_id, param_name, param_key, param_value, param_type, category, remark, creator, updater)
SELECT 1, '一级审核角色', 'content.review.level1.role', 'ip_group_leader', 'STRING', 'CONTENT_REVIEW',
       '一级审核角色；ip_group_leader=内容所属IP组组长范围（ADR-064）', 'deploy-seed', 'deploy-seed'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE tenant_id = 1 AND param_key = 'content.review.level1.role' AND deleted = 0);

INSERT INTO sys_param (tenant_id, param_name, param_key, param_value, param_type, category, remark, creator, updater)
SELECT 1, '二级审核角色', 'content.review.level2.role', 'ops_manager', 'STRING', 'CONTENT_REVIEW',
       '二级审核角色；ops_manager（ADR-064）', 'deploy-seed', 'deploy-seed'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE tenant_id = 1 AND param_key = 'content.review.level2.role' AND deleted = 0);

-- ----- V167 -----
INSERT INTO sys_param (tenant_id, param_name, param_key, param_value, param_type, category, remark, creator, updater)
SELECT 1, '统一采集调度Cron', 'collect.schedule.cron', '0 0 23 * * ?', 'STRING', 'COLLECT',
       'ADR-061 租户统一采集任务默认每日 23:00', 'deploy-seed', 'deploy-seed'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE tenant_id = 1 AND param_key = 'collect.schedule.cron' AND deleted = 0);

-- ----- V170 DingTalk / notification (empty secrets) -----
INSERT INTO sys_param (tenant_id, param_name, param_key, param_value, param_type, category, remark, creator, updater)
SELECT 1, '钉钉集成启用', 'dingtalk.enabled', 'false', 'BOOLEAN', 'DINGTALK',
       'ADR-026 工作通知主通道；true 启用 asyncsend_v2', 'deploy-seed', 'deploy-seed'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE tenant_id = 1 AND param_key = 'dingtalk.enabled' AND deleted = 0);

INSERT INTO sys_param (tenant_id, param_name, param_key, param_value, param_type, category, remark, creator, updater)
SELECT 1, '钉钉 AppKey', 'dingtalk.client-id', '', 'STRING', 'DINGTALK',
       '企业内部应用 ClientId / AppKey（部署后在 M8 填写，勿写入 SQL）', 'deploy-seed', 'deploy-seed'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE tenant_id = 1 AND param_key = 'dingtalk.client-id' AND deleted = 0);

INSERT INTO sys_param (tenant_id, param_name, param_key, param_value, param_type, category, remark, creator, updater)
SELECT 1, '钉钉 AppSecret', 'dingtalk.client-secret', '', 'STRING', 'DINGTALK',
       '企业内部应用 ClientSecret（部署后在 M8 填写，勿写入 SQL）', 'deploy-seed', 'deploy-seed'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE tenant_id = 1 AND param_key = 'dingtalk.client-secret' AND deleted = 0);

INSERT INTO sys_param (tenant_id, param_name, param_key, param_value, param_type, category, remark, creator, updater)
SELECT 1, '钉钉 CorpId', 'dingtalk.corp-id', '', 'STRING', 'DINGTALK',
       '企业 ID（可选归档）', 'deploy-seed', 'deploy-seed'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE tenant_id = 1 AND param_key = 'dingtalk.corp-id' AND deleted = 0);

INSERT INTO sys_param (tenant_id, param_name, param_key, param_value, param_type, category, remark, creator, updater)
SELECT 1, '钉钉 AgentId', 'dingtalk.agent-id', '', 'STRING', 'DINGTALK',
       '工作通知微应用 AgentId', 'deploy-seed', 'deploy-seed'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE tenant_id = 1 AND param_key = 'dingtalk.agent-id' AND deleted = 0);

INSERT INTO sys_param (tenant_id, param_name, param_key, param_value, param_type, category, remark, creator, updater)
SELECT 1, '钉钉机器人启用', 'dingtalk.robot.enabled', 'false', 'BOOLEAN', 'DINGTALK',
       '工作通知失败时的可选 Webhook 降级', 'deploy-seed', 'deploy-seed'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE tenant_id = 1 AND param_key = 'dingtalk.robot.enabled' AND deleted = 0);

INSERT INTO sys_param (tenant_id, param_name, param_key, param_value, param_type, category, remark, creator, updater)
SELECT 1, '钉钉机器人 Webhook', 'dingtalk.robot.webhook-url', '', 'STRING', 'DINGTALK',
       '自定义机器人 Webhook URL（含 access_token；部署后填写）', 'deploy-seed', 'deploy-seed'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE tenant_id = 1 AND param_key = 'dingtalk.robot.webhook-url' AND deleted = 0);

INSERT INTO sys_param (tenant_id, param_name, param_key, param_value, param_type, category, remark, creator, updater)
SELECT 1, '钉钉机器人加签', 'dingtalk.robot.secret', '', 'STRING', 'DINGTALK',
       'Webhook 加签 SEC 密钥（可选；部署后填写）', 'deploy-seed', 'deploy-seed'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE tenant_id = 1 AND param_key = 'dingtalk.robot.secret' AND deleted = 0);

INSERT INTO sys_param (tenant_id, param_name, param_key, param_value, param_type, category, remark, creator, updater)
SELECT 1, '通知平台根 URL', 'notification.platform-base-url', '', 'STRING', 'NOTIFICATION',
       '钉钉消息跳转链接前缀（如 https://ops.example.com/ops）', 'deploy-seed', 'deploy-seed'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE tenant_id = 1 AND param_key = 'notification.platform-base-url' AND deleted = 0);

-- ----- V175 / V177 collector placeholders -----
INSERT INTO sys_param (tenant_id, param_name, param_key, param_value, param_type, category, remark, creator, updater)
SELECT 1, '外部统一采集调度Cron', 'collect.external.unified.cron', '0 0 22 * * ?', 'STRING', 'COLLECT',
       'ADR-068 统一外部数据采集任务默认每日 22:00', 'deploy-seed', 'deploy-seed'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE tenant_id = 1 AND param_key = 'collect.external.unified.cron' AND deleted = 0);

INSERT INTO sys_param (tenant_id, param_name, param_key, param_value, param_type, category, remark, creator, updater)
SELECT 1, '外部公众号采集 Cookie', 'collect.external.wechat_official.cookie', '', 'STRING', 'COLLECT',
       'mp.weixin.qq.com 运营后台 Session Cookie（部署后在 M8 填写，勿写入 SQL）', 'deploy-seed', 'deploy-seed'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE tenant_id = 1 AND param_key = 'collect.external.wechat_official.cookie' AND deleted = 0);

-- =============================================================================
-- ===== 03_sys_param_work_task.sql =====
-- work_task.default_template_id / default_node_id (V181 §4 + V182)
-- =============================================================================

-- IMPORTANT: Replace {{WORK_TASK_DEFAULT_TEMPLATE_ID}} and {{WORK_TASK_DEFAULT_NODE_ID}} before executing.

INSERT INTO sys_param (tenant_id, param_name, param_key, param_value, param_type, category, remark, creator, updater)
SELECT 1, '工作任务默认 SOP 模板 ID', 'work_task.default_template_id', '', 'STRING', 'WORK_TASK',
       '确认登记生成 oa_task 时 template_id；节点须为 CONTENT_GENERATION', 'deploy-v181', 'deploy-v181'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE tenant_id = 1 AND param_key = 'work_task.default_template_id' AND deleted = 0);

INSERT INTO sys_param (tenant_id, param_name, param_key, param_value, param_type, category, remark, creator, updater)
SELECT 1, '工作任务默认 SOP 节点 ID', 'work_task.default_node_id', '', 'STRING', 'WORK_TASK',
       '确认登记生成 oa_task 时 node_id；类型须 CONTENT_GENERATION', 'deploy-v181', 'deploy-v181'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE tenant_id = 1 AND param_key = 'work_task.default_node_id' AND deleted = 0);

-- V182: backfill when empty — replace 9402/9404 with prod-valid IDs if different
UPDATE sys_param
SET param_value = '{{WORK_TASK_DEFAULT_TEMPLATE_ID}}', updater = 'deploy-v182'
WHERE tenant_id = 1 AND param_key = 'work_task.default_template_id' AND deleted = 0
  AND (param_value IS NULL OR param_value = '');

UPDATE sys_param
SET param_value = '{{WORK_TASK_DEFAULT_NODE_ID}}', updater = 'deploy-v182'
WHERE tenant_id = 1 AND param_key = 'work_task.default_node_id' AND deleted = 0
  AND (param_value IS NULL OR param_value = '');

-- Idempotent insert-with-value fallback (patch_v182 pattern)
INSERT INTO sys_param (tenant_id, param_name, param_key, param_value, param_type, category, remark, creator, updater)
SELECT 1, '工作任务默认 SOP 模板 ID', 'work_task.default_template_id', '{{WORK_TASK_DEFAULT_TEMPLATE_ID}}', 'STRING', 'WORK_TASK', 'S-17 deploy patch', 'deploy', 'deploy'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE tenant_id = 1 AND param_key = 'work_task.default_template_id' AND deleted = 0);

INSERT INTO sys_param (tenant_id, param_name, param_key, param_value, param_type, category, remark, creator, updater)
SELECT 1, '工作任务默认 SOP 节点 ID', 'work_task.default_node_id', '{{WORK_TASK_DEFAULT_NODE_ID}}', 'STRING', 'WORK_TASK', 'S-17 deploy patch', 'deploy', 'deploy'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE tenant_id = 1 AND param_key = 'work_task.default_node_id' AND deleted = 0);
