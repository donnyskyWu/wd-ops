-- =============================================================================
-- Optional: record V205 / V206 in flyway_schema_history (shenyu-ops)
--
-- 何时需要：
--   生产 Nacos 模板 spring.flyway.enabled=false（见 ops-greenfield-production/config/nacos-ops-server-prod.yaml），
--   且已手工执行 01-shenyu-ops-schema-V205-V206.sql，希望后续发版 Flyway 不再重复跑同版本。
--
-- 何时跳过：
--   生产 FLYWAY_ENABLED=true 且由应用启动自动迁移；或 DBA 另有 checksum 对齐流程。
--
-- Execution (after 01 succeeds):
--   mysql -h<host> -u<user> -p --default-character-set=utf8mb4 shenyu-ops < 02-shenyu-ops-flyway-history-insert.sql
-- =============================================================================

SET NAMES utf8mb4;

INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
SELECT (SELECT COALESCE(MAX(installed_rank), 0) + 1 FROM flyway_schema_history fsh),
       '205', 'm4 holder m2 publish account', 'SQL', 'V205__m4_holder_m2_publish_account.sql', NULL, 'manual-prod-20260928', NOW(), 0, 1
FROM DUAL
WHERE EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'flyway_schema_history')
  AND NOT EXISTS (SELECT 1 FROM flyway_schema_history WHERE version = '205' AND success = 1);

INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
SELECT (SELECT COALESCE(MAX(installed_rank), 0) + 1 FROM flyway_schema_history fsh),
       '206', 'oa account short video live operator', 'SQL', 'V206__oa_account_short_video_live_operator.sql', NULL, 'manual-prod-20260928', NOW(), 0, 1
FROM DUAL
WHERE EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'flyway_schema_history')
  AND NOT EXISTS (SELECT 1 FROM flyway_schema_history WHERE version = '206' AND success = 1);

SELECT version, description, installed_by, installed_on, success
FROM flyway_schema_history
WHERE version IN ('205', '206')
ORDER BY installed_rank;
