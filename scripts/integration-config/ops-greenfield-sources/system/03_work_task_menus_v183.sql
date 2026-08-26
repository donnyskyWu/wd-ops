-- =============================================================================
-- System DB (shenyu-system) — V183 menus + role_menu for FR-M2-010
-- Source: V183__m2_work_task_menu_dict_fix.sql (§1)
-- Version: 2026-08-19 · Apply on: {{SYSTEM_DB_HOST}}/{{SYSTEM_DB_NAME}}
-- Menu IDs: 6194 (page) · 6195 (register) · 6196 (matrix)
-- NOTE: Do NOT apply V181 menu IDs 6176–6178 (beta collision); use 6194–6196 SSOT.
-- =============================================================================
SET NAMES utf8mb4;

-- Parent: 6102 内容生产

INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator
)
SELECT
    6194, '工作任务管理', 'ops:work-task:list', 2, 10, 6102, 'work-task', 'ep:calendar',
    'ops/production/work-task/index', 'WorkTask', 0, b'1', b'1', b'1', 'deploy-v183'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6194)
  AND NOT EXISTS (
      SELECT 1 FROM system_menu m
      WHERE m.permission = 'ops:work-task:list' AND m.deleted = b'0'
  );

INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator
)
SELECT
    6195, '工作任务登记', 'ops:work-task:register', 3, 1,
    (SELECT id FROM system_menu WHERE permission = 'ops:work-task:list' AND deleted = b'0' LIMIT 1),
    '', '', '', NULL,
    0, b'1', b'1', b'1', 'deploy-v183'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6195)
  AND NOT EXISTS (
      SELECT 1 FROM system_menu m
      WHERE m.permission = 'ops:work-task:register' AND m.deleted = b'0'
  );

INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator
)
SELECT
    6196, '工作任务管理矩阵', 'ops:work-task:manage', 3, 2,
    (SELECT id FROM system_menu WHERE permission = 'ops:work-task:list' AND deleted = b'0' LIMIT 1),
    '', '', '', NULL,
    0, b'1', b'1', b'1', 'deploy-v183'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6196)
  AND NOT EXISTS (
      SELECT 1 FROM system_menu m
      WHERE m.permission = 'ops:work-task:manage' AND m.deleted = b'0'
  );

-- Admin role (role_id=1)
INSERT INTO system_role_menu (role_id, menu_id, creator, tenant_id)
SELECT 1, m.id, 'deploy-v183', 1
FROM system_menu m
WHERE m.permission = 'ops:work-task:list' AND m.deleted = b'0'
  AND NOT EXISTS (
      SELECT 1 FROM system_role_menu rm
      WHERE rm.role_id = 1 AND rm.menu_id = m.id AND rm.deleted = b'0'
  )
LIMIT 1;

INSERT INTO system_role_menu (role_id, menu_id, creator, tenant_id)
SELECT 1, m.id, 'deploy-v183', 1
FROM system_menu m
WHERE m.permission = 'ops:work-task:register' AND m.deleted = b'0'
  AND NOT EXISTS (
      SELECT 1 FROM system_role_menu rm
      WHERE rm.role_id = 1 AND rm.menu_id = m.id AND rm.deleted = b'0'
  )
LIMIT 1;

INSERT INTO system_role_menu (role_id, menu_id, creator, tenant_id)
SELECT 1, m.id, 'deploy-v183', 1
FROM system_menu m
WHERE m.permission = 'ops:work-task:manage' AND m.deleted = b'0'
  AND NOT EXISTS (
      SELECT 1 FROM system_role_menu rm
      WHERE rm.role_id = 1 AND rm.menu_id = m.id AND rm.deleted = b'0'
  )
LIMIT 1;

-- IP组长 role (code=ip_group_leader)
INSERT INTO system_role_menu (role_id, menu_id, creator, tenant_id)
SELECT r.id, m.id, 'deploy-v183', 1
FROM system_role r
JOIN system_menu m
  ON m.permission = 'ops:work-task:list' AND m.deleted = b'0'
WHERE r.code = 'ip_group_leader' AND r.tenant_id = 1 AND r.deleted = b'0'
  AND NOT EXISTS (
      SELECT 1 FROM system_role_menu rm
      WHERE rm.role_id = r.id AND rm.menu_id = m.id AND rm.deleted = b'0'
  )
LIMIT 1;

INSERT INTO system_role_menu (role_id, menu_id, creator, tenant_id)
SELECT r.id, m.id, 'deploy-v183', 1
FROM system_role r
JOIN system_menu m
  ON m.permission = 'ops:work-task:register' AND m.deleted = b'0'
WHERE r.code = 'ip_group_leader' AND r.tenant_id = 1 AND r.deleted = b'0'
  AND NOT EXISTS (
      SELECT 1 FROM system_role_menu rm
      WHERE rm.role_id = r.id AND rm.menu_id = m.id AND rm.deleted = b'0'
  )
LIMIT 1;

INSERT INTO system_role_menu (role_id, menu_id, creator, tenant_id)
SELECT r.id, m.id, 'deploy-v183', 1
FROM system_role r
JOIN system_menu m
  ON m.permission = 'ops:work-task:manage' AND m.deleted = b'0'
WHERE r.code = 'ip_group_leader' AND r.tenant_id = 1 AND r.deleted = b'0'
  AND NOT EXISTS (
      SELECT 1 FROM system_role_menu rm
      WHERE rm.role_id = r.id AND rm.menu_id = m.id AND rm.deleted = b'0'
  )
LIMIT 1;
