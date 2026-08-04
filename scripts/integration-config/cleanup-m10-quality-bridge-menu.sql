-- CLEANUP: M10 数据质量 + 私域桥接 — 本期不做，隐藏菜单与权限（幂等）
-- Target DB: shenyu-system（Football 菜单 / 角色菜单 SSOT）
-- Refs:
--   ADR-060 Phase 2 OOS Accept（quality / bridge stub）
--   scripts/integration-config/cleanup-oa-parallel-menu-perm.sql（DELETE 模式）
--   scripts/integration-config/seed-oa-system-menu.sql（6134/6135 已注释移除）
--
-- Scope:
--   1) Hard DELETE menus 6134 / 6135 + 子按钮（parent_id IN (6134,6135)）
--   2) Hard DELETE system_role_menu 绑定（含 ADR-064 六角色 71234/71235/72031/72032 等）
--   3) Hard DELETE 仍挂 ops:collect:bridge:* / ops:collect:quality:* 的菜单行
--
-- Out of scope:
--   - oa_private_domain_conversion_bridge 业务表 / 漏斗分析 PRIVATE_DOMAIN 预置
--   - dict_quality_* / dict_private_domain_* 字典（其他模块仍可能引用）
--   - shenyu-ops.sys_permission（鉴权 SSOT = system_menu.permission）
--
-- Apply:
--   mysql --default-character-set=utf8mb4 -hHOST -uUSER -p shenyu-system < cleanup-m10-quality-bridge-menu.sql

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------------
-- 0) Before counts
-- ---------------------------------------------------------------------------
SELECT 'BEFORE' AS phase,
  (SELECT COUNT(*) FROM system_menu WHERE id IN (6134, 6135)) AS menu_target_any,
  (SELECT COUNT(*) FROM system_menu WHERE parent_id IN (6134, 6135)) AS menu_children_any,
  (SELECT COUNT(*) FROM system_role_menu WHERE menu_id IN (6134, 6135)
      OR menu_id IN (SELECT id FROM system_menu WHERE parent_id IN (6134, 6135))) AS role_menu_target_any,
  (SELECT COUNT(*) FROM system_menu
     WHERE permission LIKE 'ops:collect:bridge:%'
        OR permission LIKE 'ops:collect:quality:%'
        OR permission LIKE 'oa:collect:bridge:%'
        OR permission LIKE 'oa:collect:quality:%') AS perm_menus_any;

-- ---------------------------------------------------------------------------
-- 1) Role-menu bindings for target menus + children
-- ---------------------------------------------------------------------------
DELETE rm
FROM system_role_menu rm
JOIN system_menu m ON m.id = rm.menu_id
WHERE m.id IN (6134, 6135)
   OR m.parent_id IN (6134, 6135)
   OR m.permission LIKE 'ops:collect:bridge:%'
   OR m.permission LIKE 'ops:collect:quality:%'
   OR m.permission LIKE 'oa:collect:bridge:%'
   OR m.permission LIKE 'oa:collect:quality:%';

-- ---------------------------------------------------------------------------
-- 2) Child button menus first, then page menus
-- ---------------------------------------------------------------------------
DELETE FROM system_menu WHERE parent_id IN (6134, 6135);
DELETE FROM system_menu WHERE id IN (6134, 6135);
DELETE FROM system_menu
WHERE permission LIKE 'ops:collect:bridge:%'
   OR permission LIKE 'ops:collect:quality:%'
   OR permission LIKE 'oa:collect:bridge:%'
   OR permission LIKE 'oa:collect:quality:%';

-- ---------------------------------------------------------------------------
-- 3) After counts
-- ---------------------------------------------------------------------------
SELECT 'AFTER' AS phase,
  (SELECT COUNT(*) FROM system_menu WHERE id IN (6134, 6135)) AS menu_target_any,
  (SELECT COUNT(*) FROM system_menu WHERE parent_id IN (6134, 6135)) AS menu_children_any,
  (SELECT COUNT(*) FROM system_role_menu WHERE menu_id IN (6134, 6135)
      OR menu_id IN (SELECT id FROM system_menu WHERE parent_id IN (6134, 6135))) AS role_menu_target_any,
  (SELECT COUNT(*) FROM system_menu
     WHERE permission LIKE 'ops:collect:bridge:%'
        OR permission LIKE 'ops:collect:quality:%'
        OR permission LIKE 'oa:collect:bridge:%'
        OR permission LIKE 'oa:collect:quality:%') AS perm_menus_any;
