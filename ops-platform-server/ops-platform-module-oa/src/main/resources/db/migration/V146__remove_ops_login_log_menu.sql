-- AL-04: Remove OPS duplicate 登录日志 menu (6138) and permission oa:log:login (id 37).
-- OPS does not host login; SSOT = Football system/login-log (OPS-AUDIT-LOG-MIGRATION-PLAN §1.1).

UPDATE system_menu
SET deleted = b'1',
    visible = b'0',
    updater = 'flyway',
    update_time = NOW()
WHERE id = 6138;

DELETE FROM system_role_menu WHERE menu_id = 6138;

UPDATE sys_permission
SET deleted = 1,
    updater = 'flyway',
    update_time = NOW()
WHERE id = 37
  AND code = 'oa:log:login';

DELETE FROM sys_role_permission WHERE permission_id = 37;
