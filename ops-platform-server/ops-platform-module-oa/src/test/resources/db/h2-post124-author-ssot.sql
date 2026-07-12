-- H2 test baseline (post-V124): member author_user SSOT + oa_author_ext (ADR-051)
-- Maps legacy oa_author seed rows to author_user_id = oa_author.id for IT compatibility.

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
