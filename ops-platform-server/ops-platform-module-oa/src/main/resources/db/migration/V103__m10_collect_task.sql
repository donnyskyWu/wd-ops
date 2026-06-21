-- M10-COL-S-01: 采集任务表 + 采集源字典
-- Spec: PRD-M10 §4.1.2 / API-M10 §1 / GLOBAL-CONVENTIONS

INSERT INTO sys_dict_type (type, name, status) VALUES
('dict_collect_source', '采集源', 'ENABLED')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_collect_source', '公众号 API', 'WECHAT_MP_API', 1, 'ENABLED'),
('dict_collect_source', '视频号 API', 'WECHAT_CHANNELS_API', 2, 'ENABLED'),
('dict_collect_source', '抖音开放平台', 'DOUYIN_OPEN_API', 3, 'ENABLED'),
('dict_collect_source', '奥创接口', 'AOCHUANG_API', 4, 'ENABLED'),
('dict_collect_source', '企微 API', 'WECOM_API', 5, 'ENABLED'),
('dict_collect_source', '个微 API', 'PERSONAL_WECHAT_API', 6, 'ENABLED')
ON DUPLICATE KEY UPDATE label = VALUES(label), sort = VALUES(sort);

CREATE TABLE IF NOT EXISTS oa_collect_task (
    id                   BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id            BIGINT       NOT NULL,
    task_name            VARCHAR(100) NOT NULL,
    platform_type        VARCHAR(32)  NOT NULL,
    account_id           BIGINT       NOT NULL,
    method               VARCHAR(32)  NOT NULL,
    source               VARCHAR(32)  NOT NULL,
    frequency            VARCHAR(32)  NOT NULL,
    cron                 VARCHAR(64)  NOT NULL,
    api_config_encrypted TEXT         NULL,
    status               VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    last_run_at          TIMESTAMP    NULL,
    next_run_at          TIMESTAMP    NULL,
    run_count            INT          NOT NULL DEFAULT 0,
    fail_count           INT          NOT NULL DEFAULT 0,
    creator              VARCHAR(64)  DEFAULT 'system',
    create_time          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater              VARCHAR(64)  DEFAULT 'system',
    update_time          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted              SMALLINT     NOT NULL DEFAULT 0,
    KEY idx_oa_collect_task_tenant (tenant_id),
    KEY idx_oa_collect_task_account (tenant_id, account_id),
    KEY idx_oa_collect_task_status (tenant_id, status)
);
