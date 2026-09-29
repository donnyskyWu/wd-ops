USE `shenyu-ops`;
-- =============================================================================
-- PRODUCTION · 仅清理 xlsx-import-20260928 批次（ROLLBACK 段）
-- 不删除 tenant 下全部 DOUYIN/KUAISHOU；适合「重导同一 Excel 批次」前手工清理。
-- 测试环境「全量替换」请用 scripts/import/import-accounts-shenyu-ops-20260928.sql 的 PRE-IMPORT 段。
-- =============================================================================

SET NAMES utf8mb4;
START TRANSACTION;

-- ========== ROLLBACK (re-import only) ==========
-- 按导入 ID 段清理抖音/快手及 Excel 批次支撑数据；dev 重导时可单独使用（注释掉上方 PRE-IMPORT 段）
-- 不影响公众号/视频号/小红书等其它平台账号。

-- --- 1. 导入批次抖音/快手账号子表 ---
-- 平台账号粉丝群
DELETE FROM oa_platform_account_fan_group WHERE tenant_id = 1 AND account_id IN (SELECT id FROM oa_account WHERE tenant_id = 1 AND platform_type IN ('DOUYIN', 'KUAISHOU') AND (id >= 140001 OR creator = 'xlsx-import-20260928'));
-- 账号成本
DELETE FROM oa_account_cost WHERE tenant_id = 1 AND account_id IN (SELECT id FROM oa_account WHERE tenant_id = 1 AND platform_type IN ('DOUYIN', 'KUAISHOU') AND (id >= 140001 OR creator = 'xlsx-import-20260928'));
-- 账号状态日志
DELETE FROM oa_account_status_log WHERE tenant_id = 1 AND account_id IN (SELECT id FROM oa_account WHERE tenant_id = 1 AND platform_type IN ('DOUYIN', 'KUAISHOU') AND (id >= 140001 OR creator = 'xlsx-import-20260928'));
-- 采集账号绑定
DELETE FROM oa_collector_account_bind WHERE tenant_id = 1 AND oa_account_id IN (SELECT id FROM oa_account WHERE tenant_id = 1 AND platform_type IN ('DOUYIN', 'KUAISHOU') AND (id >= 140001 OR creator = 'xlsx-import-20260928'));

-- --- 2. 导入批次抖音/快手账号（id >= 140001 或 creator 标记） ---
DELETE FROM oa_account WHERE tenant_id = 1 AND platform_type IN ('DOUYIN', 'KUAISHOU') AND (id >= 140001 OR creator = 'xlsx-import-20260928');

-- --- 3. Excel 导入批次支撑表 ---
-- 实名人-中介关联（导入批次实名人）
DELETE FROM oa_realname_intermediary WHERE tenant_id = 1 AND realname_id IN (SELECT id FROM oa_realname WHERE tenant_id = 1 AND (id >= 110001 OR creator = 'xlsx-import-20260928'));
-- 公司扩容记录（导入批次公司）
DELETE FROM oa_company_expansion WHERE tenant_id = 1 AND company_id IN (SELECT id FROM oa_company WHERE tenant_id = 1 AND (id >= 100001 OR creator = 'xlsx-import-20260928'));
-- 手机卡（导入批次；无剩余账号引用）
DELETE FROM oa_sim_card WHERE tenant_id = 1 AND (id >= 130001 OR creator = 'xlsx-import-20260928') AND id NOT IN (SELECT sim_card_id FROM oa_account WHERE tenant_id = 1 AND sim_card_id IS NOT NULL);
-- 手机设备（导入批次；无剩余账号/SIM 引用）
DELETE FROM oa_phone WHERE tenant_id = 1 AND (id >= 120001 OR creator = 'xlsx-import-20260928') AND id NOT IN (SELECT phone_id FROM oa_account WHERE tenant_id = 1 AND phone_id IS NOT NULL) AND id NOT IN (SELECT phone_id FROM oa_sim_card WHERE tenant_id = 1 AND phone_id IS NOT NULL);
-- 实名人（导入批次；无剩余账号/设备引用）
DELETE FROM oa_realname WHERE tenant_id = 1 AND (id >= 110001 OR creator = 'xlsx-import-20260928') AND id NOT IN (SELECT realname_id FROM oa_account WHERE tenant_id = 1 AND realname_id IS NOT NULL) AND id NOT IN (SELECT realname_id FROM oa_phone WHERE tenant_id = 1 AND realname_id IS NOT NULL);
-- 公司（导入批次；无剩余账号引用）
DELETE FROM oa_company WHERE tenant_id = 1 AND (id >= 100001 OR creator = 'xlsx-import-20260928') AND id NOT IN (SELECT company_id FROM oa_account WHERE tenant_id = 1 AND company_id IS NOT NULL);

COMMIT;
