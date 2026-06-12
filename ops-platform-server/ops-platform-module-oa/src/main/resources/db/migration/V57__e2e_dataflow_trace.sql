-- E2E 数据流全链路追溯种子 | TRACE=E2E-DF-20260611
-- 账号 91001 / IP组 92001 / 作者 93001 / 作品 94001-94003

-- ========== 1. 平台账号（视频号） ==========
INSERT INTO oa_account (id, tenant_id, platform_type, account_type, account_name, external_account_id,
                        company_id, realname_id, phone_id, phone_number_hash, status, ip_group_id, creator, updater)
SELECT 91001, 1, 'WECHAT_VIDEO', 'PERSONAL_ACCOUNT', 'E2E-DF-视频号主号', 'e2e_df_wv_main',
       9002, 9004, 9004, 'c6379a61450e948c13460a8d5f0f656aa5cd06b2141dbee510a657bf81c135b4', 'NORMAL', NULL,
       'e2e-df', 'e2e-df'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM oa_account WHERE id = 91001);

-- ========== 2. 内部采集配置（视频号 Tab） ==========
INSERT INTO oa_collect_config (tenant_id, scope, config_name, sub_type, platform_type, account_id, account_identifier,
                               app_id, collect_method, status, remark, creator, updater)
SELECT 1, 'INTERNAL', 'E2E-DF-视频号主号', 'platform', 'WECHAT_VIDEO', 91001, 'e2e_df_wv_main',
       'e2e_df_app_id', 'INTERNAL', 'ENABLED', 'E2E-DF-20260611 内部采集配置', 'e2e-df', 'e2e-df'
FROM DUAL WHERE NOT EXISTS (
  SELECT 1 FROM oa_collect_config WHERE tenant_id = 1 AND account_id = 91001 AND scope = 'INTERNAL' AND deleted = 0
);

-- ========== 3. 粉丝日数据（今日高粉 + 历史对比） ==========
INSERT INTO oa_follower_daily (tenant_id, account_id, stat_date, follower_count, new_follower, unfollow_count, net_growth, growth_rate, creator)
SELECT 1, 91001, '2026-06-10', 2450000, 12000, 800, 11200, 0.0046, 'e2e-df'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM oa_follower_daily WHERE account_id = 91001 AND stat_date = '2026-06-10' AND deleted = 0);

INSERT INTO oa_follower_daily (tenant_id, account_id, stat_date, follower_count, new_follower, unfollow_count, net_growth, growth_rate, creator)
SELECT 1, 91001, '2026-06-11', 2520000, 85000, 1200, 83800, 0.0342, 'e2e-df'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM oa_follower_daily WHERE account_id = 91001 AND stat_date = '2026-06-11' AND deleted = 0);

-- ========== 4. 短视频作品（累计指标 + 爆款标识） ==========
INSERT INTO oa_content (id, tenant_id, account_id, author_id, title, platform_type, content_type, publish_time,
                        read_count, like_count, comment_count, forward_count, is_hit, data_source, status, creator, updater)
SELECT 94001, 1, 91001, NULL, 'E2E-DF-爆款短视频A', 'WECHAT_VIDEO', 'SHORT_VIDEO', '2026-06-09 10:00:00',
       5200000, 380000, 42000, 85000, 1, 'API', 'PUBLISHED', 'e2e-df', 'e2e-df'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM oa_content WHERE id = 94001);

INSERT INTO oa_content (id, tenant_id, account_id, author_id, title, platform_type, content_type, publish_time,
                        read_count, like_count, comment_count, forward_count, is_hit, data_source, status, creator, updater)
SELECT 94002, 1, 91001, NULL, 'E2E-DF-短视频B', 'WECHAT_VIDEO', 'SHORT_VIDEO', '2026-06-10 14:00:00',
       1800000, 95000, 12000, 28000, 0, 'API', 'PUBLISHED', 'e2e-df', 'e2e-df'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM oa_content WHERE id = 94002);

INSERT INTO oa_content (id, tenant_id, account_id, author_id, title, platform_type, content_type, publish_time,
                        read_count, like_count, comment_count, forward_count, is_hit, data_source, status, creator, updater)
SELECT 94003, 1, 91001, NULL, 'E2E-DF-短视频C', 'WECHAT_VIDEO', 'SHORT_VIDEO', '2026-06-11 08:30:00',
       3200000, 210000, 28000, 52000, 1, 'API', 'PUBLISHED', 'e2e-df', 'e2e-df'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM oa_content WHERE id = 94003);

-- ========== 5. 作品日趋势（昨日 vs 今日增量） ==========
INSERT INTO oa_content_daily (tenant_id, content_id, stat_date, read_count, play_count, creator)
SELECT 1, 94001, '2026-06-10', 4100000, 4100000, 'e2e-df'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM oa_content_daily WHERE content_id = 94001 AND stat_date = '2026-06-10' AND deleted = 0);

INSERT INTO oa_content_daily (tenant_id, content_id, stat_date, read_count, play_count, creator)
SELECT 1, 94001, '2026-06-11', 5200000, 5200000, 'e2e-df'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM oa_content_daily WHERE content_id = 94001 AND stat_date = '2026-06-11' AND deleted = 0);

INSERT INTO oa_content_daily (tenant_id, content_id, stat_date, read_count, play_count, creator)
SELECT 1, 94003, '2026-06-11', 3200000, 3200000, 'e2e-df'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM oa_content_daily WHERE content_id = 94003 AND stat_date = '2026-06-11' AND deleted = 0);

-- ========== 6. IP 组 + 成员 + 账号归属 ==========
INSERT INTO oa_ip_group (id, tenant_id, group_name, group_type, parent_id, leader_user_id, sort_order, status, remark, creator, updater)
SELECT 92001, 1, 'E2E-DF-测试IP组', 2, 9000, 1002, 99, 1, 'E2E-DF-20260611', 'e2e-df', 'e2e-df'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM oa_ip_group WHERE id = 92001);

INSERT INTO oa_ip_group_member (tenant_id, ip_group_id, user_id, position, is_leader, creator, updater)
SELECT 1, 92001, 1003, 'OPERATOR', 0, 'e2e-df', 'e2e-df'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM oa_ip_group_member WHERE ip_group_id = 92001 AND user_id = 1003 AND deleted = 0);

UPDATE oa_account SET ip_group_id = 92001, updater = 'e2e-df' WHERE id = 91001 AND tenant_id = 1;

-- ========== 7. 作者 ==========
INSERT INTO oa_author (id, tenant_id, author_name, ip_group_id, author_type, primary_account_id, user_id, status, remark, creator, updater)
SELECT 93001, 1, 'E2E-DF-测试作者', 92001, 'SHORT_VIDEO', 91001, 1003, 1, 'E2E-DF-20260611', 'e2e-df', 'e2e-df'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM oa_author WHERE id = 93001);

UPDATE oa_content SET author_id = 93001, updater = 'e2e-df' WHERE account_id = 91001 AND tenant_id = 1 AND id IN (94001, 94002, 94003);

-- ========== 8. 财务成本 ==========
INSERT INTO oa_account_cost (id, tenant_id, account_id, cost_type, amount, pay_method, pay_date, period, remark, creator, updater)
SELECT 97101, 1, 91001, 'PROCUREMENT', 88000.00, 'BANK_TRANSFER', '2026-06-01', 'ONCE', 'E2E-DF 采购成本', 'e2e-df', 'e2e-df'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM oa_account_cost WHERE id = 97101);

INSERT INTO oa_account_cost (id, tenant_id, account_id, cost_type, amount, pay_method, pay_date, period, remark, creator, updater)
SELECT 97102, 1, 91001, 'PROCESS', 32000.00, 'BANK_TRANSFER', '2026-06-05', 'MONTH', 'E2E-DF 过程成本', 'e2e-df', 'e2e-df'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM oa_account_cost WHERE id = 97102);

-- ========== 9. 订单 + 归因 ==========
INSERT INTO oa_order (id, tenant_id, order_no, order_amount, order_time, account_id, ip_group_id, remark, creator, updater)
SELECT 98101, 1, 'E2E-DF-ORD-20260611-001', 56800.00, '2026-06-11 11:20:00', 91001, 92001, 'E2E-DF 作品带货订单', 'e2e-df', 'e2e-df'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM oa_order WHERE id = 98101);

INSERT INTO oa_order_attribution (id, tenant_id, order_id, account_id, ip_group_id, author_id, ops_user_id, revenue, cost, roi, stat_date, creator, updater)
SELECT 98111, 1, 98101, 91001, 92001, 93001, 1003, 56800.00, 12000.00, 4.7333, '2026-06-11', 'e2e-df', 'e2e-df'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM oa_order_attribution WHERE id = 98111);

-- ========== 10. M7 外部监测作品（爆款/高粉页面） ==========
INSERT INTO oa_external_work (id, tenant_id, account_id, platform_type, content_type, title, work_url, play_count, completion_rate, like_count, publish_time, industry, ip_group_id, is_external, creator, updater)
SELECT 96101, 1, 91001, 'WECHAT_VIDEO', 'SHORT_VIDEO', 'E2E-DF-爆款短视频A', 'https://e2e.example/wv/94001', 5200000, 0.6800, 380000, '2026-06-09 10:00:00', 'LIFESTYLE', 92001, 0, 'e2e-df', 'e2e-df'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM oa_external_work WHERE id = 96101);

INSERT INTO oa_external_work (id, tenant_id, account_id, platform_type, content_type, title, work_url, play_count, completion_rate, like_count, publish_time, industry, ip_group_id, is_external, creator, updater)
SELECT 96102, 1, 91001, 'WECHAT_VIDEO', 'SHORT_VIDEO', 'E2E-DF-短视频C', 'https://e2e.example/wv/94003', 3200000, 0.5500, 210000, '2026-06-11 08:30:00', 'LIFESTYLE', 92001, 0, 'e2e-df', 'e2e-df'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM oa_external_work WHERE id = 96102);

-- ========== 11. 粉丝阈值（触发高粉判定） ==========
INSERT INTO oa_threshold_config (tenant_id, threshold_category, metric_name, metric_type, platform_type, low_fans, high_fans, daily_low, daily_high, compare_operator, threshold_value, status, remark, creator, updater)
SELECT 1, 'FANS', 'FAN_COUNT', 'FOLLOWER', 'WECHAT_VIDEO', 10000, 1000000, 100, 50000, 'GTE', 0, 'ENABLED', 'E2E-DF 视频号粉丝阈值', 'e2e-df', 'e2e-df'
FROM DUAL WHERE NOT EXISTS (
  SELECT 1 FROM oa_threshold_config WHERE tenant_id = 1 AND threshold_category = 'FANS' AND platform_type = 'WECHAT_VIDEO' AND deleted = 0
);

INSERT INTO oa_threshold_config (tenant_id, threshold_category, metric_name, metric_type, platform_type, content_type, hot_value, low_value, judge_mode, compare_operator, threshold_value, status, remark, creator, updater)
SELECT 1, 'WORK', 'PLAY_COUNT', 'PLAY_COUNT', 'WECHAT_VIDEO', 'SHORT_VIDEO', 1000000, 1000, 'OR', 'GTE', 1000000, 'ENABLED', 'E2E-DF 爆款作品阈值', 'e2e-df', 'e2e-df'
FROM DUAL WHERE NOT EXISTS (
  SELECT 1 FROM oa_threshold_config WHERE tenant_id = 1 AND threshold_category = 'WORK' AND platform_type = 'WECHAT_VIDEO' AND deleted = 0
);

-- ========== 12. E2E 专用指标（账号/作品/IP组/人员） ==========
INSERT INTO oa_metric (id, tenant_id, metric_name, metric_code, unit, category, metric_formula, data_source, status, creator, updater)
SELECT 99201, 1, 'E2E-DF-账号粉丝数', 'E2E_DF_ACCOUNT_FANS', '人', 'GROWTH',
       'SELECT COALESCE(MAX(f.follower_count),0) AS metric_value FROM oa_follower_daily f WHERE f.tenant_id = :tenantId AND f.deleted = 0 AND f.account_id = 91001',
       'oa_follower_daily', 1, 'e2e-df', 'e2e-df'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM oa_metric WHERE id = 99201);

INSERT INTO oa_metric (id, tenant_id, metric_name, metric_code, unit, category, metric_formula, data_source, status, creator, updater)
SELECT 99202, 1, 'E2E-DF-作品播放量', 'E2E_DF_CONTENT_PLAY', '次', 'CONTENT',
       'SELECT COALESCE(SUM(c.read_count),0) AS metric_value FROM oa_content c WHERE c.tenant_id = :tenantId AND c.deleted = 0 AND c.account_id = 91001',
       'oa_content', 1, 'e2e-df', 'e2e-df'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM oa_metric WHERE id = 99202);

INSERT INTO oa_metric (id, tenant_id, metric_name, metric_code, unit, category, metric_formula, data_source, status, creator, updater)
SELECT 99203, 1, 'E2E-DF-IP组订单营收', 'E2E_DF_IPG_REVENUE', '元', 'REVENUE',
       'SELECT COALESCE(SUM(a.revenue),0) AS metric_value FROM oa_order_attribution a WHERE a.tenant_id = :tenantId AND a.deleted = 0 AND a.ip_group_id = 92001',
       'oa_order_attribution', 1, 'e2e-df', 'e2e-df'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM oa_metric WHERE id = 99203);

INSERT INTO oa_metric (id, tenant_id, metric_name, metric_code, unit, category, metric_formula, data_source, status, creator, updater)
SELECT 99204, 1, 'E2E-DF-人员作品数', 'E2E_DF_AUTHOR_CONTENT', '篇', 'CONTENT',
       'SELECT COUNT(*) AS metric_value FROM oa_content c WHERE c.tenant_id = :tenantId AND c.deleted = 0 AND c.author_id = 93001',
       'oa_content', 1, 'e2e-df', 'e2e-df'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM oa_metric WHERE id = 99204);

-- ========== 13. 自定义漏斗 ==========
INSERT INTO oa_funnel (id, tenant_id, funnel_name, funnel_type, status, remark, creator, updater)
SELECT 99301, 1, 'E2E-DF-转化漏斗', 'CUSTOM', 1, 'E2E-DF-20260611', 'e2e-df', 'e2e-df'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM oa_funnel WHERE id = 99301);

INSERT INTO oa_funnel_step (funnel_id, step_order, event_code, step_name, creator, updater)
SELECT 99301, 1, 'E2E_DF_ACCOUNT_FANS', '粉丝规模', 'e2e-df', 'e2e-df'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM oa_funnel_step WHERE funnel_id = 99301 AND step_order = 1 AND deleted = 0);

INSERT INTO oa_funnel_step (funnel_id, step_order, event_code, step_name, creator, updater)
SELECT 99301, 2, 'E2E_DF_CONTENT_PLAY', '作品播放', 'e2e-df', 'e2e-df'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM oa_funnel_step WHERE funnel_id = 99301 AND step_order = 2 AND deleted = 0);

INSERT INTO oa_funnel_step (funnel_id, step_order, event_code, step_name, creator, updater)
SELECT 99301, 3, 'E2E_DF_IPG_REVENUE', '订单营收', 'e2e-df', 'e2e-df'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM oa_funnel_step WHERE funnel_id = 99301 AND step_order = 3 AND deleted = 0);
