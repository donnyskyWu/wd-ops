-- =============================================================================
-- 本地库数据/结构同步 — S-20 matchScheme + ADR-074/075 工作任务增量
-- 目标: localhost MySQL root/root
--   shenyu-ops  — Flyway V192~V195 结构（幂等）
--   shenyu-sys  — V193 营销计划字典（本地 system 库；非 shenyu-system）
-- 用法（必须 utf8mb4 文件重定向；禁止 PowerShell Get-Content | mysql，会把中文写成字面 '?'）:
--   mysql --default-character-set=utf8mb4 -uroot -proot < scripts/integration-config/local-sync-s20-data.sql
--   或: .\scripts\sync-local-db-s20.ps1
-- =============================================================================

SET NAMES utf8mb4;

-- ----- shenyu-ops: V192 ADR-074 work-task schema (idempotent) -----
USE `shenyu-ops`;

SET @has_marketing_plan := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'shenyu-ops' AND TABLE_NAME = 'oa_sop_template' AND COLUMN_NAME = 'marketing_plan'
);
SET @sql := IF(@has_marketing_plan = 0,
    'ALTER TABLE oa_sop_template ADD COLUMN marketing_plan VARCHAR(32) NULL AFTER platform_type',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_wta_comp_ids := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'shenyu-ops' AND TABLE_NAME = 'oa_work_task_assignment' AND COLUMN_NAME = 'competition_ids_json'
);
SET @sql := IF(@has_wta_comp_ids = 0,
    'ALTER TABLE oa_work_task_assignment ADD COLUMN competition_ids_json JSON NULL AFTER competition_name',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_task_comp_ids := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'shenyu-ops' AND TABLE_NAME = 'oa_task' AND COLUMN_NAME = 'competition_ids_json'
);
SET @sql := IF(@has_task_comp_ids = 0,
    'ALTER TABLE oa_task ADD COLUMN competition_ids_json JSON NULL AFTER competition_id',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS oa_work_task_assignment_task (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id       BIGINT       NOT NULL,
    assignment_id   BIGINT       NOT NULL,
    task_id         BIGINT       NOT NULL,
    creator         VARCHAR(64)  DEFAULT 'system',
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         VARCHAR(64)  DEFAULT 'system',
    update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         SMALLINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_wt_assignment_task (tenant_id, assignment_id, task_id, deleted),
    KEY idx_wt_assignment_task_assignment (tenant_id, assignment_id),
    KEY idx_wt_assignment_task_task (tenant_id, task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @has_exec_group := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'shenyu-ops' AND TABLE_NAME = 'oa_work_task_assignment' AND COLUMN_NAME = 'execution_group_id'
);
SET @sql := IF(@has_exec_group = 0,
    'ALTER TABLE oa_work_task_assignment ADD COLUMN execution_group_id BIGINT NULL AFTER generated_task_id',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_exec_idx := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = 'shenyu-ops' AND TABLE_NAME = 'oa_work_task_assignment' AND INDEX_NAME = 'idx_wt_assignment_exec_group'
);
SET @sql := IF(@has_exec_idx = 0,
    'CREATE INDEX idx_wt_assignment_exec_group ON oa_work_task_assignment (tenant_id, sheet_id, execution_group_id)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ----- shenyu-ops: V195 S-20 matchScheme (idempotent) -----
SET @has_match_scheme := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'shenyu-ops' AND TABLE_NAME = 'oa_production_content' AND COLUMN_NAME = 'match_scheme_json'
);
SET @sql := IF(@has_match_scheme = 0,
    'ALTER TABLE oa_production_content
        ADD COLUMN match_scheme_json JSON NULL AFTER competition_name,
        ADD COLUMN match_type TINYINT NULL AFTER match_scheme_json,
        ADD COLUMN competition_ids_json JSON NULL AFTER match_type',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ----- shenyu-sys: V193 ADR-074 dict_marketing_plan_type (本地 system 库) -----
USE `shenyu-sys`;

UPDATE system_dict_data
SET deleted = b'1', updater = 'local-sync-s20', update_time = NOW()
WHERE dict_type = 'dict_marketing_plan_type'
  AND value IN ('LIVE_PUBLIC', 'PAID_SALES', 'LIVE_DRAIN')
  AND deleted = b'0';

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '快手付费课程', 'KUAISHOU_PAID_COURSE', 'dict_marketing_plan_type', 0, 'primary', '', 'ADR-074', 'local-sync-s20', NOW(), 'local-sync-s20', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd
    WHERE sd.dict_type = 'dict_marketing_plan_type' AND sd.value = 'KUAISHOU_PAID_COURSE' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '公司平台付费', 'COMPANY_PAID', 'dict_marketing_plan_type', 0, 'warning', '', 'ADR-074', 'local-sync-s20', NOW(), 'local-sync-s20', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd
    WHERE sd.dict_type = 'dict_marketing_plan_type' AND sd.value = 'COMPANY_PAID' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '免费公推', 'FREE_PUBLIC', 'dict_marketing_plan_type', 0, 'success', '', 'ADR-074', 'local-sync-s20', NOW(), 'local-sync-s20', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd
    WHERE sd.dict_type = 'dict_marketing_plan_type' AND sd.value = 'FREE_PUBLIC' AND sd.deleted = b'0'
);

-- Repair labels if a prior Windows pipe import stored literal '?' (HEX 3F). SSOT = ADR-074 / PRD-M2-工作任务管理.
UPDATE system_dict_data
SET label = '快手付费课程', updater = 'local-sync-s20', update_time = NOW()
WHERE dict_type = 'dict_marketing_plan_type' AND value = 'KUAISHOU_PAID_COURSE' AND deleted = b'0'
  AND label <> '快手付费课程';

UPDATE system_dict_data
SET label = '公司平台付费', updater = 'local-sync-s20', update_time = NOW()
WHERE dict_type = 'dict_marketing_plan_type' AND value = 'COMPANY_PAID' AND deleted = b'0'
  AND label <> '公司平台付费';

UPDATE system_dict_data
SET label = '免费公推', updater = 'local-sync-s20', update_time = NOW()
WHERE dict_type = 'dict_marketing_plan_type' AND value = 'FREE_PUBLIC' AND deleted = b'0'
  AND label <> '免费公推';

-- ----- shenyu-ops: 本地验证 seed — admin 担任 IP 组组长（内容/E2E 用 /led） -----
USE `shenyu-ops`;

UPDATE oa_ip_group
SET leader_user_id = (
    SELECT id FROM `shenyu-sys`.system_users WHERE username = 'admin' AND deleted = 0 LIMIT 1
),
    updater = 'local-sync-s20',
    update_time = NOW()
WHERE id = 92002
  AND tenant_id = 1
  AND deleted = 0
  AND EXISTS (SELECT 1 FROM `shenyu-sys`.system_users WHERE username = 'admin' AND deleted = 0);

INSERT INTO oa_ip_group_member (tenant_id, ip_group_id, user_id, position, is_leader, creator, updater, deleted)
SELECT 1, 92002, u.id, 'leader', 1, 'local-sync-s20', 'local-sync-s20', 0
FROM `shenyu-sys`.system_users u
WHERE u.username = 'admin' AND u.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM oa_ip_group_member m
    WHERE m.tenant_id = 1 AND m.ip_group_id = 92002 AND m.user_id = u.id AND m.deleted = 0
  );

SELECT 'local-sync-s20 done' AS status;
