-- §23 #3: Align legacy author_id values to member.author_user.id (ADR-051)
-- Run against localhost wd after member SSOT import. Idempotent.
-- Maps orphan author_id on content/task tables to first author in same IP group when possible.

-- Report rows whose author_id is not in member SSOT (dry-run SELECT)
-- SELECT 'oa_production_content' AS tbl, pc.id, pc.author_id, pc.ip_group_id
-- FROM wd.oa_production_content pc
-- WHERE pc.deleted = 0 AND pc.author_id IS NOT NULL
--   AND NOT EXISTS (SELECT 1 FROM `shenyu-member`.author_user au
--                   WHERE au.id = pc.author_id AND au.tenant_id = pc.tenant_id AND au.deleted = 0);

UPDATE wd.oa_production_content pc
INNER JOIN (
    SELECT e.tenant_id, e.ip_group_id, MIN(e.author_user_id) AS author_user_id
    FROM wd.oa_author_ext e
    WHERE e.deleted = 0 AND e.status = 1
    GROUP BY e.tenant_id, e.ip_group_id
) d ON d.tenant_id = pc.tenant_id AND d.ip_group_id = pc.ip_group_id
SET pc.author_id = d.author_user_id,
    pc.updater = 'patch-content-author-id',
    pc.update_time = NOW()
WHERE pc.deleted = 0
  AND pc.author_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `shenyu-member`.author_user au
      WHERE au.id = pc.author_id AND au.tenant_id = pc.tenant_id AND au.deleted = 0
  );

UPDATE wd.oa_task t
INNER JOIN (
    SELECT e.tenant_id, e.ip_group_id, MIN(e.author_user_id) AS author_user_id
    FROM wd.oa_author_ext e
    WHERE e.deleted = 0 AND e.status = 1
    GROUP BY e.tenant_id, e.ip_group_id
) d ON d.tenant_id = t.tenant_id AND d.ip_group_id = t.ip_group_id
SET t.author_id = d.author_user_id,
    t.updater = 'patch-content-author-id',
    t.update_time = NOW()
WHERE t.deleted = 0
  AND t.author_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `shenyu-member`.author_user au
      WHERE au.id = t.author_id AND au.tenant_id = t.tenant_id AND au.deleted = 0
  );

UPDATE wd.oa_content c
INNER JOIN wd.oa_account_ext ae ON ae.mp_account_id = c.account_id AND ae.tenant_id = c.tenant_id AND ae.deleted = 0
INNER JOIN (
    SELECT e.tenant_id, e.ip_group_id, MIN(e.author_user_id) AS author_user_id
    FROM wd.oa_author_ext e
    WHERE e.deleted = 0 AND e.status = 1
    GROUP BY e.tenant_id, e.ip_group_id
) d ON d.tenant_id = c.tenant_id AND d.ip_group_id = ae.ip_group_id
SET c.author_id = d.author_user_id,
    c.updater = 'patch-content-author-id',
    c.update_time = NOW()
WHERE c.deleted = 0
  AND c.author_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `shenyu-member`.author_user au
      WHERE au.id = c.author_id AND au.tenant_id = c.tenant_id AND au.deleted = 0
  );
