-- ADR-054 P1: OPS 内容生产 × Football author_article 扩展表 + 双正文字段

ALTER TABLE oa_production_content
    ADD COLUMN paid_body TEXT NULL COMMENT '付费内容（sync → author_article.content）' AFTER body,
    ADD COLUMN free_body TEXT NULL COMMENT '免费内容（sync → author_article.free_content）' AFTER paid_body;

UPDATE oa_production_content
SET paid_body = COALESCE(NULLIF(TRIM(layout_html), ''), body)
WHERE paid_body IS NULL;

CREATE TABLE IF NOT EXISTS oa_production_content_ext (
    id                      BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id               BIGINT       NOT NULL COMMENT '租户ID',
    production_content_id   BIGINT       NOT NULL COMMENT '→ oa_production_content.id',
    author_article_id       BIGINT       NULL COMMENT '→ shenyu-member.author_article.id',
    ip_group_id             BIGINT       NULL COMMENT '冗余查询',
    task_id                 BIGINT       NULL COMMENT '关联任务',
    scheme_types            VARCHAR(256) NULL COMMENT 'OPS dict_scheme_type 逗号分隔',
    competition_id          VARCHAR(64)  NULL COMMENT '外部 scheduleId',
    competition_name        VARCHAR(128) NULL COMMENT '赛事展示名快照',
    sync_football_at        TIMESTAMP    NULL COMMENT '最后一次 sync 成功时间',
    football_sync_error     VARCHAR(512) NULL COMMENT 'Football 同步失败原因',
    source                  VARCHAR(32)  NOT NULL DEFAULT 'OPS' COMMENT 'OPS / AMPHIPODA_LEGACY',
    creator                 VARCHAR(64)  DEFAULT 'system',
    create_time             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater                 VARCHAR(64)  DEFAULT 'system',
    update_time             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted                 SMALLINT     NOT NULL DEFAULT 0,
    UNIQUE KEY uk_prod_content_ext_content (production_content_id),
    UNIQUE KEY uk_prod_content_ext_article (author_article_id),
    KEY idx_prod_content_ext_tenant (tenant_id)
) COMMENT='OPS 内容生产 Football 方案桥接扩展（ADR-054）';

INSERT INTO oa_production_content_ext (
    tenant_id, production_content_id, author_article_id, ip_group_id, task_id,
    scheme_types, competition_id, competition_name, source,
    creator, updater, create_time, update_time, deleted
)
SELECT
    pc.tenant_id, pc.id, NULL, pc.ip_group_id, pc.task_id,
    pc.scheme_type, pc.competition_id, pc.competition_name, 'OPS',
    pc.creator, pc.updater, pc.create_time, pc.update_time, pc.deleted
FROM oa_production_content pc
WHERE pc.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM oa_production_content_ext ext
      WHERE ext.production_content_id = pc.id AND ext.deleted = 0
  );
