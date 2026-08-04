-- Menu/role fixes for remote test shenyu-system (run as shenyu-system user).
-- Dict merge: scripts/integration-config/seed-ops-test-remote-dict.py (separate credentials).

SET NAMES utf8mb4;

UPDATE system_menu
SET name = '我的任务',
    updater = 'ops-test-seed',
    update_time = NOW()
WHERE id = 6124
  AND deleted = b'0';

INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, user_type
)
SELECT
    6175, '全部任务', 'ops:task:list', 2, 9, 6102, 'task/all', 'ep:document',
    'ops/production/task/all', 'TaskAll', 0, b'1', b'1', b'1', 'ops-test-seed', 2
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6175);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70064, 1, 6175, 'ops-test-seed', 1, 2
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70064);

UPDATE system_menu SET path = 'log', updater = 'ops-test-seed', update_time = NOW()
WHERE id = 6133 AND parent_id = 6104 AND path IN ('collect/log', 'log') AND deleted = b'0';

-- M10 Phase 2 OOS: hide 数据质量 + 私域桥接
DELETE FROM system_role_menu WHERE menu_id IN (6134, 6135)
   OR menu_id IN (SELECT id FROM system_menu WHERE parent_id IN (6134, 6135));
DELETE FROM system_menu WHERE parent_id IN (6134, 6135);
DELETE FROM system_menu WHERE id IN (6134, 6135);

UPDATE system_menu SET path = 'task', updater = 'ops-test-seed', update_time = NOW()
WHERE id = 6136 AND parent_id = 6104 AND path IN ('collect/task', 'task') AND deleted = b'0';

INSERT INTO system_role (
    id, name, code, sort, data_scope, data_scope_dept_ids, status, type, remark,
    creator, create_time, updater, update_time, deleted, tenant_id
)
SELECT
    160, 'IP组长', 'ip_group_leader', 20, 5, '', 0, 1,
    'OPS 内置：IP 组组长候选人',
    'ops-test-seed', NOW(), 'ops-test-seed', NOW(), b'0', 1
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_role r
    WHERE r.code = 'ip_group_leader' AND r.tenant_id = 1 AND r.deleted = b'0'
);

-- Repair prior PowerShell-pipe seed: Chinese became literal '?' (HEX 3F)
UPDATE system_role
SET name = 'IP组长',
    remark = 'OPS 内置：IP 组组长候选人',
    updater = 'ops-test-seed',
    update_time = NOW()
WHERE code = 'ip_group_leader'
  AND tenant_id = 1
  AND deleted = b'0'
  AND (HEX(name) LIKE '%3F%' OR name LIKE '%?%' OR name <> 'IP组长');

DELETE FROM system_role_menu WHERE menu_id IN (6137, 6138, 6139, 6155);
DELETE FROM system_menu WHERE id IN (6137, 6138, 6139, 6155);
