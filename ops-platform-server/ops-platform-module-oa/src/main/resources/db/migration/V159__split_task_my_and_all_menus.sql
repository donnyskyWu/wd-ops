-- Split task management into 我的任务 (/task) and 全部任务 (/task/all).

UPDATE system_menu
SET name = '我的任务',
    updater = 'flyway',
    update_time = NOW()
WHERE id = 6124
  AND deleted = b'0';

INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, user_type
)
SELECT
    6175, '全部任务', 'oa:task:list', 2, 9, 6102, 'task/all', 'ep:document',
    'ops/production/task/all', 'TaskAll', 0, b'1', b'1', b'1', 'flyway', 2
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6175);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id)
SELECT 70064, 1, 6175, 'flyway', 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70064);
