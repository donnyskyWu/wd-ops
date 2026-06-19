-- ADR-046: idempotent backfill — grant oa:metadata:* to OA_ADMIN (role_id=1)
-- Safe when V96 already ran but sys_role_permission rows were missing on existing deployments.

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, p.id
FROM sys_permission p
WHERE p.code IN (
    'oa:metadata:query',
    'oa:metadata:create',
    'oa:metadata:update',
    'oa:metadata:delete'
)
  AND p.deleted = 0
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);
