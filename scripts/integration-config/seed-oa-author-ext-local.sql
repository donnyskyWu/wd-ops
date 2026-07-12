-- Local integration: seed oa_author_ext from member.author_user (post-S0 TRUNCATE)
-- Maps each author to IP group 9000/9001/9002 by id modulo (dev-only; product mapping TBD).
-- Idempotent.

INSERT INTO wd.oa_author_ext (
    author_user_id, tenant_id, ip_group_id, author_type, status,
    sync_status, creator, updater, deleted
)
SELECT au.id,
       1,
       ELT(1 + (au.id % 3), 9000, 9001, 9002),
       'SHORT_VIDEO',
       1,
       'SYNCED',
       'seed-author-ext-local',
       'seed-author-ext-local',
       0
FROM `shenyu-member`.author_user au
WHERE au.tenant_id = 1
  AND au.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM wd.oa_author_ext e WHERE e.author_user_id = au.id
  );
