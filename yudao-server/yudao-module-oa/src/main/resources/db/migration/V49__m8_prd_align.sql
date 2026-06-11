-- V49: M8 配置管理 PRD 对齐（ADR-014）

-- oa_collect_config 扩展
ALTER TABLE oa_collect_config ADD COLUMN account_identifier VARCHAR(100) NULL COMMENT '账号标识' AFTER account_id;
ALTER TABLE oa_collect_config ADD COLUMN app_id VARCHAR(100) NULL AFTER account_identifier;
ALTER TABLE oa_collect_config ADD COLUMN app_secret_encrypted VARCHAR(512) NULL AFTER app_id;
ALTER TABLE oa_collect_config ADD COLUMN cookie TEXT NULL AFTER app_secret_encrypted;
ALTER TABLE oa_collect_config ADD COLUMN auth_token_encrypted VARCHAR(512) NULL AFTER cookie;
ALTER TABLE oa_collect_config ADD COLUMN field_mapping TEXT NULL AFTER auth_token_encrypted;
ALTER TABLE oa_collect_config ADD COLUMN is_live TINYINT NOT NULL DEFAULT 0 AFTER field_mapping;
ALTER TABLE oa_collect_config ADD COLUMN db_host VARCHAR(50) NULL AFTER is_live;
ALTER TABLE oa_collect_config ADD COLUMN db_port INT NULL DEFAULT 3306 AFTER db_host;
ALTER TABLE oa_collect_config ADD COLUMN db_name VARCHAR(100) NULL AFTER db_port;
ALTER TABLE oa_collect_config ADD COLUMN db_username VARCHAR(100) NULL AFTER db_name;
ALTER TABLE oa_collect_config ADD COLUMN db_password_encrypted VARCHAR(512) NULL AFTER db_username;
ALTER TABLE oa_collect_config ADD COLUMN table_name VARCHAR(100) NULL DEFAULT 'pay_all_order' AFTER db_password_encrypted;
ALTER TABLE oa_collect_config ADD COLUMN sync_mode VARCHAR(20) NULL DEFAULT 'INCREMENTAL' AFTER table_name;
ALTER TABLE oa_collect_config ADD COLUMN conn_status VARCHAR(20) NULL DEFAULT 'DISCONNECTED' AFTER sync_mode;

-- oa_threshold_config 扩展
ALTER TABLE oa_threshold_config ADD COLUMN threshold_category VARCHAR(32) NOT NULL DEFAULT 'ALERT' AFTER tenant_id;
ALTER TABLE oa_threshold_config ADD COLUMN threshold_type VARCHAR(20) NULL AFTER threshold_category;
ALTER TABLE oa_threshold_config ADD COLUMN content_type VARCHAR(20) NULL AFTER platform_type;
ALTER TABLE oa_threshold_config ADD COLUMN judge_mode VARCHAR(10) NULL DEFAULT 'AND' AFTER content_type;
ALTER TABLE oa_threshold_config ADD COLUMN low_fans BIGINT NULL AFTER judge_mode;
ALTER TABLE oa_threshold_config ADD COLUMN high_fans BIGINT NULL AFTER low_fans;
ALTER TABLE oa_threshold_config ADD COLUMN daily_low INT NULL AFTER high_fans;
ALTER TABLE oa_threshold_config ADD COLUMN daily_high INT NULL AFTER daily_low;
ALTER TABLE oa_threshold_config ADD COLUMN hot_value BIGINT NULL AFTER daily_high;
ALTER TABLE oa_threshold_config ADD COLUMN low_value BIGINT NULL AFTER hot_value;
ALTER TABLE oa_threshold_config ADD COLUMN override_account_id BIGINT NULL AFTER low_value;
ALTER TABLE oa_threshold_config ADD COLUMN override_value BIGINT NULL AFTER override_account_id;

-- oa_ai_model_config 扩展
ALTER TABLE oa_ai_model_config ADD COLUMN model_id VARCHAR(100) NULL AFTER model_name;
ALTER TABLE oa_ai_model_config ADD COLUMN timeout INT NULL DEFAULT 60 AFTER max_tokens;
ALTER TABLE oa_ai_model_config ADD COLUMN is_default TINYINT NOT NULL DEFAULT 0 AFTER timeout;
ALTER TABLE oa_ai_model_config ADD COLUMN conn_status VARCHAR(20) NULL DEFAULT 'DISCONNECTED' AFTER is_default;

-- oa_ai_prompt_config 扩展
ALTER TABLE oa_ai_prompt_config ADD COLUMN version VARCHAR(20) NOT NULL DEFAULT 'v1' AFTER template_name;

-- 关键词配置表
CREATE TABLE IF NOT EXISTS oa_config_keyword (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    platform        VARCHAR(50)  NOT NULL,
    keyword         VARCHAR(100) NOT NULL,
    match_type      VARCHAR(20)  NOT NULL DEFAULT 'FUZZY',
    status          VARCHAR(32)  NOT NULL DEFAULT 'ENABLED',
    creator         VARCHAR(64)  DEFAULT 'system',
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         VARCHAR(64)  DEFAULT 'system',
    update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT     NOT NULL DEFAULT 0,
    KEY idx_oa_config_keyword_tenant (tenant_id)
);

-- 奥创接口配置表
CREATE TABLE IF NOT EXISTS oa_aocreate_api (
    id                  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id           BIGINT       NOT NULL,
    api_url             VARCHAR(255) NOT NULL,
    app_id              VARCHAR(100) NOT NULL,
    app_secret_encrypted VARCHAR(512) NULL,
    token_encrypted     VARCHAR(512) NULL,
    status              VARCHAR(32)  NOT NULL DEFAULT 'ENABLED',
    daily_quota         INT          NOT NULL DEFAULT 10000,
    current_usage       INT          NOT NULL DEFAULT 0,
    creator             VARCHAR(64)  DEFAULT 'system',
    create_time         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater             VARCHAR(64)  DEFAULT 'system',
    update_time         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             SMALLINT     NOT NULL DEFAULT 0,
    UNIQUE KEY uk_oa_aocreate_api_tenant (tenant_id)
);

-- 字典补充
INSERT INTO sys_dict_type (type, name, status) VALUES
('dict_threshold_category', '阈值分类', 'ENABLED'),
('dict_match_type', '关键词匹配类型', 'ENABLED'),
('dict_sync_mode', '采集同步模式', 'ENABLED'),
('dict_threshold_type', '阈值类型', 'ENABLED'),
('dict_content_type', '内容类型', 'ENABLED'),
('dict_judge_mode', '判定模式', 'ENABLED'),
('dict_conn_status', '连接状态', 'ENABLED'),
('dict_prompt_type', '提示词类型', 'ENABLED'),
('dict_notify_channel', '通知渠道', 'ENABLED')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_platform_type', '服务号', 'SERVICE_ACCOUNT', 8, 'ENABLED'),
('dict_threshold_category', '预警阈值', 'ALERT', 1, 'ENABLED'),
('dict_threshold_category', '粉丝阈值', 'FANS', 2, 'ENABLED'),
('dict_threshold_category', '作品阈值', 'WORK', 3, 'ENABLED'),
('dict_threshold_category', '账号覆盖', 'OVERRIDE', 4, 'ENABLED'),
('dict_match_type', '模糊匹配', 'FUZZY', 1, 'ENABLED'),
('dict_match_type', '精确匹配', 'EXACT', 2, 'ENABLED'),
('dict_sync_mode', '增量', 'INCREMENTAL', 1, 'ENABLED'),
('dict_sync_mode', '全量', 'FULL', 2, 'ENABLED'),
('dict_threshold_type', '百分比', 'PERCENTAGE', 1, 'ENABLED'),
('dict_threshold_type', '绝对值', 'ABSOLUTE', 2, 'ENABLED'),
('dict_content_type', '文章', 'ARTICLE', 1, 'ENABLED'),
('dict_content_type', '短视频', 'SHORT_VIDEO', 2, 'ENABLED'),
('dict_judge_mode', '全部满足', 'AND', 1, 'ENABLED'),
('dict_judge_mode', '任一满足', 'OR', 2, 'ENABLED'),
('dict_conn_status', '已连接', 'CONNECTED', 1, 'ENABLED'),
('dict_conn_status', '未连接', 'DISCONNECTED', 2, 'ENABLED'),
('dict_prompt_type', '视频分析', 'VIDEO_ANALYSIS', 1, 'ENABLED'),
('dict_prompt_type', '图文分析', 'IMAGE_TEXT_ANALYSIS', 2, 'ENABLED'),
('dict_prompt_type', '数据解读', 'DATA_INTERPRET', 3, 'ENABLED'),
('dict_prompt_type', '内容生成', 'CONTENT_GEN', 4, 'ENABLED'),
('dict_notify_channel', '站内消息', 'IN_APP', 1, 'ENABLED'),
('dict_notify_channel', '钉钉', 'DINGTALK', 2, 'ENABLED'),
('dict_notify_channel', '短信', 'SMS', 3, 'ENABLED')
ON DUPLICATE KEY UPDATE label = VALUES(label);
