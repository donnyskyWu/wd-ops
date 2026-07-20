SET NAMES utf8mb4;
DELETE FROM system_role_menu WHERE role_id = 1 AND tenant_id = 1;
INSERT INTO system_role_menu (role_id, menu_id, user_type, creator, tenant_id, deleted)
SELECT 1, m.id, m.user_type, 'menu-reset', 1, b'0'
FROM system_menu m
WHERE m.deleted = b'0';
SELECT 'role_menu_super_admin' AS step, COUNT(*) AS cnt FROM system_role_menu WHERE role_id=1 AND tenant_id=1;
SELECT 'total_role_menu' AS step, COUNT(*) AS cnt FROM system_role_menu;
