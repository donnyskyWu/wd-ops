-- seed-ops: 作者 / 粉丝日表 / 作品 / 运营主播关联

INSERT INTO oa_author (id, tenant_id, author_name, ip_group_id, author_type, primary_account_id, user_id, status, remark, creator, updater) VALUES
(9101, 1, 'SEED-作者张三', 9001, 'SHORT_VIDEO', 9001, 1002, 1, 'seed-ops', 'seed-ops', 'seed-ops'),
(9102, 1, 'SEED-作者李四', 9001, 'SHORT_VIDEO', 9002, 1003, 1, 'seed-ops', 'seed-ops', 'seed-ops'),
(9103, 1, 'SEED-作者王五', 9001, 'ARTICLE',     9003, 1004, 1, 'seed-ops', 'seed-ops', 'seed-ops'),
(9104, 1, 'SEED-作者赵六', 9002, 'SHORT_VIDEO', 9010, 1005, 1, 'seed-ops', 'seed-ops', 'seed-ops'),
(9105, 1, 'SEED-作者钱七', 9002, 'ARTICLE',     NULL,   1002, 1, 'seed-ops', 'seed-ops', 'seed-ops');

INSERT INTO oa_ops_anchor_rel (id, tenant_id, ops_user_id, anchor_user_id, ip_group_id, start_date, end_date, creator, updater) VALUES
(9201, 1, 1003, 1004, 9001, '2026-01-01', '2026-12-31', 'seed-ops', 'seed-ops'),
(9202, 1, 1002, 1005, 9002, '2026-03-01', '2026-09-30', 'seed-ops', 'seed-ops');

INSERT INTO oa_ip_group_member (tenant_id, ip_group_id, user_id, position, is_leader, creator, updater) VALUES
(1, 9001, 1003, 'OPERATOR', 0, 'seed-ops', 'seed-ops'),
(1, 9002, 1005, 'ANCHOR', 0, 'seed-ops', 'seed-ops');

INSERT INTO oa_ip_group_anchor_rel (tenant_id, ip_group_id, anchor_user_id, anchor_type, creator, updater) VALUES
(1, 9001, 1004, 'VIDEO', 'seed-ops', 'seed-ops'),
(1, 9002, 1005, 'LIVE', 'seed-ops', 'seed-ops');

-- 粉丝日表：账号 9001 连续 30 天（2026-05-10 ~ 2026-06-08）
INSERT INTO oa_follower_daily (tenant_id, account_id, stat_date, follower_count, new_follower, unfollow_count, net_growth, growth_rate, creator) VALUES
(1, 9001, '2026-05-10', 100000, 500, 100, 400, 0.0040, 'seed-ops'),
(1, 9001, '2026-05-11', 100400, 520, 90, 430, 0.0043, 'seed-ops'),
(1, 9001, '2026-05-12', 100830, 480, 110, 370, 0.0037, 'seed-ops'),
(1, 9001, '2026-05-13', 101200, 510, 140, 370, 0.0037, 'seed-ops'),
(1, 9001, '2026-05-14', 101570, 490, 120, 370, 0.0036, 'seed-ops'),
(1, 9001, '2026-05-15', 101940, 530, 160, 370, 0.0036, 'seed-ops'),
(1, 9001, '2026-05-16', 102310, 500, 130, 370, 0.0036, 'seed-ops'),
(1, 9001, '2026-05-17', 102680, 520, 150, 370, 0.0036, 'seed-ops'),
(1, 9001, '2026-05-18', 103050, 510, 140, 370, 0.0036, 'seed-ops'),
(1, 9001, '2026-05-19', 103420, 490, 120, 370, 0.0036, 'seed-ops'),
(1, 9001, '2026-05-20', 103790, 530, 160, 370, 0.0036, 'seed-ops'),
(1, 9001, '2026-05-21', 104160, 500, 130, 370, 0.0036, 'seed-ops'),
(1, 9001, '2026-05-22', 104530, 520, 150, 370, 0.0035, 'seed-ops'),
(1, 9001, '2026-05-23', 104900, 510, 140, 370, 0.0035, 'seed-ops'),
(1, 9001, '2026-05-24', 105270, 490, 120, 370, 0.0035, 'seed-ops'),
(1, 9001, '2026-05-25', 105640, 530, 160, 370, 0.0035, 'seed-ops'),
(1, 9001, '2026-05-26', 106010, 500, 130, 370, 0.0035, 'seed-ops'),
(1, 9001, '2026-05-27', 106380, 520, 150, 370, 0.0035, 'seed-ops'),
(1, 9001, '2026-05-28', 106750, 510, 140, 370, 0.0035, 'seed-ops'),
(1, 9001, '2026-05-29', 107120, 490, 120, 370, 0.0035, 'seed-ops'),
(1, 9001, '2026-05-30', 107490, 530, 160, 370, 0.0034, 'seed-ops'),
(1, 9001, '2026-05-31', 107860, 500, 130, 370, 0.0034, 'seed-ops'),
(1, 9001, '2026-06-01', 108230, 520, 150, 370, 0.0034, 'seed-ops'),
(1, 9001, '2026-06-02', 108600, 510, 140, 370, 0.0034, 'seed-ops'),
(1, 9001, '2026-06-03', 108970, 490, 120, 370, 0.0034, 'seed-ops'),
(1, 9001, '2026-06-04', 109340, 530, 160, 370, 0.0034, 'seed-ops'),
(1, 9001, '2026-06-05', 109710, 500, 130, 370, 0.0034, 'seed-ops'),
(1, 9001, '2026-06-06', 110080, 520, 150, 370, 0.0034, 'seed-ops'),
(1, 9001, '2026-06-07', 110450, 510, 140, 370, 0.0033, 'seed-ops'),
(1, 9001, '2026-06-08', 110820, 490, 120, 370, 0.0033, 'seed-ops');

INSERT INTO oa_content (id, tenant_id, account_id, title, platform_type, content_type, publish_time, read_count, like_count, comment_count, forward_count, is_hit, data_source, status, creator, updater) VALUES
(9301, 1, 9001, 'SEED-爆款文章A', 'WECHAT_OFFICIAL', 'ARTICLE', '2026-06-01 10:00:00', 120000, 6000, 200, 1000, 1, 'API', 'PUBLISHED', 'seed-ops', 'seed-ops'),
(9302, 1, 9001, 'SEED-普通文章B', 'WECHAT_OFFICIAL', 'ARTICLE', '2026-06-02 11:00:00', 5000, 100, 20, 50, 0, 'API', 'PUBLISHED', 'seed-ops', 'seed-ops'),
(9303, 1, 9002, 'SEED-爆款文章C', 'WECHAT_OFFICIAL', 'ARTICLE', '2026-06-03 09:00:00', 150000, 8000, 300, 2000, 1, 'API', 'PUBLISHED', 'seed-ops', 'seed-ops'),
(9304, 1, 9006, 'SEED-抖音作品D',  'DOUYIN',          'VIDEO',   '2026-06-04 20:00:00', 80000, 4000, 150, 800, 0, 'API', 'PUBLISHED', 'seed-ops', 'seed-ops');
