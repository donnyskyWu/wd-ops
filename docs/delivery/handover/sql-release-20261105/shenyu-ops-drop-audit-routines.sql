-- =============================================================================
-- shenyu-ops · 移除 ROI 库内操作审计（ADR-085）
-- 目标库：shenyu-ops · MySQL 5.7+ / MariaDB 10.x
-- 幂等：DROP … IF EXISTS；可重复执行
-- Binlog 环境：触发器/函数 DROP 可能报 1419，须 DBA 先设 log_bin_trust_function_creators=1（见 DBA-1419-一次性设置.md）
-- 保留：roi_audit 轻量行级表（非操作日志 UI）；RoiAuditSupport 会话变量无影响
-- SSOT 来源：football-backend-saas submodule git（20261026_roi_operation_logs 等）
-- 统计：触发器 106 · 函数 3 · 表 2 · 合计 111
-- 生成：python docs/delivery/handover/sql-release-20261105/_gen_drop_audit_routines.py
-- =============================================================================

SET NAMES utf8mb4;
USE `shenyu-ops`;

-- Section A: 业务表 / roi_operation_log 审计触发器
DELIMITER $$
DROP TRIGGER IF EXISTS `roi_operation_log_no_update_v1`$$
DROP TRIGGER IF EXISTS `roi_operation_log_no_delete_v1`$$
DROP TRIGGER IF EXISTS `roi_log_r_record_i_v1`$$
DROP TRIGGER IF EXISTS `roi_log_r_record_u_v1`$$
DROP TRIGGER IF EXISTS `roi_log_r_record_d_v1`$$
DROP TRIGGER IF EXISTS `roi_log_r_attachment_i_v1`$$
DROP TRIGGER IF EXISTS `roi_log_r_attachment_u_v1`$$
DROP TRIGGER IF EXISTS `roi_log_r_attachment_d_v1`$$
DROP TRIGGER IF EXISTS `roi_log_r_attachment_grant_i_v1`$$
DROP TRIGGER IF EXISTS `roi_log_r_attachment_grant_u_v1`$$
DROP TRIGGER IF EXISTS `roi_log_r_attachment_grant_d_v1`$$
DROP TRIGGER IF EXISTS `roi_log_r_role_i_v1`$$
DROP TRIGGER IF EXISTS `roi_log_r_role_u_v1`$$
DROP TRIGGER IF EXISTS `roi_log_r_role_d_v1`$$
DROP TRIGGER IF EXISTS `roi_log_r_role_override_i_v1`$$
DROP TRIGGER IF EXISTS `roi_log_r_role_override_u_v1`$$
DROP TRIGGER IF EXISTS `roi_log_r_role_override_d_v1`$$
DROP TRIGGER IF EXISTS `roi_log_r_author_scope_i_v1`$$
DROP TRIGGER IF EXISTS `roi_log_r_author_scope_u_v1`$$
DROP TRIGGER IF EXISTS `roi_log_r_author_scope_d_v1`$$
DROP TRIGGER IF EXISTS `roi_log_r_account_assignment_i_v1`$$
DROP TRIGGER IF EXISTS `roi_log_r_account_assignment_u_v1`$$
DROP TRIGGER IF EXISTS `roi_log_r_account_assignment_d_v1`$$
DROP TRIGGER IF EXISTS `roi_log_r_account_ban_history_i_v1`$$
DROP TRIGGER IF EXISTS `roi_log_r_account_ban_history_u_v1`$$
DROP TRIGGER IF EXISTS `roi_log_r_account_ban_history_d_v1`$$
DROP TRIGGER IF EXISTS `roi_log_r_account_qualification_i_v1`$$
DROP TRIGGER IF EXISTS `roi_log_r_account_qualification_u_v1`$$
DROP TRIGGER IF EXISTS `roi_log_r_account_qualification_d_v1`$$
DROP TRIGGER IF EXISTS `roi_log_r_session_attribution_binding_i_v1`$$
DROP TRIGGER IF EXISTS `roi_log_r_session_attribution_binding_u_v1`$$
DROP TRIGGER IF EXISTS `roi_log_r_session_attribution_binding_d_v1`$$
DROP TRIGGER IF EXISTS `roi_log_r_session_attribution_article_i_v1`$$
DROP TRIGGER IF EXISTS `roi_log_r_session_attribution_article_u_v1`$$
DROP TRIGGER IF EXISTS `roi_log_r_session_attribution_article_d_v1`$$
DROP TRIGGER IF EXISTS `roi_log_r_wecom_session_link_i_v1`$$
DROP TRIGGER IF EXISTS `roi_log_r_wecom_session_link_u_v1`$$
DROP TRIGGER IF EXISTS `roi_log_r_wecom_session_link_d_v1`$$
DROP TRIGGER IF EXISTS `roi_log_r_wecom_event_i_v1`$$
DROP TRIGGER IF EXISTS `roi_log_r_wecom_event_u_v1`$$
DROP TRIGGER IF EXISTS `roi_log_r_wecom_event_d_v1`$$
DROP TRIGGER IF EXISTS `roi_log_r_wecom_welcome_outbox_i_v1`$$
DROP TRIGGER IF EXISTS `roi_log_r_wecom_welcome_outbox_u_v1`$$
DROP TRIGGER IF EXISTS `roi_log_r_wecom_welcome_outbox_d_v1`$$
DROP TRIGGER IF EXISTS `roi_log_r_wecom_callback_state_i_v1`$$
DROP TRIGGER IF EXISTS `roi_log_r_wecom_callback_state_u_v1`$$
DROP TRIGGER IF EXISTS `roi_log_r_wecom_callback_state_d_v1`$$
DROP TRIGGER IF EXISTS `roi_log_r_approval_inbox_i_v1`$$
DROP TRIGGER IF EXISTS `roi_log_r_approval_inbox_u_v1`$$
DROP TRIGGER IF EXISTS `roi_log_r_approval_inbox_d_v1`$$
DROP TRIGGER IF EXISTS `roi_log_o_template_i_v1`$$
DROP TRIGGER IF EXISTS `roi_log_o_template_u_v1`$$
DROP TRIGGER IF EXISTS `roi_log_o_template_d_v1`$$
DROP TRIGGER IF EXISTS `roi_log_r_process_i_v1`$$
DROP TRIGGER IF EXISTS `roi_log_r_process_u_v1`$$
DROP TRIGGER IF EXISTS `roi_log_r_process_d_v1`$$
DROP TRIGGER IF EXISTS `roi_log_o_instance_i_v1`$$
DROP TRIGGER IF EXISTS `roi_log_o_instance_u_v1`$$
DROP TRIGGER IF EXISTS `roi_log_o_instance_d_v1`$$
DROP TRIGGER IF EXISTS `roi_log_o_cost_candidate_i_v1`$$
DROP TRIGGER IF EXISTS `roi_log_o_cost_candidate_u_v1`$$
DROP TRIGGER IF EXISTS `roi_log_o_cost_candidate_d_v1`$$
DROP TRIGGER IF EXISTS `roi_log_o_account_cost_link_i_v1`$$
DROP TRIGGER IF EXISTS `roi_log_o_account_cost_link_u_v1`$$
DROP TRIGGER IF EXISTS `roi_log_o_account_cost_link_d_v1`$$
DROP TRIGGER IF EXISTS `roi_log_o_cost_alias_i_v1`$$
DROP TRIGGER IF EXISTS `roi_log_o_cost_alias_u_v1`$$
DROP TRIGGER IF EXISTS `roi_log_o_cost_alias_d_v1`$$
DROP TRIGGER IF EXISTS `roi_log_o_cost_link_i_v1`$$
DROP TRIGGER IF EXISTS `roi_log_o_cost_link_u_v1`$$
DROP TRIGGER IF EXISTS `roi_log_o_cost_link_d_v1`$$
DROP TRIGGER IF EXISTS `roi_log_oa_account_cost_i_v1`$$
DROP TRIGGER IF EXISTS `roi_log_oa_account_cost_u_v1`$$
DROP TRIGGER IF EXISTS `roi_log_oa_account_cost_d_v1`$$
DROP TRIGGER IF EXISTS `roi_log_oa_account_i_v1`$$
DROP TRIGGER IF EXISTS `roi_log_oa_account_u_v1`$$
DROP TRIGGER IF EXISTS `roi_log_oa_account_d_v1`$$
DROP TRIGGER IF EXISTS `roi_org_member_i_audit_v1`$$
DROP TRIGGER IF EXISTS `roi_org_member_u_audit_v1`$$
DROP TRIGGER IF EXISTS `roi_org_member_d_audit_v1`$$
DROP TRIGGER IF EXISTS `roi_org_role_mapping_i_audit_v1`$$
DROP TRIGGER IF EXISTS `roi_org_role_mapping_u_audit_v1`$$
DROP TRIGGER IF EXISTS `roi_org_role_mapping_d_audit_v1`$$
DROP TRIGGER IF EXISTS `roi_ip_group_scope_i_audit_v1`$$
DROP TRIGGER IF EXISTS `roi_ip_group_scope_u_audit_v1`$$
DROP TRIGGER IF EXISTS `roi_ip_group_scope_d_audit_v1`$$
DROP TRIGGER IF EXISTS `roi_wfm_media_i_v1`$$
DROP TRIGGER IF EXISTS `roi_wfm_media_u_v1`$$
DROP TRIGGER IF EXISTS `roi_wfm_media_d_v1`$$
DROP TRIGGER IF EXISTS `roi_wfm_link_operation_i_v1`$$
DROP TRIGGER IF EXISTS `roi_wfm_link_operation_u_v1`$$
DROP TRIGGER IF EXISTS `roi_wfm_link_operation_d_v1`$$
DROP TRIGGER IF EXISTS `roi_wfm_link_owner_history_i_v1`$$
DROP TRIGGER IF EXISTS `roi_wfm_link_owner_history_u_v1`$$
DROP TRIGGER IF EXISTS `roi_wfm_link_owner_history_d_v1`$$
DROP TRIGGER IF EXISTS `roi_log_r_author_operator_i_20261029`$$
DROP TRIGGER IF EXISTS `roi_log_r_author_operator_u_20261029`$$
DROP TRIGGER IF EXISTS `roi_log_r_account_operator_i_20261029`$$
DROP TRIGGER IF EXISTS `roi_log_r_account_operator_u_20261029`$$
DROP TRIGGER IF EXISTS `roi_log_r_cost_source_i_20261029`$$
DROP TRIGGER IF EXISTS `roi_log_r_cost_source_u_20261029`$$
DROP TRIGGER IF EXISTS `roi_log_r_cost_attribution_i_20261029`$$
DROP TRIGGER IF EXISTS `roi_log_r_cost_attribution_u_20261029`$$
DROP TRIGGER IF EXISTS `roi_cost_classification_log_i_20261030`$$
DROP TRIGGER IF EXISTS `roi_cost_classification_log_u_20261030`$$
DROP TRIGGER IF EXISTS `roi_cost_classification_no_delete_20261030`$$
DELIMITER ;

-- Section B: 审计 JSON 投影函数（含 IP 投影 Section 08）
DELIMITER $$
DROP FUNCTION IF EXISTS `roi_audit_text_v1`$$
DROP FUNCTION IF EXISTS `roi_audit_leaf_v1`$$
DROP FUNCTION IF EXISTS `roi_audit_project_v1`$$
DELIMITER ;

-- Section C: 操作日志及迁移状态表
DROP TABLE IF EXISTS `roi_operation_log`;
DROP TABLE IF EXISTS `roi_audit_migration_state`;
