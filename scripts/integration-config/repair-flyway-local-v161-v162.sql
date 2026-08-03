-- Local wd DB: align flyway_schema_history checksums when V161/V162 SQL files changed after apply.
-- Symptom: oa-server fails with "Migration checksum mismatch for migration version 161/162".
-- Run: mysql -uroot -proot wd < scripts/integration-config/repair-flyway-local-v161-v162.sql
-- Prefer matching "Resolved locally" values from oa-server startup log (Flyway 10.x).

UPDATE flyway_schema_history SET checksum = 728249119 WHERE version = '161';
UPDATE flyway_schema_history SET checksum = -1124762603 WHERE version = '162';

SELECT version, description, checksum, success
FROM flyway_schema_history
WHERE version IN ('161', '162')
ORDER BY installed_rank;
