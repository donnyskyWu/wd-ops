-- V152: Fix V148 target DB — merge wd.sys_dict_* → shenyu-system.system_dict_*
--
-- V148 ran on Flyway master (wd) and inserted into wd.system_dict_type by mistake.
-- DictService / SystemDictAdapter read @DS("system") → shenyu-system.system_dict_*.
-- This script uses fully-qualified shenyu-system table names so Flyway on wd can cross-DB merge.
--
-- Manual copy: docs/sql/merge_ops_dict_to_football_manual.sql (run connected to shenyu-system).

SET NAMES utf8mb4;

INSERT INTO `shenyu-system`.system_dict_type (
    name, type, status, remark,
    creator, create_time, updater, update_time, deleted
)
SELECT
    w.name,
    w.type,
    CASE UPPER(TRIM(w.status)) WHEN 'DISABLED' THEN 1 ELSE 0 END,
    CONCAT('ops-dict-merge:', IFNULL(w.type, '')),
    'ops-dict-merge-v152',
    IFNULL(w.create_time, NOW()),
    'ops-dict-merge-v152',
    NOW(),
    b'0'
FROM wd.sys_dict_type w
WHERE w.type LIKE 'dict\_%'
  AND w.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM `shenyu-system`.system_dict_type t
      WHERE t.type COLLATE utf8mb4_unicode_ci = w.type COLLATE utf8mb4_unicode_ci
        AND t.deleted = b'0'
  );

INSERT INTO `shenyu-system`.system_dict_data (
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
    'ops-dict-merge-v152',
    IFNULL(d.create_time, NOW()),
    'ops-dict-merge-v152',
    NOW(),
    b'0'
FROM wd.sys_dict_data d
WHERE d.dict_type LIKE 'dict\_%'
  AND d.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM `shenyu-system`.system_dict_data sd
      WHERE sd.dict_type COLLATE utf8mb4_unicode_ci = d.dict_type COLLATE utf8mb4_unicode_ci
        AND sd.value COLLATE utf8mb4_unicode_ci = d.dict_value COLLATE utf8mb4_unicode_ci
        AND sd.deleted = b'0'
  );
