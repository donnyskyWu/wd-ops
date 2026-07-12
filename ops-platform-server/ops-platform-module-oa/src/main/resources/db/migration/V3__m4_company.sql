-- M4 S-01: 公司管理

CREATE TABLE IF NOT EXISTS oa_company (
    id                      BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '公司ID',
    tenant_id               BIGINT       NOT NULL COMMENT '租户ID',
    company_name            VARCHAR(100) NOT NULL COMMENT '公司名称',
    credit_code             VARCHAR(18)  NOT NULL COMMENT '统一社会信用代码',
    industry                VARCHAR(40)  NULL COMMENT '所属行业',
    address                 VARCHAR(200) NULL COMMENT '公司地址',
    legal_name              VARCHAR(64)  NULL COMMENT '法人姓名',
    legal_id_card_encrypted VARCHAR(128) NULL COMMENT '法人身份证号（加密）',
    mp_capacity_standard    INT          NOT NULL DEFAULT 0 COMMENT '公众号容量标准',
    mp_registered_count     INT          NOT NULL DEFAULT 0 COMMENT '已注册公众号数量',
    status                  VARCHAR(32)  NOT NULL DEFAULT 'ENABLED' COMMENT '公司状态: ENABLED-启用, DISABLED-停用',
    creator                 VARCHAR(64)  DEFAULT 'system' COMMENT '创建人',
    create_time             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater                 VARCHAR(64)  DEFAULT 'system' COMMENT '更新人',
    update_time             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted                 SMALLINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除标志: 0-未删除, 1-已删除',
    UNIQUE KEY uk_oa_company_credit (tenant_id, credit_code),
    KEY idx_oa_company_tenant (tenant_id)
) COMMENT='公司信息表';

CREATE TABLE IF NOT EXISTS oa_company_expansion (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '扩容记录ID',
    tenant_id       BIGINT       NOT NULL COMMENT '租户ID',
    company_id      BIGINT       NOT NULL COMMENT '公司ID',
    from_capacity   INT          NOT NULL COMMENT '扩容前容量',
    to_capacity     INT          NOT NULL COMMENT '扩容后容量',
    reason          VARCHAR(200) NOT NULL COMMENT '扩容原因',
    operator_name   VARCHAR(64)  NULL COMMENT '操作人姓名',
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_oa_company_expansion_company (tenant_id, company_id)
) COMMENT='公司扩容记录表';

INSERT INTO sys_dict_type (type, name, status) VALUES
('dict_company_status', '公司状态', 'ENABLED');

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_company_status', '启用', 'ENABLED', 1, 'ENABLED'),
('dict_company_status', '停用', 'DISABLED', 2, 'ENABLED');
