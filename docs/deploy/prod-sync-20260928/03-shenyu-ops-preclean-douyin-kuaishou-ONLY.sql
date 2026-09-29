USE `shenyu-ops`;
-- =============================================================================
-- PRODUCTION · 可选预清理 · 仅 tenant=1 抖音/快手账号（含子表）
-- ⚠️ 不删除 oa_company / oa_realname / oa_phone / oa_sim_card 及 expansion/intermediary。
-- ⚠️ 生产「整批重导抖快」时：先本脚本，再 04-shenyu-ops-data-import-0928-OPTIONAL.sql。
-- ⚠️ 保留其它平台账号（公众号/视频号/小红书/企微等）及全部支撑表存量。
-- SSOT 生成器：scripts/import/prod-release/_generate_prod_sync_data_sql.py
-- 测试全量替换（含支撑表清理）仍用 scripts/import/import-accounts-shenyu-ops-20260928.sql PRE-IMPORT 全段。
-- =============================================================================

SET NAMES utf8mb4;
START TRANSACTION;

-- --- 1. 抖音/快手账号子表（按 account_id 限定） ---
-- 平台账号粉丝群
DELETE FROM oa_platform_account_fan_group WHERE tenant_id = 1 AND account_id IN (SELECT id FROM oa_account WHERE tenant_id = 1 AND platform_type IN ('DOUYIN', 'KUAISHOU'));
-- 账号成本
DELETE FROM oa_account_cost WHERE tenant_id = 1 AND account_id IN (SELECT id FROM oa_account WHERE tenant_id = 1 AND platform_type IN ('DOUYIN', 'KUAISHOU'));
-- 账号状态日志
DELETE FROM oa_account_status_log WHERE tenant_id = 1 AND account_id IN (SELECT id FROM oa_account WHERE tenant_id = 1 AND platform_type IN ('DOUYIN', 'KUAISHOU'));
-- 采集账号绑定
DELETE FROM oa_collector_account_bind WHERE tenant_id = 1 AND oa_account_id IN (SELECT id FROM oa_account WHERE tenant_id = 1 AND platform_type IN ('DOUYIN', 'KUAISHOU'));

-- --- 2. 平台账号：仅抖音/快手（tenant 全量，含历史手工录入） ---
-- 导入会整体替换抖音/快手，故删除 tenant_id=1 下全部 DOUYIN/KUAISHOU，不限 id 段。
DELETE FROM oa_account WHERE tenant_id = 1 AND platform_type IN ('DOUYIN', 'KUAISHOU');

COMMIT;
