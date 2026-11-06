-- =============================================================================
-- 用途：M-Live 投产比（ROI）· shenyu-ops 增量结构 + 可选钉钉字段映射
-- 目标库：shenyu-ops
-- 执行顺序：Section 01 → 14（必做）；Section 15 为 [OPTIONAL]（见 README）
-- 幂等性：是（CREATE IF NOT EXISTS、information_schema 条件 ALTER、INSERT IGNORE）
-- 回滚：生产需备份 shenyu-ops（至少 roi_* / ops_oa_* / oa_account_cost）；无自动回滚。
--       IP 审计函数回滚见 Section 08 注释（20261026_roi_ip_audit_projection_rollback.sql）。
-- 关联：ADR-084（ROI 运行时统一 ops-server :48094）；Flyway V205/V206 见 Section 12–13
-- 生成：python docs/delivery/handover/sql-release-20261105/_build_sql_release_20261105.py
-- =============================================================================

-- ---------------------------------------------------------------------------
-- MySQL 5.7 / 托管 · 导入前 DBA 须知（本段注释-only，非 DDL）
-- Error 1419: log_bin=ON 且无 SUPER 时，含 CREATE FUNCTION 的例程段会失败。
--   DBA: SET GLOBAL log_bin_trust_function_creators = 1; 或 my.cnf / 云参数组 + 重启
--   校验: SHOW VARIABLES LIKE 'log_bin_trust_function_creators'; → ON/1
-- 本脚本 intentionally 不含 SET GLOBAL（无权限账号误跑会整包失败）。
-- 完整说明: 同目录 00-mysql57-prerequisites.sql 与 README.md「Error 1419」
-- 兼容: utf8mb4_unicode_ci、DROP+CREATE 例程/触发器；5.7 DEFAULT CURRENT_TIMESTAMP 按会话 TZ
-- ---------------------------------------------------------------------------


SET NAMES utf8mb4;
USE `shenyu-ops`;


-- =============================================================================
-- Section 01: ROI + OA 镜像基础表
-- SSOT: 20261023_01_local_feature_schema.sql (shenyu-ops 段)
-- =============================================================================

USE `shenyu-ops`;
CREATE TABLE IF NOT EXISTS `ops_oa_call_budget` (
  `tenant_id` bigint NOT NULL,
  `day_key` date NOT NULL,
  `call_count` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`tenant_id`,`day_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `ops_oa_event_queue` (
  `tenant_id` bigint NOT NULL,
  `event_key` char(64) COLLATE utf8mb4_bin NOT NULL,
  `process_code` varchar(128) COLLATE utf8mb4_bin NOT NULL,
  `instance_id` varchar(256) COLLATE utf8mb4_bin NOT NULL,
  `state` varchar(24) COLLATE utf8mb4_bin NOT NULL,
  `attempts` int NOT NULL DEFAULT '0',
  `next_attempt_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `error_code` varchar(128) COLLATE utf8mb4_bin DEFAULT NULL,
  `received_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `finished_at` datetime(3) DEFAULT NULL,
  PRIMARY KEY (`tenant_id`,`event_key`),
  KEY `ix_ops_oa_events` (`state`,`next_attempt_at`),
  KEY `ix_ops_oa_unclassified` (`tenant_id`,`state`,`process_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS `ops_oa_instance` (
  `id` char(36) COLLATE utf8mb4_bin NOT NULL,
  `tenant_id` bigint NOT NULL,
  `domain` varchar(16) COLLATE utf8mb4_bin NOT NULL,
  `instance_id` varchar(256) COLLATE utf8mb4_bin NOT NULL,
  `business_id` varchar(128) COLLATE utf8mb4_bin DEFAULT NULL,
  `process_code` varchar(128) COLLATE utf8mb4_bin NOT NULL,
  `template_name` varchar(256) COLLATE utf8mb4_bin NOT NULL,
  `title` varchar(512) COLLATE utf8mb4_bin NOT NULL,
  `originator_dingtalk_id` varchar(128) COLLATE utf8mb4_bin NOT NULL,
  `provider_status` varchar(32) COLLATE utf8mb4_bin NOT NULL,
  `provider_result` varchar(32) COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `form_amount` decimal(18,2) DEFAULT NULL,
  `amount_state` varchar(32) COLLATE utf8mb4_bin NOT NULL,
  `provider_created_raw` varchar(100) COLLATE utf8mb4_bin NOT NULL,
  `provider_created_date` date DEFAULT NULL,
  `payload_json` longtext COLLATE utf8mb4_bin NOT NULL,
  `participants_json` longtext COLLATE utf8mb4_bin NOT NULL,
  `association_state` varchar(32) COLLATE utf8mb4_bin NOT NULL DEFAULT 'unassigned',
  `roi_record_id` char(36) COLLATE utf8mb4_bin DEFAULT NULL,
  `source` varchar(64) COLLATE utf8mb4_bin NOT NULL,
  `created_by` bigint NOT NULL,
  `updated_by` bigint NOT NULL,
  `first_synced_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `synced_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `next_sync_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `failure_count` int NOT NULL DEFAULT '0',
  `error_code` varchar(128) COLLATE utf8mb4_bin DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_ops_oa_instance` (`tenant_id`,`instance_id`),
  UNIQUE KEY `uq_ops_oa_business` (`tenant_id`,`business_id`),
  KEY `ix_ops_oa_list` (`tenant_id`,`domain`,`provider_created_date`,`provider_status`),
  KEY `ix_ops_oa_due` (`next_sync_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS `ops_oa_member_role` (
  `tenant_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `role` varchar(32) COLLATE utf8mb4_bin NOT NULL,
  PRIMARY KEY (`tenant_id`,`user_id`,`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS `ops_oa_sync_cursor` (
  `tenant_id` bigint NOT NULL,
  `process_code` varchar(128) COLLATE utf8mb4_bin NOT NULL,
  `window_start_ms` bigint DEFAULT NULL,
  `window_end_ms` bigint DEFAULT NULL,
  `next_token` bigint NOT NULL DEFAULT '0',
  `last_completed_ms` bigint DEFAULT NULL,
  `lease_until` datetime(3) DEFAULT NULL,
  `next_attempt_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `last_success_at` datetime(3) DEFAULT NULL,
  `error_code` varchar(128) COLLATE utf8mb4_bin DEFAULT NULL,
  `failure_count` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`tenant_id`,`process_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS `ops_oa_template` (
  `tenant_id` bigint NOT NULL,
  `process_code` varchar(128) COLLATE utf8mb4_bin NOT NULL,
  `domain` varchar(16) COLLATE utf8mb4_bin NOT NULL,
  `template_name` varchar(256) COLLATE utf8mb4_bin NOT NULL,
  `app_client_id` varchar(128) COLLATE utf8mb4_bin NOT NULL,
  `corp_id` varchar(128) COLLATE utf8mb4_bin NOT NULL,
  `amount_field_id` varchar(160) COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `source` varchar(64) COLLATE utf8mb4_bin NOT NULL,
  `enabled` tinyint NOT NULL DEFAULT '1',
  `verified_by` bigint NOT NULL,
  `verified_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`tenant_id`,`process_code`),
  KEY `ix_ops_oa_domain` (`tenant_id`,`domain`,`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS `roi_account_assignment` (
  `id` varchar(36) NOT NULL,
  `tenant_id` bigint NOT NULL,
  `account_id` varchar(36) NOT NULL,
  `account_version` int NOT NULL,
  `author_id` varchar(64) DEFAULT NULL,
  `effective_at` datetime(3) NOT NULL,
  `source` varchar(32) NOT NULL,
  `reason` varchar(500) NOT NULL DEFAULT '',
  `created_by` varchar(64) NOT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_roi_assignment_version` (`tenant_id`,`account_id`,`account_version`),
  KEY `ix_roi_assignment_time` (`tenant_id`,`account_id`,`effective_at`,`account_version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `roi_account_ban_history` (
  `id` varchar(36) NOT NULL,
  `tenant_id` bigint NOT NULL,
  `account_id` varchar(36) NOT NULL,
  `account_version` int NOT NULL,
  `ban_type` varchar(16) NOT NULL,
  `starts_at` datetime(3) NOT NULL,
  `ends_at` datetime(3) DEFAULT NULL,
  `permanent` tinyint NOT NULL,
  `reason` varchar(500) NOT NULL DEFAULT '',
  `source` varchar(32) NOT NULL,
  `source_session_id` varchar(36) DEFAULT NULL,
  `source_key` varchar(120) NOT NULL,
  `resolved_at` datetime(3) DEFAULT NULL,
  `resolved_by` varchar(64) DEFAULT NULL,
  `resolution_note` varchar(500) DEFAULT NULL,
  `created_by` varchar(64) NOT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_roi_ban_revision` (`tenant_id`,`account_id`,`source_key`),
  KEY `ix_roi_ban_account` (`tenant_id`,`account_id`,`ban_type`,`starts_at`),
  KEY `ix_roi_ban_session` (`tenant_id`,`source_session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `roi_account_qualification` (
  `id` varchar(36) NOT NULL,
  `tenant_id` bigint NOT NULL,
  `account_id` varchar(36) NOT NULL,
  `company_id` bigint NOT NULL,
  `blue_v` tinyint NOT NULL,
  `valid_from` datetime(3) NOT NULL,
  `valid_to` datetime(3) NOT NULL,
  `original_valid_to` datetime(3) NOT NULL,
  `reason` varchar(500) NOT NULL DEFAULT '',
  `evidence_ids` text NOT NULL,
  `version` int NOT NULL DEFAULT '1',
  `created_by` varchar(64) NOT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `closed_by` varchar(64) DEFAULT NULL,
  `closed_at` datetime(3) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_roi_qualification_account` (`tenant_id`,`account_id`,`valid_from`,`valid_to`),
  KEY `ix_roi_qualification_company` (`tenant_id`,`company_id`,`valid_from`,`valid_to`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `roi_approval_discovery_cursor` (
  `tenant_id` bigint NOT NULL,
  `type` varchar(32) COLLATE utf8mb4_bin NOT NULL,
  `process_code` varchar(128) COLLATE utf8mb4_bin NOT NULL,
  `window_start_ms` bigint DEFAULT NULL,
  `window_end_ms` bigint DEFAULT NULL,
  `next_token` bigint NOT NULL DEFAULT '0',
  `last_completed_ms` bigint DEFAULT NULL,
  `lease_until` datetime(3) DEFAULT NULL,
  `last_attempt_at` datetime(3) DEFAULT NULL,
  `last_success_at` datetime(3) DEFAULT NULL,
  `error_code` varchar(128) COLLATE utf8mb4_bin DEFAULT NULL,
  PRIMARY KEY (`tenant_id`,`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS `roi_approval_inbox` (
  `tenant_id` bigint NOT NULL,
  `instance_id` varchar(256) COLLATE utf8mb4_bin NOT NULL,
  `process_code` varchar(128) COLLATE utf8mb4_bin NOT NULL,
  `type` varchar(32) COLLATE utf8mb4_bin NOT NULL,
  `originator_dingtalk_id` varchar(128) COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `originator_backend_id` bigint DEFAULT NULL,
  `provider_status` varchar(32) COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `provider_result` varchar(32) COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `match_status` varchar(32) COLLATE utf8mb4_bin NOT NULL,
  `match_reason` varchar(255) COLLATE utf8mb4_bin NOT NULL,
  `record_id` char(36) COLLATE utf8mb4_bin DEFAULT NULL,
  `approval_record_id` char(36) COLLATE utf8mb4_bin DEFAULT NULL,
  `author_id` bigint DEFAULT NULL,
  `payload_json` longtext COLLATE utf8mb4_bin NOT NULL,
  `discovered_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`tenant_id`,`instance_id`),
  KEY `idx_roi_approval_inbox_state` (`tenant_id`,`match_status`,`type`),
  KEY `idx_roi_approval_inbox_owner` (`tenant_id`,`originator_backend_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS `roi_approval_sync_state` (
  `tenant_id` bigint NOT NULL,
  `record_id` char(36) COLLATE utf8mb4_bin NOT NULL,
  `next_attempt_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `lease_until` datetime(3) DEFAULT NULL,
  `last_attempt_at` datetime(3) DEFAULT NULL,
  `last_success_at` datetime(3) DEFAULT NULL,
  `error_code` varchar(128) COLLATE utf8mb4_bin DEFAULT NULL,
  `request_id` varchar(128) COLLATE utf8mb4_bin DEFAULT NULL,
  `failure_count` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`tenant_id`,`record_id`),
  KEY `idx_roi_approval_due` (`next_attempt_at`,`lease_until`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS `roi_attachment` (
  `id` char(36) NOT NULL,
  `tenant_id` bigint NOT NULL,
  `purpose` varchar(24) NOT NULL,
  `record_id` char(36) DEFAULT NULL,
  `created_by` bigint NOT NULL,
  `file_name` varchar(180) NOT NULL,
  `content_type` varchar(64) NOT NULL,
  `file_size` bigint NOT NULL,
  `storage_name` char(36) NOT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_roi_file` (`tenant_id`,`record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `roi_attachment_grant` (
  `token_hash` char(64) NOT NULL,
  `tenant_id` bigint NOT NULL,
  `attachment_id` char(36) NOT NULL,
  `expires_at` datetime NOT NULL,
  PRIMARY KEY (`token_hash`),
  KEY `idx_roi_grant_expiry` (`expires_at`),
  KEY `idx_roi_grant_file` (`tenant_id`,`attachment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `roi_audit` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `record_id` varchar(100) NOT NULL,
  `user_id` bigint NOT NULL,
  `action` varchar(32) NOT NULL,
  `details` longtext NOT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_roi_audit` (`tenant_id`,`record_id`,`id`)
) ENGINE=InnoDB AUTO_INCREMENT=735 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `roi_author_scope` (
  `tenant_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `author_id` bigint NOT NULL,
  PRIMARY KEY (`tenant_id`,`user_id`,`author_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `roi_process` (
  `tenant_id` bigint NOT NULL,
  `type` varchar(32) NOT NULL,
  `name` varchar(128) NOT NULL,
  `process_code` varchar(128) NOT NULL,
  `field_map` longtext NOT NULL,
  `enabled` tinyint NOT NULL DEFAULT '1',
  PRIMARY KEY (`tenant_id`,`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `roi_record` (
  `id` char(36) NOT NULL,
  `tenant_id` bigint NOT NULL,
  `kind` varchar(24) NOT NULL,
  `business_key` varchar(160) NOT NULL,
  `author_id` bigint DEFAULT NULL,
  `created_by` bigint NOT NULL,
  `updated_by` bigint NOT NULL,
  `version` int NOT NULL DEFAULT '1',
  `payload` longtext NOT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_roi_business` (`tenant_id`,`kind`,`business_key`),
  KEY `idx_roi_author` (`tenant_id`,`kind`,`author_id`),
  CONSTRAINT `ck_roi_json` CHECK (json_valid(`payload`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `roi_role` (
  `tenant_id` bigint NOT NULL,
  `subject_id` varchar(64) NOT NULL,
  `role` varchar(24) NOT NULL,
  PRIMARY KEY (`tenant_id`,`subject_id`,`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `roi_role_override` (
  `tenant_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`tenant_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `roi_sequence` (
  `tenant_id` bigint NOT NULL,
  `seq_key` varchar(64) NOT NULL,
  `next_value` bigint NOT NULL,
  PRIMARY KEY (`tenant_id`,`seq_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `roi_session_attribution_article` (
  `tenant_id` bigint NOT NULL,
  `session_id` varchar(36) NOT NULL,
  `source_type` varchar(24) NOT NULL,
  `source_id` varchar(64) NOT NULL,
  `author_id` varchar(64) NOT NULL,
  PRIMARY KEY (`tenant_id`,`source_type`,`source_id`),
  KEY `ix_roi_attribution_session` (`tenant_id`,`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `roi_session_attribution_binding` (
  `tenant_id` bigint NOT NULL,
  `session_id` varchar(36) NOT NULL,
  `author_id` varchar(64) NOT NULL,
  `binding_payload` mediumtext NOT NULL,
  `created_by` varchar(64) NOT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`tenant_id`,`session_id`),
  KEY `ix_roi_attribution_author` (`tenant_id`,`author_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `roi_wecom_callback_state` (
  `tenant_id` bigint NOT NULL,
  `config_id` bigint NOT NULL,
  `verified_at` datetime(3) DEFAULT NULL,
  `last_event_at` datetime(3) DEFAULT NULL,
  PRIMARY KEY (`tenant_id`,`config_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS `roi_wecom_event` (
  `tenant_id` bigint NOT NULL,
  `event_key` char(64) COLLATE utf8mb4_bin NOT NULL,
  `config_id` bigint NOT NULL,
  `corp_id` varchar(128) COLLATE utf8mb4_bin NOT NULL,
  `event_type` varchar(64) COLLATE utf8mb4_bin NOT NULL,
  `change_type` varchar(64) COLLATE utf8mb4_bin NOT NULL,
  `external_user_id` varchar(128) COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `member_user_id` varchar(128) COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `state_value` varchar(255) COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `event_time` datetime(3) NOT NULL,
  `received_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `link_id` varchar(32) COLLATE utf8mb4_bin DEFAULT NULL,
  `session_id` char(36) COLLATE utf8mb4_bin DEFAULT NULL,
  `session_no` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL,
  `author_id` bigint DEFAULT NULL,
  `attribution_status` varchar(32) COLLATE utf8mb4_bin NOT NULL,
  `reason` varchar(255) COLLATE utf8mb4_bin NOT NULL,
  `payload_json` longtext COLLATE utf8mb4_bin NOT NULL,
  PRIMARY KEY (`tenant_id`,`event_key`),
  KEY `idx_roi_wecom_session` (`tenant_id`,`session_id`,`change_type`),
  KEY `idx_roi_wecom_event_received` (`tenant_id`,`received_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS `roi_wecom_session_link` (
  `id` char(36) COLLATE utf8mb4_bin NOT NULL,
  `tenant_id` bigint NOT NULL,
  `corp_id` varchar(128) COLLATE utf8mb4_bin NOT NULL,
  `link_id` varchar(32) COLLATE utf8mb4_bin NOT NULL,
  `session_id` char(36) COLLATE utf8mb4_bin NOT NULL,
  `session_no` varchar(64) COLLATE utf8mb4_bin NOT NULL,
  `author_id` bigint NOT NULL,
  `attribution_state` char(32) COLLATE utf8mb4_bin NOT NULL,
  `created_by` bigint NOT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_roi_wecom_state` (`tenant_id`,`corp_id`,`attribution_state`),
  UNIQUE KEY `uk_roi_wecom_session_link` (`tenant_id`,`session_id`,`link_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS `roi_wecom_welcome_outbox` (
  `id` char(36) COLLATE utf8mb4_bin NOT NULL,
  `tenant_id` bigint NOT NULL,
  `event_key` char(64) COLLATE utf8mb4_bin NOT NULL,
  `corp_id` varchar(128) COLLATE utf8mb4_bin NOT NULL,
  `config_id` bigint NOT NULL,
  `link_id` varchar(32) COLLATE utf8mb4_bin NOT NULL,
  `code_hash` char(64) COLLATE utf8mb4_bin NOT NULL,
  `sealed_code` text COLLATE utf8mb4_bin NOT NULL,
  `content_json` longtext COLLATE utf8mb4_bin NOT NULL,
  `status` varchar(32) COLLATE utf8mb4_bin NOT NULL,
  `attempt_count` tinyint NOT NULL DEFAULT '0',
  `error_code` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `expires_at` datetime(3) NOT NULL,
  `attempted_at` datetime(3) DEFAULT NULL,
  `finished_at` datetime(3) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_roi_wecom_welcome_event` (`tenant_id`,`event_key`),
  UNIQUE KEY `uk_roi_wecom_welcome_code` (`tenant_id`,`corp_id`,`code_hash`),
  KEY `idx_roi_wecom_welcome_pending` (`status`,`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

-- =============================================================================
-- Section 02: OA 费用行归因 ops_oa_cost_link
-- SSOT: 20261025_oa_cost_links.sql
-- =============================================================================

-- Apply to shenyu-ops before deploying the OA presentation/linking service. Safe to rerun.
-- Independent local attribution; does not modify DingTalk payloads, costs, payment, employee rights or historical data.
CREATE TABLE IF NOT EXISTS ops_oa_cost_link (
 tenant_id BIGINT NOT NULL, oa_id CHAR(36) NOT NULL, line_key VARCHAR(80) NOT NULL,
 anchor_id VARCHAR(64) NOT NULL DEFAULT '', category VARCHAR(32) NOT NULL,
 project VARCHAR(256) NOT NULL DEFAULT '', session_id CHAR(36) NOT NULL DEFAULT '',
 account_id CHAR(36) NOT NULL DEFAULT '', allocation_amount DECIMAL(18,2) NULL,
 updated_by VARCHAR(64) NOT NULL, updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
 PRIMARY KEY(tenant_id,oa_id,line_key), KEY ix_oa_link_anchor(tenant_id,anchor_id),
 KEY ix_oa_link_session(tenant_id,session_id), KEY ix_oa_link_category(tenant_id,category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
-- Verify: SHOW CREATE TABLE ops_oa_cost_link; SELECT COUNT(*) FROM ops_oa_cost_link;
-- Rollback application first; preserve this additive table and its attribution records. No destructive rollback is required.

-- =============================================================================
-- Section 03: 审批成本候选 + oa_account_cost 扩展列
-- SSOT: ops_roi_approval_cost_20261026.sql
-- =============================================================================

-- 1. Run against the OPS business database (independent test: `shenyu-ops`).
-- Prerequisites: ops_oa_schema_20261022.sql and the existing oa_account_cost table.
-- Existing ops_oa_cost_link belongs to livestream attribution and is preserved.
-- Additive, repeatable migration. Does not import approvals, grant roles, or create costs.
-- Back up oa_account_cost, ops_oa_template and ops_oa_instance before execution.
CREATE TABLE IF NOT EXISTS ops_oa_cost_candidate (
 id CHAR(36) NOT NULL, tenant_id BIGINT NOT NULL, instance_id VARCHAR(256) NOT NULL,
 business_id VARCHAR(128) NOT NULL, process_code VARCHAR(128) NOT NULL,
 source_key CHAR(64) NOT NULL, source_hash CHAR(64) NOT NULL, row_key VARCHAR(190) NOT NULL,
 amount DECIMAL(16,2) NULL, cost_date DATE NULL, state VARCHAR(24) NOT NULL DEFAULT 'PENDING',
 version INT NOT NULL DEFAULT 1, payload_json LONGTEXT NOT NULL, review_json LONGTEXT NULL,
 source_synced_at DATETIME(3) NOT NULL,
 created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 PRIMARY KEY(id), UNIQUE KEY uq_oa_cost_candidate(tenant_id,source_key),
 KEY ix_oa_cost_queue(tenant_id,state,cost_date), KEY ix_oa_cost_instance(tenant_id,instance_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS ops_oa_account_cost_link (
 tenant_id BIGINT NOT NULL, candidate_id CHAR(36) NOT NULL, cost_id BIGINT NOT NULL,
 allocation_index INT NOT NULL, link_type VARCHAR(16) NOT NULL, amount DECIMAL(16,2) NOT NULL,
 PRIMARY KEY(tenant_id,candidate_id,allocation_index), KEY ix_oa_cost_link(tenant_id,cost_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS ops_oa_cost_alias (
 tenant_id BIGINT NOT NULL, alias VARCHAR(160) NOT NULL, account_id BIGINT NOT NULL,
 created_by BIGINT NOT NULL, created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 PRIMARY KEY(tenant_id,alias), KEY ix_oa_cost_alias_account(tenant_id,account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

-- Routine is removed after execution; no credentials or tenant-specific source config.
DROP PROCEDURE IF EXISTS football_roi_approval_cost_20261026;
-- ---------------------------------------------------------------------------
-- 首个 DELIMITER 块（Section 03 存储过程）起 — 若上一段 1419 未处理，此处开始可能报错
-- ---------------------------------------------------------------------------

DELIMITER $$
CREATE PROCEDURE football_roi_approval_cost_20261026()
BEGIN
 IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='oa_account_cost' AND COLUMN_NAME='cost_date') THEN
  ALTER TABLE oa_account_cost ADD COLUMN cost_date DATE NULL COMMENT 'Recognized expense date; legacy records fall back to pay_date';
 END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='oa_account_cost' AND COLUMN_NAME='source_type') THEN
  ALTER TABLE oa_account_cost ADD COLUMN source_type VARCHAR(16) NOT NULL DEFAULT 'MANUAL';
 END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='oa_account_cost' AND COLUMN_NAME='source_key') THEN
  ALTER TABLE oa_account_cost ADD COLUMN source_key CHAR(64) NULL;
 END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='oa_account_cost' AND COLUMN_NAME='source_candidate_id') THEN
  ALTER TABLE oa_account_cost ADD COLUMN source_candidate_id CHAR(36) NULL;
 END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='oa_account_cost' AND COLUMN_NAME='approval_business_id') THEN
  ALTER TABLE oa_account_cost ADD COLUMN approval_business_id VARCHAR(128) NULL;
 END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='oa_account_cost' AND COLUMN_NAME='approval_instance_id') THEN
  ALTER TABLE oa_account_cost ADD COLUMN approval_instance_id VARCHAR(256) NULL;
 END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='oa_account_cost' AND COLUMN_NAME='author_id') THEN
  ALTER TABLE oa_account_cost ADD COLUMN author_id BIGINT NULL;
 END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='oa_account_cost' AND COLUMN_NAME='attribution_ip_group_id') THEN
  ALTER TABLE oa_account_cost ADD COLUMN attribution_ip_group_id BIGINT NULL;
 END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='oa_account_cost' AND COLUMN_NAME='payment_state') THEN
  ALTER TABLE oa_account_cost ADD COLUMN payment_state VARCHAR(16) NOT NULL DEFAULT 'LEGACY';
 END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='oa_account_cost' AND COLUMN_NAME='paid_reference') THEN
  ALTER TABLE oa_account_cost ADD COLUMN paid_reference VARCHAR(200) NULL;
 END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='oa_account_cost' AND COLUMN_NAME='payment_note') THEN
  ALTER TABLE oa_account_cost ADD COLUMN payment_note TEXT NULL;
 END IF;
 IF EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='oa_account_cost' AND COLUMN_NAME='pay_date' AND IS_NULLABLE='NO') THEN
  ALTER TABLE oa_account_cost MODIFY COLUMN pay_date DATE NULL COMMENT 'Actual payment date; unknown until cashier registration';
 END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='oa_account_cost' AND INDEX_NAME='uq_oa_cost_source_key') THEN
  ALTER TABLE oa_account_cost ADD UNIQUE KEY uq_oa_cost_source_key(tenant_id,source_key);
 END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='oa_account_cost' AND INDEX_NAME='ix_oa_cost_date') THEN
  ALTER TABLE oa_account_cost ADD KEY ix_oa_cost_date(tenant_id,cost_date,account_id);
 END IF;
END$$
DELIMITER ;
CALL football_roi_approval_cost_20261026();
DROP PROCEDURE football_roi_approval_cost_20261026;

-- Verify: SHOW COLUMNS FROM oa_account_cost; SELECT state,COUNT(*) FROM ops_oa_cost_candidate GROUP BY state;
-- Keep all tables/links on rollback. Never drop confirmed financial history.
-- A pre-feature package does not understand cost_date: after any confirmation, retain
-- the date-aware ROI code even when disabling the new workbench. See release notes.

-- =============================================================================
-- Section 05: 组织目录 / IP 组范围
-- SSOT: 20261026_roi_org_directory.sql
-- =============================================================================

-- 抖音投产：钉钉组织缓存、精确岗位映射、IP组授权
-- 库：shenyu-ops；MySQL 8.4。仅建增量表/触发器，不覆盖现有账号或业务角色。
-- 重复执行安全；CREATE IF NOT EXISTS 不会重置已配置数据。
-- 验证：SHOW TABLES LIKE 'roi_org_%'; SHOW TRIGGERS LIKE 'roi_org_%';
USE `shenyu-ops`;
CREATE TABLE IF NOT EXISTS roi_org_member (
 tenant_id BIGINT NOT NULL,dingtalk_user_id VARCHAR(64) NOT NULL,name VARCHAR(160) NOT NULL,job_title VARCHAR(160) NOT NULL DEFAULT '',
 departments_json JSON NOT NULL,labels_json JSON NOT NULL,labels_synced_at DATETIME(3) NULL,active TINYINT NOT NULL DEFAULT 1,
 synced_at DATETIME(3) NOT NULL,PRIMARY KEY(tenant_id,dingtalk_user_id),KEY ix_roi_org_title(tenant_id,job_title)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS roi_org_role_mapping (
 id VARCHAR(36) NOT NULL PRIMARY KEY,tenant_id BIGINT NOT NULL,source_type VARCHAR(16) NOT NULL,source_key VARCHAR(160) NOT NULL,
 source_name VARCHAR(160) NOT NULL,role VARCHAR(24) NOT NULL,enabled TINYINT NOT NULL DEFAULT 1,
 UNIQUE KEY uq_roi_org_mapping(tenant_id,source_type,source_key,role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS roi_ip_group_scope (
 tenant_id BIGINT NOT NULL,user_id BIGINT NOT NULL,ip_group_id BIGINT NOT NULL,
 PRIMARY KEY(tenant_id,user_id,ip_group_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS roi_org_sync_state (
 tenant_id BIGINT NOT NULL PRIMARY KEY,status VARCHAR(32) NOT NULL,last_synced_at DATETIME(3) NULL,
 member_count INT NOT NULL DEFAULT 0,department_count INT NOT NULL DEFAULT 0,complete TINYINT NOT NULL DEFAULT 0,error_code VARCHAR(255) NOT NULL DEFAULT ''
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- Section 06: 岗位→角色默认映射（tenant=1）
-- SSOT: 20261026_roi_org_role_defaults.sql
-- =============================================================================

-- 本公司投产应用的初始准确岗位映射，依赖 20261026_roi_org_directory.sql。
-- 当前组织租户为1；部署到其它租户需由管理员按实际组织调整，不能跨租户套用。
-- 已明确个人角色优先；此脚本不更改任何现有人员权限，不映射总管理员/负责人。
-- 重跑保留管理员已编辑或停用的规则；同一来源职位已存在任意目标角色时不再补默认授权。
-- 只补缺少的精确来源岗位，不能用新增默认角色覆盖管理员自定义的同源配置。
USE `shenyu-ops`;
SET @roi_org_seed_tenant=1;
INSERT IGNORE INTO roi_org_role_mapping(id,tenant_id,source_type,source_key,source_name,role,enabled) SELECT '8c68c27750c2102c9d8745de3735ce3b',@roi_org_seed_tenant,'title','全盘财务','全盘财务','FINANCE',1 WHERE NOT EXISTS (SELECT 1 FROM roi_org_role_mapping WHERE tenant_id=@roi_org_seed_tenant AND source_type='title' AND source_key='全盘财务');
INSERT IGNORE INTO roi_org_role_mapping(id,tenant_id,source_type,source_key,source_name,role,enabled) SELECT 'e4d4cc12449f7cf45c01bdeadc07775b',@roi_org_seed_tenant,'title','财务','财务','FINANCE',1 WHERE NOT EXISTS (SELECT 1 FROM roi_org_role_mapping WHERE tenant_id=@roi_org_seed_tenant AND source_type='title' AND source_key='财务');
INSERT IGNORE INTO roi_org_role_mapping(id,tenant_id,source_type,source_key,source_name,role,enabled) SELECT '73009c4110e7a6b6cbdfcf7ce18e2d4e',@roi_org_seed_tenant,'title','出纳','出纳','CASHIER',1 WHERE NOT EXISTS (SELECT 1 FROM roi_org_role_mapping WHERE tenant_id=@roi_org_seed_tenant AND source_type='title' AND source_key='出纳');
INSERT IGNORE INTO roi_org_role_mapping(id,tenant_id,source_type,source_key,source_name,role,enabled) SELECT 'aff3cd0934582bd4a7dd7e5f9f0c6347',@roi_org_seed_tenant,'title','账号管理专员','账号管理专员','ACCOUNT',1 WHERE NOT EXISTS (SELECT 1 FROM roi_org_role_mapping WHERE tenant_id=@roi_org_seed_tenant AND source_type='title' AND source_key='账号管理专员');
INSERT IGNORE INTO roi_org_role_mapping(id,tenant_id,source_type,source_key,source_name,role,enabled) SELECT '49a31d56f038146774daceca597181b0',@roi_org_seed_tenant,'title','账号管理员','账号管理员','ACCOUNT',1 WHERE NOT EXISTS (SELECT 1 FROM roi_org_role_mapping WHERE tenant_id=@roi_org_seed_tenant AND source_type='title' AND source_key='账号管理员');
INSERT IGNORE INTO roi_org_role_mapping(id,tenant_id,source_type,source_key,source_name,role,enabled) SELECT 'd8eab2d7c9897dd2e3442c3d139e00c6',@roi_org_seed_tenant,'title','流量运营','流量运营','MEDIA',1 WHERE NOT EXISTS (SELECT 1 FROM roi_org_role_mapping WHERE tenant_id=@roi_org_seed_tenant AND source_type='title' AND source_key='流量运营');
INSERT IGNORE INTO roi_org_role_mapping(id,tenant_id,source_type,source_key,source_name,role,enabled) SELECT 'c8fda3d533d71783799adb505d152a20',@roi_org_seed_tenant,'title','新媒体运营','新媒体运营','MEDIA',1 WHERE NOT EXISTS (SELECT 1 FROM roi_org_role_mapping WHERE tenant_id=@roi_org_seed_tenant AND source_type='title' AND source_key='新媒体运营');
INSERT IGNORE INTO roi_org_role_mapping(id,tenant_id,source_type,source_key,source_name,role,enabled) SELECT '47492f884e99a4b05a2a9f64579ca37c',@roi_org_seed_tenant,'title','新媒体运营总监','新媒体运营总监','MANAGER',1 WHERE NOT EXISTS (SELECT 1 FROM roi_org_role_mapping WHERE tenant_id=@roi_org_seed_tenant AND source_type='title' AND source_key='新媒体运营总监');
INSERT IGNORE INTO roi_org_role_mapping(id,tenant_id,source_type,source_key,source_name,role,enabled) SELECT '13a48d47a584740e0226ae17a501db9e',@roi_org_seed_tenant,'title','主播','主播','HOST',1 WHERE NOT EXISTS (SELECT 1 FROM roi_org_role_mapping WHERE tenant_id=@roi_org_seed_tenant AND source_type='title' AND source_key='主播');
-- 核验：SELECT source_key,role,enabled FROM roi_org_role_mapping WHERE tenant_id=@roi_org_seed_tenant;
-- 回退：通过应用岗位匹配规则停用/移除；不要清空整个映射表或删除历史日志。

-- =============================================================================
-- Section 07: 企微管理扩展表
-- SSOT: 20261026_roi_wecom_management_restore.sql
-- =============================================================================

-- 2026-10-26: restore actual WeCom management routes used by the existing ROI UI.
-- CREATE IF NOT EXISTS preserves existing data. No secrets or real customer records are seeded.
-- Default background customer-detail sync stays false; existing welcome send switch is unchanged.
-- Validation: SHOW TABLES LIKE 'roi_wecom_%'; inspect roi_wfm_* triggers; authenticated GET /wecom/manage/variables.
-- Rollback: roll application back; keep these additive tables and evidence. Drop only roi_wfm_* triggers if required.
USE `shenyu-ops`;
-- Additive WeCom link ownership / new-customer evidence and private welcome media.
-- Apply to the existing ops master schema, preserve all prior business records.
CREATE TABLE IF NOT EXISTS roi_wecom_link_owner_history (
 id VARCHAR(36) NOT NULL, tenant_id BIGINT NOT NULL, corp_id VARCHAR(128) NOT NULL,
 link_id VARCHAR(32) NOT NULL, author_id VARCHAR(32) NOT NULL,
 effective_from DATETIME(3) NOT NULL, effective_to DATETIME(3) NULL,
 created_by VARCHAR(32) NOT NULL, created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 PRIMARY KEY(id), KEY idx_roi_link_owner_time(tenant_id,link_id,effective_from,effective_to)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS roi_wecom_link_operation (
 id VARCHAR(36) NOT NULL, tenant_id BIGINT NOT NULL, request_id VARCHAR(100) NOT NULL,
 link_id VARCHAR(32) NOT NULL, actor_id VARCHAR(32) NOT NULL, request_hash CHAR(64) NOT NULL,
 status VARCHAR(40) NOT NULL, requested_json MEDIUMTEXT NOT NULL, result_json MEDIUMTEXT NULL,
 created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), finished_at DATETIME(3) NULL,
 PRIMARY KEY(id), UNIQUE KEY uk_roi_link_request(tenant_id,request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS roi_wecom_customer_relation (
 id VARCHAR(36) NOT NULL, tenant_id BIGINT NOT NULL, relation_key CHAR(64) NOT NULL,
 corp_id VARCHAR(128) NOT NULL, config_id VARCHAR(32) NOT NULL, link_id VARCHAR(32) NULL,
 author_id VARCHAR(32) NULL, session_id VARCHAR(36) NULL, session_no VARCHAR(80) NULL,
 external_user_id VARCHAR(128) NOT NULL, member_user_id VARCHAR(128) NOT NULL,
 state_value VARCHAR(255) NOT NULL DEFAULT '', customer_name VARCHAR(256) NOT NULL DEFAULT '',
 member_name VARCHAR(256) NOT NULL DEFAULT '', tags_json TEXT NOT NULL, tag_ids_json TEXT NULL,
 added_at DATETIME(3) NULL, added_time_source VARCHAR(40) NOT NULL DEFAULT 'unknown',
 chat_status INT NULL, attribution_status VARCHAR(40) NOT NULL,
 evidence_source VARCHAR(40) NOT NULL, detail_status VARCHAR(40) NOT NULL, detail_cursor VARCHAR(2048) NOT NULL DEFAULT '',
 first_seen_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 PRIMARY KEY(id), UNIQUE KEY uk_roi_customer_relation(tenant_id,relation_key),
 KEY idx_roi_customer_filter(tenant_id,author_id,link_id,added_at),
 KEY idx_roi_customer_member(tenant_id,member_user_id,added_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS roi_wecom_customer_sync (
 tenant_id BIGINT NOT NULL, link_id VARCHAR(32) NOT NULL, config_id VARCHAR(32) NOT NULL,
 next_cursor VARCHAR(2048) NOT NULL DEFAULT '', cycle_complete TINYINT NOT NULL DEFAULT 0,
 pages_fetched INT NOT NULL DEFAULT 0, last_success_at DATETIME(3) NULL,
 last_error_code VARCHAR(40) NOT NULL DEFAULT '', updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 PRIMARY KEY(tenant_id,link_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS roi_wecom_media (
 id VARCHAR(36) NOT NULL, tenant_id BIGINT NOT NULL, corp_id VARCHAR(128) NOT NULL,
 config_id VARCHAR(32) NOT NULL, link_id VARCHAR(32) NOT NULL, author_id VARCHAR(32) NOT NULL,
 created_by VARCHAR(32) NOT NULL, file_name VARCHAR(256) NOT NULL, content_type VARCHAR(100) NOT NULL,
 file_size BIGINT NOT NULL, storage_name VARCHAR(100) NOT NULL, media_type VARCHAR(20) NOT NULL,
 provider_media_id VARCHAR(256) NOT NULL DEFAULT '', provider_expires_at DATETIME(3) NULL,
 provider_status VARCHAR(40) NOT NULL DEFAULT 'local_only',
 created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 PRIMARY KEY(id), KEY idx_roi_welcome_media_scope(tenant_id,link_id,created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS roi_wecom_customer_api_budget (
 tenant_id BIGINT NOT NULL, config_id VARCHAR(32) NOT NULL, budget_day DATE NOT NULL,
 detail_calls INT NOT NULL DEFAULT 0, next_retry_at DATETIME(3) NULL,
 last_error_code VARCHAR(40) NOT NULL DEFAULT '', updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 PRIMARY KEY(tenant_id,config_id,budget_day)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =============================================================================
-- Section 09: 作者/运营责任 + 费用来源/归属
-- SSOT: roi_author_cost_foundation_20261029.sql
-- =============================================================================

-- Author/operator assignment and cost-source relation foundation, 2026-10-29.
-- Target: the OPS business database `shenyu-ops`; MySQL >= 8.0.29.
-- Additive and repeatable: four empty relation tables with new-table audit only. No cost copy,
-- historical assignment backfill, source rewrite, permission grant, or payment.
-- Times are Asia/Shanghai business-local DATETIME(3), including application writes.
-- Active intervals are [starts_at, ends_at); NULL ends_at means no upper bound.
-- Java must lock the existing author/account rows and check interval overlap.
-- CREATE IF NOT EXISTS preserves existing rows but does not repair schema drift.
-- See roi_author_cost_foundation_20261029.md for preflight, verification, rollback.
USE `shenyu-ops`;
SET SESSION time_zone = '+08:00';

CREATE TABLE IF NOT EXISTS `roi_author_operator_assignment` (
  `id` CHAR(36) NOT NULL COMMENT 'Application-generated UUID',
  `tenant_id` BIGINT NOT NULL,
  `author_id` BIGINT NOT NULL COMMENT 'member author_user.id; not employee user ID',
  `operator_user_id` BIGINT NOT NULL COMMENT 'Football administrator/operator user ID',
  `starts_at` DATETIME(3) NOT NULL COMMENT 'Inclusive, Asia/Shanghai business-local time',
  `ends_at` DATETIME(3) NULL COMMENT 'Exclusive; NULL means no upper bound',
  `state` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE=valid history; VOID=invalidated',
  `version` INT NOT NULL DEFAULT 1,
  `reason` VARCHAR(500) NOT NULL DEFAULT '',
  `evidence` VARCHAR(1000) NOT NULL DEFAULT '' COMMENT 'Evidence references; never credentials or attachment contents',
  `created_by` BIGINT NOT NULL,
  `updated_by` BIGINT NOT NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `ix_roi_author_op_author_time` (`tenant_id`, `author_id`, `starts_at`),
  KEY `ix_roi_author_op_user_time` (`tenant_id`, `operator_user_id`, `starts_at`),
  CONSTRAINT `ck_roi_author_op_state` CHECK (`state` IN ('ACTIVE', 'VOID')),
  CONSTRAINT `ck_roi_author_op_version` CHECK (`version` >= 1),
  CONSTRAINT `ck_roi_author_op_interval` CHECK (`ends_at` IS NULL OR `ends_at` > `starts_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin
  COMMENT='Author to sole operator assignment history; overlap checked under Java row locks';

CREATE TABLE IF NOT EXISTS `roi_account_operator_assignment` (
  `id` CHAR(36) NOT NULL COMMENT 'Application-generated UUID',
  `tenant_id` BIGINT NOT NULL,
  `account_id` CHAR(36) NOT NULL COMMENT 'ROI account roi_record.id; not oa_account.id',
  `author_assignment_id` CHAR(36) NOT NULL COMMENT 'roi_author_operator_assignment.id in the same tenant',
  `author_id` BIGINT NOT NULL,
  `operator_user_id` BIGINT NOT NULL,
  `starts_at` DATETIME(3) NOT NULL COMMENT 'Inclusive, Asia/Shanghai business-local time',
  `ends_at` DATETIME(3) NULL COMMENT 'Exclusive; NULL means no upper bound',
  `state` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE=valid history; VOID=invalidated',
  `version` INT NOT NULL DEFAULT 1,
  `reason` VARCHAR(500) NOT NULL DEFAULT '',
  `evidence` VARCHAR(1000) NOT NULL DEFAULT '' COMMENT 'Evidence references; never credentials or attachment contents',
  `created_by` BIGINT NOT NULL,
  `updated_by` BIGINT NOT NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `ix_roi_account_op_account_time` (`tenant_id`, `account_id`, `starts_at`),
  KEY `ix_roi_account_op_assignment` (`tenant_id`, `author_assignment_id`),
  KEY `ix_roi_account_op_author_time` (`tenant_id`, `author_id`, `starts_at`),
  KEY `ix_roi_account_op_user_time` (`tenant_id`, `operator_user_id`, `starts_at`),
  CONSTRAINT `ck_roi_account_op_state` CHECK (`state` IN ('ACTIVE', 'VOID')),
  CONSTRAINT `ck_roi_account_op_version` CHECK (`version` >= 1),
  CONSTRAINT `ck_roi_account_op_interval` CHECK (`ends_at` IS NULL OR `ends_at` > `starts_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin
  COMMENT='Account claim history tied to author/operator assignment; no historical backfill';

CREATE TABLE IF NOT EXISTS `roi_cost_source` (
  `id` CHAR(36) NOT NULL COMMENT 'Application-generated UUID',
  `tenant_id` BIGINT NOT NULL,
  `source_type` VARCHAR(24) NOT NULL COMMENT 'ACCOUNT_COST or ROI_EXPENSE',
  `source_id` VARCHAR(128) NOT NULL COMMENT 'Exact source ID; no copied amount or payload',
  `canonical_type` VARCHAR(24) NOT NULL COMMENT 'ACCOUNT_COST or ROI_EXPENSE',
  `canonical_id` VARCHAR(128) NOT NULL COMMENT 'Exact ID of the retained canonical source',
  `source_hash` CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'SHA-256 of source financial snapshot',
  `canonical_hash` CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'SHA-256 of canonical financial snapshot',
  `state` VARCHAR(24) NOT NULL DEFAULT 'LINKED' COMMENT 'LINKED=deduplicated; UNLINKED=relation retained but inactive',
  `reason` VARCHAR(1000) NOT NULL DEFAULT '',
  `version` INT NOT NULL DEFAULT 1,
  `created_by` BIGINT NOT NULL,
  `updated_by` BIGINT NOT NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_roi_cost_source` (`tenant_id`, `source_type`, `source_id`),
  KEY `ix_roi_cost_source_canonical` (`tenant_id`, `canonical_type`, `canonical_id`),
  CONSTRAINT `ck_roi_cost_source_type` CHECK (`source_type` IN ('ACCOUNT_COST', 'ROI_EXPENSE')),
  CONSTRAINT `ck_roi_cost_canonical_type` CHECK (`canonical_type` IN ('ACCOUNT_COST', 'ROI_EXPENSE')),
  CONSTRAINT `ck_roi_cost_source_state` CHECK (`state` IN ('LINKED', 'UNLINKED')),
  CONSTRAINT `ck_roi_cost_source_version` CHECK (`version` >= 1),
  CONSTRAINT `ck_roi_cost_source_hash` CHECK (CHAR_LENGTH(`source_hash`) = 64),
  CONSTRAINT `ck_roi_cost_canonical_hash` CHECK (CHAR_LENGTH(`canonical_hash`) = 64)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin
  COMMENT='Explicit duplicate-source relation only; canonical cost remains in its original ledger';

CREATE TABLE IF NOT EXISTS `roi_cost_attribution` (
  `id` CHAR(36) NOT NULL COMMENT 'Application-generated UUID',
  `tenant_id` BIGINT NOT NULL,
  `source_type` VARCHAR(24) NOT NULL COMMENT 'ACCOUNT_COST or ROI_EXPENSE',
  `source_id` VARCHAR(128) NOT NULL COMMENT 'Exact source ID; no copied amount or payload',
  `occurred_at` DATETIME(3) NULL COMMENT 'Finance-confirmed business-local occurrence override; NULL means no override',
  `operator_user_id` BIGINT NULL COMMENT 'Finance-confirmed operator override; NULL means no override',
  `author_id` BIGINT NULL COMMENT 'Finance-confirmed author override; NULL means no override',
  `source_hash` CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'SHA-256 guarding the original financial snapshot',
  `reason` VARCHAR(1000) NOT NULL DEFAULT '',
  `evidence` VARCHAR(1000) NOT NULL DEFAULT '' COMMENT 'Evidence references; never credentials or attachment contents',
  `version` INT NOT NULL DEFAULT 1,
  `created_by` BIGINT NOT NULL,
  `updated_by` BIGINT NOT NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_roi_cost_attribution` (`tenant_id`, `source_type`, `source_id`),
  KEY `ix_roi_cost_attr_author_time` (`tenant_id`, `author_id`, `occurred_at`),
  KEY `ix_roi_cost_attr_user_time` (`tenant_id`, `operator_user_id`, `occurred_at`),
  CONSTRAINT `ck_roi_cost_attr_type` CHECK (`source_type` IN ('ACCOUNT_COST', 'ROI_EXPENSE')),
  CONSTRAINT `ck_roi_cost_attr_version` CHECK (`version` >= 1),
  CONSTRAINT `ck_roi_cost_attr_hash` CHECK (CHAR_LENGTH(`source_hash`) = 64)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin
  COMMENT='Finance attribution overrides guarded by source hash; original cost is unchanged';


-- New-table audit only: no existing table or trigger is replaced.
-- Structural snapshots deliberately omit reason/evidence values; changed_fields
-- may name these fields, but never contains their text. Logs remain UTC while
-- the assignment business-time fields remain Asia/Shanghai-local DATETIME(3).

-- =============================================================================
-- Section 10: 费用业务分类 roi_cost_classification
-- SSOT: roi_cost_business_classification_20261030.sql
-- =============================================================================

-- Target shenyu-ops; MySQL >= 8.0.29. Run after roi_author_cost_foundation_20261029.sql.
-- Additive empty evidence sidecar only. No historical backfill, payment/approval update or ACL grant.
-- Missing rows remain PENDING. Scope is checked against current native account/session facts on each read.
USE `shenyu-ops`;
CREATE TABLE IF NOT EXISTS `roi_cost_classification` (
  `id` CHAR(36) NOT NULL,
  `tenant_id` BIGINT NOT NULL,
  `source_type` VARCHAR(24) NOT NULL,
  `source_id` VARCHAR(128) NOT NULL,
  `source_hash` CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `classification_json` JSON NOT NULL COMMENT 'Explicit businessType, native author/account/session, platform ID, period and evidence; never inferred from remarks',
  `version` INT NOT NULL DEFAULT 1,
  `created_by` BIGINT NOT NULL,
  `updated_by` BIGINT NOT NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_roi_cost_classification_source` (`tenant_id`,`source_type`,`source_id`),
  CONSTRAINT `ck_roi_cost_classification_source` CHECK (`source_type` IN ('ACCOUNT_COST','ROI_EXPENSE')),
  CONSTRAINT `ck_roi_cost_classification_version` CHECK (`version` >= 1),
  CONSTRAINT `ck_roi_cost_classification_business` CHECK (JSON_CONTAINS_PATH(`classification_json`,'one','$.businessType') = 1 AND JSON_TYPE(JSON_EXTRACT(`classification_json`,'$.businessType')) = 'STRING' AND JSON_UNQUOTE(JSON_EXTRACT(`classification_json`,'$.businessType')) IN ('PENDING','DOUYIN_LIVE','OTHER'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin
  COMMENT='Explicit business cost classification; source financial identity and payment remain unchanged';

-- New-table audit only; evidence text and approval/payment payloads are intentionally excluded.

-- =============================================================================
-- Section 11: 媒体只读委托关系
-- SSOT: 20261030_roi_media_read_delegation.sql
-- =============================================================================

-- Database: shenyu-ops. Requires existing ROI role/scope/audit tables and system native users.
-- Additive EMPTY relations only. No identity, role, manager or employee is seeded.
-- Execute before the matching football-selfhost runtime; repeatable MySQL 8.0+ DDL.
CREATE TABLE IF NOT EXISTS roi_media_delegate_manager (
  tenant_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 0,
  evidence VARCHAR(500) NOT NULL,
  updated_by BIGINT NOT NULL,
  version INT NOT NULL DEFAULT 1,
  updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (tenant_id,user_id),
  CONSTRAINT chk_roi_media_manager_enabled CHECK(enabled IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS roi_media_read_grant (
  id VARCHAR(36) NOT NULL,
  tenant_id BIGINT NOT NULL,
  grantor_user_id BIGINT NOT NULL,
  grantee_user_id BIGINT NOT NULL,
  author_id BIGINT NOT NULL,
  reason VARCHAR(500) NOT NULL,
  expires_at DATETIME(3) NOT NULL,
  revoked_at DATETIME(3) NULL,
  revoked_by BIGINT NULL,
  revoke_reason VARCHAR(500) NULL,
  version INT NOT NULL DEFAULT 1,
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_roi_media_recipient (tenant_id,grantee_user_id,revoked_at,expires_at),
  KEY idx_roi_media_grantor (tenant_id,grantor_user_id,author_id),
  CONSTRAINT chk_roi_media_one_hop CHECK(grantor_user_id<>grantee_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS roi_media_delegate_subordinate (
  tenant_id BIGINT NOT NULL,
  manager_user_id BIGINT NOT NULL,
  subordinate_user_id BIGINT NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 0,
  evidence VARCHAR(500) NOT NULL,
  updated_by BIGINT NOT NULL,
  version INT NOT NULL DEFAULT 1,
  updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(tenant_id,manager_user_id,subordinate_user_id),
  CONSTRAINT chk_roi_media_subordinate_enabled CHECK(enabled IN (0,1)),
  CONSTRAINT chk_roi_media_subordinate_not_self CHECK(manager_user_id<>subordinate_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- Section 12: Flyway V205 持有人 + M2 发布账号
-- SSOT: V205__m4_holder_m2_publish_account.sql
-- =============================================================================

-- FR-M4 平台账号持有人（Football 用户 SSOT · ADR-056）
-- FR-M2-010 任务登记发布账号
-- 修订 2026-11-04：列/索引已存在则跳过，避免重复 ADD COLUMN（Duplicate column name 'holder_user_id'）。
-- 不 DROP 列。后半段 publish_account_id 与索引在缺失时仍会执行。

-- ---------------------------------------------------------------------------
-- oa_account.holder_user_id
-- ---------------------------------------------------------------------------
SET @needs_v205_holder := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'oa_account'
      AND COLUMN_NAME = 'holder_user_id'
);

SET @sql_v205_holder := IF(@needs_v205_holder = 0,
    'ALTER TABLE oa_account ADD COLUMN holder_user_id BIGINT NULL COMMENT ''持有人 system_users.id'' AFTER admin_user_id',
    'SELECT ''V205 skip: oa_account.holder_user_id exists'' AS notice'
);
PREPARE stmt_v205_holder FROM @sql_v205_holder;
EXECUTE stmt_v205_holder;
DEALLOCATE PREPARE stmt_v205_holder;

SET @needs_idx_holder := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'oa_account'
      AND INDEX_NAME = 'idx_oa_account_holder'
);

SET @sql_idx_holder := IF(@needs_idx_holder = 0,
    'CREATE INDEX idx_oa_account_holder ON oa_account (tenant_id, holder_user_id)',
    'SELECT ''V205 skip: idx_oa_account_holder exists'' AS notice'
);
PREPARE stmt_idx_holder FROM @sql_idx_holder;
EXECUTE stmt_idx_holder;
DEALLOCATE PREPARE stmt_idx_holder;

-- ---------------------------------------------------------------------------
-- oa_work_task_assignment.publish_account_id
-- ---------------------------------------------------------------------------
SET @needs_v205_publish := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'oa_work_task_assignment'
      AND COLUMN_NAME = 'publish_account_id'
);

SET @sql_v205_publish := IF(@needs_v205_publish = 0,
    'ALTER TABLE oa_work_task_assignment ADD COLUMN publish_account_id BIGINT NULL COMMENT ''发布平台账号 oa_account.id'' AFTER assignee_id',
    'SELECT ''V205 skip: oa_work_task_assignment.publish_account_id exists'' AS notice'
);
PREPARE stmt_v205_publish FROM @sql_v205_publish;
EXECUTE stmt_v205_publish;
DEALLOCATE PREPARE stmt_v205_publish;

SET @needs_idx_publish := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'oa_work_task_assignment'
      AND INDEX_NAME = 'idx_wt_assignment_publish_account'
);

SET @sql_idx_publish := IF(@needs_idx_publish = 0,
    'CREATE INDEX idx_wt_assignment_publish_account ON oa_work_task_assignment (tenant_id, publish_account_id)',
    'SELECT ''V205 skip: idx_wt_assignment_publish_account exists'' AS notice'
);
PREPARE stmt_idx_publish FROM @sql_idx_publish;
EXECUTE stmt_idx_publish;
DEALLOCATE PREPARE stmt_idx_publish;

-- =============================================================================
-- Section 13: Flyway V206 抖快双状态 + 运营人
-- SSOT: V206__oa_account_short_video_live_operator.sql
-- =============================================================================

-- V206: M4 抖音/快手 Excel 导入 — 双状态 + 运营人（ADR-078；与 V205 holder_user_id「持有人」并存）
-- password_encrypted 已存在（V86 公众号）；本迁移仅增抖音/快手展示与导入字段。
-- Excel「持有人」→ holder_user_id（V205）；Excel「运营人」→ operator_user_id（API operatorUserId）；实名人 realname_id 并行。
-- 修订 2026-11-04：列/索引已存在则跳过。本地库这些对象已在，但 flyway_schema_history 无 V206 成功记录；
-- 直接 ADD COLUMN 会在 V205 通过后再次失败。不 DROP 列。

SET @needs_v206_sv := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'oa_account'
      AND COLUMN_NAME = 'short_video_status'
);

SET @sql_v206_sv := IF(@needs_v206_sv = 0,
    'ALTER TABLE oa_account ADD COLUMN short_video_status VARCHAR(128) NULL COMMENT ''短视频状态（Excel 原文，抖音/快手）'' AFTER password_encrypted',
    'SELECT ''V206 skip: short_video_status exists'' AS notice'
);
PREPARE stmt_v206_sv FROM @sql_v206_sv;
EXECUTE stmt_v206_sv;
DEALLOCATE PREPARE stmt_v206_sv;

SET @needs_v206_live := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'oa_account'
      AND COLUMN_NAME = 'live_status'
);

SET @sql_v206_live := IF(@needs_v206_live = 0,
    'ALTER TABLE oa_account ADD COLUMN live_status VARCHAR(128) NULL COMMENT ''直播状态（Excel 原文，抖音/快手）'' AFTER short_video_status',
    'SELECT ''V206 skip: live_status exists'' AS notice'
);
PREPARE stmt_v206_live FROM @sql_v206_live;
EXECUTE stmt_v206_live;
DEALLOCATE PREPARE stmt_v206_live;

SET @needs_v206_op := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'oa_account'
      AND COLUMN_NAME = 'operator_user_id'
);

SET @sql_v206_op := IF(@needs_v206_op = 0,
    'ALTER TABLE oa_account ADD COLUMN operator_user_id BIGINT NULL COMMENT ''运营人 Football system_users.id（Excel 运营人）'' AFTER ip_group_id',
    'SELECT ''V206 skip: operator_user_id exists'' AS notice'
);
PREPARE stmt_v206_op FROM @sql_v206_op;
EXECUTE stmt_v206_op;
DEALLOCATE PREPARE stmt_v206_op;

SET @needs_idx_operator := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'oa_account'
      AND INDEX_NAME = 'idx_oa_account_operator_user'
);

SET @sql_idx_operator := IF(@needs_idx_operator = 0,
    'CREATE INDEX idx_oa_account_operator_user ON oa_account (tenant_id, operator_user_id)',
    'SELECT ''V206 skip: idx_oa_account_operator_user exists'' AS notice'
);
PREPARE stmt_idx_operator FROM @sql_idx_operator;
EXECUTE stmt_idx_operator;
DEALLOCATE PREPARE stmt_idx_operator;

-- =============================================================================
-- Section 14: oa_account 作者 + 封禁 JSON
-- SSOT: 20261102_oa_account_author_ban.sql
-- =============================================================================

-- 版本 20261102：平台账号增加所属作者与三类封禁区间。
-- 作者是 member author_user.id（主播），不是持有人/运营人员工。
-- 封禁 JSON 与投产预检的 accountBan/videoBan/liveBan 同结构。可重复执行。
USE `shenyu-ops`;

SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `oa_account` ADD COLUMN `author_user_id` BIGINT NULL COMMENT ''所属作者/主播 author_user.id'' AFTER `operator_user_id`',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'shenyu-ops' AND TABLE_NAME = 'oa_account' AND COLUMN_NAME = 'author_user_id');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `oa_account` ADD COLUMN `account_ban_json` JSON NULL COMMENT ''账号封禁区间'' AFTER `live_status`',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'shenyu-ops' AND TABLE_NAME = 'oa_account' AND COLUMN_NAME = 'account_ban_json');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `oa_account` ADD COLUMN `video_ban_json` JSON NULL COMMENT ''短视频封禁区间'' AFTER `account_ban_json`',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'shenyu-ops' AND TABLE_NAME = 'oa_account' AND COLUMN_NAME = 'video_ban_json');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `oa_account` ADD COLUMN `live_ban_json` JSON NULL COMMENT ''直播封禁区间'' AFTER `video_ban_json`',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'shenyu-ops' AND TABLE_NAME = 'oa_account' AND COLUMN_NAME = 'live_ban_json');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `oa_account` ADD INDEX `idx_oa_account_tenant_author` (`tenant_id`, `author_user_id`)',
    'SELECT 1')
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = 'shenyu-ops' AND TABLE_NAME = 'oa_account' AND INDEX_NAME = 'idx_oa_account_tenant_author');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- =============================================================================
-- [OPTIONAL] Section 15: tenant=1 钉钉审批 field_map + ops_oa_template
-- SSOT: 20261025_roi_approval_field_map.sql
-- 跳过：非 tenant=1 或未核对钉钉字段时可注释/不执行本段（见 README.md）
-- =============================================================================

-- tenant_id=1 钉钉审批字段 ID 与 ops_oa_template 登记
-- 警告：含本公司 process_code / corp_id；其他租户勿直接执行，需按环境改 WHERE 条件
-- 幂等性：UPDATE 幂等；INSERT … NOT EXISTS
-- 回滚：自备份 roi_process.field_map 与 ops_oa_template 恢复

-- Apply after 20261025_oa_cost_links.sql, shenyu-ops on the same DingTalk organization/tenant 1.
-- Idempotent mapping of already-created/read-back-verified DingTalk field IDs; no instances or roles created.
-- Preserve all existing field maps, workflow IDs and credentials. Back up the four roi_process rows privately first.
-- Verification: each enabled process must have fieldMap.author_selection and project_name; expense also has actual start/end.
-- Rollback: restore those four field_map values from the pre-migration private backup; do not delete historical approvals.
START TRANSACTION;
UPDATE roi_process SET field_map=JSON_SET(field_map,'$.fieldMap.author_selection',JSON_EXTRACT('{"id":"DDSelectField_8AB0B6DDC4B741","label":"关联主播","componentName":"DDSelectField","required":true}','$'),'$.fieldMap.project_name',JSON_EXTRACT('{"id":"TextField_9139F18004474E","label":"关联项目","componentName":"TextField","required":false}','$'),'$.fieldMap.session_started_at',JSON_EXTRACT('{"id":"DDDateField_0C6BD05D506C46","label":"直播开始时间","componentName":"DDDateField","required":false}','$'),'$.fieldMap.session_ended_at',JSON_EXTRACT('{"id":"DDDateField_373B994C576843","label":"直播结束时间","componentName":"DDDateField","required":false}','$')) WHERE tenant_id=1 AND type='expense' AND process_code='PROC-302BD6BE-8691-4F29-BBB0-666435B26222' AND enabled=1 AND JSON_VALID(field_map) AND JSON_TYPE(JSON_EXTRACT(field_map,'$.fieldMap'))='OBJECT';
UPDATE roi_process SET field_map=JSON_SET(field_map,'$.fieldMap.author_selection',JSON_EXTRACT('{"id":"DDSelectField_B6E28D7E3DB94D","label":"关联主播","componentName":"DDSelectField","required":false}','$'),'$.fieldMap.project_name',JSON_EXTRACT('{"id":"TextField_DE582059256B42","label":"关联项目","componentName":"TextField","required":false}','$'),'$.fieldMap.category',JSON_EXTRACT('{"id":"DDSelectField_F9119D4ADEE142","label":"费用类别","componentName":"DDSelectField","required":true}','$')) WHERE tenant_id=1 AND type='account' AND process_code='PROC-E149E063-9954-430D-9084-1C507DC71E92' AND enabled=1 AND JSON_VALID(field_map) AND JSON_TYPE(JSON_EXTRACT(field_map,'$.fieldMap'))='OBJECT';
UPDATE roi_process SET field_map=JSON_SET(field_map,'$.fieldMap."allocations.author_selection"',JSON_EXTRACT('{"id":"DDSelectField_291FAF77D53D47","label":"关联主播","componentName":"DDSelectField","required":false}','$'),'$.fieldMap."allocations.category"',JSON_EXTRACT('{"id":"DDSelectField_1AC0EFBB41F140","label":"费用类别","componentName":"DDSelectField","required":false}','$'),'$.fieldMap.author_selection',JSON_EXTRACT('{"id":"DDSelectField_B06E792E07A445","label":"关联主播","componentName":"DDSelectField","required":false}','$'),'$.fieldMap.project_name',JSON_EXTRACT('{"id":"TextField_D6C81DF9508949","label":"关联项目","componentName":"TextField","required":false}','$')) WHERE tenant_id=1 AND type='clearing' AND process_code='PROC-9B7D03D6-55DF-439B-89F4-8035D2FB28D7' AND enabled=1 AND JSON_VALID(field_map) AND JSON_TYPE(JSON_EXTRACT(field_map,'$.fieldMap'))='OBJECT';
UPDATE roi_process SET field_map=JSON_SET(field_map,'$.fieldMap."allocations.author_selection"',JSON_EXTRACT('{"id":"DDSelectField_D15C73AC60F540","label":"关联主播","componentName":"DDSelectField","required":false}','$'),'$.fieldMap."allocations.category"',JSON_EXTRACT('{"id":"DDSelectField_6D6C76B8BEFF49","label":"费用类别","componentName":"DDSelectField","required":false}','$'),'$.fieldMap.author_selection',JSON_EXTRACT('{"id":"DDSelectField_478C5E8518F647","label":"关联主播","componentName":"DDSelectField","required":false}','$'),'$.fieldMap.project_name',JSON_EXTRACT('{"id":"TextField_7D372315ACC64C","label":"关联项目","componentName":"TextField","required":false}','$')) WHERE tenant_id=1 AND type='payment' AND process_code='PROC-58E6A8DC-F2E0-47AC-8B4A-4460827BBAB6' AND enabled=1 AND JSON_VALID(field_map) AND JSON_TYPE(JSON_EXTRACT(field_map,'$.fieldMap'))='OBJECT';
-- Register the four existing dedicated forms after production App Token schema reads and DWS admin readback.
-- Existing unrelated/general finance templates remain untouched.
INSERT INTO ops_oa_template(tenant_id,process_code,domain,template_name,app_client_id,corp_id,amount_field_id,source,enabled,verified_by) SELECT tenant_id,process_code,'finance',name,'dingkhrw3izg4tfmdeeb','ding7da77227df029514bc961a6cb783455b','MoneyField_219WPJNDMB5YN','dws_admin_template_verified',1,2077584620716617728 FROM roi_process p WHERE tenant_id=1 AND type='expense' AND process_code='PROC-302BD6BE-8691-4F29-BBB0-666435B26222' AND enabled=1 AND NOT EXISTS(SELECT 1 FROM ops_oa_template o WHERE o.tenant_id=p.tenant_id AND o.process_code=p.process_code);
INSERT INTO ops_oa_template(tenant_id,process_code,domain,template_name,app_client_id,corp_id,amount_field_id,source,enabled,verified_by) SELECT tenant_id,process_code,'finance',name,'dingkhrw3izg4tfmdeeb','ding7da77227df029514bc961a6cb783455b','MoneyField_WFO9QC8NMXPF','dws_admin_template_verified',1,2077584620716617728 FROM roi_process p WHERE tenant_id=1 AND type='account' AND process_code='PROC-E149E063-9954-430D-9084-1C507DC71E92' AND enabled=1 AND NOT EXISTS(SELECT 1 FROM ops_oa_template o WHERE o.tenant_id=p.tenant_id AND o.process_code=p.process_code);
INSERT INTO ops_oa_template(tenant_id,process_code,domain,template_name,app_client_id,corp_id,amount_field_id,source,enabled,verified_by) SELECT tenant_id,process_code,'finance',name,'dingkhrw3izg4tfmdeeb','ding7da77227df029514bc961a6cb783455b','MoneyField_1KUT5T3E9R75I','dws_admin_template_verified',1,2077584620716617728 FROM roi_process p WHERE tenant_id=1 AND type='clearing' AND process_code='PROC-9B7D03D6-55DF-439B-89F4-8035D2FB28D7' AND enabled=1 AND NOT EXISTS(SELECT 1 FROM ops_oa_template o WHERE o.tenant_id=p.tenant_id AND o.process_code=p.process_code);
INSERT INTO ops_oa_template(tenant_id,process_code,domain,template_name,app_client_id,corp_id,amount_field_id,source,enabled,verified_by) SELECT tenant_id,process_code,'finance',name,'dingkhrw3izg4tfmdeeb','ding7da77227df029514bc961a6cb783455b','MoneyField_1NT1EKM854G1J','dws_admin_template_verified',1,2077584620716617728 FROM roi_process p WHERE tenant_id=1 AND type='payment' AND process_code='PROC-58E6A8DC-F2E0-47AC-8B4A-4460827BBAB6' AND enabled=1 AND NOT EXISTS(SELECT 1 FROM ops_oa_template o WHERE o.tenant_id=p.tenant_id AND o.process_code=p.process_code);
COMMIT;
SELECT type,process_code,JSON_EXTRACT(field_map,'$.fieldMap.author_selection.id') author_field FROM roi_process WHERE tenant_id=1 AND type IN ('account','expense','clearing','payment') AND enabled=1;
