-- M4 S-04: 手机管理

CREATE TABLE IF NOT EXISTS oa_phone (
    id                      BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '手机ID',
    tenant_id               BIGINT       NOT NULL COMMENT '租户ID',
    realname_id             BIGINT       NULL COMMENT '实名人ID',
    phone_number_encrypted  VARCHAR(128) NOT NULL COMMENT '手机号（加密）',
    phone_number_hash       VARCHAR(64)  NOT NULL COMMENT '手机号哈希值',
    phone_code              VARCHAR(32)  NULL COMMENT '手机验证码',
    phone_model             VARCHAR(100) NULL COMMENT '手机型号',
    keeper_id               BIGINT       NULL COMMENT '保管人ID',
    wechat_bound            VARCHAR(64)  NULL COMMENT '绑定的微信号',
    status                  VARCHAR(32)  NOT NULL DEFAULT 'ENABLED' COMMENT '状态: ENABLED-在用, DISABLED-停用',
    account_bound_count     INT          NOT NULL DEFAULT 0 COMMENT '已绑定账号数量',
    creator                 VARCHAR(64)  DEFAULT 'system' COMMENT '创建人',
    create_time             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater                 VARCHAR(64)  DEFAULT 'system' COMMENT '更新人',
    update_time             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted                 SMALLINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除标志: 0-未删除, 1-已删除',
    UNIQUE KEY uk_oa_phone_number (tenant_id, phone_number_hash),
    KEY idx_oa_phone_tenant (tenant_id),
    KEY idx_oa_phone_realname (tenant_id, realname_id)
) COMMENT='手机信息表';

INSERT INTO sys_dict_type (type, name, status) VALUES
('dict_phone_status', '手机状态', 'ENABLED');

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_phone_status', '在用', 'ENABLED', 1, 'ENABLED'),
('dict_phone_status', '停用', 'DISABLED', 2, 'ENABLED');
