-- M8 配置管理：采集 / 阈值 / AI 模型 / 提示词

CREATE TABLE IF NOT EXISTS oa_collect_config (
    id                  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id           BIGINT       NOT NULL,
    scope               VARCHAR(32)  NOT NULL,
    config_name         VARCHAR(128) NOT NULL,
    sub_type            VARCHAR(32)  NULL,
    platform_type       VARCHAR(64)  NULL,
    account_id          BIGINT       NULL,
    collect_frequency   VARCHAR(32)  NULL,
    collect_method      VARCHAR(32)  NULL,
    api_url             VARCHAR(512) NULL,
    api_key_encrypted   VARCHAR(512) NULL,
    request_method      VARCHAR(16)  NULL,
    request_params      TEXT         NULL,
    response_mapping    TEXT         NULL,
    collect_fields      TEXT         NULL,
    status              VARCHAR(32)  NOT NULL DEFAULT 'ENABLED',
    remark              VARCHAR(512) NULL,
    creator             VARCHAR(64)  DEFAULT 'system',
    create_time         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater             VARCHAR(64)  DEFAULT 'system',
    update_time         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             SMALLINT     NOT NULL DEFAULT 0,
    KEY idx_oa_collect_config_tenant_scope (tenant_id, scope)
);

CREATE TABLE IF NOT EXISTS oa_threshold_config (
    id                  BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id           BIGINT        NOT NULL,
    metric_name         VARCHAR(128)  NOT NULL,
    metric_type         VARCHAR(64)   NOT NULL,
    platform_type       VARCHAR(64)   NULL,
    ip_group_id         BIGINT        NULL,
    compare_operator    VARCHAR(16)   NOT NULL DEFAULT 'GTE',
    threshold_value     DECIMAL(18,4) NOT NULL,
    alert_level         VARCHAR(32)   NOT NULL DEFAULT 'WARNING',
    notify_methods      VARCHAR(256)  NULL,
    status              VARCHAR(32)   NOT NULL DEFAULT 'ENABLED',
    remark              VARCHAR(512)  NULL,
    creator             VARCHAR(64)   DEFAULT 'system',
    create_time         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater             VARCHAR(64)   DEFAULT 'system',
    update_time         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             SMALLINT      NOT NULL DEFAULT 0,
    KEY idx_oa_threshold_config_tenant (tenant_id)
);

CREATE TABLE IF NOT EXISTS oa_ai_model_config (
    id                  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id           BIGINT       NOT NULL,
    model_name          VARCHAR(128) NOT NULL,
    model_type          VARCHAR(64)  NULL,
    api_endpoint        VARCHAR(512) NULL,
    api_key_encrypted   VARCHAR(512) NULL,
    max_tokens          INT          NULL,
    temperature         DECIMAL(4,2) NULL,
    top_p               DECIMAL(4,2) NULL,
    status              VARCHAR(32)  NOT NULL DEFAULT 'ENABLED',
    remark              VARCHAR(512) NULL,
    creator             VARCHAR(64)  DEFAULT 'system',
    create_time         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater             VARCHAR(64)  DEFAULT 'system',
    update_time         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             SMALLINT     NOT NULL DEFAULT 0,
    KEY idx_oa_ai_model_config_tenant (tenant_id)
);

CREATE TABLE IF NOT EXISTS oa_ai_prompt_config (
    id                  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id           BIGINT       NOT NULL,
    template_name       VARCHAR(128) NOT NULL,
    scene               VARCHAR(64)  NULL,
    prompt_content      TEXT         NOT NULL,
    variable_desc       TEXT         NULL,
    temperature         DECIMAL(4,2) NULL,
    status              VARCHAR(32)  NOT NULL DEFAULT 'ENABLED',
    remark              VARCHAR(512) NULL,
    creator             VARCHAR(64)  DEFAULT 'system',
    create_time         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater             VARCHAR(64)  DEFAULT 'system',
    update_time         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             SMALLINT     NOT NULL DEFAULT 0,
    KEY idx_oa_ai_prompt_config_tenant (tenant_id)
);

-- M8 字典
INSERT INTO sys_dict_type (type, name, status) VALUES
('dict_collect_frequency', '采集频率', 'ENABLED'),
('dict_collect_method', '采集方式', 'ENABLED'),
('dict_config_status', '配置状态', 'ENABLED'),
('dict_threshold_metric', '阈值指标', 'ENABLED'),
('dict_third_platform', '第三方平台', 'ENABLED'),
('dict_ecom_platform', '电商平台', 'ENABLED'),
('dict_sync_frequency', '同步频率', 'ENABLED'),
('dict_ai_model_type', 'AI模型类型', 'ENABLED'),
('dict_ai_scene', 'AI应用场景', 'ENABLED'),
('dict_compare_operator', '比较符', 'ENABLED'),
('dict_alert_level', '告警级别', 'ENABLED')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_collect_frequency', '每日', 'DAILY', 1, 'ENABLED'),
('dict_collect_frequency', '每小时', 'HOURLY', 2, 'ENABLED'),
('dict_collect_frequency', '每周', 'WEEKLY', 3, 'ENABLED'),
('dict_collect_method', '内部采集', 'INTERNAL', 1, 'ENABLED'),
('dict_collect_method', 'API对接', 'API', 2, 'ENABLED'),
('dict_collect_method', '爬虫', 'CRAWLER', 3, 'ENABLED'),
('dict_config_status', '启用', 'ENABLED', 1, 'ENABLED'),
('dict_config_status', '停用', 'DISABLED', 2, 'ENABLED'),
('dict_threshold_metric', '爆款阈值', 'HIT_THRESHOLD', 1, 'ENABLED'),
('dict_threshold_metric', '低分阈值', 'LOW_SCORE', 2, 'ENABLED'),
('dict_threshold_metric', '粉丝预警', 'FAN_ALERT', 3, 'ENABLED'),
('dict_third_platform', '新榜', 'NEWRANK', 1, 'ENABLED'),
('dict_third_platform', '飞瓜', 'FEIGUA', 2, 'ENABLED'),
('dict_ecom_platform', '淘宝', 'TAOBAO', 1, 'ENABLED'),
('dict_ecom_platform', '京东', 'JD', 2, 'ENABLED'),
('dict_ecom_platform', '拼多多', 'PDD', 3, 'ENABLED'),
('dict_sync_frequency', '每日', 'DAILY', 1, 'ENABLED'),
('dict_sync_frequency', '每小时', 'HOURLY', 2, 'ENABLED'),
('dict_ai_model_type', 'GPT', 'GPT', 1, 'ENABLED'),
('dict_ai_model_type', 'Claude', 'CLAUDE', 2, 'ENABLED'),
('dict_ai_model_type', 'Gemini', 'GEMINI', 3, 'ENABLED'),
('dict_ai_scene', '内容生成', 'CONTENT_GEN', 1, 'ENABLED'),
('dict_ai_scene', '标题优化', 'TITLE_OPT', 2, 'ENABLED'),
('dict_compare_operator', '大于等于', 'GTE', 1, 'ENABLED'),
('dict_compare_operator', '小于等于', 'LTE', 2, 'ENABLED'),
('dict_compare_operator', '等于', 'EQ', 3, 'ENABLED'),
('dict_alert_level', '提示', 'INFO', 1, 'ENABLED'),
('dict_alert_level', '警告', 'WARNING', 2, 'ENABLED'),
('dict_alert_level', '严重', 'CRITICAL', 3, 'ENABLED')
ON DUPLICATE KEY UPDATE label = VALUES(label);
