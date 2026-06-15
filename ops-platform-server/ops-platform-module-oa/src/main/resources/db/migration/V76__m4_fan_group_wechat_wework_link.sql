-- M4 Req-127: 抖音/快手平台账号粉丝群
-- M4 Req-128: 个微 ↔ 企微员工双向关联（SSOT: oa_personal_wechat_account.linked_wework_employee_id）

CREATE TABLE IF NOT EXISTS oa_platform_account_fan_group (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    account_id      BIGINT       NOT NULL COMMENT '关联 oa_account.id',
    group_name      VARCHAR(100) NOT NULL COMMENT '粉丝群名称',
    member_count    INT          NOT NULL DEFAULT 0 COMMENT '粉丝群人数',
    creator         VARCHAR(64)  DEFAULT 'system',
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         VARCHAR(64)  DEFAULT 'system',
    update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT     NOT NULL DEFAULT 0,
    UNIQUE KEY uk_fan_group_account_name (tenant_id, account_id, group_name),
    KEY idx_fan_group_tenant (tenant_id),
    KEY idx_fan_group_account (tenant_id, account_id)
);

ALTER TABLE oa_personal_wechat_account
    ADD COLUMN linked_wework_employee_id BIGINT NULL COMMENT '关联 oa_wework_employee.id（个微↔企微 SSOT）';

CREATE UNIQUE INDEX uk_pwa_linked_wework ON oa_personal_wechat_account (tenant_id, linked_wework_employee_id);

-- seed: 抖音/快手账号粉丝群样本
INSERT INTO oa_platform_account_fan_group (id, tenant_id, account_id, group_name, member_count, creator, updater)
VALUES
    (9601, 1, 9006, 'SEED-抖音粉丝群1', 1280, 'seed-m4', 'seed-m4'),
    (9602, 1, 9006, 'SEED-抖音粉丝群2', 856, 'seed-m4', 'seed-m4'),
    (9603, 1, 9008, 'SEED-快手粉丝群1', 520, 'seed-m4', 'seed-m4');

-- seed: 个微张三 ↔ 企微员工李四
UPDATE oa_personal_wechat_account
SET linked_wework_employee_id = 9001
WHERE id = 9001 AND tenant_id = 1;
