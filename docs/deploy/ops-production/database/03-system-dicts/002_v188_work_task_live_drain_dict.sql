-- =============================================================================
-- System DB (shenyu-system) — V188 dict: 营销计划「直播引流」LIVE_DRAIN
-- Source:
--   V188__m2_work_task_marketing_live_drain.sql (Flyway no-op — cross-DB)
--   scripts/integration-config/fix_local_work_task_menu.sql
-- Version: 2026-08-24 · Apply on: {{SYSTEM_DB_HOST}}/{{SYSTEM_DB_NAME}}
-- Idempotent: WHERE NOT EXISTS
-- =============================================================================
SET NAMES utf8mb4;
USE `{{SYSTEM_DB_NAME}}`;

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '直播引流', 'LIVE_DRAIN', 'dict_marketing_plan_type', 0, 'primary', '', NULL, 'deploy-v188', NOW(), 'deploy-v188', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd
    WHERE sd.dict_type = 'dict_marketing_plan_type' AND sd.value = 'LIVE_DRAIN' AND sd.deleted = b'0'
);
