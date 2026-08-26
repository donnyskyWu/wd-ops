-- Local fix ONLY — minimal work-task menu/dict patch when 02 is too heavy.
-- Target DB: shenyu-sys (local dev per football-integration-overlay.yml).
-- Schema SSOT: Beta test shenyu-system @ 110.42.49.224 (system_menu.user_type, system_dict_data.value).
-- Prefer: mysql ... shenyu-sys < docs/deploy/ops-greenfield-production/sql/02-shenyu-system-menus.sql
SET NAMES utf8mb4;

-- 6100 运营数据 (root)
INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, user_type
)
SELECT 6100, '运营数据', '', 1, 1, 0, '/ops', 'ep:data-analysis', NULL, NULL,
       0, b'1', b'1', b'1', 'fix-local-work-task', 2
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6100);

-- 6102 内容生产 (parent of work-task)
INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, user_type
)
SELECT 6102, '内容生产', '', 1, 2, 6100, 'production', 'ep:folder', NULL, NULL,
       0, b'1', b'1', b'1', 'fix-local-work-task', 2
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6102);

UPDATE system_menu
SET name = '内容生产', parent_id = 6100, path = 'production', icon = 'ep:folder',
    visible = b'1', deleted = b'0', status = 0, user_type = 2, updater = 'fix-local-work-task'
WHERE id = 6102;

-- Work-task page + buttons (6194-6196)
INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, user_type
)
SELECT 6194, '工作任务管理', 'ops:work-task:list', 2, 10, 6102, 'work-task', 'ep:calendar',
       'ops/production/work-task/index', 'WorkTask', 0, b'1', b'1', b'1', 'fix-local-work-task', 2
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6194);

UPDATE system_menu
SET name = '工作任务管理', permission = 'ops:work-task:list', type = 2, sort = 10, parent_id = 6102,
    path = 'work-task', icon = 'ep:calendar', component = 'ops/production/work-task/index',
    component_name = 'WorkTask', visible = b'1', deleted = b'0', status = 0, user_type = 2,
    updater = 'fix-local-work-task'
WHERE id = 6194;

INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, user_type
)
SELECT 6195, '工作任务登记', 'ops:work-task:register', 3, 1, 6194, '', '', '', NULL,
       0, b'1', b'1', b'1', 'fix-local-work-task', 2
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6195);

UPDATE system_menu
SET name = '工作任务登记', permission = 'ops:work-task:register', parent_id = 6194,
    visible = b'1', deleted = b'0', user_type = 2, updater = 'fix-local-work-task'
WHERE id = 6195;

INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, user_type
)
SELECT 6196, '工作任务管理矩阵', 'ops:work-task:manage', 3, 2, 6194, '', '', '', NULL,
       0, b'1', b'1', b'1', 'fix-local-work-task', 2
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6196);

UPDATE system_menu
SET name = '工作任务管理矩阵', permission = 'ops:work-task:manage', parent_id = 6194,
    visible = b'1', deleted = b'0', user_type = 2, updater = 'fix-local-work-task'
WHERE id = 6196;

-- Deprecated match-pool menu (V185/V189); soft-delete if present
UPDATE system_menu
SET deleted = b'1', updater = 'deploy-cleanup', update_time = NOW()
WHERE id = 6197 AND deleted = b'0';

UPDATE system_role_menu
SET deleted = b'1', updater = 'deploy-cleanup', update_time = NOW()
WHERE menu_id = 6197 AND deleted = b'0';

-- Admin role (1): ancestors + work-task menus
INSERT INTO system_role_menu (role_id, menu_id, creator, tenant_id, user_type)
SELECT 1, m.id, 'fix-local-work-task', 1, 2
FROM system_menu m
WHERE m.id IN (6100, 6102, 6194, 6195, 6196) AND m.deleted = b'0'
  AND NOT EXISTS (
      SELECT 1 FROM system_role_menu rm
      WHERE rm.role_id = 1 AND rm.menu_id = m.id AND rm.deleted = b'0'
  );

-- IP组长 role
INSERT INTO system_role_menu (role_id, menu_id, creator, tenant_id, user_type)
SELECT r.id, m.id, 'fix-local-work-task', 1, 2
FROM system_role r
JOIN system_menu m ON m.id IN (6100, 6102, 6194, 6195, 6196)
WHERE r.code = 'ip_group_leader' AND r.tenant_id = 1 AND r.deleted = b'0' AND m.deleted = b'0'
  AND NOT EXISTS (
      SELECT 1 FROM system_role_menu rm
      WHERE rm.role_id = r.id AND rm.menu_id = m.id AND rm.deleted = b'0'
  );

-- Ops manager role
INSERT INTO system_role_menu (role_id, menu_id, creator, tenant_id, user_type)
SELECT r.id, m.id, 'fix-local-work-task', 1, 2
FROM system_role r
JOIN system_menu m ON m.id IN (6100, 6102, 6194, 6195, 6196)
WHERE r.code = 'ops_manager' AND r.tenant_id = 1 AND r.deleted = b'0' AND m.deleted = b'0'
  AND NOT EXISTS (
      SELECT 1 FROM system_role_menu rm
      WHERE rm.role_id = r.id AND rm.menu_id = m.id AND rm.deleted = b'0'
  );

-- Dict types
INSERT INTO system_dict_type (name, type, status, remark, creator, updater, deleted)
SELECT '营销计划类型', 'dict_marketing_plan_type', 0, 'FR-M2-010 work-task', 'fix-local-work-task', 'fix-local-work-task', b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_dict_type WHERE type = 'dict_marketing_plan_type' AND deleted = b'0');

INSERT INTO system_dict_type (name, type, status, remark, creator, updater, deleted)
SELECT '销售平台', 'dict_sales_platform', 0, 'FR-M2-010 work-task', 'fix-local-work-task', 'fix-local-work-task', b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_dict_type WHERE type = 'dict_sales_platform' AND deleted = b'0');

INSERT INTO system_dict_type (name, type, status, remark, creator, updater, deleted)
SELECT '红黑预测', 'dict_win_prediction', 0, 'FR-M2-010 work-task', 'fix-local-work-task', 'fix-local-work-task', b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_dict_type WHERE type = 'dict_win_prediction' AND deleted = b'0');

INSERT INTO system_dict_type (name, type, status, remark, creator, updater, deleted)
SELECT '工作任务登记状态', 'dict_work_task_sheet_status', 0, 'FR-M2-010 work-task', 'fix-local-work-task', 'fix-local-work-task', b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_dict_type WHERE type = 'dict_work_task_sheet_status' AND deleted = b'0');

-- Dict data (local schema uses value)
INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, remark, creator, updater, deleted)
SELECT 1, '直播公推', 'LIVE_PUBLIC', 'dict_marketing_plan_type', 0, 'success', NULL, 'fix-local-work-task', 'fix-local-work-task', b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='dict_marketing_plan_type' AND value='LIVE_PUBLIC' AND deleted=b'0');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, remark, creator, updater, deleted)
SELECT 2, '付费销售', 'PAID_SALES', 'dict_marketing_plan_type', 0, 'warning', NULL, 'fix-local-work-task', 'fix-local-work-task', b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='dict_marketing_plan_type' AND value='PAID_SALES' AND deleted=b'0');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, remark, creator, updater, deleted)
SELECT 3, '直播引流', 'LIVE_DRAIN', 'dict_marketing_plan_type', 0, 'primary', NULL, 'fix-local-work-task', 'fix-local-work-task', b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='dict_marketing_plan_type' AND value='LIVE_DRAIN' AND deleted=b'0');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, remark, creator, updater, deleted)
SELECT 1, '私域', 'PRIVATE', 'dict_sales_platform', 0, 'primary', NULL, 'fix-local-work-task', 'fix-local-work-task', b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='dict_sales_platform' AND value='PRIVATE' AND deleted=b'0');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, remark, creator, updater, deleted)
SELECT 2, '快手', 'KUAISHOU', 'dict_sales_platform', 0, 'primary', NULL, 'fix-local-work-task', 'fix-local-work-task', b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='dict_sales_platform' AND value='KUAISHOU' AND deleted=b'0');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, remark, creator, updater, deleted)
SELECT 3, '抖音', 'DOUYIN', 'dict_sales_platform', 0, 'primary', NULL, 'fix-local-work-task', 'fix-local-work-task', b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='dict_sales_platform' AND value='DOUYIN' AND deleted=b'0');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, remark, creator, updater, deleted)
SELECT 4, '无', 'NONE', 'dict_sales_platform', 0, 'info', NULL, 'fix-local-work-task', 'fix-local-work-task', b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='dict_sales_platform' AND value='NONE' AND deleted=b'0');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, remark, creator, updater, deleted)
SELECT 1, '未知', 'UNKNOWN', 'dict_win_prediction', 0, 'info', NULL, 'fix-local-work-task', 'fix-local-work-task', b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='dict_win_prediction' AND value='UNKNOWN' AND deleted=b'0');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, remark, creator, updater, deleted)
SELECT 2, '红', 'RED', 'dict_win_prediction', 0, 'danger', NULL, 'fix-local-work-task', 'fix-local-work-task', b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='dict_win_prediction' AND value='RED' AND deleted=b'0');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, remark, creator, updater, deleted)
SELECT 3, '黑', 'BLACK', 'dict_win_prediction', 0, 'default', NULL, 'fix-local-work-task', 'fix-local-work-task', b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='dict_win_prediction' AND value='BLACK' AND deleted=b'0');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, remark, creator, updater, deleted)
SELECT 1, '草稿', 'DRAFT', 'dict_work_task_sheet_status', 0, 'info', NULL, 'fix-local-work-task', 'fix-local-work-task', b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='dict_work_task_sheet_status' AND value='DRAFT' AND deleted=b'0');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, remark, creator, updater, deleted)
SELECT 2, '已确认', 'CONFIRMED', 'dict_work_task_sheet_status', 0, 'success', NULL, 'fix-local-work-task', 'fix-local-work-task', b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='dict_work_task_sheet_status' AND value='CONFIRMED' AND deleted=b'0');
