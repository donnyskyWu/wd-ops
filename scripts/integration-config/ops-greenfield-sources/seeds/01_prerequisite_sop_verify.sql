-- =============================================================================
-- Ops DB (shenyu-ops) — 工作任务 SOP 前置验证（只读）
-- 版本: 2026-08-27
-- Target DB: pass on mysql CLI, e.g. mysql -h HOST -u USER -p shenyu-ops < thisfile.sql
-- 期望: 至少 1 行 template + 1 行 content_generation_node
-- =============================================================================
SET NAMES utf8mb4;

SELECT '--- SOP templates (tenant_id=1, deleted=0) ---' AS section;
SELECT id, template_name, status, create_time
FROM oa_sop_template
WHERE deleted = 0 AND tenant_id = 1
ORDER BY id;

SELECT '--- CONTENT_GENERATION nodes ---' AS section;
SELECT n.id AS node_id,
       n.node_name,
       n.node_type,
       n.template_id,
       t.template_name
FROM oa_sop_node n
JOIN oa_sop_template t ON t.id = n.template_id AND t.deleted = 0
WHERE n.node_type = 'CONTENT_GENERATION'
  AND n.deleted = 0
  AND t.tenant_id = 1
ORDER BY n.template_id, n.id;

SELECT '--- sys_param work_task (if already seeded) ---' AS section;
SELECT param_key, param_value, updater, update_time
FROM sys_param
WHERE tenant_id = 1
  AND param_key IN ('work_task.default_template_id', 'work_task.default_node_id')
  AND deleted = 0;

-- 部署提示：将上面查询得到的 template_id / node_id 填入
-- sql/03-shenyu-ops-seeds.sql 占位符：
--   {{WORK_TASK_DEFAULT_TEMPLATE_ID}}
--   {{WORK_TASK_DEFAULT_NODE_ID}}
-- 本地/空库参考：template 9402 / node 9404（V20 seed + V62 node_type=CONTENT_GENERATION）
