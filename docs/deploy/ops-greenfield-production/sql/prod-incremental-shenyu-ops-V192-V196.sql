-- =============================================================================
-- Production incremental deploy: shenyu-ops database
--
-- Purpose:
--   Apply OPS schema changes for this week's release (ADR-074/075/076/077).
--   Flyway SSOT: football-module-ops-server/src/main/resources/db/migration/
--
-- Versions included (execution order):
--   V192 — ADR-074: 营销计划、多赛事、assignment↔task 关联表
--   V194 — ADR-075: 任务合并执行组 execution_group_id
--   V195 — ADR-076: 内容生产 matchScheme / matchType SSOT
--   V196 — ADR-077: SOP 节点 document_type、task 反规范化、内容 AI 生成状态列（OPS 表部分）
--
-- Pre-requisite:
--   Production shenyu-ops Flyway baseline at V191 (V191 及之前已执行).
--
-- Companion script:
--   prod-incremental-shenyu-system-V193-V196.sql (字典 V193 + V196，在 shenyu-system 库执行)
--
-- Note:
--   ADR-080（任务节点名称与执行页登记备注）无 SQL 变更，本脚本不包含。
--
-- Execution:
--   mysql -h<host> -u<user> -p --default-character-set=utf8mb4 shenyu-ops < prod-incremental-shenyu-ops-V192-V196.sql
--
-- Idempotency:
--   V192 CREATE TABLE IF NOT EXISTS 已幂等；ALTER 重复执行会 Duplicate column，需人工确认。
-- =============================================================================

-- ---------------------------------------------------------------------------
-- V192: ADR-074 work-task v0.4 — SOP marketing_plan, multi-competition, assignment↔task junction
-- ---------------------------------------------------------------------------
SET NAMES utf8mb4;

ALTER TABLE oa_sop_template
    ADD COLUMN marketing_plan VARCHAR(32) NULL COMMENT 'dict_marketing_plan_type; enabled 1:1 per tenant' AFTER platform_type;

ALTER TABLE oa_work_task_assignment
    ADD COLUMN competition_ids_json JSON NULL COMMENT 'multi-match snapshots [{competitionId,...}]' AFTER competition_name;

ALTER TABLE oa_task
    ADD COLUMN competition_ids_json JSON NULL COMMENT 'work-task row competitions copy' AFTER competition_id;

CREATE TABLE IF NOT EXISTS oa_work_task_assignment_task (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    tenant_id       BIGINT       NOT NULL COMMENT '租户ID',
    assignment_id   BIGINT       NOT NULL COMMENT 'oa_work_task_assignment.id',
    task_id         BIGINT       NOT NULL COMMENT 'oa_task.id',
    creator         VARCHAR(64)  DEFAULT 'system' COMMENT '创建者',
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater         VARCHAR(64)  DEFAULT 'system' COMMENT '更新者',
    update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         SMALLINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_wt_assignment_task (tenant_id, assignment_id, task_id, deleted),
    KEY idx_wt_assignment_task_assignment (tenant_id, assignment_id),
    KEY idx_wt_assignment_task_task (tenant_id, task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作任务登记行与生成 task 关联（ADR-074）';

ALTER TABLE oa_work_task_assignment DROP INDEX uk_work_task_assignment_unique;

-- ---------------------------------------------------------------------------
-- V194: ADR-075 — merge execution group (single match per row, combined confirm)
-- ---------------------------------------------------------------------------
SET NAMES utf8mb4;

ALTER TABLE oa_work_task_assignment
    ADD COLUMN execution_group_id BIGINT NULL COMMENT 'sheet内合并执行组；同组 confirm 仅生成一套 SOP task' AFTER generated_task_id;

CREATE INDEX idx_wt_assignment_exec_group
    ON oa_work_task_assignment (tenant_id, sheet_id, execution_group_id);

-- ---------------------------------------------------------------------------
-- V195: S-20 / ADR-076 — OPS 内容生产 matchScheme + matchType SSOT
-- ---------------------------------------------------------------------------
ALTER TABLE oa_production_content
    ADD COLUMN match_scheme_json JSON NULL COMMENT 'Football matchScheme SSOT（ADR-076）' AFTER competition_name,
    ADD COLUMN match_type TINYINT NULL COMMENT '1竞足2传足3北单4足球5临场' AFTER match_scheme_json,
    ADD COLUMN competition_ids_json JSON NULL COMMENT 'N场scheduleId快照' AFTER match_type;

-- ---------------------------------------------------------------------------
-- V196 / ADR-077 Slice A: SOP 节点 documentType + task 反规范化列 + 内容 AI 生成状态（OPS 表部分）
-- 字典 seed 见 prod-incremental-shenyu-system-V193-V196.sql
-- ---------------------------------------------------------------------------
SET NAMES utf8mb4;

ALTER TABLE oa_sop_node
    ADD COLUMN document_type VARCHAR(32) NULL COMMENT 'dict_document_type；仅 CONTENT_GENERATION 必填（ADR-077）' AFTER node_type;

ALTER TABLE oa_task
    ADD COLUMN document_type VARCHAR(32) NULL COMMENT '自 SOP 节点拷贝的 dict_document_type（ADR-077 D2，Slice B 写入）' AFTER node_id;

ALTER TABLE oa_production_content
    ADD COLUMN ai_generate_status VARCHAR(32) NULL COMMENT 'dict_ai_generate_status（ADR-077）' AFTER document_type,
    ADD COLUMN ai_generate_error VARCHAR(500) NULL COMMENT 'jingcai 失败原因，可展示（ADR-077）' AFTER ai_generate_status;

-- =============================================================================
-- Optional: Flyway schema history records (manual deploy only; uncomment if needed)
-- Assumes flyway_schema_history table exists in shenyu-ops and versions not yet recorded.
-- =============================================================================
-- INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
-- VALUES
--   ((SELECT COALESCE(MAX(installed_rank), 0) + 1 FROM flyway_schema_history fsh), '192', 'm2 work task adr074 schema', 'SQL', 'V192__m2_work_task_adr074_schema.sql', NULL, 'manual-prod', NOW(), 0, 1),
--   ((SELECT COALESCE(MAX(installed_rank), 0) + 2 FROM flyway_schema_history fsh), '194', 'm2 work task execution merge group', 'SQL', 'V194__m2_work_task_execution_merge_group.sql', NULL, 'manual-prod', NOW(), 0, 1),
--   ((SELECT COALESCE(MAX(installed_rank), 0) + 3 FROM flyway_schema_history fsh), '195', 'm2 content match scheme', 'SQL', 'V195__m2_content_match_scheme.sql', NULL, 'manual-prod', NOW(), 0, 1),
--   ((SELECT COALESCE(MAX(installed_rank), 0) + 4 FROM flyway_schema_history fsh), '196', 'adr077 s21a sop node document type', 'SQL', 'V196__adr077_s21a_sop_node_document_type.sql', NULL, 'manual-prod', NOW(), 0, 1);
