-- M4 S-02: 实名人管理

CREATE TABLE IF NOT EXISTS oa_realname (
    id                  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '实名人ID',
    tenant_id           BIGINT       NOT NULL COMMENT '租户ID',
    company_id          BIGINT       NULL COMMENT '所属公司ID',
    real_name           VARCHAR(64)  NOT NULL COMMENT '真实姓名',
    id_type             VARCHAR(32)  NOT NULL DEFAULT 'ID_CARD' COMMENT '证件类型: ID_CARD-身份证, PASSPORT-护照',
    id_card_encrypted   VARCHAR(128) NOT NULL COMMENT '身份证号（加密）',
    phone_encrypted     VARCHAR(128) NOT NULL COMMENT '手机号（加密）',
    wechat              VARCHAR(64)  NULL COMMENT '微信号',
    gender              VARCHAR(16)  NULL COMMENT '性别: MALE-男, FEMALE-女',
    status              VARCHAR(32)  NOT NULL DEFAULT 'ENABLED' COMMENT '状态: ENABLED-启用, DISABLED-停用',
    account_bound_count INT          NOT NULL DEFAULT 0 COMMENT '已绑定账号数量',
    creator             VARCHAR(64)  DEFAULT 'system' COMMENT '创建人',
    create_time         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater             VARCHAR(64)  DEFAULT 'system' COMMENT '更新人',
    update_time         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted             SMALLINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除标志: 0-未删除, 1-已删除',
    KEY idx_oa_realname_tenant (tenant_id),
    KEY idx_oa_realname_company (tenant_id, company_id),
    KEY idx_oa_realname_name (tenant_id, real_name)
) COMMENT='实名人信息表';

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
