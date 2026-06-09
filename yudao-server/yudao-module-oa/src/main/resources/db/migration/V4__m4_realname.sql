-- M4 S-02: 实名人管理

CREATE TABLE IF NOT EXISTS oa_realname (
    id                  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id           BIGINT       NOT NULL,
    company_id          BIGINT       NULL,
    real_name           VARCHAR(64)  NOT NULL,
    id_type             VARCHAR(32)  NOT NULL DEFAULT 'ID_CARD',
    id_card_encrypted   VARCHAR(128) NOT NULL,
    phone_encrypted     VARCHAR(128) NOT NULL,
    wechat              VARCHAR(64)  NULL,
    gender              VARCHAR(16)  NULL,
    status              VARCHAR(32)  NOT NULL DEFAULT 'ENABLED',
    account_bound_count INT          NOT NULL DEFAULT 0,
    creator             VARCHAR(64)  DEFAULT 'system',
    create_time         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater             VARCHAR(64)  DEFAULT 'system',
    update_time         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             SMALLINT     NOT NULL DEFAULT 0,
    KEY idx_oa_realname_tenant (tenant_id),
    KEY idx_oa_realname_company (tenant_id, company_id),
    KEY idx_oa_realname_name (tenant_id, real_name)
);

INSERT INTO sys_dict_type (type, name, status) VALUES
('dict_id_type', '证件类型', 'ENABLED'),
('dict_gender', '性别', 'ENABLED'),
('dict_realname_status', '实名人状态', 'ENABLED');

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_id_type', '身份证', 'ID_CARD', 1, 'ENABLED'),
('dict_id_type', '护照', 'PASSPORT', 2, 'ENABLED'),
('dict_gender', '男', 'MALE', 1, 'ENABLED'),
('dict_gender', '女', 'FEMALE', 2, 'ENABLED'),
('dict_realname_status', '启用', 'ENABLED', 1, 'ENABLED'),
('dict_realname_status', '停用', 'DISABLED', 2, 'ENABLED');
