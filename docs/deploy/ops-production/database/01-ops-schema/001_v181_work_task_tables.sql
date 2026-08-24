-- =============================================================================
-- Ops DB (shenyu-ops) — V181 DDL: FR-M2-010 工作任务管理
-- Source: V181__m2_work_task_foundation.sql (§1 Tables + oa_task column)
-- Version: 2026-08-19 · Apply on: {{OPS_DB_HOST}}/{{OPS_DB_NAME}}
-- Idempotent: CREATE TABLE IF NOT EXISTS; column add guarded by procedure
-- =============================================================================
SET NAMES utf8mb4;
USE `{{OPS_DB_NAME}}`;  -- e.g. shenyu-ops

-- ---------------------------------------------------------------------------
-- 1. oa_work_task_sheet — 日登记批次（表头）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS oa_work_task_sheet (
    id                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    tenant_id           BIGINT       NOT NULL COMMENT '租户ID',
    ip_group_id         BIGINT       NOT NULL COMMENT '登记 IP 组',
    work_date           DATE         NOT NULL COMMENT '工作日期',
    status              VARCHAR(32)  NOT NULL DEFAULT 'DRAFT' COMMENT 'dict_work_task_sheet_status: DRAFT/CONFIRMED',
    registrar_user_id   BIGINT       NULL COMMENT '登记人 Football system_users.id',
    confirmed_at        TIMESTAMP    NULL COMMENT '确认时间',
    remark              VARCHAR(500) NULL COMMENT '备注',
    creator             VARCHAR(64)  DEFAULT 'system' COMMENT '创建者',
    create_time         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater             VARCHAR(64)  DEFAULT 'system' COMMENT '更新者',
    update_time         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted             SMALLINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除(0=未删除,1=已删除)',
    PRIMARY KEY (id),
    UNIQUE KEY uk_work_task_sheet_group_date (tenant_id, ip_group_id, work_date, deleted),
    KEY idx_work_task_sheet_tenant_status (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作任务登记批次（日表头）';

-- ---------------------------------------------------------------------------
-- 2. oa_work_task_assignment — 登记行
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS oa_work_task_assignment (
    id                      BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    tenant_id               BIGINT       NOT NULL COMMENT '租户ID',
    sheet_id                BIGINT       NOT NULL COMMENT '登记批次 oa_work_task_sheet.id',
    row_no                  INT          NOT NULL COMMENT '行号 1~N',
    competition_id          VARCHAR(64)  NULL COMMENT '赛事 scheduleId',
    competition_name        VARCHAR(200) NULL COMMENT '赛事名称快照',
    session_no              VARCHAR(16)  NULL COMMENT '场次序号 001~010',
    league_name             VARCHAR(100) NULL COMMENT '联赛名称',
    match_time              TIMESTAMP    NULL COMMENT '比赛时间',
    author_id               BIGINT       NULL COMMENT '内容作者 author_user.id',
    assignee_id             BIGINT       NULL COMMENT '执行人 system_users.id',
    work_date               DATE         NOT NULL COMMENT '行级工作日期',
    marketing_plan          VARCHAR(32)  NULL COMMENT 'dict_marketing_plan_type',
    is_live                 TINYINT      NULL DEFAULT 0 COMMENT '是否直播 0/1',
    live_time               TIME         NULL COMMENT '直播时间',
    sales_platform          VARCHAR(32)  NULL COMMENT 'dict_sales_platform',
    win_prediction          VARCHAR(32)  NOT NULL DEFAULT 'UNKNOWN' COMMENT 'dict_win_prediction',
    win_prediction_source   VARCHAR(16)  NULL COMMENT 'JOB/MANUAL',
    win_prediction_at       TIMESTAMP    NULL COMMENT '红黑判定时间',
    ai_prompt_scene         VARCHAR(64)  NULL COMMENT 'AI 提示词 scene 快照',
    generated_task_id       BIGINT       NULL COMMENT '确认后 oa_task.id',
    creator                 VARCHAR(64)  DEFAULT 'system' COMMENT '创建者',
    create_time             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater                 VARCHAR(64)  DEFAULT 'system' COMMENT '更新者',
    update_time             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted                 SMALLINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除(0=未删除,1=已删除)',
    PRIMARY KEY (id),
    KEY idx_work_task_assignment_sheet (tenant_id, sheet_id),
    KEY idx_work_task_assignment_task (tenant_id, generated_task_id),
    UNIQUE KEY uk_work_task_assignment_unique (tenant_id, work_date, competition_id, author_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作任务登记行';

-- ---------------------------------------------------------------------------
-- 3. oa_task 反向追溯（ADR-071 D4）
-- Run once; skip if column already exists.
-- ---------------------------------------------------------------------------
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'oa_task'
      AND COLUMN_NAME = 'work_task_assignment_id'
);
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE oa_task ADD COLUMN work_task_assignment_id BIGINT NULL COMMENT ''工作任务登记行 oa_work_task_assignment.id'' AFTER plan_id',
    'SELECT ''skip: oa_task.work_task_assignment_id already exists'' AS info'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'oa_task'
      AND INDEX_NAME = 'idx_oa_task_work_task_assignment'
);
SET @idx_ddl := IF(@idx_exists = 0,
    'ALTER TABLE oa_task ADD KEY idx_oa_task_work_task_assignment (tenant_id, work_task_assignment_id)',
    'SELECT ''skip: idx_oa_task_work_task_assignment already exists'' AS info'
);
PREPARE stmt2 FROM @idx_ddl;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;
