-- =============================================================================
-- shenyu-ops — business seeds (work task AI prompt + sys_param)
-- Generated: 2026-08-25 by gen-ops-greenfield-sql.py — do not hand-edit
--
-- *** BEFORE RUNNING ***
-- 1. Run prerequisite verify queries (see pointer below) or OPERATIONS-GUIDE.md Step 4
-- 2. Edit {{WORK_TASK_DEFAULT_TEMPLATE_ID}} / {{WORK_TASK_DEFAULT_NODE_ID}}
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
