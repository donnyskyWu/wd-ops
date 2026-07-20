-- Incremental patch: grant smoke-probe OA permissions to admin role.
-- Root cause: system_menu rows exist (6137/6149) but system_role_menu missing for role_id=1
-- after partial seed or TRUNCATE — author (6155) may remain while account/log/dict 403.
-- API smoke probes: wechat_account_list, dict_list (POST-MDB §23 #2; login/operation log removed AL-04/AL-11).

SET NAMES utf8mb4;

BEGIN;

-- Ensure page menus exist (idempotent; no DELETE)
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
VALUES
  (6137, '字典配置', 'oa:dict:admin-list', 2, 1, 6105, 'system-dict', 'ep:document', 'ops/system/DictManage', 'SystemDict', 0, b'1', b'1', b'1', 'integration', 2),
  (6149, '平台账号管理', 'oa:platform-account:list', 2, 2, 6108, 'internal-account', 'ep:document', 'ops/internal/InternalAccountManage', 'InternalAccount', 0, b'1', b'1', b'1', 'integration', 2)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  permission = VALUES(permission),
  type = VALUES(type),
  sort = VALUES(sort),
  parent_id = VALUES(parent_id),
  visible = VALUES(visible),
  deleted = b'0';

-- Hidden button alias: oa:account:list (PlatformAccountController hasAnyAuthority fallback)
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
VALUES
  (6174, '平台账号查询', 'oa:account:list', 3, 1, 6149, '', '', '', NULL, 0, b'0', b'1', b'1', 'integration', 2)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  permission = VALUES(permission),
  type = VALUES(type),
  parent_id = VALUES(parent_id),
  visible = VALUES(visible),
  deleted = b'0';

-- Super admin (role 1)
INSERT IGNORE INTO system_role_menu (id, role_id, menu_id, creator, tenant_id) VALUES
  (70027, 1, 6137, 'integration', 1),
  (70039, 1, 6149, 'integration', 1),
  (70063, 1, 6174, 'integration', 1);

-- Propagate to roles that already have parent directory menus
INSERT IGNORE INTO system_role_menu (role_id, menu_id, creator, tenant_id)
SELECT rm.role_id, m.id, 'integration', rm.tenant_id
FROM system_role_menu rm
INNER JOIN system_menu m ON m.id IN (6137, 6149, 6174) AND m.deleted = 0
LEFT JOIN system_role_menu existing
  ON existing.role_id = rm.role_id AND existing.menu_id = m.id AND existing.deleted = 0
WHERE rm.menu_id IN (6105, 6108, 6149)
  AND rm.deleted = 0
  AND existing.id IS NULL;

COMMIT;
