-- =============================================================================
-- System DB (shenyu-system) — V183 work-task dictionaries (@InDict SSOT)
-- Source: V183__m2_work_task_menu_dict_fix.sql (§2)
-- Version: 2026-08-19 · Apply on: {{SYSTEM_DB_HOST}}/{{SYSTEM_DB_NAME}}
-- Types: dict_marketing_plan_type · dict_sales_platform · dict_win_prediction · dict_work_task_sheet_status
-- =============================================================================
SET NAMES utf8mb4;

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '营销计划类型', 'dict_marketing_plan_type', 0, 'FR-M2-010 work-task', 'deploy-v183', NOW(), 'deploy-v183', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st
    WHERE st.type = 'dict_marketing_plan_type' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '销售平台', 'dict_sales_platform', 0, 'FR-M2-010 work-task', 'deploy-v183', NOW(), 'deploy-v183', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st
    WHERE st.type = 'dict_sales_platform' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '红黑预测', 'dict_win_prediction', 0, 'FR-M2-010 work-task', 'deploy-v183', NOW(), 'deploy-v183', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st
    WHERE st.type = 'dict_win_prediction' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '工作任务登记状态', 'dict_work_task_sheet_status', 0, 'FR-M2-010 work-task', 'deploy-v183', NOW(), 'deploy-v183', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st
    WHERE st.type = 'dict_work_task_sheet_status' AND st.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '直播公推', 'LIVE_PUBLIC', 'dict_marketing_plan_type', 0, 'success', '', NULL, 'deploy-v183', NOW(), 'deploy-v183', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd
    WHERE sd.dict_type = 'dict_marketing_plan_type' AND sd.value = 'LIVE_PUBLIC' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '付费销售', 'PAID_SALES', 'dict_marketing_plan_type', 0, 'warning', '', NULL, 'deploy-v183', NOW(), 'deploy-v183', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd
    WHERE sd.dict_type = 'dict_marketing_plan_type' AND sd.value = 'PAID_SALES' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '私域', 'PRIVATE', 'dict_sales_platform', 0, 'primary', '', NULL, 'deploy-v183', NOW(), 'deploy-v183', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd
    WHERE sd.dict_type = 'dict_sales_platform' AND sd.value = 'PRIVATE' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '快手', 'KUAISHOU', 'dict_sales_platform', 0, 'primary', '', NULL, 'deploy-v183', NOW(), 'deploy-v183', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd
    WHERE sd.dict_type = 'dict_sales_platform' AND sd.value = 'KUAISHOU' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '抖音', 'DOUYIN', 'dict_sales_platform', 0, 'primary', '', NULL, 'deploy-v183', NOW(), 'deploy-v183', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd
    WHERE sd.dict_type = 'dict_sales_platform' AND sd.value = 'DOUYIN' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 4, '无', 'NONE', 'dict_sales_platform', 0, 'info', '', NULL, 'deploy-v183', NOW(), 'deploy-v183', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd
    WHERE sd.dict_type = 'dict_sales_platform' AND sd.value = 'NONE' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '未知', 'UNKNOWN', 'dict_win_prediction', 0, 'info', '', NULL, 'deploy-v183', NOW(), 'deploy-v183', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd
    WHERE sd.dict_type = 'dict_win_prediction' AND sd.value = 'UNKNOWN' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '红', 'RED', 'dict_win_prediction', 0, 'danger', '', NULL, 'deploy-v183', NOW(), 'deploy-v183', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd
    WHERE sd.dict_type = 'dict_win_prediction' AND sd.value = 'RED' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '黑', 'BLACK', 'dict_win_prediction', 0, 'default', '', NULL, 'deploy-v183', NOW(), 'deploy-v183', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd
    WHERE sd.dict_type = 'dict_win_prediction' AND sd.value = 'BLACK' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '草稿', 'DRAFT', 'dict_work_task_sheet_status', 0, 'info', '', NULL, 'deploy-v183', NOW(), 'deploy-v183', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd
    WHERE sd.dict_type = 'dict_work_task_sheet_status' AND sd.value = 'DRAFT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '已确认', 'CONFIRMED', 'dict_work_task_sheet_status', 0, 'success', '', NULL, 'deploy-v183', NOW(), 'deploy-v183', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd
    WHERE sd.dict_type = 'dict_work_task_sheet_status' AND sd.value = 'CONFIRMED' AND sd.deleted = b'0'
);
