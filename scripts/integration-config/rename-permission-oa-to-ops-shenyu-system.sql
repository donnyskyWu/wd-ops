-- P-D / ADR-058 P6: shenyu-system menu permission codes oa:* → ops:*
-- Idempotent. Local already applied 2026-07-31; keep for Beta / re-seed environments.
-- Ops master (shenyu-ops) is handled by Flyway V166__rename_permission_oa_to_ops.sql.

SET NAMES utf8mb4;

UPDATE system_menu
SET permission = CONCAT('ops:', SUBSTRING(permission, 4)),
    updater = 'p-d-perm',
    update_time = NOW()
WHERE permission IS NOT NULL
  AND permission LIKE 'oa:%'
  AND deleted = b'0';
