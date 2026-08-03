-- CLEANUP P0-3 / A-WP2：平行菜单与平行权限残留清理（幂等）
-- Target DB: shenyu-system（Football 菜单 / 角色菜单 SSOT）
-- Refs:
--   docs/delivery/OPS-FOOTBALL-MERGE-CLEANUP-INVENTORY.md §5 · P0-3
--   docs/delivery/OPS-DICT-MERGE-FOOTBALL-PLAN.md（6137 / oa:dict:*）
--   scripts/integration-config/seed-ops-test-remote-shenyu-system-menus.sql
--   scripts/integration-config/seed-oa-system-menu.sql（6137–6139/6155 已注释移除）
--
-- Scope:
--   1) Hard DELETE menus 6137 / 6138 / 6139 / 6155 + system_role_menu（与 seed 一致）
--   2) Hard DELETE 仍挂平行权限码的 OPS 菜单行（oa:/ops: user|dept|dict|log|role|tenant|permission|author）
--      及其 role_menu 绑定（不含 OPS 业务模块如 ops:collect:log:* / ops:account:* 等）
--
-- Out of scope（本脚本不触碰）:
--   - archive_* 表 / oa_* 业务表 / sys_metadata_* / shenyu-system 字典 SSOT
--   - shenyu-ops.sys_permission（B-WP4 stop-write / 已 RENAME archive_*；鉴权 SSOT=system_menu）
--
-- Apply:
--   mysql --default-character-set=utf8mb4 -hHOST -uUSER -p shenyu-system < cleanup-oa-parallel-menu-perm.sql

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------------
-- 0) Before counts（便于人工对照；不影响清理）
-- ---------------------------------------------------------------------------
SELECT 'BEFORE' AS phase,
  (SELECT COUNT(*) FROM system_menu WHERE id IN (6137, 6138, 6139, 6155)) AS menu_target_any,
  (SELECT COUNT(*) FROM system_role_menu WHERE menu_id IN (6137, 6138, 6139, 6155)) AS role_menu_target_any,
  (SELECT COUNT(*) FROM system_menu
     WHERE permission LIKE 'oa:user:%' OR permission LIKE 'oa:dept:%' OR permission LIKE 'oa:dict:%'
        OR permission LIKE 'oa:log:%' OR permission LIKE 'oa:role:%' OR permission LIKE 'oa:tenant:%'
        OR permission LIKE 'oa:permission:%' OR permission LIKE 'oa:author:%'
        OR permission LIKE 'ops:user:%' OR permission LIKE 'ops:dept:%' OR permission LIKE 'ops:dict:%'
        OR permission LIKE 'ops:log:%' OR permission LIKE 'ops:role:%' OR permission LIKE 'ops:tenant:%'
        OR permission LIKE 'ops:permission:%' OR permission LIKE 'ops:author:%') AS parallel_perm_menus_any,
  (SELECT COUNT(*) FROM system_menu WHERE deleted = b'0' AND id BETWEEN 6100 AND 6999) AS ops_menus_active,
  (SELECT COUNT(*) FROM system_role_menu rm
     JOIN system_role r ON r.id = rm.role_id AND r.code = 'super_admin' AND r.deleted = b'0'
     JOIN system_menu m ON m.id = rm.menu_id AND m.deleted = b'0'
     WHERE rm.deleted = b'0' AND m.id BETWEEN 6100 AND 6999) AS super_admin_ops_bindings;

-- ---------------------------------------------------------------------------
-- 1) Documented parallel menus（字典 / 登录日志 / 操作日志 / 作者）
-- ---------------------------------------------------------------------------
DELETE FROM system_role_menu WHERE menu_id IN (6137, 6138, 6139, 6155);
DELETE FROM system_menu WHERE id IN (6137, 6138, 6139, 6155);

-- ---------------------------------------------------------------------------
-- 2) Any remaining parallel-permission menus（含已 rename oa→ops 的平行码）
--    Keep OPS business codes: ops:account:* / ops:plan:* / ops:collect:* / …
-- ---------------------------------------------------------------------------
DELETE rm
FROM system_role_menu rm
JOIN system_menu m ON m.id = rm.menu_id
WHERE m.permission LIKE 'oa:user:%' OR m.permission LIKE 'oa:dept:%' OR m.permission LIKE 'oa:dict:%'
   OR m.permission LIKE 'oa:log:%' OR m.permission LIKE 'oa:role:%' OR m.permission LIKE 'oa:tenant:%'
   OR m.permission LIKE 'oa:permission:%' OR m.permission LIKE 'oa:author:%'
   OR m.permission LIKE 'ops:user:%' OR m.permission LIKE 'ops:dept:%' OR m.permission LIKE 'ops:dict:%'
   OR m.permission LIKE 'ops:log:%' OR m.permission LIKE 'ops:role:%' OR m.permission LIKE 'ops:tenant:%'
   OR m.permission LIKE 'ops:permission:%' OR m.permission LIKE 'ops:author:%';

DELETE FROM system_menu
WHERE permission LIKE 'oa:user:%' OR permission LIKE 'oa:dept:%' OR permission LIKE 'oa:dict:%'
   OR permission LIKE 'oa:log:%' OR permission LIKE 'oa:role:%' OR permission LIKE 'oa:tenant:%'
   OR permission LIKE 'oa:permission:%' OR permission LIKE 'oa:author:%'
   OR permission LIKE 'ops:user:%' OR permission LIKE 'ops:dept:%' OR permission LIKE 'ops:dict:%'
   OR permission LIKE 'ops:log:%' OR permission LIKE 'ops:role:%' OR permission LIKE 'ops:tenant:%'
   OR permission LIKE 'ops:permission:%' OR permission LIKE 'ops:author:%';

-- ---------------------------------------------------------------------------
-- 3) After counts
-- ---------------------------------------------------------------------------
SELECT 'AFTER' AS phase,
  (SELECT COUNT(*) FROM system_menu WHERE id IN (6137, 6138, 6139, 6155)) AS menu_target_any,
  (SELECT COUNT(*) FROM system_role_menu WHERE menu_id IN (6137, 6138, 6139, 6155)) AS role_menu_target_any,
  (SELECT COUNT(*) FROM system_menu
     WHERE permission LIKE 'oa:user:%' OR permission LIKE 'oa:dept:%' OR permission LIKE 'oa:dict:%'
        OR permission LIKE 'oa:log:%' OR permission LIKE 'oa:role:%' OR permission LIKE 'oa:tenant:%'
        OR permission LIKE 'oa:permission:%' OR permission LIKE 'oa:author:%'
        OR permission LIKE 'ops:user:%' OR permission LIKE 'ops:dept:%' OR permission LIKE 'ops:dict:%'
        OR permission LIKE 'ops:log:%' OR permission LIKE 'ops:role:%' OR permission LIKE 'ops:tenant:%'
        OR permission LIKE 'ops:permission:%' OR permission LIKE 'ops:author:%') AS parallel_perm_menus_any,
  (SELECT COUNT(*) FROM system_menu WHERE deleted = b'0' AND id BETWEEN 6100 AND 6999) AS ops_menus_active,
  (SELECT COUNT(*) FROM system_role_menu rm
     JOIN system_role r ON r.id = rm.role_id AND r.code = 'super_admin' AND r.deleted = b'0'
     JOIN system_menu m ON m.id = rm.menu_id AND m.deleted = b'0'
     WHERE rm.deleted = b'0' AND m.id BETWEEN 6100 AND 6999) AS super_admin_ops_bindings;
