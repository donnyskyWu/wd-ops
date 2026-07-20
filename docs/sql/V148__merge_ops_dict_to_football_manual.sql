-- =============================================================================
-- V148: Merge OPS business dict (wd.sys_dict_*) → Football (shenyu-system.system_dict_*)
-- =============================================================================
--
-- **执行方式**：手工脚本，**非 Flyway**。在维护窗内于 MySQL 客户端执行。
--
-- **前置条件**
--   1. 已对 shenyu-system 做全库备份（mysqldump / 快照）。
--   2. 确认 wd 与 shenyu-system 在同一 MySQL 实例（localhost:3306 集成环境）。
--   3. 建议在只读副本或 staging 先演练一遍。
--
-- **连接**：USE shenyu-system; 或在客户端默认库设为 shenyu-system。
--
-- **范围**：type LIKE 'dict_%' AND deleted = 0（共 97 个业务字典 type，UAT 2026-07-04 实测 type-list=94+）
--
-- **冲突策略**
--   - system_dict_type：type 已存在 → 跳过 INSERT
--   - system_dict_data：(dict_type, value) 已存在 → 跳过 INSERT
--
-- **列映射**
--   sys_dict_type.status  ENABLED/DISABLED  →  system_dict_type.status  0/1
--   sys_dict_data.dict_value                →  system_dict_data.value
--   sys_dict_data.status  ENABLED/DISABLED  →  system_dict_data.status  0/1
--
-- **关联文档**：docs/delivery/OPS-DICT-MERGE-FOOTBALL-PLAN.md
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 业务 dict_* type 清单（97，Flyway seed 汇总 2026-07-18）
-- -----------------------------------------------------------------------------
-- dict_account_status, dict_account_type, dict_ai_model_type, dict_ai_scene,
-- dict_alert_level, dict_anchor_style, dict_anchor_type, dict_aochuang_bind_status,
-- dict_aochuang_message_direction, dict_aochuang_message_type, dict_aochuang_sync_type,
-- dict_author_type, dict_collect_data_type, dict_collect_frequency, dict_collect_method,
-- dict_collect_source, dict_collect_status, dict_collector_bind_status, dict_company_status,
-- dict_compare_operator, dict_config_status, dict_conn_status, dict_content_body_format,
-- dict_content_import_type, dict_content_length_type, dict_content_review_result,
-- dict_content_status, dict_content_type, dict_cost_pay_method, dict_cost_period,
-- dict_cost_type, dict_dashboard_type, dict_data_source, dict_document_type,
-- dict_ecom_platform, dict_funnel_type, dict_gender, dict_id_type, dict_industry,
-- dict_intermediary_relation, dict_ip_group_level, dict_judge_mode, dict_knowledge_category,
-- dict_layout_import_job_status, dict_layout_style_category, dict_layout_style_status,
-- dict_layout_template_source, dict_layout_template_status, dict_log_level, dict_log_module,
-- dict_log_type, dict_match_type, dict_message_category, dict_message_status,
-- dict_metadata_entity_status, dict_metadata_query_condition_type, dict_monitor_freq,
-- dict_notify_channel, dict_param_category, dict_param_type, dict_perf_grade,
-- dict_perf_metric_type, dict_perf_period, dict_perf_status, dict_phone_status,
-- dict_phone_type, dict_plan_status, dict_platform_type, dict_position,
-- dict_private_domain_identity_type, dict_private_domain_match_method,
-- dict_private_domain_review_status, dict_prompt_type, dict_qualification_type,
-- dict_quality_check_type, dict_query_status, dict_realname_status, dict_review_stage,
-- dict_review_status, dict_roi_dimension, dict_scheme_type, dict_sim_operator,
-- dict_sim_status, dict_sop_node_status, dict_sop_node_type, dict_sync_frequency,
-- dict_sync_mode, dict_tenant_status, dict_third_platform, dict_threshold_category,
-- dict_threshold_metric, dict_threshold_type, dict_time_dimension, dict_triple_rel_type,
-- dict_user_status, dict_wechat_usage_status, dict_yes_no
-- -----------------------------------------------------------------------------

SET NAMES utf8mb4;

-- 可选：执行前快照计数
SELECT 'wd.sys_dict_type (dict_*)' AS label, COUNT(*) AS cnt
FROM wd.sys_dict_type
WHERE deleted = 0 AND type LIKE 'dict\_%';

SELECT 'wd.sys_dict_data (dict_*)' AS label, COUNT(*) AS cnt
FROM wd.sys_dict_data d
INNER JOIN wd.sys_dict_type t ON t.type = d.dict_type AND t.deleted = 0
WHERE d.deleted = 0 AND d.dict_type LIKE 'dict\_%';

-- =============================================================================
-- Step 1: 迁移 dict_type → system_dict_type（skip if type exists）
-- =============================================================================
INSERT INTO system_dict_type (
    name, type, status, remark,
    creator, create_time, updater, update_time, deleted
)
SELECT
    s.name,
    s.type,
    CASE s.status WHEN 'ENABLED' THEN 0 WHEN 'DISABLED' THEN 1 ELSE 0 END,
    NULL,
    COALESCE(s.creator, 'dict-merge'),
    s.create_time,
    COALESCE(s.updater, 'dict-merge'),
    s.update_time,
    b'0'
FROM wd.sys_dict_type s
WHERE s.deleted = 0
  AND s.type LIKE 'dict\_%'
  AND NOT EXISTS (
      SELECT 1 FROM system_dict_type t
      WHERE t.type = s.type
        AND t.deleted = b'0'
  );

-- =============================================================================
-- Step 2: 迁移 dict_data → system_dict_data（merge by dict_type + value, skip dup）
-- =============================================================================
INSERT INTO system_dict_data (
    sort, label, value, dict_type, status,
    color_type, css_class, remark,
    creator, create_time, updater, update_time, deleted
)
SELECT
    d.sort,
    d.label,
    d.dict_value,
    d.dict_type,
    CASE d.status WHEN 'ENABLED' THEN 0 WHEN 'DISABLED' THEN 1 ELSE 0 END,
    COALESCE(NULLIF(d.color_type, ''), 'default'),
    '',
    d.remark,
    COALESCE(d.creator, 'dict-merge'),
    d.create_time,
    COALESCE(d.updater, 'dict-merge'),
    d.update_time,
    b'0'
FROM wd.sys_dict_data d
INNER JOIN wd.sys_dict_type t
    ON t.type = d.dict_type AND t.deleted = 0
WHERE d.deleted = 0
  AND d.dict_type LIKE 'dict\_%'
  AND NOT EXISTS (
      SELECT 1 FROM system_dict_data x
      WHERE x.dict_type = d.dict_type
        AND x.value = d.dict_value
        AND x.deleted = b'0'
  );

-- =============================================================================
-- Step 3: 可选 — 对已存在 data 行刷新 label/sort/status（仅当 wd 更新更晚时）
-- 默认注释；若需覆盖 Football 侧陈旧 label，取消注释后单独执行。
-- =============================================================================
/*
UPDATE system_dict_data tgt
INNER JOIN wd.sys_dict_data src
    ON src.dict_type = tgt.dict_type
   AND src.dict_value = tgt.value
   AND src.deleted = 0
SET
    tgt.label = src.label,
    tgt.sort = src.sort,
    tgt.status = CASE src.status WHEN 'ENABLED' THEN 0 WHEN 'DISABLED' THEN 1 ELSE 0 END,
    tgt.color_type = COALESCE(NULLIF(src.color_type, ''), 'default'),
    tgt.remark = src.remark,
    tgt.updater = 'dict-merge',
    tgt.update_time = GREATEST(tgt.update_time, src.update_time)
WHERE src.dict_type LIKE 'dict\_%'
  AND tgt.deleted = b'0'
  AND src.update_time > tgt.update_time;
*/

-- =============================================================================
-- Step 4:  post-migration 验证
-- =============================================================================
SELECT 'system_dict_type (dict_*)' AS label, COUNT(*) AS cnt
FROM system_dict_type
WHERE deleted = b'0' AND type LIKE 'dict\_%';

SELECT 'system_dict_data (dict_*)' AS label, COUNT(*) AS cnt
FROM system_dict_data
WHERE deleted = b'0' AND dict_type LIKE 'dict\_%';

-- 列出 wd 有而 system 缺的 type（应为 0 行）
SELECT s.type, s.name
FROM wd.sys_dict_type s
WHERE s.deleted = 0
  AND s.type LIKE 'dict\_%'
  AND NOT EXISTS (
      SELECT 1 FROM system_dict_type t
      WHERE t.type = s.type AND t.deleted = b'0'
  );

-- 列出 wd 有而 system 缺的 data（应为 0 行）
SELECT d.dict_type, d.dict_value, d.label
FROM wd.sys_dict_data d
WHERE d.deleted = 0
  AND d.dict_type LIKE 'dict\_%'
  AND NOT EXISTS (
      SELECT 1 FROM system_dict_data x
      WHERE x.dict_type = d.dict_type
        AND x.value = d.dict_value
        AND x.deleted = b'0'
  );

-- 按 type 对比行数（抽样排查）
SELECT
    s.type,
    wd_cnt.cnt AS wd_data_cnt,
    sys_cnt.cnt AS system_data_cnt
FROM wd.sys_dict_type s
INNER JOIN (
    SELECT dict_type, COUNT(*) AS cnt
    FROM wd.sys_dict_data
    WHERE deleted = 0 AND dict_type LIKE 'dict\_%'
    GROUP BY dict_type
) wd_cnt ON wd_cnt.dict_type = s.type
LEFT JOIN (
    SELECT dict_type, COUNT(*) AS cnt
    FROM system_dict_data
    WHERE deleted = b'0' AND dict_type LIKE 'dict\_%'
    GROUP BY dict_type
) sys_cnt ON sys_cnt.dict_type = s.type
WHERE s.deleted = 0 AND s.type LIKE 'dict\_%'
  AND COALESCE(sys_cnt.cnt, 0) < wd_cnt.cnt
ORDER BY s.type;
