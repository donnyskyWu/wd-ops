-- M10-COL-S-02: 采集日志表
-- Spec: PRD-M10 §4.1 / API-M10 §1.6

CREATE TABLE IF NOT EXISTS oa_collect_log (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id     BIGINT       NOT NULL,
    task_id       BIGINT       NOT NULL,
    status        VARCHAR(32)  NOT NULL,
    start_at      TIMESTAMP    NOT NULL,
    end_at        TIMESTAMP    NULL,
    duration_ms   BIGINT       NULL,
    record_count  INT          NOT NULL DEFAULT 0,
    error_message TEXT         NULL,
    retry_count   INT          NOT NULL DEFAULT 0,
    creator       VARCHAR(64)  DEFAULT 'system',
    create_time   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater       VARCHAR(64)  DEFAULT 'system',
    update_time   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted       SMALLINT     NOT NULL DEFAULT 0,
    KEY idx_oa_collect_log_task (tenant_id, task_id),
    KEY idx_oa_collect_log_start (tenant_id, start_at)
);
