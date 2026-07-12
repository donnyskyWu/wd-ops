-- V130: 作者 Ops 扩展表（ADR-051 Option A）
-- author_user (shenyu-member) = SSOT；oa_author_ext = Ops 运营维度 + author_user_id 映射
-- oa_author 保留供 wd 内 FK（oa_content/oa_task/oa_order_attribution 等），分阶段废弃

CREATE TABLE IF NOT EXISTS oa_author_ext (
    id                  BIGINT       NOT NULL PRIMARY KEY COMMENT '与 oa_author.id 1:1，wd 内 author_id FK 锚点',
    tenant_id           BIGINT       NOT NULL COMMENT '租户ID',
    author_user_id      BIGINT       NULL COMMENT '→ shenyu-member.author_user.id（逻辑 FK；Phase1 历史 seed 可 NULL 待人工映射）',
    ip_group_id         BIGINT       NOT NULL COMMENT '小 IP 组 → oa_ip_group.id',
    author_type         VARCHAR(32)  NULL COMMENT '作者类型 dict_author_type',
    primary_account_id  BIGINT       NULL COMMENT '主推号 → oa_account.id',
    status              TINYINT      NOT NULL DEFAULT 1 COMMENT 'Ops 侧启用状态 0停用 1启用',
    remark              VARCHAR(200) NULL COMMENT 'Ops 备注',
    sync_status         VARCHAR(32)  NOT NULL DEFAULT 'PENDING_MAP' COMMENT 'PENDING_MAP/SYNCED/ERROR',
    sync_error          VARCHAR(512) NULL COMMENT '双写或映射失败原因',
    creator             VARCHAR(64)  DEFAULT 'system',
    create_time         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater             VARCHAR(64)  DEFAULT 'system',
    update_time         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             SMALLINT     NOT NULL DEFAULT 0,
    UNIQUE KEY uk_author_ext_user (author_user_id),
    KEY idx_author_ext_tenant (tenant_id),
    KEY idx_author_ext_ip_group (tenant_id, ip_group_id)
) COMMENT='作者 Ops 扩展（关联 author_user SSOT，非纯映射表）';

-- Backfill only when V130 id-PK schema still present (skip after V131 / post-TRUNCATE)
SET @has_ext_id := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_author_ext' AND COLUMN_NAME = 'id'
);
SET @sql_backfill := IF(@has_ext_id > 0,
    'INSERT INTO oa_author_ext (
        id, tenant_id, author_user_id, ip_group_id, author_type, primary_account_id,
        status, remark, sync_status, creator, updater, create_time, update_time, deleted
    )
    SELECT
        a.id, a.tenant_id, NULL, a.ip_group_id, a.author_type, a.primary_account_id,
        a.status, a.remark, ''PENDING_MAP'', a.creator, a.updater, a.create_time, a.update_time, a.deleted
    FROM oa_author a
    WHERE a.deleted = 0
      AND NOT EXISTS (SELECT 1 FROM oa_author_ext e WHERE e.id = a.id)',
    'SELECT 1');
PREPARE stmt FROM @sql_backfill;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
