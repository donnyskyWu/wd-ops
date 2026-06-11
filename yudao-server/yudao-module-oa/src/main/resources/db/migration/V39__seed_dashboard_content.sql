-- seed-dashboard: home trend / platform-dist last-7-day test data (2026-06-05 ~ 2026-06-11)

INSERT IGNORE INTO oa_content (id, tenant_id, account_id, title, platform_type, content_type, publish_time, read_count, like_count, comment_count, forward_count, is_hit, data_source, status, creator, updater) VALUES
(9401, 1, 9001, 'SEED-dashboard-06-05-wx', 'WECHAT_OFFICIAL', 'ARTICLE', '2026-06-05 09:00:00', 12000, 600, 40, 80, 0, 'API', 'PUBLISHED', 'seed-dashboard', 'seed-dashboard'),
(9402, 1, 9002, 'SEED-dashboard-06-05-wx2', 'WECHAT_OFFICIAL', 'ARTICLE', '2026-06-05 14:00:00', 8500, 420, 25, 55, 0, 'API', 'PUBLISHED', 'seed-dashboard', 'seed-dashboard'),
(9403, 1, 9006, 'SEED-dashboard-06-06-dy', 'DOUYIN', 'VIDEO', '2026-06-06 10:30:00', 45000, 2200, 120, 350, 1, 'API', 'PUBLISHED', 'seed-dashboard', 'seed-dashboard'),
(9404, 1, 9007, 'SEED-dashboard-06-06-dy2', 'DOUYIN', 'VIDEO', '2026-06-06 18:00:00', 32000, 1600, 90, 200, 0, 'API', 'PUBLISHED', 'seed-dashboard', 'seed-dashboard'),
(9405, 1, 9008, 'SEED-dashboard-06-07-ks', 'KUAISHOU', 'VIDEO', '2026-06-07 11:00:00', 28000, 1400, 75, 180, 0, 'API', 'PUBLISHED', 'seed-dashboard', 'seed-dashboard'),
(9406, 1, 9001, 'SEED-dashboard-06-07-wx', 'WECHAT_OFFICIAL', 'ARTICLE', '2026-06-07 16:00:00', 15000, 750, 50, 100, 0, 'API', 'PUBLISHED', 'seed-dashboard', 'seed-dashboard'),
(9407, 1, 9004, 'SEED-dashboard-06-08-wv', 'WECHAT_VIDEO', 'VIDEO', '2026-06-08 10:00:00', 22000, 1100, 60, 150, 0, 'API', 'PUBLISHED', 'seed-dashboard', 'seed-dashboard'),
(9408, 1, 9003, 'SEED-dashboard-06-08-xhs', 'XIAOHONGSHU', 'ARTICLE', '2026-06-08 15:30:00', 18000, 900, 55, 120, 0, 'API', 'PUBLISHED', 'seed-dashboard', 'seed-dashboard'),
(9409, 1, 9006, 'SEED-dashboard-06-09-dy', 'DOUYIN', 'VIDEO', '2026-06-09 09:30:00', 52000, 2600, 140, 400, 1, 'API', 'PUBLISHED', 'seed-dashboard', 'seed-dashboard'),
(9410, 1, 9002, 'SEED-dashboard-06-09-wx', 'WECHAT_OFFICIAL', 'ARTICLE', '2026-06-09 13:00:00', 11000, 550, 35, 70, 0, 'API', 'PUBLISHED', 'seed-dashboard', 'seed-dashboard'),
(9411, 1, 9010, 'SEED-dashboard-06-10-wx', 'WECHAT_OFFICIAL', 'ARTICLE', '2026-06-10 10:00:00', 9500, 480, 30, 65, 0, 'API', 'PUBLISHED', 'seed-dashboard', 'seed-dashboard'),
(9412, 1, 9008, 'SEED-dashboard-06-10-ks', 'KUAISHOU', 'VIDEO', '2026-06-10 19:00:00', 35000, 1750, 95, 220, 0, 'API', 'PUBLISHED', 'seed-dashboard', 'seed-dashboard'),
(9413, 1, 9001, 'SEED-dashboard-06-11-wx', 'WECHAT_OFFICIAL', 'ARTICLE', '2026-06-11 08:30:00', 20000, 1000, 65, 130, 1, 'API', 'PUBLISHED', 'seed-dashboard', 'seed-dashboard'),
(9414, 1, 9007, 'SEED-dashboard-06-11-dy', 'DOUYIN', 'VIDEO', '2026-06-11 12:00:00', 48000, 2400, 130, 380, 0, 'API', 'PUBLISHED', 'seed-dashboard', 'seed-dashboard');
