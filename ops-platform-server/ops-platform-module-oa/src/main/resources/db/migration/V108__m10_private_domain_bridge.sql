-- M10-AO-S-07: 私域转化桥接表骨架（ADR-045 · a1f1ec21 P2 AI 预留）

CREATE TABLE IF NOT EXISTS oa_private_domain_conversion_bridge (
    id                  BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id           BIGINT         NOT NULL,
    source_type         VARCHAR(32)    NOT NULL COMMENT '来源身份类型 dict_private_domain_identity_type',
    source_id           BIGINT         NOT NULL COMMENT '来源实体主键',
    target_type         VARCHAR(32)    NOT NULL COMMENT '目标身份类型 dict_private_domain_identity_type',
    target_id           BIGINT         NOT NULL COMMENT '目标实体主键',
    match_method        VARCHAR(32)    NOT NULL COMMENT '匹配方式 dict_private_domain_match_method',
    confidence          DECIMAL(5, 2)  NULL COMMENT '置信度 0~100',
    match_evidence_json TEXT           NULL COMMENT '匹配证据 JSON',
    review_status       VARCHAR(32)    NOT NULL DEFAULT 'PENDING' COMMENT '审核状态 dict_private_domain_review_status',
    linked_by           VARCHAR(64)    NULL,
    linked_at           TIMESTAMP      NULL,
    creator             VARCHAR(64)    DEFAULT 'system',
    create_time         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater             VARCHAR(64)    DEFAULT 'system',
    update_time         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             SMALLINT       NOT NULL DEFAULT 0,
    UNIQUE KEY uk_oa_pd_bridge_pair (tenant_id, source_type, source_id, target_type, target_id),
    KEY idx_oa_pd_bridge_review (tenant_id, review_status),
    KEY idx_oa_pd_bridge_source (tenant_id, source_type, source_id),
    KEY idx_oa_pd_bridge_target (tenant_id, target_type, target_id)
);

INSERT INTO sys_dict_type (type, name, status) VALUES
('dict_private_domain_identity_type', '私域身份类型', 'ENABLED'),
('dict_private_domain_match_method', '私域匹配方式', 'ENABLED'),
('dict_private_domain_review_status', '私域桥接审核状态', 'ENABLED')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_private_domain_identity_type', '奥创好友', 'AOCHUANG_FRIEND', 1, 'ENABLED'),
('dict_private_domain_identity_type', '公众号粉丝', 'MP_FOLLOWER', 2, 'ENABLED'),
('dict_private_domain_identity_type', '企微客户', 'WECOM_CUSTOMER', 3, 'ENABLED'),
('dict_private_domain_identity_type', '企微员工', 'WECOM_EMPLOYEE', 4, 'ENABLED'),
('dict_private_domain_identity_type', '手机', 'PHONE', 5, 'ENABLED'),
('dict_private_domain_identity_type', '实名人', 'REALNAME', 6, 'ENABLED'),
('dict_private_domain_match_method', '人工', 'MANUAL', 1, 'ENABLED'),
('dict_private_domain_match_method', '规则', 'RULE', 2, 'ENABLED'),
('dict_private_domain_match_method', '手机号', 'PHONE', 3, 'ENABLED'),
('dict_private_domain_match_method', 'UnionID', 'UNIONID', 4, 'ENABLED'),
('dict_private_domain_match_method', 'AI', 'AI', 5, 'ENABLED'),
('dict_private_domain_review_status', '待审核', 'PENDING', 1, 'ENABLED'),
('dict_private_domain_review_status', '已通过', 'APPROVED', 2, 'ENABLED'),
('dict_private_domain_review_status', '已驳回', 'REJECTED', 3, 'ENABLED')
ON DUPLICATE KEY UPDATE label = VALUES(label);
