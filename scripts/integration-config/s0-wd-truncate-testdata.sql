-- S0: TRUNCATE wd 业务测试数据（§N.8 B/C 组）
-- 目标库：localhost:3306/wd ONLY — 禁止在远程 101.37.161.136 执行
-- 前置：mysqldump 备份（见 OPS-FOOTBALL-MULTI-DB-EXECUTION-PLAN.md §S0 回滚）
-- 用户确认：2026-07-05

USE wd;

-- ========== KEEP（勿 TRUNCATE）==========
-- sys_dict_type, sys_dict_data, sys_param
-- sys_metadata_entity, sys_metadata_field, sys_notification_event
-- oa_ai_model_config, oa_ai_prompt_config, oa_threshold_config
-- oa_config_keyword, oa_aocreate_api
-- oa_metric, oa_perf_template, oa_perf_template_item
-- oa_funnel, oa_funnel_step, oa_custom_query, oa_dashboard
-- oa_sop_template, oa_sop_node
-- oa_typesetting_rule, oa_wechat_layout_template, oa_layout_style
-- flyway_schema_history

-- ========== TRUNCATE — 业务测试数据（B 组）==========
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE oa_order_attribution;
TRUNCATE TABLE oa_order;
TRUNCATE TABLE oa_content;
TRUNCATE TABLE oa_production_content;
TRUNCATE TABLE oa_content_plan_step;
TRUNCATE TABLE oa_content_plan_competition;
TRUNCATE TABLE oa_content_plan;
TRUNCATE TABLE oa_task;
TRUNCATE TABLE oa_sop_review;
TRUNCATE TABLE oa_review_record;
TRUNCATE TABLE oa_content_publish_record;
TRUNCATE TABLE oa_content_data_import;
TRUNCATE TABLE oa_knowledge_base;
TRUNCATE TABLE oa_author_ext;
TRUNCATE TABLE oa_author;
TRUNCATE TABLE oa_account;
TRUNCATE TABLE oa_ip_group_member;
TRUNCATE TABLE oa_ip_group_anchor_rel;
TRUNCATE TABLE oa_ip_group;
TRUNCATE TABLE oa_ops_anchor_rel;
-- M4 资产
TRUNCATE TABLE oa_account_wechat_video_wework_rel;
TRUNCATE TABLE oa_platform_account_fan_group;
TRUNCATE TABLE oa_company_expansion;
TRUNCATE TABLE oa_company;
TRUNCATE TABLE oa_realname_intermediary;
TRUNCATE TABLE oa_realname;
TRUNCATE TABLE oa_phone;
TRUNCATE TABLE oa_sim_card;
TRUNCATE TABLE oa_personal_wechat_account;
TRUNCATE TABLE oa_wework_employee;
TRUNCATE TABLE oa_wework_account;
-- 废弃 sys_*
TRUNCATE TABLE sys_user_role;
TRUNCATE TABLE sys_role_permission;
TRUNCATE TABLE sys_user_token;
TRUNCATE TABLE sys_user;
TRUNCATE TABLE sys_role;
TRUNCATE TABLE sys_permission;
TRUNCATE TABLE sys_dept;
TRUNCATE TABLE sys_audit_log;
-- wd 内 Football 副本（若存在数据）
TRUNCATE TABLE author_channel_sales;
TRUNCATE TABLE author_user;
TRUNCATE TABLE pay_gold_order;
TRUNCATE TABLE pay_all_order;
-- Demo
TRUNCATE TABLE football_demo01_contact;
TRUNCATE TABLE football_demo02_category;
TRUNCATE TABLE football_demo03_student;
TRUNCATE TABLE football_demo03_grade;
TRUNCATE TABLE football_demo03_course;
TRUNCATE TABLE oa_demo_item;
SET FOREIGN_KEY_CHECKS = 1;

-- ========== TRUNCATE — 采集 / 分析实例（C 组）==========
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE oa_collect_log;
TRUNCATE TABLE oa_collect_task;
TRUNCATE TABLE oa_collector_account_bind;
TRUNCATE TABLE oa_content_daily;
TRUNCATE TABLE oa_follower_daily;
TRUNCATE TABLE oa_account_status_log;
TRUNCATE TABLE oa_account_cost;
TRUNCATE TABLE oa_douyin_follower;
TRUNCATE TABLE oa_douyin_video;
TRUNCATE TABLE oa_kuaishou_video;
TRUNCATE TABLE oa_wechat_video_work;
TRUNCATE TABLE oa_xiaohongshu_note;
TRUNCATE TABLE oa_wechat_mp_article;
TRUNCATE TABLE oa_wechat_mp_follower;
TRUNCATE TABLE oa_perf_item_record;
TRUNCATE TABLE oa_perf_record;
TRUNCATE TABLE oa_external_work;
TRUNCATE TABLE oa_home_alert;
TRUNCATE TABLE sys_login_log;
TRUNCATE TABLE sys_operation_log;
TRUNCATE TABLE sys_message;
SET FOREIGN_KEY_CHECKS = 1;
