-- M4 S-03: 实名人中介人（1:N）

CREATE TABLE IF NOT EXISTS oa_realname_intermediary (
    id                          BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id                   BIGINT         NOT NULL,
    realname_id                 BIGINT         NOT NULL,
    intermediary_name           VARCHAR(64)    NOT NULL,
    intermediary_phone_encrypted VARCHAR(128)  NULL,
    intermediary_wechat         VARCHAR(64)    NULL,
    relation_type               VARCHAR(32)    NOT NULL,
    commission_rate             DECIMAL(5, 2)  NOT NULL DEFAULT 0,
    remark                      VARCHAR(200)   NULL,
    creator                     VARCHAR(64)    DEFAULT 'system',
    create_time                 TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater                     VARCHAR(64)    DEFAULT 'system',
    update_time                 TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                     SMALLINT       NOT NULL DEFAULT 0,
    KEY idx_oa_realname_intermediary_realname (tenant_id, realname_id)
);

INSERT INTO sys_dict_type (type, name, status) VALUES
('dict_intermediary_relation', '中介人关系类型', 'ENABLED');

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_intermediary_relation', '直签', 'DIRECT', 1, 'ENABLED'),
('dict_intermediary_relation', '中介代理', 'INTERMEDIARY', 2, 'ENABLED'),
('dict_intermediary_relation', '机构合作', 'AGENCY', 3, 'ENABLED');
