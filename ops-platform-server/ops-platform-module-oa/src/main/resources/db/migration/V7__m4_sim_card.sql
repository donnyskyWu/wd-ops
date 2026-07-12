-- M4 S-05: 手机卡 + 跨平台账号聚合（最小 oa_account 表供 linked-accounts 查询）

CREATE TABLE IF NOT EXISTS oa_sim_card (
    id                      BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '手机卡ID',
    tenant_id               BIGINT       NOT NULL COMMENT '租户ID',
    phone_id                BIGINT       NULL COMMENT '所属手机ID',
    phone_number_encrypted  VARCHAR(128) NOT NULL COMMENT '手机号（加密）',
    phone_number_hash       VARCHAR(64)  NOT NULL COMMENT '手机号哈希值',
    is_primary              VARCHAR(8)   NOT NULL DEFAULT 'YES' COMMENT '是否主卡: YES-是, NO-否',
    operator                VARCHAR(32)  NOT NULL COMMENT '运营商: MOBILE-中国移动, UNICOM-中国联通, TELECOM-中国电信',
    assigned_user_id        BIGINT       NOT NULL COMMENT '分配用户ID',
    iccid_encrypted         VARCHAR(128) NULL COMMENT 'ICCID（加密）',
    iccid_hash              VARCHAR(64)  NULL COMMENT 'ICCID哈希值',
    package_name            VARCHAR(100) NULL COMMENT '套餐名称',
    status                  VARCHAR(32)  NOT NULL DEFAULT 'ENABLED' COMMENT '状态: ENABLED-在用, DISABLED-停用',
    account_bound_count     INT          NOT NULL DEFAULT 0 COMMENT '已绑定账号数量',
    creator                 VARCHAR(64)  DEFAULT 'system' COMMENT '创建人',
    create_time             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater                 VARCHAR(64)  DEFAULT 'system' COMMENT '更新人',
    update_time             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted                 SMALLINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除标志: 0-未删除, 1-已删除',
    UNIQUE KEY uk_oa_sim_phone (tenant_id, phone_number_hash),
    KEY idx_oa_sim_tenant (tenant_id),
    KEY idx_oa_sim_phone_id (tenant_id, phone_id),
    KEY idx_oa_sim_operator (tenant_id, operator)
) COMMENT='手机卡信息表';

CREATE TABLE IF NOT EXISTS oa_account (
    id                  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '账号ID',
    tenant_id           BIGINT       NOT NULL COMMENT '租户ID',
    platform_type       VARCHAR(32)  NOT NULL COMMENT '平台类型: WECHAT_OFFICIAL-微信公众号, DOUYIN-抖音, WEWORK-企业微信等',
    account_name        VARCHAR(128) NOT NULL COMMENT '账号名称',
    external_account_id VARCHAR(64)  NULL COMMENT '外部平台账号ID',
    phone_id            BIGINT       NULL COMMENT '绑定手机ID',
    phone_number_hash   VARCHAR(64)  NULL COMMENT '手机号哈希值',
    sim_card_id         BIGINT       NULL COMMENT '绑定手机卡ID',
    status              VARCHAR(32)  NOT NULL DEFAULT 'NORMAL' COMMENT '账号状态: NORMAL-正常, DISABLED-停用',
    linked_at           TIMESTAMP    NULL COMMENT '关联时间',
    creator             VARCHAR(64)  DEFAULT 'system' COMMENT '创建人',
    create_time         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater             VARCHAR(64)  DEFAULT 'system' COMMENT '更新人',
    update_time         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted             SMALLINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除标志: 0-未删除, 1-已删除',
    KEY idx_oa_account_tenant (tenant_id),
    KEY idx_oa_account_phone (tenant_id, phone_id),
    KEY idx_oa_account_phone_hash (tenant_id, phone_number_hash),
    KEY idx_oa_account_sim (tenant_id, sim_card_id),
    KEY idx_oa_account_platform (tenant_id, platform_type)
) COMMENT='跨平台账号聚合表';

INSERT INTO sys_dict_type (type, name, status) VALUES
('dict_sim_operator', 'SIM运营商', 'ENABLED'),
('dict_sim_status', '手机卡状态', 'ENABLED'),
('dict_account_status', '账号状态', 'ENABLED');

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_sim_operator', '中国移动', 'MOBILE', 1, 'ENABLED'),
('dict_sim_operator', '中国联通', 'UNICOM', 2, 'ENABLED'),
('dict_sim_operator', '中国电信', 'TELECOM', 3, 'ENABLED'),
('dict_sim_status', '在用', 'ENABLED', 1, 'ENABLED'),
('dict_sim_status', '停用', 'DISABLED', 2, 'ENABLED'),
('dict_account_status', '正常', 'NORMAL', 1, 'ENABLED'),
('dict_account_status', '停用', 'DISABLED', 2, 'ENABLED'),
('dict_platform_type', '企业微信', 'WEWORK', 3, 'ENABLED');
