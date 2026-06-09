-- M3 绩效核算：指标 / 模板 / 考核记录 / 订单归因

CREATE TABLE IF NOT EXISTS oa_metric (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    metric_name     VARCHAR(100) NOT NULL,
    metric_code     VARCHAR(50)  NOT NULL,
    unit            VARCHAR(32)  NULL,
    category        VARCHAR(32)  NULL,
    status          TINYINT      NOT NULL DEFAULT 1 COMMENT '0=停用 1=启用',
    creator         VARCHAR(64)  DEFAULT 'system',
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         VARCHAR(64)  DEFAULT 'system',
    update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT     NOT NULL DEFAULT 0,
    UNIQUE KEY uk_oa_metric_code (tenant_id, metric_code, deleted),
    KEY idx_oa_metric_tenant (tenant_id)
);

CREATE TABLE IF NOT EXISTS oa_perf_template (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    position        VARCHAR(32)  NOT NULL,
    template_name   VARCHAR(100) NOT NULL,
    is_active       TINYINT      NOT NULL DEFAULT 0 COMMENT '0=停用 1=启用',
    remark          VARCHAR(500) NULL,
    creator         VARCHAR(64)  DEFAULT 'system',
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         VARCHAR(64)  DEFAULT 'system',
    update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT     NOT NULL DEFAULT 0,
    KEY idx_oa_perf_template_tenant (tenant_id),
    KEY idx_oa_perf_template_position (tenant_id, position, is_active)
);

CREATE TABLE IF NOT EXISTS oa_perf_template_item (
    id                      BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    template_id             BIGINT        NOT NULL,
    metric_id               BIGINT        NOT NULL,
    weight                  DECIMAL(5,2)  NOT NULL,
    calc_rule               VARCHAR(32)   NOT NULL DEFAULT 'AUTO',
    score_standard_json     TEXT          NOT NULL,
    creator                 VARCHAR(64)   DEFAULT 'system',
    create_time             TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater                 VARCHAR(64)   DEFAULT 'system',
    update_time             TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                 SMALLINT      NOT NULL DEFAULT 0,
    KEY idx_oa_perf_template_item_tpl (template_id)
);

CREATE TABLE IF NOT EXISTS oa_perf_record (
    id              BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT        NOT NULL,
    template_id     BIGINT        NOT NULL,
    target_user_id  BIGINT        NOT NULL,
    ip_group_id     BIGINT        NULL,
    period_type     VARCHAR(32)   NOT NULL,
    period_start    DATE          NOT NULL,
    period_end      DATE          NOT NULL,
    total_score     DECIMAL(8,2)  NULL,
    grade           VARCHAR(8)    NULL,
    status          VARCHAR(32)   NOT NULL DEFAULT 'DRAFT',
    remark          VARCHAR(500)  NULL,
    creator         VARCHAR(64)   DEFAULT 'system',
    create_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         VARCHAR(64)   DEFAULT 'system',
    update_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT      NOT NULL DEFAULT 0,
    UNIQUE KEY uk_oa_perf_record_period (tenant_id, target_user_id, period_type, period_start, period_end, deleted),
    KEY idx_oa_perf_record_tenant (tenant_id),
    KEY idx_oa_perf_record_user (tenant_id, target_user_id),
    KEY idx_oa_perf_record_status (tenant_id, status)
);

CREATE TABLE IF NOT EXISTS oa_perf_item_record (
    id                  BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    record_id           BIGINT        NOT NULL,
    metric_id           BIGINT        NOT NULL,
    metric_value        DECIMAL(16,4) NULL,
    score               DECIMAL(8,2)  NULL,
    manual_adjustment   DECIMAL(8,2)  NULL DEFAULT 0,
    final_score         DECIMAL(8,2)  NULL,
    remark              VARCHAR(500)  NULL,
    creator             VARCHAR(64)   DEFAULT 'system',
    create_time         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater             VARCHAR(64)   DEFAULT 'system',
    update_time         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             SMALLINT      NOT NULL DEFAULT 0,
    KEY idx_oa_perf_item_record_rec (record_id)
);

CREATE TABLE IF NOT EXISTS oa_order (
    id              BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT        NOT NULL,
    order_no        VARCHAR(64)   NOT NULL,
    order_amount    DECIMAL(16,2) NOT NULL,
    order_time      TIMESTAMP     NOT NULL,
    account_id      BIGINT        NULL,
    ip_group_id     BIGINT        NULL,
    remark          VARCHAR(500)  NULL,
    creator         VARCHAR(64)   DEFAULT 'system',
    create_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         VARCHAR(64)   DEFAULT 'system',
    update_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT      NOT NULL DEFAULT 0,
    UNIQUE KEY uk_oa_order_no (tenant_id, order_no, deleted),
    KEY idx_oa_order_tenant (tenant_id),
    KEY idx_oa_order_time (tenant_id, order_time)
);

CREATE TABLE IF NOT EXISTS oa_order_attribution (
    id              BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT        NOT NULL,
    order_id        BIGINT        NOT NULL,
    account_id      BIGINT        NULL,
    ip_group_id     BIGINT        NULL,
    author_id       BIGINT        NULL,
    ops_user_id     BIGINT        NULL,
    revenue         DECIMAL(16,2) NOT NULL DEFAULT 0,
    cost            DECIMAL(16,2) NOT NULL DEFAULT 0,
    roi             DECIMAL(10,4) NULL,
    stat_date       DATE          NOT NULL,
    creator         VARCHAR(64)   DEFAULT 'system',
    create_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         VARCHAR(64)   DEFAULT 'system',
    update_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT      NOT NULL DEFAULT 0,
    KEY idx_oa_order_attr_tenant (tenant_id),
    KEY idx_oa_order_attr_date (tenant_id, stat_date),
    KEY idx_oa_order_attr_ip (tenant_id, ip_group_id),
    KEY idx_oa_order_attr_account (tenant_id, account_id)
);

-- 字典
INSERT INTO sys_dict_type (type, name, status) VALUES
('dict_perf_period', '考核周期', 'ENABLED'),
('dict_perf_status', '考核状态', 'ENABLED')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_perf_period', '月度', 'MONTH', 1, 'ENABLED'),
('dict_perf_period', '季度', 'QUARTER', 2, 'ENABLED'),
('dict_perf_status', '草稿', 'DRAFT', 1, 'ENABLED'),
('dict_perf_status', '已确认', 'CONFIRMED', 2, 'ENABLED')
ON DUPLICATE KEY UPDATE label = VALUES(label);
