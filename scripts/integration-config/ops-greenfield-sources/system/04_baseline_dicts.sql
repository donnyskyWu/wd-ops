-- =============================================================================
-- System DB (shenyu-system) — Ops baseline dict_* (G-DICT-01 / ADR-047)
-- Generated: 2026-08-27 by _gen_04_baseline_dicts.py — do not hand-edit
-- Sources:
--   docs/delivery/e2e-artifacts/B-WP4-ARCHIVE-20260731/backup/wd-q1-candidates-20260731.sql
--   CHECKLIST-M1 / GLOBAL-CONVENTIONS (dict_ip_group_type/status, dict_author_status, dict_anchor_type)
--   apply_v171_param_category.py · apply_v176_threshold_metric.py
--   ADR-067 live collect + M10-EXTERNAL slice §4.2 EXT_*
-- Target: {{SYSTEM_DB_HOST}}/{{SYSTEM_DB_NAME}}  (Football system_dict_*; status 0=enabled)
-- Idempotent: INSERT … WHERE NOT EXISTS
-- Excludes: dict_xx1 (test). Work-task 4 types + LIVE_DRAIN remain in 05/06.
-- =============================================================================
SET NAMES utf8mb4;

-- ----- dict types -----

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '账号状态', 'dict_account_status', 0, 'ops-greenfield:dict_account_status', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_account_status' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '账号类型', 'dict_account_type', 0, 'ops-greenfield:dict_account_type', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_account_type' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT 'AI模型类型', 'dict_ai_model_type', 0, 'ops-greenfield:dict_ai_model_type', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_ai_model_type' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT 'AI应用场景', 'dict_ai_scene', 0, 'ops-greenfield:dict_ai_scene', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_ai_scene' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '预警级别', 'dict_alert_level', 0, 'ops-greenfield:dict_alert_level', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_alert_level' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '主播风格', 'dict_anchor_style', 0, 'ops-greenfield:dict_anchor_style', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_anchor_style' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '主播类型', 'dict_anchor_type', 0, 'ops-greenfield:dict_anchor_type', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_anchor_type' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '奥创设备绑定状态', 'dict_aochuang_bind_status', 0, 'ops-greenfield:dict_aochuang_bind_status', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_aochuang_bind_status' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '奥创消息方向', 'dict_aochuang_message_direction', 0, 'ops-greenfield:dict_aochuang_message_direction', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_aochuang_message_direction' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '奥创消息类型', 'dict_aochuang_message_type', 0, 'ops-greenfield:dict_aochuang_message_type', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_aochuang_message_type' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '奥创同步类型', 'dict_aochuang_sync_type', 0, 'ops-greenfield:dict_aochuang_sync_type', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_aochuang_sync_type' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '作者状态', 'dict_author_status', 0, 'ops-greenfield:dict_author_status', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_author_status' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '作者类型', 'dict_author_type', 0, 'ops-greenfield:dict_author_type', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_author_type' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '采集数据类型', 'dict_collect_data_type', 0, 'ops-greenfield:dict_collect_data_type', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_collect_data_type' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '采集频率', 'dict_collect_frequency', 0, 'ops-greenfield:dict_collect_frequency', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_collect_frequency' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '采集方式', 'dict_collect_method', 0, 'ops-greenfield:dict_collect_method', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_collect_method' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '采集源', 'dict_collect_source', 0, 'ops-greenfield:dict_collect_source', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_collect_source' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '采集状态', 'dict_collect_status', 0, 'ops-greenfield:dict_collect_status', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_collect_status' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT 'Collector 绑定状态', 'dict_collector_bind_status', 0, 'ops-greenfield:dict_collector_bind_status', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_collector_bind_status' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '公司状态', 'dict_company_status', 0, 'ops-greenfield:dict_company_status', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_company_status' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '比较符', 'dict_compare_operator', 0, 'ops-greenfield:dict_compare_operator', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_compare_operator' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '配置状态', 'dict_config_status', 0, 'ops-greenfield:dict_config_status', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_config_status' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '连接状态', 'dict_conn_status', 0, 'ops-greenfield:dict_conn_status', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_conn_status' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT 'Content body format', 'dict_content_body_format', 0, 'ops-greenfield:dict_content_body_format', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_content_body_format' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '补录类型', 'dict_content_import_type', 0, 'ops-greenfield:dict_content_import_type', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_content_import_type' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '内容篇幅类型', 'dict_content_length_type', 0, 'ops-greenfield:dict_content_length_type', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_content_length_type' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '内容审核结果', 'dict_content_review_result', 0, 'ops-greenfield:dict_content_review_result', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_content_review_result' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '生产内容状态', 'dict_content_status', 0, 'ops-greenfield:dict_content_status', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_content_status' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '内容类型', 'dict_content_type', 0, 'ops-greenfield:dict_content_type', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_content_type' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '支付方式', 'dict_cost_pay_method', 0, 'ops-greenfield:dict_cost_pay_method', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_cost_pay_method' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '成本周期', 'dict_cost_period', 0, 'ops-greenfield:dict_cost_period', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_cost_period' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '成本类型', 'dict_cost_type', 0, 'ops-greenfield:dict_cost_type', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_cost_type' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '大屏类型', 'dict_dashboard_type', 0, 'ops-greenfield:dict_dashboard_type', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_dashboard_type' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '数据来源', 'dict_data_source', 0, 'ops-greenfield:dict_data_source', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_data_source' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '文档类型', 'dict_document_type', 0, 'ops-greenfield:dict_document_type', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_document_type' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '电商平台', 'dict_ecom_platform', 0, 'ops-greenfield:dict_ecom_platform', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_ecom_platform' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '漏斗类型', 'dict_funnel_type', 0, 'ops-greenfield:dict_funnel_type', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_funnel_type' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '性别', 'dict_gender', 0, 'ops-greenfield:dict_gender', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_gender' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '证件类型', 'dict_id_type', 0, 'ops-greenfield:dict_id_type', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_id_type' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '行业', 'dict_industry', 0, 'ops-greenfield:dict_industry', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_industry' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '中介人关系类型', 'dict_intermediary_relation', 0, 'ops-greenfield:dict_intermediary_relation', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_intermediary_relation' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT 'IP组等级', 'dict_ip_group_level', 0, 'ops-greenfield:dict_ip_group_level', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_ip_group_level' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT 'IP组状态', 'dict_ip_group_status', 0, 'ops-greenfield:dict_ip_group_status', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_ip_group_status' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT 'IP组类型', 'dict_ip_group_type', 0, 'ops-greenfield:dict_ip_group_type', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_ip_group_type' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '判定模式', 'dict_judge_mode', 0, 'ops-greenfield:dict_judge_mode', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_judge_mode' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '知识库分类', 'dict_knowledge_category', 0, 'ops-greenfield:dict_knowledge_category', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_knowledge_category' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '版式导入任务状态', 'dict_layout_import_job_status', 0, 'ops-greenfield:dict_layout_import_job_status', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_layout_import_job_status' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT 'Layout style category', 'dict_layout_style_category', 0, 'ops-greenfield:dict_layout_style_category', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_layout_style_category' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT 'Layout style status', 'dict_layout_style_status', 0, 'ops-greenfield:dict_layout_style_status', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_layout_style_status' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT 'Layout template source', 'dict_layout_template_source', 0, 'ops-greenfield:dict_layout_template_source', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_layout_template_source' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT 'Layout template status', 'dict_layout_template_status', 0, 'ops-greenfield:dict_layout_template_status', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_layout_template_status' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '日志级别', 'dict_log_level', 0, 'ops-greenfield:dict_log_level', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_log_level' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '日志模块', 'dict_log_module', 0, 'ops-greenfield:dict_log_module', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_log_module' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '日志类型', 'dict_log_type', 0, 'ops-greenfield:dict_log_type', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_log_type' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '关键词匹配类型', 'dict_match_type', 0, 'ops-greenfield:dict_match_type', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_match_type' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '消息分类', 'dict_message_category', 0, 'ops-greenfield:dict_message_category', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_message_category' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '消息状态', 'dict_message_status', 0, 'ops-greenfield:dict_message_status', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_message_status' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '元数据实体状态', 'dict_metadata_entity_status', 0, 'ops-greenfield:dict_metadata_entity_status', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_metadata_entity_status' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '元数据查询条件类别', 'dict_metadata_query_condition_type', 0, 'ops-greenfield:dict_metadata_query_condition_type', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_metadata_query_condition_type' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '监测频率', 'dict_monitor_freq', 0, 'ops-greenfield:dict_monitor_freq', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_monitor_freq' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '通知渠道', 'dict_notify_channel', 0, 'ops-greenfield:dict_notify_channel', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_notify_channel' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '参数分类', 'dict_param_category', 0, 'ops-greenfield:dict_param_category', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_param_category' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '参数类型', 'dict_param_type', 0, 'ops-greenfield:dict_param_type', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_param_type' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '绩效等级', 'dict_perf_grade', 0, 'ops-greenfield:dict_perf_grade', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_perf_grade' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '指标类型', 'dict_perf_metric_type', 0, 'ops-greenfield:dict_perf_metric_type', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_perf_metric_type' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '考核周期', 'dict_perf_period', 0, 'ops-greenfield:dict_perf_period', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_perf_period' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '考核状态', 'dict_perf_status', 0, 'ops-greenfield:dict_perf_status', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_perf_status' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '手机状态', 'dict_phone_status', 0, 'ops-greenfield:dict_phone_status', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_phone_status' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '手机类型', 'dict_phone_type', 0, 'ops-greenfield:dict_phone_type', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_phone_type' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '计划状态', 'dict_plan_status', 0, 'ops-greenfield:dict_plan_status', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_plan_status' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '平台类型', 'dict_platform_type', 0, 'ops-greenfield:dict_platform_type', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_platform_type' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '岗位', 'dict_position', 0, 'ops-greenfield:dict_position', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_position' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '私域身份类型', 'dict_private_domain_identity_type', 0, 'ops-greenfield:dict_private_domain_identity_type', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_private_domain_identity_type' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '私域匹配方式', 'dict_private_domain_match_method', 0, 'ops-greenfield:dict_private_domain_match_method', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_private_domain_match_method' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '私域桥接审核状态', 'dict_private_domain_review_status', 0, 'ops-greenfield:dict_private_domain_review_status', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_private_domain_review_status' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '提示词类型', 'dict_prompt_type', 0, 'ops-greenfield:dict_prompt_type', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_prompt_type' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '资质类型', 'dict_qualification_type', 0, 'ops-greenfield:dict_qualification_type', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_qualification_type' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '数据质量检查类型', 'dict_quality_check_type', 0, 'ops-greenfield:dict_quality_check_type', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_quality_check_type' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '数据质量等级', 'dict_quality_level', 0, 'ops-greenfield:dict_quality_level', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_quality_level' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '查询状态', 'dict_query_status', 0, 'ops-greenfield:dict_query_status', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_query_status' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '实名人状态', 'dict_realname_status', 0, 'ops-greenfield:dict_realname_status', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_realname_status' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '审核阶段', 'dict_review_stage', 0, 'ops-greenfield:dict_review_stage', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_review_stage' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '审核状态', 'dict_review_status', 0, 'ops-greenfield:dict_review_status', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_review_status' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT 'ROI分析维度', 'dict_roi_dimension', 0, 'ops-greenfield:dict_roi_dimension', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_roi_dimension' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '赛事方案类型', 'dict_scheme_type', 0, 'ops-greenfield:dict_scheme_type', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_scheme_type' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT 'SIM运营商', 'dict_sim_operator', 0, 'ops-greenfield:dict_sim_operator', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_sim_operator' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '手机卡状态', 'dict_sim_status', 0, 'ops-greenfield:dict_sim_status', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_sim_status' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT 'SOP任务状态', 'dict_sop_node_status', 0, 'ops-greenfield:dict_sop_node_status', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_sop_node_status' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT 'SOP节点类型', 'dict_sop_node_type', 0, 'ops-greenfield:dict_sop_node_type', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_sop_node_type' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '同步频率', 'dict_sync_frequency', 0, 'ops-greenfield:dict_sync_frequency', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_sync_frequency' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '采集同步模式', 'dict_sync_mode', 0, 'ops-greenfield:dict_sync_mode', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_sync_mode' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '租户状态', 'dict_tenant_status', 0, 'ops-greenfield:dict_tenant_status', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_tenant_status' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '第三方平台', 'dict_third_platform', 0, 'ops-greenfield:dict_third_platform', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_third_platform' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '阈值分类', 'dict_threshold_category', 0, 'ops-greenfield:dict_threshold_category', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_threshold_category' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '阈值指标', 'dict_threshold_metric', 0, 'ops-greenfield:dict_threshold_metric', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_threshold_metric' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '阈值类型', 'dict_threshold_type', 0, 'ops-greenfield:dict_threshold_type', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_threshold_type' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '时间维度', 'dict_time_dimension', 0, 'ops-greenfield:dict_time_dimension', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_time_dimension' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '三方关联类型', 'dict_triple_rel_type', 0, 'ops-greenfield:dict_triple_rel_type', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_triple_rel_type' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '用户状态', 'dict_user_status', 0, 'ops-greenfield:dict_user_status', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_user_status' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '公众号使用状态', 'dict_wechat_usage_status', 0, 'ops-greenfield:dict_wechat_usage_status', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_wechat_usage_status' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '是否', 'dict_yes_no', 0, 'ops-greenfield:dict_yes_no', 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st WHERE st.type = 'dict_yes_no' AND st.deleted = b'0'
);

-- ----- dict data -----

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '正常', 'NORMAL', 'dict_account_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_account_status' AND sd.value = 'NORMAL' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '停用', 'DISABLED', 'dict_account_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_account_status' AND sd.value = 'DISABLED' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '官方账号', 'OFFICIAL_ACCOUNT', 'dict_account_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_account_type' AND sd.value = 'OFFICIAL_ACCOUNT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '个人账号', 'PERSONAL_ACCOUNT', 'dict_account_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_account_type' AND sd.value = 'PERSONAL_ACCOUNT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '服务号', 'SERVICE_ACCOUNT', 'dict_account_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_account_type' AND sd.value = 'SERVICE_ACCOUNT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 4, '订阅号', 'SUBSCRIPTION_ACCOUNT', 'dict_account_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_account_type' AND sd.value = 'SUBSCRIPTION_ACCOUNT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '通义千问', 'QWEN', 'dict_ai_model_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_ai_model_type' AND sd.value = 'QWEN' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '文心一言', 'ERNIE', 'dict_ai_model_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_ai_model_type' AND sd.value = 'ERNIE' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '智谱 AI', 'GLM', 'dict_ai_model_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_ai_model_type' AND sd.value = 'GLM' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 4, 'DeepSeek', 'DEEPSEEK', 'dict_ai_model_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_ai_model_type' AND sd.value = 'DEEPSEEK' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 5, 'Kimi', 'KIMI', 'dict_ai_model_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_ai_model_type' AND sd.value = 'KIMI' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 6, '豆包', 'DOUBAO', 'dict_ai_model_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_ai_model_type' AND sd.value = 'DOUBAO' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 7, 'OpenAI GPT', 'GPT', 'dict_ai_model_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_ai_model_type' AND sd.value = 'GPT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 8, 'Claude', 'CLAUDE', 'dict_ai_model_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_ai_model_type' AND sd.value = 'CLAUDE' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 9, 'Gemini', 'GEMINI', 'dict_ai_model_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_ai_model_type' AND sd.value = 'GEMINI' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 10, '月之暗面', 'MOONSHOT', 'dict_ai_model_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_ai_model_type' AND sd.value = 'MOONSHOT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '内容生成', 'CONTENT_GEN', 'dict_ai_scene', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_ai_scene' AND sd.value = 'CONTENT_GEN' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '标题优化', 'TITLE_OPT', 'dict_ai_scene', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_ai_scene' AND sd.value = 'TITLE_OPT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 10, '短视频文案', 'SHORT_VIDEO', 'dict_ai_scene', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_ai_scene' AND sd.value = 'SHORT_VIDEO' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 11, '直播脚本', 'LIVE_SCRIPT', 'dict_ai_scene', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_ai_scene' AND sd.value = 'LIVE_SCRIPT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 12, '小红书笔记', 'XIAOHONGSHU', 'dict_ai_scene', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_ai_scene' AND sd.value = 'XIAOHONGSHU' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 13, '公众号文章', 'WECHAT_ARTICLE', 'dict_ai_scene', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_ai_scene' AND sd.value = 'WECHAT_ARTICLE' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 14, '数据分析', 'DATA_ANALYSIS', 'dict_ai_scene', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_ai_scene' AND sd.value = 'DATA_ANALYSIS' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 15, '周报月报', 'REPORT', 'dict_ai_scene', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_ai_scene' AND sd.value = 'REPORT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 16, '竞品分析', 'COMPETITOR', 'dict_ai_scene', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_ai_scene' AND sd.value = 'COMPETITOR' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 17, 'AI内容对话', 'AI_CONTENT_CHAT', 'dict_ai_scene', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_ai_scene' AND sd.value = 'AI_CONTENT_CHAT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 18, '内容生成', 'CONTENT_GENERATE', 'dict_ai_scene', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_ai_scene' AND sd.value = 'CONTENT_GENERATE' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '提示', 'INFO', 'dict_alert_level', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_alert_level' AND sd.value = 'INFO' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '低', 'LOW', 'dict_alert_level', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_alert_level' AND sd.value = 'LOW' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '中', 'MEDIUM', 'dict_alert_level', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_alert_level' AND sd.value = 'MEDIUM' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '警告', 'WARNING', 'dict_alert_level', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_alert_level' AND sd.value = 'WARNING' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '紧急', 'CRITICAL', 'dict_alert_level', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_alert_level' AND sd.value = 'CRITICAL' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '高', 'HIGH', 'dict_alert_level', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_alert_level' AND sd.value = 'HIGH' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '激进型', 'aggressive', 'dict_anchor_style', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_anchor_style' AND sd.value = 'aggressive' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '稳健型', 'conservative', 'dict_anchor_style', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_anchor_style' AND sd.value = 'conservative' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '数据型', 'data', 'dict_anchor_style', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_anchor_style' AND sd.value = 'data' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 4, '情感型', 'emotional', 'dict_anchor_style', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_anchor_style' AND sd.value = 'emotional' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 5, '综合分析型', 'comprehensive', 'dict_anchor_style', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_anchor_style' AND sd.value = 'comprehensive' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 0, '直播', 'LIVE', 'dict_anchor_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_anchor_type' AND sd.value = 'LIVE' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '未绑定', 'UNBOUND', 'dict_aochuang_bind_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_aochuang_bind_status' AND sd.value = 'UNBOUND' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '自动绑定', 'AUTO', 'dict_aochuang_bind_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_aochuang_bind_status' AND sd.value = 'AUTO' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '手工绑定', 'MANUAL', 'dict_aochuang_bind_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_aochuang_bind_status' AND sd.value = 'MANUAL' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 4, '待绑定', 'PENDING', 'dict_aochuang_bind_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_aochuang_bind_status' AND sd.value = 'PENDING' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '发送', 'SENT', 'dict_aochuang_message_direction', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_aochuang_message_direction' AND sd.value = 'SENT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '接收', 'RECEIVED', 'dict_aochuang_message_direction', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_aochuang_message_direction' AND sd.value = 'RECEIVED' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '文本', 'TEXT', 'dict_aochuang_message_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_aochuang_message_type' AND sd.value = 'TEXT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '图片', 'IMAGE', 'dict_aochuang_message_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_aochuang_message_type' AND sd.value = 'IMAGE' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '语音', 'VOICE', 'dict_aochuang_message_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_aochuang_message_type' AND sd.value = 'VOICE' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 4, '视频', 'VIDEO', 'dict_aochuang_message_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_aochuang_message_type' AND sd.value = 'VIDEO' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 5, '其他', 'OTHER', 'dict_aochuang_message_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_aochuang_message_type' AND sd.value = 'OTHER' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '好友', 'FRIENDS', 'dict_aochuang_sync_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_aochuang_sync_type' AND sd.value = 'FRIENDS' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '消息', 'MESSAGES', 'dict_aochuang_sync_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_aochuang_sync_type' AND sd.value = 'MESSAGES' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '启用', 'ENABLED', 'dict_author_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_author_status' AND sd.value = 'ENABLED' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '停用', 'DISABLED', 'dict_author_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_author_status' AND sd.value = 'DISABLED' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 0, '直播', 'LIVE', 'dict_author_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_author_type' AND sd.value = 'LIVE' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '短视频', 'SHORT_VIDEO', 'dict_author_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_author_type' AND sd.value = 'SHORT_VIDEO' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '图文', 'ARTICLE', 'dict_author_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_author_type' AND sd.value = 'ARTICLE' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '直播+短视频', 'BOTH', 'dict_author_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_author_type' AND sd.value = 'BOTH' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 4, '图文', 'IMAGE_TEXT', 'dict_author_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_author_type' AND sd.value = 'IMAGE_TEXT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '公众号粉丝', 'MP_FOLLOWER_LIST', 'dict_collect_data_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_collect_data_type' AND sd.value = 'MP_FOLLOWER_LIST' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '公众号图文', 'MP_ARTICLE_LIST', 'dict_collect_data_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_collect_data_type' AND sd.value = 'MP_ARTICLE_LIST' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '公众号粉丝统计', 'MP_FOLLOWER_STATS', 'dict_collect_data_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_collect_data_type' AND sd.value = 'MP_FOLLOWER_STATS' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '企微日统计', 'WECOM_DAILY_STATS', 'dict_collect_data_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_collect_data_type' AND sd.value = 'WECOM_DAILY_STATS' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 4, '抖音粉丝列表', 'DOUYIN_FOLLOWER_LIST', 'dict_collect_data_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_collect_data_type' AND sd.value = 'DOUYIN_FOLLOWER_LIST' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 4, '公众号图文明细', 'MP_ARTICLE_STATS', 'dict_collect_data_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_collect_data_type' AND sd.value = 'MP_ARTICLE_STATS' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 5, '抖音作品列表', 'DOUYIN_VIDEO_LIST', 'dict_collect_data_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_collect_data_type' AND sd.value = 'DOUYIN_VIDEO_LIST' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 5, '公众号图文内容', 'MP_ARTICLE_CONTENT', 'dict_collect_data_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_collect_data_type' AND sd.value = 'MP_ARTICLE_CONTENT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 6, '抖音作品明细', 'DOUYIN_VIDEO_STATS', 'dict_collect_data_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_collect_data_type' AND sd.value = 'DOUYIN_VIDEO_STATS' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 7, '视频号作品列表', 'WECHAT_VIDEO_LIST', 'dict_collect_data_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_collect_data_type' AND sd.value = 'WECHAT_VIDEO_LIST' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 8, '视频号作品明细', 'WECHAT_VIDEO_STATS', 'dict_collect_data_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_collect_data_type' AND sd.value = 'WECHAT_VIDEO_STATS' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 9, '快手作品列表', 'KUAISHOU_VIDEO_LIST', 'dict_collect_data_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_collect_data_type' AND sd.value = 'KUAISHOU_VIDEO_LIST' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 10, '快手作品明细', 'KUAISHOU_VIDEO_STATS', 'dict_collect_data_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_collect_data_type' AND sd.value = 'KUAISHOU_VIDEO_STATS' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 11, '小红书笔记列表', 'XIAOHONGSHU_NOTE_LIST', 'dict_collect_data_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_collect_data_type' AND sd.value = 'XIAOHONGSHU_NOTE_LIST' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 12, '小红书笔记明细', 'XIAOHONGSHU_NOTE_STATS', 'dict_collect_data_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_collect_data_type' AND sd.value = 'XIAOHONGSHU_NOTE_STATS' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 20, '快手竞品作品列表', 'EXT_KUAISHOU_USER_VIDEOS', 'dict_collect_data_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_collect_data_type' AND sd.value = 'EXT_KUAISHOU_USER_VIDEOS' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 21, '公众号搜索', 'EXT_WECHAT_MP_SEARCH', 'dict_collect_data_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_collect_data_type' AND sd.value = 'EXT_WECHAT_MP_SEARCH' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 22, '公众号图文列表', 'EXT_WECHAT_MP_ARTICLE_LIST', 'dict_collect_data_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_collect_data_type' AND sd.value = 'EXT_WECHAT_MP_ARTICLE_LIST' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 23, '抖音用户资料', 'EXT_DOUYIN_USER_PROFILE', 'dict_collect_data_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_collect_data_type' AND sd.value = 'EXT_DOUYIN_USER_PROFILE' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 24, '抖音竞品作品列表', 'EXT_DOUYIN_USER_VIDEOS', 'dict_collect_data_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_collect_data_type' AND sd.value = 'EXT_DOUYIN_USER_VIDEOS' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 25, '视频号用户', 'EXT_WECHAT_VIDEO_USER', 'dict_collect_data_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_collect_data_type' AND sd.value = 'EXT_WECHAT_VIDEO_USER' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 26, '视频号作品列表', 'EXT_WECHAT_VIDEO_WORK_LIST', 'dict_collect_data_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_collect_data_type' AND sd.value = 'EXT_WECHAT_VIDEO_WORK_LIST' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 27, '视频号粉丝统计', 'EXT_WECHAT_VIDEO_FOLLOWER_STATS', 'dict_collect_data_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_collect_data_type' AND sd.value = 'EXT_WECHAT_VIDEO_FOLLOWER_STATS' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 30, '抖音直播列表', 'DOUYIN_LIVE_LIST', 'dict_collect_data_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_collect_data_type' AND sd.value = 'DOUYIN_LIVE_LIST' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 31, '抖音直播明细', 'DOUYIN_LIVE_STATS', 'dict_collect_data_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_collect_data_type' AND sd.value = 'DOUYIN_LIVE_STATS' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 32, '视频号直播列表', 'WECHAT_VIDEO_LIVE_LIST', 'dict_collect_data_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_collect_data_type' AND sd.value = 'WECHAT_VIDEO_LIVE_LIST' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 33, '视频号直播明细', 'WECHAT_VIDEO_LIVE_STATS', 'dict_collect_data_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_collect_data_type' AND sd.value = 'WECHAT_VIDEO_LIVE_STATS' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '每日', 'DAILY', 'dict_collect_frequency', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_collect_frequency' AND sd.value = 'DAILY' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '每小时', 'HOURLY', 'dict_collect_frequency', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_collect_frequency' AND sd.value = 'HOURLY' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '每周', 'WEEKLY', 'dict_collect_frequency', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_collect_frequency' AND sd.value = 'WEEKLY' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '内部采集', 'INTERNAL', 'dict_collect_method', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_collect_method' AND sd.value = 'INTERNAL' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, 'API对接', 'API', 'dict_collect_method', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_collect_method' AND sd.value = 'API' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '爬虫', 'CRAWLER', 'dict_collect_method', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_collect_method' AND sd.value = 'CRAWLER' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 4, '外部竞品', 'EXTERNAL', 'dict_collect_method', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_collect_method' AND sd.value = 'EXTERNAL' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '公众号 API', 'WECHAT_MP_API', 'dict_collect_source', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_collect_source' AND sd.value = 'WECHAT_MP_API' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '视频号 API', 'WECHAT_CHANNELS_API', 'dict_collect_source', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_collect_source' AND sd.value = 'WECHAT_CHANNELS_API' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '抖音开放平台', 'DOUYIN_OPEN_API', 'dict_collect_source', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_collect_source' AND sd.value = 'DOUYIN_OPEN_API' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 4, '奥创接口', 'AOCHUANG_API', 'dict_collect_source', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_collect_source' AND sd.value = 'AOCHUANG_API' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 5, '企微 API', 'WECOM_API', 'dict_collect_source', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_collect_source' AND sd.value = 'WECOM_API' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 6, '个微 API', 'PERSONAL_WECHAT_API', 'dict_collect_source', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_collect_source' AND sd.value = 'PERSONAL_WECHAT_API' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 7, '快手 Cookie 采集', 'KUAISHOU_OPEN_API', 'dict_collect_source', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_collect_source' AND sd.value = 'KUAISHOU_OPEN_API' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 8, '小红书 Cookie 采集', 'XIAOHONGSHU_OPEN_API', 'dict_collect_source', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_collect_source' AND sd.value = 'XIAOHONGSHU_OPEN_API' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 9, 'Bilibili Cookie 采集', 'BILIBILI_OPEN_API', 'dict_collect_source', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_collect_source' AND sd.value = 'BILIBILI_OPEN_API' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 10, '统一采集-外部竞品', 'UNIFY_COLLECTOR_EXTERNAL', 'dict_collect_source', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_collect_source' AND sd.value = 'UNIFY_COLLECTOR_EXTERNAL' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '待执行', 'PENDING', 'dict_collect_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_collect_status' AND sd.value = 'PENDING' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '执行中', 'RUNNING', 'dict_collect_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_collect_status' AND sd.value = 'RUNNING' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '成功', 'SUCCESS', 'dict_collect_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_collect_status' AND sd.value = 'SUCCESS' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 4, '失败', 'FAILED', 'dict_collect_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_collect_status' AND sd.value = 'FAILED' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 5, '部分成功', 'PARTIAL', 'dict_collect_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_collect_status' AND sd.value = 'PARTIAL' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 6, '已停止', 'STOPPED', 'dict_collect_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_collect_status' AND sd.value = 'STOPPED' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '已绑定', 'BOUND', 'dict_collector_bind_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_collector_bind_status' AND sd.value = 'BOUND' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '待绑定', 'PENDING', 'dict_collector_bind_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_collector_bind_status' AND sd.value = 'PENDING' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '绑定失败', 'FAILED', 'dict_collector_bind_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_collector_bind_status' AND sd.value = 'FAILED' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '启用', 'ENABLED', 'dict_company_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_company_status' AND sd.value = 'ENABLED' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '停用', 'DISABLED', 'dict_company_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_company_status' AND sd.value = 'DISABLED' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '大于等于', 'GTE', 'dict_compare_operator', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_compare_operator' AND sd.value = 'GTE' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '小于等于', 'LTE', 'dict_compare_operator', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_compare_operator' AND sd.value = 'LTE' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '等于', 'EQ', 'dict_compare_operator', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_compare_operator' AND sd.value = 'EQ' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '启用', 'ENABLED', 'dict_config_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_config_status' AND sd.value = 'ENABLED' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '停用', 'DISABLED', 'dict_config_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_config_status' AND sd.value = 'DISABLED' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '已连接', 'CONNECTED', 'dict_conn_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_conn_status' AND sd.value = 'CONNECTED' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '未连接', 'DISCONNECTED', 'dict_conn_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_conn_status' AND sd.value = 'DISCONNECTED' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '连接正常', 'OK', 'dict_conn_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_conn_status' AND sd.value = 'OK' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 4, 'Token失效', 'TOKEN_FAIL', 'dict_conn_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_conn_status' AND sd.value = 'TOKEN_FAIL' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 5, '权限不足', 'PERMISSION_DENIED', 'dict_conn_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_conn_status' AND sd.value = 'PERMISSION_DENIED' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '纯文本', 'PLAIN', 'dict_content_body_format', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_content_body_format' AND sd.value = 'PLAIN' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '富版式', 'LAYOUT', 'dict_content_body_format', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_content_body_format' AND sd.value = 'LAYOUT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '接口异常', 'API_EXCEPTION', 'dict_content_import_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_content_import_type' AND sd.value = 'API_EXCEPTION' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '线下补录', 'OFFLINE', 'dict_content_import_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_content_import_type' AND sd.value = 'OFFLINE' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '短篇500字', 'SHORT', 'dict_content_length_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_content_length_type' AND sd.value = 'SHORT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '中篇1000字', 'MEDIUM', 'dict_content_length_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_content_length_type' AND sd.value = 'MEDIUM' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '长篇3000字', 'LONG', 'dict_content_length_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_content_length_type' AND sd.value = 'LONG' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '通过', 'APPROVE', 'dict_content_review_result', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_content_review_result' AND sd.value = 'APPROVE' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '驳回', 'REJECT', 'dict_content_review_result', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_content_review_result' AND sd.value = 'REJECT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '草稿', 'DRAFT', 'dict_content_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_content_status' AND sd.value = 'DRAFT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '待一级审核', 'PENDING_FIRST_REVIEW', 'dict_content_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_content_status' AND sd.value = 'PENDING_FIRST_REVIEW' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '待二级审核', 'PENDING_SECOND_REVIEW', 'dict_content_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_content_status' AND sd.value = 'PENDING_SECOND_REVIEW' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 4, '待终审', 'PENDING_FINAL_REVIEW', 'dict_content_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_content_status' AND sd.value = 'PENDING_FINAL_REVIEW' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 5, '已驳回', 'REJECTED', 'dict_content_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_content_status' AND sd.value = 'REJECTED' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 6, '待发布', 'PENDING_PUBLISH', 'dict_content_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_content_status' AND sd.value = 'PENDING_PUBLISH' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 7, '已发布草稿', 'PUBLISHED_DRAFT', 'dict_content_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_content_status' AND sd.value = 'PUBLISHED_DRAFT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 8, '已正式发布', 'FORMALLY_PUBLISHED', 'dict_content_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_content_status' AND sd.value = 'FORMALLY_PUBLISHED' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 9, '已发布', 'PUBLISHED', 'dict_content_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_content_status' AND sd.value = 'PUBLISHED' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 10, '已下架', 'UNPUBLISHED', 'dict_content_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_content_status' AND sd.value = 'UNPUBLISHED' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 11, '已完成', 'COMPLETED', 'dict_content_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_content_status' AND sd.value = 'COMPLETED' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 0, '全部', 'ALL', 'dict_content_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_content_type' AND sd.value = 'ALL' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '短视频', 'SHORT_VIDEO', 'dict_content_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_content_type' AND sd.value = 'SHORT_VIDEO' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '文章', 'ARTICLE', 'dict_content_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_content_type' AND sd.value = 'ARTICLE' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '直播', 'LIVE', 'dict_content_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_content_type' AND sd.value = 'LIVE' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '视频', 'VIDEO', 'dict_content_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_content_type' AND sd.value = 'VIDEO' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '微信', 'WECHAT', 'dict_cost_pay_method', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_cost_pay_method' AND sd.value = 'WECHAT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '支付宝', 'ALIPAY', 'dict_cost_pay_method', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_cost_pay_method' AND sd.value = 'ALIPAY' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '银行卡', 'BANK', 'dict_cost_pay_method', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_cost_pay_method' AND sd.value = 'BANK' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 4, '对公转账', 'CORPORATE', 'dict_cost_pay_method', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_cost_pay_method' AND sd.value = 'CORPORATE' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '一次性', 'ONCE', 'dict_cost_period', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_cost_period' AND sd.value = 'ONCE' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '月度', 'MONTH', 'dict_cost_period', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_cost_period' AND sd.value = 'MONTH' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '季度', 'QUARTER', 'dict_cost_period', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_cost_period' AND sd.value = 'QUARTER' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '购买成本', 'PURCHASE', 'dict_cost_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_cost_type' AND sd.value = 'PURCHASE' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '人力成本', 'PROCESS_HUMAN', 'dict_cost_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_cost_type' AND sd.value = 'PROCESS_HUMAN' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '投放成本', 'AD_SPEND', 'dict_cost_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_cost_type' AND sd.value = 'AD_SPEND' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '业务大屏', 'BUSINESS', 'dict_dashboard_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_dashboard_type' AND sd.value = 'BUSINESS' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '运营大屏', 'OPS', 'dict_dashboard_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_dashboard_type' AND sd.value = 'OPS' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, 'API采集', 'API', 'dict_data_source', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_data_source' AND sd.value = 'API' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '手工补录', 'IMPORT', 'dict_data_source', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_data_source' AND sd.value = 'IMPORT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '短视频文案', 'SHORT_VIDEO_SCRIPT', 'dict_document_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_document_type' AND sd.value = 'SHORT_VIDEO_SCRIPT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '新号引流', 'NEW_ACCOUNT_TRAFFIC', 'dict_document_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_document_type' AND sd.value = 'NEW_ACCOUNT_TRAFFIC' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '赛后复盘', 'POST_MATCH_REVIEW', 'dict_document_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_document_type' AND sd.value = 'POST_MATCH_REVIEW' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 4, '正式方案', 'OFFICIAL_PLAN', 'dict_document_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_document_type' AND sd.value = 'OFFICIAL_PLAN' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 5, '预热前瞻', 'PREHEAT_PREVIEW', 'dict_document_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_document_type' AND sd.value = 'PREHEAT_PREVIEW' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '淘宝', 'TAOBAO', 'dict_ecom_platform', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_ecom_platform' AND sd.value = 'TAOBAO' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '京东', 'JD', 'dict_ecom_platform', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_ecom_platform' AND sd.value = 'JD' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '拼多多', 'PDD', 'dict_ecom_platform', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_ecom_platform' AND sd.value = 'PDD' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '自定义', 'CUSTOM', 'dict_funnel_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_funnel_type' AND sd.value = 'CUSTOM' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '转化', 'CONVERSION', 'dict_funnel_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_funnel_type' AND sd.value = 'CONVERSION' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '私域转化', 'PRIVATE_DOMAIN', 'dict_funnel_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_funnel_type' AND sd.value = 'PRIVATE_DOMAIN' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '男', 'MALE', 'dict_gender', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_gender' AND sd.value = 'MALE' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '女', 'FEMALE', 'dict_gender', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_gender' AND sd.value = 'FEMALE' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '身份证', 'ID_CARD', 'dict_id_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_id_type' AND sd.value = 'ID_CARD' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '护照', 'PASSPORT', 'dict_id_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_id_type' AND sd.value = 'PASSPORT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '新媒体', 'new_media', 'dict_industry', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_industry' AND sd.value = 'new_media' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, 'MCN', 'mcn', 'dict_industry', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_industry' AND sd.value = 'mcn' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '文化传媒', 'media', 'dict_industry', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_industry' AND sd.value = 'media' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 4, '教育', 'education', 'dict_industry', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_industry' AND sd.value = 'education' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 5, '电商', 'ecommerce', 'dict_industry', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_industry' AND sd.value = 'ecommerce' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '直签', 'DIRECT', 'dict_intermediary_relation', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_intermediary_relation' AND sd.value = 'DIRECT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '中介代理', 'INTERMEDIARY', 'dict_intermediary_relation', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_intermediary_relation' AND sd.value = 'INTERMEDIARY' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '机构合作', 'AGENCY', 'dict_intermediary_relation', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_intermediary_relation' AND sd.value = 'AGENCY' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, 'S级', 'S', 'dict_ip_group_level', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_ip_group_level' AND sd.value = 'S' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, 'A级', 'A', 'dict_ip_group_level', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_ip_group_level' AND sd.value = 'A' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, 'B级', 'B', 'dict_ip_group_level', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_ip_group_level' AND sd.value = 'B' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 4, 'C级', 'C', 'dict_ip_group_level', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_ip_group_level' AND sd.value = 'C' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '启用', 'ENABLED', 'dict_ip_group_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_ip_group_status' AND sd.value = 'ENABLED' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '停用', 'DISABLED', 'dict_ip_group_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_ip_group_status' AND sd.value = 'DISABLED' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '大组', 'BIG', 'dict_ip_group_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_ip_group_type' AND sd.value = 'BIG' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '小组', 'SMALL', 'dict_ip_group_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_ip_group_type' AND sd.value = 'SMALL' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '全部满足', 'AND', 'dict_judge_mode', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_judge_mode' AND sd.value = 'AND' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '任一满足', 'OR', 'dict_judge_mode', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_judge_mode' AND sd.value = 'OR' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '模板库', 'TEMPLATE_LIB', 'dict_knowledge_category', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_knowledge_category' AND sd.value = 'TEMPLATE_LIB' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '运营技巧', 'OPS_TIPS', 'dict_knowledge_category', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_knowledge_category' AND sd.value = 'OPS_TIPS' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '案例库', 'CASE_LIB', 'dict_knowledge_category', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_knowledge_category' AND sd.value = 'CASE_LIB' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 4, '行业资料', 'INDUSTRY_LIB', 'dict_knowledge_category', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_knowledge_category' AND sd.value = 'INDUSTRY_LIB' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 5, '运营经验', 'EXPERIENCE_LIB', 'dict_knowledge_category', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_knowledge_category' AND sd.value = 'EXPERIENCE_LIB' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '待处理', 'PENDING', 'dict_layout_import_job_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_layout_import_job_status' AND sd.value = 'PENDING' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '处理中', 'RUNNING', 'dict_layout_import_job_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_layout_import_job_status' AND sd.value = 'RUNNING' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '成功', 'SUCCESS', 'dict_layout_import_job_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_layout_import_job_status' AND sd.value = 'SUCCESS' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 4, '失败', 'FAILED', 'dict_layout_import_job_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_layout_import_job_status' AND sd.value = 'FAILED' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '标题', 'HEADING', 'dict_layout_style_category', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_layout_style_category' AND sd.value = 'HEADING' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '正文', 'BODY', 'dict_layout_style_category', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_layout_style_category' AND sd.value = 'BODY' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '图文', 'IMAGE_TEXT', 'dict_layout_style_category', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_layout_style_category' AND sd.value = 'IMAGE_TEXT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 4, '引导', 'GUIDE', 'dict_layout_style_category', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_layout_style_category' AND sd.value = 'GUIDE' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 5, '分隔', 'DIVIDER', 'dict_layout_style_category', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_layout_style_category' AND sd.value = 'DIVIDER' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '已启用', 'ENABLED', 'dict_layout_style_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_layout_style_status' AND sd.value = 'ENABLED' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '已停用', 'DISABLED', 'dict_layout_style_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_layout_style_status' AND sd.value = 'DISABLED' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '手动创建', 'MANUAL', 'dict_layout_template_source', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_layout_template_source' AND sd.value = 'MANUAL' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '链接导入', 'URL', 'dict_layout_template_source', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_layout_template_source' AND sd.value = 'URL' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, 'Word 导入', 'DOCX', 'dict_layout_template_source', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_layout_template_source' AND sd.value = 'DOCX' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 4, '粘贴导入', 'PASTE', 'dict_layout_template_source', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_layout_template_source' AND sd.value = 'PASTE' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 5, '系统预置', 'PRESET', 'dict_layout_template_source', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_layout_template_source' AND sd.value = 'PRESET' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '草稿', 'DRAFT', 'dict_layout_template_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_layout_template_status' AND sd.value = 'DRAFT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '已启用', 'ENABLED', 'dict_layout_template_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_layout_template_status' AND sd.value = 'ENABLED' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '已停用', 'DISABLED', 'dict_layout_template_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_layout_template_status' AND sd.value = 'DISABLED' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, 'DEBUG', 'DEBUG', 'dict_log_level', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_log_level' AND sd.value = 'DEBUG' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, 'INFO', 'INFO', 'dict_log_level', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_log_level' AND sd.value = 'INFO' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, 'WARN', 'WARN', 'dict_log_level', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_log_level' AND sd.value = 'WARN' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 4, 'ERROR', 'ERROR', 'dict_log_level', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_log_level' AND sd.value = 'ERROR' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '系统', 'SYSTEM', 'dict_log_module', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_log_module' AND sd.value = 'SYSTEM' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '用户', 'USER', 'dict_log_module', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_log_module' AND sd.value = 'USER' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '账号', 'ACCOUNT', 'dict_log_module', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_log_module' AND sd.value = 'ACCOUNT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 4, '内容', 'CONTENT', 'dict_log_module', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_log_module' AND sd.value = 'CONTENT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 5, '财务', 'FINANCE', 'dict_log_module', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_log_module' AND sd.value = 'FINANCE' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '登录', 'LOGIN', 'dict_log_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_log_type' AND sd.value = 'LOGIN' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '操作', 'OPERATION', 'dict_log_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_log_type' AND sd.value = 'OPERATION' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '异常', 'EXCEPTION', 'dict_log_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_log_type' AND sd.value = 'EXCEPTION' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 4, '审计', 'AUDIT', 'dict_log_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_log_type' AND sd.value = 'AUDIT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '模糊匹配', 'FUZZY', 'dict_match_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_match_type' AND sd.value = 'FUZZY' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '精确匹配', 'EXACT', 'dict_match_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_match_type' AND sd.value = 'EXACT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '预警通知', 'ALERT', 'dict_message_category', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_message_category' AND sd.value = 'ALERT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '系统通知', 'SYSTEM', 'dict_message_category', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_message_category' AND sd.value = 'SYSTEM' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '业务通知', 'BUSINESS', 'dict_message_category', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_message_category' AND sd.value = 'BUSINESS' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '待发送', 'PENDING', 'dict_message_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_message_status' AND sd.value = 'PENDING' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '已发送', 'SENT', 'dict_message_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_message_status' AND sd.value = 'SENT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '发送失败', 'FAILED', 'dict_message_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_message_status' AND sd.value = 'FAILED' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '启用', 'ENABLED', 'dict_metadata_entity_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_metadata_entity_status' AND sd.value = 'ENABLED' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '停用', 'DISABLED', 'dict_metadata_entity_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_metadata_entity_status' AND sd.value = 'DISABLED' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 10, '文本', 'TEXT', 'dict_metadata_query_condition_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_metadata_query_condition_type' AND sd.value = 'TEXT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 20, '数值', 'NUMBER', 'dict_metadata_query_condition_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_metadata_query_condition_type' AND sd.value = 'NUMBER' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 30, '日期', 'DATE', 'dict_metadata_query_condition_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_metadata_query_condition_type' AND sd.value = 'DATE' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 40, '日期范围', 'DATE_RANGE', 'dict_metadata_query_condition_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_metadata_query_condition_type' AND sd.value = 'DATE_RANGE' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 50, '枚举字典', 'DICT', 'dict_metadata_query_condition_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_metadata_query_condition_type' AND sd.value = 'DICT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 60, 'IP组选择', 'IP_GROUP_SELECT', 'dict_metadata_query_condition_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_metadata_query_condition_type' AND sd.value = 'IP_GROUP_SELECT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 70, '人员选择', 'USER_SELECT', 'dict_metadata_query_condition_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_metadata_query_condition_type' AND sd.value = 'USER_SELECT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 80, '平台选择', 'PLATFORM_SELECT', 'dict_metadata_query_condition_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_metadata_query_condition_type' AND sd.value = 'PLATFORM_SELECT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 90, '账号选择', 'ACCOUNT_SELECT', 'dict_metadata_query_condition_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_metadata_query_condition_type' AND sd.value = 'ACCOUNT_SELECT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 100, '赛事选择', 'COMPETITION_SELECT', 'dict_metadata_query_condition_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_metadata_query_condition_type' AND sd.value = 'COMPETITION_SELECT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '5分钟', '5MIN', 'dict_monitor_freq', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_monitor_freq' AND sd.value = '5MIN' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '30分钟', '30MIN', 'dict_monitor_freq', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_monitor_freq' AND sd.value = '30MIN' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '1小时', '1H', 'dict_monitor_freq', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_monitor_freq' AND sd.value = '1H' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 4, '24小时', '24H', 'dict_monitor_freq', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_monitor_freq' AND sd.value = '24H' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '站内消息', 'IN_APP', 'dict_notify_channel', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_notify_channel' AND sd.value = 'IN_APP' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '钉钉', 'DINGTALK', 'dict_notify_channel', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_notify_channel' AND sd.value = 'DINGTALK' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '短信', 'SMS', 'dict_notify_channel', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_notify_channel' AND sd.value = 'SMS' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '基础配置', 'BASIC', 'dict_param_category', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_param_category' AND sd.value = 'BASIC' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '采集配置', 'COLLECT', 'dict_param_category', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_param_category' AND sd.value = 'COLLECT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, 'AI配置', 'AI', 'dict_param_category', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_param_category' AND sd.value = 'AI' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 4, '通知配置', 'NOTIFICATION', 'dict_param_category', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_param_category' AND sd.value = 'NOTIFICATION' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 5, '内容审核', 'CONTENT_REVIEW', 'dict_param_category', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_param_category' AND sd.value = 'CONTENT_REVIEW' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 6, '钉钉配置', 'DINGTALK', 'dict_param_category', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_param_category' AND sd.value = 'DINGTALK' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 7, '工作任务', 'WORK_TASK', 'dict_param_category', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_param_category' AND sd.value = 'WORK_TASK' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '字符串', 'STRING', 'dict_param_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_param_type' AND sd.value = 'STRING' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '数字', 'NUMBER', 'dict_param_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_param_type' AND sd.value = 'NUMBER' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '布尔', 'BOOLEAN', 'dict_param_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_param_type' AND sd.value = 'BOOLEAN' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 4, 'JSON', 'JSON', 'dict_param_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_param_type' AND sd.value = 'JSON' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, 'S（优秀）', 'S', 'dict_perf_grade', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_perf_grade' AND sd.value = 'S' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, 'A（良好）', 'A', 'dict_perf_grade', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_perf_grade' AND sd.value = 'A' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, 'B（合格）', 'B', 'dict_perf_grade', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_perf_grade' AND sd.value = 'B' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 4, 'C（待改进）', 'C', 'dict_perf_grade', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_perf_grade' AND sd.value = 'C' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 5, 'D（不合格）', 'D', 'dict_perf_grade', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_perf_grade' AND sd.value = 'D' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '基础指标', 'BASIC', 'dict_perf_metric_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_perf_metric_type' AND sd.value = 'BASIC' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '复合指标', 'COMPOSITE', 'dict_perf_metric_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_perf_metric_type' AND sd.value = 'COMPOSITE' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '月度', 'MONTH', 'dict_perf_period', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_perf_period' AND sd.value = 'MONTH' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '季度', 'QUARTER', 'dict_perf_period', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_perf_period' AND sd.value = 'QUARTER' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '周度', 'WEEK', 'dict_perf_period', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_perf_period' AND sd.value = 'WEEK' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 4, '年度', 'YEAR', 'dict_perf_period', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_perf_period' AND sd.value = 'YEAR' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 5, '自定义', 'CUSTOM', 'dict_perf_period', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_perf_period' AND sd.value = 'CUSTOM' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '草稿', 'DRAFT', 'dict_perf_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_perf_status' AND sd.value = 'DRAFT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '已确认', 'CONFIRMED', 'dict_perf_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_perf_status' AND sd.value = 'CONFIRMED' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '在用', 'ENABLED', 'dict_phone_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_phone_status' AND sd.value = 'ENABLED' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '停用', 'DISABLED', 'dict_phone_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_phone_status' AND sd.value = 'DISABLED' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, 'Android', 'ANDROID', 'dict_phone_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_phone_type' AND sd.value = 'ANDROID' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, 'iPhone', 'IPHONE', 'dict_phone_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_phone_type' AND sd.value = 'IPHONE' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '草稿', 'DRAFT', 'dict_plan_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_plan_status' AND sd.value = 'DRAFT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '进行中', 'IN_PROGRESS', 'dict_plan_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_plan_status' AND sd.value = 'IN_PROGRESS' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '终止审批中', 'TERMINATE_PENDING', 'dict_plan_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_plan_status' AND sd.value = 'TERMINATE_PENDING' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 4, '已终止', 'TERMINATED', 'dict_plan_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_plan_status' AND sd.value = 'TERMINATED' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 0, '全部', 'ALL', 'dict_platform_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_platform_type' AND sd.value = 'ALL' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '微信公众号', 'WECHAT_OFFICIAL', 'dict_platform_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_platform_type' AND sd.value = 'WECHAT_OFFICIAL' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '抖音', 'DOUYIN', 'dict_platform_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_platform_type' AND sd.value = 'DOUYIN' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '企业微信', 'WEWORK', 'dict_platform_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_platform_type' AND sd.value = 'WEWORK' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 4, '视频号', 'WECHAT_VIDEO', 'dict_platform_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_platform_type' AND sd.value = 'WECHAT_VIDEO' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 5, '快手', 'KUAISHOU', 'dict_platform_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_platform_type' AND sd.value = 'KUAISHOU' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 6, '小红书', 'XIAOHONGSHU', 'dict_platform_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_platform_type' AND sd.value = 'XIAOHONGSHU' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 7, '个微', 'WECHAT_PERSONAL', 'dict_platform_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_platform_type' AND sd.value = 'WECHAT_PERSONAL' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 8, '服务号', 'SERVICE_ACCOUNT', 'dict_platform_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_platform_type' AND sd.value = 'SERVICE_ACCOUNT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '运营', 'OPERATOR', 'dict_position', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_position' AND sd.value = 'OPERATOR' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '编辑', 'EDITOR', 'dict_position', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_position' AND sd.value = 'EDITOR' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '主播', 'ANCHOR', 'dict_position', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_position' AND sd.value = 'ANCHOR' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 4, '销售', 'SALES', 'dict_position', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_position' AND sd.value = 'SALES' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 5, '运营组长', 'OPS_LEADER', 'dict_position', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_position' AND sd.value = 'OPS_LEADER' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 5, '运营官方号', 'OPS_OFFICIAL', 'dict_position', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_position' AND sd.value = 'OPS_OFFICIAL' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 6, '直播运营', 'LIVE_OPERATOR', 'dict_position', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_position' AND sd.value = 'LIVE_OPERATOR' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '奥创好友', 'AOCHUANG_FRIEND', 'dict_private_domain_identity_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_private_domain_identity_type' AND sd.value = 'AOCHUANG_FRIEND' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '公众号粉丝', 'MP_FOLLOWER', 'dict_private_domain_identity_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_private_domain_identity_type' AND sd.value = 'MP_FOLLOWER' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '企微客户', 'WECOM_CUSTOMER', 'dict_private_domain_identity_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_private_domain_identity_type' AND sd.value = 'WECOM_CUSTOMER' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 4, '企微员工', 'WECOM_EMPLOYEE', 'dict_private_domain_identity_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_private_domain_identity_type' AND sd.value = 'WECOM_EMPLOYEE' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 5, '手机', 'PHONE', 'dict_private_domain_identity_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_private_domain_identity_type' AND sd.value = 'PHONE' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 6, '实名人', 'REALNAME', 'dict_private_domain_identity_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_private_domain_identity_type' AND sd.value = 'REALNAME' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '人工', 'MANUAL', 'dict_private_domain_match_method', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_private_domain_match_method' AND sd.value = 'MANUAL' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '规则', 'RULE', 'dict_private_domain_match_method', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_private_domain_match_method' AND sd.value = 'RULE' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '手机号', 'PHONE', 'dict_private_domain_match_method', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_private_domain_match_method' AND sd.value = 'PHONE' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 4, 'UnionID', 'UNIONID', 'dict_private_domain_match_method', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_private_domain_match_method' AND sd.value = 'UNIONID' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 5, 'AI', 'AI', 'dict_private_domain_match_method', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_private_domain_match_method' AND sd.value = 'AI' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '待审核', 'PENDING', 'dict_private_domain_review_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_private_domain_review_status' AND sd.value = 'PENDING' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '已通过', 'APPROVED', 'dict_private_domain_review_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_private_domain_review_status' AND sd.value = 'APPROVED' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '已驳回', 'REJECTED', 'dict_private_domain_review_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_private_domain_review_status' AND sd.value = 'REJECTED' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '视频分析', 'VIDEO_ANALYSIS', 'dict_prompt_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_prompt_type' AND sd.value = 'VIDEO_ANALYSIS' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '图文分析', 'IMAGE_TEXT_ANALYSIS', 'dict_prompt_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_prompt_type' AND sd.value = 'IMAGE_TEXT_ANALYSIS' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '数据解读', 'DATA_INTERPRET', 'dict_prompt_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_prompt_type' AND sd.value = 'DATA_INTERPRET' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 4, '内容生成', 'CONTENT_GEN', 'dict_prompt_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_prompt_type' AND sd.value = 'CONTENT_GEN' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '企业', 'ENTERPRISE', 'dict_qualification_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_qualification_type' AND sd.value = 'ENTERPRISE' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '个人', 'PERSONAL', 'dict_qualification_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_qualification_type' AND sd.value = 'PERSONAL' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '完整性', 'COMPLETENESS', 'dict_quality_check_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_quality_check_type' AND sd.value = 'COMPLETENESS' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '准确性', 'ACCURACY', 'dict_quality_check_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_quality_check_type' AND sd.value = 'ACCURACY' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '一致性', 'CONSISTENCY', 'dict_quality_check_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_quality_check_type' AND sd.value = 'CONSISTENCY' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 4, '时效性', 'TIMELINESS', 'dict_quality_check_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_quality_check_type' AND sd.value = 'TIMELINESS' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 5, '唯一性', 'UNIQUENESS', 'dict_quality_check_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_quality_check_type' AND sd.value = 'UNIQUENESS' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '优', 'EXCELLENT', 'dict_quality_level', 0, 'success', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_quality_level' AND sd.value = 'EXCELLENT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '良', 'GOOD', 'dict_quality_level', 0, 'primary', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_quality_level' AND sd.value = 'GOOD' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '中', 'MEDIUM', 'dict_quality_level', 0, 'warning', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_quality_level' AND sd.value = 'MEDIUM' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 4, '差', 'POOR', 'dict_quality_level', 0, 'danger', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_quality_level' AND sd.value = 'POOR' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '草稿', 'DRAFT', 'dict_query_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_query_status' AND sd.value = 'DRAFT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '已发布', 'PUBLISHED', 'dict_query_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_query_status' AND sd.value = 'PUBLISHED' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '启用', 'ENABLED', 'dict_realname_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_realname_status' AND sd.value = 'ENABLED' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '停用', 'DISABLED', 'dict_realname_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_realname_status' AND sd.value = 'DISABLED' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '一级审核', 'FIRST_REVIEW', 'dict_review_stage', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_review_stage' AND sd.value = 'FIRST_REVIEW' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '二级审核', 'SECOND_REVIEW', 'dict_review_stage', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_review_stage' AND sd.value = 'SECOND_REVIEW' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '终审', 'FINAL_REVIEW', 'dict_review_stage', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_review_stage' AND sd.value = 'FINAL_REVIEW' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '待审核', 'PENDING', 'dict_review_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_review_status' AND sd.value = 'PENDING' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '审核中', 'REVIEWING', 'dict_review_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_review_status' AND sd.value = 'REVIEWING' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '已通过', 'APPROVED', 'dict_review_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_review_status' AND sd.value = 'APPROVED' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 4, '已驳回', 'REJECTED', 'dict_review_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_review_status' AND sd.value = 'REJECTED' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, 'IP组', 'IP_GROUP', 'dict_roi_dimension', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_roi_dimension' AND sd.value = 'IP_GROUP' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '账号', 'ACCOUNT', 'dict_roi_dimension', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_roi_dimension' AND sd.value = 'ACCOUNT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '人员', 'PERSON', 'dict_roi_dimension', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_roi_dimension' AND sd.value = 'PERSON' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '胜平负分析', 'WIN_DRAW_LOSE', 'dict_scheme_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_scheme_type' AND sd.value = 'WIN_DRAW_LOSE' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '让球分析', 'HANDICAP', 'dict_scheme_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_scheme_type' AND sd.value = 'HANDICAP' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '大小球分析', 'OVER_UNDER', 'dict_scheme_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_scheme_type' AND sd.value = 'OVER_UNDER' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 4, '比分预测', 'SCORE_PREDICT', 'dict_scheme_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_scheme_type' AND sd.value = 'SCORE_PREDICT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 5, '综合推荐', 'COMPREHENSIVE', 'dict_scheme_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_scheme_type' AND sd.value = 'COMPREHENSIVE' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '中国移动', 'MOBILE', 'dict_sim_operator', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_sim_operator' AND sd.value = 'MOBILE' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '中国联通', 'UNICOM', 'dict_sim_operator', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_sim_operator' AND sd.value = 'UNICOM' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '中国电信', 'TELECOM', 'dict_sim_operator', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_sim_operator' AND sd.value = 'TELECOM' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '在用', 'ENABLED', 'dict_sim_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_sim_status' AND sd.value = 'ENABLED' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '停用', 'DISABLED', 'dict_sim_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_sim_status' AND sd.value = 'DISABLED' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '损坏', 'DAMAGED', 'dict_sim_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_sim_status' AND sd.value = 'DAMAGED' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 4, '丢失', 'LOST', 'dict_sim_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_sim_status' AND sd.value = 'LOST' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '待执行', 'PENDING', 'dict_sop_node_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_sop_node_status' AND sd.value = 'PENDING' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '执行中', 'IN_PROGRESS', 'dict_sop_node_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_sop_node_status' AND sd.value = 'IN_PROGRESS' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '已完成', 'COMPLETED', 'dict_sop_node_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_sop_node_status' AND sd.value = 'COMPLETED' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 4, '待审核', 'PENDING_REVIEW', 'dict_sop_node_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_sop_node_status' AND sd.value = 'PENDING_REVIEW' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 5, '审核通过', 'APPROVED', 'dict_sop_node_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_sop_node_status' AND sd.value = 'APPROVED' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 6, '审核驳回', 'REJECTED', 'dict_sop_node_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_sop_node_status' AND sd.value = 'REJECTED' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 7, '节点完成', 'DONE', 'dict_sop_node_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_sop_node_status' AND sd.value = 'DONE' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 8, '计划草稿', 'PLAN_DRAFT', 'dict_sop_node_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_sop_node_status' AND sd.value = 'PLAN_DRAFT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 9, '已终止', 'TERMINATED', 'dict_sop_node_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_sop_node_status' AND sd.value = 'TERMINATED' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '内容生成', 'CONTENT_GENERATION', 'dict_sop_node_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_sop_node_type' AND sd.value = 'CONTENT_GENERATION' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '内容发布', 'CONTENT_PUBLISH', 'dict_sop_node_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_sop_node_type' AND sd.value = 'CONTENT_PUBLISH' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '普通节点', 'NORMAL', 'dict_sop_node_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_sop_node_type' AND sd.value = 'NORMAL' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '每日', 'DAILY', 'dict_sync_frequency', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_sync_frequency' AND sd.value = 'DAILY' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '每小时', 'HOURLY', 'dict_sync_frequency', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_sync_frequency' AND sd.value = 'HOURLY' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '增量', 'INCREMENTAL', 'dict_sync_mode', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_sync_mode' AND sd.value = 'INCREMENTAL' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '全量', 'FULL', 'dict_sync_mode', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_sync_mode' AND sd.value = 'FULL' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '正常', 'NORMAL', 'dict_tenant_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_tenant_status' AND sd.value = 'NORMAL' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '试用', 'TRIAL', 'dict_tenant_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_tenant_status' AND sd.value = 'TRIAL' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '已到期', 'EXPIRED', 'dict_tenant_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_tenant_status' AND sd.value = 'EXPIRED' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 4, '已停用', 'DISABLED', 'dict_tenant_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_tenant_status' AND sd.value = 'DISABLED' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '新榜', 'NEWRANK', 'dict_third_platform', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_third_platform' AND sd.value = 'NEWRANK' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '飞瓜', 'FEIGUA', 'dict_third_platform', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_third_platform' AND sd.value = 'FEIGUA' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '预警阈值', 'ALERT', 'dict_threshold_category', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_threshold_category' AND sd.value = 'ALERT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '粉丝阈值', 'FANS', 'dict_threshold_category', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_threshold_category' AND sd.value = 'FANS' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '作品阈值', 'WORK', 'dict_threshold_category', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_threshold_category' AND sd.value = 'WORK' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 4, '账号覆盖', 'OVERRIDE', 'dict_threshold_category', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_threshold_category' AND sd.value = 'OVERRIDE' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '爆款阈值', 'HIT_THRESHOLD', 'dict_threshold_metric', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_threshold_metric' AND sd.value = 'HIT_THRESHOLD' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '播放量', 'PLAY_COUNT', 'dict_threshold_metric', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_threshold_metric' AND sd.value = 'PLAY_COUNT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '点赞数', 'LIKE_COUNT', 'dict_threshold_metric', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_threshold_metric' AND sd.value = 'LIKE_COUNT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '低分阈值', 'LOW_SCORE', 'dict_threshold_metric', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_threshold_metric' AND sd.value = 'LOW_SCORE' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '评论数', 'COMMENT_COUNT', 'dict_threshold_metric', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_threshold_metric' AND sd.value = 'COMMENT_COUNT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '粉丝预警', 'FAN_ALERT', 'dict_threshold_metric', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_threshold_metric' AND sd.value = 'FAN_ALERT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 4, '转发数', 'SHARE_COUNT', 'dict_threshold_metric', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_threshold_metric' AND sd.value = 'SHARE_COUNT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 5, '阅读量', 'READ_COUNT', 'dict_threshold_metric', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_threshold_metric' AND sd.value = 'READ_COUNT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 6, '粉丝增长', 'FAN_GROWTH', 'dict_threshold_metric', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_threshold_metric' AND sd.value = 'FAN_GROWTH' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 7, '粉丝数', 'FAN_COUNT', 'dict_threshold_metric', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_threshold_metric' AND sd.value = 'FAN_COUNT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 8, '粉丝数', 'FOLLOWER', 'dict_threshold_metric', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_threshold_metric' AND sd.value = 'FOLLOWER' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 9, '互动率', 'ENGAGEMENT', 'dict_threshold_metric', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_threshold_metric' AND sd.value = 'ENGAGEMENT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 10, '转化率', 'CONVERSION', 'dict_threshold_metric', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_threshold_metric' AND sd.value = 'CONVERSION' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 11, '直播在线人数', 'LIVE_ONLINE', 'dict_threshold_metric', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_threshold_metric' AND sd.value = 'LIVE_ONLINE' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 12, '负面情绪比例', 'NEGATIVE_RATE', 'dict_threshold_metric', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_threshold_metric' AND sd.value = 'NEGATIVE_RATE' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 13, '发布频率', 'POST_FREQUENCY', 'dict_threshold_metric', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_threshold_metric' AND sd.value = 'POST_FREQUENCY' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 17, 'GMV', 'GMV', 'dict_threshold_metric', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_threshold_metric' AND sd.value = 'GMV' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 18, '阅读量骤降', 'VIEW_DROP', 'dict_threshold_metric', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_threshold_metric' AND sd.value = 'VIEW_DROP' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 19, '播放量骤降', 'PLAY_DROP', 'dict_threshold_metric', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_threshold_metric' AND sd.value = 'PLAY_DROP' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '百分比', 'PERCENTAGE', 'dict_threshold_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_threshold_type' AND sd.value = 'PERCENTAGE' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '绝对值', 'ABSOLUTE', 'dict_threshold_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_threshold_type' AND sd.value = 'ABSOLUTE' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '按日', 'DAY', 'dict_time_dimension', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_time_dimension' AND sd.value = 'DAY' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '按周', 'WEEK', 'dict_time_dimension', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_time_dimension' AND sd.value = 'WEEK' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '按月', 'MONTH', 'dict_time_dimension', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_time_dimension' AND sd.value = 'MONTH' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '完整三方', 'FULL_TRIPLE', 'dict_triple_rel_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_triple_rel_type' AND sd.value = 'FULL_TRIPLE' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '微信+视频', 'WECHAT_VIDEO', 'dict_triple_rel_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_triple_rel_type' AND sd.value = 'WECHAT_VIDEO' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '微信+企微', 'WECHAT_WEWORK', 'dict_triple_rel_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_triple_rel_type' AND sd.value = 'WECHAT_WEWORK' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 4, '视频+企微', 'VIDEO_WEWORK', 'dict_triple_rel_type', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_triple_rel_type' AND sd.value = 'VIDEO_WEWORK' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '启用', 'ENABLED', 'dict_user_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_user_status' AND sd.value = 'ENABLED' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '停用', 'DISABLED', 'dict_user_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_user_status' AND sd.value = 'DISABLED' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '锁定', 'LOCKED', 'dict_user_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_user_status' AND sd.value = 'LOCKED' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '注册', 'REGISTERED', 'dict_wechat_usage_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_wechat_usage_status' AND sd.value = 'REGISTERED' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '认证', 'CERTIFIED', 'dict_wechat_usage_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_wechat_usage_status' AND sd.value = 'CERTIFIED' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '续费', 'RENEWED', 'dict_wechat_usage_status', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_wechat_usage_status' AND sd.value = 'RENEWED' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '是', 'YES', 'dict_yes_no', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_yes_no' AND sd.value = 'YES' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '否', 'NO', 'dict_yes_no', 0, 'default', '', NULL, 'deploy-dict-seed', NOW(), 'deploy-dict-seed', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = 'dict_yes_no' AND sd.value = 'NO' AND sd.deleted = b'0'
);

