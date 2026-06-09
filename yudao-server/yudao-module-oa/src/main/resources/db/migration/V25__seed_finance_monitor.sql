-- seed-finance / seed-monitor / funnel / dashboard

-- ========== M5 账号成本 (10+) ==========
INSERT INTO oa_account_cost (id, tenant_id, account_id, cost_type, amount, pay_method, pay_date, period, remark, creator, updater) VALUES
(9701, 1, 9001, 'PURCHASE', 50000.00, 'CORPORATE', '2026-03-15', 'ONCE', 'SEED-公众号A1购买', 'seed-finance', 'seed-finance'),
(9702, 1, 9001, 'PROCESS_HUMAN', 12000.00, 'WECHAT', '2026-04-01', 'MONTH', 'SEED-运营人力', 'seed-finance', 'seed-finance'),
(9703, 1, 9002, 'PURCHASE', 38000.00, 'CORPORATE', '2026-03-20', 'ONCE', 'SEED-公众号A2购买', 'seed-finance', 'seed-finance'),
(9704, 1, 9002, 'AD_SPEND', 8000.00, 'ALIPAY', '2026-05-01', 'MONTH', 'SEED-投放', 'seed-finance', 'seed-finance'),
(9705, 1, 9003, 'PROCESS_HUMAN', 9500.00, 'BANK', '2026-04-15', 'MONTH', 'SEED-服务号人力', 'seed-finance', 'seed-finance'),
(9706, 1, 9006, 'PURCHASE', 25000.00, 'CORPORATE', '2026-03-25', 'ONCE', 'SEED-抖音号购买', 'seed-finance', 'seed-finance'),
(9707, 1, 9006, 'AD_SPEND', 15000.00, 'WECHAT', '2026-05-10', 'MONTH', 'SEED-抖音投放', 'seed-finance', 'seed-finance'),
(9708, 1, 9007, 'PROCESS_HUMAN', 11000.00, 'ALIPAY', '2026-04-20', 'MONTH', 'SEED-抖音运营', 'seed-finance', 'seed-finance'),
(9709, 1, 9010, 'PURCHASE', 42000.00, 'CORPORATE', '2026-03-18', 'ONCE', 'SEED-公众号B1购买', 'seed-finance', 'seed-finance'),
(9710, 1, 9010, 'AD_SPEND', 6000.00, 'WECHAT', '2026-05-05', 'MONTH', 'SEED-B1投放', 'seed-finance', 'seed-finance'),
(9711, 1, 9008, 'PROCESS_HUMAN', 7500.00, 'BANK', '2026-04-10', 'MONTH', 'SEED-快手运营', 'seed-finance', 'seed-finance'),
(9712, 1, 9004, 'PURCHASE', 18000.00, 'CORPORATE', '2026-03-22', 'ONCE', 'SEED-视频号购买', 'seed-finance', 'seed-finance');

-- ========== M6 漏斗 (2) + 步骤 ==========
INSERT INTO oa_funnel (id, tenant_id, funnel_name, funnel_type, status, remark, creator, updater) VALUES
(9801, 1, 'SEED-内容转化漏斗', 'CONVERSION', 1, 'seed-analytics', 'seed-analytics', 'seed-analytics'),
(9802, 1, 'SEED-粉丝增长漏斗', 'CUSTOM', 1, 'seed-analytics', 'seed-analytics', 'seed-analytics');

INSERT INTO oa_funnel_step (id, funnel_id, step_order, event_code, step_name, creator, updater) VALUES
(9811, 9801, 1, 'VIEW', '曝光', 'seed-analytics', 'seed-analytics'),
(9812, 9801, 2, 'READ', '阅读', 'seed-analytics', 'seed-analytics'),
(9813, 9801, 3, 'LIKE', '点赞', 'seed-analytics', 'seed-analytics'),
(9814, 9801, 4, 'SHARE', '转发', 'seed-analytics', 'seed-analytics'),
(9821, 9802, 1, 'EXPOSURE', '曝光', 'seed-analytics', 'seed-analytics'),
(9822, 9802, 2, 'FOLLOW', '关注', 'seed-analytics', 'seed-analytics'),
(9823, 9802, 3, 'REVISIT', '二次访问', 'seed-analytics', 'seed-analytics');

-- ========== M6 大屏 (1) ==========
INSERT INTO oa_dashboard (id, tenant_id, dashboard_name, dashboard_type, layout_json, status, creator, updater) VALUES
(9851, 1, 'SEED-运营总览大屏', 'BUSINESS', '[{"type":"kpi","metric":"follower_total"},{"type":"chart","metric":"content_read"}]', 1, 'seed-analytics', 'seed-analytics');

-- ========== M6 自定义查询样本 ==========
INSERT INTO oa_custom_query (id, tenant_id, query_name, status, sql_text, params_json, creator, updater) VALUES
(9861, 1, 'SEED-近30天粉丝增长', 'PUBLISHED', 'SELECT stat_date, follower_count FROM oa_follower_daily WHERE account_id = :accountId', '{"accountId":9001}', 'seed-analytics', 'seed-analytics');

-- ========== M7 外部作品 (15+) ==========
INSERT INTO oa_external_work (id, tenant_id, account_id, platform_type, title, work_url, play_count, completion_rate, like_count, publish_time, industry, ip_group_id, is_external, creator, updater) VALUES
(9901, 1, 9006, 'DOUYIN', 'SEED-爆款短视频-娱乐1', 'https://example.com/dy/9901', 2500000, 0.4500, 120000, '2026-05-20 18:00:00', 'ENTERTAINMENT', 9002, 1, 'seed-monitor', 'seed-monitor'),
(9902, 1, 9006, 'DOUYIN', 'SEED-爆款短视频-美妆1', 'https://example.com/dy/9902', 1800000, 0.3800, 95000, '2026-05-22 12:00:00', 'BEAUTY', 9002, 1, 'seed-monitor', 'seed-monitor'),
(9903, 1, 9007, 'DOUYIN', 'SEED-爆款短视频-美食1', 'https://example.com/dy/9903', 3200000, 0.5200, 180000, '2026-05-25 20:00:00', 'FOOD', 9002, 1, 'seed-monitor', 'seed-monitor'),
(9904, 1, 9008, 'KUAISHOU', 'SEED-爆款快手-娱乐2', 'https://example.com/ks/9904', 1500000, 0.3500, 80000, '2026-05-18 15:00:00', 'ENTERTAINMENT', 9002, 1, 'seed-monitor', 'seed-monitor'),
(9905, 1, 9004, 'WECHAT_VIDEO', 'SEED-视频号爆款1', 'https://example.com/wv/9905', 1100000, 0.4200, 55000, '2026-05-28 10:00:00', 'LIFESTYLE', 9002, 1, 'seed-monitor', 'seed-monitor'),
(9906, 1, 9006, 'DOUYIN', 'SEED-低分作品-1', 'https://example.com/dy/9906', 50000, 0.1200, 800, '2026-05-15 09:00:00', 'ENTERTAINMENT', 9002, 1, 'seed-monitor', 'seed-monitor'),
(9907, 1, 9007, 'DOUYIN', 'SEED-低分作品-2', 'https://example.com/dy/9907', 80000, 0.1500, 1200, '2026-05-16 11:00:00', 'BEAUTY', 9002, 1, 'seed-monitor', 'seed-monitor'),
(9908, 1, 9008, 'KUAISHOU', 'SEED-低分作品-3', 'https://example.com/ks/9908', 30000, 0.0800, 400, '2026-05-17 14:00:00', 'FOOD', 9002, 1, 'seed-monitor', 'seed-monitor'),
(9909, 1, 9006, 'DOUYIN', 'SEED-普通作品-1', 'https://example.com/dy/9909', 250000, 0.2800, 12000, '2026-05-19 16:00:00', 'ENTERTAINMENT', 9002, 1, 'seed-monitor', 'seed-monitor'),
(9910, 1, 9007, 'DOUYIN', 'SEED-普通作品-2', 'https://example.com/dy/9910', 180000, 0.3200, 9000, '2026-05-21 19:00:00', 'BEAUTY', 9002, 1, 'seed-monitor', 'seed-monitor'),
(9911, 1, 9001, 'WECHAT_OFFICIAL', 'SEED-微信图文-1', 'https://example.com/mp/9911', 85000, NULL, 4200, '2026-05-23 08:00:00', 'LIFESTYLE', 9001, 1, 'seed-monitor', 'seed-monitor'),
(9912, 1, 9002, 'WECHAT_OFFICIAL', 'SEED-微信图文-2', 'https://example.com/mp/9912', 120000, NULL, 6800, '2026-05-24 09:30:00', 'BEAUTY', 9001, 1, 'seed-monitor', 'seed-monitor'),
(9913, 1, 9010, 'WECHAT_OFFICIAL', 'SEED-微信图文-3', 'https://example.com/mp/9913', 95000, NULL, 5100, '2026-05-26 10:00:00', 'ENTERTAINMENT', 9002, 1, 'seed-monitor', 'seed-monitor'),
(9914, 1, 9006, 'DOUYIN', 'SEED-行业-教育1', 'https://example.com/dy/9914', 450000, 0.3500, 22000, '2026-05-27 17:00:00', 'EDUCATION', 9002, 1, 'seed-monitor', 'seed-monitor'),
(9915, 1, 9007, 'DOUYIN', 'SEED-行业-科技1', 'https://example.com/dy/9915', 380000, 0.3100, 18000, '2026-05-29 21:00:00', 'TECH', 9002, 1, 'seed-monitor', 'seed-monitor'),
(9916, 1, 9008, 'KUAISHOU', 'SEED-低分作品-4', 'https://example.com/ks/9916', 45000, 0.1800, 600, '2026-05-30 13:00:00', 'ENTERTAINMENT', 9002, 1, 'seed-monitor', 'seed-monitor'),
(9917, 1, 9004, 'WECHAT_VIDEO', 'SEED-视频号普通', 'https://example.com/wv/9917', 200000, 0.2500, 10000, '2026-06-01 11:00:00', 'LIFESTYLE', 9002, 1, 'seed-monitor', 'seed-monitor');
