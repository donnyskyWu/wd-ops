-- V164: Repair OPS system_menu charset in shenyu-system
-- Beta/test remote: shenyu-ops Flyway user has no cross-DB UPDATE on shenyu-system.
-- Re-seed: python scripts/integration-config/apply-seed-oa-menu.py --host ... --database shenyu-system
-- Or: .\scripts\integration-config\seed-ops-test-remote.ps1
--
-- Master (shenyu-ops) overlay system_menu is deprecated; Football reads shenyu-system.system_menu.
-- Intentional no-op here so oa-server can start on beta; charset repair runs via seed scripts.

SELECT 1;
