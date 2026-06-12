-- S-R21-Mike / ADR-008 方案 A：oa_content 加 author_id（内容生产者 sys_user.id）

ALTER TABLE oa_content ADD COLUMN author_id BIGINT NULL COMMENT '内容生产者 sys_user.id';

CREATE INDEX idx_oa_content_author ON oa_content (tenant_id, author_id);

-- 历史数据回填：账号主推号作者 → user_id
UPDATE oa_content c
SET author_id = (
    SELECT a.user_id
    FROM oa_author a
    WHERE a.primary_account_id = c.account_id
      AND a.tenant_id = c.tenant_id
      AND a.deleted = 0
    LIMIT 1
)
WHERE c.author_id IS NULL
  AND EXISTS (
    SELECT 1
    FROM oa_author a
    WHERE a.primary_account_id = c.account_id
      AND a.tenant_id = c.tenant_id
      AND a.deleted = 0
);
