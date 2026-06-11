-- V50: 外部采集 account/keyword 种子 + 内部采集关联 oa_account (M4)

-- ================================================================
-- 1. 外部账号 (scope=EXTERNAL, sub_type=account, dict_third_platform)
-- UI ExternalCollectConfig 按 subType=account 过滤；V43 种子为 COMPETITOR_MONITOR 导致列表为空
-- ================================================================
INSERT INTO oa_collect_config
  (tenant_id, scope, config_name, sub_type, platform_type, account_identifier, collect_method, status, remark, creator, updater)
SELECT 1, 'EXTERNAL', '新榜-竞品公众号「十点读书」', 'account', 'NEWRANK', 'sdushu_official', 'API', 'ENABLED', '新榜第三方-竞品公众号监控', 'seed-m8', 'seed-m8'
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM oa_collect_config WHERE tenant_id = 1 AND scope = 'EXTERNAL' AND sub_type = 'account' AND deleted = 0 LIMIT 1
);

INSERT INTO oa_collect_config
  (tenant_id, scope, config_name, sub_type, platform_type, account_identifier, collect_method, status, remark, creator, updater)
SELECT 1, 'EXTERNAL', '新榜-竞品小红书「美妆日记」', 'account', 'NEWRANK', 'beauty_diary_xhs', 'API', 'ENABLED', '新榜第三方-竞品小红书监控', 'seed-m8', 'seed-m8'
FROM DUAL
WHERE (SELECT COUNT(*) FROM oa_collect_config WHERE tenant_id = 1 AND scope = 'EXTERNAL' AND sub_type = 'account' AND deleted = 0) < 2;

INSERT INTO oa_collect_config
  (tenant_id, scope, config_name, sub_type, platform_type, account_identifier, collect_method, status, remark, creator, updater)
SELECT 1, 'EXTERNAL', '飞瓜-竞品抖音「疯狂小杨哥」', 'account', 'FEIGUA', 'yangge_dy', 'API', 'ENABLED', '飞瓜第三方-竞品抖音达人监控', 'seed-m8', 'seed-m8'
FROM DUAL
WHERE (SELECT COUNT(*) FROM oa_collect_config WHERE tenant_id = 1 AND scope = 'EXTERNAL' AND sub_type = 'account' AND deleted = 0) < 3;

INSERT INTO oa_collect_config
  (tenant_id, scope, config_name, sub_type, platform_type, account_identifier, collect_method, status, remark, creator, updater)
SELECT 1, 'EXTERNAL', '飞瓜-竞品快手「辛巴」', 'account', 'FEIGUA', 'xinba_ks', 'API', 'ENABLED', '飞瓜第三方-竞品快手达人监控', 'seed-m8', 'seed-m8'
FROM DUAL
WHERE (SELECT COUNT(*) FROM oa_collect_config WHERE tenant_id = 1 AND scope = 'EXTERNAL' AND sub_type = 'account' AND deleted = 0) < 4;

-- ================================================================
-- 2. 关键词 (oa_config_keyword, dict_platform_type + dict_match_type)
-- ================================================================
INSERT INTO oa_config_keyword (tenant_id, platform, keyword, match_type, status, creator, updater)
SELECT 1, 'DOUYIN', '直播带货', 'FUZZY', 'ENABLED', 'seed-m8', 'seed-m8'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM oa_config_keyword WHERE tenant_id = 1 AND deleted = 0 LIMIT 1);

INSERT INTO oa_config_keyword (tenant_id, platform, keyword, match_type, status, creator, updater)
SELECT 1, 'DOUYIN', '神鱼运营', 'EXACT', 'ENABLED', 'seed-m8', 'seed-m8'
FROM DUAL
WHERE (SELECT COUNT(*) FROM oa_config_keyword WHERE tenant_id = 1 AND deleted = 0) < 2;

INSERT INTO oa_config_keyword (tenant_id, platform, keyword, match_type, status, creator, updater)
SELECT 1, 'XIAOHONGSHU', '种草测评', 'FUZZY', 'ENABLED', 'seed-m8', 'seed-m8'
FROM DUAL
WHERE (SELECT COUNT(*) FROM oa_config_keyword WHERE tenant_id = 1 AND deleted = 0) < 3;

INSERT INTO oa_config_keyword (tenant_id, platform, keyword, match_type, status, creator, updater)
SELECT 1, 'WECHAT_OFFICIAL', '行业周报', 'FUZZY', 'ENABLED', 'seed-m8', 'seed-m8'
FROM DUAL
WHERE (SELECT COUNT(*) FROM oa_config_keyword WHERE tenant_id = 1 AND deleted = 0) < 4;

INSERT INTO oa_config_keyword (tenant_id, platform, keyword, match_type, status, creator, updater)
SELECT 1, 'KUAISHOU', '老铁经济', 'FUZZY', 'DISABLED', 'seed-m8', 'seed-m8'
FROM DUAL
WHERE (SELECT COUNT(*) FROM oa_config_keyword WHERE tenant_id = 1 AND deleted = 0) < 5;

-- ================================================================
-- 3. 内部采集：关联 M4 平台账号 oa_account (9001-9010)
-- 按 platformType Tab 过滤；服务号 Tab 用 platform_type=SERVICE_ACCOUNT
-- ================================================================
INSERT INTO oa_collect_config
  (tenant_id, scope, config_name, sub_type, platform_type, account_id, account_identifier, app_id, collect_method, status, remark, creator, updater)
SELECT 1, 'INTERNAL', a.account_name, 'platform', a.platform_type, a.id, a.external_account_id,
       CONCAT('seed_app_', a.external_account_id), 'INTERNAL', 'ENABLED',
       CONCAT('内部采集-关联M4账号#', a.id), 'seed-m8', 'seed-m8'
FROM oa_account a
WHERE a.tenant_id = 1 AND a.deleted = 0
  AND a.id IN (9001, 9002, 9004, 9005, 9006, 9007, 9008, 9010)
  AND NOT EXISTS (
    SELECT 1 FROM oa_collect_config c
    WHERE c.tenant_id = 1 AND c.scope = 'INTERNAL' AND c.account_id = a.id AND c.deleted = 0
  );

-- 服务号 Tab：账号 9003 的 platform_type 为 WECHAT_OFFICIAL，采集配置 Tab 键为 SERVICE_ACCOUNT
INSERT INTO oa_collect_config
  (tenant_id, scope, config_name, sub_type, platform_type, account_id, account_identifier, app_id, collect_method, status, remark, creator, updater)
SELECT 1, 'INTERNAL', a.account_name, 'platform', 'SERVICE_ACCOUNT', a.id, a.external_account_id,
       CONCAT('seed_app_', a.external_account_id), 'INTERNAL', 'ENABLED',
       CONCAT('内部采集-服务号-关联M4账号#', a.id), 'seed-m8', 'seed-m8'
FROM oa_account a
WHERE a.tenant_id = 1 AND a.deleted = 0 AND a.id = 9003
  AND NOT EXISTS (
    SELECT 1 FROM oa_collect_config c
    WHERE c.tenant_id = 1 AND c.scope = 'INTERNAL' AND c.account_id = 9003 AND c.deleted = 0
  );

-- 企微 / 个微 Tab 保留独立配置（非 oa_account）
INSERT INTO oa_collect_config
  (tenant_id, scope, config_name, sub_type, platform_type, account_identifier, app_id, collect_method, status, remark, creator, updater)
SELECT 1, 'INTERNAL', 'SEED-企微A', 'wework', 'WEWORK', 'seed_corp_a', 'seed_agent_a', 'INTERNAL', 'ENABLED', '企微通讯录 API 采集', 'seed-m8', 'seed-m8'
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM oa_collect_config WHERE tenant_id = 1 AND scope = 'INTERNAL' AND platform_type = 'WEWORK' AND deleted = 0 LIMIT 1
);

INSERT INTO oa_collect_config
  (tenant_id, scope, config_name, sub_type, platform_type, account_identifier, app_id, collect_method, status, remark, creator, updater)
SELECT 1, 'INTERNAL', 'SEED-个微张三', 'wechat', 'PERSONAL_WECHAT', 'seed_wx_zhangsan', 'ao_seed_app', 'INTERNAL', 'ENABLED', '个微-奥创 API 采集', 'seed-m8', 'seed-m8'
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM oa_collect_config WHERE tenant_id = 1 AND scope = 'INTERNAL' AND platform_type = 'PERSONAL_WECHAT' AND deleted = 0 LIMIT 1
);
