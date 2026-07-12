-- Incremental patch: grant layout-template write/import permissions via Football button menus.
-- Root cause: menu 6120 only had oa:layout-template:list; publish/delete/create/update/import APIs need granular oa:* codes.

SET NAMES utf8mb4;

BEGIN;

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
VALUES
  (6170, '公推模板创建', 'oa:layout-template:create', 3, 1, 6120, '', '', '', NULL, 0, b'0', b'1', b'1', 'integration', 2),
  (6171, '公推模板更新', 'oa:layout-template:update', 3, 2, 6120, '', '', '', NULL, 0, b'0', b'1', b'1', 'integration', 2),
  (6172, '公推模板删除', 'oa:layout-template:delete', 3, 3, 6120, '', '', '', NULL, 0, b'0', b'1', b'1', 'integration', 2),
  (6173, '公推模板导入', 'oa:layout-template:import', 3, 4, 6120, '', '', '', NULL, 0, b'0', b'1', b'1', 'integration', 2)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  permission = VALUES(permission),
  type = VALUES(type),
  sort = VALUES(sort),
  parent_id = VALUES(parent_id),
  visible = VALUES(visible),
  deleted = b'0';

-- Super admin (role 1)
INSERT IGNORE INTO system_role_menu (id, role_id, menu_id, creator, tenant_id) VALUES
  (70059, 1, 6170, 'integration', 1),
  (70060, 1, 6171, 'integration', 1),
  (70061, 1, 6172, 'integration', 1),
  (70062, 1, 6173, 'integration', 1);

-- Any role that already has the layout-template page also gets its button permissions
INSERT IGNORE INTO system_role_menu (role_id, menu_id, creator, tenant_id)
SELECT rm.role_id, btn.id, 'integration', rm.tenant_id
FROM system_role_menu rm
INNER JOIN system_menu btn ON btn.id IN (6170, 6171, 6172, 6173) AND btn.deleted = 0
LEFT JOIN system_role_menu existing
  ON existing.role_id = rm.role_id AND existing.menu_id = btn.id AND existing.deleted = 0
WHERE rm.menu_id = 6120
  AND rm.deleted = 0
  AND existing.id IS NULL;

COMMIT;
