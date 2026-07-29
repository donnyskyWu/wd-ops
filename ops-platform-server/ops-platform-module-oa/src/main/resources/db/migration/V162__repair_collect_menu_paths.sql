-- V162: Fix duplicate collect prefix in OPS menu paths.
-- Parent 6104 path=collect + child collect/task produced /ops/collect/collect/task in Football shell.

UPDATE system_menu
SET path = 'log',
    updater = 'flyway',
    update_time = NOW()
WHERE id = 6133
  AND parent_id = 6104
  AND path = 'collect/log'
  AND deleted = b'0';

UPDATE system_menu
SET path = 'private-domain-bridge',
    updater = 'flyway',
    update_time = NOW()
WHERE id = 6134
  AND parent_id = 6104
  AND path = 'collect/private-domain-bridge'
  AND deleted = b'0';

UPDATE system_menu
SET path = 'quality',
    updater = 'flyway',
    update_time = NOW()
WHERE id = 6135
  AND parent_id = 6104
  AND path = 'collect/quality'
  AND deleted = b'0';

UPDATE system_menu
SET path = 'task',
    updater = 'flyway',
    update_time = NOW()
WHERE id = 6136
  AND parent_id = 6104
  AND path = 'collect/task'
  AND deleted = b'0';

-- shenyu-system menu path repair: beta has no cross-DB UPDATE grant for shenyu-ops.
-- Apply via seed-ops-test-remote-shenyu-system-menus.sql / seed-ops-test-remote.ps1 instead.
