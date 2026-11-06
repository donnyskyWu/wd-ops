-- =============================================================================
-- 用途：M-Live 投产比 · 原生只读边界（ROI 媒体委托读）+ ROI 分析维度字典
-- 目标库：shenyu-system
-- 执行顺序：在 shenyu-ops.sql 之后、ops-server 发版前（见 README.md）
-- 幂等性：是（CREATE IF NOT EXISTS；字典 NOT EXISTS / ON DUPLICATE KEY UPDATE）
-- 回滚：生产需备份；边界表可停用行；字典勿删历史业务引用
-- 关联：ADR-084；native_read_boundary_20261030.md；Flyway V42 dict_roi_dimension
-- 生成：python docs/delivery/handover/sql-release-20261105/_build_sql_release_20261105.py
-- =============================================================================

SET NAMES utf8mb4;
USE `shenyu-system`;


-- =============================================================================
-- Section A: 原生只读边界表
-- SSOT: native_read_boundary_20261030.sql
-- =============================================================================

-- Native read boundary v1. Target database: shenyu-system, MySQL >= 8.0.29.
-- Additive EMPTY tables only. NO employee/role assignment, financial write or historical backfill.
-- Execution: native read boundary schema before deploying system/member/pay adapters.
-- Repeatable CREATE; existing same-name schema must be reviewed, never silently dropped.

CREATE TABLE IF NOT EXISTS system_native_read_boundary (
  id BIGINT NOT NULL AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  admin_user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  boundary_kind VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  business_match_type INT NULL,
  evidence_ref VARCHAR(512) NOT NULL,
  allow_export TINYINT NOT NULL DEFAULT 0,
  enabled TINYINT NOT NULL DEFAULT 0,
  valid_from DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  valid_until DATETIME NULL,
  creator VARCHAR(64) NOT NULL DEFAULT '',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater VARCHAR(64) NOT NULL DEFAULT '',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted BIT(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (id),
  KEY idx_native_boundary_principal (tenant_id,admin_user_id,enabled,deleted,role_id),
  CONSTRAINT ck_native_boundary_kind CHECK (
    (boundary_kind='AUTHOR' AND business_match_type IS NULL) OR
    (boundary_kind='SERVICE' AND business_match_type IS NOT NULL AND business_match_type BETWEEN 1 AND 5)),
  CONSTRAINT ck_native_boundary_export CHECK (allow_export IN (0,1)),
  CONSTRAINT ck_native_boundary_identity CHECK (admin_user_id>0 AND role_id>0),
  CONSTRAINT ck_native_boundary_interval CHECK (valid_until IS NULL OR valid_until>valid_from),
  CONSTRAINT ck_native_boundary_evidence CHECK (CHAR_LENGTH(TRIM(evidence_ref))>0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Explicit native admin read restriction; no menu or financial-write grant';

CREATE TABLE IF NOT EXISTS system_native_service_subject (
  id BIGINT NOT NULL AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  boundary_id BIGINT NOT NULL,
  subject_type VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  subject_id BIGINT NOT NULL,
  servicing_admin_user_id BIGINT NOT NULL,
  service_team_id BIGINT NULL,
  evidence_ref VARCHAR(512) NOT NULL,
  team_authorization_ref VARCHAR(512) NOT NULL DEFAULT '',
  enabled TINYINT NOT NULL DEFAULT 0,
  valid_from DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  valid_until DATETIME NULL,
  creator VARCHAR(64) NOT NULL DEFAULT '',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater VARCHAR(64) NOT NULL DEFAULT '',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted BIT(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (id),
  KEY idx_native_service_boundary (tenant_id,boundary_id,enabled,deleted,subject_type,subject_id),
  CONSTRAINT ck_native_service_type CHECK (subject_type IN ('CUSTOMER','LIVE_CODE')),
  CONSTRAINT ck_native_service_ids CHECK (boundary_id>0 AND subject_id>0 AND servicing_admin_user_id>0),
  CONSTRAINT ck_native_service_interval CHECK (valid_until IS NULL OR valid_until>valid_from),
  CONSTRAINT ck_native_service_evidence CHECK (CHAR_LENGTH(TRIM(evidence_ref))>0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Actual served customer/live-code evidence; follows and remarks are insufficient';
CREATE TABLE IF NOT EXISTS system_native_service_team_member (
  id BIGINT NOT NULL AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  team_id BIGINT NOT NULL,
  leader_admin_user_id BIGINT NOT NULL,
  member_admin_user_id BIGINT NOT NULL,
  business_match_type INT NOT NULL,
  evidence_ref VARCHAR(512) NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 0,
  valid_from DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  valid_until DATETIME NULL,
  creator VARCHAR(64) NOT NULL DEFAULT '',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater VARCHAR(64) NOT NULL DEFAULT '',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted BIT(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (id),
  KEY idx_native_team_current (tenant_id,team_id,business_match_type,enabled,deleted),
  CONSTRAINT ck_native_team_identity CHECK (team_id>0 AND leader_admin_user_id>0 AND member_admin_user_id>0),
  CONSTRAINT ck_native_team_business CHECK (business_match_type BETWEEN 1 AND 5),
  CONSTRAINT ck_native_team_interval CHECK (valid_until IS NULL OR valid_until>valid_from),
  CONSTRAINT ck_native_team_evidence CHECK (CHAR_LENGTH(TRIM(evidence_ref))>0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Explicit current native service leader/member relation; no department/name inference';

-- Validate SHOW CREATE TABLE, zero rows at first install, and native role freshness before enabling any grant.
-- Rollback app release first, retain additive tables/evidence; no DROP in automated rollback.

-- =============================================================================
-- Section B: dict_roi_dimension
-- SSOT: V42__dict_roi_dimension.sql (system 读路径)
-- =============================================================================

-- Section B: dict_roi_dimension（Flyway V42 → shenyu-system 读路径）
-- DictService @DS("system")；与 ops 库 sys_dict_* 解耦

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT 'ROI分析维度', 'dict_roi_dimension', 0, 'M-Live/ROI handoff 20261105 from V42', 'sql-release-20261105', NOW(), 'sql-release-20261105', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM system_dict_type WHERE type = 'dict_roi_dimension' AND deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, 'IP组', 'IP_GROUP', 'dict_roi_dimension', 0, '', '', 'V42', 'sql-release-20261105', NOW(), 'sql-release-20261105', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (
  SELECT 1 FROM system_dict_data WHERE dict_type = 'dict_roi_dimension' AND value = 'IP_GROUP' AND deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '账号', 'ACCOUNT', 'dict_roi_dimension', 0, '', '', 'V42', 'sql-release-20261105', NOW(), 'sql-release-20261105', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (
  SELECT 1 FROM system_dict_data WHERE dict_type = 'dict_roi_dimension' AND value = 'ACCOUNT' AND deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '人员', 'PERSON', 'dict_roi_dimension', 0, '', '', 'V42', 'sql-release-20261105', NOW(), 'sql-release-20261105', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (
  SELECT 1 FROM system_dict_data WHERE dict_type = 'dict_roi_dimension' AND value = 'PERSON' AND deleted = b'0'
);
