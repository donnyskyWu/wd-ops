-- M4 S-08b: 企微员工用户账号（关联 oa_wework_account 应用配置）
CREATE TABLE IF NOT EXISTS oa_wework_employee (
    id                  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id           BIGINT       NOT NULL,
    wework_account_id   BIGINT       NOT NULL COMMENT '关联 oa_wework_account.id',
    nickname            VARCHAR(100) NOT NULL COMMENT '昵称',
    wework_user_id      VARCHAR(64)  NOT NULL COMMENT '企微用户 ID',
    phone               VARCHAR(20)  NULL COMMENT '手机号',
    department          VARCHAR(100) NULL COMMENT '部门',
    position            VARCHAR(100) NULL COMMENT '岗位',
    status              VARCHAR(32)  NOT NULL DEFAULT 'ENABLED',
    creator             VARCHAR(64)  DEFAULT 'system',
    create_time         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater             VARCHAR(64)  DEFAULT 'system',
    update_time         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             SMALLINT     NOT NULL DEFAULT 0,
    UNIQUE KEY uk_oa_wework_emp_user (tenant_id, wework_account_id, wework_user_id),
    KEY idx_oa_wework_emp_tenant (tenant_id),
    KEY idx_oa_wework_emp_account (tenant_id, wework_account_id)
);

-- seed: 关联 SEED-企微A (id=9001)
INSERT INTO oa_wework_employee (id, tenant_id, wework_account_id, nickname, wework_user_id, phone, department, position, status, creator, updater)
VALUES (9001, 1, 9001, 'SEED-员工李四', 'seed_wework_user_001', '13900139001', '运营部', '运营专员', 'ENABLED', 'seed-assets', 'seed-assets');
