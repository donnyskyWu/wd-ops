-- DM-02/DM-03: Remove OPS duplicate 字典配置 menu (6137) and permissions oa:dict:* (id 32–35).
-- SSOT = Football system/dict (menu 105; permissions system:dict:query/create/update/delete @ 1026–1029).
-- OPS-DICT-MERGE-FOOTBALL-PLAN §1.2 / §4 DM-02 · V148 = data merge only.

-- Grant Football dict chain to roles that had OPS menu 6137 (before removal).
-- Menu ids: 105 字典管理, 1026–1029 system:dict:query/create/update/delete
INSERT INTO system_role_menu (role_id, menu_id, creator, tenant_id)
SELECT DISTINCT rm.role_id, fm.id, 'flyway', rm.tenant_id
FROM system_role_menu rm
INNER JOIN system_menu fm ON fm.id IN (105, 1026, 1027, 1028, 1029) AND fm.deleted = b'0'
WHERE rm.menu_id = 6137
  AND rm.deleted = b'0'
  AND NOT EXISTS (
    SELECT 1 FROM system_role_menu ex
    WHERE ex.role_id = rm.role_id
      AND ex.menu_id = fm.id
      AND ex.tenant_id = rm.tenant_id
      AND ex.deleted = b'0'
  );

UPDATE system_menu
SET deleted = b'1',
    visible = b'0',
    updater = 'flyway',
    update_time = NOW()
WHERE id = 6137;

DELETE FROM system_role_menu WHERE menu_id = 6137;

UPDATE sys_permission
SET deleted = 1,
    updater = 'flyway',
    update_time = NOW()
WHERE id IN (32, 33, 34, 35)
  AND code IN (
    'oa:dict:admin-list',
    'oa:dict:create',
    'oa:dict:update',
    'oa:dict:delete'
  );

DELETE FROM sys_role_permission WHERE permission_id IN (32, 33, 34, 35);
