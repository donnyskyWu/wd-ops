-- =============================================================================
-- 【生产热修 / 可单独执行】shenyu-system — Ops 六业务角色 + 角色菜单绑定
-- 日期: 2026-08-27
-- 目标库: shenyu-system（Football system_role / system_role_menu；不要在 shenyu-ops 执行）
--
-- 用途: 生产环境缺少 Ops 六角色或角色-菜单绑定时补数。不必跑完整 02。
-- 幂等: 角色按 code + tenant_id + deleted=0 的 WHERE NOT EXISTS 插入（已存在则跳过，不覆盖 Football）；
--       role_menu 先按角色清理 6100–6999（保留 6194–6196 工作任务绑定），再 JOIN 插入。
-- 主键: 不写 system_role.id / system_role_menu.id（AUTO_INCREMENT）；按 r.code JOIN m.id。
-- 密钥: 本文件不含任何密钥/账号。
-- 不含: Football 超管 role_id=1 的菜单授权（本文件只管六业务角色）。
--
-- ★ 前置依赖（必读）:
--   role_menu 通过 INNER JOIN system_menu m ON m.id IN (6100, 6101, …) 绑定。
--   生产须已存在 Ops 菜单（id 6100+，来自 02-shenyu-system-menus.sql 菜单段）。
--   若菜单从未灌入: 六个角色仍会插入，但 system_role_menu 会绑定 0 行（JOIN 不到菜单）。
--   执行前请抽检: SELECT COUNT(*) FROM system_menu WHERE id >= 6100 AND id < 7000 AND deleted = b'0';
--   期望至少有基线菜单（6100–6168 / 6175）；工作任务页 6194–6196 若缺失则工作任务菜单也无法绑定。
--
-- 工作任务菜单 6194–6196:
--   本文件不新建这些绑定（与 07 SSOT 一致：保留已有、不覆盖）。
--   若角色是本次新插入、且从未跑过 02 的工作任务 role_menu 段，这三条不会自动绑上。
--
-- 角色 code: ip_group_leader / ops_manager / finance / content_editor / ops_operator / data_analyst
-- 期望菜单绑定数（ADR-064，不含 6194–6196）:
--   ip_group_leader=49  ops_manager=70  finance=34
--   content_editor=30   ops_operator=35  data_analyst=52
--
-- 执行（Linux/macOS）:
--   mysql -h {{SYSTEM_DB_HOST}} -P {{SYSTEM_DB_PORT}} -u {{SYSTEM_DB_USER}} -p shenyu-system \
--     < docs/deploy/ops-greenfield-production/sql/02b-shenyu-system-ops-roles.sql
-- 执行（Windows PowerShell）:
--   Get-Content -Raw docs/deploy/ops-greenfield-production/sql/02b-shenyu-system-ops-roles.sql |
--     mysql -h HOST -u USER -p -D shenyu-system
-- =============================================================================

SET NAMES utf8mb4;


-- =============================================================================
-- ===== 07_ops_six_roles_rbac.sql =====
-- ADR-064 六角色（按 code，不写 id）+ role_menu JOIN
-- =============================================================================

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
    6159, 6168, 6170, 6171, 6172, 6173, 6174, 6175, 6290
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
    6166, 6167, 6168, 6170, 6171, 6172, 6173, 6174, 6175, 6290
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
    6150, 6151, 6152, 6153, 6154, 6157, 6158, 6168, 6174, 6290
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
    6157, 6158, 6168, 6174, 6290
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
--   ip_group_leader: 49
--   ops_manager: 70
--   finance: 34
--   content_editor: 30
--   ops_operator: 35
--   data_analyst: 52

-- =============================================================================
-- 执行后抽检（在 shenyu-system 上跑）
-- =============================================================================
-- SELECT code, id FROM system_role
-- WHERE code IN ('ip_group_leader','ops_manager','finance','content_editor','ops_operator','data_analyst')
--   AND tenant_id = 1 AND deleted = b'0';
-- SELECT r.code, COUNT(*) AS menu_cnt
-- FROM system_role r
-- JOIN system_role_menu rm ON rm.role_id = r.id AND rm.deleted = b'0'
-- WHERE r.code IN ('ip_group_leader','ops_manager','finance','content_editor','ops_operator','data_analyst')
--   AND r.tenant_id = 1 AND r.deleted = b'0'
--   AND rm.menu_id >= 6100 AND rm.menu_id < 7000
-- GROUP BY r.code;
-- -- 若 menu_cnt 全为 0：Ops 菜单（6100+）不在本库，请先执行 02 的菜单段后再重跑本文件。

