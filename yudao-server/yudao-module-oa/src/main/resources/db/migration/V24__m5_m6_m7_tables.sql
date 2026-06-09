-- M5/M6/M7 表结构 + 字典

-- ========== M5 财务管理 ==========
CREATE TABLE IF NOT EXISTS oa_account_cost (
    id              BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT        NOT NULL,
    account_id      BIGINT        NOT NULL,
    cost_type       VARCHAR(32)   NOT NULL,
    amount          DECIMAL(16,2) NOT NULL,
    pay_method      VARCHAR(32)   NOT NULL,
    pay_date        DATE          NOT NULL,
    period          VARCHAR(32)   NOT NULL DEFAULT 'ONCE',
    remark          VARCHAR(500)  NULL,
    attachment_id   BIGINT        NULL,
    creator         VARCHAR(64)   DEFAULT 'system',
    create_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         VARCHAR(64)   DEFAULT 'system',
    update_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT      NOT NULL DEFAULT 0,
    KEY idx_oa_account_cost_tenant (tenant_id),
    KEY idx_oa_account_cost_account (tenant_id, account_id),
    KEY idx_oa_account_cost_date (tenant_id, pay_date)
);

-- ========== M6 数据分析 ==========
CREATE TABLE IF NOT EXISTS oa_funnel (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    funnel_name     VARCHAR(100) NOT NULL,
    funnel_type     VARCHAR(32)  NOT NULL DEFAULT 'CUSTOM',
    status          TINYINT      NOT NULL DEFAULT 1,
    remark          VARCHAR(500) NULL,
    creator         VARCHAR(64)  DEFAULT 'system',
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         VARCHAR(64)  DEFAULT 'system',
    update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT     NOT NULL DEFAULT 0,
    KEY idx_oa_funnel_tenant (tenant_id)
);

CREATE TABLE IF NOT EXISTS oa_funnel_step (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    funnel_id       BIGINT       NOT NULL,
    step_order      INT          NOT NULL,
    event_code      VARCHAR(64)  NOT NULL,
    step_name       VARCHAR(100) NULL,
    creator         VARCHAR(64)  DEFAULT 'system',
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         VARCHAR(64)  DEFAULT 'system',
    update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT     NOT NULL DEFAULT 0,
    KEY idx_oa_funnel_step_funnel (funnel_id)
);

CREATE TABLE IF NOT EXISTS oa_custom_query (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    query_name      VARCHAR(100) NOT NULL,
    status          VARCHAR(32)  NOT NULL DEFAULT 'DRAFT',
    sql_text        TEXT         NOT NULL,
    params_json     TEXT         NULL,
    creator         VARCHAR(64)  DEFAULT 'system',
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         VARCHAR(64)  DEFAULT 'system',
    update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT     NOT NULL DEFAULT 0,
    KEY idx_oa_custom_query_tenant (tenant_id),
    KEY idx_oa_custom_query_status (tenant_id, status)
);

CREATE TABLE IF NOT EXISTS oa_dashboard (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    dashboard_name  VARCHAR(100) NOT NULL,
    dashboard_type  VARCHAR(32)  NOT NULL DEFAULT 'BUSINESS',
    layout_json     TEXT         NULL,
    status          TINYINT      NOT NULL DEFAULT 1,
    creator         VARCHAR(64)  DEFAULT 'system',
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         VARCHAR(64)  DEFAULT 'system',
    update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT     NOT NULL DEFAULT 0,
    KEY idx_oa_dashboard_tenant (tenant_id)
);

-- ========== M7 作品监测 ==========
CREATE TABLE IF NOT EXISTS oa_external_work (
    id                  BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id           BIGINT        NOT NULL,
    account_id          BIGINT        NULL,
    platform_type       VARCHAR(32)   NOT NULL,
    title               VARCHAR(200)  NOT NULL,
    work_url            VARCHAR(500)  NULL,
    play_count          BIGINT        NOT NULL DEFAULT 0,
    completion_rate     DECIMAL(6,4)  NULL,
    like_count          INT           NOT NULL DEFAULT 0,
    publish_time        TIMESTAMP     NULL,
    industry            VARCHAR(32)   NULL,
    ip_group_id         BIGINT        NULL,
    is_external         TINYINT       NOT NULL DEFAULT 1,
    creator             VARCHAR(64)   DEFAULT 'system',
    create_time         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater             VARCHAR(64)   DEFAULT 'system',
    update_time         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             SMALLINT      NOT NULL DEFAULT 0,
    KEY idx_oa_external_work_tenant (tenant_id),
    KEY idx_oa_external_work_play (tenant_id, play_count),
    KEY idx_oa_external_work_industry (tenant_id, industry)
);

-- ========== 字典 ==========
INSERT INTO sys_dict_type (type, name, status) VALUES
('dict_cost_type', '成本类型', 'ENABLED'),
('dict_cost_pay_method', '支付方式', 'ENABLED'),
('dict_cost_period', '成本周期', 'ENABLED'),
('dict_funnel_type', '漏斗类型', 'ENABLED'),
('dict_query_status', '查询状态', 'ENABLED'),
('dict_dashboard_type', '大屏类型', 'ENABLED'),
('dict_monitor_freq', '监测频率', 'ENABLED'),
('dict_alert_level', '预警级别', 'ENABLED'),
('dict_perf_metric_type', '指标类型', 'ENABLED')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_cost_type', '购买成本', 'PURCHASE', 1, 'ENABLED'),
('dict_cost_type', '人力成本', 'PROCESS_HUMAN', 2, 'ENABLED'),
('dict_cost_type', '投放成本', 'AD_SPEND', 3, 'ENABLED'),
('dict_cost_pay_method', '微信', 'WECHAT', 1, 'ENABLED'),
('dict_cost_pay_method', '支付宝', 'ALIPAY', 2, 'ENABLED'),
('dict_cost_pay_method', '银行卡', 'BANK', 3, 'ENABLED'),
('dict_cost_pay_method', '对公转账', 'CORPORATE', 4, 'ENABLED'),
('dict_cost_period', '一次性', 'ONCE', 1, 'ENABLED'),
('dict_cost_period', '月度', 'MONTH', 2, 'ENABLED'),
('dict_cost_period', '季度', 'QUARTER', 3, 'ENABLED'),
('dict_funnel_type', '自定义', 'CUSTOM', 1, 'ENABLED'),
('dict_funnel_type', '转化', 'CONVERSION', 2, 'ENABLED'),
('dict_query_status', '草稿', 'DRAFT', 1, 'ENABLED'),
('dict_query_status', '已发布', 'PUBLISHED', 2, 'ENABLED'),
('dict_dashboard_type', '业务大屏', 'BUSINESS', 1, 'ENABLED'),
('dict_dashboard_type', '运营大屏', 'OPS', 2, 'ENABLED'),
('dict_monitor_freq', '5分钟', '5MIN', 1, 'ENABLED'),
('dict_monitor_freq', '30分钟', '30MIN', 2, 'ENABLED'),
('dict_monitor_freq', '1小时', '1H', 3, 'ENABLED'),
('dict_monitor_freq', '24小时', '24H', 4, 'ENABLED'),
('dict_alert_level', '低', 'LOW', 1, 'ENABLED'),
('dict_alert_level', '中', 'MEDIUM', 2, 'ENABLED'),
('dict_alert_level', '高', 'HIGH', 3, 'ENABLED'),
('dict_alert_level', '紧急', 'CRITICAL', 4, 'ENABLED'),
('dict_perf_metric_type', '基础指标', 'BASIC', 1, 'ENABLED'),
('dict_perf_metric_type', '复合指标', 'COMPOSITE', 2, 'ENABLED')
ON DUPLICATE KEY UPDATE label = VALUES(label);

-- 分析指标（复用 oa_metric，category=ANALYTICS）
INSERT INTO oa_metric (id, tenant_id, metric_name, metric_code, unit, category, status, creator, updater) VALUES
(9601, 1, '全平台粉丝数', 'ANALYTICS_FOLLOWER_TOTAL', '人', 'ANALYTICS', 1, 'seed-analytics', 'seed-analytics'),
(9602, 1, '内容阅读量', 'ANALYTICS_READ_TOTAL', '次', 'ANALYTICS', 1, 'seed-analytics', 'seed-analytics'),
(9603, 1, '视频播放量', 'ANALYTICS_PLAY_TOTAL', '次', 'ANALYTICS', 1, 'seed-analytics', 'seed-analytics'),
(9604, 1, '账号成本合计', 'ANALYTICS_COST_TOTAL', '元', 'ANALYTICS', 1, 'seed-analytics', 'seed-analytics')
ON DUPLICATE KEY UPDATE metric_name = VALUES(metric_name);
