-- =============================================================================
-- Ops DB (shenyu-ops) — sys_param work_task defaults (V181 insert + V182 backfill)
-- Source: V181 §4 + V182__m2_work_task_default_params.sql
-- Version: 2026-08-19
-- IMPORTANT: Verify template_id/node_id exist in prod before confirm flow (see README).
-- Seed reference: template 9402 / node 9404 (V20+V62 SEED SOP CONTENT_GENERATION)
-- =============================================================================
SET NAMES utf8mb4;
USE `{{OPS_DB_NAME}}`;

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
