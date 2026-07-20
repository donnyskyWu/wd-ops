-- =============================================================================
-- OPS dict_* → Football system_dict_* 手动迁移脚本
-- =============================================================================
-- 目标库 : shenyu-system（Football system-server）
-- 源库   : wd（OPS sys_dict_type / sys_dict_data）
-- 范围   : type LIKE 'dict_%' 的 OPS 业务字典（不含 system_user_sex 等平台枚举）
--
-- ⚠ 执行前必读
--   1. 生产环境：由 DBA 审阅后手工执行；勿在未备份环境运行。
--   2. 先备份：mysqldump shenyu-system system_dict_type system_dict_data
--   3. 前提：wd 与 shenyu-system 在同一 MySQL 实例（localhost 集成为真）。
--      若远程分库：先从 wd 导出 dict 表 CSV/SQL，再在本脚本 §1/§2 改为 staging 表导入。
--   4. 冲突策略：type / (dict_type,value) 已存在 → 跳过（不覆盖 Football 既有行）。
--   5. 不迁移 wd.id，避免与 Football AUTO_INCREMENT 冲突。
--
-- 关联文档：docs/delivery/OPS-DICT-MERGE-FOOTBALL-PLAN.md
-- Flyway 副本：ops-platform-server/.../V148__merge_ops_dict_to_football_manual.sql
-- =============================================================================

SET NAMES utf8mb4;

-- -----------------------------------------------------------------------------
-- §0 迁移前快照（可选，仅查询）
-- -----------------------------------------------------------------------------
-- SELECT COUNT(*) AS wd_types FROM wd.sys_dict_type WHERE type LIKE 'dict_%' AND deleted = 0;
-- SELECT COUNT(*) AS wd_data  FROM wd.sys_dict_data  WHERE dict_type LIKE 'dict_%' AND deleted = 0;
-- SELECT COUNT(*) AS sys_types FROM system_dict_type WHERE type LIKE 'dict_%' AND deleted = b'0';
-- SELECT COUNT(*) AS sys_data  FROM system_dict_data  WHERE dict_type LIKE 'dict_%' AND deleted = b'0';

-- -----------------------------------------------------------------------------
-- §0.1 冲突预检（执行 INSERT 前建议跑一遍）
-- -----------------------------------------------------------------------------
-- 类型冲突（wd 有、system 也有 — INSERT 将跳过）
-- SELECT w.type, w.name AS wd_name, s.name AS sys_name
-- FROM wd.sys_dict_type w
-- INNER JOIN system_dict_type s ON s.type = w.type AND s.deleted = b'0'
-- WHERE w.type LIKE 'dict_%' AND w.deleted = 0;

-- 数据冲突（同 type+value 已存在 — INSERT 将跳过）
-- SELECT d.dict_type, d.dict_value, d.label AS wd_label, sd.label AS sys_label
-- FROM wd.sys_dict_data d
-- INNER JOIN system_dict_data sd
--   ON sd.dict_type = d.dict_type AND sd.value = d.dict_value AND sd.deleted = b'0'
-- WHERE d.dict_type LIKE 'dict_%' AND d.deleted = 0;

-- wd 独有数据（将被插入）
-- SELECT COUNT(*) AS to_insert_data
-- FROM wd.sys_dict_data d
-- WHERE d.dict_type LIKE 'dict_%' AND d.deleted = 0
--   AND NOT EXISTS (
--     SELECT 1 FROM system_dict_data sd
--     WHERE sd.dict_type = d.dict_type AND sd.value = d.dict_value AND sd.deleted = b'0'
--   );

-- -----------------------------------------------------------------------------
-- §1 迁移字典类型：wd.sys_dict_type → system_dict_type
-- -----------------------------------------------------------------------------
INSERT INTO system_dict_type (
    name,
    type,
    status,
    remark,
    creator,
    create_time,
    updater,
    update_time,
    deleted
)
SELECT
    w.name,
    w.type,
    CASE UPPER(TRIM(w.status))
        WHEN 'ENABLED'  THEN 0
        WHEN 'DISABLED' THEN 1
        ELSE 0
    END,
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
      SELECT 1
      FROM system_dict_type t
      WHERE t.type = w.type
        AND t.deleted = b'0'
  );

-- -----------------------------------------------------------------------------
-- §2 迁移字典数据：wd.sys_dict_data → system_dict_data
--   列映射：dict_value → value；status ENABLED/DISABLED → 0/1
-- -----------------------------------------------------------------------------
INSERT INTO system_dict_data (
    sort,
    label,
    value,
    dict_type,
    status,
    color_type,
    css_class,
    remark,
    creator,
    create_time,
    updater,
    update_time,
    deleted
)
SELECT
    IFNULL(d.sort, 0),
    d.label,
    d.dict_value,
    d.dict_type,
    CASE UPPER(TRIM(d.status))
        WHEN 'ENABLED'  THEN 0
        WHEN 'DISABLED' THEN 1
        ELSE 0
    END,
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
      SELECT 1
      FROM system_dict_data sd
      WHERE sd.dict_type = d.dict_type
        AND sd.value = d.dict_value
        AND sd.deleted = b'0'
  );

-- -----------------------------------------------------------------------------
-- §3 迁移后验证（应手动执行并留存结果）
-- -----------------------------------------------------------------------------
-- 3.1 类型计数：system 库 dict_* 类型数应 ≥ wd 源库
SELECT
    (SELECT COUNT(*) FROM wd.sys_dict_type
     WHERE type LIKE 'dict_%' AND deleted = 0) AS wd_type_count,
    (SELECT COUNT(*) FROM system_dict_type
     WHERE type LIKE 'dict_%' AND deleted = b'0') AS system_type_count;

-- 3.2 数据计数
SELECT
    (SELECT COUNT(*) FROM wd.sys_dict_data
     WHERE dict_type LIKE 'dict_%' AND deleted = 0) AS wd_data_count,
    (SELECT COUNT(*) FROM system_dict_data
     WHERE dict_type LIKE 'dict_%' AND deleted = b'0') AS system_data_count;

-- 3.3 抽样：dict_platform_type（Gate 基线 ≥6 行）
SELECT dict_type, value, label, status, sort
FROM system_dict_data
WHERE dict_type = 'dict_platform_type'
  AND deleted = b'0'
ORDER BY sort, value;

-- 3.4 抽样：dict_position（绩效/SOP 依赖）
SELECT dict_type, value, label, status, sort
FROM system_dict_data
WHERE dict_type = 'dict_position'
  AND deleted = b'0'
ORDER BY sort, value;

-- 3.5 孤儿数据（有 data 无 type — 应为 0）
SELECT DISTINCT d.dict_type
FROM system_dict_data d
LEFT JOIN system_dict_type t ON t.type = d.dict_type AND t.deleted = b'0'
WHERE d.dict_type LIKE 'dict_%'
  AND d.deleted = b'0'
  AND t.id IS NULL;

-- -----------------------------------------------------------------------------
-- §4 回滚提示（不做自动回滚）
--   恢复 mysqldump 备份，或：
--   DELETE FROM system_dict_data  WHERE creator = 'ops-dict-merge';
--   DELETE FROM system_dict_type  WHERE creator = 'ops-dict-merge';
--   （仅当 §1/§2 未改 creator 时安全；生产请用备份恢复）
-- =============================================================================
