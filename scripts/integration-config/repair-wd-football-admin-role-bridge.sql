-- C-WP1 / ADR-003: Gateway LoginUser.id = Football snowflake (shenyu-system).
-- OA PreAuthorize loads authorities from wd master via FootballOAuth2MasterTokenMapper.
-- When Feign AdminUserApi is down / username missing, authorityUserId falls back to snowflake.
-- Sync shenyu-system admin role bindings onto wd so oa:* permissions resolve.
-- Idempotent. Local integration (NO -Beta).

SET @fid := 1749825673829120001;

INSERT INTO wd.system_user_role (id, user_id, role_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT
  900000000000000000 + ur.id,
  ur.user_id,
  ur.role_id,
  'c-wp1-bridge',
  NOW(),
  'c-wp1-bridge',
  NOW(),
  b'0',
  ur.tenant_id
FROM `shenyu-system`.system_user_role ur
WHERE ur.user_id = @fid AND ur.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM wd.system_user_role w
    WHERE w.user_id = ur.user_id AND w.role_id = ur.role_id AND w.deleted = 0
  );

-- Verify
SELECT user_id, role_id FROM wd.system_user_role WHERE user_id = @fid AND deleted = 0;
SELECT GROUP_CONCAT(DISTINCT m.permission) AS account_perms
FROM wd.system_menu m
INNER JOIN wd.system_role_menu rm ON rm.menu_id = m.id AND rm.deleted = 0
INNER JOIN wd.system_user_role ur ON ur.role_id = rm.role_id AND ur.deleted = 0
WHERE ur.user_id = @fid
  AND m.deleted = 0
  AND m.permission IN ('ops:account:list', 'ops:platform-account:list');
