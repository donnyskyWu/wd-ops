-- H2 test baseline (post-V124): member author_user SSOT + oa_author_ext (ADR-051)
-- Maps legacy oa_author seed rows to author_user_id = oa_author.id for IT compatibility.
-- Also seeds sys_role ip_group_leader (V150 skipped on H2 — no system_role overlay).

INSERT INTO sys_role (id, tenant_id, code, name, status, data_scope, remark, creator, updater)
SELECT 6, 1, 'ip_group_leader', 'IP组长', 'ENABLED', 'SELF',
       'h2-seed · IP组长', 'h2-seed', 'h2-seed'
FROM sys_user WHERE id = 1001
  AND NOT EXISTS (
    SELECT 1 FROM sys_role r WHERE r.code = 'ip_group_leader' AND r.tenant_id = 1 AND r.deleted = 0
);

INSERT INTO sys_user_role (user_id, role_id)
SELECT 1002, r.id FROM sys_role r
WHERE r.code = 'ip_group_leader' AND r.tenant_id = 1 AND r.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_user_role ur WHERE ur.user_id = 1002 AND ur.role_id = r.id);

INSERT INTO sys_user_role (user_id, role_id)
SELECT 1003, r.id FROM sys_role r
WHERE r.code = 'ip_group_leader' AND r.tenant_id = 1 AND r.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_user_role ur WHERE ur.user_id = 1003 AND ur.role_id = r.id);

CREATE TABLE IF NOT EXISTS author_user (
    id           BIGINT       NOT NULL PRIMARY KEY,
    user_id      BIGINT       NULL,
    nickname     VARCHAR(255) NULL,
    avatar_url   VARCHAR(255) NULL,
    status       INT          NULL,
    tenant_id    BIGINT       NOT NULL,
    creator      VARCHAR(64)  NULL,
    create_time  TIMESTAMP    NULL,
    updater      VARCHAR(64)  NULL,
    update_time  TIMESTAMP    NULL,
    deleted      SMALLINT     NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS oa_author_ext (
    author_user_id        BIGINT       NOT NULL PRIMARY KEY,
    tenant_id             BIGINT       NOT NULL,
    ip_group_id           BIGINT       NOT NULL,
    author_type           VARCHAR(32)  NULL,
    primary_mp_account_id BIGINT       NULL,
    status                TINYINT      NOT NULL DEFAULT 1,
    remark                VARCHAR(200) NULL,
    sync_status           VARCHAR(32)  NOT NULL DEFAULT 'SYNCED',
    sync_error            VARCHAR(512) NULL,
    creator               VARCHAR(64)  NULL,
    create_time           TIMESTAMP    NULL,
    updater               VARCHAR(64)  NULL,
    update_time           TIMESTAMP    NULL,
    deleted               SMALLINT     NOT NULL DEFAULT 0
);

INSERT INTO author_user (id, user_id, nickname, status, tenant_id, creator, updater, create_time, update_time, deleted)
SELECT a.id, a.user_id, a.author_name,
       CASE WHEN a.status = 1 THEN 0 ELSE 1 END,
       a.tenant_id, a.creator, a.updater, a.create_time, a.update_time, a.deleted
FROM oa_author a
WHERE a.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM author_user u WHERE u.id = a.id);

INSERT INTO oa_author_ext (
    author_user_id, tenant_id, ip_group_id, author_type, primary_mp_account_id,
    status, remark, sync_status, creator, updater, create_time, update_time, deleted
)
SELECT a.id, a.tenant_id, a.ip_group_id, a.author_type, a.primary_account_id,
       a.status, a.remark, 'SYNCED', a.creator, a.updater, a.create_time, a.update_time, a.deleted
FROM oa_author a
WHERE a.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM oa_author_ext e WHERE e.author_user_id = a.id);

-- IP 组「关联作者」SSOT = oa_ip_group_anchor_rel.anchor_user_id（author_user.id）
DELETE FROM oa_ip_group_anchor_rel WHERE tenant_id = 1;

INSERT INTO oa_ip_group_anchor_rel (tenant_id, ip_group_id, anchor_user_id, anchor_type, creator, updater, deleted)
SELECT a.tenant_id, a.ip_group_id, a.id, COALESCE(a.author_type, 'VIDEO'), 'h2-seed', 'h2-seed', 0
FROM oa_author a
WHERE a.deleted = 0
  AND a.ip_group_id IS NOT NULL;
