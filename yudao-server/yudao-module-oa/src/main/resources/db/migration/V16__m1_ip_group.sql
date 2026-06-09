-- M1 S-01: IP 组树 + 详情骨架

CREATE TABLE IF NOT EXISTS oa_ip_group (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    group_name      VARCHAR(50)  NOT NULL,
    group_type      TINYINT      NOT NULL COMMENT '1=大组 2=小组',
    parent_id       BIGINT       NULL,
    leader_user_id  BIGINT       NULL,
    sort_order      INT          NOT NULL DEFAULT 0,
    status          TINYINT      NOT NULL DEFAULT 1 COMMENT '0=停用 1=启用',
    remark          VARCHAR(200) NULL,
    creator         VARCHAR(64)  DEFAULT 'system',
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         VARCHAR(64)  DEFAULT 'system',
    update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT     NOT NULL DEFAULT 0,
    UNIQUE KEY uk_oa_ip_group_name (tenant_id, parent_id, group_name, deleted),
    KEY idx_oa_ip_group_tenant (tenant_id),
    KEY idx_oa_ip_group_parent (tenant_id, parent_id)
);

CREATE TABLE IF NOT EXISTS oa_ip_group_member (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    ip_group_id     BIGINT       NOT NULL,
    user_id         BIGINT       NOT NULL,
    position        VARCHAR(32)  NULL,
    is_leader       SMALLINT     NOT NULL DEFAULT 0,
    creator         VARCHAR(64)  DEFAULT 'system',
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         VARCHAR(64)  DEFAULT 'system',
    update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT     NOT NULL DEFAULT 0,
    KEY idx_oa_ip_group_member_group (tenant_id, ip_group_id)
);

CREATE TABLE IF NOT EXISTS oa_ip_group_anchor_rel (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    ip_group_id     BIGINT       NOT NULL,
    anchor_user_id  BIGINT       NOT NULL,
    anchor_type     VARCHAR(16)  NULL,
    creator         VARCHAR(64)  DEFAULT 'system',
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         VARCHAR(64)  DEFAULT 'system',
    update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT     NOT NULL DEFAULT 0,
    KEY idx_oa_ip_group_anchor_group (tenant_id, ip_group_id)
);

-- 与 V15 seed-auth 账号 ip_group_id 对齐
INSERT INTO oa_ip_group (id, tenant_id, group_name, group_type, parent_id, leader_user_id, sort_order, status, remark, creator, updater) VALUES
(9000, 1, 'SEED-运营大组', 1, NULL, 1002, 1, 1, 'seed-ops 骨架', 'seed-ops', 'seed-ops'),
(9001, 1, 'SEED-八卦一组', 2, 9000, 1002, 1, 1, 'AUTH-005 数据范围组', 'seed-ops', 'seed-ops'),
(9002, 1, 'SEED-美妆一组', 2, 9000, 1002, 2, 1, 'seed-ops 骨架', 'seed-ops', 'seed-ops');
