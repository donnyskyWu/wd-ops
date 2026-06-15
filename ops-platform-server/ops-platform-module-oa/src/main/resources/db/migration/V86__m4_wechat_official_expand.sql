-- M4: 公众号字段扩展 + 续费认证记录

ALTER TABLE oa_account ADD COLUMN trademark_name VARCHAR(128) NULL COMMENT '商标名称';
ALTER TABLE oa_account ADD COLUMN email VARCHAR(128) NULL COMMENT '邮箱';
ALTER TABLE oa_account ADD COLUMN password_encrypted VARCHAR(512) NULL COMMENT '登录密码 AES-256';
ALTER TABLE oa_account ADD COLUMN qualification_type VARCHAR(32) NULL COMMENT '资质类型 dict_qualification_type';
ALTER TABLE oa_account ADD COLUMN usage_status VARCHAR(32) NULL COMMENT '使用状态 dict_wechat_usage_status';
ALTER TABLE oa_account ADD COLUMN original_account_name VARCHAR(128) NULL COMMENT '原公众号名称';
ALTER TABLE oa_account ADD COLUMN cert_expiry_time TIMESTAMP NULL COMMENT '公众号认证到期时间';
ALTER TABLE oa_account ADD COLUMN cert_count INT NOT NULL DEFAULT 0 COMMENT '公众号认证次数';
ALTER TABLE oa_account ADD COLUMN linked_video_account_id BIGINT NULL COMMENT '关联视频号 oa_account.id';
ALTER TABLE oa_account ADD COLUMN video_account_registered_at TIMESTAMP NULL COMMENT '视频号注册时间';
ALTER TABLE oa_account ADD COLUMN admin_name VARCHAR(64) NULL COMMENT '管理员姓名';
ALTER TABLE oa_account ADD COLUMN admin_user_id BIGINT NULL COMMENT '管理员关联用户 sys_user.id';
ALTER TABLE oa_account ADD COLUMN admin_id_card_encrypted VARCHAR(256) NULL COMMENT '管理员身份证 AES-256';

CREATE TABLE IF NOT EXISTS oa_wechat_official_cert_renewal (
    id                  BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id           BIGINT         NOT NULL,
    account_id          BIGINT         NOT NULL COMMENT '关联 oa_account.id',
    renewal_time        TIMESTAMP      NOT NULL COMMENT '续费时间',
    renewer_user_id     BIGINT         NULL COMMENT '续费人 sys_user.id',
    renewal_amount      DECIMAL(10, 2) NOT NULL DEFAULT 300.00 COMMENT '续费金额',
    creator             VARCHAR(64)    DEFAULT 'system',
    create_time         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater             VARCHAR(64)    DEFAULT 'system',
    update_time         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             SMALLINT       NOT NULL DEFAULT 0,
    KEY idx_wocr_tenant (tenant_id),
    KEY idx_wocr_account (tenant_id, account_id)
);

INSERT INTO sys_dict_type (type, name, status) VALUES
('dict_qualification_type', '资质类型', 'ENABLED'),
('dict_wechat_usage_status', '公众号使用状态', 'ENABLED');

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_account_type', '订阅号', 'SUBSCRIPTION_ACCOUNT', 4, 'ENABLED'),
('dict_qualification_type', '企业', 'ENTERPRISE', 1, 'ENABLED'),
('dict_qualification_type', '个人', 'PERSONAL', 2, 'ENABLED'),
('dict_wechat_usage_status', '注册', 'REGISTERED', 1, 'ENABLED'),
('dict_wechat_usage_status', '认证', 'CERTIFIED', 2, 'ENABLED'),
('dict_wechat_usage_status', '续费', 'RENEWED', 3, 'ENABLED');
