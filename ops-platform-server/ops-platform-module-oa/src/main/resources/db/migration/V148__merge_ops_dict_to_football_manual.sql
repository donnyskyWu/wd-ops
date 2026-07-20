-- V148: OPS dict_* → Football system_dict_* 数据合并（手动迁移副本）
--
-- ⚠ BUG (fixed by V152): Flyway runs on master (wd), so unqualified system_dict_* targeted wd
--    replica tables, not shenyu-system. DictService reads @DS("system") → use V152 instead.
--
-- ⚠ 生产环境：优先使用 docs/sql/merge_ops_dict_to_football_manual.sql 由 DBA 手工执行。
--    本 Flyway 脚本供 localhost 集成环境自动对齐；远程 cutover 前须备份 shenyu-system。
--
-- 关联：docs/delivery/OPS-DICT-MERGE-FOOTBALL-PLAN.md · Worker DB-1
-- 菜单移除（6137 / oa:dict:*）见规划 V149，不在本脚本范围。

SET NAMES utf8mb4;

-- §1 字典类型
INSERT INTO system_dict_type (
    name, type, status, remark,
    creator, create_time, updater, update_time, deleted
)
SELECT
    w.name,
    w.type,
    CASE UPPER(TRIM(w.status)) WHEN 'DISABLED' THEN 1 ELSE 0 END,
    CONCAT('ops-dict-merge:', IFNULL(w.type, '')),
    'ops-dict-merge',
    IFNULL(w.create_time, NOW()),
    'ops-dict-merge',
    NOW(),
    b'0'
FROM wd.sys_dict_type w
WHERE w.type LIKE 'dict\_%'
  AND w.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM system_dict_type t
      WHERE t.type COLLATE utf8mb4_unicode_ci = w.type COLLATE utf8mb4_unicode_ci
        AND t.deleted = b'0'
  );

-- §2 字典数据（dict_value → value）
INSERT INTO system_dict_data (
    sort, label, value, dict_type, status,
    color_type, css_class, remark,
    creator, create_time, updater, update_time, deleted
)
SELECT
    IFNULL(d.sort, 0),
    d.label,
    d.dict_value,
    d.dict_type,
    CASE UPPER(TRIM(d.status)) WHEN 'DISABLED' THEN 1 ELSE 0 END,
    IFNULL(NULLIF(TRIM(d.color_type), ''), 'default'),
    '',
    d.remark,
    'ops-dict-merge',
    IFNULL(d.create_time, NOW()),
    'ops-dict-merge',
    NOW(),
    b'0'
FROM wd.sys_dict_data d
WHERE d.dict_type LIKE 'dict\_%'
  AND d.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM system_dict_data sd
      WHERE sd.dict_type COLLATE utf8mb4_unicode_ci = d.dict_type COLLATE utf8mb4_unicode_ci
        AND sd.value COLLATE utf8mb4_unicode_ci = d.dict_value COLLATE utf8mb4_unicode_ci
        AND sd.deleted = b'0'
  );
