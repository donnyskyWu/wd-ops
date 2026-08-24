-- =============================================================================
-- Ops DB (shenyu-ops) — V184 DDL: M6 私域报表 MVP — 周度销售反馈 U 列
-- Source: V184__m6_private_domain_report_mvp.sql
-- Version: 2026-08-24 · Apply on: {{OPS_DB_HOST}}/{{OPS_DB_NAME}}
-- Idempotent: CREATE TABLE IF NOT EXISTS
--
-- Notes:
--   · IP业务月达成 / 周度私域转化 为只读聚合报表，无额外 DDL（读 member-server + oa_ip_group_anchor_rel）
--   · 仅 weekly-feedback（U 列）需持久化表 oa_report_weekly_feedback
--   · UI 入口：6126 数据报表中心（permission oa:report:list），无独立 system_menu
-- =============================================================================
SET NAMES utf8mb4;
USE `{{OPS_DB_NAME}}`;  -- e.g. shenyu-ops

CREATE TABLE IF NOT EXISTS oa_report_weekly_feedback (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    tenant_id       BIGINT       NOT NULL COMMENT '租户ID',
    author_id       BIGINT       NOT NULL COMMENT '作者 author_user.id',
    week_label      VARCHAR(32)  NOT NULL COMMENT '周度标签，如 D32周',
    channel         VARCHAR(16)  NOT NULL COMMENT 'DOUYIN/KUAISHOU/SUMMARY',
    feedback_text   TEXT         NULL COMMENT '销售反馈（U列）',
    creator         VARCHAR(64)  DEFAULT 'system' COMMENT '创建者',
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater         VARCHAR(64)  DEFAULT 'system' COMMENT '更新者',
    update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         SMALLINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除(0=未删除,1=已删除)',
    PRIMARY KEY (id),
    UNIQUE KEY uk_report_weekly_feedback (tenant_id, author_id, week_label, channel, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='私域周度报表销售反馈';
