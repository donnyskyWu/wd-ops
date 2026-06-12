-- M0 首页：告警表 + tenant2 IP 组（跨租户 1504）+ 快捷入口权限

CREATE TABLE IF NOT EXISTS oa_home_alert (
    id              BIGINT        NOT NULL PRIMARY KEY,
    tenant_id       BIGINT        NOT NULL,
    alert_level     VARCHAR(32)   NOT NULL,
    alert_content   VARCHAR(512)  NOT NULL,
    alert_source    VARCHAR(64)   NULL,
    trigger_time    DATETIME      NOT NULL,
    status          VARCHAR(32)   NOT NULL DEFAULT 'PENDING',
    creator         VARCHAR(64)   NULL,
    create_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         VARCHAR(64)   NULL,
    update_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT       NOT NULL DEFAULT 0,
    KEY idx_oa_home_alert_tenant (tenant_id, status)
);

INSERT INTO oa_home_alert (id, tenant_id, alert_level, alert_content, alert_source, trigger_time, status, creator, updater) VALUES
(9601, 1, 'WARNING', 'SEED-账号9001粉丝增长异常下降', 'ACCOUNT_STATUS', '2026-06-08 09:00:00', 'PENDING', 'seed-m0', 'seed-m0'),
(9602, 1, 'CRITICAL', 'SEED-外部作品低完播率告警', 'EXTERNAL_WORK', '2026-06-08 10:30:00', 'PENDING', 'seed-m0', 'seed-m0'),
(9603, 1, 'INFO', 'SEED-阈值配置触发：抖音投放超预算', 'THRESHOLD', '2026-06-08 11:15:00', 'PENDING', 'seed-m0', 'seed-m0');

-- tenant=2 IP 组（跨租户测试 ipGroupId=8001）
INSERT INTO oa_ip_group (id, tenant_id, group_name, group_type, parent_id, leader_user_id, sort_order, status, remark, creator, updater) VALUES
(8001, 2, 'SEED-T2-隔离组', 2, NULL, 2001, 1, 1, 'seed-m0 cross-tenant', 'seed-m0', 'seed-m0');

-- 首页快捷入口权限（授予 OA_ADMIN）
INSERT INTO sys_permission (id, code, name, module, creator, updater) VALUES
(16, 'oa:home:view', '首页查看', 'M0', 'seed-m0', 'seed-m0'),
(17, 'oa:ip-group:list', 'IP组查询', 'M1', 'seed-m0', 'seed-m0'),
(18, 'oa:author:list', '作者查询', 'M1', 'seed-m0', 'seed-m0'),
(19, 'oa:sop:list', 'SOP查询', 'M2', 'seed-m0', 'seed-m0'),
(20, 'oa:perf:list', '绩效查询', 'M3', 'seed-m0', 'seed-m0'),
(21, 'oa:report:list', '报表查询', 'M6', 'seed-m0', 'seed-m0')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO sys_role_permission (role_id, permission_id) VALUES
(1, 16), (1, 17), (1, 18), (1, 19), (1, 20), (1, 21);
