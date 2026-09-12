-- ADR-064: OPS six business roles + system_role_menu (exclude super_admin)
-- Apply AFTER seed-oa-system-menu.sql (utf8mb4 stdin via apply-seed-oa-menu.py)
-- Target: Football shenyu-system.system_role / system_role_menu
-- Pattern: dict-style INSERT (no id; AUTO_INCREMENT) WHERE NOT EXISTS by code + tenant_id + deleted=0.
-- role_menu: INSERT … SELECT r.id, m.id JOIN by code / menu id; no hardcoded role_id or role_menu.id.
-- Idempotent: skip existing same-code roles (do not overwrite Football); rebuild Ops menu binds;
--             preserves work-task role_menu 6194-6196 (V183 / 03_work_task_menus_v183).
-- Historical: older packs used preferred ids 160–165; re-run binds those rows by code.
SET NAMES utf8mb4;

BEGIN;

-- ===== IP组长 (ip_group_leader) menus=48 =====
INSERT INTO system_role (
    name, code, sort, data_scope, data_scope_dept_ids, status, type, remark,
    creator, create_time, updater, update_time, deleted, tenant_id
)
SELECT
    'IP组长', 'ip_group_leader', 20, 5, '', 0, 1,
    'ADR-064：IP组组长；一级内容审核（本组）',
    'adr-064-seed', NOW(), 'adr-064-seed', NOW(), b'0', 1
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_role x WHERE x.code = 'ip_group_leader' AND x.tenant_id = 1 AND x.deleted = b'0'
);

DELETE rm FROM system_role_menu rm
INNER JOIN system_role r ON r.id = rm.role_id
WHERE r.code = 'ip_group_leader' AND r.tenant_id = 1 AND r.deleted = b'0'
  AND rm.menu_id >= 6100 AND rm.menu_id < 7000
  AND rm.menu_id NOT IN (6194, 6195, 6196);  -- preserve work-task (03_work_task_menus_v183)

INSERT INTO system_role_menu (role_id, menu_id, creator, tenant_id, user_type)
SELECT r.id, m.id, 'adr-064-seed', 1, 2
FROM system_role r
INNER JOIN system_menu m ON m.id IN (
    6100, 6101, 6102, 6103, 6106, 6107, 6108, 6109, 6112, 6113,
    6114, 6115, 6116, 6117, 6118, 6119, 6120, 6121, 6122, 6123,
    6124, 6126, 6128, 6130, 6142, 6143, 6144, 6145, 6146, 6147,
    6148, 6149, 6150, 6151, 6152, 6153, 6154, 6156, 6157, 6158,
    6159, 6168, 6170, 6171, 6172, 6173, 6174, 6175
)
WHERE r.code = 'ip_group_leader' AND r.tenant_id = 1 AND r.deleted = b'0'
  AND m.deleted = b'0'
  AND NOT EXISTS (
      SELECT 1 FROM system_role_menu rm
      WHERE rm.role_id = r.id AND rm.menu_id = m.id AND rm.deleted = b'0'
  );

-- ===== 运营主管 (ops_manager) menus=69 =====
INSERT INTO system_role (
    name, code, sort, data_scope, data_scope_dept_ids, status, type, remark,
    creator, create_time, updater, update_time, deleted, tenant_id
)
SELECT
    '运营主管', 'ops_manager', 21, 1, '', 0, 2,
    'ADR-064：运营主管；二级内容审核；租户 ALL',
    'adr-064-seed', NOW(), 'adr-064-seed', NOW(), b'0', 1
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_role x WHERE x.code = 'ops_manager' AND x.tenant_id = 1 AND x.deleted = b'0'
);

DELETE rm FROM system_role_menu rm
INNER JOIN system_role r ON r.id = rm.role_id
WHERE r.code = 'ops_manager' AND r.tenant_id = 1 AND r.deleted = b'0'
  AND rm.menu_id >= 6100 AND rm.menu_id < 7000
  AND rm.menu_id NOT IN (6194, 6195, 6196);  -- preserve work-task (03_work_task_menus_v183)

INSERT INTO system_role_menu (role_id, menu_id, creator, tenant_id, user_type)
SELECT r.id, m.id, 'adr-064-seed', 1, 2
FROM system_role r
INNER JOIN system_menu m ON m.id IN (
    6100, 6101, 6102, 6103, 6104, 6105, 6106, 6107, 6108, 6109,
    6110, 6111, 6112, 6113, 6114, 6115, 6116, 6117, 6118, 6119,
    6120, 6121, 6122, 6123, 6124, 6125, 6126, 6127, 6128, 6129,
    6130, 6131, 6132, 6133, 6136, 6140, 6141, 6142, 6143, 6144,
    6145, 6146, 6147, 6148, 6149, 6150, 6151, 6152, 6153, 6154,
    6156, 6157, 6158, 6159, 6160, 6161, 6162, 6163, 6164, 6165,
    6166, 6167, 6168, 6170, 6171, 6172, 6173, 6174, 6175
)
WHERE r.code = 'ops_manager' AND r.tenant_id = 1 AND r.deleted = b'0'
  AND m.deleted = b'0'
  AND NOT EXISTS (
      SELECT 1 FROM system_role_menu rm
      WHERE rm.role_id = r.id AND rm.menu_id = m.id AND rm.deleted = b'0'
  );

-- ===== 财务人员 (finance) menus=34 =====
INSERT INTO system_role (
    name, code, sort, data_scope, data_scope_dept_ids, status, type, remark,
    creator, create_time, updater, update_time, deleted, tenant_id
)
SELECT
    '财务人员', 'finance', 22, 1, '', 0, 2,
    'ADR-064：财务域；成本/ROI/绩效结果',
    'adr-064-seed', NOW(), 'adr-064-seed', NOW(), b'0', 1
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_role x WHERE x.code = 'finance' AND x.tenant_id = 1 AND x.deleted = b'0'
);

DELETE rm FROM system_role_menu rm
INNER JOIN system_role r ON r.id = rm.role_id
WHERE r.code = 'finance' AND r.tenant_id = 1 AND r.deleted = b'0'
  AND rm.menu_id >= 6100 AND rm.menu_id < 7000
  AND rm.menu_id NOT IN (6194, 6195, 6196);  -- preserve work-task (03_work_task_menus_v183)

INSERT INTO system_role_menu (role_id, menu_id, creator, tenant_id, user_type)
SELECT r.id, m.id, 'adr-064-seed', 1, 2
FROM system_role r
INNER JOIN system_menu m ON m.id IN (
    6100, 6101, 6102, 6103, 6106, 6107, 6108, 6109, 6111, 6112,
    6113, 6114, 6115, 6116, 6117, 6126, 6127, 6142, 6143, 6144,
    6146, 6147, 6148, 6149, 6150, 6151, 6152, 6153, 6154, 6156,
    6157, 6158, 6168, 6174
)
WHERE r.code = 'finance' AND r.tenant_id = 1 AND r.deleted = b'0'
  AND m.deleted = b'0'
  AND NOT EXISTS (
      SELECT 1 FROM system_role_menu rm
      WHERE rm.role_id = r.id AND rm.menu_id = m.id AND rm.deleted = b'0'
  );

-- ===== 内容编辑 (content_editor) menus=29 =====
INSERT INTO system_role (
    name, code, sort, data_scope, data_scope_dept_ids, status, type, remark,
    creator, create_time, updater, update_time, deleted, tenant_id
)
SELECT
    '内容编辑', 'content_editor', 23, 5, '', 0, 2,
    'ADR-064：内容编辑；SELF+本组只读；不审（无6118）',
    'adr-064-seed', NOW(), 'adr-064-seed', NOW(), b'0', 1
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_role x WHERE x.code = 'content_editor' AND x.tenant_id = 1 AND x.deleted = b'0'
);

DELETE rm FROM system_role_menu rm
INNER JOIN system_role r ON r.id = rm.role_id
WHERE r.code = 'content_editor' AND r.tenant_id = 1 AND r.deleted = b'0'
  AND rm.menu_id >= 6100 AND rm.menu_id < 7000
  AND rm.menu_id NOT IN (6194, 6195, 6196);  -- preserve work-task (03_work_task_menus_v183)

INSERT INTO system_role_menu (role_id, menu_id, creator, tenant_id, user_type)
SELECT r.id, m.id, 'adr-064-seed', 1, 2
FROM system_role r
INNER JOIN system_menu m ON m.id IN (
    6100, 6101, 6102, 6103, 6108, 6109, 6112, 6113, 6114, 6115,
    6116, 6117, 6119, 6120, 6121, 6124, 6125, 6128, 6148, 6149,
    6150, 6151, 6152, 6153, 6154, 6157, 6158, 6168, 6174
)
WHERE r.code = 'content_editor' AND r.tenant_id = 1 AND r.deleted = b'0'
  AND m.deleted = b'0'
  AND NOT EXISTS (
      SELECT 1 FROM system_role_menu rm
      WHERE rm.role_id = r.id AND rm.menu_id = m.id AND rm.deleted = b'0'
  );

-- ===== 运营 (ops_operator) menus=34 =====
INSERT INTO system_role (
    name, code, sort, data_scope, data_scope_dept_ids, status, type, remark,
    creator, create_time, updater, update_time, deleted, tenant_id
)
SELECT
    '运营', 'ops_operator', 24, 5, '', 0, 2,
    'ADR-064：运营（含主播/快手）；IP_GROUP+SELF；无审核/无全部任务',
    'adr-064-seed', NOW(), 'adr-064-seed', NOW(), b'0', 1
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_role x WHERE x.code = 'ops_operator' AND x.tenant_id = 1 AND x.deleted = b'0'
);

DELETE rm FROM system_role_menu rm
INNER JOIN system_role r ON r.id = rm.role_id
WHERE r.code = 'ops_operator' AND r.tenant_id = 1 AND r.deleted = b'0'
  AND rm.menu_id >= 6100 AND rm.menu_id < 7000
  AND rm.menu_id NOT IN (6194, 6195, 6196);  -- preserve work-task (03_work_task_menus_v183)

INSERT INTO system_role_menu (role_id, menu_id, creator, tenant_id, user_type)
SELECT r.id, m.id, 'adr-064-seed', 1, 2
FROM system_role r
INNER JOIN system_menu m ON m.id IN (
    6100, 6101, 6102, 6106, 6107, 6108, 6109, 6112, 6113, 6114,
    6115, 6116, 6117, 6119, 6120, 6121, 6122, 6124, 6143, 6144,
    6146, 6147, 6148, 6149, 6150, 6151, 6152, 6153, 6154, 6156,
    6157, 6158, 6168, 6174
)
WHERE r.code = 'ops_operator' AND r.tenant_id = 1 AND r.deleted = b'0'
  AND m.deleted = b'0'
  AND NOT EXISTS (
      SELECT 1 FROM system_role_menu rm
      WHERE rm.role_id = r.id AND rm.menu_id = m.id AND rm.deleted = b'0'
  );

-- ===== 数据分析 (data_analyst) menus=52 =====
INSERT INTO system_role (
    name, code, sort, data_scope, data_scope_dept_ids, status, type, remark,
    creator, create_time, updater, update_time, deleted, tenant_id
)
SELECT
    '数据分析', 'data_analyst', 25, 1, '', 0, 2,
    'ADR-064：分析域 ALL；监测/报表 RWD；采集 R；无内容审核',
    'adr-064-seed', NOW(), 'adr-064-seed', NOW(), b'0', 1
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_role x WHERE x.code = 'data_analyst' AND x.tenant_id = 1 AND x.deleted = b'0'
);

DELETE rm FROM system_role_menu rm
INNER JOIN system_role r ON r.id = rm.role_id
WHERE r.code = 'data_analyst' AND r.tenant_id = 1 AND r.deleted = b'0'
  AND rm.menu_id >= 6100 AND rm.menu_id < 7000
  AND rm.menu_id NOT IN (6194, 6195, 6196);  -- preserve work-task (03_work_task_menus_v183)

INSERT INTO system_role_menu (role_id, menu_id, creator, tenant_id, user_type)
SELECT r.id, m.id, 'adr-064-seed', 1, 2
FROM system_role r
INNER JOIN system_menu m ON m.id IN (
    6100, 6101, 6102, 6103, 6104, 6106, 6107, 6108, 6109, 6110,
    6111, 6112, 6113, 6114, 6115, 6116, 6117, 6119, 6120, 6121,
    6122, 6124, 6125, 6126, 6127, 6128, 6129, 6130, 6131, 6132,
    6133, 6136, 6142, 6143, 6144, 6145, 6146, 6147, 6148, 6149,
    6150, 6151, 6152, 6153, 6154, 6156, 6157, 6158, 6159, 6165,
    6168, 6174
)
WHERE r.code = 'data_analyst' AND r.tenant_id = 1 AND r.deleted = b'0'
  AND m.deleted = b'0'
  AND NOT EXISTS (
      SELECT 1 FROM system_role_menu rm
      WHERE rm.role_id = r.id AND rm.menu_id = m.id AND rm.deleted = b'0'
  );

COMMIT;

-- Expected menu counts (ADR-064 §5):
--   ip_group_leader: 48
--   ops_manager: 69
--   finance: 34
--   content_editor: 29
--   ops_operator: 34
--   data_analyst: 52
