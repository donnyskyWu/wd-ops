-- AL-11: Remove OPS duplicate 操作日志 menu (6139) and permission oa:log:operation (id 36).
-- SSOT = Football system/operatelog (menu 500; permission system:operate-log:query @ menu 1040).
-- OPS-AUDIT-LOG-MIGRATION-PLAN §1.2 / AL-11.

-- Grant Football audit-log chain to roles that had OPS menu 6139 (before removal).
-- Menu ids: 108 审计日志, 500 操作日志 page, 1040 system:operate-log:query
INSERT INTO system_role_menu (role_id, menu_id, creator, tenant_id)
SELECT DISTINCT rm.role_id, fm.id, 'flyway', rm.tenant_id
FROM system_role_menu rm
INNER JOIN system_menu fm ON fm.id IN (108, 500, 1040) AND fm.deleted = b'0'
WHERE rm.menu_id = 6139
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
WHERE id = 6139;

DELETE FROM system_role_menu WHERE menu_id = 6139;

UPDATE sys_permission
SET deleted = 1,
    updater = 'flyway',
    update_time = NOW()
WHERE id = 36
  AND code = 'oa:log:operation';

DELETE FROM sys_role_permission WHERE permission_id = 36;
