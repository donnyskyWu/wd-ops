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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `roi_attachment_grant` (
  `token_hash` char(64) NOT NULL,
  `tenant_id` bigint NOT NULL,
  `attachment_id` char(36) NOT NULL,
  `expires_at` datetime NOT NULL,
  PRIMARY KEY (`token_hash`),
  KEY `idx_roi_grant_expiry` (`expires_at`),
  KEY `idx_roi_grant_file` (`tenant_id`,`attachment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

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
) ENGINE=InnoDB AUTO_INCREMENT=735 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `roi_author_scope` (
  `tenant_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `author_id` bigint NOT NULL,
  PRIMARY KEY (`tenant_id`,`user_id`,`author_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `roi_process` (
  `tenant_id` bigint NOT NULL,
  `type` varchar(32) NOT NULL,
  `name` varchar(128) NOT NULL,
  `process_code` varchar(128) NOT NULL,
  `field_map` longtext NOT NULL,
  `enabled` tinyint NOT NULL DEFAULT '1',
  PRIMARY KEY (`tenant_id`,`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `roi_role` (
  `tenant_id` bigint NOT NULL,
  `subject_id` varchar(64) NOT NULL,
  `role` varchar(24) NOT NULL,
  PRIMARY KEY (`tenant_id`,`subject_id`,`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `roi_role_override` (
  `tenant_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`tenant_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `roi_sequence` (
  `tenant_id` bigint NOT NULL,
  `seq_key` varchar(64) NOT NULL,
  `next_value` bigint NOT NULL,
  PRIMARY KEY (`tenant_id`,`seq_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `roi_session_attribution_article` (
  `tenant_id` bigint NOT NULL,
  `session_id` varchar(36) NOT NULL,
  `source_type` varchar(24) NOT NULL,
  `source_id` varchar(64) NOT NULL,
  `author_id` varchar(64) NOT NULL,
  PRIMARY KEY (`tenant_id`,`source_type`,`source_id`),
  KEY `ix_roi_attribution_session` (`tenant_id`,`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `roi_session_attribution_binding` (
  `tenant_id` bigint NOT NULL,
  `session_id` varchar(36) NOT NULL,
  `author_id` varchar(64) NOT NULL,
  `binding_payload` mediumtext NOT NULL,
  `created_by` varchar(64) NOT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`tenant_id`,`session_id`),
  KEY `ix_roi_attribution_author` (`tenant_id`,`author_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

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
-- Section 04: ROI 操作审计 roi_operation_log + 触发器
-- SSOT: 20261026_roi_operation_logs.sql
-- =============================================================================

-- 抖音投产管理：不可通过业务接口改删的操作日志。目标库 shenyu-ops，MySQL >= 8.0.29。
-- 先执行此前 ROI / OA / ops_roi_approval_cost_20261026.sql，再执行本脚本，再发布本轮后端。
-- 本脚本不修改既有业务值；IF NOT EXISTS 可重复执行且不清日志。既有定义变更请另发版本迁移。
-- 快照只投影白名单业务字段；原始审批表单、密钥、银行卡、身份证和请求正文不会进入新日志。
-- occurred_at 明确存UTC；旧roi_audit.created_at按旧系统北京时间转为UTC，API统一+08:00返回。
USE `shenyu-ops`;
CREATE TABLE IF NOT EXISTS roi_audit_migration_state (
 migration_key VARCHAR(64) NOT NULL PRIMARY KEY,
 legacy_max_id BIGINT NOT NULL,
 installed_at DATETIME(3) NOT NULL DEFAULT (UTC_TIMESTAMP(3))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ROI审计迁移覆盖边界，不含业务原文';
-- 首次记录全局旧审计ID边界；重跑不更新，之后产生的兼容roi_audit不会重复伪装为历史。
INSERT IGNORE INTO roi_audit_migration_state(migration_key,legacy_max_id,installed_at)
SELECT 'operation_log_v1',COALESCE(MAX(id),0),UTC_TIMESTAMP(3) FROM roi_audit;
CREATE TABLE IF NOT EXISTS roi_operation_log (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, tenant_id BIGINT NOT NULL,
 occurred_at DATETIME(3) NOT NULL DEFAULT (UTC_TIMESTAMP(3)),
 actor_id VARCHAR(64) NOT NULL DEFAULT '0', actor_name VARCHAR(160) NOT NULL,
 actor_roles LONGTEXT NULL, source VARCHAR(32) NOT NULL, request_id VARCHAR(64) NULL, request_path VARCHAR(255) NULL,
 module VARCHAR(32) NOT NULL, action VARCHAR(64) NOT NULL, record_type VARCHAR(64) NOT NULL,
 record_id VARCHAR(180) NOT NULL, related_record_id VARCHAR(180) NULL, record_label VARCHAR(255) NOT NULL,
 author_id VARCHAR(64) NULL, previous_author_id VARCHAR(64) NULL,
 before_json JSON NULL, after_json JSON NULL, changed_fields JSON NULL,
 result VARCHAR(32) NOT NULL DEFAULT 'SUCCESS', legacy TINYINT NOT NULL DEFAULT 0,
 legacy_audit_id BIGINT NULL,
 UNIQUE KEY uq_roi_operation_legacy(tenant_id,legacy_audit_id),
 KEY ix_roi_operation_time(tenant_id,occurred_at,id),
 KEY ix_roi_operation_record(tenant_id,record_type,record_id,id),
 KEY ix_roi_operation_related(tenant_id,related_record_id,id),
 KEY ix_roi_operation_actor(tenant_id,actor_id,occurred_at),
 KEY ix_roi_operation_author(tenant_id,author_id,occurred_at),
 KEY ix_roi_operation_module(tenant_id,module,occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='ROI业务变更审计：与业务DML同事务，UTC';
DELIMITER $$
CREATE FUNCTION IF NOT EXISTS roi_audit_text_v1(p LONGTEXT) RETURNS LONGTEXT DETERMINISTIC NO SQL
BEGIN
 IF p IS NULL THEN RETURN NULL; END IF;
 IF JSON_VALID(p) THEN IF JSON_TYPE(JSON_EXTRACT(p,'$')) IN ('OBJECT','ARRAY') THEN RETURN '[结构化原文未存入审计]'; END IF; END IF;
 RETURN LEFT(REGEXP_REPLACE(REGEXP_REPLACE(p,'(password|passwd|secret|token|cookie|authorization|密钥|密码)[[:space:]]*[:=：][[:space:]]*[^[:space:],;，；]+','[凭据已脱敏]',1,0,'i'),'[0-9]{11,20}','[号码已脱敏]'),2000);
END$$
CREATE FUNCTION IF NOT EXISTS roi_audit_leaf_v1(p LONGTEXT) RETURNS LONGTEXT DETERMINISTIC NO SQL
BEGIN
 DECLARE result LONGTEXT DEFAULT '{}'; DECLARE allowed LONGTEXT; DECLARE k VARCHAR(100); DECLARE v LONGTEXT; DECLARE i INT DEFAULT 0;
 IF p IS NULL OR NOT JSON_VALID(p) OR JSON_TYPE(JSON_EXTRACT(p,'$'))<>'OBJECT' THEN RETURN '{}'; END IF;
 SET allowed=JSON_ARRAY('id','kind','businessKey','authorId','name','platform','platformId','masterAccountId','status','price','purchaseDate','purchaseNo','mode','channelId','accountId','sessionId','sessionNo','displayName','date','startTime','endTime','expenseNo','category','amount','occurredDate','recognizedAmount','paidAmount','refundedAmount','reviewerId','reviewerName','reviewedAt','quantity','fishCount','publicIp','prefix','policy','metricSource','fishCountSource','approvalRecordId','approvalInstanceId','approvalStatus','approvalResult','type','accountVersion','effectiveAt','companyId','blueV','validFrom','validTo','originalValidTo','accountNo','costType','period','costDate','costTypeName','project','state','matchState','suggestedAccountId','suggestedAuthorId','authorSuggestionSource','sourceMissing','stableRow','confirmedBy','confirmedAt','confirmedAmount','excludedBy','excludedAt','reversedBy','reversedAt','newCostCount','payDate','payMethod','registeredBy','registeredAt','approvalBusinessId','originatorName','rowKey','amountState','existingCostId','costId','linkType','totalAmount','refundAmount','expenseId','action','verified','sourceKind','sourceType','sourceId','source','selected','sessionStart','sessionEnd','version','hasArticleWindow','peakOnline','realOnline','avgOnline','avgStaySeconds','enterUsers','exposureUsers','pinwheelExposure','pinwheelClick','pinwheelCtr','commentUsers','likes','newFans','shareCount','fanClubUsers','wecomAdded','platformFollows','serviceFollows','articleViews','reservations','beidanFans','income','giftUsers','giftRate','giftIncome','memberIncome','guardIncome','predictedIncome','permanent','start','end','endedAt','appliedAmount','actualAmount','remainingAmount','expectedAmount','settledAmount','settlementDate','settlementNo','lastStatus','active','fromAuthorId','toAuthorId','attachmentId','authorName','originatorBackendId','originatorDingtalkId','total','nativeState','providerStatus','providerResult','sourceChangeState','latestHash','observedAt','latestApprovalStatus','latestApprovalResult','originalName','urlState','approvedCount','rejectedCount','selectedAuthorId','selectedProject','selectedCategory','liveBanStatus','allocationIndex','matchedRowCount','exposure','entrants','shares','fanClub','pinwheelImpressions','pinwheelClicks','wecomAdds','subscriptionFollows','summaryViewers','readCount','readUsers','paidUsers','repeatUsers','repeatAmount','payValue','repeatValue','batchNo','paymentId','actualRecordId','refundRecordId','actualOccurredDate','verifiedBy','verifiedAt','scope','enabled','linkId','corpId','proofRecordId','clearingId','clearingNo','settledBy','settledAt','submittedBy','submittedAt','preparedBy','preparedAt','requestId','requestHash','comparisonStatus');
 WHILE i<JSON_LENGTH(allowed) DO
  SET k=JSON_UNQUOTE(JSON_EXTRACT(allowed,CONCAT('$[',i,']')));
  SET v=JSON_EXTRACT(p,CONCAT('$."',k,'"'));
  IF v IS NOT NULL AND JSON_TYPE(v) IN ('STRING','INTEGER','DOUBLE','DECIMAL','BOOLEAN','NULL') THEN
   IF JSON_TYPE(v)='STRING' THEN SET result=JSON_SET(result,CONCAT('$."',k,'"'),roi_audit_text_v1(JSON_UNQUOTE(v)));
   ELSE SET result=JSON_SET(result,CONCAT('$."',k,'"'),JSON_EXTRACT(v,'$')); END IF;
  END IF;
  SET i=i+1;
 END WHILE;
 RETURN result;
END$$
CREATE FUNCTION IF NOT EXISTS roi_audit_project_v1(p LONGTEXT) RETURNS LONGTEXT DETERMINISTIC NO SQL
BEGIN
 DECLARE result LONGTEXT DEFAULT '{}'; DECLARE containers LONGTEXT; DECLARE k VARCHAR(100); DECLARE v LONGTEXT; DECLARE arr LONGTEXT; DECLARE i INT DEFAULT 0; DECLARE j INT;
 IF p IS NULL OR NOT JSON_VALID(p) THEN RETURN '{}'; END IF;
 SET result=roi_audit_leaf_v1(p);
 SET containers=JSON_ARRAY('metrics','accountBan','videoBan','liveBan','payment','reversal','sourceChange','actualSettlement','articleWindowAdoption','ocrEvidence','selected');
 WHILE i<JSON_LENGTH(containers) DO
  SET k=JSON_UNQUOTE(JSON_EXTRACT(containers,CONCAT('$[',i,']')));SET v=JSON_EXTRACT(p,CONCAT('$."',k,'"'));
  IF v IS NOT NULL THEN SET result=JSON_SET(result,CONCAT('$."',k,'"'),JSON_EXTRACT(roi_audit_leaf_v1(v),'$')); END IF;
  SET i=i+1;
 END WHILE;
 SET containers=JSON_ARRAY('allocations','items','expenses','attachmentIds','paymentAttachmentIds','expenseIds','authorIds','roles','sourceIds','articleIds'); SET i=0;
 WHILE i<JSON_LENGTH(containers) DO
  SET k=JSON_UNQUOTE(JSON_EXTRACT(containers,CONCAT('$[',i,']')));SET v=JSON_EXTRACT(p,CONCAT('$."',k,'"')); SET arr='[]'; SET j=0;
  IF v IS NOT NULL AND JSON_TYPE(v)='ARRAY' THEN
   WHILE j<LEAST(JSON_LENGTH(v),100) DO
    IF JSON_TYPE(JSON_EXTRACT(v,CONCAT('$[',j,']')))='OBJECT' THEN SET arr=JSON_ARRAY_APPEND(arr,'$',JSON_EXTRACT(roi_audit_leaf_v1(JSON_EXTRACT(v,CONCAT('$[',j,']'))),'$'));
    ELSEIF JSON_TYPE(JSON_EXTRACT(v,CONCAT('$[',j,']'))) IN ('STRING','INTEGER','DOUBLE','BOOLEAN') THEN SET arr=JSON_ARRAY_APPEND(arr,'$',roi_audit_text_v1(JSON_UNQUOTE(JSON_EXTRACT(v,CONCAT('$[',j,']'))))); END IF;
    SET j=j+1;
   END WHILE;
   SET result=JSON_SET(result,CONCAT('$."',k,'"'),JSON_EXTRACT(arr,'$'));
  END IF;
  SET i=i+1;
 END WHILE;
 RETURN result;
END$$
CREATE TRIGGER IF NOT EXISTS roi_operation_log_no_update_v1 BEFORE UPDATE ON roi_operation_log FOR EACH ROW
BEGIN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='ROI operation logs are append-only'; END$$
CREATE TRIGGER IF NOT EXISTS roi_operation_log_no_delete_v1 BEFORE DELETE ON roi_operation_log FOR EACH ROW
BEGIN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='ROI operation logs cannot be deleted'; END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_r_record_i_v1` AFTER INSERT ON `roi_record` FOR EACH ROW
BEGIN
 IF 1=1 THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (NEW.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_path,NULL),CASE NEW.kind WHEN 'account' THEN 'account' WHEN 'account_verification' THEN 'account' WHEN 'expense_actual' THEN 'finance' WHEN 'refund' THEN 'finance' WHEN 'clearing' THEN 'finance' WHEN 'payment_proof' THEN 'finance' WHEN 'ocr_draft' THEN 'session' WHEN 'expense' THEN 'finance' WHEN 'payment' THEN 'finance' WHEN 'channel' THEN 'finance' WHEN 'session' THEN 'session' WHEN 'blacklist' THEN 'ip' WHEN 'approval' THEN 'approval' ELSE 'wecom' END,IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'create'),'roi_record',CAST(NEW.`id` AS CHAR),NULL,LEFT(COALESCE(NEW.business_key,''),255),CAST(NEW.author_id AS CHAR),NULL,NULL,JSON_OBJECT('id',CAST(NEW.`id` AS CHAR),'kind',CAST(NEW.`kind` AS CHAR),'business_key',CAST(NEW.`business_key` AS CHAR),'author_id',CAST(NEW.`author_id` AS CHAR),'version',NEW.`version`,'data',JSON_EXTRACT(roi_audit_project_v1(NEW.`payload`),'$')),JSON_ARRAY('id','tenant_id','kind','business_key','author_id','version','payload'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_r_record_u_v1` AFTER UPDATE ON `roi_record` FOR EACH ROW
BEGIN
 IF (1=1 OR 1=1) AND (NOT(OLD.`id` <=> NEW.`id`) OR NOT(OLD.`tenant_id` <=> NEW.`tenant_id`) OR NOT(OLD.`kind` <=> NEW.`kind`) OR NOT(OLD.`business_key` <=> NEW.`business_key`) OR NOT(OLD.`author_id` <=> NEW.`author_id`) OR NOT(OLD.`version` <=> NEW.`version`) OR NOT(OLD.`payload` <=> NEW.`payload`)) THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (NEW.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_path,NULL),CASE NEW.kind WHEN 'account' THEN 'account' WHEN 'account_verification' THEN 'account' WHEN 'expense_actual' THEN 'finance' WHEN 'refund' THEN 'finance' WHEN 'clearing' THEN 'finance' WHEN 'payment_proof' THEN 'finance' WHEN 'ocr_draft' THEN 'session' WHEN 'expense' THEN 'finance' WHEN 'payment' THEN 'finance' WHEN 'channel' THEN 'finance' WHEN 'session' THEN 'session' WHEN 'blacklist' THEN 'ip' WHEN 'approval' THEN 'approval' ELSE 'wecom' END,IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'update'),'roi_record',CAST(NEW.`id` AS CHAR),NULL,LEFT(COALESCE(NEW.business_key,''),255),CAST(NEW.author_id AS CHAR),CAST(OLD.author_id AS CHAR),JSON_OBJECT('id',CAST(OLD.`id` AS CHAR),'kind',CAST(OLD.`kind` AS CHAR),'business_key',CAST(OLD.`business_key` AS CHAR),'author_id',CAST(OLD.`author_id` AS CHAR),'version',OLD.`version`,'data',JSON_EXTRACT(roi_audit_project_v1(OLD.`payload`),'$')),JSON_OBJECT('id',CAST(NEW.`id` AS CHAR),'kind',CAST(NEW.`kind` AS CHAR),'business_key',CAST(NEW.`business_key` AS CHAR),'author_id',CAST(NEW.`author_id` AS CHAR),'version',NEW.`version`,'data',JSON_EXTRACT(roi_audit_project_v1(NEW.`payload`),'$')),JSON_MERGE_PRESERVE(JSON_MERGE_PRESERVE(IF(NOT(OLD.`id` <=> NEW.`id`),JSON_ARRAY('id'),JSON_ARRAY()),IF(NOT(OLD.`tenant_id` <=> NEW.`tenant_id`),JSON_ARRAY('tenant_id'),JSON_ARRAY()),IF(NOT(OLD.`kind` <=> NEW.`kind`),JSON_ARRAY('kind'),JSON_ARRAY()),IF(NOT(OLD.`business_key` <=> NEW.`business_key`),JSON_ARRAY('business_key'),JSON_ARRAY()),IF(NOT(OLD.`author_id` <=> NEW.`author_id`),JSON_ARRAY('author_id'),JSON_ARRAY()),IF(NOT(OLD.`version` <=> NEW.`version`),JSON_ARRAY('version'),JSON_ARRAY()),IF(NOT(OLD.`payload` <=> NEW.`payload`),JSON_ARRAY('payload'),JSON_ARRAY())),IF(NOT(JSON_EXTRACT(OLD.payload,'$.text') <=> JSON_EXTRACT(NEW.payload,'$.text')),JSON_ARRAY('data.text'),JSON_ARRAY()),IF(NOT(JSON_EXTRACT(OLD.payload,'$.linkTitle') <=> JSON_EXTRACT(NEW.payload,'$.linkTitle')),JSON_ARRAY('data.linkTitle'),JSON_ARRAY()),IF(NOT(JSON_EXTRACT(OLD.payload,'$.linkUrl') <=> JSON_EXTRACT(NEW.payload,'$.linkUrl')),JSON_ARRAY('data.linkUrl'),JSON_ARRAY()),IF(NOT(JSON_EXTRACT(OLD.payload,'$.note') <=> JSON_EXTRACT(NEW.payload,'$.note')),JSON_ARRAY('data.note'),JSON_ARRAY()),IF(NOT(JSON_EXTRACT(OLD.payload,'$.notes') <=> JSON_EXTRACT(NEW.payload,'$.notes')),JSON_ARRAY('data.notes'),JSON_ARRAY()),IF(NOT(JSON_EXTRACT(OLD.payload,'$.remarks') <=> JSON_EXTRACT(NEW.payload,'$.remarks')),JSON_ARRAY('data.remarks'),JSON_ARRAY()),IF(NOT(JSON_EXTRACT(OLD.payload,'$.reason') <=> JSON_EXTRACT(NEW.payload,'$.reason')),JSON_ARRAY('data.reason'),JSON_ARRAY()),IF(NOT(JSON_EXTRACT(OLD.payload,'$.reviewNote') <=> JSON_EXTRACT(NEW.payload,'$.reviewNote')),JSON_ARRAY('data.reviewNote'),JSON_ARRAY()),IF(NOT(JSON_EXTRACT(OLD.payload,'$.description') <=> JSON_EXTRACT(NEW.payload,'$.description')),JSON_ARRAY('data.description'),JSON_ARRAY()),IF(NOT(JSON_EXTRACT(OLD.payload,'$.seller') <=> JSON_EXTRACT(NEW.payload,'$.seller')),JSON_ARRAY('data.seller'),JSON_ARRAY()),IF(NOT(JSON_EXTRACT(OLD.payload,'$.payeeName') <=> JSON_EXTRACT(NEW.payload,'$.payeeName')),JSON_ARRAY('data.payeeName'),JSON_ARRAY()),IF(NOT(JSON_EXTRACT(OLD.payload,'$.payeeInformation') <=> JSON_EXTRACT(NEW.payload,'$.payeeInformation')),JSON_ARRAY('data.payeeInformation'),JSON_ARRAY()),IF(NOT(JSON_EXTRACT(OLD.payload,'$.bankRef') <=> JSON_EXTRACT(NEW.payload,'$.bankRef')),JSON_ARRAY('data.bankRef'),JSON_ARRAY()),IF(NOT(JSON_EXTRACT(OLD.payload,'$.formSnapshot') <=> JSON_EXTRACT(NEW.payload,'$.formSnapshot')),JSON_ARRAY('data.formSnapshot'),JSON_ARRAY()),IF(NOT(JSON_EXTRACT(OLD.payload,'$.expectedValues') <=> JSON_EXTRACT(NEW.payload,'$.expectedValues')),JSON_ARRAY('data.expectedValues'),JSON_ARRAY()),IF(NOT(JSON_EXTRACT(OLD.payload,'$.detail') <=> JSON_EXTRACT(NEW.payload,'$.detail')),JSON_ARRAY('data.detail'),JSON_ARRAY()),IF(NOT(JSON_EXTRACT(OLD.payload,'$.paymentProofNote') <=> JSON_EXTRACT(NEW.payload,'$.paymentProofNote')),JSON_ARRAY('data.paymentProofNote'),JSON_ARRAY()),IF(NOT(JSON_EXTRACT(OLD.payload,'$.allocationNote') <=> JSON_EXTRACT(NEW.payload,'$.allocationNote')),JSON_ARRAY('data.allocationNote'),JSON_ARRAY()),IF(NOT(JSON_EXTRACT(OLD.payload,'$.duplicateReason') <=> JSON_EXTRACT(NEW.payload,'$.duplicateReason')),JSON_ARRAY('data.duplicateReason'),JSON_ARRAY())));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_r_record_d_v1` AFTER DELETE ON `roi_record` FOR EACH ROW
BEGIN
 IF 1=1 THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (OLD.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_path,NULL),CASE OLD.kind WHEN 'account' THEN 'account' WHEN 'account_verification' THEN 'account' WHEN 'expense_actual' THEN 'finance' WHEN 'refund' THEN 'finance' WHEN 'clearing' THEN 'finance' WHEN 'payment_proof' THEN 'finance' WHEN 'ocr_draft' THEN 'session' WHEN 'expense' THEN 'finance' WHEN 'payment' THEN 'finance' WHEN 'channel' THEN 'finance' WHEN 'session' THEN 'session' WHEN 'blacklist' THEN 'ip' WHEN 'approval' THEN 'approval' ELSE 'wecom' END,IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'delete'),'roi_record',CAST(OLD.`id` AS CHAR),NULL,LEFT(COALESCE(OLD.business_key,''),255),CAST(OLD.author_id AS CHAR),CAST(OLD.author_id AS CHAR),JSON_OBJECT('id',CAST(OLD.`id` AS CHAR),'kind',CAST(OLD.`kind` AS CHAR),'business_key',CAST(OLD.`business_key` AS CHAR),'author_id',CAST(OLD.`author_id` AS CHAR),'version',OLD.`version`,'data',JSON_EXTRACT(roi_audit_project_v1(OLD.`payload`),'$')),NULL,JSON_ARRAY('id','tenant_id','kind','business_key','author_id','version','payload'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_r_attachment_i_v1` AFTER INSERT ON `roi_attachment` FOR EACH ROW
BEGIN
 IF 1=1 THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (NEW.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_path,NULL),'attachment',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'create'),'roi_attachment',CAST(NEW.`id` AS CHAR),NEW.record_id,LEFT(COALESCE(roi_audit_text_v1(NEW.file_name),''),255),(SELECT CAST(r.author_id AS CHAR) FROM roi_record r WHERE r.tenant_id=NEW.tenant_id AND r.id=NEW.record_id LIMIT 1),NULL,NULL,JSON_OBJECT('id',CAST(NEW.`id` AS CHAR),'purpose',CAST(NEW.`purpose` AS CHAR),'record_id',CAST(NEW.`record_id` AS CHAR),'file_name',roi_audit_text_v1(CAST(NEW.`file_name` AS CHAR)),'content_type',CAST(NEW.`content_type` AS CHAR),'file_size',NEW.`file_size`,'storage_name',IF(NEW.`storage_name` IS NULL,NULL,'[已脱敏]')),JSON_ARRAY('id','tenant_id','purpose','record_id','file_name','content_type','file_size','storage_name'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_r_attachment_u_v1` AFTER UPDATE ON `roi_attachment` FOR EACH ROW
BEGIN
 IF (1=1 OR 1=1) AND (NOT(OLD.`id` <=> NEW.`id`) OR NOT(OLD.`tenant_id` <=> NEW.`tenant_id`) OR NOT(OLD.`purpose` <=> NEW.`purpose`) OR NOT(OLD.`record_id` <=> NEW.`record_id`) OR NOT(OLD.`file_name` <=> NEW.`file_name`) OR NOT(OLD.`content_type` <=> NEW.`content_type`) OR NOT(OLD.`file_size` <=> NEW.`file_size`) OR NOT(OLD.`storage_name` <=> NEW.`storage_name`)) THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (NEW.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_path,NULL),'attachment',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'update'),'roi_attachment',CAST(NEW.`id` AS CHAR),NEW.record_id,LEFT(COALESCE(roi_audit_text_v1(NEW.file_name),''),255),(SELECT CAST(r.author_id AS CHAR) FROM roi_record r WHERE r.tenant_id=NEW.tenant_id AND r.id=NEW.record_id LIMIT 1),(SELECT CAST(r.author_id AS CHAR) FROM roi_record r WHERE r.tenant_id=OLD.tenant_id AND r.id=OLD.record_id LIMIT 1),JSON_OBJECT('id',CAST(OLD.`id` AS CHAR),'purpose',CAST(OLD.`purpose` AS CHAR),'record_id',CAST(OLD.`record_id` AS CHAR),'file_name',roi_audit_text_v1(CAST(OLD.`file_name` AS CHAR)),'content_type',CAST(OLD.`content_type` AS CHAR),'file_size',OLD.`file_size`,'storage_name',IF(OLD.`storage_name` IS NULL,NULL,'[已脱敏]')),JSON_OBJECT('id',CAST(NEW.`id` AS CHAR),'purpose',CAST(NEW.`purpose` AS CHAR),'record_id',CAST(NEW.`record_id` AS CHAR),'file_name',roi_audit_text_v1(CAST(NEW.`file_name` AS CHAR)),'content_type',CAST(NEW.`content_type` AS CHAR),'file_size',NEW.`file_size`,'storage_name',IF(NEW.`storage_name` IS NULL,NULL,'[已脱敏]')),JSON_MERGE_PRESERVE(IF(NOT(OLD.`id` <=> NEW.`id`),JSON_ARRAY('id'),JSON_ARRAY()),IF(NOT(OLD.`tenant_id` <=> NEW.`tenant_id`),JSON_ARRAY('tenant_id'),JSON_ARRAY()),IF(NOT(OLD.`purpose` <=> NEW.`purpose`),JSON_ARRAY('purpose'),JSON_ARRAY()),IF(NOT(OLD.`record_id` <=> NEW.`record_id`),JSON_ARRAY('record_id'),JSON_ARRAY()),IF(NOT(OLD.`file_name` <=> NEW.`file_name`),JSON_ARRAY('file_name'),JSON_ARRAY()),IF(NOT(OLD.`content_type` <=> NEW.`content_type`),JSON_ARRAY('content_type'),JSON_ARRAY()),IF(NOT(OLD.`file_size` <=> NEW.`file_size`),JSON_ARRAY('file_size'),JSON_ARRAY()),IF(NOT(OLD.`storage_name` <=> NEW.`storage_name`),JSON_ARRAY('storage_name'),JSON_ARRAY())));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_r_attachment_d_v1` AFTER DELETE ON `roi_attachment` FOR EACH ROW
BEGIN
 IF 1=1 THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (OLD.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_path,NULL),'attachment',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'delete'),'roi_attachment',CAST(OLD.`id` AS CHAR),OLD.record_id,LEFT(COALESCE(roi_audit_text_v1(OLD.file_name),''),255),(SELECT CAST(r.author_id AS CHAR) FROM roi_record r WHERE r.tenant_id=OLD.tenant_id AND r.id=OLD.record_id LIMIT 1),(SELECT CAST(r.author_id AS CHAR) FROM roi_record r WHERE r.tenant_id=OLD.tenant_id AND r.id=OLD.record_id LIMIT 1),JSON_OBJECT('id',CAST(OLD.`id` AS CHAR),'purpose',CAST(OLD.`purpose` AS CHAR),'record_id',CAST(OLD.`record_id` AS CHAR),'file_name',roi_audit_text_v1(CAST(OLD.`file_name` AS CHAR)),'content_type',CAST(OLD.`content_type` AS CHAR),'file_size',OLD.`file_size`,'storage_name',IF(OLD.`storage_name` IS NULL,NULL,'[已脱敏]')),NULL,JSON_ARRAY('id','tenant_id','purpose','record_id','file_name','content_type','file_size','storage_name'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_r_attachment_grant_i_v1` AFTER INSERT ON `roi_attachment_grant` FOR EACH ROW
BEGIN
 IF 1=1 THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (NEW.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_path,NULL),'attachment',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'create'),'roi_attachment_grant',CONCAT('attachment:',NEW.attachment_id,':',UNIX_TIMESTAMP(NEW.expires_at)),(SELECT r.record_id FROM roi_attachment r WHERE r.tenant_id=NEW.tenant_id AND r.id=NEW.attachment_id LIMIT 1),LEFT(COALESCE(CONCAT('attachment:',NEW.attachment_id,':',UNIX_TIMESTAMP(NEW.expires_at)),''),255),(SELECT CAST(r.author_id AS CHAR) FROM roi_record r WHERE r.tenant_id=NEW.tenant_id AND r.id=(SELECT r.record_id FROM roi_attachment r WHERE r.tenant_id=NEW.tenant_id AND r.id=NEW.attachment_id LIMIT 1) LIMIT 1),NULL,NULL,JSON_OBJECT('token_hash',IF(NEW.`token_hash` IS NULL,NULL,'[已脱敏]'),'attachment_id',CAST(NEW.`attachment_id` AS CHAR),'expires_at',CAST(NEW.`expires_at` AS CHAR)),JSON_ARRAY('token_hash','tenant_id','attachment_id','expires_at'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_r_attachment_grant_u_v1` AFTER UPDATE ON `roi_attachment_grant` FOR EACH ROW
BEGIN
 IF (1=1 OR 1=1) AND (NOT(OLD.`token_hash` <=> NEW.`token_hash`) OR NOT(OLD.`tenant_id` <=> NEW.`tenant_id`) OR NOT(OLD.`attachment_id` <=> NEW.`attachment_id`) OR NOT(OLD.`expires_at` <=> NEW.`expires_at`)) THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (NEW.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_path,NULL),'attachment',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'update'),'roi_attachment_grant',CONCAT('attachment:',NEW.attachment_id,':',UNIX_TIMESTAMP(NEW.expires_at)),(SELECT r.record_id FROM roi_attachment r WHERE r.tenant_id=NEW.tenant_id AND r.id=NEW.attachment_id LIMIT 1),LEFT(COALESCE(CONCAT('attachment:',NEW.attachment_id,':',UNIX_TIMESTAMP(NEW.expires_at)),''),255),(SELECT CAST(r.author_id AS CHAR) FROM roi_record r WHERE r.tenant_id=NEW.tenant_id AND r.id=(SELECT r.record_id FROM roi_attachment r WHERE r.tenant_id=NEW.tenant_id AND r.id=NEW.attachment_id LIMIT 1) LIMIT 1),(SELECT CAST(r.author_id AS CHAR) FROM roi_record r WHERE r.tenant_id=OLD.tenant_id AND r.id=(SELECT r.record_id FROM roi_attachment r WHERE r.tenant_id=OLD.tenant_id AND r.id=OLD.attachment_id LIMIT 1) LIMIT 1),JSON_OBJECT('token_hash',IF(OLD.`token_hash` IS NULL,NULL,'[已脱敏]'),'attachment_id',CAST(OLD.`attachment_id` AS CHAR),'expires_at',CAST(OLD.`expires_at` AS CHAR)),JSON_OBJECT('token_hash',IF(NEW.`token_hash` IS NULL,NULL,'[已脱敏]'),'attachment_id',CAST(NEW.`attachment_id` AS CHAR),'expires_at',CAST(NEW.`expires_at` AS CHAR)),JSON_MERGE_PRESERVE(IF(NOT(OLD.`token_hash` <=> NEW.`token_hash`),JSON_ARRAY('token_hash'),JSON_ARRAY()),IF(NOT(OLD.`tenant_id` <=> NEW.`tenant_id`),JSON_ARRAY('tenant_id'),JSON_ARRAY()),IF(NOT(OLD.`attachment_id` <=> NEW.`attachment_id`),JSON_ARRAY('attachment_id'),JSON_ARRAY()),IF(NOT(OLD.`expires_at` <=> NEW.`expires_at`),JSON_ARRAY('expires_at'),JSON_ARRAY())));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_r_attachment_grant_d_v1` AFTER DELETE ON `roi_attachment_grant` FOR EACH ROW
BEGIN
 IF 1=1 THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (OLD.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_path,NULL),'attachment',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'delete'),'roi_attachment_grant',CONCAT('attachment:',OLD.attachment_id,':',UNIX_TIMESTAMP(OLD.expires_at)),(SELECT r.record_id FROM roi_attachment r WHERE r.tenant_id=OLD.tenant_id AND r.id=OLD.attachment_id LIMIT 1),LEFT(COALESCE(CONCAT('attachment:',OLD.attachment_id,':',UNIX_TIMESTAMP(OLD.expires_at)),''),255),(SELECT CAST(r.author_id AS CHAR) FROM roi_record r WHERE r.tenant_id=OLD.tenant_id AND r.id=(SELECT r.record_id FROM roi_attachment r WHERE r.tenant_id=OLD.tenant_id AND r.id=OLD.attachment_id LIMIT 1) LIMIT 1),(SELECT CAST(r.author_id AS CHAR) FROM roi_record r WHERE r.tenant_id=OLD.tenant_id AND r.id=(SELECT r.record_id FROM roi_attachment r WHERE r.tenant_id=OLD.tenant_id AND r.id=OLD.attachment_id LIMIT 1) LIMIT 1),JSON_OBJECT('token_hash',IF(OLD.`token_hash` IS NULL,NULL,'[已脱敏]'),'attachment_id',CAST(OLD.`attachment_id` AS CHAR),'expires_at',CAST(OLD.`expires_at` AS CHAR)),NULL,JSON_ARRAY('token_hash','tenant_id','attachment_id','expires_at'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_r_role_i_v1` AFTER INSERT ON `roi_role` FOR EACH ROW
BEGIN
 IF 1=1 THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (NEW.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_path,NULL),'permission',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'create'),'roi_role',CONCAT_WS(':',CAST(NEW.`subject_id` AS CHAR),CAST(NEW.`role` AS CHAR)),NULL,LEFT(COALESCE(CONCAT_WS(':',CAST(NEW.`subject_id` AS CHAR),CAST(NEW.`role` AS CHAR)),''),255),NULL,NULL,NULL,JSON_OBJECT('subject_id',CAST(NEW.`subject_id` AS CHAR),'role',CAST(NEW.`role` AS CHAR)),JSON_ARRAY('tenant_id','subject_id','role'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_r_role_u_v1` AFTER UPDATE ON `roi_role` FOR EACH ROW
BEGIN
 IF (1=1 OR 1=1) AND (NOT(OLD.`tenant_id` <=> NEW.`tenant_id`) OR NOT(OLD.`subject_id` <=> NEW.`subject_id`) OR NOT(OLD.`role` <=> NEW.`role`)) THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (NEW.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_path,NULL),'permission',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'update'),'roi_role',CONCAT_WS(':',CAST(NEW.`subject_id` AS CHAR),CAST(NEW.`role` AS CHAR)),NULL,LEFT(COALESCE(CONCAT_WS(':',CAST(NEW.`subject_id` AS CHAR),CAST(NEW.`role` AS CHAR)),''),255),NULL,NULL,JSON_OBJECT('subject_id',CAST(OLD.`subject_id` AS CHAR),'role',CAST(OLD.`role` AS CHAR)),JSON_OBJECT('subject_id',CAST(NEW.`subject_id` AS CHAR),'role',CAST(NEW.`role` AS CHAR)),JSON_MERGE_PRESERVE(IF(NOT(OLD.`tenant_id` <=> NEW.`tenant_id`),JSON_ARRAY('tenant_id'),JSON_ARRAY()),IF(NOT(OLD.`subject_id` <=> NEW.`subject_id`),JSON_ARRAY('subject_id'),JSON_ARRAY()),IF(NOT(OLD.`role` <=> NEW.`role`),JSON_ARRAY('role'),JSON_ARRAY())));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_r_role_d_v1` AFTER DELETE ON `roi_role` FOR EACH ROW
BEGIN
 IF 1=1 THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (OLD.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_path,NULL),'permission',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'delete'),'roi_role',CONCAT_WS(':',CAST(OLD.`subject_id` AS CHAR),CAST(OLD.`role` AS CHAR)),NULL,LEFT(COALESCE(CONCAT_WS(':',CAST(OLD.`subject_id` AS CHAR),CAST(OLD.`role` AS CHAR)),''),255),NULL,NULL,JSON_OBJECT('subject_id',CAST(OLD.`subject_id` AS CHAR),'role',CAST(OLD.`role` AS CHAR)),NULL,JSON_ARRAY('tenant_id','subject_id','role'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_r_role_override_i_v1` AFTER INSERT ON `roi_role_override` FOR EACH ROW
BEGIN
 IF 1=1 THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (NEW.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_path,NULL),'permission',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'create'),'roi_role_override',CONCAT_WS(':',CAST(NEW.`user_id` AS CHAR)),NULL,LEFT(COALESCE(CONCAT_WS(':',CAST(NEW.`user_id` AS CHAR)),''),255),NULL,NULL,NULL,JSON_OBJECT('user_id',CAST(NEW.`user_id` AS CHAR)),JSON_ARRAY('tenant_id','user_id'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_r_role_override_u_v1` AFTER UPDATE ON `roi_role_override` FOR EACH ROW
BEGIN
 IF (1=1 OR 1=1) AND (NOT(OLD.`tenant_id` <=> NEW.`tenant_id`) OR NOT(OLD.`user_id` <=> NEW.`user_id`)) THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (NEW.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_path,NULL),'permission',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'update'),'roi_role_override',CONCAT_WS(':',CAST(NEW.`user_id` AS CHAR)),NULL,LEFT(COALESCE(CONCAT_WS(':',CAST(NEW.`user_id` AS CHAR)),''),255),NULL,NULL,JSON_OBJECT('user_id',CAST(OLD.`user_id` AS CHAR)),JSON_OBJECT('user_id',CAST(NEW.`user_id` AS CHAR)),JSON_MERGE_PRESERVE(IF(NOT(OLD.`tenant_id` <=> NEW.`tenant_id`),JSON_ARRAY('tenant_id'),JSON_ARRAY()),IF(NOT(OLD.`user_id` <=> NEW.`user_id`),JSON_ARRAY('user_id'),JSON_ARRAY())));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_r_role_override_d_v1` AFTER DELETE ON `roi_role_override` FOR EACH ROW
BEGIN
 IF 1=1 THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (OLD.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_path,NULL),'permission',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'delete'),'roi_role_override',CONCAT_WS(':',CAST(OLD.`user_id` AS CHAR)),NULL,LEFT(COALESCE(CONCAT_WS(':',CAST(OLD.`user_id` AS CHAR)),''),255),NULL,NULL,JSON_OBJECT('user_id',CAST(OLD.`user_id` AS CHAR)),NULL,JSON_ARRAY('tenant_id','user_id'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_r_author_scope_i_v1` AFTER INSERT ON `roi_author_scope` FOR EACH ROW
BEGIN
 IF 1=1 THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (NEW.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_path,NULL),'permission',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'create'),'roi_author_scope',CONCAT_WS(':',CAST(NEW.`user_id` AS CHAR),CAST(NEW.`author_id` AS CHAR)),NULL,LEFT(COALESCE(CONCAT_WS(':',CAST(NEW.`user_id` AS CHAR),CAST(NEW.`author_id` AS CHAR)),''),255),CAST(NEW.author_id AS CHAR),NULL,NULL,JSON_OBJECT('user_id',CAST(NEW.`user_id` AS CHAR),'author_id',CAST(NEW.`author_id` AS CHAR)),JSON_ARRAY('tenant_id','user_id','author_id'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_r_author_scope_u_v1` AFTER UPDATE ON `roi_author_scope` FOR EACH ROW
BEGIN
 IF (1=1 OR 1=1) AND (NOT(OLD.`tenant_id` <=> NEW.`tenant_id`) OR NOT(OLD.`user_id` <=> NEW.`user_id`) OR NOT(OLD.`author_id` <=> NEW.`author_id`)) THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (NEW.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_path,NULL),'permission',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'update'),'roi_author_scope',CONCAT_WS(':',CAST(NEW.`user_id` AS CHAR),CAST(NEW.`author_id` AS CHAR)),NULL,LEFT(COALESCE(CONCAT_WS(':',CAST(NEW.`user_id` AS CHAR),CAST(NEW.`author_id` AS CHAR)),''),255),CAST(NEW.author_id AS CHAR),CAST(OLD.author_id AS CHAR),JSON_OBJECT('user_id',CAST(OLD.`user_id` AS CHAR),'author_id',CAST(OLD.`author_id` AS CHAR)),JSON_OBJECT('user_id',CAST(NEW.`user_id` AS CHAR),'author_id',CAST(NEW.`author_id` AS CHAR)),JSON_MERGE_PRESERVE(IF(NOT(OLD.`tenant_id` <=> NEW.`tenant_id`),JSON_ARRAY('tenant_id'),JSON_ARRAY()),IF(NOT(OLD.`user_id` <=> NEW.`user_id`),JSON_ARRAY('user_id'),JSON_ARRAY()),IF(NOT(OLD.`author_id` <=> NEW.`author_id`),JSON_ARRAY('author_id'),JSON_ARRAY())));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_r_author_scope_d_v1` AFTER DELETE ON `roi_author_scope` FOR EACH ROW
BEGIN
 IF 1=1 THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (OLD.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_path,NULL),'permission',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'delete'),'roi_author_scope',CONCAT_WS(':',CAST(OLD.`user_id` AS CHAR),CAST(OLD.`author_id` AS CHAR)),NULL,LEFT(COALESCE(CONCAT_WS(':',CAST(OLD.`user_id` AS CHAR),CAST(OLD.`author_id` AS CHAR)),''),255),CAST(OLD.author_id AS CHAR),CAST(OLD.author_id AS CHAR),JSON_OBJECT('user_id',CAST(OLD.`user_id` AS CHAR),'author_id',CAST(OLD.`author_id` AS CHAR)),NULL,JSON_ARRAY('tenant_id','user_id','author_id'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_r_account_assignment_i_v1` AFTER INSERT ON `roi_account_assignment` FOR EACH ROW
BEGIN
 IF 1=1 THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (NEW.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_path,NULL),'account',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'create'),'roi_account_assignment',CAST(NEW.`id` AS CHAR),CAST(NEW.account_id AS CHAR),LEFT(COALESCE(CAST(NEW.`id` AS CHAR),''),255),CAST(NEW.author_id AS CHAR),NULL,NULL,JSON_OBJECT('id',CAST(NEW.`id` AS CHAR),'account_id',CAST(NEW.`account_id` AS CHAR),'account_version',NEW.`account_version`,'author_id',CAST(NEW.`author_id` AS CHAR),'effective_at',CAST(NEW.`effective_at` AS CHAR),'source',CAST(NEW.`source` AS CHAR),'reason',IF(NEW.`reason` IS NULL,NULL,'[自由文本原文未存入审计]')),JSON_ARRAY('id','tenant_id','account_id','account_version','author_id','effective_at','source','reason'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_r_account_assignment_u_v1` AFTER UPDATE ON `roi_account_assignment` FOR EACH ROW
BEGIN
 IF (1=1 OR 1=1) AND (NOT(OLD.`id` <=> NEW.`id`) OR NOT(OLD.`tenant_id` <=> NEW.`tenant_id`) OR NOT(OLD.`account_id` <=> NEW.`account_id`) OR NOT(OLD.`account_version` <=> NEW.`account_version`) OR NOT(OLD.`author_id` <=> NEW.`author_id`) OR NOT(OLD.`effective_at` <=> NEW.`effective_at`) OR NOT(OLD.`source` <=> NEW.`source`) OR NOT(OLD.`reason` <=> NEW.`reason`)) THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (NEW.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_path,NULL),'account',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'update'),'roi_account_assignment',CAST(NEW.`id` AS CHAR),CAST(NEW.account_id AS CHAR),LEFT(COALESCE(CAST(NEW.`id` AS CHAR),''),255),CAST(NEW.author_id AS CHAR),CAST(OLD.author_id AS CHAR),JSON_OBJECT('id',CAST(OLD.`id` AS CHAR),'account_id',CAST(OLD.`account_id` AS CHAR),'account_version',OLD.`account_version`,'author_id',CAST(OLD.`author_id` AS CHAR),'effective_at',CAST(OLD.`effective_at` AS CHAR),'source',CAST(OLD.`source` AS CHAR),'reason',IF(OLD.`reason` IS NULL,NULL,'[自由文本原文未存入审计]')),JSON_OBJECT('id',CAST(NEW.`id` AS CHAR),'account_id',CAST(NEW.`account_id` AS CHAR),'account_version',NEW.`account_version`,'author_id',CAST(NEW.`author_id` AS CHAR),'effective_at',CAST(NEW.`effective_at` AS CHAR),'source',CAST(NEW.`source` AS CHAR),'reason',IF(NEW.`reason` IS NULL,NULL,'[自由文本原文未存入审计]')),JSON_MERGE_PRESERVE(IF(NOT(OLD.`id` <=> NEW.`id`),JSON_ARRAY('id'),JSON_ARRAY()),IF(NOT(OLD.`tenant_id` <=> NEW.`tenant_id`),JSON_ARRAY('tenant_id'),JSON_ARRAY()),IF(NOT(OLD.`account_id` <=> NEW.`account_id`),JSON_ARRAY('account_id'),JSON_ARRAY()),IF(NOT(OLD.`account_version` <=> NEW.`account_version`),JSON_ARRAY('account_version'),JSON_ARRAY()),IF(NOT(OLD.`author_id` <=> NEW.`author_id`),JSON_ARRAY('author_id'),JSON_ARRAY()),IF(NOT(OLD.`effective_at` <=> NEW.`effective_at`),JSON_ARRAY('effective_at'),JSON_ARRAY()),IF(NOT(OLD.`source` <=> NEW.`source`),JSON_ARRAY('source'),JSON_ARRAY()),IF(NOT(OLD.`reason` <=> NEW.`reason`),JSON_ARRAY('reason'),JSON_ARRAY())));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_r_account_assignment_d_v1` AFTER DELETE ON `roi_account_assignment` FOR EACH ROW
BEGIN
 IF 1=1 THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (OLD.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_path,NULL),'account',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'delete'),'roi_account_assignment',CAST(OLD.`id` AS CHAR),CAST(OLD.account_id AS CHAR),LEFT(COALESCE(CAST(OLD.`id` AS CHAR),''),255),CAST(OLD.author_id AS CHAR),CAST(OLD.author_id AS CHAR),JSON_OBJECT('id',CAST(OLD.`id` AS CHAR),'account_id',CAST(OLD.`account_id` AS CHAR),'account_version',OLD.`account_version`,'author_id',CAST(OLD.`author_id` AS CHAR),'effective_at',CAST(OLD.`effective_at` AS CHAR),'source',CAST(OLD.`source` AS CHAR),'reason',IF(OLD.`reason` IS NULL,NULL,'[自由文本原文未存入审计]')),NULL,JSON_ARRAY('id','tenant_id','account_id','account_version','author_id','effective_at','source','reason'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_r_account_ban_history_i_v1` AFTER INSERT ON `roi_account_ban_history` FOR EACH ROW
BEGIN
 IF 1=1 THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (NEW.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_path,NULL),'account',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'create'),'roi_account_ban_history',CAST(NEW.`id` AS CHAR),CAST(NEW.account_id AS CHAR),LEFT(COALESCE(CAST(NEW.`id` AS CHAR),''),255),(SELECT CAST(r.author_id AS CHAR) FROM roi_record r WHERE r.tenant_id=NEW.tenant_id AND r.id=CAST(NEW.account_id AS CHAR) LIMIT 1),NULL,NULL,JSON_OBJECT('id',CAST(NEW.`id` AS CHAR),'account_id',CAST(NEW.`account_id` AS CHAR),'account_version',NEW.`account_version`,'ban_type',CAST(NEW.`ban_type` AS CHAR),'starts_at',CAST(NEW.`starts_at` AS CHAR),'ends_at',CAST(NEW.`ends_at` AS CHAR),'permanent',NEW.`permanent`,'reason',IF(NEW.`reason` IS NULL,NULL,'[自由文本原文未存入审计]'),'source',CAST(NEW.`source` AS CHAR),'source_session_id',CAST(NEW.`source_session_id` AS CHAR),'source_key',CAST(NEW.`source_key` AS CHAR),'resolved_at',CAST(NEW.`resolved_at` AS CHAR),'resolved_by',CAST(NEW.`resolved_by` AS CHAR),'resolution_note',IF(NEW.`resolution_note` IS NULL,NULL,'[自由文本原文未存入审计]')),JSON_ARRAY('id','tenant_id','account_id','account_version','ban_type','starts_at','ends_at','permanent','reason','source','source_session_id','source_key','resolved_at','resolved_by','resolution_note'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_r_account_ban_history_u_v1` AFTER UPDATE ON `roi_account_ban_history` FOR EACH ROW
BEGIN
 IF (1=1 OR 1=1) AND (NOT(OLD.`id` <=> NEW.`id`) OR NOT(OLD.`tenant_id` <=> NEW.`tenant_id`) OR NOT(OLD.`account_id` <=> NEW.`account_id`) OR NOT(OLD.`account_version` <=> NEW.`account_version`) OR NOT(OLD.`ban_type` <=> NEW.`ban_type`) OR NOT(OLD.`starts_at` <=> NEW.`starts_at`) OR NOT(OLD.`ends_at` <=> NEW.`ends_at`) OR NOT(OLD.`permanent` <=> NEW.`permanent`) OR NOT(OLD.`reason` <=> NEW.`reason`) OR NOT(OLD.`source` <=> NEW.`source`) OR NOT(OLD.`source_session_id` <=> NEW.`source_session_id`) OR NOT(OLD.`source_key` <=> NEW.`source_key`) OR NOT(OLD.`resolved_at` <=> NEW.`resolved_at`) OR NOT(OLD.`resolved_by` <=> NEW.`resolved_by`) OR NOT(OLD.`resolution_note` <=> NEW.`resolution_note`)) THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (NEW.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_path,NULL),'account',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'update'),'roi_account_ban_history',CAST(NEW.`id` AS CHAR),CAST(NEW.account_id AS CHAR),LEFT(COALESCE(CAST(NEW.`id` AS CHAR),''),255),(SELECT CAST(r.author_id AS CHAR) FROM roi_record r WHERE r.tenant_id=NEW.tenant_id AND r.id=CAST(NEW.account_id AS CHAR) LIMIT 1),(SELECT CAST(r.author_id AS CHAR) FROM roi_record r WHERE r.tenant_id=OLD.tenant_id AND r.id=CAST(OLD.account_id AS CHAR) LIMIT 1),JSON_OBJECT('id',CAST(OLD.`id` AS CHAR),'account_id',CAST(OLD.`account_id` AS CHAR),'account_version',OLD.`account_version`,'ban_type',CAST(OLD.`ban_type` AS CHAR),'starts_at',CAST(OLD.`starts_at` AS CHAR),'ends_at',CAST(OLD.`ends_at` AS CHAR),'permanent',OLD.`permanent`,'reason',IF(OLD.`reason` IS NULL,NULL,'[自由文本原文未存入审计]'),'source',CAST(OLD.`source` AS CHAR),'source_session_id',CAST(OLD.`source_session_id` AS CHAR),'source_key',CAST(OLD.`source_key` AS CHAR),'resolved_at',CAST(OLD.`resolved_at` AS CHAR),'resolved_by',CAST(OLD.`resolved_by` AS CHAR),'resolution_note',IF(OLD.`resolution_note` IS NULL,NULL,'[自由文本原文未存入审计]')),JSON_OBJECT('id',CAST(NEW.`id` AS CHAR),'account_id',CAST(NEW.`account_id` AS CHAR),'account_version',NEW.`account_version`,'ban_type',CAST(NEW.`ban_type` AS CHAR),'starts_at',CAST(NEW.`starts_at` AS CHAR),'ends_at',CAST(NEW.`ends_at` AS CHAR),'permanent',NEW.`permanent`,'reason',IF(NEW.`reason` IS NULL,NULL,'[自由文本原文未存入审计]'),'source',CAST(NEW.`source` AS CHAR),'source_session_id',CAST(NEW.`source_session_id` AS CHAR),'source_key',CAST(NEW.`source_key` AS CHAR),'resolved_at',CAST(NEW.`resolved_at` AS CHAR),'resolved_by',CAST(NEW.`resolved_by` AS CHAR),'resolution_note',IF(NEW.`resolution_note` IS NULL,NULL,'[自由文本原文未存入审计]')),JSON_MERGE_PRESERVE(IF(NOT(OLD.`id` <=> NEW.`id`),JSON_ARRAY('id'),JSON_ARRAY()),IF(NOT(OLD.`tenant_id` <=> NEW.`tenant_id`),JSON_ARRAY('tenant_id'),JSON_ARRAY()),IF(NOT(OLD.`account_id` <=> NEW.`account_id`),JSON_ARRAY('account_id'),JSON_ARRAY()),IF(NOT(OLD.`account_version` <=> NEW.`account_version`),JSON_ARRAY('account_version'),JSON_ARRAY()),IF(NOT(OLD.`ban_type` <=> NEW.`ban_type`),JSON_ARRAY('ban_type'),JSON_ARRAY()),IF(NOT(OLD.`starts_at` <=> NEW.`starts_at`),JSON_ARRAY('starts_at'),JSON_ARRAY()),IF(NOT(OLD.`ends_at` <=> NEW.`ends_at`),JSON_ARRAY('ends_at'),JSON_ARRAY()),IF(NOT(OLD.`permanent` <=> NEW.`permanent`),JSON_ARRAY('permanent'),JSON_ARRAY()),IF(NOT(OLD.`reason` <=> NEW.`reason`),JSON_ARRAY('reason'),JSON_ARRAY()),IF(NOT(OLD.`source` <=> NEW.`source`),JSON_ARRAY('source'),JSON_ARRAY()),IF(NOT(OLD.`source_session_id` <=> NEW.`source_session_id`),JSON_ARRAY('source_session_id'),JSON_ARRAY()),IF(NOT(OLD.`source_key` <=> NEW.`source_key`),JSON_ARRAY('source_key'),JSON_ARRAY()),IF(NOT(OLD.`resolved_at` <=> NEW.`resolved_at`),JSON_ARRAY('resolved_at'),JSON_ARRAY()),IF(NOT(OLD.`resolved_by` <=> NEW.`resolved_by`),JSON_ARRAY('resolved_by'),JSON_ARRAY()),IF(NOT(OLD.`resolution_note` <=> NEW.`resolution_note`),JSON_ARRAY('resolution_note'),JSON_ARRAY())));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_r_account_ban_history_d_v1` AFTER DELETE ON `roi_account_ban_history` FOR EACH ROW
BEGIN
 IF 1=1 THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (OLD.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_path,NULL),'account',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'delete'),'roi_account_ban_history',CAST(OLD.`id` AS CHAR),CAST(OLD.account_id AS CHAR),LEFT(COALESCE(CAST(OLD.`id` AS CHAR),''),255),(SELECT CAST(r.author_id AS CHAR) FROM roi_record r WHERE r.tenant_id=OLD.tenant_id AND r.id=CAST(OLD.account_id AS CHAR) LIMIT 1),(SELECT CAST(r.author_id AS CHAR) FROM roi_record r WHERE r.tenant_id=OLD.tenant_id AND r.id=CAST(OLD.account_id AS CHAR) LIMIT 1),JSON_OBJECT('id',CAST(OLD.`id` AS CHAR),'account_id',CAST(OLD.`account_id` AS CHAR),'account_version',OLD.`account_version`,'ban_type',CAST(OLD.`ban_type` AS CHAR),'starts_at',CAST(OLD.`starts_at` AS CHAR),'ends_at',CAST(OLD.`ends_at` AS CHAR),'permanent',OLD.`permanent`,'reason',IF(OLD.`reason` IS NULL,NULL,'[自由文本原文未存入审计]'),'source',CAST(OLD.`source` AS CHAR),'source_session_id',CAST(OLD.`source_session_id` AS CHAR),'source_key',CAST(OLD.`source_key` AS CHAR),'resolved_at',CAST(OLD.`resolved_at` AS CHAR),'resolved_by',CAST(OLD.`resolved_by` AS CHAR),'resolution_note',IF(OLD.`resolution_note` IS NULL,NULL,'[自由文本原文未存入审计]')),NULL,JSON_ARRAY('id','tenant_id','account_id','account_version','ban_type','starts_at','ends_at','permanent','reason','source','source_session_id','source_key','resolved_at','resolved_by','resolution_note'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_r_account_qualification_i_v1` AFTER INSERT ON `roi_account_qualification` FOR EACH ROW
BEGIN
 IF 1=1 THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (NEW.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_path,NULL),'account',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'create'),'roi_account_qualification',CAST(NEW.`id` AS CHAR),CAST(NEW.account_id AS CHAR),LEFT(COALESCE(CAST(NEW.`id` AS CHAR),''),255),(SELECT CAST(r.author_id AS CHAR) FROM roi_record r WHERE r.tenant_id=NEW.tenant_id AND r.id=CAST(NEW.account_id AS CHAR) LIMIT 1),NULL,NULL,JSON_OBJECT('id',CAST(NEW.`id` AS CHAR),'account_id',CAST(NEW.`account_id` AS CHAR),'company_id',CAST(NEW.`company_id` AS CHAR),'blue_v',NEW.`blue_v`,'valid_from',CAST(NEW.`valid_from` AS CHAR),'valid_to',CAST(NEW.`valid_to` AS CHAR),'original_valid_to',CAST(NEW.`original_valid_to` AS CHAR),'reason',IF(NEW.`reason` IS NULL,NULL,'[自由文本原文未存入审计]'),'evidence_ids',IF(NEW.`evidence_ids` IS NULL,NULL,'[已脱敏]'),'version',NEW.`version`,'closed_by',CAST(NEW.`closed_by` AS CHAR),'closed_at',CAST(NEW.`closed_at` AS CHAR)),JSON_ARRAY('id','tenant_id','account_id','company_id','blue_v','valid_from','valid_to','original_valid_to','reason','evidence_ids','version','closed_by','closed_at'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_r_account_qualification_u_v1` AFTER UPDATE ON `roi_account_qualification` FOR EACH ROW
BEGIN
 IF (1=1 OR 1=1) AND (NOT(OLD.`id` <=> NEW.`id`) OR NOT(OLD.`tenant_id` <=> NEW.`tenant_id`) OR NOT(OLD.`account_id` <=> NEW.`account_id`) OR NOT(OLD.`company_id` <=> NEW.`company_id`) OR NOT(OLD.`blue_v` <=> NEW.`blue_v`) OR NOT(OLD.`valid_from` <=> NEW.`valid_from`) OR NOT(OLD.`valid_to` <=> NEW.`valid_to`) OR NOT(OLD.`original_valid_to` <=> NEW.`original_valid_to`) OR NOT(OLD.`reason` <=> NEW.`reason`) OR NOT(OLD.`evidence_ids` <=> NEW.`evidence_ids`) OR NOT(OLD.`version` <=> NEW.`version`) OR NOT(OLD.`closed_by` <=> NEW.`closed_by`) OR NOT(OLD.`closed_at` <=> NEW.`closed_at`)) THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (NEW.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_path,NULL),'account',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'update'),'roi_account_qualification',CAST(NEW.`id` AS CHAR),CAST(NEW.account_id AS CHAR),LEFT(COALESCE(CAST(NEW.`id` AS CHAR),''),255),(SELECT CAST(r.author_id AS CHAR) FROM roi_record r WHERE r.tenant_id=NEW.tenant_id AND r.id=CAST(NEW.account_id AS CHAR) LIMIT 1),(SELECT CAST(r.author_id AS CHAR) FROM roi_record r WHERE r.tenant_id=OLD.tenant_id AND r.id=CAST(OLD.account_id AS CHAR) LIMIT 1),JSON_OBJECT('id',CAST(OLD.`id` AS CHAR),'account_id',CAST(OLD.`account_id` AS CHAR),'company_id',CAST(OLD.`company_id` AS CHAR),'blue_v',OLD.`blue_v`,'valid_from',CAST(OLD.`valid_from` AS CHAR),'valid_to',CAST(OLD.`valid_to` AS CHAR),'original_valid_to',CAST(OLD.`original_valid_to` AS CHAR),'reason',IF(OLD.`reason` IS NULL,NULL,'[自由文本原文未存入审计]'),'evidence_ids',IF(OLD.`evidence_ids` IS NULL,NULL,'[已脱敏]'),'version',OLD.`version`,'closed_by',CAST(OLD.`closed_by` AS CHAR),'closed_at',CAST(OLD.`closed_at` AS CHAR)),JSON_OBJECT('id',CAST(NEW.`id` AS CHAR),'account_id',CAST(NEW.`account_id` AS CHAR),'company_id',CAST(NEW.`company_id` AS CHAR),'blue_v',NEW.`blue_v`,'valid_from',CAST(NEW.`valid_from` AS CHAR),'valid_to',CAST(NEW.`valid_to` AS CHAR),'original_valid_to',CAST(NEW.`original_valid_to` AS CHAR),'reason',IF(NEW.`reason` IS NULL,NULL,'[自由文本原文未存入审计]'),'evidence_ids',IF(NEW.`evidence_ids` IS NULL,NULL,'[已脱敏]'),'version',NEW.`version`,'closed_by',CAST(NEW.`closed_by` AS CHAR),'closed_at',CAST(NEW.`closed_at` AS CHAR)),JSON_MERGE_PRESERVE(IF(NOT(OLD.`id` <=> NEW.`id`),JSON_ARRAY('id'),JSON_ARRAY()),IF(NOT(OLD.`tenant_id` <=> NEW.`tenant_id`),JSON_ARRAY('tenant_id'),JSON_ARRAY()),IF(NOT(OLD.`account_id` <=> NEW.`account_id`),JSON_ARRAY('account_id'),JSON_ARRAY()),IF(NOT(OLD.`company_id` <=> NEW.`company_id`),JSON_ARRAY('company_id'),JSON_ARRAY()),IF(NOT(OLD.`blue_v` <=> NEW.`blue_v`),JSON_ARRAY('blue_v'),JSON_ARRAY()),IF(NOT(OLD.`valid_from` <=> NEW.`valid_from`),JSON_ARRAY('valid_from'),JSON_ARRAY()),IF(NOT(OLD.`valid_to` <=> NEW.`valid_to`),JSON_ARRAY('valid_to'),JSON_ARRAY()),IF(NOT(OLD.`original_valid_to` <=> NEW.`original_valid_to`),JSON_ARRAY('original_valid_to'),JSON_ARRAY()),IF(NOT(OLD.`reason` <=> NEW.`reason`),JSON_ARRAY('reason'),JSON_ARRAY()),IF(NOT(OLD.`evidence_ids` <=> NEW.`evidence_ids`),JSON_ARRAY('evidence_ids'),JSON_ARRAY()),IF(NOT(OLD.`version` <=> NEW.`version`),JSON_ARRAY('version'),JSON_ARRAY()),IF(NOT(OLD.`closed_by` <=> NEW.`closed_by`),JSON_ARRAY('closed_by'),JSON_ARRAY()),IF(NOT(OLD.`closed_at` <=> NEW.`closed_at`),JSON_ARRAY('closed_at'),JSON_ARRAY())));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_r_account_qualification_d_v1` AFTER DELETE ON `roi_account_qualification` FOR EACH ROW
BEGIN
 IF 1=1 THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (OLD.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_path,NULL),'account',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'delete'),'roi_account_qualification',CAST(OLD.`id` AS CHAR),CAST(OLD.account_id AS CHAR),LEFT(COALESCE(CAST(OLD.`id` AS CHAR),''),255),(SELECT CAST(r.author_id AS CHAR) FROM roi_record r WHERE r.tenant_id=OLD.tenant_id AND r.id=CAST(OLD.account_id AS CHAR) LIMIT 1),(SELECT CAST(r.author_id AS CHAR) FROM roi_record r WHERE r.tenant_id=OLD.tenant_id AND r.id=CAST(OLD.account_id AS CHAR) LIMIT 1),JSON_OBJECT('id',CAST(OLD.`id` AS CHAR),'account_id',CAST(OLD.`account_id` AS CHAR),'company_id',CAST(OLD.`company_id` AS CHAR),'blue_v',OLD.`blue_v`,'valid_from',CAST(OLD.`valid_from` AS CHAR),'valid_to',CAST(OLD.`valid_to` AS CHAR),'original_valid_to',CAST(OLD.`original_valid_to` AS CHAR),'reason',IF(OLD.`reason` IS NULL,NULL,'[自由文本原文未存入审计]'),'evidence_ids',IF(OLD.`evidence_ids` IS NULL,NULL,'[已脱敏]'),'version',OLD.`version`,'closed_by',CAST(OLD.`closed_by` AS CHAR),'closed_at',CAST(OLD.`closed_at` AS CHAR)),NULL,JSON_ARRAY('id','tenant_id','account_id','company_id','blue_v','valid_from','valid_to','original_valid_to','reason','evidence_ids','version','closed_by','closed_at'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_r_session_attribution_binding_i_v1` AFTER INSERT ON `roi_session_attribution_binding` FOR EACH ROW
BEGIN
 IF 1=1 THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (NEW.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_path,NULL),'attribution',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'create'),'roi_session_attribution_binding',CONCAT_WS(':',CAST(NEW.`session_id` AS CHAR)),NEW.session_id,LEFT(COALESCE(CONCAT_WS(':',CAST(NEW.`session_id` AS CHAR)),''),255),CAST(NEW.author_id AS CHAR),NULL,NULL,JSON_OBJECT('session_id',CAST(NEW.`session_id` AS CHAR),'author_id',CAST(NEW.`author_id` AS CHAR),'binding_payload',IF(NEW.`binding_payload` IS NULL,NULL,'[已脱敏]')),JSON_ARRAY('tenant_id','session_id','author_id','binding_payload'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_r_session_attribution_binding_u_v1` AFTER UPDATE ON `roi_session_attribution_binding` FOR EACH ROW
BEGIN
 IF (1=1 OR 1=1) AND (NOT(OLD.`tenant_id` <=> NEW.`tenant_id`) OR NOT(OLD.`session_id` <=> NEW.`session_id`) OR NOT(OLD.`author_id` <=> NEW.`author_id`) OR NOT(OLD.`binding_payload` <=> NEW.`binding_payload`)) THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (NEW.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_path,NULL),'attribution',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'update'),'roi_session_attribution_binding',CONCAT_WS(':',CAST(NEW.`session_id` AS CHAR)),NEW.session_id,LEFT(COALESCE(CONCAT_WS(':',CAST(NEW.`session_id` AS CHAR)),''),255),CAST(NEW.author_id AS CHAR),CAST(OLD.author_id AS CHAR),JSON_OBJECT('session_id',CAST(OLD.`session_id` AS CHAR),'author_id',CAST(OLD.`author_id` AS CHAR),'binding_payload',IF(OLD.`binding_payload` IS NULL,NULL,'[已脱敏]')),JSON_OBJECT('session_id',CAST(NEW.`session_id` AS CHAR),'author_id',CAST(NEW.`author_id` AS CHAR),'binding_payload',IF(NEW.`binding_payload` IS NULL,NULL,'[已脱敏]')),JSON_MERGE_PRESERVE(IF(NOT(OLD.`tenant_id` <=> NEW.`tenant_id`),JSON_ARRAY('tenant_id'),JSON_ARRAY()),IF(NOT(OLD.`session_id` <=> NEW.`session_id`),JSON_ARRAY('session_id'),JSON_ARRAY()),IF(NOT(OLD.`author_id` <=> NEW.`author_id`),JSON_ARRAY('author_id'),JSON_ARRAY()),IF(NOT(OLD.`binding_payload` <=> NEW.`binding_payload`),JSON_ARRAY('binding_payload'),JSON_ARRAY())));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_r_session_attribution_binding_d_v1` AFTER DELETE ON `roi_session_attribution_binding` FOR EACH ROW
BEGIN
 IF 1=1 THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (OLD.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_path,NULL),'attribution',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'delete'),'roi_session_attribution_binding',CONCAT_WS(':',CAST(OLD.`session_id` AS CHAR)),OLD.session_id,LEFT(COALESCE(CONCAT_WS(':',CAST(OLD.`session_id` AS CHAR)),''),255),CAST(OLD.author_id AS CHAR),CAST(OLD.author_id AS CHAR),JSON_OBJECT('session_id',CAST(OLD.`session_id` AS CHAR),'author_id',CAST(OLD.`author_id` AS CHAR),'binding_payload',IF(OLD.`binding_payload` IS NULL,NULL,'[已脱敏]')),NULL,JSON_ARRAY('tenant_id','session_id','author_id','binding_payload'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_r_session_attribution_article_i_v1` AFTER INSERT ON `roi_session_attribution_article` FOR EACH ROW
BEGIN
 IF 1=1 THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (NEW.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_path,NULL),'attribution',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'create'),'roi_session_attribution_article',CONCAT_WS(':',CAST(NEW.`session_id` AS CHAR),CAST(NEW.`source_type` AS CHAR),CAST(NEW.`source_id` AS CHAR)),NEW.session_id,LEFT(COALESCE(CONCAT_WS(':',CAST(NEW.`session_id` AS CHAR),CAST(NEW.`source_type` AS CHAR),CAST(NEW.`source_id` AS CHAR)),''),255),CAST(NEW.author_id AS CHAR),NULL,NULL,JSON_OBJECT('session_id',CAST(NEW.`session_id` AS CHAR),'source_type',CAST(NEW.`source_type` AS CHAR),'source_id',CAST(NEW.`source_id` AS CHAR),'author_id',CAST(NEW.`author_id` AS CHAR)),JSON_ARRAY('tenant_id','session_id','source_type','source_id','author_id'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_r_session_attribution_article_u_v1` AFTER UPDATE ON `roi_session_attribution_article` FOR EACH ROW
BEGIN
 IF (1=1 OR 1=1) AND (NOT(OLD.`tenant_id` <=> NEW.`tenant_id`) OR NOT(OLD.`session_id` <=> NEW.`session_id`) OR NOT(OLD.`source_type` <=> NEW.`source_type`) OR NOT(OLD.`source_id` <=> NEW.`source_id`) OR NOT(OLD.`author_id` <=> NEW.`author_id`)) THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (NEW.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_path,NULL),'attribution',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'update'),'roi_session_attribution_article',CONCAT_WS(':',CAST(NEW.`session_id` AS CHAR),CAST(NEW.`source_type` AS CHAR),CAST(NEW.`source_id` AS CHAR)),NEW.session_id,LEFT(COALESCE(CONCAT_WS(':',CAST(NEW.`session_id` AS CHAR),CAST(NEW.`source_type` AS CHAR),CAST(NEW.`source_id` AS CHAR)),''),255),CAST(NEW.author_id AS CHAR),CAST(OLD.author_id AS CHAR),JSON_OBJECT('session_id',CAST(OLD.`session_id` AS CHAR),'source_type',CAST(OLD.`source_type` AS CHAR),'source_id',CAST(OLD.`source_id` AS CHAR),'author_id',CAST(OLD.`author_id` AS CHAR)),JSON_OBJECT('session_id',CAST(NEW.`session_id` AS CHAR),'source_type',CAST(NEW.`source_type` AS CHAR),'source_id',CAST(NEW.`source_id` AS CHAR),'author_id',CAST(NEW.`author_id` AS CHAR)),JSON_MERGE_PRESERVE(IF(NOT(OLD.`tenant_id` <=> NEW.`tenant_id`),JSON_ARRAY('tenant_id'),JSON_ARRAY()),IF(NOT(OLD.`session_id` <=> NEW.`session_id`),JSON_ARRAY('session_id'),JSON_ARRAY()),IF(NOT(OLD.`source_type` <=> NEW.`source_type`),JSON_ARRAY('source_type'),JSON_ARRAY()),IF(NOT(OLD.`source_id` <=> NEW.`source_id`),JSON_ARRAY('source_id'),JSON_ARRAY()),IF(NOT(OLD.`author_id` <=> NEW.`author_id`),JSON_ARRAY('author_id'),JSON_ARRAY())));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_r_session_attribution_article_d_v1` AFTER DELETE ON `roi_session_attribution_article` FOR EACH ROW
BEGIN
 IF 1=1 THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (OLD.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_path,NULL),'attribution',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'delete'),'roi_session_attribution_article',CONCAT_WS(':',CAST(OLD.`session_id` AS CHAR),CAST(OLD.`source_type` AS CHAR),CAST(OLD.`source_id` AS CHAR)),OLD.session_id,LEFT(COALESCE(CONCAT_WS(':',CAST(OLD.`session_id` AS CHAR),CAST(OLD.`source_type` AS CHAR),CAST(OLD.`source_id` AS CHAR)),''),255),CAST(OLD.author_id AS CHAR),CAST(OLD.author_id AS CHAR),JSON_OBJECT('session_id',CAST(OLD.`session_id` AS CHAR),'source_type',CAST(OLD.`source_type` AS CHAR),'source_id',CAST(OLD.`source_id` AS CHAR),'author_id',CAST(OLD.`author_id` AS CHAR)),NULL,JSON_ARRAY('tenant_id','session_id','source_type','source_id','author_id'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_r_wecom_session_link_i_v1` AFTER INSERT ON `roi_wecom_session_link` FOR EACH ROW
BEGIN
 IF 1=1 THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (NEW.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_path,NULL),'wecom',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'create'),'roi_wecom_session_link',CAST(NEW.`id` AS CHAR),NEW.session_id,LEFT(COALESCE(CAST(NEW.`id` AS CHAR),''),255),CAST(NEW.author_id AS CHAR),NULL,NULL,JSON_OBJECT('id',CAST(NEW.`id` AS CHAR),'corp_id',CAST(NEW.`corp_id` AS CHAR),'link_id',CAST(NEW.`link_id` AS CHAR),'session_id',CAST(NEW.`session_id` AS CHAR),'session_no',CAST(NEW.`session_no` AS CHAR),'author_id',CAST(NEW.`author_id` AS CHAR),'attribution_state',CAST(NEW.`attribution_state` AS CHAR)),JSON_ARRAY('id','tenant_id','corp_id','link_id','session_id','session_no','author_id','attribution_state'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_r_wecom_session_link_u_v1` AFTER UPDATE ON `roi_wecom_session_link` FOR EACH ROW
BEGIN
 IF (1=1 OR 1=1) AND (NOT(OLD.`id` <=> NEW.`id`) OR NOT(OLD.`tenant_id` <=> NEW.`tenant_id`) OR NOT(OLD.`corp_id` <=> NEW.`corp_id`) OR NOT(OLD.`link_id` <=> NEW.`link_id`) OR NOT(OLD.`session_id` <=> NEW.`session_id`) OR NOT(OLD.`session_no` <=> NEW.`session_no`) OR NOT(OLD.`author_id` <=> NEW.`author_id`) OR NOT(OLD.`attribution_state` <=> NEW.`attribution_state`)) THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (NEW.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_path,NULL),'wecom',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'update'),'roi_wecom_session_link',CAST(NEW.`id` AS CHAR),NEW.session_id,LEFT(COALESCE(CAST(NEW.`id` AS CHAR),''),255),CAST(NEW.author_id AS CHAR),CAST(OLD.author_id AS CHAR),JSON_OBJECT('id',CAST(OLD.`id` AS CHAR),'corp_id',CAST(OLD.`corp_id` AS CHAR),'link_id',CAST(OLD.`link_id` AS CHAR),'session_id',CAST(OLD.`session_id` AS CHAR),'session_no',CAST(OLD.`session_no` AS CHAR),'author_id',CAST(OLD.`author_id` AS CHAR),'attribution_state',CAST(OLD.`attribution_state` AS CHAR)),JSON_OBJECT('id',CAST(NEW.`id` AS CHAR),'corp_id',CAST(NEW.`corp_id` AS CHAR),'link_id',CAST(NEW.`link_id` AS CHAR),'session_id',CAST(NEW.`session_id` AS CHAR),'session_no',CAST(NEW.`session_no` AS CHAR),'author_id',CAST(NEW.`author_id` AS CHAR),'attribution_state',CAST(NEW.`attribution_state` AS CHAR)),JSON_MERGE_PRESERVE(IF(NOT(OLD.`id` <=> NEW.`id`),JSON_ARRAY('id'),JSON_ARRAY()),IF(NOT(OLD.`tenant_id` <=> NEW.`tenant_id`),JSON_ARRAY('tenant_id'),JSON_ARRAY()),IF(NOT(OLD.`corp_id` <=> NEW.`corp_id`),JSON_ARRAY('corp_id'),JSON_ARRAY()),IF(NOT(OLD.`link_id` <=> NEW.`link_id`),JSON_ARRAY('link_id'),JSON_ARRAY()),IF(NOT(OLD.`session_id` <=> NEW.`session_id`),JSON_ARRAY('session_id'),JSON_ARRAY()),IF(NOT(OLD.`session_no` <=> NEW.`session_no`),JSON_ARRAY('session_no'),JSON_ARRAY()),IF(NOT(OLD.`author_id` <=> NEW.`author_id`),JSON_ARRAY('author_id'),JSON_ARRAY()),IF(NOT(OLD.`attribution_state` <=> NEW.`attribution_state`),JSON_ARRAY('attribution_state'),JSON_ARRAY())));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_r_wecom_session_link_d_v1` AFTER DELETE ON `roi_wecom_session_link` FOR EACH ROW
BEGIN
 IF 1=1 THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (OLD.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_path,NULL),'wecom',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'delete'),'roi_wecom_session_link',CAST(OLD.`id` AS CHAR),OLD.session_id,LEFT(COALESCE(CAST(OLD.`id` AS CHAR),''),255),CAST(OLD.author_id AS CHAR),CAST(OLD.author_id AS CHAR),JSON_OBJECT('id',CAST(OLD.`id` AS CHAR),'corp_id',CAST(OLD.`corp_id` AS CHAR),'link_id',CAST(OLD.`link_id` AS CHAR),'session_id',CAST(OLD.`session_id` AS CHAR),'session_no',CAST(OLD.`session_no` AS CHAR),'author_id',CAST(OLD.`author_id` AS CHAR),'attribution_state',CAST(OLD.`attribution_state` AS CHAR)),NULL,JSON_ARRAY('id','tenant_id','corp_id','link_id','session_id','session_no','author_id','attribution_state'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_r_wecom_event_i_v1` AFTER INSERT ON `roi_wecom_event` FOR EACH ROW
BEGIN
 IF 1=1 THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (NEW.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_path,NULL),'wecom',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'create'),'roi_wecom_event',CONCAT_WS(':',CAST(NEW.`event_key` AS CHAR)),NEW.session_id,LEFT(COALESCE(CONCAT_WS(':',CAST(NEW.`event_key` AS CHAR)),''),255),CAST(NEW.author_id AS CHAR),NULL,NULL,JSON_OBJECT('event_key',CAST(NEW.`event_key` AS CHAR),'config_id',CAST(NEW.`config_id` AS CHAR),'corp_id',CAST(NEW.`corp_id` AS CHAR),'event_type',CAST(NEW.`event_type` AS CHAR),'change_type',CAST(NEW.`change_type` AS CHAR),'external_user_id',IF(NEW.`external_user_id` IS NULL,NULL,'[已脱敏]'),'member_user_id',IF(NEW.`member_user_id` IS NULL,NULL,'[已脱敏]'),'state_value',IF(NEW.`state_value` IS NULL,NULL,'[已脱敏]'),'event_time',CAST(NEW.`event_time` AS CHAR),'link_id',CAST(NEW.`link_id` AS CHAR),'session_id',CAST(NEW.`session_id` AS CHAR),'session_no',CAST(NEW.`session_no` AS CHAR),'author_id',CAST(NEW.`author_id` AS CHAR),'attribution_status',CAST(NEW.`attribution_status` AS CHAR),'reason',IF(NEW.`reason` IS NULL,NULL,'[自由文本原文未存入审计]'),'payload_json',IF(NEW.`payload_json` IS NULL,NULL,'[原始内容未存入审计]')),JSON_ARRAY('tenant_id','event_key','config_id','corp_id','event_type','change_type','external_user_id','member_user_id','state_value','event_time','link_id','session_id','session_no','author_id','attribution_status','reason','payload_json'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_r_wecom_event_u_v1` AFTER UPDATE ON `roi_wecom_event` FOR EACH ROW
BEGIN
 IF (1=1 OR 1=1) AND (NOT(OLD.`tenant_id` <=> NEW.`tenant_id`) OR NOT(OLD.`event_key` <=> NEW.`event_key`) OR NOT(OLD.`config_id` <=> NEW.`config_id`) OR NOT(OLD.`corp_id` <=> NEW.`corp_id`) OR NOT(OLD.`event_type` <=> NEW.`event_type`) OR NOT(OLD.`change_type` <=> NEW.`change_type`) OR NOT(OLD.`external_user_id` <=> NEW.`external_user_id`) OR NOT(OLD.`member_user_id` <=> NEW.`member_user_id`) OR NOT(OLD.`state_value` <=> NEW.`state_value`) OR NOT(OLD.`event_time` <=> NEW.`event_time`) OR NOT(OLD.`link_id` <=> NEW.`link_id`) OR NOT(OLD.`session_id` <=> NEW.`session_id`) OR NOT(OLD.`session_no` <=> NEW.`session_no`) OR NOT(OLD.`author_id` <=> NEW.`author_id`) OR NOT(OLD.`attribution_status` <=> NEW.`attribution_status`) OR NOT(OLD.`reason` <=> NEW.`reason`) OR NOT(OLD.`payload_json` <=> NEW.`payload_json`)) THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (NEW.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_path,NULL),'wecom',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'update'),'roi_wecom_event',CONCAT_WS(':',CAST(NEW.`event_key` AS CHAR)),NEW.session_id,LEFT(COALESCE(CONCAT_WS(':',CAST(NEW.`event_key` AS CHAR)),''),255),CAST(NEW.author_id AS CHAR),CAST(OLD.author_id AS CHAR),JSON_OBJECT('event_key',CAST(OLD.`event_key` AS CHAR),'config_id',CAST(OLD.`config_id` AS CHAR),'corp_id',CAST(OLD.`corp_id` AS CHAR),'event_type',CAST(OLD.`event_type` AS CHAR),'change_type',CAST(OLD.`change_type` AS CHAR),'external_user_id',IF(OLD.`external_user_id` IS NULL,NULL,'[已脱敏]'),'member_user_id',IF(OLD.`member_user_id` IS NULL,NULL,'[已脱敏]'),'state_value',IF(OLD.`state_value` IS NULL,NULL,'[已脱敏]'),'event_time',CAST(OLD.`event_time` AS CHAR),'link_id',CAST(OLD.`link_id` AS CHAR),'session_id',CAST(OLD.`session_id` AS CHAR),'session_no',CAST(OLD.`session_no` AS CHAR),'author_id',CAST(OLD.`author_id` AS CHAR),'attribution_status',CAST(OLD.`attribution_status` AS CHAR),'reason',IF(OLD.`reason` IS NULL,NULL,'[自由文本原文未存入审计]'),'payload_json',IF(OLD.`payload_json` IS NULL,NULL,'[原始内容未存入审计]')),JSON_OBJECT('event_key',CAST(NEW.`event_key` AS CHAR),'config_id',CAST(NEW.`config_id` AS CHAR),'corp_id',CAST(NEW.`corp_id` AS CHAR),'event_type',CAST(NEW.`event_type` AS CHAR),'change_type',CAST(NEW.`change_type` AS CHAR),'external_user_id',IF(NEW.`external_user_id` IS NULL,NULL,'[已脱敏]'),'member_user_id',IF(NEW.`member_user_id` IS NULL,NULL,'[已脱敏]'),'state_value',IF(NEW.`state_value` IS NULL,NULL,'[已脱敏]'),'event_time',CAST(NEW.`event_time` AS CHAR),'link_id',CAST(NEW.`link_id` AS CHAR),'session_id',CAST(NEW.`session_id` AS CHAR),'session_no',CAST(NEW.`session_no` AS CHAR),'author_id',CAST(NEW.`author_id` AS CHAR),'attribution_status',CAST(NEW.`attribution_status` AS CHAR),'reason',IF(NEW.`reason` IS NULL,NULL,'[自由文本原文未存入审计]'),'payload_json',IF(NEW.`payload_json` IS NULL,NULL,'[原始内容未存入审计]')),JSON_MERGE_PRESERVE(IF(NOT(OLD.`tenant_id` <=> NEW.`tenant_id`),JSON_ARRAY('tenant_id'),JSON_ARRAY()),IF(NOT(OLD.`event_key` <=> NEW.`event_key`),JSON_ARRAY('event_key'),JSON_ARRAY()),IF(NOT(OLD.`config_id` <=> NEW.`config_id`),JSON_ARRAY('config_id'),JSON_ARRAY()),IF(NOT(OLD.`corp_id` <=> NEW.`corp_id`),JSON_ARRAY('corp_id'),JSON_ARRAY()),IF(NOT(OLD.`event_type` <=> NEW.`event_type`),JSON_ARRAY('event_type'),JSON_ARRAY()),IF(NOT(OLD.`change_type` <=> NEW.`change_type`),JSON_ARRAY('change_type'),JSON_ARRAY()),IF(NOT(OLD.`external_user_id` <=> NEW.`external_user_id`),JSON_ARRAY('external_user_id'),JSON_ARRAY()),IF(NOT(OLD.`member_user_id` <=> NEW.`member_user_id`),JSON_ARRAY('member_user_id'),JSON_ARRAY()),IF(NOT(OLD.`state_value` <=> NEW.`state_value`),JSON_ARRAY('state_value'),JSON_ARRAY()),IF(NOT(OLD.`event_time` <=> NEW.`event_time`),JSON_ARRAY('event_time'),JSON_ARRAY()),IF(NOT(OLD.`link_id` <=> NEW.`link_id`),JSON_ARRAY('link_id'),JSON_ARRAY()),IF(NOT(OLD.`session_id` <=> NEW.`session_id`),JSON_ARRAY('session_id'),JSON_ARRAY()),IF(NOT(OLD.`session_no` <=> NEW.`session_no`),JSON_ARRAY('session_no'),JSON_ARRAY()),IF(NOT(OLD.`author_id` <=> NEW.`author_id`),JSON_ARRAY('author_id'),JSON_ARRAY()),IF(NOT(OLD.`attribution_status` <=> NEW.`attribution_status`),JSON_ARRAY('attribution_status'),JSON_ARRAY()),IF(NOT(OLD.`reason` <=> NEW.`reason`),JSON_ARRAY('reason'),JSON_ARRAY()),IF(NOT(OLD.`payload_json` <=> NEW.`payload_json`),JSON_ARRAY('payload_json'),JSON_ARRAY())));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_r_wecom_event_d_v1` AFTER DELETE ON `roi_wecom_event` FOR EACH ROW
BEGIN
 IF 1=1 THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (OLD.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_path,NULL),'wecom',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'delete'),'roi_wecom_event',CONCAT_WS(':',CAST(OLD.`event_key` AS CHAR)),OLD.session_id,LEFT(COALESCE(CONCAT_WS(':',CAST(OLD.`event_key` AS CHAR)),''),255),CAST(OLD.author_id AS CHAR),CAST(OLD.author_id AS CHAR),JSON_OBJECT('event_key',CAST(OLD.`event_key` AS CHAR),'config_id',CAST(OLD.`config_id` AS CHAR),'corp_id',CAST(OLD.`corp_id` AS CHAR),'event_type',CAST(OLD.`event_type` AS CHAR),'change_type',CAST(OLD.`change_type` AS CHAR),'external_user_id',IF(OLD.`external_user_id` IS NULL,NULL,'[已脱敏]'),'member_user_id',IF(OLD.`member_user_id` IS NULL,NULL,'[已脱敏]'),'state_value',IF(OLD.`state_value` IS NULL,NULL,'[已脱敏]'),'event_time',CAST(OLD.`event_time` AS CHAR),'link_id',CAST(OLD.`link_id` AS CHAR),'session_id',CAST(OLD.`session_id` AS CHAR),'session_no',CAST(OLD.`session_no` AS CHAR),'author_id',CAST(OLD.`author_id` AS CHAR),'attribution_status',CAST(OLD.`attribution_status` AS CHAR),'reason',IF(OLD.`reason` IS NULL,NULL,'[自由文本原文未存入审计]'),'payload_json',IF(OLD.`payload_json` IS NULL,NULL,'[原始内容未存入审计]')),NULL,JSON_ARRAY('tenant_id','event_key','config_id','corp_id','event_type','change_type','external_user_id','member_user_id','state_value','event_time','link_id','session_id','session_no','author_id','attribution_status','reason','payload_json'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_r_wecom_welcome_outbox_i_v1` AFTER INSERT ON `roi_wecom_welcome_outbox` FOR EACH ROW
BEGIN
 IF 1=1 THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (NEW.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_path,NULL),'wecom',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'create'),'roi_wecom_welcome_outbox',CAST(NEW.`id` AS CHAR),NULL,LEFT(COALESCE(CAST(NEW.`id` AS CHAR),''),255),NULL,NULL,NULL,JSON_OBJECT('id',CAST(NEW.`id` AS CHAR),'event_key',CAST(NEW.`event_key` AS CHAR),'corp_id',CAST(NEW.`corp_id` AS CHAR),'config_id',CAST(NEW.`config_id` AS CHAR),'link_id',CAST(NEW.`link_id` AS CHAR),'code_hash',CAST(NEW.`code_hash` AS CHAR),'sealed_code',IF(NEW.`sealed_code` IS NULL,NULL,'[已脱敏]'),'content_json',IF(NEW.`content_json` IS NULL,NULL,'[已脱敏]'),'status',CAST(NEW.`status` AS CHAR),'error_code',CAST(NEW.`error_code` AS CHAR),'expires_at',CAST(NEW.`expires_at` AS CHAR)),JSON_ARRAY('id','tenant_id','event_key','corp_id','config_id','link_id','code_hash','sealed_code','content_json','status','error_code','expires_at'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_r_wecom_welcome_outbox_u_v1` AFTER UPDATE ON `roi_wecom_welcome_outbox` FOR EACH ROW
BEGIN
 IF (1=1 OR 1=1) AND (NOT(OLD.`id` <=> NEW.`id`) OR NOT(OLD.`tenant_id` <=> NEW.`tenant_id`) OR NOT(OLD.`event_key` <=> NEW.`event_key`) OR NOT(OLD.`corp_id` <=> NEW.`corp_id`) OR NOT(OLD.`config_id` <=> NEW.`config_id`) OR NOT(OLD.`link_id` <=> NEW.`link_id`) OR NOT(OLD.`code_hash` <=> NEW.`code_hash`) OR NOT(OLD.`sealed_code` <=> NEW.`sealed_code`) OR NOT(OLD.`content_json` <=> NEW.`content_json`) OR NOT(OLD.`status` <=> NEW.`status`) OR NOT(OLD.`error_code` <=> NEW.`error_code`) OR NOT(OLD.`expires_at` <=> NEW.`expires_at`)) THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (NEW.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_path,NULL),'wecom',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'update'),'roi_wecom_welcome_outbox',CAST(NEW.`id` AS CHAR),NULL,LEFT(COALESCE(CAST(NEW.`id` AS CHAR),''),255),NULL,NULL,JSON_OBJECT('id',CAST(OLD.`id` AS CHAR),'event_key',CAST(OLD.`event_key` AS CHAR),'corp_id',CAST(OLD.`corp_id` AS CHAR),'config_id',CAST(OLD.`config_id` AS CHAR),'link_id',CAST(OLD.`link_id` AS CHAR),'code_hash',CAST(OLD.`code_hash` AS CHAR),'sealed_code',IF(OLD.`sealed_code` IS NULL,NULL,'[已脱敏]'),'content_json',IF(OLD.`content_json` IS NULL,NULL,'[已脱敏]'),'status',CAST(OLD.`status` AS CHAR),'error_code',CAST(OLD.`error_code` AS CHAR),'expires_at',CAST(OLD.`expires_at` AS CHAR)),JSON_OBJECT('id',CAST(NEW.`id` AS CHAR),'event_key',CAST(NEW.`event_key` AS CHAR),'corp_id',CAST(NEW.`corp_id` AS CHAR),'config_id',CAST(NEW.`config_id` AS CHAR),'link_id',CAST(NEW.`link_id` AS CHAR),'code_hash',CAST(NEW.`code_hash` AS CHAR),'sealed_code',IF(NEW.`sealed_code` IS NULL,NULL,'[已脱敏]'),'content_json',IF(NEW.`content_json` IS NULL,NULL,'[已脱敏]'),'status',CAST(NEW.`status` AS CHAR),'error_code',CAST(NEW.`error_code` AS CHAR),'expires_at',CAST(NEW.`expires_at` AS CHAR)),JSON_MERGE_PRESERVE(IF(NOT(OLD.`id` <=> NEW.`id`),JSON_ARRAY('id'),JSON_ARRAY()),IF(NOT(OLD.`tenant_id` <=> NEW.`tenant_id`),JSON_ARRAY('tenant_id'),JSON_ARRAY()),IF(NOT(OLD.`event_key` <=> NEW.`event_key`),JSON_ARRAY('event_key'),JSON_ARRAY()),IF(NOT(OLD.`corp_id` <=> NEW.`corp_id`),JSON_ARRAY('corp_id'),JSON_ARRAY()),IF(NOT(OLD.`config_id` <=> NEW.`config_id`),JSON_ARRAY('config_id'),JSON_ARRAY()),IF(NOT(OLD.`link_id` <=> NEW.`link_id`),JSON_ARRAY('link_id'),JSON_ARRAY()),IF(NOT(OLD.`code_hash` <=> NEW.`code_hash`),JSON_ARRAY('code_hash'),JSON_ARRAY()),IF(NOT(OLD.`sealed_code` <=> NEW.`sealed_code`),JSON_ARRAY('sealed_code'),JSON_ARRAY()),IF(NOT(OLD.`content_json` <=> NEW.`content_json`),JSON_ARRAY('content_json'),JSON_ARRAY()),IF(NOT(OLD.`status` <=> NEW.`status`),JSON_ARRAY('status'),JSON_ARRAY()),IF(NOT(OLD.`error_code` <=> NEW.`error_code`),JSON_ARRAY('error_code'),JSON_ARRAY()),IF(NOT(OLD.`expires_at` <=> NEW.`expires_at`),JSON_ARRAY('expires_at'),JSON_ARRAY())));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_r_wecom_welcome_outbox_d_v1` AFTER DELETE ON `roi_wecom_welcome_outbox` FOR EACH ROW
BEGIN
 IF 1=1 THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (OLD.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_path,NULL),'wecom',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'delete'),'roi_wecom_welcome_outbox',CAST(OLD.`id` AS CHAR),NULL,LEFT(COALESCE(CAST(OLD.`id` AS CHAR),''),255),NULL,NULL,JSON_OBJECT('id',CAST(OLD.`id` AS CHAR),'event_key',CAST(OLD.`event_key` AS CHAR),'corp_id',CAST(OLD.`corp_id` AS CHAR),'config_id',CAST(OLD.`config_id` AS CHAR),'link_id',CAST(OLD.`link_id` AS CHAR),'code_hash',CAST(OLD.`code_hash` AS CHAR),'sealed_code',IF(OLD.`sealed_code` IS NULL,NULL,'[已脱敏]'),'content_json',IF(OLD.`content_json` IS NULL,NULL,'[已脱敏]'),'status',CAST(OLD.`status` AS CHAR),'error_code',CAST(OLD.`error_code` AS CHAR),'expires_at',CAST(OLD.`expires_at` AS CHAR)),NULL,JSON_ARRAY('id','tenant_id','event_key','corp_id','config_id','link_id','code_hash','sealed_code','content_json','status','error_code','expires_at'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_r_wecom_callback_state_i_v1` AFTER INSERT ON `roi_wecom_callback_state` FOR EACH ROW
BEGIN
 IF 1=1 THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (NEW.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_path,NULL),'wecom',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'create'),'roi_wecom_callback_state',CONCAT_WS(':',CAST(NEW.`config_id` AS CHAR)),NULL,LEFT(COALESCE(CONCAT_WS(':',CAST(NEW.`config_id` AS CHAR)),''),255),NULL,NULL,NULL,JSON_OBJECT('config_id',CAST(NEW.`config_id` AS CHAR)),JSON_ARRAY('config_id'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_r_wecom_callback_state_u_v1` AFTER UPDATE ON `roi_wecom_callback_state` FOR EACH ROW
BEGIN
 IF (1=1 OR 1=1) AND (NOT(OLD.`config_id` <=> NEW.`config_id`)) THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (NEW.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_path,NULL),'wecom',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'update'),'roi_wecom_callback_state',CONCAT_WS(':',CAST(NEW.`config_id` AS CHAR)),NULL,LEFT(COALESCE(CONCAT_WS(':',CAST(NEW.`config_id` AS CHAR)),''),255),NULL,NULL,JSON_OBJECT('config_id',CAST(OLD.`config_id` AS CHAR)),JSON_OBJECT('config_id',CAST(NEW.`config_id` AS CHAR)),IF(NOT(OLD.`config_id` <=> NEW.`config_id`),JSON_ARRAY('config_id'),JSON_ARRAY()));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_r_wecom_callback_state_d_v1` AFTER DELETE ON `roi_wecom_callback_state` FOR EACH ROW
BEGIN
 IF 1=1 THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (OLD.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_path,NULL),'wecom',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'delete'),'roi_wecom_callback_state',CONCAT_WS(':',CAST(OLD.`config_id` AS CHAR)),NULL,LEFT(COALESCE(CONCAT_WS(':',CAST(OLD.`config_id` AS CHAR)),''),255),NULL,NULL,JSON_OBJECT('config_id',CAST(OLD.`config_id` AS CHAR)),NULL,JSON_ARRAY('config_id'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_r_approval_inbox_i_v1` AFTER INSERT ON `roi_approval_inbox` FOR EACH ROW
BEGIN
 IF 1=1 THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (NEW.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_path,NULL),'approval',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'create'),'roi_approval_inbox',CONCAT_WS(':',CAST(NEW.`instance_id` AS CHAR)),NEW.record_id,LEFT(COALESCE(CONCAT_WS(':',CAST(NEW.`instance_id` AS CHAR)),''),255),CAST(NEW.author_id AS CHAR),NULL,NULL,JSON_OBJECT('instance_id',CAST(NEW.`instance_id` AS CHAR),'process_code',CAST(NEW.`process_code` AS CHAR),'type',CAST(NEW.`type` AS CHAR),'originator_dingtalk_id',IF(NEW.`originator_dingtalk_id` IS NULL,NULL,'[已脱敏]'),'originator_backend_id',CAST(NEW.`originator_backend_id` AS CHAR),'provider_status',CAST(NEW.`provider_status` AS CHAR),'provider_result',CAST(NEW.`provider_result` AS CHAR),'match_status',CAST(NEW.`match_status` AS CHAR),'match_reason',IF(NEW.`match_reason` IS NULL,NULL,'[自由文本原文未存入审计]'),'record_id',CAST(NEW.`record_id` AS CHAR),'approval_record_id',CAST(NEW.`approval_record_id` AS CHAR),'author_id',CAST(NEW.`author_id` AS CHAR),'payload_json',IF(NEW.`payload_json` IS NULL,NULL,'[原始内容未存入审计]'),'discovered_at',CAST(NEW.`discovered_at` AS CHAR)),JSON_ARRAY('tenant_id','instance_id','process_code','type','originator_dingtalk_id','originator_backend_id','provider_status','provider_result','match_status','match_reason','record_id','approval_record_id','author_id','payload_json','discovered_at'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_r_approval_inbox_u_v1` AFTER UPDATE ON `roi_approval_inbox` FOR EACH ROW
BEGIN
 IF (1=1 OR 1=1) AND (NOT(OLD.`tenant_id` <=> NEW.`tenant_id`) OR NOT(OLD.`instance_id` <=> NEW.`instance_id`) OR NOT(OLD.`process_code` <=> NEW.`process_code`) OR NOT(OLD.`type` <=> NEW.`type`) OR NOT(OLD.`originator_dingtalk_id` <=> NEW.`originator_dingtalk_id`) OR NOT(OLD.`originator_backend_id` <=> NEW.`originator_backend_id`) OR NOT(OLD.`provider_status` <=> NEW.`provider_status`) OR NOT(OLD.`provider_result` <=> NEW.`provider_result`) OR NOT(OLD.`match_status` <=> NEW.`match_status`) OR NOT(OLD.`match_reason` <=> NEW.`match_reason`) OR NOT(OLD.`record_id` <=> NEW.`record_id`) OR NOT(OLD.`approval_record_id` <=> NEW.`approval_record_id`) OR NOT(OLD.`author_id` <=> NEW.`author_id`) OR NOT(OLD.`payload_json` <=> NEW.`payload_json`) OR NOT(OLD.`discovered_at` <=> NEW.`discovered_at`)) THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (NEW.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_path,NULL),'approval',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'update'),'roi_approval_inbox',CONCAT_WS(':',CAST(NEW.`instance_id` AS CHAR)),NEW.record_id,LEFT(COALESCE(CONCAT_WS(':',CAST(NEW.`instance_id` AS CHAR)),''),255),CAST(NEW.author_id AS CHAR),CAST(OLD.author_id AS CHAR),JSON_OBJECT('instance_id',CAST(OLD.`instance_id` AS CHAR),'process_code',CAST(OLD.`process_code` AS CHAR),'type',CAST(OLD.`type` AS CHAR),'originator_dingtalk_id',IF(OLD.`originator_dingtalk_id` IS NULL,NULL,'[已脱敏]'),'originator_backend_id',CAST(OLD.`originator_backend_id` AS CHAR),'provider_status',CAST(OLD.`provider_status` AS CHAR),'provider_result',CAST(OLD.`provider_result` AS CHAR),'match_status',CAST(OLD.`match_status` AS CHAR),'match_reason',IF(OLD.`match_reason` IS NULL,NULL,'[自由文本原文未存入审计]'),'record_id',CAST(OLD.`record_id` AS CHAR),'approval_record_id',CAST(OLD.`approval_record_id` AS CHAR),'author_id',CAST(OLD.`author_id` AS CHAR),'payload_json',IF(OLD.`payload_json` IS NULL,NULL,'[原始内容未存入审计]'),'discovered_at',CAST(OLD.`discovered_at` AS CHAR)),JSON_OBJECT('instance_id',CAST(NEW.`instance_id` AS CHAR),'process_code',CAST(NEW.`process_code` AS CHAR),'type',CAST(NEW.`type` AS CHAR),'originator_dingtalk_id',IF(NEW.`originator_dingtalk_id` IS NULL,NULL,'[已脱敏]'),'originator_backend_id',CAST(NEW.`originator_backend_id` AS CHAR),'provider_status',CAST(NEW.`provider_status` AS CHAR),'provider_result',CAST(NEW.`provider_result` AS CHAR),'match_status',CAST(NEW.`match_status` AS CHAR),'match_reason',IF(NEW.`match_reason` IS NULL,NULL,'[自由文本原文未存入审计]'),'record_id',CAST(NEW.`record_id` AS CHAR),'approval_record_id',CAST(NEW.`approval_record_id` AS CHAR),'author_id',CAST(NEW.`author_id` AS CHAR),'payload_json',IF(NEW.`payload_json` IS NULL,NULL,'[原始内容未存入审计]'),'discovered_at',CAST(NEW.`discovered_at` AS CHAR)),JSON_MERGE_PRESERVE(IF(NOT(OLD.`tenant_id` <=> NEW.`tenant_id`),JSON_ARRAY('tenant_id'),JSON_ARRAY()),IF(NOT(OLD.`instance_id` <=> NEW.`instance_id`),JSON_ARRAY('instance_id'),JSON_ARRAY()),IF(NOT(OLD.`process_code` <=> NEW.`process_code`),JSON_ARRAY('process_code'),JSON_ARRAY()),IF(NOT(OLD.`type` <=> NEW.`type`),JSON_ARRAY('type'),JSON_ARRAY()),IF(NOT(OLD.`originator_dingtalk_id` <=> NEW.`originator_dingtalk_id`),JSON_ARRAY('originator_dingtalk_id'),JSON_ARRAY()),IF(NOT(OLD.`originator_backend_id` <=> NEW.`originator_backend_id`),JSON_ARRAY('originator_backend_id'),JSON_ARRAY()),IF(NOT(OLD.`provider_status` <=> NEW.`provider_status`),JSON_ARRAY('provider_status'),JSON_ARRAY()),IF(NOT(OLD.`provider_result` <=> NEW.`provider_result`),JSON_ARRAY('provider_result'),JSON_ARRAY()),IF(NOT(OLD.`match_status` <=> NEW.`match_status`),JSON_ARRAY('match_status'),JSON_ARRAY()),IF(NOT(OLD.`match_reason` <=> NEW.`match_reason`),JSON_ARRAY('match_reason'),JSON_ARRAY()),IF(NOT(OLD.`record_id` <=> NEW.`record_id`),JSON_ARRAY('record_id'),JSON_ARRAY()),IF(NOT(OLD.`approval_record_id` <=> NEW.`approval_record_id`),JSON_ARRAY('approval_record_id'),JSON_ARRAY()),IF(NOT(OLD.`author_id` <=> NEW.`author_id`),JSON_ARRAY('author_id'),JSON_ARRAY()),IF(NOT(OLD.`payload_json` <=> NEW.`payload_json`),JSON_ARRAY('payload_json'),JSON_ARRAY()),IF(NOT(OLD.`discovered_at` <=> NEW.`discovered_at`),JSON_ARRAY('discovered_at'),JSON_ARRAY())));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_r_approval_inbox_d_v1` AFTER DELETE ON `roi_approval_inbox` FOR EACH ROW
BEGIN
 IF 1=1 THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (OLD.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_path,NULL),'approval',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'delete'),'roi_approval_inbox',CONCAT_WS(':',CAST(OLD.`instance_id` AS CHAR)),OLD.record_id,LEFT(COALESCE(CONCAT_WS(':',CAST(OLD.`instance_id` AS CHAR)),''),255),CAST(OLD.author_id AS CHAR),CAST(OLD.author_id AS CHAR),JSON_OBJECT('instance_id',CAST(OLD.`instance_id` AS CHAR),'process_code',CAST(OLD.`process_code` AS CHAR),'type',CAST(OLD.`type` AS CHAR),'originator_dingtalk_id',IF(OLD.`originator_dingtalk_id` IS NULL,NULL,'[已脱敏]'),'originator_backend_id',CAST(OLD.`originator_backend_id` AS CHAR),'provider_status',CAST(OLD.`provider_status` AS CHAR),'provider_result',CAST(OLD.`provider_result` AS CHAR),'match_status',CAST(OLD.`match_status` AS CHAR),'match_reason',IF(OLD.`match_reason` IS NULL,NULL,'[自由文本原文未存入审计]'),'record_id',CAST(OLD.`record_id` AS CHAR),'approval_record_id',CAST(OLD.`approval_record_id` AS CHAR),'author_id',CAST(OLD.`author_id` AS CHAR),'payload_json',IF(OLD.`payload_json` IS NULL,NULL,'[原始内容未存入审计]'),'discovered_at',CAST(OLD.`discovered_at` AS CHAR)),NULL,JSON_ARRAY('tenant_id','instance_id','process_code','type','originator_dingtalk_id','originator_backend_id','provider_status','provider_result','match_status','match_reason','record_id','approval_record_id','author_id','payload_json','discovered_at'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_o_template_i_v1` AFTER INSERT ON `ops_oa_template` FOR EACH ROW
BEGIN
 IF NEW.domain='finance' THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (NEW.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_path,NULL),'permission',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'create'),'ops_oa_template',CONCAT_WS(':',CAST(NEW.`process_code` AS CHAR)),NULL,LEFT(COALESCE(CONCAT_WS(':',CAST(NEW.`process_code` AS CHAR)),''),255),NULL,NULL,NULL,JSON_OBJECT('process_code',CAST(NEW.`process_code` AS CHAR),'domain',CAST(NEW.`domain` AS CHAR),'template_name',roi_audit_text_v1(CAST(NEW.`template_name` AS CHAR)),'app_client_id',CAST(NEW.`app_client_id` AS CHAR),'corp_id',CAST(NEW.`corp_id` AS CHAR),'amount_field_id',CAST(NEW.`amount_field_id` AS CHAR),'source',CAST(NEW.`source` AS CHAR),'enabled',NEW.`enabled`,'verified_by',CAST(NEW.`verified_by` AS CHAR)),JSON_ARRAY('tenant_id','process_code','domain','template_name','app_client_id','corp_id','amount_field_id','source','enabled','verified_by'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_o_template_u_v1` AFTER UPDATE ON `ops_oa_template` FOR EACH ROW
BEGIN
 IF (OLD.domain='finance' OR NEW.domain='finance') AND (NOT(OLD.`tenant_id` <=> NEW.`tenant_id`) OR NOT(OLD.`process_code` <=> NEW.`process_code`) OR NOT(OLD.`domain` <=> NEW.`domain`) OR NOT(OLD.`template_name` <=> NEW.`template_name`) OR NOT(OLD.`app_client_id` <=> NEW.`app_client_id`) OR NOT(OLD.`corp_id` <=> NEW.`corp_id`) OR NOT(OLD.`amount_field_id` <=> NEW.`amount_field_id`) OR NOT(OLD.`source` <=> NEW.`source`) OR NOT(OLD.`enabled` <=> NEW.`enabled`) OR NOT(OLD.`verified_by` <=> NEW.`verified_by`)) THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (NEW.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_path,NULL),'permission',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'update'),'ops_oa_template',CONCAT_WS(':',CAST(NEW.`process_code` AS CHAR)),NULL,LEFT(COALESCE(CONCAT_WS(':',CAST(NEW.`process_code` AS CHAR)),''),255),NULL,NULL,JSON_OBJECT('process_code',CAST(OLD.`process_code` AS CHAR),'domain',CAST(OLD.`domain` AS CHAR),'template_name',roi_audit_text_v1(CAST(OLD.`template_name` AS CHAR)),'app_client_id',CAST(OLD.`app_client_id` AS CHAR),'corp_id',CAST(OLD.`corp_id` AS CHAR),'amount_field_id',CAST(OLD.`amount_field_id` AS CHAR),'source',CAST(OLD.`source` AS CHAR),'enabled',OLD.`enabled`,'verified_by',CAST(OLD.`verified_by` AS CHAR)),JSON_OBJECT('process_code',CAST(NEW.`process_code` AS CHAR),'domain',CAST(NEW.`domain` AS CHAR),'template_name',roi_audit_text_v1(CAST(NEW.`template_name` AS CHAR)),'app_client_id',CAST(NEW.`app_client_id` AS CHAR),'corp_id',CAST(NEW.`corp_id` AS CHAR),'amount_field_id',CAST(NEW.`amount_field_id` AS CHAR),'source',CAST(NEW.`source` AS CHAR),'enabled',NEW.`enabled`,'verified_by',CAST(NEW.`verified_by` AS CHAR)),JSON_MERGE_PRESERVE(IF(NOT(OLD.`tenant_id` <=> NEW.`tenant_id`),JSON_ARRAY('tenant_id'),JSON_ARRAY()),IF(NOT(OLD.`process_code` <=> NEW.`process_code`),JSON_ARRAY('process_code'),JSON_ARRAY()),IF(NOT(OLD.`domain` <=> NEW.`domain`),JSON_ARRAY('domain'),JSON_ARRAY()),IF(NOT(OLD.`template_name` <=> NEW.`template_name`),JSON_ARRAY('template_name'),JSON_ARRAY()),IF(NOT(OLD.`app_client_id` <=> NEW.`app_client_id`),JSON_ARRAY('app_client_id'),JSON_ARRAY()),IF(NOT(OLD.`corp_id` <=> NEW.`corp_id`),JSON_ARRAY('corp_id'),JSON_ARRAY()),IF(NOT(OLD.`amount_field_id` <=> NEW.`amount_field_id`),JSON_ARRAY('amount_field_id'),JSON_ARRAY()),IF(NOT(OLD.`source` <=> NEW.`source`),JSON_ARRAY('source'),JSON_ARRAY()),IF(NOT(OLD.`enabled` <=> NEW.`enabled`),JSON_ARRAY('enabled'),JSON_ARRAY()),IF(NOT(OLD.`verified_by` <=> NEW.`verified_by`),JSON_ARRAY('verified_by'),JSON_ARRAY())));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_o_template_d_v1` AFTER DELETE ON `ops_oa_template` FOR EACH ROW
BEGIN
 IF OLD.domain='finance' THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (OLD.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_path,NULL),'permission',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'delete'),'ops_oa_template',CONCAT_WS(':',CAST(OLD.`process_code` AS CHAR)),NULL,LEFT(COALESCE(CONCAT_WS(':',CAST(OLD.`process_code` AS CHAR)),''),255),NULL,NULL,JSON_OBJECT('process_code',CAST(OLD.`process_code` AS CHAR),'domain',CAST(OLD.`domain` AS CHAR),'template_name',roi_audit_text_v1(CAST(OLD.`template_name` AS CHAR)),'app_client_id',CAST(OLD.`app_client_id` AS CHAR),'corp_id',CAST(OLD.`corp_id` AS CHAR),'amount_field_id',CAST(OLD.`amount_field_id` AS CHAR),'source',CAST(OLD.`source` AS CHAR),'enabled',OLD.`enabled`,'verified_by',CAST(OLD.`verified_by` AS CHAR)),NULL,JSON_ARRAY('tenant_id','process_code','domain','template_name','app_client_id','corp_id','amount_field_id','source','enabled','verified_by'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_r_process_i_v1` AFTER INSERT ON `roi_process` FOR EACH ROW
BEGIN
 IF 1=1 THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (NEW.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_path,NULL),'permission',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'create'),'roi_process',CONCAT_WS(':',CAST(NEW.`type` AS CHAR)),NULL,LEFT(COALESCE(CONCAT_WS(':',CAST(NEW.`type` AS CHAR)),''),255),NULL,NULL,NULL,JSON_OBJECT('type',CAST(NEW.`type` AS CHAR),'name',roi_audit_text_v1(CAST(NEW.`name` AS CHAR)),'process_code',CAST(NEW.`process_code` AS CHAR),'field_map',IF(NEW.`field_map` IS NULL,NULL,'[已脱敏]'),'enabled',NEW.`enabled`),JSON_ARRAY('tenant_id','type','name','process_code','field_map','enabled'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_r_process_u_v1` AFTER UPDATE ON `roi_process` FOR EACH ROW
BEGIN
 IF (1=1 OR 1=1) AND (NOT(OLD.`tenant_id` <=> NEW.`tenant_id`) OR NOT(OLD.`type` <=> NEW.`type`) OR NOT(OLD.`name` <=> NEW.`name`) OR NOT(OLD.`process_code` <=> NEW.`process_code`) OR NOT(OLD.`field_map` <=> NEW.`field_map`) OR NOT(OLD.`enabled` <=> NEW.`enabled`)) THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (NEW.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_path,NULL),'permission',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'update'),'roi_process',CONCAT_WS(':',CAST(NEW.`type` AS CHAR)),NULL,LEFT(COALESCE(CONCAT_WS(':',CAST(NEW.`type` AS CHAR)),''),255),NULL,NULL,JSON_OBJECT('type',CAST(OLD.`type` AS CHAR),'name',roi_audit_text_v1(CAST(OLD.`name` AS CHAR)),'process_code',CAST(OLD.`process_code` AS CHAR),'field_map',IF(OLD.`field_map` IS NULL,NULL,'[已脱敏]'),'enabled',OLD.`enabled`),JSON_OBJECT('type',CAST(NEW.`type` AS CHAR),'name',roi_audit_text_v1(CAST(NEW.`name` AS CHAR)),'process_code',CAST(NEW.`process_code` AS CHAR),'field_map',IF(NEW.`field_map` IS NULL,NULL,'[已脱敏]'),'enabled',NEW.`enabled`),JSON_MERGE_PRESERVE(IF(NOT(OLD.`tenant_id` <=> NEW.`tenant_id`),JSON_ARRAY('tenant_id'),JSON_ARRAY()),IF(NOT(OLD.`type` <=> NEW.`type`),JSON_ARRAY('type'),JSON_ARRAY()),IF(NOT(OLD.`name` <=> NEW.`name`),JSON_ARRAY('name'),JSON_ARRAY()),IF(NOT(OLD.`process_code` <=> NEW.`process_code`),JSON_ARRAY('process_code'),JSON_ARRAY()),IF(NOT(OLD.`field_map` <=> NEW.`field_map`),JSON_ARRAY('field_map'),JSON_ARRAY()),IF(NOT(OLD.`enabled` <=> NEW.`enabled`),JSON_ARRAY('enabled'),JSON_ARRAY())));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_r_process_d_v1` AFTER DELETE ON `roi_process` FOR EACH ROW
BEGIN
 IF 1=1 THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (OLD.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_path,NULL),'permission',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'delete'),'roi_process',CONCAT_WS(':',CAST(OLD.`type` AS CHAR)),NULL,LEFT(COALESCE(CONCAT_WS(':',CAST(OLD.`type` AS CHAR)),''),255),NULL,NULL,JSON_OBJECT('type',CAST(OLD.`type` AS CHAR),'name',roi_audit_text_v1(CAST(OLD.`name` AS CHAR)),'process_code',CAST(OLD.`process_code` AS CHAR),'field_map',IF(OLD.`field_map` IS NULL,NULL,'[已脱敏]'),'enabled',OLD.`enabled`),NULL,JSON_ARRAY('tenant_id','type','name','process_code','field_map','enabled'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_o_instance_i_v1` AFTER INSERT ON `ops_oa_instance` FOR EACH ROW
BEGIN
 IF NEW.domain='finance' THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (NEW.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_path,NULL),'approval',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'create'),'ops_oa_instance',CAST(NEW.`id` AS CHAR),NEW.roi_record_id,LEFT(COALESCE(COALESCE(NULLIF(NEW.business_id,''),NEW.instance_id),''),255),(SELECT CAST(r.author_id AS CHAR) FROM roi_record r WHERE r.tenant_id=NEW.tenant_id AND r.id=NEW.roi_record_id LIMIT 1),NULL,NULL,JSON_OBJECT('id',CAST(NEW.`id` AS CHAR),'domain',CAST(NEW.`domain` AS CHAR),'instance_id',CAST(NEW.`instance_id` AS CHAR),'business_id',CAST(NEW.`business_id` AS CHAR),'process_code',CAST(NEW.`process_code` AS CHAR),'template_name',roi_audit_text_v1(CAST(NEW.`template_name` AS CHAR)),'title',IF(NEW.`title` IS NULL,NULL,'[自由文本原文未存入审计]'),'originator_dingtalk_id',IF(NEW.`originator_dingtalk_id` IS NULL,NULL,'[已脱敏]'),'provider_status',CAST(NEW.`provider_status` AS CHAR),'provider_result',CAST(NEW.`provider_result` AS CHAR),'form_amount',CAST(NEW.`form_amount` AS CHAR),'amount_state',CAST(NEW.`amount_state` AS CHAR),'provider_created_raw',CAST(NEW.`provider_created_raw` AS CHAR),'provider_created_date',CAST(NEW.`provider_created_date` AS CHAR),'payload_json',IF(NEW.`payload_json` IS NULL,NULL,'[原始内容未存入审计]'),'participants_json',IF(NEW.`participants_json` IS NULL,NULL,'[已脱敏]'),'association_state',CAST(NEW.`association_state` AS CHAR),'roi_record_id',CAST(NEW.`roi_record_id` AS CHAR),'source',CAST(NEW.`source` AS CHAR),'error_code',CAST(NEW.`error_code` AS CHAR)),JSON_ARRAY('id','tenant_id','domain','instance_id','business_id','process_code','template_name','title','originator_dingtalk_id','provider_status','provider_result','form_amount','amount_state','provider_created_raw','provider_created_date','association_state','roi_record_id','source'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_o_instance_u_v1` AFTER UPDATE ON `ops_oa_instance` FOR EACH ROW
BEGIN
 IF (OLD.domain='finance' OR NEW.domain='finance') AND (NOT(OLD.`id` <=> NEW.`id`) OR NOT(OLD.`tenant_id` <=> NEW.`tenant_id`) OR NOT(OLD.`domain` <=> NEW.`domain`) OR NOT(OLD.`instance_id` <=> NEW.`instance_id`) OR NOT(OLD.`business_id` <=> NEW.`business_id`) OR NOT(OLD.`process_code` <=> NEW.`process_code`) OR NOT(OLD.`template_name` <=> NEW.`template_name`) OR NOT(OLD.`title` <=> NEW.`title`) OR NOT(OLD.`originator_dingtalk_id` <=> NEW.`originator_dingtalk_id`) OR NOT(OLD.`provider_status` <=> NEW.`provider_status`) OR NOT(OLD.`provider_result` <=> NEW.`provider_result`) OR NOT(OLD.`form_amount` <=> NEW.`form_amount`) OR NOT(OLD.`amount_state` <=> NEW.`amount_state`) OR NOT(OLD.`provider_created_raw` <=> NEW.`provider_created_raw`) OR NOT(OLD.`provider_created_date` <=> NEW.`provider_created_date`) OR NOT(OLD.`association_state` <=> NEW.`association_state`) OR NOT(OLD.`roi_record_id` <=> NEW.`roi_record_id`) OR NOT(OLD.`source` <=> NEW.`source`)) THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (NEW.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_path,NULL),'approval',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'update'),'ops_oa_instance',CAST(NEW.`id` AS CHAR),NEW.roi_record_id,LEFT(COALESCE(COALESCE(NULLIF(NEW.business_id,''),NEW.instance_id),''),255),(SELECT CAST(r.author_id AS CHAR) FROM roi_record r WHERE r.tenant_id=NEW.tenant_id AND r.id=NEW.roi_record_id LIMIT 1),(SELECT CAST(r.author_id AS CHAR) FROM roi_record r WHERE r.tenant_id=OLD.tenant_id AND r.id=OLD.roi_record_id LIMIT 1),JSON_OBJECT('id',CAST(OLD.`id` AS CHAR),'domain',CAST(OLD.`domain` AS CHAR),'instance_id',CAST(OLD.`instance_id` AS CHAR),'business_id',CAST(OLD.`business_id` AS CHAR),'process_code',CAST(OLD.`process_code` AS CHAR),'template_name',roi_audit_text_v1(CAST(OLD.`template_name` AS CHAR)),'title',IF(OLD.`title` IS NULL,NULL,'[自由文本原文未存入审计]'),'originator_dingtalk_id',IF(OLD.`originator_dingtalk_id` IS NULL,NULL,'[已脱敏]'),'provider_status',CAST(OLD.`provider_status` AS CHAR),'provider_result',CAST(OLD.`provider_result` AS CHAR),'form_amount',CAST(OLD.`form_amount` AS CHAR),'amount_state',CAST(OLD.`amount_state` AS CHAR),'provider_created_raw',CAST(OLD.`provider_created_raw` AS CHAR),'provider_created_date',CAST(OLD.`provider_created_date` AS CHAR),'payload_json',IF(OLD.`payload_json` IS NULL,NULL,'[原始内容未存入审计]'),'participants_json',IF(OLD.`participants_json` IS NULL,NULL,'[已脱敏]'),'association_state',CAST(OLD.`association_state` AS CHAR),'roi_record_id',CAST(OLD.`roi_record_id` AS CHAR),'source',CAST(OLD.`source` AS CHAR),'error_code',CAST(OLD.`error_code` AS CHAR)),JSON_OBJECT('id',CAST(NEW.`id` AS CHAR),'domain',CAST(NEW.`domain` AS CHAR),'instance_id',CAST(NEW.`instance_id` AS CHAR),'business_id',CAST(NEW.`business_id` AS CHAR),'process_code',CAST(NEW.`process_code` AS CHAR),'template_name',roi_audit_text_v1(CAST(NEW.`template_name` AS CHAR)),'title',IF(NEW.`title` IS NULL,NULL,'[自由文本原文未存入审计]'),'originator_dingtalk_id',IF(NEW.`originator_dingtalk_id` IS NULL,NULL,'[已脱敏]'),'provider_status',CAST(NEW.`provider_status` AS CHAR),'provider_result',CAST(NEW.`provider_result` AS CHAR),'form_amount',CAST(NEW.`form_amount` AS CHAR),'amount_state',CAST(NEW.`amount_state` AS CHAR),'provider_created_raw',CAST(NEW.`provider_created_raw` AS CHAR),'provider_created_date',CAST(NEW.`provider_created_date` AS CHAR),'payload_json',IF(NEW.`payload_json` IS NULL,NULL,'[原始内容未存入审计]'),'participants_json',IF(NEW.`participants_json` IS NULL,NULL,'[已脱敏]'),'association_state',CAST(NEW.`association_state` AS CHAR),'roi_record_id',CAST(NEW.`roi_record_id` AS CHAR),'source',CAST(NEW.`source` AS CHAR),'error_code',CAST(NEW.`error_code` AS CHAR)),JSON_MERGE_PRESERVE(IF(NOT(OLD.`id` <=> NEW.`id`),JSON_ARRAY('id'),JSON_ARRAY()),IF(NOT(OLD.`tenant_id` <=> NEW.`tenant_id`),JSON_ARRAY('tenant_id'),JSON_ARRAY()),IF(NOT(OLD.`domain` <=> NEW.`domain`),JSON_ARRAY('domain'),JSON_ARRAY()),IF(NOT(OLD.`instance_id` <=> NEW.`instance_id`),JSON_ARRAY('instance_id'),JSON_ARRAY()),IF(NOT(OLD.`business_id` <=> NEW.`business_id`),JSON_ARRAY('business_id'),JSON_ARRAY()),IF(NOT(OLD.`process_code` <=> NEW.`process_code`),JSON_ARRAY('process_code'),JSON_ARRAY()),IF(NOT(OLD.`template_name` <=> NEW.`template_name`),JSON_ARRAY('template_name'),JSON_ARRAY()),IF(NOT(OLD.`title` <=> NEW.`title`),JSON_ARRAY('title'),JSON_ARRAY()),IF(NOT(OLD.`originator_dingtalk_id` <=> NEW.`originator_dingtalk_id`),JSON_ARRAY('originator_dingtalk_id'),JSON_ARRAY()),IF(NOT(OLD.`provider_status` <=> NEW.`provider_status`),JSON_ARRAY('provider_status'),JSON_ARRAY()),IF(NOT(OLD.`provider_result` <=> NEW.`provider_result`),JSON_ARRAY('provider_result'),JSON_ARRAY()),IF(NOT(OLD.`form_amount` <=> NEW.`form_amount`),JSON_ARRAY('form_amount'),JSON_ARRAY()),IF(NOT(OLD.`amount_state` <=> NEW.`amount_state`),JSON_ARRAY('amount_state'),JSON_ARRAY()),IF(NOT(OLD.`provider_created_raw` <=> NEW.`provider_created_raw`),JSON_ARRAY('provider_created_raw'),JSON_ARRAY()),IF(NOT(OLD.`provider_created_date` <=> NEW.`provider_created_date`),JSON_ARRAY('provider_created_date'),JSON_ARRAY()),IF(NOT(OLD.`association_state` <=> NEW.`association_state`),JSON_ARRAY('association_state'),JSON_ARRAY()),IF(NOT(OLD.`roi_record_id` <=> NEW.`roi_record_id`),JSON_ARRAY('roi_record_id'),JSON_ARRAY()),IF(NOT(OLD.`source` <=> NEW.`source`),JSON_ARRAY('source'),JSON_ARRAY())));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_o_instance_d_v1` AFTER DELETE ON `ops_oa_instance` FOR EACH ROW
BEGIN
 IF OLD.domain='finance' THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (OLD.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_path,NULL),'approval',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'delete'),'ops_oa_instance',CAST(OLD.`id` AS CHAR),OLD.roi_record_id,LEFT(COALESCE(COALESCE(NULLIF(OLD.business_id,''),OLD.instance_id),''),255),(SELECT CAST(r.author_id AS CHAR) FROM roi_record r WHERE r.tenant_id=OLD.tenant_id AND r.id=OLD.roi_record_id LIMIT 1),(SELECT CAST(r.author_id AS CHAR) FROM roi_record r WHERE r.tenant_id=OLD.tenant_id AND r.id=OLD.roi_record_id LIMIT 1),JSON_OBJECT('id',CAST(OLD.`id` AS CHAR),'domain',CAST(OLD.`domain` AS CHAR),'instance_id',CAST(OLD.`instance_id` AS CHAR),'business_id',CAST(OLD.`business_id` AS CHAR),'process_code',CAST(OLD.`process_code` AS CHAR),'template_name',roi_audit_text_v1(CAST(OLD.`template_name` AS CHAR)),'title',IF(OLD.`title` IS NULL,NULL,'[自由文本原文未存入审计]'),'originator_dingtalk_id',IF(OLD.`originator_dingtalk_id` IS NULL,NULL,'[已脱敏]'),'provider_status',CAST(OLD.`provider_status` AS CHAR),'provider_result',CAST(OLD.`provider_result` AS CHAR),'form_amount',CAST(OLD.`form_amount` AS CHAR),'amount_state',CAST(OLD.`amount_state` AS CHAR),'provider_created_raw',CAST(OLD.`provider_created_raw` AS CHAR),'provider_created_date',CAST(OLD.`provider_created_date` AS CHAR),'payload_json',IF(OLD.`payload_json` IS NULL,NULL,'[原始内容未存入审计]'),'participants_json',IF(OLD.`participants_json` IS NULL,NULL,'[已脱敏]'),'association_state',CAST(OLD.`association_state` AS CHAR),'roi_record_id',CAST(OLD.`roi_record_id` AS CHAR),'source',CAST(OLD.`source` AS CHAR),'error_code',CAST(OLD.`error_code` AS CHAR)),NULL,JSON_ARRAY('id','tenant_id','domain','instance_id','business_id','process_code','template_name','title','originator_dingtalk_id','provider_status','provider_result','form_amount','amount_state','provider_created_raw','provider_created_date','association_state','roi_record_id','source'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_o_cost_candidate_i_v1` AFTER INSERT ON `ops_oa_cost_candidate` FOR EACH ROW
BEGIN
 IF 1=1 THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (NEW.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_path,NULL),'finance',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'create'),'ops_oa_cost_candidate',CAST(NEW.`id` AS CHAR),NEW.instance_id,LEFT(COALESCE(CONCAT(COALESCE(NEW.business_id,''),' / ',COALESCE(NEW.row_key,'')),''),255),NULLIF(JSON_UNQUOTE(JSON_EXTRACT(NEW.payload_json,'$.suggestedAuthorId')),''),NULL,NULL,JSON_OBJECT('id',CAST(NEW.`id` AS CHAR),'instance_id',CAST(NEW.`instance_id` AS CHAR),'business_id',CAST(NEW.`business_id` AS CHAR),'process_code',CAST(NEW.`process_code` AS CHAR),'source_key',CAST(NEW.`source_key` AS CHAR),'source_hash',CAST(NEW.`source_hash` AS CHAR),'row_key',CAST(NEW.`row_key` AS CHAR),'amount',NEW.`amount`,'cost_date',CAST(NEW.`cost_date` AS CHAR),'state',CAST(NEW.`state` AS CHAR),'version',NEW.`version`,'proposal',JSON_EXTRACT(roi_audit_project_v1(NEW.`payload_json`),'$'),'review',JSON_EXTRACT(roi_audit_project_v1(NEW.`review_json`),'$')),JSON_ARRAY('id','tenant_id','instance_id','business_id','process_code','source_key','source_hash','row_key','amount','cost_date','state','version','payload_json','review_json'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_o_cost_candidate_u_v1` AFTER UPDATE ON `ops_oa_cost_candidate` FOR EACH ROW
BEGIN
 IF (1=1 OR 1=1) AND (NOT(OLD.`id` <=> NEW.`id`) OR NOT(OLD.`tenant_id` <=> NEW.`tenant_id`) OR NOT(OLD.`instance_id` <=> NEW.`instance_id`) OR NOT(OLD.`business_id` <=> NEW.`business_id`) OR NOT(OLD.`process_code` <=> NEW.`process_code`) OR NOT(OLD.`source_key` <=> NEW.`source_key`) OR NOT(OLD.`source_hash` <=> NEW.`source_hash`) OR NOT(OLD.`row_key` <=> NEW.`row_key`) OR NOT(OLD.`amount` <=> NEW.`amount`) OR NOT(OLD.`cost_date` <=> NEW.`cost_date`) OR NOT(OLD.`state` <=> NEW.`state`) OR NOT(OLD.`version` <=> NEW.`version`) OR NOT(OLD.`payload_json` <=> NEW.`payload_json`) OR NOT(OLD.`review_json` <=> NEW.`review_json`)) THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (NEW.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_path,NULL),'finance',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'update'),'ops_oa_cost_candidate',CAST(NEW.`id` AS CHAR),NEW.instance_id,LEFT(COALESCE(CONCAT(COALESCE(NEW.business_id,''),' / ',COALESCE(NEW.row_key,'')),''),255),NULLIF(JSON_UNQUOTE(JSON_EXTRACT(NEW.payload_json,'$.suggestedAuthorId')),''),NULLIF(JSON_UNQUOTE(JSON_EXTRACT(OLD.payload_json,'$.suggestedAuthorId')),''),JSON_OBJECT('id',CAST(OLD.`id` AS CHAR),'instance_id',CAST(OLD.`instance_id` AS CHAR),'business_id',CAST(OLD.`business_id` AS CHAR),'process_code',CAST(OLD.`process_code` AS CHAR),'source_key',CAST(OLD.`source_key` AS CHAR),'source_hash',CAST(OLD.`source_hash` AS CHAR),'row_key',CAST(OLD.`row_key` AS CHAR),'amount',OLD.`amount`,'cost_date',CAST(OLD.`cost_date` AS CHAR),'state',CAST(OLD.`state` AS CHAR),'version',OLD.`version`,'proposal',JSON_EXTRACT(roi_audit_project_v1(OLD.`payload_json`),'$'),'review',JSON_EXTRACT(roi_audit_project_v1(OLD.`review_json`),'$')),JSON_OBJECT('id',CAST(NEW.`id` AS CHAR),'instance_id',CAST(NEW.`instance_id` AS CHAR),'business_id',CAST(NEW.`business_id` AS CHAR),'process_code',CAST(NEW.`process_code` AS CHAR),'source_key',CAST(NEW.`source_key` AS CHAR),'source_hash',CAST(NEW.`source_hash` AS CHAR),'row_key',CAST(NEW.`row_key` AS CHAR),'amount',NEW.`amount`,'cost_date',CAST(NEW.`cost_date` AS CHAR),'state',CAST(NEW.`state` AS CHAR),'version',NEW.`version`,'proposal',JSON_EXTRACT(roi_audit_project_v1(NEW.`payload_json`),'$'),'review',JSON_EXTRACT(roi_audit_project_v1(NEW.`review_json`),'$')),JSON_MERGE_PRESERVE(IF(NOT(OLD.`id` <=> NEW.`id`),JSON_ARRAY('id'),JSON_ARRAY()),IF(NOT(OLD.`tenant_id` <=> NEW.`tenant_id`),JSON_ARRAY('tenant_id'),JSON_ARRAY()),IF(NOT(OLD.`instance_id` <=> NEW.`instance_id`),JSON_ARRAY('instance_id'),JSON_ARRAY()),IF(NOT(OLD.`business_id` <=> NEW.`business_id`),JSON_ARRAY('business_id'),JSON_ARRAY()),IF(NOT(OLD.`process_code` <=> NEW.`process_code`),JSON_ARRAY('process_code'),JSON_ARRAY()),IF(NOT(OLD.`source_key` <=> NEW.`source_key`),JSON_ARRAY('source_key'),JSON_ARRAY()),IF(NOT(OLD.`source_hash` <=> NEW.`source_hash`),JSON_ARRAY('source_hash'),JSON_ARRAY()),IF(NOT(OLD.`row_key` <=> NEW.`row_key`),JSON_ARRAY('row_key'),JSON_ARRAY()),IF(NOT(OLD.`amount` <=> NEW.`amount`),JSON_ARRAY('amount'),JSON_ARRAY()),IF(NOT(OLD.`cost_date` <=> NEW.`cost_date`),JSON_ARRAY('cost_date'),JSON_ARRAY()),IF(NOT(OLD.`state` <=> NEW.`state`),JSON_ARRAY('state'),JSON_ARRAY()),IF(NOT(OLD.`version` <=> NEW.`version`),JSON_ARRAY('version'),JSON_ARRAY()),IF(NOT(OLD.`payload_json` <=> NEW.`payload_json`),JSON_ARRAY('payload_json'),JSON_ARRAY()),IF(NOT(OLD.`review_json` <=> NEW.`review_json`),JSON_ARRAY('review_json'),JSON_ARRAY())));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_o_cost_candidate_d_v1` AFTER DELETE ON `ops_oa_cost_candidate` FOR EACH ROW
BEGIN
 IF 1=1 THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (OLD.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_path,NULL),'finance',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'delete'),'ops_oa_cost_candidate',CAST(OLD.`id` AS CHAR),OLD.instance_id,LEFT(COALESCE(CONCAT(COALESCE(OLD.business_id,''),' / ',COALESCE(OLD.row_key,'')),''),255),NULLIF(JSON_UNQUOTE(JSON_EXTRACT(OLD.payload_json,'$.suggestedAuthorId')),''),NULLIF(JSON_UNQUOTE(JSON_EXTRACT(OLD.payload_json,'$.suggestedAuthorId')),''),JSON_OBJECT('id',CAST(OLD.`id` AS CHAR),'instance_id',CAST(OLD.`instance_id` AS CHAR),'business_id',CAST(OLD.`business_id` AS CHAR),'process_code',CAST(OLD.`process_code` AS CHAR),'source_key',CAST(OLD.`source_key` AS CHAR),'source_hash',CAST(OLD.`source_hash` AS CHAR),'row_key',CAST(OLD.`row_key` AS CHAR),'amount',OLD.`amount`,'cost_date',CAST(OLD.`cost_date` AS CHAR),'state',CAST(OLD.`state` AS CHAR),'version',OLD.`version`,'proposal',JSON_EXTRACT(roi_audit_project_v1(OLD.`payload_json`),'$'),'review',JSON_EXTRACT(roi_audit_project_v1(OLD.`review_json`),'$')),NULL,JSON_ARRAY('id','tenant_id','instance_id','business_id','process_code','source_key','source_hash','row_key','amount','cost_date','state','version','payload_json','review_json'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_o_account_cost_link_i_v1` AFTER INSERT ON `ops_oa_account_cost_link` FOR EACH ROW
BEGIN
 IF 1=1 THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (NEW.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_path,NULL),'finance',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'create'),'ops_oa_account_cost_link',CONCAT_WS(':',CAST(NEW.`candidate_id` AS CHAR),CAST(NEW.`cost_id` AS CHAR)),NEW.candidate_id,LEFT(COALESCE(CONCAT_WS(':',CAST(NEW.`candidate_id` AS CHAR),CAST(NEW.`cost_id` AS CHAR)),''),255),(SELECT CAST(c.author_id AS CHAR) FROM oa_account_cost c WHERE c.tenant_id=NEW.tenant_id AND c.id=NEW.cost_id LIMIT 1),NULL,NULL,JSON_OBJECT('candidate_id',CAST(NEW.`candidate_id` AS CHAR),'cost_id',CAST(NEW.`cost_id` AS CHAR),'allocation_index',NEW.`allocation_index`,'link_type',CAST(NEW.`link_type` AS CHAR),'amount',NEW.`amount`),JSON_ARRAY('tenant_id','candidate_id','cost_id','allocation_index','link_type','amount'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_o_account_cost_link_u_v1` AFTER UPDATE ON `ops_oa_account_cost_link` FOR EACH ROW
BEGIN
 IF (1=1 OR 1=1) AND (NOT(OLD.`tenant_id` <=> NEW.`tenant_id`) OR NOT(OLD.`candidate_id` <=> NEW.`candidate_id`) OR NOT(OLD.`cost_id` <=> NEW.`cost_id`) OR NOT(OLD.`allocation_index` <=> NEW.`allocation_index`) OR NOT(OLD.`link_type` <=> NEW.`link_type`) OR NOT(OLD.`amount` <=> NEW.`amount`)) THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (NEW.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_path,NULL),'finance',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'update'),'ops_oa_account_cost_link',CONCAT_WS(':',CAST(NEW.`candidate_id` AS CHAR),CAST(NEW.`cost_id` AS CHAR)),NEW.candidate_id,LEFT(COALESCE(CONCAT_WS(':',CAST(NEW.`candidate_id` AS CHAR),CAST(NEW.`cost_id` AS CHAR)),''),255),(SELECT CAST(c.author_id AS CHAR) FROM oa_account_cost c WHERE c.tenant_id=NEW.tenant_id AND c.id=NEW.cost_id LIMIT 1),(SELECT CAST(c.author_id AS CHAR) FROM oa_account_cost c WHERE c.tenant_id=OLD.tenant_id AND c.id=OLD.cost_id LIMIT 1),JSON_OBJECT('candidate_id',CAST(OLD.`candidate_id` AS CHAR),'cost_id',CAST(OLD.`cost_id` AS CHAR),'allocation_index',OLD.`allocation_index`,'link_type',CAST(OLD.`link_type` AS CHAR),'amount',OLD.`amount`),JSON_OBJECT('candidate_id',CAST(NEW.`candidate_id` AS CHAR),'cost_id',CAST(NEW.`cost_id` AS CHAR),'allocation_index',NEW.`allocation_index`,'link_type',CAST(NEW.`link_type` AS CHAR),'amount',NEW.`amount`),JSON_MERGE_PRESERVE(IF(NOT(OLD.`tenant_id` <=> NEW.`tenant_id`),JSON_ARRAY('tenant_id'),JSON_ARRAY()),IF(NOT(OLD.`candidate_id` <=> NEW.`candidate_id`),JSON_ARRAY('candidate_id'),JSON_ARRAY()),IF(NOT(OLD.`cost_id` <=> NEW.`cost_id`),JSON_ARRAY('cost_id'),JSON_ARRAY()),IF(NOT(OLD.`allocation_index` <=> NEW.`allocation_index`),JSON_ARRAY('allocation_index'),JSON_ARRAY()),IF(NOT(OLD.`link_type` <=> NEW.`link_type`),JSON_ARRAY('link_type'),JSON_ARRAY()),IF(NOT(OLD.`amount` <=> NEW.`amount`),JSON_ARRAY('amount'),JSON_ARRAY())));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_o_account_cost_link_d_v1` AFTER DELETE ON `ops_oa_account_cost_link` FOR EACH ROW
BEGIN
 IF 1=1 THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (OLD.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_path,NULL),'finance',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'delete'),'ops_oa_account_cost_link',CONCAT_WS(':',CAST(OLD.`candidate_id` AS CHAR),CAST(OLD.`cost_id` AS CHAR)),OLD.candidate_id,LEFT(COALESCE(CONCAT_WS(':',CAST(OLD.`candidate_id` AS CHAR),CAST(OLD.`cost_id` AS CHAR)),''),255),(SELECT CAST(c.author_id AS CHAR) FROM oa_account_cost c WHERE c.tenant_id=OLD.tenant_id AND c.id=OLD.cost_id LIMIT 1),(SELECT CAST(c.author_id AS CHAR) FROM oa_account_cost c WHERE c.tenant_id=OLD.tenant_id AND c.id=OLD.cost_id LIMIT 1),JSON_OBJECT('candidate_id',CAST(OLD.`candidate_id` AS CHAR),'cost_id',CAST(OLD.`cost_id` AS CHAR),'allocation_index',OLD.`allocation_index`,'link_type',CAST(OLD.`link_type` AS CHAR),'amount',OLD.`amount`),NULL,JSON_ARRAY('tenant_id','candidate_id','cost_id','allocation_index','link_type','amount'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_o_cost_alias_i_v1` AFTER INSERT ON `ops_oa_cost_alias` FOR EACH ROW
BEGIN
 IF 1=1 THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (NEW.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_path,NULL),'finance',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'create'),'ops_oa_cost_alias',CONCAT_WS(':',CAST(NEW.`alias` AS CHAR)),NULL,LEFT(COALESCE(CONCAT_WS(':',CAST(NEW.`alias` AS CHAR)),''),255),NULL,NULL,NULL,JSON_OBJECT('alias',roi_audit_text_v1(CAST(NEW.`alias` AS CHAR)),'account_id',CAST(NEW.`account_id` AS CHAR)),JSON_ARRAY('tenant_id','alias','account_id'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_o_cost_alias_u_v1` AFTER UPDATE ON `ops_oa_cost_alias` FOR EACH ROW
BEGIN
 IF (1=1 OR 1=1) AND (NOT(OLD.`tenant_id` <=> NEW.`tenant_id`) OR NOT(OLD.`alias` <=> NEW.`alias`) OR NOT(OLD.`account_id` <=> NEW.`account_id`)) THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (NEW.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_path,NULL),'finance',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'update'),'ops_oa_cost_alias',CONCAT_WS(':',CAST(NEW.`alias` AS CHAR)),NULL,LEFT(COALESCE(CONCAT_WS(':',CAST(NEW.`alias` AS CHAR)),''),255),NULL,NULL,JSON_OBJECT('alias',roi_audit_text_v1(CAST(OLD.`alias` AS CHAR)),'account_id',CAST(OLD.`account_id` AS CHAR)),JSON_OBJECT('alias',roi_audit_text_v1(CAST(NEW.`alias` AS CHAR)),'account_id',CAST(NEW.`account_id` AS CHAR)),JSON_MERGE_PRESERVE(IF(NOT(OLD.`tenant_id` <=> NEW.`tenant_id`),JSON_ARRAY('tenant_id'),JSON_ARRAY()),IF(NOT(OLD.`alias` <=> NEW.`alias`),JSON_ARRAY('alias'),JSON_ARRAY()),IF(NOT(OLD.`account_id` <=> NEW.`account_id`),JSON_ARRAY('account_id'),JSON_ARRAY())));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_o_cost_alias_d_v1` AFTER DELETE ON `ops_oa_cost_alias` FOR EACH ROW
BEGIN
 IF 1=1 THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (OLD.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_path,NULL),'finance',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'delete'),'ops_oa_cost_alias',CONCAT_WS(':',CAST(OLD.`alias` AS CHAR)),NULL,LEFT(COALESCE(CONCAT_WS(':',CAST(OLD.`alias` AS CHAR)),''),255),NULL,NULL,JSON_OBJECT('alias',roi_audit_text_v1(CAST(OLD.`alias` AS CHAR)),'account_id',CAST(OLD.`account_id` AS CHAR)),NULL,JSON_ARRAY('tenant_id','alias','account_id'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_o_cost_link_i_v1` AFTER INSERT ON `ops_oa_cost_link` FOR EACH ROW
BEGIN
 IF 1=1 THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (NEW.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_path,NULL),'finance',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'create'),'ops_oa_cost_link',CONCAT_WS(':',CAST(NEW.`oa_id` AS CHAR),CAST(NEW.`line_key` AS CHAR)),NEW.oa_id,LEFT(COALESCE(CONCAT_WS(':',CAST(NEW.`oa_id` AS CHAR),CAST(NEW.`line_key` AS CHAR)),''),255),CAST(NEW.anchor_id AS CHAR),NULL,NULL,JSON_OBJECT('oa_id',CAST(NEW.`oa_id` AS CHAR),'line_key',CAST(NEW.`line_key` AS CHAR),'anchor_id',CAST(NEW.`anchor_id` AS CHAR),'category',CAST(NEW.`category` AS CHAR),'project',roi_audit_text_v1(CAST(NEW.`project` AS CHAR)),'session_id',CAST(NEW.`session_id` AS CHAR),'account_id',CAST(NEW.`account_id` AS CHAR),'allocation_amount',NEW.`allocation_amount`),JSON_ARRAY('tenant_id','oa_id','line_key','anchor_id','category','project','session_id','account_id','allocation_amount'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_o_cost_link_u_v1` AFTER UPDATE ON `ops_oa_cost_link` FOR EACH ROW
BEGIN
 IF (1=1 OR 1=1) AND (NOT(OLD.`tenant_id` <=> NEW.`tenant_id`) OR NOT(OLD.`oa_id` <=> NEW.`oa_id`) OR NOT(OLD.`line_key` <=> NEW.`line_key`) OR NOT(OLD.`anchor_id` <=> NEW.`anchor_id`) OR NOT(OLD.`category` <=> NEW.`category`) OR NOT(OLD.`project` <=> NEW.`project`) OR NOT(OLD.`session_id` <=> NEW.`session_id`) OR NOT(OLD.`account_id` <=> NEW.`account_id`) OR NOT(OLD.`allocation_amount` <=> NEW.`allocation_amount`)) THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (NEW.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_path,NULL),'finance',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'update'),'ops_oa_cost_link',CONCAT_WS(':',CAST(NEW.`oa_id` AS CHAR),CAST(NEW.`line_key` AS CHAR)),NEW.oa_id,LEFT(COALESCE(CONCAT_WS(':',CAST(NEW.`oa_id` AS CHAR),CAST(NEW.`line_key` AS CHAR)),''),255),CAST(NEW.anchor_id AS CHAR),CAST(OLD.anchor_id AS CHAR),JSON_OBJECT('oa_id',CAST(OLD.`oa_id` AS CHAR),'line_key',CAST(OLD.`line_key` AS CHAR),'anchor_id',CAST(OLD.`anchor_id` AS CHAR),'category',CAST(OLD.`category` AS CHAR),'project',roi_audit_text_v1(CAST(OLD.`project` AS CHAR)),'session_id',CAST(OLD.`session_id` AS CHAR),'account_id',CAST(OLD.`account_id` AS CHAR),'allocation_amount',OLD.`allocation_amount`),JSON_OBJECT('oa_id',CAST(NEW.`oa_id` AS CHAR),'line_key',CAST(NEW.`line_key` AS CHAR),'anchor_id',CAST(NEW.`anchor_id` AS CHAR),'category',CAST(NEW.`category` AS CHAR),'project',roi_audit_text_v1(CAST(NEW.`project` AS CHAR)),'session_id',CAST(NEW.`session_id` AS CHAR),'account_id',CAST(NEW.`account_id` AS CHAR),'allocation_amount',NEW.`allocation_amount`),JSON_MERGE_PRESERVE(IF(NOT(OLD.`tenant_id` <=> NEW.`tenant_id`),JSON_ARRAY('tenant_id'),JSON_ARRAY()),IF(NOT(OLD.`oa_id` <=> NEW.`oa_id`),JSON_ARRAY('oa_id'),JSON_ARRAY()),IF(NOT(OLD.`line_key` <=> NEW.`line_key`),JSON_ARRAY('line_key'),JSON_ARRAY()),IF(NOT(OLD.`anchor_id` <=> NEW.`anchor_id`),JSON_ARRAY('anchor_id'),JSON_ARRAY()),IF(NOT(OLD.`category` <=> NEW.`category`),JSON_ARRAY('category'),JSON_ARRAY()),IF(NOT(OLD.`project` <=> NEW.`project`),JSON_ARRAY('project'),JSON_ARRAY()),IF(NOT(OLD.`session_id` <=> NEW.`session_id`),JSON_ARRAY('session_id'),JSON_ARRAY()),IF(NOT(OLD.`account_id` <=> NEW.`account_id`),JSON_ARRAY('account_id'),JSON_ARRAY()),IF(NOT(OLD.`allocation_amount` <=> NEW.`allocation_amount`),JSON_ARRAY('allocation_amount'),JSON_ARRAY())));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_o_cost_link_d_v1` AFTER DELETE ON `ops_oa_cost_link` FOR EACH ROW
BEGIN
 IF 1=1 THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (OLD.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_path,NULL),'finance',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'delete'),'ops_oa_cost_link',CONCAT_WS(':',CAST(OLD.`oa_id` AS CHAR),CAST(OLD.`line_key` AS CHAR)),OLD.oa_id,LEFT(COALESCE(CONCAT_WS(':',CAST(OLD.`oa_id` AS CHAR),CAST(OLD.`line_key` AS CHAR)),''),255),CAST(OLD.anchor_id AS CHAR),CAST(OLD.anchor_id AS CHAR),JSON_OBJECT('oa_id',CAST(OLD.`oa_id` AS CHAR),'line_key',CAST(OLD.`line_key` AS CHAR),'anchor_id',CAST(OLD.`anchor_id` AS CHAR),'category',CAST(OLD.`category` AS CHAR),'project',roi_audit_text_v1(CAST(OLD.`project` AS CHAR)),'session_id',CAST(OLD.`session_id` AS CHAR),'account_id',CAST(OLD.`account_id` AS CHAR),'allocation_amount',OLD.`allocation_amount`),NULL,JSON_ARRAY('tenant_id','oa_id','line_key','anchor_id','category','project','session_id','account_id','allocation_amount'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_oa_account_cost_i_v1` AFTER INSERT ON `oa_account_cost` FOR EACH ROW
BEGIN
 IF (NEW.source_type='OA' OR EXISTS(SELECT 1 FROM roi_record r WHERE r.tenant_id=NEW.tenant_id AND r.kind='account' AND JSON_UNQUOTE(JSON_EXTRACT(r.payload,'$.masterAccountId'))=CAST(NEW.account_id AS CHAR))) THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (NEW.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_path,NULL),'finance',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'create'),'oa_account_cost',CAST(NEW.`id` AS CHAR),NEW.source_candidate_id,LEFT(COALESCE(CONCAT('账号成本 ',NEW.id),''),255),CAST(NEW.author_id AS CHAR),NULL,NULL,JSON_OBJECT('id',CAST(NEW.`id` AS CHAR),'account_id',CAST(NEW.`account_id` AS CHAR),'cost_type',CAST(NEW.`cost_type` AS CHAR),'amount',NEW.`amount`,'pay_method',CAST(NEW.`pay_method` AS CHAR),'pay_date',CAST(NEW.`pay_date` AS CHAR),'period',CAST(NEW.`period` AS CHAR),'remark',IF(NEW.`remark` IS NULL,NULL,'[自由文本原文未存入审计]'),'attachment_id',CAST(NEW.`attachment_id` AS CHAR),'deleted',NEW.`deleted`,'cost_date',CAST(NEW.`cost_date` AS CHAR),'source_type',CAST(NEW.`source_type` AS CHAR),'source_key',CAST(NEW.`source_key` AS CHAR),'source_candidate_id',CAST(NEW.`source_candidate_id` AS CHAR),'approval_business_id',CAST(NEW.`approval_business_id` AS CHAR),'approval_instance_id',CAST(NEW.`approval_instance_id` AS CHAR),'author_id',CAST(NEW.`author_id` AS CHAR),'attribution_ip_group_id',CAST(NEW.`attribution_ip_group_id` AS CHAR),'payment_state',CAST(NEW.`payment_state` AS CHAR),'paid_reference',IF(NEW.`paid_reference` IS NULL,NULL,'[已脱敏]'),'payment_note',IF(NEW.`payment_note` IS NULL,NULL,'[自由文本原文未存入审计]')),JSON_ARRAY('id','tenant_id','account_id','cost_type','amount','pay_method','pay_date','period','remark','attachment_id','deleted','cost_date','source_type','source_key','source_candidate_id','approval_business_id','approval_instance_id','author_id','attribution_ip_group_id','payment_state','paid_reference','payment_note'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_oa_account_cost_u_v1` AFTER UPDATE ON `oa_account_cost` FOR EACH ROW
BEGIN
 IF ((OLD.source_type='OA' OR EXISTS(SELECT 1 FROM roi_record r WHERE r.tenant_id=OLD.tenant_id AND r.kind='account' AND JSON_UNQUOTE(JSON_EXTRACT(r.payload,'$.masterAccountId'))=CAST(OLD.account_id AS CHAR))) OR (NEW.source_type='OA' OR EXISTS(SELECT 1 FROM roi_record r WHERE r.tenant_id=NEW.tenant_id AND r.kind='account' AND JSON_UNQUOTE(JSON_EXTRACT(r.payload,'$.masterAccountId'))=CAST(NEW.account_id AS CHAR)))) AND (NOT(OLD.`id` <=> NEW.`id`) OR NOT(OLD.`tenant_id` <=> NEW.`tenant_id`) OR NOT(OLD.`account_id` <=> NEW.`account_id`) OR NOT(OLD.`cost_type` <=> NEW.`cost_type`) OR NOT(OLD.`amount` <=> NEW.`amount`) OR NOT(OLD.`pay_method` <=> NEW.`pay_method`) OR NOT(OLD.`pay_date` <=> NEW.`pay_date`) OR NOT(OLD.`period` <=> NEW.`period`) OR NOT(OLD.`remark` <=> NEW.`remark`) OR NOT(OLD.`attachment_id` <=> NEW.`attachment_id`) OR NOT(OLD.`deleted` <=> NEW.`deleted`) OR NOT(OLD.`cost_date` <=> NEW.`cost_date`) OR NOT(OLD.`source_type` <=> NEW.`source_type`) OR NOT(OLD.`source_key` <=> NEW.`source_key`) OR NOT(OLD.`source_candidate_id` <=> NEW.`source_candidate_id`) OR NOT(OLD.`approval_business_id` <=> NEW.`approval_business_id`) OR NOT(OLD.`approval_instance_id` <=> NEW.`approval_instance_id`) OR NOT(OLD.`author_id` <=> NEW.`author_id`) OR NOT(OLD.`attribution_ip_group_id` <=> NEW.`attribution_ip_group_id`) OR NOT(OLD.`payment_state` <=> NEW.`payment_state`) OR NOT(OLD.`paid_reference` <=> NEW.`paid_reference`) OR NOT(OLD.`payment_note` <=> NEW.`payment_note`)) THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (NEW.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_path,NULL),'finance',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'update'),'oa_account_cost',CAST(NEW.`id` AS CHAR),NEW.source_candidate_id,LEFT(COALESCE(CONCAT('账号成本 ',NEW.id),''),255),CAST(NEW.author_id AS CHAR),CAST(OLD.author_id AS CHAR),JSON_OBJECT('id',CAST(OLD.`id` AS CHAR),'account_id',CAST(OLD.`account_id` AS CHAR),'cost_type',CAST(OLD.`cost_type` AS CHAR),'amount',OLD.`amount`,'pay_method',CAST(OLD.`pay_method` AS CHAR),'pay_date',CAST(OLD.`pay_date` AS CHAR),'period',CAST(OLD.`period` AS CHAR),'remark',IF(OLD.`remark` IS NULL,NULL,'[自由文本原文未存入审计]'),'attachment_id',CAST(OLD.`attachment_id` AS CHAR),'deleted',OLD.`deleted`,'cost_date',CAST(OLD.`cost_date` AS CHAR),'source_type',CAST(OLD.`source_type` AS CHAR),'source_key',CAST(OLD.`source_key` AS CHAR),'source_candidate_id',CAST(OLD.`source_candidate_id` AS CHAR),'approval_business_id',CAST(OLD.`approval_business_id` AS CHAR),'approval_instance_id',CAST(OLD.`approval_instance_id` AS CHAR),'author_id',CAST(OLD.`author_id` AS CHAR),'attribution_ip_group_id',CAST(OLD.`attribution_ip_group_id` AS CHAR),'payment_state',CAST(OLD.`payment_state` AS CHAR),'paid_reference',IF(OLD.`paid_reference` IS NULL,NULL,'[已脱敏]'),'payment_note',IF(OLD.`payment_note` IS NULL,NULL,'[自由文本原文未存入审计]')),JSON_OBJECT('id',CAST(NEW.`id` AS CHAR),'account_id',CAST(NEW.`account_id` AS CHAR),'cost_type',CAST(NEW.`cost_type` AS CHAR),'amount',NEW.`amount`,'pay_method',CAST(NEW.`pay_method` AS CHAR),'pay_date',CAST(NEW.`pay_date` AS CHAR),'period',CAST(NEW.`period` AS CHAR),'remark',IF(NEW.`remark` IS NULL,NULL,'[自由文本原文未存入审计]'),'attachment_id',CAST(NEW.`attachment_id` AS CHAR),'deleted',NEW.`deleted`,'cost_date',CAST(NEW.`cost_date` AS CHAR),'source_type',CAST(NEW.`source_type` AS CHAR),'source_key',CAST(NEW.`source_key` AS CHAR),'source_candidate_id',CAST(NEW.`source_candidate_id` AS CHAR),'approval_business_id',CAST(NEW.`approval_business_id` AS CHAR),'approval_instance_id',CAST(NEW.`approval_instance_id` AS CHAR),'author_id',CAST(NEW.`author_id` AS CHAR),'attribution_ip_group_id',CAST(NEW.`attribution_ip_group_id` AS CHAR),'payment_state',CAST(NEW.`payment_state` AS CHAR),'paid_reference',IF(NEW.`paid_reference` IS NULL,NULL,'[已脱敏]'),'payment_note',IF(NEW.`payment_note` IS NULL,NULL,'[自由文本原文未存入审计]')),JSON_MERGE_PRESERVE(IF(NOT(OLD.`id` <=> NEW.`id`),JSON_ARRAY('id'),JSON_ARRAY()),IF(NOT(OLD.`tenant_id` <=> NEW.`tenant_id`),JSON_ARRAY('tenant_id'),JSON_ARRAY()),IF(NOT(OLD.`account_id` <=> NEW.`account_id`),JSON_ARRAY('account_id'),JSON_ARRAY()),IF(NOT(OLD.`cost_type` <=> NEW.`cost_type`),JSON_ARRAY('cost_type'),JSON_ARRAY()),IF(NOT(OLD.`amount` <=> NEW.`amount`),JSON_ARRAY('amount'),JSON_ARRAY()),IF(NOT(OLD.`pay_method` <=> NEW.`pay_method`),JSON_ARRAY('pay_method'),JSON_ARRAY()),IF(NOT(OLD.`pay_date` <=> NEW.`pay_date`),JSON_ARRAY('pay_date'),JSON_ARRAY()),IF(NOT(OLD.`period` <=> NEW.`period`),JSON_ARRAY('period'),JSON_ARRAY()),IF(NOT(OLD.`remark` <=> NEW.`remark`),JSON_ARRAY('remark'),JSON_ARRAY()),IF(NOT(OLD.`attachment_id` <=> NEW.`attachment_id`),JSON_ARRAY('attachment_id'),JSON_ARRAY()),IF(NOT(OLD.`deleted` <=> NEW.`deleted`),JSON_ARRAY('deleted'),JSON_ARRAY()),IF(NOT(OLD.`cost_date` <=> NEW.`cost_date`),JSON_ARRAY('cost_date'),JSON_ARRAY()),IF(NOT(OLD.`source_type` <=> NEW.`source_type`),JSON_ARRAY('source_type'),JSON_ARRAY()),IF(NOT(OLD.`source_key` <=> NEW.`source_key`),JSON_ARRAY('source_key'),JSON_ARRAY()),IF(NOT(OLD.`source_candidate_id` <=> NEW.`source_candidate_id`),JSON_ARRAY('source_candidate_id'),JSON_ARRAY()),IF(NOT(OLD.`approval_business_id` <=> NEW.`approval_business_id`),JSON_ARRAY('approval_business_id'),JSON_ARRAY()),IF(NOT(OLD.`approval_instance_id` <=> NEW.`approval_instance_id`),JSON_ARRAY('approval_instance_id'),JSON_ARRAY()),IF(NOT(OLD.`author_id` <=> NEW.`author_id`),JSON_ARRAY('author_id'),JSON_ARRAY()),IF(NOT(OLD.`attribution_ip_group_id` <=> NEW.`attribution_ip_group_id`),JSON_ARRAY('attribution_ip_group_id'),JSON_ARRAY()),IF(NOT(OLD.`payment_state` <=> NEW.`payment_state`),JSON_ARRAY('payment_state'),JSON_ARRAY()),IF(NOT(OLD.`paid_reference` <=> NEW.`paid_reference`),JSON_ARRAY('paid_reference'),JSON_ARRAY()),IF(NOT(OLD.`payment_note` <=> NEW.`payment_note`),JSON_ARRAY('payment_note'),JSON_ARRAY())));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_oa_account_cost_d_v1` AFTER DELETE ON `oa_account_cost` FOR EACH ROW
BEGIN
 IF (OLD.source_type='OA' OR EXISTS(SELECT 1 FROM roi_record r WHERE r.tenant_id=OLD.tenant_id AND r.kind='account' AND JSON_UNQUOTE(JSON_EXTRACT(r.payload,'$.masterAccountId'))=CAST(OLD.account_id AS CHAR))) THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (OLD.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_path,NULL),'finance',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'delete'),'oa_account_cost',CAST(OLD.`id` AS CHAR),OLD.source_candidate_id,LEFT(COALESCE(CONCAT('账号成本 ',OLD.id),''),255),CAST(OLD.author_id AS CHAR),CAST(OLD.author_id AS CHAR),JSON_OBJECT('id',CAST(OLD.`id` AS CHAR),'account_id',CAST(OLD.`account_id` AS CHAR),'cost_type',CAST(OLD.`cost_type` AS CHAR),'amount',OLD.`amount`,'pay_method',CAST(OLD.`pay_method` AS CHAR),'pay_date',CAST(OLD.`pay_date` AS CHAR),'period',CAST(OLD.`period` AS CHAR),'remark',IF(OLD.`remark` IS NULL,NULL,'[自由文本原文未存入审计]'),'attachment_id',CAST(OLD.`attachment_id` AS CHAR),'deleted',OLD.`deleted`,'cost_date',CAST(OLD.`cost_date` AS CHAR),'source_type',CAST(OLD.`source_type` AS CHAR),'source_key',CAST(OLD.`source_key` AS CHAR),'source_candidate_id',CAST(OLD.`source_candidate_id` AS CHAR),'approval_business_id',CAST(OLD.`approval_business_id` AS CHAR),'approval_instance_id',CAST(OLD.`approval_instance_id` AS CHAR),'author_id',CAST(OLD.`author_id` AS CHAR),'attribution_ip_group_id',CAST(OLD.`attribution_ip_group_id` AS CHAR),'payment_state',CAST(OLD.`payment_state` AS CHAR),'paid_reference',IF(OLD.`paid_reference` IS NULL,NULL,'[已脱敏]'),'payment_note',IF(OLD.`payment_note` IS NULL,NULL,'[自由文本原文未存入审计]')),NULL,JSON_ARRAY('id','tenant_id','account_id','cost_type','amount','pay_method','pay_date','period','remark','attachment_id','deleted','cost_date','source_type','source_key','source_candidate_id','approval_business_id','approval_instance_id','author_id','attribution_ip_group_id','payment_state','paid_reference','payment_note'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_oa_account_i_v1` AFTER INSERT ON `oa_account` FOR EACH ROW
BEGIN
 IF ((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) OR EXISTS(SELECT 1 FROM roi_record r WHERE r.tenant_id=NEW.tenant_id AND r.kind='account' AND JSON_UNQUOTE(JSON_EXTRACT(r.payload,'$.masterAccountId'))=CAST(NEW.id AS CHAR))) THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (NEW.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_path,NULL),'account',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'create'),'oa_account',CAST(NEW.`id` AS CHAR),NULL,LEFT(COALESCE(roi_audit_text_v1(NEW.account_name),''),255),NULL,NULL,NULL,JSON_OBJECT('id',CAST(NEW.`id` AS CHAR),'platform_type',CAST(NEW.`platform_type` AS CHAR),'account_name',roi_audit_text_v1(CAST(NEW.`account_name` AS CHAR)),'external_account_id',CAST(NEW.`external_account_id` AS CHAR),'phone_id',IF(NEW.`phone_id` IS NULL,NULL,'[已脱敏]'),'phone_number_hash',IF(NEW.`phone_number_hash` IS NULL,NULL,'[已脱敏]'),'sim_card_id',CAST(NEW.`sim_card_id` AS CHAR),'status',CAST(NEW.`status` AS CHAR),'linked_at',CAST(NEW.`linked_at` AS CHAR),'deleted',NEW.`deleted`,'company_id',CAST(NEW.`company_id` AS CHAR),'realname_id',IF(NEW.`realname_id` IS NULL,NULL,'[已脱敏]'),'intermediary_id',CAST(NEW.`intermediary_id` AS CHAR),'account_type',CAST(NEW.`account_type` AS CHAR),'ip_group_id',CAST(NEW.`ip_group_id` AS CHAR),'operator_user_id',CAST(NEW.`operator_user_id` AS CHAR),'cookie_encrypted',IF(NEW.`cookie_encrypted` IS NULL,NULL,'[已脱敏]'),'publish_enabled',CAST(NEW.`publish_enabled` AS CHAR),'collect_enabled',CAST(NEW.`collect_enabled` AS CHAR),'collect_live_enabled',CAST(NEW.`collect_live_enabled` AS CHAR),'trademark_name',roi_audit_text_v1(CAST(NEW.`trademark_name` AS CHAR)),'email',IF(NEW.`email` IS NULL,NULL,'[已脱敏]'),'password_encrypted',IF(NEW.`password_encrypted` IS NULL,NULL,'[已脱敏]'),'short_video_status',CAST(NEW.`short_video_status` AS CHAR),'live_status',CAST(NEW.`live_status` AS CHAR),'qualification_type',CAST(NEW.`qualification_type` AS CHAR),'usage_status',CAST(NEW.`usage_status` AS CHAR),'original_account_name',roi_audit_text_v1(CAST(NEW.`original_account_name` AS CHAR)),'cert_expiry_time',CAST(NEW.`cert_expiry_time` AS CHAR),'cert_count',CAST(NEW.`cert_count` AS CHAR),'linked_video_account_id',CAST(NEW.`linked_video_account_id` AS CHAR),'video_account_registered_at',CAST(NEW.`video_account_registered_at` AS CHAR),'admin_name',CAST(NEW.`admin_name` AS CHAR),'admin_user_id',CAST(NEW.`admin_user_id` AS CHAR),'holder_user_id',CAST(NEW.`holder_user_id` AS CHAR),'admin_id_card_encrypted',IF(NEW.`admin_id_card_encrypted` IS NULL,NULL,'[已脱敏]'),'mp_token_encrypted',IF(NEW.`mp_token_encrypted` IS NULL,NULL,'[已脱敏]'),'auth_token_encrypted',IF(NEW.`auth_token_encrypted` IS NULL,NULL,'[已脱敏]'),'field_mapping',IF(NEW.`field_mapping` IS NULL,NULL,'[已脱敏]'),'app_id',CAST(NEW.`app_id` AS CHAR),'app_secret_encrypted',IF(NEW.`app_secret_encrypted` IS NULL,NULL,'[已脱敏]')),JSON_ARRAY('id','tenant_id','platform_type','account_name','external_account_id','phone_id','phone_number_hash','sim_card_id','status','linked_at','deleted','company_id','realname_id','intermediary_id','account_type','ip_group_id','operator_user_id','cookie_encrypted','publish_enabled','collect_enabled','collect_live_enabled','trademark_name','email','password_encrypted','short_video_status','live_status','qualification_type','usage_status','original_account_name','cert_expiry_time','cert_count','linked_video_account_id','video_account_registered_at','admin_name','admin_user_id','holder_user_id','admin_id_card_encrypted','mp_token_encrypted','auth_token_encrypted','field_mapping','app_id','app_secret_encrypted'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_oa_account_u_v1` AFTER UPDATE ON `oa_account` FOR EACH ROW
BEGIN
 IF (((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id) OR EXISTS(SELECT 1 FROM roi_record r WHERE r.tenant_id=OLD.tenant_id AND r.kind='account' AND JSON_UNQUOTE(JSON_EXTRACT(r.payload,'$.masterAccountId'))=CAST(OLD.id AS CHAR))) OR ((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) OR EXISTS(SELECT 1 FROM roi_record r WHERE r.tenant_id=NEW.tenant_id AND r.kind='account' AND JSON_UNQUOTE(JSON_EXTRACT(r.payload,'$.masterAccountId'))=CAST(NEW.id AS CHAR)))) AND (NOT(OLD.`id` <=> NEW.`id`) OR NOT(OLD.`tenant_id` <=> NEW.`tenant_id`) OR NOT(OLD.`platform_type` <=> NEW.`platform_type`) OR NOT(OLD.`account_name` <=> NEW.`account_name`) OR NOT(OLD.`external_account_id` <=> NEW.`external_account_id`) OR NOT(OLD.`phone_id` <=> NEW.`phone_id`) OR NOT(OLD.`phone_number_hash` <=> NEW.`phone_number_hash`) OR NOT(OLD.`sim_card_id` <=> NEW.`sim_card_id`) OR NOT(OLD.`status` <=> NEW.`status`) OR NOT(OLD.`linked_at` <=> NEW.`linked_at`) OR NOT(OLD.`deleted` <=> NEW.`deleted`) OR NOT(OLD.`company_id` <=> NEW.`company_id`) OR NOT(OLD.`realname_id` <=> NEW.`realname_id`) OR NOT(OLD.`intermediary_id` <=> NEW.`intermediary_id`) OR NOT(OLD.`account_type` <=> NEW.`account_type`) OR NOT(OLD.`ip_group_id` <=> NEW.`ip_group_id`) OR NOT(OLD.`operator_user_id` <=> NEW.`operator_user_id`) OR NOT(OLD.`cookie_encrypted` <=> NEW.`cookie_encrypted`) OR NOT(OLD.`publish_enabled` <=> NEW.`publish_enabled`) OR NOT(OLD.`collect_enabled` <=> NEW.`collect_enabled`) OR NOT(OLD.`collect_live_enabled` <=> NEW.`collect_live_enabled`) OR NOT(OLD.`trademark_name` <=> NEW.`trademark_name`) OR NOT(OLD.`email` <=> NEW.`email`) OR NOT(OLD.`password_encrypted` <=> NEW.`password_encrypted`) OR NOT(OLD.`short_video_status` <=> NEW.`short_video_status`) OR NOT(OLD.`live_status` <=> NEW.`live_status`) OR NOT(OLD.`qualification_type` <=> NEW.`qualification_type`) OR NOT(OLD.`usage_status` <=> NEW.`usage_status`) OR NOT(OLD.`original_account_name` <=> NEW.`original_account_name`) OR NOT(OLD.`cert_expiry_time` <=> NEW.`cert_expiry_time`) OR NOT(OLD.`cert_count` <=> NEW.`cert_count`) OR NOT(OLD.`linked_video_account_id` <=> NEW.`linked_video_account_id`) OR NOT(OLD.`video_account_registered_at` <=> NEW.`video_account_registered_at`) OR NOT(OLD.`admin_name` <=> NEW.`admin_name`) OR NOT(OLD.`admin_user_id` <=> NEW.`admin_user_id`) OR NOT(OLD.`holder_user_id` <=> NEW.`holder_user_id`) OR NOT(OLD.`admin_id_card_encrypted` <=> NEW.`admin_id_card_encrypted`) OR NOT(OLD.`mp_token_encrypted` <=> NEW.`mp_token_encrypted`) OR NOT(OLD.`auth_token_encrypted` <=> NEW.`auth_token_encrypted`) OR NOT(OLD.`field_mapping` <=> NEW.`field_mapping`) OR NOT(OLD.`app_id` <=> NEW.`app_id`) OR NOT(OLD.`app_secret_encrypted` <=> NEW.`app_secret_encrypted`)) THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (NEW.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_path,NULL),'account',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'update'),'oa_account',CAST(NEW.`id` AS CHAR),NULL,LEFT(COALESCE(roi_audit_text_v1(NEW.account_name),''),255),NULL,NULL,JSON_OBJECT('id',CAST(OLD.`id` AS CHAR),'platform_type',CAST(OLD.`platform_type` AS CHAR),'account_name',roi_audit_text_v1(CAST(OLD.`account_name` AS CHAR)),'external_account_id',CAST(OLD.`external_account_id` AS CHAR),'phone_id',IF(OLD.`phone_id` IS NULL,NULL,'[已脱敏]'),'phone_number_hash',IF(OLD.`phone_number_hash` IS NULL,NULL,'[已脱敏]'),'sim_card_id',CAST(OLD.`sim_card_id` AS CHAR),'status',CAST(OLD.`status` AS CHAR),'linked_at',CAST(OLD.`linked_at` AS CHAR),'deleted',OLD.`deleted`,'company_id',CAST(OLD.`company_id` AS CHAR),'realname_id',IF(OLD.`realname_id` IS NULL,NULL,'[已脱敏]'),'intermediary_id',CAST(OLD.`intermediary_id` AS CHAR),'account_type',CAST(OLD.`account_type` AS CHAR),'ip_group_id',CAST(OLD.`ip_group_id` AS CHAR),'operator_user_id',CAST(OLD.`operator_user_id` AS CHAR),'cookie_encrypted',IF(OLD.`cookie_encrypted` IS NULL,NULL,'[已脱敏]'),'publish_enabled',CAST(OLD.`publish_enabled` AS CHAR),'collect_enabled',CAST(OLD.`collect_enabled` AS CHAR),'collect_live_enabled',CAST(OLD.`collect_live_enabled` AS CHAR),'trademark_name',roi_audit_text_v1(CAST(OLD.`trademark_name` AS CHAR)),'email',IF(OLD.`email` IS NULL,NULL,'[已脱敏]'),'password_encrypted',IF(OLD.`password_encrypted` IS NULL,NULL,'[已脱敏]'),'short_video_status',CAST(OLD.`short_video_status` AS CHAR),'live_status',CAST(OLD.`live_status` AS CHAR),'qualification_type',CAST(OLD.`qualification_type` AS CHAR),'usage_status',CAST(OLD.`usage_status` AS CHAR),'original_account_name',roi_audit_text_v1(CAST(OLD.`original_account_name` AS CHAR)),'cert_expiry_time',CAST(OLD.`cert_expiry_time` AS CHAR),'cert_count',CAST(OLD.`cert_count` AS CHAR),'linked_video_account_id',CAST(OLD.`linked_video_account_id` AS CHAR),'video_account_registered_at',CAST(OLD.`video_account_registered_at` AS CHAR),'admin_name',CAST(OLD.`admin_name` AS CHAR),'admin_user_id',CAST(OLD.`admin_user_id` AS CHAR),'holder_user_id',CAST(OLD.`holder_user_id` AS CHAR),'admin_id_card_encrypted',IF(OLD.`admin_id_card_encrypted` IS NULL,NULL,'[已脱敏]'),'mp_token_encrypted',IF(OLD.`mp_token_encrypted` IS NULL,NULL,'[已脱敏]'),'auth_token_encrypted',IF(OLD.`auth_token_encrypted` IS NULL,NULL,'[已脱敏]'),'field_mapping',IF(OLD.`field_mapping` IS NULL,NULL,'[已脱敏]'),'app_id',CAST(OLD.`app_id` AS CHAR),'app_secret_encrypted',IF(OLD.`app_secret_encrypted` IS NULL,NULL,'[已脱敏]')),JSON_OBJECT('id',CAST(NEW.`id` AS CHAR),'platform_type',CAST(NEW.`platform_type` AS CHAR),'account_name',roi_audit_text_v1(CAST(NEW.`account_name` AS CHAR)),'external_account_id',CAST(NEW.`external_account_id` AS CHAR),'phone_id',IF(NEW.`phone_id` IS NULL,NULL,'[已脱敏]'),'phone_number_hash',IF(NEW.`phone_number_hash` IS NULL,NULL,'[已脱敏]'),'sim_card_id',CAST(NEW.`sim_card_id` AS CHAR),'status',CAST(NEW.`status` AS CHAR),'linked_at',CAST(NEW.`linked_at` AS CHAR),'deleted',NEW.`deleted`,'company_id',CAST(NEW.`company_id` AS CHAR),'realname_id',IF(NEW.`realname_id` IS NULL,NULL,'[已脱敏]'),'intermediary_id',CAST(NEW.`intermediary_id` AS CHAR),'account_type',CAST(NEW.`account_type` AS CHAR),'ip_group_id',CAST(NEW.`ip_group_id` AS CHAR),'operator_user_id',CAST(NEW.`operator_user_id` AS CHAR),'cookie_encrypted',IF(NEW.`cookie_encrypted` IS NULL,NULL,'[已脱敏]'),'publish_enabled',CAST(NEW.`publish_enabled` AS CHAR),'collect_enabled',CAST(NEW.`collect_enabled` AS CHAR),'collect_live_enabled',CAST(NEW.`collect_live_enabled` AS CHAR),'trademark_name',roi_audit_text_v1(CAST(NEW.`trademark_name` AS CHAR)),'email',IF(NEW.`email` IS NULL,NULL,'[已脱敏]'),'password_encrypted',IF(NEW.`password_encrypted` IS NULL,NULL,'[已脱敏]'),'short_video_status',CAST(NEW.`short_video_status` AS CHAR),'live_status',CAST(NEW.`live_status` AS CHAR),'qualification_type',CAST(NEW.`qualification_type` AS CHAR),'usage_status',CAST(NEW.`usage_status` AS CHAR),'original_account_name',roi_audit_text_v1(CAST(NEW.`original_account_name` AS CHAR)),'cert_expiry_time',CAST(NEW.`cert_expiry_time` AS CHAR),'cert_count',CAST(NEW.`cert_count` AS CHAR),'linked_video_account_id',CAST(NEW.`linked_video_account_id` AS CHAR),'video_account_registered_at',CAST(NEW.`video_account_registered_at` AS CHAR),'admin_name',CAST(NEW.`admin_name` AS CHAR),'admin_user_id',CAST(NEW.`admin_user_id` AS CHAR),'holder_user_id',CAST(NEW.`holder_user_id` AS CHAR),'admin_id_card_encrypted',IF(NEW.`admin_id_card_encrypted` IS NULL,NULL,'[已脱敏]'),'mp_token_encrypted',IF(NEW.`mp_token_encrypted` IS NULL,NULL,'[已脱敏]'),'auth_token_encrypted',IF(NEW.`auth_token_encrypted` IS NULL,NULL,'[已脱敏]'),'field_mapping',IF(NEW.`field_mapping` IS NULL,NULL,'[已脱敏]'),'app_id',CAST(NEW.`app_id` AS CHAR),'app_secret_encrypted',IF(NEW.`app_secret_encrypted` IS NULL,NULL,'[已脱敏]')),JSON_MERGE_PRESERVE(IF(NOT(OLD.`id` <=> NEW.`id`),JSON_ARRAY('id'),JSON_ARRAY()),IF(NOT(OLD.`tenant_id` <=> NEW.`tenant_id`),JSON_ARRAY('tenant_id'),JSON_ARRAY()),IF(NOT(OLD.`platform_type` <=> NEW.`platform_type`),JSON_ARRAY('platform_type'),JSON_ARRAY()),IF(NOT(OLD.`account_name` <=> NEW.`account_name`),JSON_ARRAY('account_name'),JSON_ARRAY()),IF(NOT(OLD.`external_account_id` <=> NEW.`external_account_id`),JSON_ARRAY('external_account_id'),JSON_ARRAY()),IF(NOT(OLD.`phone_id` <=> NEW.`phone_id`),JSON_ARRAY('phone_id'),JSON_ARRAY()),IF(NOT(OLD.`phone_number_hash` <=> NEW.`phone_number_hash`),JSON_ARRAY('phone_number_hash'),JSON_ARRAY()),IF(NOT(OLD.`sim_card_id` <=> NEW.`sim_card_id`),JSON_ARRAY('sim_card_id'),JSON_ARRAY()),IF(NOT(OLD.`status` <=> NEW.`status`),JSON_ARRAY('status'),JSON_ARRAY()),IF(NOT(OLD.`linked_at` <=> NEW.`linked_at`),JSON_ARRAY('linked_at'),JSON_ARRAY()),IF(NOT(OLD.`deleted` <=> NEW.`deleted`),JSON_ARRAY('deleted'),JSON_ARRAY()),IF(NOT(OLD.`company_id` <=> NEW.`company_id`),JSON_ARRAY('company_id'),JSON_ARRAY()),IF(NOT(OLD.`realname_id` <=> NEW.`realname_id`),JSON_ARRAY('realname_id'),JSON_ARRAY()),IF(NOT(OLD.`intermediary_id` <=> NEW.`intermediary_id`),JSON_ARRAY('intermediary_id'),JSON_ARRAY()),IF(NOT(OLD.`account_type` <=> NEW.`account_type`),JSON_ARRAY('account_type'),JSON_ARRAY()),IF(NOT(OLD.`ip_group_id` <=> NEW.`ip_group_id`),JSON_ARRAY('ip_group_id'),JSON_ARRAY()),IF(NOT(OLD.`operator_user_id` <=> NEW.`operator_user_id`),JSON_ARRAY('operator_user_id'),JSON_ARRAY()),IF(NOT(OLD.`cookie_encrypted` <=> NEW.`cookie_encrypted`),JSON_ARRAY('cookie_encrypted'),JSON_ARRAY()),IF(NOT(OLD.`publish_enabled` <=> NEW.`publish_enabled`),JSON_ARRAY('publish_enabled'),JSON_ARRAY()),IF(NOT(OLD.`collect_enabled` <=> NEW.`collect_enabled`),JSON_ARRAY('collect_enabled'),JSON_ARRAY()),IF(NOT(OLD.`collect_live_enabled` <=> NEW.`collect_live_enabled`),JSON_ARRAY('collect_live_enabled'),JSON_ARRAY()),IF(NOT(OLD.`trademark_name` <=> NEW.`trademark_name`),JSON_ARRAY('trademark_name'),JSON_ARRAY()),IF(NOT(OLD.`email` <=> NEW.`email`),JSON_ARRAY('email'),JSON_ARRAY()),IF(NOT(OLD.`password_encrypted` <=> NEW.`password_encrypted`),JSON_ARRAY('password_encrypted'),JSON_ARRAY()),IF(NOT(OLD.`short_video_status` <=> NEW.`short_video_status`),JSON_ARRAY('short_video_status'),JSON_ARRAY()),IF(NOT(OLD.`live_status` <=> NEW.`live_status`),JSON_ARRAY('live_status'),JSON_ARRAY()),IF(NOT(OLD.`qualification_type` <=> NEW.`qualification_type`),JSON_ARRAY('qualification_type'),JSON_ARRAY()),IF(NOT(OLD.`usage_status` <=> NEW.`usage_status`),JSON_ARRAY('usage_status'),JSON_ARRAY()),IF(NOT(OLD.`original_account_name` <=> NEW.`original_account_name`),JSON_ARRAY('original_account_name'),JSON_ARRAY()),IF(NOT(OLD.`cert_expiry_time` <=> NEW.`cert_expiry_time`),JSON_ARRAY('cert_expiry_time'),JSON_ARRAY()),IF(NOT(OLD.`cert_count` <=> NEW.`cert_count`),JSON_ARRAY('cert_count'),JSON_ARRAY()),IF(NOT(OLD.`linked_video_account_id` <=> NEW.`linked_video_account_id`),JSON_ARRAY('linked_video_account_id'),JSON_ARRAY()),IF(NOT(OLD.`video_account_registered_at` <=> NEW.`video_account_registered_at`),JSON_ARRAY('video_account_registered_at'),JSON_ARRAY()),IF(NOT(OLD.`admin_name` <=> NEW.`admin_name`),JSON_ARRAY('admin_name'),JSON_ARRAY()),IF(NOT(OLD.`admin_user_id` <=> NEW.`admin_user_id`),JSON_ARRAY('admin_user_id'),JSON_ARRAY()),IF(NOT(OLD.`holder_user_id` <=> NEW.`holder_user_id`),JSON_ARRAY('holder_user_id'),JSON_ARRAY()),IF(NOT(OLD.`admin_id_card_encrypted` <=> NEW.`admin_id_card_encrypted`),JSON_ARRAY('admin_id_card_encrypted'),JSON_ARRAY()),IF(NOT(OLD.`mp_token_encrypted` <=> NEW.`mp_token_encrypted`),JSON_ARRAY('mp_token_encrypted'),JSON_ARRAY()),IF(NOT(OLD.`auth_token_encrypted` <=> NEW.`auth_token_encrypted`),JSON_ARRAY('auth_token_encrypted'),JSON_ARRAY()),IF(NOT(OLD.`field_mapping` <=> NEW.`field_mapping`),JSON_ARRAY('field_mapping'),JSON_ARRAY()),IF(NOT(OLD.`app_id` <=> NEW.`app_id`),JSON_ARRAY('app_id'),JSON_ARRAY()),IF(NOT(OLD.`app_secret_encrypted` <=> NEW.`app_secret_encrypted`),JSON_ARRAY('app_secret_encrypted'),JSON_ARRAY())));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_log_oa_account_d_v1` AFTER DELETE ON `oa_account` FOR EACH ROW
BEGIN
 IF ((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id) OR EXISTS(SELECT 1 FROM roi_record r WHERE r.tenant_id=OLD.tenant_id AND r.kind='account' AND JSON_UNQUOTE(JSON_EXTRACT(r.payload,'$.masterAccountId'))=CAST(OLD.id AS CHAR))) THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES (OLD.tenant_id,UTC_TIMESTAMP(3),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_roles,'[]'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_request,NULL),IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id),@roi_audit_path,NULL),'account',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=OLD.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'delete'),'oa_account',CAST(OLD.`id` AS CHAR),NULL,LEFT(COALESCE(roi_audit_text_v1(OLD.account_name),''),255),NULL,NULL,JSON_OBJECT('id',CAST(OLD.`id` AS CHAR),'platform_type',CAST(OLD.`platform_type` AS CHAR),'account_name',roi_audit_text_v1(CAST(OLD.`account_name` AS CHAR)),'external_account_id',CAST(OLD.`external_account_id` AS CHAR),'phone_id',IF(OLD.`phone_id` IS NULL,NULL,'[已脱敏]'),'phone_number_hash',IF(OLD.`phone_number_hash` IS NULL,NULL,'[已脱敏]'),'sim_card_id',CAST(OLD.`sim_card_id` AS CHAR),'status',CAST(OLD.`status` AS CHAR),'linked_at',CAST(OLD.`linked_at` AS CHAR),'deleted',OLD.`deleted`,'company_id',CAST(OLD.`company_id` AS CHAR),'realname_id',IF(OLD.`realname_id` IS NULL,NULL,'[已脱敏]'),'intermediary_id',CAST(OLD.`intermediary_id` AS CHAR),'account_type',CAST(OLD.`account_type` AS CHAR),'ip_group_id',CAST(OLD.`ip_group_id` AS CHAR),'operator_user_id',CAST(OLD.`operator_user_id` AS CHAR),'cookie_encrypted',IF(OLD.`cookie_encrypted` IS NULL,NULL,'[已脱敏]'),'publish_enabled',CAST(OLD.`publish_enabled` AS CHAR),'collect_enabled',CAST(OLD.`collect_enabled` AS CHAR),'collect_live_enabled',CAST(OLD.`collect_live_enabled` AS CHAR),'trademark_name',roi_audit_text_v1(CAST(OLD.`trademark_name` AS CHAR)),'email',IF(OLD.`email` IS NULL,NULL,'[已脱敏]'),'password_encrypted',IF(OLD.`password_encrypted` IS NULL,NULL,'[已脱敏]'),'short_video_status',CAST(OLD.`short_video_status` AS CHAR),'live_status',CAST(OLD.`live_status` AS CHAR),'qualification_type',CAST(OLD.`qualification_type` AS CHAR),'usage_status',CAST(OLD.`usage_status` AS CHAR),'original_account_name',roi_audit_text_v1(CAST(OLD.`original_account_name` AS CHAR)),'cert_expiry_time',CAST(OLD.`cert_expiry_time` AS CHAR),'cert_count',CAST(OLD.`cert_count` AS CHAR),'linked_video_account_id',CAST(OLD.`linked_video_account_id` AS CHAR),'video_account_registered_at',CAST(OLD.`video_account_registered_at` AS CHAR),'admin_name',CAST(OLD.`admin_name` AS CHAR),'admin_user_id',CAST(OLD.`admin_user_id` AS CHAR),'holder_user_id',CAST(OLD.`holder_user_id` AS CHAR),'admin_id_card_encrypted',IF(OLD.`admin_id_card_encrypted` IS NULL,NULL,'[已脱敏]'),'mp_token_encrypted',IF(OLD.`mp_token_encrypted` IS NULL,NULL,'[已脱敏]'),'auth_token_encrypted',IF(OLD.`auth_token_encrypted` IS NULL,NULL,'[已脱敏]'),'field_mapping',IF(OLD.`field_mapping` IS NULL,NULL,'[已脱敏]'),'app_id',CAST(OLD.`app_id` AS CHAR),'app_secret_encrypted',IF(OLD.`app_secret_encrypted` IS NULL,NULL,'[已脱敏]')),NULL,JSON_ARRAY('id','tenant_id','platform_type','account_name','external_account_id','phone_id','phone_number_hash','sim_card_id','status','linked_at','deleted','company_id','realname_id','intermediary_id','account_type','ip_group_id','operator_user_id','cookie_encrypted','publish_enabled','collect_enabled','collect_live_enabled','trademark_name','email','password_encrypted','short_video_status','live_status','qualification_type','usage_status','original_account_name','cert_expiry_time','cert_count','linked_video_account_id','video_account_registered_at','admin_name','admin_user_id','holder_user_id','admin_id_card_encrypted','mp_token_encrypted','auth_token_encrypted','field_mapping','app_id','app_secret_encrypted'));
 END IF;
END$$
DELIMITER ;
-- 导入旧审计的元数据。旧details未经递归脱敏且只有after，不复制、不伪造完整快照。
INSERT IGNORE INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,module,action,record_type,record_id,related_record_id,record_label,author_id,before_json,after_json,changed_fields,result,legacy,legacy_audit_id)
SELECT a.tenant_id,CONVERT_TZ(a.created_at,'+08:00','+00:00'),CAST(a.user_id AS CHAR),CASE WHEN a.user_id=0 THEN '旧系统任务' ELSE CONCAT('历史员工 ',a.user_id) END,'[]','legacy',
 CASE WHEN a.record_id LIKE 'scope:%' OR a.action='assign' THEN 'permission' WHEN r.kind='account' THEN 'account' WHEN r.kind='session' THEN 'session' WHEN r.kind IN ('expense','payment','channel') OR a.action LIKE 'oa_cost_%' THEN 'finance' WHEN r.kind='approval' THEN 'approval' ELSE 'other' END,
 a.action,CASE WHEN r.id IS NOT NULL THEN 'roi_record' WHEN c.id IS NOT NULL THEN 'ops_oa_cost_candidate' ELSE 'legacy' END,a.record_id,NULL,COALESCE(r.business_key,c.business_id,a.record_id),CAST(r.author_id AS CHAR),NULL,NULL,JSON_ARRAY(),'SUCCESS',1,a.id
FROM roi_audit a JOIN roi_audit_migration_state m ON m.migration_key='operation_log_v1' AND a.id<=m.legacy_max_id LEFT JOIN roi_record r ON r.tenant_id=a.tenant_id AND r.id=a.record_id LEFT JOIN ops_oa_cost_candidate c ON c.tenant_id=a.tenant_id AND c.id=a.record_id;
-- 执行后校验：SHOW TRIGGERS LIKE 'roi_operation_log'; SELECT module,COUNT(*) FROM roi_operation_log GROUP BY module;
-- 日志不提供业务删除/修改/清空入口；DBA拥有DDL权限仍可删除触发器，需配合备份和数据库访问审计。

-- =============================================================================
-- Section 05: 组织目录 / IP 组范围
-- SSOT: 20261026_roi_org_directory.sql
-- =============================================================================

-- 抖音投产：钉钉组织缓存、精确岗位映射、IP组授权
-- 执行顺序：20261026_roi_operation_logs.sql -> 本脚本 -> 本轮 Java 发布。
-- 库：shenyu-ops；MySQL 8.4。仅建增量表/触发器，不覆盖现有账号或业务角色。
-- 重复执行安全；CREATE IF NOT EXISTS 不会重置已配置数据。
-- 验证：SHOW TABLES LIKE 'roi_org_%'; SHOW TRIGGERS LIKE 'roi_org_%';
-- 回退：先回退本轮Java；保留缓存/范围和审计记录，停用映射可 UPDATE roi_org_role_mapping SET enabled=0；禁止删除审计日志。
USE `shenyu-ops`;
CREATE TABLE IF NOT EXISTS roi_org_member (
 tenant_id BIGINT NOT NULL,dingtalk_user_id VARCHAR(64) NOT NULL,name VARCHAR(160) NOT NULL,job_title VARCHAR(160) NOT NULL DEFAULT '',
 departments_json JSON NOT NULL,labels_json JSON NOT NULL,labels_synced_at DATETIME(3) NULL,active TINYINT NOT NULL DEFAULT 1,
 synced_at DATETIME(3) NOT NULL,PRIMARY KEY(tenant_id,dingtalk_user_id),KEY ix_roi_org_title(tenant_id,job_title)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE IF NOT EXISTS roi_org_role_mapping (
 id VARCHAR(36) NOT NULL PRIMARY KEY,tenant_id BIGINT NOT NULL,source_type VARCHAR(16) NOT NULL,source_key VARCHAR(160) NOT NULL,
 source_name VARCHAR(160) NOT NULL,role VARCHAR(24) NOT NULL,enabled TINYINT NOT NULL DEFAULT 1,
 UNIQUE KEY uq_roi_org_mapping(tenant_id,source_type,source_key,role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE IF NOT EXISTS roi_ip_group_scope (
 tenant_id BIGINT NOT NULL,user_id BIGINT NOT NULL,ip_group_id BIGINT NOT NULL,
 PRIMARY KEY(tenant_id,user_id,ip_group_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE IF NOT EXISTS roi_org_sync_state (
 tenant_id BIGINT NOT NULL PRIMARY KEY,status VARCHAR(32) NOT NULL,last_synced_at DATETIME(3) NULL,
 member_count INT NOT NULL DEFAULT 0,department_count INT NOT NULL DEFAULT 0,complete TINYINT NOT NULL DEFAULT 0,error_code VARCHAR(255) NOT NULL DEFAULT ''
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DELIMITER $$
CREATE TRIGGER IF NOT EXISTS roi_org_member_i_audit_v1 AFTER INSERT ON roi_org_member FOR EACH ROW
BEGIN
 IF 1=1 THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,record_label,before_json,after_json,changed_fields)
  VALUES(NEW.tenant_id,UTC_TIMESTAMP(3),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,COALESCE(@roi_audit_actor,'0'),'0'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,@roi_audit_roles,'[]'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,@roi_audit_source,'database_trigger'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,@roi_audit_request,NULL),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,@roi_audit_path,NULL),'permission','create','roi_org_member',NEW.`dingtalk_user_id`,'组织岗位与主播权限',NULL,JSON_OBJECT('dingtalk_user_id',NEW.`dingtalk_user_id`,'name',NEW.`name`,'job_title',NEW.`job_title`,'departments_json',NEW.`departments_json`,'labels_json',NEW.`labels_json`,'active',NEW.`active`),JSON_ARRAY('dingtalk_user_id','name','job_title','departments_json','labels_json','active'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS roi_org_member_u_audit_v1 AFTER UPDATE ON roi_org_member FOR EACH ROW
BEGIN
 IF NOT(OLD.`dingtalk_user_id` <=> NEW.`dingtalk_user_id`) OR NOT(OLD.`name` <=> NEW.`name`) OR NOT(OLD.`job_title` <=> NEW.`job_title`) OR NOT(OLD.`departments_json` <=> NEW.`departments_json`) OR NOT(OLD.`labels_json` <=> NEW.`labels_json`) OR NOT(OLD.`active` <=> NEW.`active`) THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,record_label,before_json,after_json,changed_fields)
  VALUES(NEW.tenant_id,UTC_TIMESTAMP(3),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,COALESCE(@roi_audit_actor,'0'),'0'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,@roi_audit_roles,'[]'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,@roi_audit_source,'database_trigger'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,@roi_audit_request,NULL),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,@roi_audit_path,NULL),'permission','update','roi_org_member',NEW.`dingtalk_user_id`,'组织岗位与主播权限',JSON_OBJECT('dingtalk_user_id',OLD.`dingtalk_user_id`,'name',OLD.`name`,'job_title',OLD.`job_title`,'departments_json',OLD.`departments_json`,'labels_json',OLD.`labels_json`,'active',OLD.`active`),JSON_OBJECT('dingtalk_user_id',NEW.`dingtalk_user_id`,'name',NEW.`name`,'job_title',NEW.`job_title`,'departments_json',NEW.`departments_json`,'labels_json',NEW.`labels_json`,'active',NEW.`active`),JSON_ARRAY('dingtalk_user_id','name','job_title','departments_json','labels_json','active'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS roi_org_member_d_audit_v1 AFTER DELETE ON roi_org_member FOR EACH ROW
BEGIN
 IF 1=1 THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,record_label,before_json,after_json,changed_fields)
  VALUES(OLD.tenant_id,UTC_TIMESTAMP(3),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=OLD.tenant_id,COALESCE(@roi_audit_actor,'0'),'0'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=OLD.tenant_id,COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=OLD.tenant_id,@roi_audit_roles,'[]'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=OLD.tenant_id,@roi_audit_source,'database_trigger'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=OLD.tenant_id,@roi_audit_request,NULL),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=OLD.tenant_id,@roi_audit_path,NULL),'permission','delete','roi_org_member',OLD.`dingtalk_user_id`,'组织岗位与主播权限',JSON_OBJECT('dingtalk_user_id',OLD.`dingtalk_user_id`,'name',OLD.`name`,'job_title',OLD.`job_title`,'departments_json',OLD.`departments_json`,'labels_json',OLD.`labels_json`,'active',OLD.`active`),NULL,JSON_ARRAY('dingtalk_user_id','name','job_title','departments_json','labels_json','active'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS roi_org_role_mapping_i_audit_v1 AFTER INSERT ON roi_org_role_mapping FOR EACH ROW
BEGIN
 IF 1=1 THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,record_label,before_json,after_json,changed_fields)
  VALUES(NEW.tenant_id,UTC_TIMESTAMP(3),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,COALESCE(@roi_audit_actor,'0'),'0'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,@roi_audit_roles,'[]'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,@roi_audit_source,'database_trigger'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,@roi_audit_request,NULL),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,@roi_audit_path,NULL),'permission','create','roi_org_role_mapping',NEW.`id`,'组织岗位与主播权限',NULL,JSON_OBJECT('id',NEW.`id`,'source_type',NEW.`source_type`,'source_key',NEW.`source_key`,'source_name',NEW.`source_name`,'role',NEW.`role`,'enabled',NEW.`enabled`),JSON_ARRAY('id','source_type','source_key','source_name','role','enabled'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS roi_org_role_mapping_u_audit_v1 AFTER UPDATE ON roi_org_role_mapping FOR EACH ROW
BEGIN
 IF NOT(OLD.`id` <=> NEW.`id`) OR NOT(OLD.`source_type` <=> NEW.`source_type`) OR NOT(OLD.`source_key` <=> NEW.`source_key`) OR NOT(OLD.`source_name` <=> NEW.`source_name`) OR NOT(OLD.`role` <=> NEW.`role`) OR NOT(OLD.`enabled` <=> NEW.`enabled`) THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,record_label,before_json,after_json,changed_fields)
  VALUES(NEW.tenant_id,UTC_TIMESTAMP(3),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,COALESCE(@roi_audit_actor,'0'),'0'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,@roi_audit_roles,'[]'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,@roi_audit_source,'database_trigger'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,@roi_audit_request,NULL),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,@roi_audit_path,NULL),'permission','update','roi_org_role_mapping',NEW.`id`,'组织岗位与主播权限',JSON_OBJECT('id',OLD.`id`,'source_type',OLD.`source_type`,'source_key',OLD.`source_key`,'source_name',OLD.`source_name`,'role',OLD.`role`,'enabled',OLD.`enabled`),JSON_OBJECT('id',NEW.`id`,'source_type',NEW.`source_type`,'source_key',NEW.`source_key`,'source_name',NEW.`source_name`,'role',NEW.`role`,'enabled',NEW.`enabled`),JSON_ARRAY('id','source_type','source_key','source_name','role','enabled'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS roi_org_role_mapping_d_audit_v1 AFTER DELETE ON roi_org_role_mapping FOR EACH ROW
BEGIN
 IF 1=1 THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,record_label,before_json,after_json,changed_fields)
  VALUES(OLD.tenant_id,UTC_TIMESTAMP(3),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=OLD.tenant_id,COALESCE(@roi_audit_actor,'0'),'0'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=OLD.tenant_id,COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=OLD.tenant_id,@roi_audit_roles,'[]'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=OLD.tenant_id,@roi_audit_source,'database_trigger'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=OLD.tenant_id,@roi_audit_request,NULL),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=OLD.tenant_id,@roi_audit_path,NULL),'permission','delete','roi_org_role_mapping',OLD.`id`,'组织岗位与主播权限',JSON_OBJECT('id',OLD.`id`,'source_type',OLD.`source_type`,'source_key',OLD.`source_key`,'source_name',OLD.`source_name`,'role',OLD.`role`,'enabled',OLD.`enabled`),NULL,JSON_ARRAY('id','source_type','source_key','source_name','role','enabled'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS roi_ip_group_scope_i_audit_v1 AFTER INSERT ON roi_ip_group_scope FOR EACH ROW
BEGIN
 IF 1=1 THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,record_label,before_json,after_json,changed_fields)
  VALUES(NEW.tenant_id,UTC_TIMESTAMP(3),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,COALESCE(@roi_audit_actor,'0'),'0'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,@roi_audit_roles,'[]'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,@roi_audit_source,'database_trigger'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,@roi_audit_request,NULL),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,@roi_audit_path,NULL),'permission','create','roi_ip_group_scope',CONCAT(NEW.user_id,':',NEW.ip_group_id),'组织岗位与主播权限',NULL,JSON_OBJECT('user_id',NEW.`user_id`,'ip_group_id',NEW.`ip_group_id`),JSON_ARRAY('user_id','ip_group_id'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS roi_ip_group_scope_u_audit_v1 AFTER UPDATE ON roi_ip_group_scope FOR EACH ROW
BEGIN
 IF NOT(OLD.`user_id` <=> NEW.`user_id`) OR NOT(OLD.`ip_group_id` <=> NEW.`ip_group_id`) THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,record_label,before_json,after_json,changed_fields)
  VALUES(NEW.tenant_id,UTC_TIMESTAMP(3),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,COALESCE(@roi_audit_actor,'0'),'0'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,@roi_audit_roles,'[]'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,@roi_audit_source,'database_trigger'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,@roi_audit_request,NULL),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,@roi_audit_path,NULL),'permission','update','roi_ip_group_scope',CONCAT(NEW.user_id,':',NEW.ip_group_id),'组织岗位与主播权限',JSON_OBJECT('user_id',OLD.`user_id`,'ip_group_id',OLD.`ip_group_id`),JSON_OBJECT('user_id',NEW.`user_id`,'ip_group_id',NEW.`ip_group_id`),JSON_ARRAY('user_id','ip_group_id'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS roi_ip_group_scope_d_audit_v1 AFTER DELETE ON roi_ip_group_scope FOR EACH ROW
BEGIN
 IF 1=1 THEN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,record_label,before_json,after_json,changed_fields)
  VALUES(OLD.tenant_id,UTC_TIMESTAMP(3),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=OLD.tenant_id,COALESCE(@roi_audit_actor,'0'),'0'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=OLD.tenant_id,COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=OLD.tenant_id,@roi_audit_roles,'[]'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=OLD.tenant_id,@roi_audit_source,'database_trigger'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=OLD.tenant_id,@roi_audit_request,NULL),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=OLD.tenant_id,@roi_audit_path,NULL),'permission','delete','roi_ip_group_scope',CONCAT(OLD.user_id,':',OLD.ip_group_id),'组织岗位与主播权限',JSON_OBJECT('user_id',OLD.`user_id`,'ip_group_id',OLD.`ip_group_id`),NULL,JSON_ARRAY('user_id','ip_group_id'));
 END IF;
END$$
DELIMITER ;

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
-- Order: after 20261026_roi_operation_logs.sql, then this file. Target: shenyu-ops.
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

DELIMITER $$
CREATE TRIGGER IF NOT EXISTS `roi_wfm_media_i_v1` AFTER INSERT ON `roi_wecom_media` FOR EACH ROW
BEGIN
 IF 1=1 THEN
 INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,before_json,after_json,changed_fields)
 VALUES(NEW.tenant_id,UTC_TIMESTAMP(3),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,COALESCE(@roi_audit_actor,'0'),'0'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,@roi_audit_roles,'[]'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,@roi_audit_source,'database_trigger'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,@roi_audit_request,NULL),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,@roi_audit_path,NULL),'wecom','create','roi_wecom_media',NEW.id,NEW.link_id,CONCAT('获客链接 ',NEW.link_id),NEW.author_id,NULL,JSON_OBJECT('id',NEW.`id`,'tenant_id',NEW.`tenant_id`,'corp_id',NEW.`corp_id`,'link_id',NEW.`link_id`,'author_id',NEW.`author_id`,'created_by',NEW.`created_by`,'file_name',roi_audit_text_v1(NEW.`file_name`),'content_type',NEW.`content_type`,'file_size',NEW.`file_size`,'media_type',NEW.`media_type`,'provider_status',NEW.`provider_status`,'provider_expires_at',NEW.`provider_expires_at`),JSON_ARRAY('id','tenant_id','corp_id','link_id','author_id','created_by','file_name','content_type','file_size','media_type','provider_status','provider_expires_at'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_wfm_media_u_v1` AFTER UPDATE ON `roi_wecom_media` FOR EACH ROW
BEGIN
 IF (NOT(OLD.`id` <=> NEW.`id`) OR NOT(OLD.`tenant_id` <=> NEW.`tenant_id`) OR NOT(OLD.`corp_id` <=> NEW.`corp_id`) OR NOT(OLD.`link_id` <=> NEW.`link_id`) OR NOT(OLD.`author_id` <=> NEW.`author_id`) OR NOT(OLD.`created_by` <=> NEW.`created_by`) OR NOT(OLD.`file_name` <=> NEW.`file_name`) OR NOT(OLD.`content_type` <=> NEW.`content_type`) OR NOT(OLD.`file_size` <=> NEW.`file_size`) OR NOT(OLD.`media_type` <=> NEW.`media_type`) OR NOT(OLD.`provider_status` <=> NEW.`provider_status`) OR NOT(OLD.`provider_expires_at` <=> NEW.`provider_expires_at`)) THEN
 INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,before_json,after_json,changed_fields)
 VALUES(NEW.tenant_id,UTC_TIMESTAMP(3),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,COALESCE(@roi_audit_actor,'0'),'0'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,@roi_audit_roles,'[]'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,@roi_audit_source,'database_trigger'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,@roi_audit_request,NULL),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,@roi_audit_path,NULL),'wecom','update','roi_wecom_media',NEW.id,NEW.link_id,CONCAT('获客链接 ',NEW.link_id),NEW.author_id,JSON_OBJECT('id',OLD.`id`,'tenant_id',OLD.`tenant_id`,'corp_id',OLD.`corp_id`,'link_id',OLD.`link_id`,'author_id',OLD.`author_id`,'created_by',OLD.`created_by`,'file_name',roi_audit_text_v1(OLD.`file_name`),'content_type',OLD.`content_type`,'file_size',OLD.`file_size`,'media_type',OLD.`media_type`,'provider_status',OLD.`provider_status`,'provider_expires_at',OLD.`provider_expires_at`),JSON_OBJECT('id',NEW.`id`,'tenant_id',NEW.`tenant_id`,'corp_id',NEW.`corp_id`,'link_id',NEW.`link_id`,'author_id',NEW.`author_id`,'created_by',NEW.`created_by`,'file_name',roi_audit_text_v1(NEW.`file_name`),'content_type',NEW.`content_type`,'file_size',NEW.`file_size`,'media_type',NEW.`media_type`,'provider_status',NEW.`provider_status`,'provider_expires_at',NEW.`provider_expires_at`),JSON_ARRAY('id','tenant_id','corp_id','link_id','author_id','created_by','file_name','content_type','file_size','media_type','provider_status','provider_expires_at'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_wfm_media_d_v1` AFTER DELETE ON `roi_wecom_media` FOR EACH ROW
BEGIN
 IF 1=1 THEN
 INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,before_json,after_json,changed_fields)
 VALUES(OLD.tenant_id,UTC_TIMESTAMP(3),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=OLD.tenant_id,COALESCE(@roi_audit_actor,'0'),'0'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=OLD.tenant_id,COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=OLD.tenant_id,@roi_audit_roles,'[]'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=OLD.tenant_id,@roi_audit_source,'database_trigger'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=OLD.tenant_id,@roi_audit_request,NULL),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=OLD.tenant_id,@roi_audit_path,NULL),'wecom','delete','roi_wecom_media',OLD.id,OLD.link_id,CONCAT('获客链接 ',OLD.link_id),OLD.author_id,JSON_OBJECT('id',OLD.`id`,'tenant_id',OLD.`tenant_id`,'corp_id',OLD.`corp_id`,'link_id',OLD.`link_id`,'author_id',OLD.`author_id`,'created_by',OLD.`created_by`,'file_name',roi_audit_text_v1(OLD.`file_name`),'content_type',OLD.`content_type`,'file_size',OLD.`file_size`,'media_type',OLD.`media_type`,'provider_status',OLD.`provider_status`,'provider_expires_at',OLD.`provider_expires_at`),NULL,JSON_ARRAY('id','tenant_id','corp_id','link_id','author_id','created_by','file_name','content_type','file_size','media_type','provider_status','provider_expires_at'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_wfm_link_operation_i_v1` AFTER INSERT ON `roi_wecom_link_operation` FOR EACH ROW
BEGIN
 IF 1=1 THEN
 INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,before_json,after_json,changed_fields)
 VALUES(NEW.tenant_id,UTC_TIMESTAMP(3),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,COALESCE(@roi_audit_actor,'0'),'0'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,@roi_audit_roles,'[]'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,@roi_audit_source,'database_trigger'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,@roi_audit_request,NULL),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,@roi_audit_path,NULL),'wecom','create','roi_wecom_link_operation',NEW.id,NEW.link_id,CONCAT('获客链接 ',NEW.link_id),(SELECT CAST(author_id AS CHAR) FROM `shenyu-wecom`.wecom_customer_acquisition_link WHERE tenant_id=NEW.tenant_id AND id=NEW.link_id LIMIT 1),NULL,JSON_OBJECT('id',NEW.`id`,'tenant_id',NEW.`tenant_id`,'link_id',NEW.`link_id`,'actor_id',NEW.`actor_id`,'request_id',NEW.`request_id`,'status',NEW.`status`),JSON_ARRAY('id','tenant_id','link_id','actor_id','request_id','status'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_wfm_link_operation_u_v1` AFTER UPDATE ON `roi_wecom_link_operation` FOR EACH ROW
BEGIN
 IF (NOT(OLD.`id` <=> NEW.`id`) OR NOT(OLD.`tenant_id` <=> NEW.`tenant_id`) OR NOT(OLD.`link_id` <=> NEW.`link_id`) OR NOT(OLD.`actor_id` <=> NEW.`actor_id`) OR NOT(OLD.`request_id` <=> NEW.`request_id`) OR NOT(OLD.`status` <=> NEW.`status`)) THEN
 INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,before_json,after_json,changed_fields)
 VALUES(NEW.tenant_id,UTC_TIMESTAMP(3),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,COALESCE(@roi_audit_actor,'0'),'0'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,@roi_audit_roles,'[]'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,@roi_audit_source,'database_trigger'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,@roi_audit_request,NULL),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,@roi_audit_path,NULL),'wecom','update','roi_wecom_link_operation',NEW.id,NEW.link_id,CONCAT('获客链接 ',NEW.link_id),(SELECT CAST(author_id AS CHAR) FROM `shenyu-wecom`.wecom_customer_acquisition_link WHERE tenant_id=NEW.tenant_id AND id=NEW.link_id LIMIT 1),JSON_OBJECT('id',OLD.`id`,'tenant_id',OLD.`tenant_id`,'link_id',OLD.`link_id`,'actor_id',OLD.`actor_id`,'request_id',OLD.`request_id`,'status',OLD.`status`),JSON_OBJECT('id',NEW.`id`,'tenant_id',NEW.`tenant_id`,'link_id',NEW.`link_id`,'actor_id',NEW.`actor_id`,'request_id',NEW.`request_id`,'status',NEW.`status`),JSON_ARRAY('id','tenant_id','link_id','actor_id','request_id','status'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_wfm_link_operation_d_v1` AFTER DELETE ON `roi_wecom_link_operation` FOR EACH ROW
BEGIN
 IF 1=1 THEN
 INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,before_json,after_json,changed_fields)
 VALUES(OLD.tenant_id,UTC_TIMESTAMP(3),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=OLD.tenant_id,COALESCE(@roi_audit_actor,'0'),'0'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=OLD.tenant_id,COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=OLD.tenant_id,@roi_audit_roles,'[]'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=OLD.tenant_id,@roi_audit_source,'database_trigger'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=OLD.tenant_id,@roi_audit_request,NULL),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=OLD.tenant_id,@roi_audit_path,NULL),'wecom','delete','roi_wecom_link_operation',OLD.id,OLD.link_id,CONCAT('获客链接 ',OLD.link_id),(SELECT CAST(author_id AS CHAR) FROM `shenyu-wecom`.wecom_customer_acquisition_link WHERE tenant_id=OLD.tenant_id AND id=OLD.link_id LIMIT 1),JSON_OBJECT('id',OLD.`id`,'tenant_id',OLD.`tenant_id`,'link_id',OLD.`link_id`,'actor_id',OLD.`actor_id`,'request_id',OLD.`request_id`,'status',OLD.`status`),NULL,JSON_ARRAY('id','tenant_id','link_id','actor_id','request_id','status'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_wfm_link_owner_history_i_v1` AFTER INSERT ON `roi_wecom_link_owner_history` FOR EACH ROW
BEGIN
 IF 1=1 THEN
 INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,before_json,after_json,changed_fields)
 VALUES(NEW.tenant_id,UTC_TIMESTAMP(3),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,COALESCE(@roi_audit_actor,'0'),'0'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,@roi_audit_roles,'[]'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,@roi_audit_source,'database_trigger'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,@roi_audit_request,NULL),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,@roi_audit_path,NULL),'wecom','create','roi_wecom_link_owner_history',NEW.id,NEW.link_id,CONCAT('获客链接 ',NEW.link_id),NEW.author_id,NULL,JSON_OBJECT('id',NEW.`id`,'tenant_id',NEW.`tenant_id`,'corp_id',NEW.`corp_id`,'link_id',NEW.`link_id`,'author_id',NEW.`author_id`,'effective_from',NEW.`effective_from`,'effective_to',NEW.`effective_to`,'created_by',NEW.`created_by`),JSON_ARRAY('id','tenant_id','corp_id','link_id','author_id','effective_from','effective_to','created_by'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_wfm_link_owner_history_u_v1` AFTER UPDATE ON `roi_wecom_link_owner_history` FOR EACH ROW
BEGIN
 IF (NOT(OLD.`id` <=> NEW.`id`) OR NOT(OLD.`tenant_id` <=> NEW.`tenant_id`) OR NOT(OLD.`corp_id` <=> NEW.`corp_id`) OR NOT(OLD.`link_id` <=> NEW.`link_id`) OR NOT(OLD.`author_id` <=> NEW.`author_id`) OR NOT(OLD.`effective_from` <=> NEW.`effective_from`) OR NOT(OLD.`effective_to` <=> NEW.`effective_to`) OR NOT(OLD.`created_by` <=> NEW.`created_by`)) THEN
 INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,before_json,after_json,changed_fields)
 VALUES(NEW.tenant_id,UTC_TIMESTAMP(3),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,COALESCE(@roi_audit_actor,'0'),'0'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,@roi_audit_roles,'[]'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,@roi_audit_source,'database_trigger'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,@roi_audit_request,NULL),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=NEW.tenant_id,@roi_audit_path,NULL),'wecom','update','roi_wecom_link_owner_history',NEW.id,NEW.link_id,CONCAT('获客链接 ',NEW.link_id),NEW.author_id,JSON_OBJECT('id',OLD.`id`,'tenant_id',OLD.`tenant_id`,'corp_id',OLD.`corp_id`,'link_id',OLD.`link_id`,'author_id',OLD.`author_id`,'effective_from',OLD.`effective_from`,'effective_to',OLD.`effective_to`,'created_by',OLD.`created_by`),JSON_OBJECT('id',NEW.`id`,'tenant_id',NEW.`tenant_id`,'corp_id',NEW.`corp_id`,'link_id',NEW.`link_id`,'author_id',NEW.`author_id`,'effective_from',NEW.`effective_from`,'effective_to',NEW.`effective_to`,'created_by',NEW.`created_by`),JSON_ARRAY('id','tenant_id','corp_id','link_id','author_id','effective_from','effective_to','created_by'));
 END IF;
END$$
CREATE TRIGGER IF NOT EXISTS `roi_wfm_link_owner_history_d_v1` AFTER DELETE ON `roi_wecom_link_owner_history` FOR EACH ROW
BEGIN
 IF 1=1 THEN
 INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,before_json,after_json,changed_fields)
 VALUES(OLD.tenant_id,UTC_TIMESTAMP(3),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=OLD.tenant_id,COALESCE(@roi_audit_actor,'0'),'0'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=OLD.tenant_id,COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=OLD.tenant_id,@roi_audit_roles,'[]'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=OLD.tenant_id,@roi_audit_source,'database_trigger'),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=OLD.tenant_id,@roi_audit_request,NULL),IF(COALESCE(@roi_audit_enabled,0)=1 AND @roi_audit_tenant=OLD.tenant_id,@roi_audit_path,NULL),'wecom','delete','roi_wecom_link_owner_history',OLD.id,OLD.link_id,CONCAT('获客链接 ',OLD.link_id),OLD.author_id,JSON_OBJECT('id',OLD.`id`,'tenant_id',OLD.`tenant_id`,'corp_id',OLD.`corp_id`,'link_id',OLD.`link_id`,'author_id',OLD.`author_id`,'effective_from',OLD.`effective_from`,'effective_to',OLD.`effective_to`,'created_by',OLD.`created_by`),NULL,JSON_ARRAY('id','tenant_id','corp_id','link_id','author_id','effective_from','effective_to','created_by'));
 END IF;
END$$
DELIMITER ;

-- =============================================================================
-- Section 08: IP 审计投影函数增量
-- SSOT: 20261026_roi_ip_audit_projection.sql
-- =============================================================================

-- 开播 IP 结果的审计快照投影增量（不修改旧迁移，不回填或删除日志）
-- 前置：20261026_roi_operation_logs.sql；使用原 ROI 业务库 shenyu-ops。
-- 必须先短暂停止 football-selfhost 及其他会写 ROI 台账的任务，再执行本脚本，随后启动服务。
-- DROP/CREATE FUNCTION 为 MySQL DDL，不能用普通事务消除函数重建间隙；执行中不得并发写 ROI。
-- 可重复执行；只替换函数定义，保留表、业务数据、日志及原有触发器。
-- 验证：SELECT roi_audit_leaf_v1(JSON_OBJECT('ipOutcome','banned','publicIp','203.0.113.10'));
-- 应包含真实 ipOutcome 与 publicIp；completionNote、banObservation 自由正文不进入快照。
-- 回退：同样暂停相关写入后执行 20261026_roi_ip_audit_projection_rollback.sql。
DELIMITER $$
DROP FUNCTION IF EXISTS roi_audit_leaf_v1$$
CREATE FUNCTION roi_audit_leaf_v1(p LONGTEXT) RETURNS LONGTEXT DETERMINISTIC NO SQL
BEGIN
 DECLARE result LONGTEXT DEFAULT '{}'; DECLARE allowed LONGTEXT; DECLARE k VARCHAR(100); DECLARE v LONGTEXT; DECLARE i INT DEFAULT 0;
 IF p IS NULL OR NOT JSON_VALID(p) OR JSON_TYPE(JSON_EXTRACT(p,'$'))<>'OBJECT' THEN RETURN '{}'; END IF;
 SET allowed=JSON_ARRAY('id','kind','businessKey','authorId','name','platform','platformId','masterAccountId','status','price','purchaseDate','purchaseNo','mode','channelId','accountId','sessionId','sessionNo','displayName','date','startTime','endTime','expenseNo','category','amount','occurredDate','recognizedAmount','paidAmount','refundedAmount','reviewerId','reviewerName','reviewedAt','quantity','fishCount','publicIp','prefix','policy','metricSource','fishCountSource','approvalRecordId','approvalInstanceId','approvalStatus','approvalResult','type','accountVersion','effectiveAt','companyId','blueV','validFrom','validTo','originalValidTo','accountNo','costType','period','costDate','costTypeName','project','state','matchState','suggestedAccountId','suggestedAuthorId','authorSuggestionSource','sourceMissing','stableRow','confirmedBy','confirmedAt','confirmedAmount','excludedBy','excludedAt','reversedBy','reversedAt','newCostCount','payDate','payMethod','registeredBy','registeredAt','approvalBusinessId','originatorName','rowKey','amountState','existingCostId','costId','linkType','totalAmount','refundAmount','expenseId','action','verified','sourceKind','sourceType','sourceId','source','selected','sessionStart','sessionEnd','version','hasArticleWindow','peakOnline','realOnline','avgOnline','avgStaySeconds','enterUsers','exposureUsers','pinwheelExposure','pinwheelClick','pinwheelCtr','commentUsers','likes','newFans','shareCount','fanClubUsers','wecomAdded','platformFollows','serviceFollows','articleViews','reservations','beidanFans','income','giftUsers','giftRate','giftIncome','memberIncome','guardIncome','predictedIncome','permanent','start','end','endedAt','appliedAmount','actualAmount','remainingAmount','expectedAmount','settledAmount','settlementDate','settlementNo','lastStatus','active','fromAuthorId','toAuthorId','attachmentId','authorName','originatorBackendId','originatorDingtalkId','total','nativeState','providerStatus','providerResult','sourceChangeState','latestHash','observedAt','latestApprovalStatus','latestApprovalResult','originalName','urlState','approvedCount','rejectedCount','selectedAuthorId','selectedProject','selectedCategory','liveBanStatus','allocationIndex','matchedRowCount','exposure','entrants','shares','fanClub','pinwheelImpressions','pinwheelClicks','wecomAdds','subscriptionFollows','summaryViewers','readCount','readUsers','paidUsers','repeatUsers','repeatAmount','payValue','repeatValue','batchNo','paymentId','actualRecordId','refundRecordId','actualOccurredDate','verifiedBy','verifiedAt','scope','enabled','linkId','corpId','proofRecordId','clearingId','clearingNo','settledBy','settledAt','submittedBy','submittedAt','preparedBy','preparedAt','requestId','requestHash','comparisonStatus','ipEntryRequestId','ipSavedBy','ipSavedAt','ipSource','ipOutcome','banObservedAt','completedBy','completedAt');
 WHILE i<JSON_LENGTH(allowed) DO
  SET k=JSON_UNQUOTE(JSON_EXTRACT(allowed,CONCAT('$[',i,']')));
  SET v=JSON_EXTRACT(p,CONCAT('$."',k,'"'));
  IF v IS NOT NULL AND JSON_TYPE(v) IN ('STRING','INTEGER','DOUBLE','DECIMAL','BOOLEAN','NULL') THEN
   IF JSON_TYPE(v)='STRING' THEN SET result=JSON_SET(result,CONCAT('$."',k,'"'),roi_audit_text_v1(JSON_UNQUOTE(v)));
   ELSE SET result=JSON_SET(result,CONCAT('$."',k,'"'),JSON_EXTRACT(v,'$')); END IF;
  END IF;
  SET i=i+1;
 END WHILE;
 RETURN result;
END$$
DELIMITER ;

-- =============================================================================
-- Section 09: 作者/运营责任 + 费用来源/归属
-- SSOT: roi_author_cost_foundation_20261029.sql
-- =============================================================================

-- Author/operator assignment and cost-source relation foundation, 2026-10-29.
-- Target: the OPS business database `shenyu-ops`; MySQL >= 8.0.29.
-- Order: existing ROI/OA-cost schema -> 20261026_roi_operation_logs.sql -> this script -> matching Java.
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
-- Prerequisite: 20261026_roi_operation_logs.sql and MySQL >= 8.0.29.
DELIMITER $$
CREATE TRIGGER IF NOT EXISTS `roi_log_r_author_operator_i_20261029` AFTER INSERT ON `roi_author_operator_assignment` FOR EACH ROW
BEGIN
    INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,
      module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,
      before_json,after_json,changed_fields)
    VALUES (NEW.tenant_id,UTC_TIMESTAMP(3),
      IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),
      IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),
      IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_roles,'[]'),
      IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),
      IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_request,NULL),
      IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_path,NULL),
      'account',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'create'),
      'roi_author_operator_assignment',CAST(NEW.id AS CHAR),NULL,LEFT(CONCAT('作者运营责任#',CAST(NEW.author_id AS CHAR)),255),CAST(NEW.author_id AS CHAR),NULL,
      NULL,
      JSON_OBJECT('id',CAST(NEW.`id` AS CHAR),'tenant_id',CAST(NEW.`tenant_id` AS CHAR),'author_id',CAST(NEW.`author_id` AS CHAR),'operator_user_id',CAST(NEW.`operator_user_id` AS CHAR),'starts_at',CAST(NEW.`starts_at` AS CHAR),'ends_at',CAST(NEW.`ends_at` AS CHAR),'state',NEW.`state`,'version',NEW.`version`,'created_by',CAST(NEW.`created_by` AS CHAR),'updated_by',CAST(NEW.`updated_by` AS CHAR),'created_at',CAST(NEW.`created_at` AS CHAR),'updated_at',CAST(NEW.`updated_at` AS CHAR)),
      JSON_ARRAY('id','tenant_id','author_id','operator_user_id','starts_at','ends_at','state','version','created_by','updated_by','created_at','updated_at','reason','evidence'));
END$$

CREATE TRIGGER IF NOT EXISTS `roi_log_r_author_operator_u_20261029` AFTER UPDATE ON `roi_author_operator_assignment` FOR EACH ROW
BEGIN
  IF NOT(OLD.`id` <=> NEW.`id`) OR NOT(OLD.`tenant_id` <=> NEW.`tenant_id`) OR NOT(OLD.`author_id` <=> NEW.`author_id`) OR NOT(OLD.`operator_user_id` <=> NEW.`operator_user_id`) OR NOT(OLD.`starts_at` <=> NEW.`starts_at`) OR NOT(OLD.`ends_at` <=> NEW.`ends_at`) OR NOT(OLD.`state` <=> NEW.`state`) OR NOT(OLD.`version` <=> NEW.`version`) OR NOT(OLD.`created_by` <=> NEW.`created_by`) OR NOT(OLD.`updated_by` <=> NEW.`updated_by`) OR NOT(OLD.`created_at` <=> NEW.`created_at`) OR NOT(OLD.`updated_at` <=> NEW.`updated_at`) OR NOT(OLD.`reason` <=> NEW.`reason`) OR NOT(OLD.`evidence` <=> NEW.`evidence`) THEN
    INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,
      module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,
      before_json,after_json,changed_fields)
    VALUES (NEW.tenant_id,UTC_TIMESTAMP(3),
      IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),
      IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),
      IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_roles,'[]'),
      IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),
      IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_request,NULL),
      IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_path,NULL),
      'account',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'update'),
      'roi_author_operator_assignment',CAST(NEW.id AS CHAR),NULL,LEFT(CONCAT('作者运营责任#',CAST(NEW.author_id AS CHAR)),255),CAST(NEW.author_id AS CHAR),CAST(OLD.author_id AS CHAR),
      JSON_OBJECT('id',CAST(OLD.`id` AS CHAR),'tenant_id',CAST(OLD.`tenant_id` AS CHAR),'author_id',CAST(OLD.`author_id` AS CHAR),'operator_user_id',CAST(OLD.`operator_user_id` AS CHAR),'starts_at',CAST(OLD.`starts_at` AS CHAR),'ends_at',CAST(OLD.`ends_at` AS CHAR),'state',OLD.`state`,'version',OLD.`version`,'created_by',CAST(OLD.`created_by` AS CHAR),'updated_by',CAST(OLD.`updated_by` AS CHAR),'created_at',CAST(OLD.`created_at` AS CHAR),'updated_at',CAST(OLD.`updated_at` AS CHAR)),
      JSON_OBJECT('id',CAST(NEW.`id` AS CHAR),'tenant_id',CAST(NEW.`tenant_id` AS CHAR),'author_id',CAST(NEW.`author_id` AS CHAR),'operator_user_id',CAST(NEW.`operator_user_id` AS CHAR),'starts_at',CAST(NEW.`starts_at` AS CHAR),'ends_at',CAST(NEW.`ends_at` AS CHAR),'state',NEW.`state`,'version',NEW.`version`,'created_by',CAST(NEW.`created_by` AS CHAR),'updated_by',CAST(NEW.`updated_by` AS CHAR),'created_at',CAST(NEW.`created_at` AS CHAR),'updated_at',CAST(NEW.`updated_at` AS CHAR)),
      JSON_MERGE_PRESERVE(IF(NOT(OLD.`id` <=> NEW.`id`),JSON_ARRAY('id'),JSON_ARRAY()),IF(NOT(OLD.`tenant_id` <=> NEW.`tenant_id`),JSON_ARRAY('tenant_id'),JSON_ARRAY()),IF(NOT(OLD.`author_id` <=> NEW.`author_id`),JSON_ARRAY('author_id'),JSON_ARRAY()),IF(NOT(OLD.`operator_user_id` <=> NEW.`operator_user_id`),JSON_ARRAY('operator_user_id'),JSON_ARRAY()),IF(NOT(OLD.`starts_at` <=> NEW.`starts_at`),JSON_ARRAY('starts_at'),JSON_ARRAY()),IF(NOT(OLD.`ends_at` <=> NEW.`ends_at`),JSON_ARRAY('ends_at'),JSON_ARRAY()),IF(NOT(OLD.`state` <=> NEW.`state`),JSON_ARRAY('state'),JSON_ARRAY()),IF(NOT(OLD.`version` <=> NEW.`version`),JSON_ARRAY('version'),JSON_ARRAY()),IF(NOT(OLD.`created_by` <=> NEW.`created_by`),JSON_ARRAY('created_by'),JSON_ARRAY()),IF(NOT(OLD.`updated_by` <=> NEW.`updated_by`),JSON_ARRAY('updated_by'),JSON_ARRAY()),IF(NOT(OLD.`created_at` <=> NEW.`created_at`),JSON_ARRAY('created_at'),JSON_ARRAY()),IF(NOT(OLD.`updated_at` <=> NEW.`updated_at`),JSON_ARRAY('updated_at'),JSON_ARRAY()),IF(NOT(OLD.`reason` <=> NEW.`reason`),JSON_ARRAY('reason'),JSON_ARRAY()),IF(NOT(OLD.`evidence` <=> NEW.`evidence`),JSON_ARRAY('evidence'),JSON_ARRAY())));
  END IF;
END$$

CREATE TRIGGER IF NOT EXISTS `roi_log_r_account_operator_i_20261029` AFTER INSERT ON `roi_account_operator_assignment` FOR EACH ROW
BEGIN
    INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,
      module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,
      before_json,after_json,changed_fields)
    VALUES (NEW.tenant_id,UTC_TIMESTAMP(3),
      IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),
      IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),
      IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_roles,'[]'),
      IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),
      IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_request,NULL),
      IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_path,NULL),
      'account',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'create'),
      'roi_account_operator_assignment',CAST(NEW.id AS CHAR),CAST(NEW.account_id AS CHAR),LEFT(CONCAT('账号领用#',CAST(NEW.account_id AS CHAR)),255),CAST(NEW.author_id AS CHAR),NULL,
      NULL,
      JSON_OBJECT('id',CAST(NEW.`id` AS CHAR),'tenant_id',CAST(NEW.`tenant_id` AS CHAR),'account_id',CAST(NEW.`account_id` AS CHAR),'author_assignment_id',CAST(NEW.`author_assignment_id` AS CHAR),'author_id',CAST(NEW.`author_id` AS CHAR),'operator_user_id',CAST(NEW.`operator_user_id` AS CHAR),'starts_at',CAST(NEW.`starts_at` AS CHAR),'ends_at',CAST(NEW.`ends_at` AS CHAR),'state',NEW.`state`,'version',NEW.`version`,'created_by',CAST(NEW.`created_by` AS CHAR),'updated_by',CAST(NEW.`updated_by` AS CHAR),'created_at',CAST(NEW.`created_at` AS CHAR),'updated_at',CAST(NEW.`updated_at` AS CHAR)),
      JSON_ARRAY('id','tenant_id','account_id','author_assignment_id','author_id','operator_user_id','starts_at','ends_at','state','version','created_by','updated_by','created_at','updated_at','reason','evidence'));
END$$

CREATE TRIGGER IF NOT EXISTS `roi_log_r_account_operator_u_20261029` AFTER UPDATE ON `roi_account_operator_assignment` FOR EACH ROW
BEGIN
  IF NOT(OLD.`id` <=> NEW.`id`) OR NOT(OLD.`tenant_id` <=> NEW.`tenant_id`) OR NOT(OLD.`account_id` <=> NEW.`account_id`) OR NOT(OLD.`author_assignment_id` <=> NEW.`author_assignment_id`) OR NOT(OLD.`author_id` <=> NEW.`author_id`) OR NOT(OLD.`operator_user_id` <=> NEW.`operator_user_id`) OR NOT(OLD.`starts_at` <=> NEW.`starts_at`) OR NOT(OLD.`ends_at` <=> NEW.`ends_at`) OR NOT(OLD.`state` <=> NEW.`state`) OR NOT(OLD.`version` <=> NEW.`version`) OR NOT(OLD.`created_by` <=> NEW.`created_by`) OR NOT(OLD.`updated_by` <=> NEW.`updated_by`) OR NOT(OLD.`created_at` <=> NEW.`created_at`) OR NOT(OLD.`updated_at` <=> NEW.`updated_at`) OR NOT(OLD.`reason` <=> NEW.`reason`) OR NOT(OLD.`evidence` <=> NEW.`evidence`) THEN
    INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,
      module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,
      before_json,after_json,changed_fields)
    VALUES (NEW.tenant_id,UTC_TIMESTAMP(3),
      IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),
      IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),
      IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_roles,'[]'),
      IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),
      IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_request,NULL),
      IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_path,NULL),
      'account',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'update'),
      'roi_account_operator_assignment',CAST(NEW.id AS CHAR),CAST(NEW.account_id AS CHAR),LEFT(CONCAT('账号领用#',CAST(NEW.account_id AS CHAR)),255),CAST(NEW.author_id AS CHAR),CAST(OLD.author_id AS CHAR),
      JSON_OBJECT('id',CAST(OLD.`id` AS CHAR),'tenant_id',CAST(OLD.`tenant_id` AS CHAR),'account_id',CAST(OLD.`account_id` AS CHAR),'author_assignment_id',CAST(OLD.`author_assignment_id` AS CHAR),'author_id',CAST(OLD.`author_id` AS CHAR),'operator_user_id',CAST(OLD.`operator_user_id` AS CHAR),'starts_at',CAST(OLD.`starts_at` AS CHAR),'ends_at',CAST(OLD.`ends_at` AS CHAR),'state',OLD.`state`,'version',OLD.`version`,'created_by',CAST(OLD.`created_by` AS CHAR),'updated_by',CAST(OLD.`updated_by` AS CHAR),'created_at',CAST(OLD.`created_at` AS CHAR),'updated_at',CAST(OLD.`updated_at` AS CHAR)),
      JSON_OBJECT('id',CAST(NEW.`id` AS CHAR),'tenant_id',CAST(NEW.`tenant_id` AS CHAR),'account_id',CAST(NEW.`account_id` AS CHAR),'author_assignment_id',CAST(NEW.`author_assignment_id` AS CHAR),'author_id',CAST(NEW.`author_id` AS CHAR),'operator_user_id',CAST(NEW.`operator_user_id` AS CHAR),'starts_at',CAST(NEW.`starts_at` AS CHAR),'ends_at',CAST(NEW.`ends_at` AS CHAR),'state',NEW.`state`,'version',NEW.`version`,'created_by',CAST(NEW.`created_by` AS CHAR),'updated_by',CAST(NEW.`updated_by` AS CHAR),'created_at',CAST(NEW.`created_at` AS CHAR),'updated_at',CAST(NEW.`updated_at` AS CHAR)),
      JSON_MERGE_PRESERVE(IF(NOT(OLD.`id` <=> NEW.`id`),JSON_ARRAY('id'),JSON_ARRAY()),IF(NOT(OLD.`tenant_id` <=> NEW.`tenant_id`),JSON_ARRAY('tenant_id'),JSON_ARRAY()),IF(NOT(OLD.`account_id` <=> NEW.`account_id`),JSON_ARRAY('account_id'),JSON_ARRAY()),IF(NOT(OLD.`author_assignment_id` <=> NEW.`author_assignment_id`),JSON_ARRAY('author_assignment_id'),JSON_ARRAY()),IF(NOT(OLD.`author_id` <=> NEW.`author_id`),JSON_ARRAY('author_id'),JSON_ARRAY()),IF(NOT(OLD.`operator_user_id` <=> NEW.`operator_user_id`),JSON_ARRAY('operator_user_id'),JSON_ARRAY()),IF(NOT(OLD.`starts_at` <=> NEW.`starts_at`),JSON_ARRAY('starts_at'),JSON_ARRAY()),IF(NOT(OLD.`ends_at` <=> NEW.`ends_at`),JSON_ARRAY('ends_at'),JSON_ARRAY()),IF(NOT(OLD.`state` <=> NEW.`state`),JSON_ARRAY('state'),JSON_ARRAY()),IF(NOT(OLD.`version` <=> NEW.`version`),JSON_ARRAY('version'),JSON_ARRAY()),IF(NOT(OLD.`created_by` <=> NEW.`created_by`),JSON_ARRAY('created_by'),JSON_ARRAY()),IF(NOT(OLD.`updated_by` <=> NEW.`updated_by`),JSON_ARRAY('updated_by'),JSON_ARRAY()),IF(NOT(OLD.`created_at` <=> NEW.`created_at`),JSON_ARRAY('created_at'),JSON_ARRAY()),IF(NOT(OLD.`updated_at` <=> NEW.`updated_at`),JSON_ARRAY('updated_at'),JSON_ARRAY()),IF(NOT(OLD.`reason` <=> NEW.`reason`),JSON_ARRAY('reason'),JSON_ARRAY()),IF(NOT(OLD.`evidence` <=> NEW.`evidence`),JSON_ARRAY('evidence'),JSON_ARRAY())));
  END IF;
END$$

CREATE TRIGGER IF NOT EXISTS `roi_log_r_cost_source_i_20261029` AFTER INSERT ON `roi_cost_source` FOR EACH ROW
BEGIN
    INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,
      module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,
      before_json,after_json,changed_fields)
    VALUES (NEW.tenant_id,UTC_TIMESTAMP(3),
      IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),
      IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),
      IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_roles,'[]'),
      IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),
      IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_request,NULL),
      IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_path,NULL),
      'finance',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'create'),
      'roi_cost_source',CAST(NEW.id AS CHAR),CAST(NEW.source_id AS CHAR),LEFT(CONCAT('费用来源#',NEW.source_type,':',NEW.source_id),255),NULL,NULL,
      NULL,
      JSON_OBJECT('id',CAST(NEW.`id` AS CHAR),'tenant_id',CAST(NEW.`tenant_id` AS CHAR),'source_type',NEW.`source_type`,'source_id',CAST(NEW.`source_id` AS CHAR),'canonical_type',NEW.`canonical_type`,'canonical_id',CAST(NEW.`canonical_id` AS CHAR),'source_hash',NEW.`source_hash`,'canonical_hash',NEW.`canonical_hash`,'state',NEW.`state`,'version',NEW.`version`,'created_by',CAST(NEW.`created_by` AS CHAR),'updated_by',CAST(NEW.`updated_by` AS CHAR),'created_at',CAST(NEW.`created_at` AS CHAR),'updated_at',CAST(NEW.`updated_at` AS CHAR)),
      JSON_ARRAY('id','tenant_id','source_type','source_id','canonical_type','canonical_id','source_hash','canonical_hash','state','version','created_by','updated_by','created_at','updated_at','reason'));
END$$

CREATE TRIGGER IF NOT EXISTS `roi_log_r_cost_source_u_20261029` AFTER UPDATE ON `roi_cost_source` FOR EACH ROW
BEGIN
  IF NOT(OLD.`id` <=> NEW.`id`) OR NOT(OLD.`tenant_id` <=> NEW.`tenant_id`) OR NOT(OLD.`source_type` <=> NEW.`source_type`) OR NOT(OLD.`source_id` <=> NEW.`source_id`) OR NOT(OLD.`canonical_type` <=> NEW.`canonical_type`) OR NOT(OLD.`canonical_id` <=> NEW.`canonical_id`) OR NOT(OLD.`source_hash` <=> NEW.`source_hash`) OR NOT(OLD.`canonical_hash` <=> NEW.`canonical_hash`) OR NOT(OLD.`state` <=> NEW.`state`) OR NOT(OLD.`version` <=> NEW.`version`) OR NOT(OLD.`created_by` <=> NEW.`created_by`) OR NOT(OLD.`updated_by` <=> NEW.`updated_by`) OR NOT(OLD.`created_at` <=> NEW.`created_at`) OR NOT(OLD.`updated_at` <=> NEW.`updated_at`) OR NOT(OLD.`reason` <=> NEW.`reason`) THEN
    INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,
      module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,
      before_json,after_json,changed_fields)
    VALUES (NEW.tenant_id,UTC_TIMESTAMP(3),
      IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),
      IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),
      IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_roles,'[]'),
      IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),
      IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_request,NULL),
      IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_path,NULL),
      'finance',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'update'),
      'roi_cost_source',CAST(NEW.id AS CHAR),CAST(NEW.source_id AS CHAR),LEFT(CONCAT('费用来源#',NEW.source_type,':',NEW.source_id),255),NULL,NULL,
      JSON_OBJECT('id',CAST(OLD.`id` AS CHAR),'tenant_id',CAST(OLD.`tenant_id` AS CHAR),'source_type',OLD.`source_type`,'source_id',CAST(OLD.`source_id` AS CHAR),'canonical_type',OLD.`canonical_type`,'canonical_id',CAST(OLD.`canonical_id` AS CHAR),'source_hash',OLD.`source_hash`,'canonical_hash',OLD.`canonical_hash`,'state',OLD.`state`,'version',OLD.`version`,'created_by',CAST(OLD.`created_by` AS CHAR),'updated_by',CAST(OLD.`updated_by` AS CHAR),'created_at',CAST(OLD.`created_at` AS CHAR),'updated_at',CAST(OLD.`updated_at` AS CHAR)),
      JSON_OBJECT('id',CAST(NEW.`id` AS CHAR),'tenant_id',CAST(NEW.`tenant_id` AS CHAR),'source_type',NEW.`source_type`,'source_id',CAST(NEW.`source_id` AS CHAR),'canonical_type',NEW.`canonical_type`,'canonical_id',CAST(NEW.`canonical_id` AS CHAR),'source_hash',NEW.`source_hash`,'canonical_hash',NEW.`canonical_hash`,'state',NEW.`state`,'version',NEW.`version`,'created_by',CAST(NEW.`created_by` AS CHAR),'updated_by',CAST(NEW.`updated_by` AS CHAR),'created_at',CAST(NEW.`created_at` AS CHAR),'updated_at',CAST(NEW.`updated_at` AS CHAR)),
      JSON_MERGE_PRESERVE(IF(NOT(OLD.`id` <=> NEW.`id`),JSON_ARRAY('id'),JSON_ARRAY()),IF(NOT(OLD.`tenant_id` <=> NEW.`tenant_id`),JSON_ARRAY('tenant_id'),JSON_ARRAY()),IF(NOT(OLD.`source_type` <=> NEW.`source_type`),JSON_ARRAY('source_type'),JSON_ARRAY()),IF(NOT(OLD.`source_id` <=> NEW.`source_id`),JSON_ARRAY('source_id'),JSON_ARRAY()),IF(NOT(OLD.`canonical_type` <=> NEW.`canonical_type`),JSON_ARRAY('canonical_type'),JSON_ARRAY()),IF(NOT(OLD.`canonical_id` <=> NEW.`canonical_id`),JSON_ARRAY('canonical_id'),JSON_ARRAY()),IF(NOT(OLD.`source_hash` <=> NEW.`source_hash`),JSON_ARRAY('source_hash'),JSON_ARRAY()),IF(NOT(OLD.`canonical_hash` <=> NEW.`canonical_hash`),JSON_ARRAY('canonical_hash'),JSON_ARRAY()),IF(NOT(OLD.`state` <=> NEW.`state`),JSON_ARRAY('state'),JSON_ARRAY()),IF(NOT(OLD.`version` <=> NEW.`version`),JSON_ARRAY('version'),JSON_ARRAY()),IF(NOT(OLD.`created_by` <=> NEW.`created_by`),JSON_ARRAY('created_by'),JSON_ARRAY()),IF(NOT(OLD.`updated_by` <=> NEW.`updated_by`),JSON_ARRAY('updated_by'),JSON_ARRAY()),IF(NOT(OLD.`created_at` <=> NEW.`created_at`),JSON_ARRAY('created_at'),JSON_ARRAY()),IF(NOT(OLD.`updated_at` <=> NEW.`updated_at`),JSON_ARRAY('updated_at'),JSON_ARRAY()),IF(NOT(OLD.`reason` <=> NEW.`reason`),JSON_ARRAY('reason'),JSON_ARRAY())));
  END IF;
END$$

CREATE TRIGGER IF NOT EXISTS `roi_log_r_cost_attribution_i_20261029` AFTER INSERT ON `roi_cost_attribution` FOR EACH ROW
BEGIN
    INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,
      module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,
      before_json,after_json,changed_fields)
    VALUES (NEW.tenant_id,UTC_TIMESTAMP(3),
      IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),
      IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),
      IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_roles,'[]'),
      IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),
      IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_request,NULL),
      IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_path,NULL),
      'finance',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'create'),
      'roi_cost_attribution',CAST(NEW.id AS CHAR),CAST(NEW.source_id AS CHAR),LEFT(CONCAT('费用归属#',NEW.source_type,':',NEW.source_id),255),CAST(NEW.author_id AS CHAR),NULL,
      NULL,
      JSON_OBJECT('id',CAST(NEW.`id` AS CHAR),'tenant_id',CAST(NEW.`tenant_id` AS CHAR),'source_type',NEW.`source_type`,'source_id',CAST(NEW.`source_id` AS CHAR),'occurred_at',CAST(NEW.`occurred_at` AS CHAR),'operator_user_id',CAST(NEW.`operator_user_id` AS CHAR),'author_id',CAST(NEW.`author_id` AS CHAR),'source_hash',NEW.`source_hash`,'version',NEW.`version`,'created_by',CAST(NEW.`created_by` AS CHAR),'updated_by',CAST(NEW.`updated_by` AS CHAR),'created_at',CAST(NEW.`created_at` AS CHAR),'updated_at',CAST(NEW.`updated_at` AS CHAR)),
      JSON_ARRAY('id','tenant_id','source_type','source_id','occurred_at','operator_user_id','author_id','source_hash','version','created_by','updated_by','created_at','updated_at','reason','evidence'));
END$$

CREATE TRIGGER IF NOT EXISTS `roi_log_r_cost_attribution_u_20261029` AFTER UPDATE ON `roi_cost_attribution` FOR EACH ROW
BEGIN
  IF NOT(OLD.`id` <=> NEW.`id`) OR NOT(OLD.`tenant_id` <=> NEW.`tenant_id`) OR NOT(OLD.`source_type` <=> NEW.`source_type`) OR NOT(OLD.`source_id` <=> NEW.`source_id`) OR NOT(OLD.`occurred_at` <=> NEW.`occurred_at`) OR NOT(OLD.`operator_user_id` <=> NEW.`operator_user_id`) OR NOT(OLD.`author_id` <=> NEW.`author_id`) OR NOT(OLD.`source_hash` <=> NEW.`source_hash`) OR NOT(OLD.`version` <=> NEW.`version`) OR NOT(OLD.`created_by` <=> NEW.`created_by`) OR NOT(OLD.`updated_by` <=> NEW.`updated_by`) OR NOT(OLD.`created_at` <=> NEW.`created_at`) OR NOT(OLD.`updated_at` <=> NEW.`updated_at`) OR NOT(OLD.`reason` <=> NEW.`reason`) OR NOT(OLD.`evidence` <=> NEW.`evidence`) THEN
    INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,
      module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,
      before_json,after_json,changed_fields)
    VALUES (NEW.tenant_id,UTC_TIMESTAMP(3),
      IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_actor,'0'),'0'),
      IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_name,'操作人未知'),'系统任务 / 操作人未记录'),
      IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_roles,'[]'),
      IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),COALESCE(@roi_audit_source,'system'),'database_trigger'),
      IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_request,NULL),
      IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id),@roi_audit_path,NULL),
      'finance',IF((COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id) AND COALESCE(@roi_audit_action,'')<>'',@roi_audit_action,'update'),
      'roi_cost_attribution',CAST(NEW.id AS CHAR),CAST(NEW.source_id AS CHAR),LEFT(CONCAT('费用归属#',NEW.source_type,':',NEW.source_id),255),CAST(NEW.author_id AS CHAR),CAST(OLD.author_id AS CHAR),
      JSON_OBJECT('id',CAST(OLD.`id` AS CHAR),'tenant_id',CAST(OLD.`tenant_id` AS CHAR),'source_type',OLD.`source_type`,'source_id',CAST(OLD.`source_id` AS CHAR),'occurred_at',CAST(OLD.`occurred_at` AS CHAR),'operator_user_id',CAST(OLD.`operator_user_id` AS CHAR),'author_id',CAST(OLD.`author_id` AS CHAR),'source_hash',OLD.`source_hash`,'version',OLD.`version`,'created_by',CAST(OLD.`created_by` AS CHAR),'updated_by',CAST(OLD.`updated_by` AS CHAR),'created_at',CAST(OLD.`created_at` AS CHAR),'updated_at',CAST(OLD.`updated_at` AS CHAR)),
      JSON_OBJECT('id',CAST(NEW.`id` AS CHAR),'tenant_id',CAST(NEW.`tenant_id` AS CHAR),'source_type',NEW.`source_type`,'source_id',CAST(NEW.`source_id` AS CHAR),'occurred_at',CAST(NEW.`occurred_at` AS CHAR),'operator_user_id',CAST(NEW.`operator_user_id` AS CHAR),'author_id',CAST(NEW.`author_id` AS CHAR),'source_hash',NEW.`source_hash`,'version',NEW.`version`,'created_by',CAST(NEW.`created_by` AS CHAR),'updated_by',CAST(NEW.`updated_by` AS CHAR),'created_at',CAST(NEW.`created_at` AS CHAR),'updated_at',CAST(NEW.`updated_at` AS CHAR)),
      JSON_MERGE_PRESERVE(IF(NOT(OLD.`id` <=> NEW.`id`),JSON_ARRAY('id'),JSON_ARRAY()),IF(NOT(OLD.`tenant_id` <=> NEW.`tenant_id`),JSON_ARRAY('tenant_id'),JSON_ARRAY()),IF(NOT(OLD.`source_type` <=> NEW.`source_type`),JSON_ARRAY('source_type'),JSON_ARRAY()),IF(NOT(OLD.`source_id` <=> NEW.`source_id`),JSON_ARRAY('source_id'),JSON_ARRAY()),IF(NOT(OLD.`occurred_at` <=> NEW.`occurred_at`),JSON_ARRAY('occurred_at'),JSON_ARRAY()),IF(NOT(OLD.`operator_user_id` <=> NEW.`operator_user_id`),JSON_ARRAY('operator_user_id'),JSON_ARRAY()),IF(NOT(OLD.`author_id` <=> NEW.`author_id`),JSON_ARRAY('author_id'),JSON_ARRAY()),IF(NOT(OLD.`source_hash` <=> NEW.`source_hash`),JSON_ARRAY('source_hash'),JSON_ARRAY()),IF(NOT(OLD.`version` <=> NEW.`version`),JSON_ARRAY('version'),JSON_ARRAY()),IF(NOT(OLD.`created_by` <=> NEW.`created_by`),JSON_ARRAY('created_by'),JSON_ARRAY()),IF(NOT(OLD.`updated_by` <=> NEW.`updated_by`),JSON_ARRAY('updated_by'),JSON_ARRAY()),IF(NOT(OLD.`created_at` <=> NEW.`created_at`),JSON_ARRAY('created_at'),JSON_ARRAY()),IF(NOT(OLD.`updated_at` <=> NEW.`updated_at`),JSON_ARRAY('updated_at'),JSON_ARRAY()),IF(NOT(OLD.`reason` <=> NEW.`reason`),JSON_ARRAY('reason'),JSON_ARRAY()),IF(NOT(OLD.`evidence` <=> NEW.`evidence`),JSON_ARRAY('evidence'),JSON_ARRAY())));
  END IF;
END$$

DELIMITER ;

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
DELIMITER $$
CREATE TRIGGER IF NOT EXISTS `roi_cost_classification_log_i_20261030` AFTER INSERT ON `roi_cost_classification` FOR EACH ROW
BEGIN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,before_json,after_json,changed_fields)
  VALUES(NEW.tenant_id,UTC_TIMESTAMP(3),
    IF(COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id,COALESCE(@roi_audit_actor,'0'),'0'),
    IF(COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id,COALESCE(@roi_audit_name,'unknown'),'database actor not recorded'),
    IF(COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id,@roi_audit_roles,'[]'),
    'database_trigger',@roi_audit_request,@roi_audit_path,'expense','cost_business_classified','roi_cost_classification',NEW.id,NEW.source_id,'Cost business classification',
    JSON_UNQUOTE(JSON_EXTRACT(NEW.classification_json,'$.authorId')),NULL,
    JSON_OBJECT('sourceType',NEW.source_type,'sourceId',NEW.source_id,'sourceHash',NEW.source_hash,'businessType',JSON_UNQUOTE(JSON_EXTRACT(NEW.classification_json,'$.businessType')),'authorId',JSON_UNQUOTE(JSON_EXTRACT(NEW.classification_json,'$.authorId')),'version',NEW.version),
    JSON_ARRAY('businessType','authorId','accountId','douyinAccountId','sessionId','promotionPlanId','periodStart','periodEnd','attributionEvidence'));
END$$
CREATE TRIGGER IF NOT EXISTS `roi_cost_classification_log_u_20261030` AFTER UPDATE ON `roi_cost_classification` FOR EACH ROW
BEGIN
  INSERT INTO roi_operation_log(tenant_id,occurred_at,actor_id,actor_name,actor_roles,source,request_id,request_path,module,action,record_type,record_id,related_record_id,record_label,author_id,previous_author_id,before_json,after_json,changed_fields)
  VALUES(NEW.tenant_id,UTC_TIMESTAMP(3),
    IF(COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id,COALESCE(@roi_audit_actor,'0'),'0'),
    IF(COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id,COALESCE(@roi_audit_name,'unknown'),'database actor not recorded'),
    IF(COALESCE(@roi_audit_enabled,0)=1 AND COALESCE(@roi_audit_tenant,0)=NEW.tenant_id,@roi_audit_roles,'[]'),
    'database_trigger',@roi_audit_request,@roi_audit_path,'expense','cost_business_classified','roi_cost_classification',NEW.id,NEW.source_id,'Cost business classification',
    JSON_UNQUOTE(JSON_EXTRACT(NEW.classification_json,'$.authorId')),JSON_UNQUOTE(JSON_EXTRACT(OLD.classification_json,'$.authorId')),
    JSON_OBJECT('sourceType',OLD.source_type,'sourceId',OLD.source_id,'sourceHash',OLD.source_hash,'businessType',JSON_UNQUOTE(JSON_EXTRACT(OLD.classification_json,'$.businessType')),'authorId',JSON_UNQUOTE(JSON_EXTRACT(OLD.classification_json,'$.authorId')),'version',OLD.version),
    JSON_OBJECT('sourceType',NEW.source_type,'sourceId',NEW.source_id,'sourceHash',NEW.source_hash,'businessType',JSON_UNQUOTE(JSON_EXTRACT(NEW.classification_json,'$.businessType')),'authorId',JSON_UNQUOTE(JSON_EXTRACT(NEW.classification_json,'$.authorId')),'version',NEW.version),
    JSON_ARRAY('businessType','authorId','accountId','douyinAccountId','sessionId','promotionPlanId','periodStart','periodEnd','attributionEvidence'));
END$$
CREATE TRIGGER IF NOT EXISTS `roi_cost_classification_no_delete_20261030` BEFORE DELETE ON `roi_cost_classification` FOR EACH ROW
BEGIN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Cost classifications are retained audit evidence; update to PENDING instead'; END$$
DELIMITER ;

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
