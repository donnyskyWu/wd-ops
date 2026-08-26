-- Repair local shenyu-ops flyway_schema_history for football-module-ops validate-on-migrate.
-- Symptom: FlywayValidateException description mismatch (seed_base vs seed base).
-- Run: mysql -h 127.0.0.1 -u root shenyu-ops < scripts/integration-config/repair-flyway-local-validate.sql
SET NAMES utf8mb4;
UPDATE flyway_schema_history
SET description = REPLACE(description, '_', ' ')
WHERE success = 1
  AND description LIKE '%\_%' ESCAPE '\\';
