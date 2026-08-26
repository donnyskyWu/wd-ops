-- =============================================================================
-- System DB ({{SYSTEM_DB_NAME}}) — 菜单补丁（6175 全部任务 / 路径修正 / 移除 OOS 菜单）
-- 来源: scripts/integration-config/seed-ops-test-remote-shenyu-system-menus.sql
-- 版本: 2026-08-25 · Greenfield prod · 幂等
-- 目标: {{SYSTEM_DB_HOST}}/{{SYSTEM_DB_NAME}}
-- 前置: 000_baseline_ops_menus.sql
-- =============================================================================
SET NAMES utf8mb4;

UPDATE system_menu
SET name = '我的任务',
    updater = 'ops-prod-deploy',
    update_time = NOW()
WHERE id = 6124
  AND deleted = b'0';

INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, user_type
)
SELECT
    6175, '全部任务', 'ops:task:list', 2, 9, 6102, 'task/all', 'ep:document',
    'ops/production/task/all', 'TaskAll', 0, b'1', b'1', b'1', 'ops-prod-deploy', 2
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6175);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70064, 1, 6175, 'ops-prod-deploy', 1, 2
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70064);

UPDATE system_menu SET path = 'log', updater = 'ops-prod-deploy', update_time = NOW()
WHERE id = 6133 AND parent_id = 6104 AND path IN ('collect/log', 'log') AND deleted = b'0';

-- M10 Phase 2 OOS: hide 数据质量 + 私域桥接
DELETE FROM system_role_menu WHERE menu_id IN (6134, 6135)
   OR menu_id IN (SELECT id FROM system_menu WHERE parent_id IN (6134, 6135));
DELETE FROM system_menu WHERE parent_id IN (6134, 6135);
DELETE FROM system_menu WHERE id IN (6134, 6135);

UPDATE system_menu SET path = 'task', updater = 'ops-prod-deploy', update_time = NOW()
WHERE id = 6136 AND parent_id = 6104 AND path IN ('collect/task', 'task') AND deleted = b'0';

-- 移除已迁移 Football 的冗余菜单（字典/日志/作者）
DELETE FROM system_role_menu WHERE menu_id IN (6137, 6138, 6139, 6155);
DELETE FROM system_menu WHERE id IN (6137, 6138, 6139, 6155);
