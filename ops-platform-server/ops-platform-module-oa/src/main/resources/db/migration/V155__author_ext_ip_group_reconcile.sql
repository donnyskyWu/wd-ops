-- ADR-055：作者 IP 组归一 SSOT = oa_ip_group_anchor_rel；清理 ext 中与 anchor_rel 不一致的冗余 ip_group_id
-- 不反向从 ext 回填 anchor_rel（须用户在 IP 组管理中操作）

ALTER TABLE oa_author_ext
    MODIFY COLUMN ip_group_id BIGINT NULL COMMENT '冗余字段，SSOT 见 oa_ip_group_anchor_rel';

UPDATE oa_author_ext e
SET e.ip_group_id = NULL,
    e.updater     = 'flyway-v155',
    e.update_time = CURRENT_TIMESTAMP
WHERE e.deleted = 0
  AND e.ip_group_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM oa_ip_group_anchor_rel r
    WHERE r.deleted = 0
      AND r.tenant_id = e.tenant_id
      AND r.anchor_user_id = e.author_user_id
      AND r.ip_group_id = e.ip_group_id
  );
