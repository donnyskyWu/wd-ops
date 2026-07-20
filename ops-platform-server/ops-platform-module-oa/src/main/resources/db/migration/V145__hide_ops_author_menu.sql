-- P0: Hide OPS duplicate 作者管理 menu (6155); CRUD SSOT = Football author/info (ADR-051)
UPDATE system_menu
SET deleted = b'1',
    visible = b'0',
    updater = 'flyway',
    update_time = NOW()
WHERE id = 6155;

DELETE FROM system_role_menu WHERE menu_id = 6155;
