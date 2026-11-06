-- =============================================================================
-- shenyu-ops 例程 / 触发器补充包（DELIMITER 块）
-- 目标库：shenyu-ops
-- 【必须】DBA 一次性设置：log_bin_trust_function_creators = 1
--   详见同目录 DBA-1419-一次性设置.md 与 00-mysql57-prerequisites.sql
--   校验：SHOW VARIABLES LIKE 'log_bin_trust_function_creators'; → ON / 1
-- 执行顺序：在 shenyu-ops-schema.sql 成功导入之后执行本文件
-- 或：DBA 已开 trust 后可直接导入 shenyu-ops-full.sql（= shenyu-ops.sql）整包
-- 生成：python docs/delivery/handover/sql-release-20261105/_build_sql_release_20261105.py
-- =============================================================================

SET NAMES utf8mb4;
USE `shenyu-ops`;

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
