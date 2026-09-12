-- =============================================================================
-- Production incremental deploy: shenyu-system database
--
-- Purpose:
--   Apply system dictionary changes for this week's release (ADR-074/077).
--   Flyway SSOT: football-module-ops-server/src/main/resources/db/migration/
--
-- Versions included (execution order):
--   V193 — ADR-074: dict_marketing_plan_type 三值替换
--   V196 — ADR-077: dict_ai_generate_status 字典类型与数据
--
-- Pre-requisite:
--   Production shenyu-system Flyway baseline at V191 (V191 及之前已执行).
--   dict_marketing_plan_type 字典类型须已存在（greenfield 02a 或历史 seed）。
--
-- Companion script:
--   prod-incremental-shenyu-ops-V192-V196.sql (OPS 表结构 V192/V194/V195/V196，在 shenyu-ops 库执行)
--
-- Note:
--   ADR-080（任务节点名称与执行页登记备注）无 SQL 变更，本脚本不包含。
--
-- Execution:
--   mysql -h<host> -u<user> -p --default-character-set=utf8mb4 shenyu-system < prod-incremental-shenyu-system-V193-V196.sql
--
-- Idempotency:
--   字典 INSERT 使用 NOT EXISTS 幂等；UPDATE soft-delete 重复执行安全。
--   若本机字典库名为 shenyu-sys（非 shenyu-system），请将下方 `shenyu-system` 替换为实际库名。
-- =============================================================================

-- ---------------------------------------------------------------------------
-- V193: ADR-074 dict_marketing_plan_type 三值替换（测试环境不迁移 assignment 历史）
-- ---------------------------------------------------------------------------
SET NAMES utf8mb4;

UPDATE `shenyu-system`.system_dict_data
SET deleted = b'1', updater = 'flyway-v193', update_time = NOW()
WHERE dict_type = 'dict_marketing_plan_type'
  AND value IN ('LIVE_PUBLIC', 'PAID_SALES', 'LIVE_DRAIN')
  AND deleted = b'0';

INSERT INTO `shenyu-system`.system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '快手付费课程', 'KUAISHOU_PAID_COURSE', 'dict_marketing_plan_type', 0, 'primary', '', 'ADR-074', 'flyway-v193', NOW(), 'flyway-v193', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM `shenyu-system`.system_dict_data sd
    WHERE sd.dict_type = 'dict_marketing_plan_type' AND sd.value = 'KUAISHOU_PAID_COURSE' AND sd.deleted = b'0'
);

INSERT INTO `shenyu-system`.system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '公司平台付费', 'COMPANY_PAID', 'dict_marketing_plan_type', 0, 'warning', '', 'ADR-074', 'flyway-v193', NOW(), 'flyway-v193', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM `shenyu-system`.system_dict_data sd
    WHERE sd.dict_type = 'dict_marketing_plan_type' AND sd.value = 'COMPANY_PAID' AND sd.deleted = b'0'
);

INSERT INTO `shenyu-system`.system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '免费公推', 'FREE_PUBLIC', 'dict_marketing_plan_type', 0, 'success', '', 'ADR-074', 'flyway-v193', NOW(), 'flyway-v193', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM `shenyu-system`.system_dict_data sd
    WHERE sd.dict_type = 'dict_marketing_plan_type' AND sd.value = 'FREE_PUBLIC' AND sd.deleted = b'0'
);

-- ---------------------------------------------------------------------------
-- V196 / ADR-077 Slice A: dict_ai_generate_status 字典 seed（shenyu-system 部分）
-- ---------------------------------------------------------------------------
SET NAMES utf8mb4;

INSERT INTO `shenyu-system`.system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT 'AI生成状态', 'dict_ai_generate_status', 0, 'ADR-077 S-21-A', 'flyway-v196', NOW(), 'flyway-v196', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM `shenyu-system`.system_dict_type st
    WHERE st.type = 'dict_ai_generate_status' AND st.deleted = b'0'
);

INSERT INTO `shenyu-system`.system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '排队中', 'QUEUED', 'dict_ai_generate_status', 0, 'info', '', 'confirm 后已入队', 'flyway-v196', NOW(), 'flyway-v196', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM `shenyu-system`.system_dict_data sd
    WHERE sd.dict_type = 'dict_ai_generate_status' AND sd.value = 'QUEUED' AND sd.deleted = b'0'
);

INSERT INTO `shenyu-system`.system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '生成中', 'GENERATING', 'dict_ai_generate_status', 0, 'warning', '', '正在调用 jingcai', 'flyway-v196', NOW(), 'flyway-v196', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM `shenyu-system`.system_dict_data sd
    WHERE sd.dict_type = 'dict_ai_generate_status' AND sd.value = 'GENERATING' AND sd.deleted = b'0'
);

INSERT INTO `shenyu-system`.system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '成功', 'SUCCESS', 'dict_ai_generate_status', 0, 'success', '', '已写入对应正文列', 'flyway-v196', NOW(), 'flyway-v196', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM `shenyu-system`.system_dict_data sd
    WHERE sd.dict_type = 'dict_ai_generate_status' AND sd.value = 'SUCCESS' AND sd.deleted = b'0'
);

INSERT INTO `shenyu-system`.system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 4, '失败', 'FAILED', 'dict_ai_generate_status', 0, 'danger', '', '可重试；ai_generate_error 非空', 'flyway-v196', NOW(), 'flyway-v196', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM `shenyu-system`.system_dict_data sd
    WHERE sd.dict_type = 'dict_ai_generate_status' AND sd.value = 'FAILED' AND sd.deleted = b'0'
);

-- =============================================================================
-- Optional: Flyway schema history records (manual deploy only; uncomment if needed)
-- Assumes flyway_schema_history table exists in shenyu-system and versions not yet recorded.
-- Note: V193/V196 Flyway scripts live in ops-server module but touch shenyu-system dict tables.
-- =============================================================================
-- INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
-- VALUES
--   ((SELECT COALESCE(MAX(installed_rank), 0) + 1 FROM flyway_schema_history fsh), '193', 'm2 work task marketing plan dict v074', 'SQL', 'V193__m2_work_task_marketing_plan_dict_v074.sql', NULL, 'manual-prod', NOW(), 0, 1),
--   ((SELECT COALESCE(MAX(installed_rank), 0) + 2 FROM flyway_schema_history fsh), '196', 'adr077 s21a sop node document type', 'SQL', 'V196__adr077_s21a_sop_node_document_type.sql', NULL, 'manual-prod', NOW(), 0, 1);
