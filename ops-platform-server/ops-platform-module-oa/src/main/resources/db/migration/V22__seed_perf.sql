-- seed-perf: 指标 / 模板 / 考核记录 / 订单归因

INSERT INTO oa_metric (id, tenant_id, metric_name, metric_code, unit, category, status, creator, updater) VALUES
(9501, 1, '推文发布数', 'POST_COUNT', '篇', 'CONTENT', 1, 'seed-perf', 'seed-perf'),
(9502, 1, '营收贡献', 'REVENUE', '元', 'REVENUE', 1, 'seed-perf', 'seed-perf'),
(9503, 1, 'ROI', 'ROI', '倍', 'REVENUE', 1, 'seed-perf', 'seed-perf'),
(9504, 1, '任务完成率', 'TASK_COMPLETE_RATE', '%', 'OPS', 1, 'seed-perf', 'seed-perf'),
(9505, 1, '粉丝净增', 'FOLLOWER_NET', '人', 'GROWTH', 1, 'seed-perf', 'seed-perf');

-- 运营组长模板（单指标 weight=100）
INSERT INTO oa_perf_template (id, tenant_id, position, template_name, is_active, remark, creator, updater) VALUES
(9511, 1, 'OPS_LEADER', 'SEED-运营组长考核-2026', 1, 'seed-perf', 'seed-perf', 'seed-perf'),
(9512, 1, 'OPERATOR', 'SEED-运营专员考核-2026', 1, 'seed-perf', 'seed-perf', 'seed-perf');

INSERT INTO oa_perf_template_item (id, template_id, metric_id, weight, calc_rule, score_standard_json, creator, updater) VALUES
(9521, 9511, 9502, 100.00, 'AUTO', '{"ranges":[{"min":0,"max":50000,"score":60,"grade":"C"},{"min":50000,"max":100000,"score":75,"grade":"B"},{"min":100000,"max":200000,"score":85,"grade":"A"},{"min":200000,"max":9999999,"score":100,"grade":"S"}]}', 'seed-perf', 'seed-perf'),
(9522, 9512, 9501, 100.00, 'AUTO', '{"ranges":[{"min":0,"max":20,"score":60,"grade":"C"},{"min":20,"max":40,"score":75,"grade":"B"},{"min":40,"max":60,"score":85,"grade":"A"},{"min":60,"max":9999,"score":100,"grade":"S"}]}', 'seed-perf', 'seed-perf');

-- 考核记录：5 条（DRAFT + CONFIRMED）
INSERT INTO oa_perf_record (id, tenant_id, template_id, target_user_id, ip_group_id, period_type, period_start, period_end, total_score, grade, status, creator, updater) VALUES
(9531, 1, 9511, 1002, NULL, 'MONTH', '2026-05-01', '2026-05-31', 85.00, 'A', 'CONFIRMED', 'seed-perf', 'seed-perf'),
(9532, 1, 9512, 1003, 9001, 'MONTH', '2026-05-01', '2026-05-31', 75.00, 'B', 'CONFIRMED', 'seed-perf', 'seed-perf'),
(9533, 1, 9511, 1002, NULL, 'MONTH', '2026-06-01', '2026-06-30', NULL, NULL, 'DRAFT', 'seed-perf', 'seed-perf'),
(9534, 1, 9512, 1003, 9001, 'MONTH', '2026-06-01', '2026-06-30', NULL, NULL, 'DRAFT', 'seed-perf', 'seed-perf'),
(9535, 1, 9512, 1005, NULL, 'MONTH', '2026-06-01', '2026-06-30', 90.00, 'A', 'CONFIRMED', 'seed-perf', 'seed-perf');

INSERT INTO oa_perf_item_record (id, record_id, metric_id, metric_value, score, manual_adjustment, final_score, creator, updater) VALUES
(9541, 9531, 9502, 150000.0000, 85.00, 0.00, 85.00, 'seed-perf', 'seed-perf'),
(9542, 9532, 9501, 45.0000, 85.00, -5.00, 80.00, 'seed-perf', 'seed-perf'),
(9543, 9535, 9501, 55.0000, 85.00, 5.00, 90.00, 'seed-perf', 'seed-perf');

-- 订单 + 归因（10+ 条，关联 M1 作者 / M4 账号 / IP 组）
INSERT INTO oa_order (id, tenant_id, order_no, order_amount, order_time, account_id, ip_group_id, creator, updater) VALUES
(9551, 1, 'SEED-ORD-20260601-001', 12800.00, '2026-06-01 10:30:00', 9001, 9001, 'seed-perf', 'seed-perf'),
(9552, 1, 'SEED-ORD-20260601-002', 8600.00,  '2026-06-01 14:20:00', 9002, 9001, 'seed-perf', 'seed-perf'),
(9553, 1, 'SEED-ORD-20260602-001', 15200.00, '2026-06-02 09:15:00', 9001, 9001, 'seed-perf', 'seed-perf'),
(9554, 1, 'SEED-ORD-20260602-002', 9800.00,  '2026-06-02 16:45:00', 9003, 9001, 'seed-perf', 'seed-perf'),
(9555, 1, 'SEED-ORD-20260603-001', 22000.00, '2026-06-03 11:00:00', 9006, 9002, 'seed-perf', 'seed-perf'),
(9556, 1, 'SEED-ORD-20260603-002', 11500.00, '2026-06-03 18:30:00', 9007, 9002, 'seed-perf', 'seed-perf'),
(9557, 1, 'SEED-ORD-20260604-001', 18900.00, '2026-06-04 08:50:00', 9001, 9001, 'seed-perf', 'seed-perf'),
(9558, 1, 'SEED-ORD-20260605-001', 7600.00,  '2026-06-05 13:10:00', 9002, 9001, 'seed-perf', 'seed-perf'),
(9559, 1, 'SEED-ORD-20260605-002', 14300.00, '2026-06-05 20:00:00', 9010, 9002, 'seed-perf', 'seed-perf'),
(9560, 1, 'SEED-ORD-20260606-001', 20100.00, '2026-06-06 12:25:00', 9001, 9001, 'seed-perf', 'seed-perf'),
(9561, 1, 'SEED-ORD-20260607-001', 9900.00,  '2026-06-07 15:40:00', 9003, 9001, 'seed-perf', 'seed-perf'),
(9562, 1, 'SEED-ORD-20260608-001', 16700.00, '2026-06-08 09:55:00', 9006, 9002, 'seed-perf', 'seed-perf');

INSERT INTO oa_order_attribution (id, tenant_id, order_id, account_id, ip_group_id, author_id, ops_user_id, revenue, cost, roi, stat_date, creator, updater) VALUES
(9571, 1, 9551, 9001, 9001, 9101, 1003, 12800.00, 3200.00, 4.0000, '2026-06-01', 'seed-perf', 'seed-perf'),
(9572, 1, 9552, 9002, 9001, 9102, 1003, 8600.00, 2150.00, 4.0000, '2026-06-01', 'seed-perf', 'seed-perf'),
(9573, 1, 9553, 9001, 9001, 9101, 1003, 15200.00, 3800.00, 4.0000, '2026-06-02', 'seed-perf', 'seed-perf'),
(9574, 1, 9554, 9003, 9001, 9103, 1003, 9800.00, 2450.00, 4.0000, '2026-06-02', 'seed-perf', 'seed-perf'),
(9575, 1, 9555, 9006, 9002, 9104, 1005, 22000.00, 5500.00, 4.0000, '2026-06-03', 'seed-perf', 'seed-perf'),
(9576, 1, 9556, 9007, 9002, 9104, 1005, 11500.00, 2875.00, 4.0000, '2026-06-03', 'seed-perf', 'seed-perf'),
(9577, 1, 9557, 9001, 9001, 9101, 1003, 18900.00, 4725.00, 4.0000, '2026-06-04', 'seed-perf', 'seed-perf'),
(9578, 1, 9558, 9002, 9001, 9102, 1003, 7600.00, 1900.00, 4.0000, '2026-06-05', 'seed-perf', 'seed-perf'),
(9579, 1, 9559, 9010, 9002, 9104, 1005, 14300.00, 3575.00, 4.0000, '2026-06-05', 'seed-perf', 'seed-perf'),
(9580, 1, 9560, 9001, 9001, 9101, 1003, 20100.00, 5025.00, 4.0000, '2026-06-06', 'seed-perf', 'seed-perf'),
(9581, 1, 9561, 9003, 9001, 9103, 1003, 9900.00, 2475.00, 4.0000, '2026-06-07', 'seed-perf', 'seed-perf'),
(9582, 1, 9562, 9006, 9002, 9104, 1005, 16700.00, 4175.00, 4.0000, '2026-06-08', 'seed-perf', 'seed-perf');
