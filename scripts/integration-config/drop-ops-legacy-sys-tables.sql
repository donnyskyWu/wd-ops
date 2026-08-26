-- =============================================================================
-- shenyu-ops — 手工清理 legacy sys_* 表（已有库增量；Greenfield 空库勿单独跑）
-- 版本: 2026-08-25
-- SSOT: docs/deploy/ops-greenfield-production/OPERATIONS-GUIDE.md § shenyu-ops 表清单
--
-- 用法:
--   mysql -h HOST -u USER -p shenyu-ops < scripts/integration-config/drop-ops-legacy-sys-tables.sql
--
-- 终态目标（Ops 库仅保留）: sys_param · sys_message · flyway_schema_history · oa_*
-- Greenfield 空库: V190 已 DROP §1 三张表；§2 须等代码 refactor 后再执行。
-- =============================================================================
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------------
-- §1 可安全 DROP（与 Flyway V190 一致；Feign SSOT 或 dead code）
-- ---------------------------------------------------------------------------
-- sys_dict_type / sys_dict_data  → DictService → shenyu-system.system_dict_*
-- sys_operation_log              → 无 Java 读写（操作日志 SSOT = Football system_operate_log）

DROP TABLE IF EXISTS sys_dict_data;
DROP TABLE IF EXISTS sys_dict_type;
DROP TABLE IF EXISTS sys_operation_log;

-- ---------------------------------------------------------------------------
-- §2 代码阻塞 — 执行前须完成 refactor（勿 uncomment 除非已改代码）
-- ---------------------------------------------------------------------------
-- | 表 | 阻塞类 | 说明 |
-- |----|--------|------|
-- | sys_metadata_entity / sys_metadata_field | MetadataServiceImpl | M6 元数据 CRUD |
-- | sys_notification_event | NotificationServiceImpl | 阈值/通知去重 (tenant_id, event_type, biz_key) |
-- | sys_tenant | OpsTenantFrameworkService | **V191 Feign TenantCommonApi — 可 DROP** |
-- | sys_user / sys_user_token / sys_user_role | FootballSystemUserValidator (test @Profile) | **V191 — 可 DROP** |
-- | sys_role / sys_role_permission / sys_permission | H2 IT harness | **V191 — 可 DROP** |

DROP TABLE IF EXISTS sys_role_permission;
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_user_token;
DROP TABLE IF EXISTS sys_permission;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS sys_tenant;

-- ---------------------------------------------------------------------------
-- §3 验收（可选）
-- ---------------------------------------------------------------------------
SELECT table_name
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name LIKE 'sys\_%' ESCAPE '\\'
ORDER BY table_name;
