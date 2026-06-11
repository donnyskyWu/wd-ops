-- M6 指标管理：灌入可被「指标管理 / 漏斗分析」直接使用的真实指标
-- 每个指标都带 metric_formula（只读 SELECT，使用 :tenantId 占位 + deleted=0），可通过 /oa/metric/preview 试跑
-- 自定义漏斗步骤 event_code 复用 metric_code，FunnelServiceImpl.resolveStepCount 会执行公式取真实值

INSERT INTO oa_metric (id, tenant_id, metric_name, metric_code, unit, category, status, metric_formula, data_source, creator, updater) VALUES
(9610, 1, '内容发布数', 'CONTENT_PUBLISH_COUNT', '篇', 'BASIC', 1,
 'SELECT COUNT(*) AS metric_value FROM oa_content t WHERE t.tenant_id = :tenantId AND t.deleted = 0', 'oa_content', 'seed-metrics', 'seed-metrics'),
(9611, 1, '阅读总量', 'CONTENT_READ_TOTAL', '次', 'BASIC', 1,
 'SELECT COALESCE(SUM(t.read_count), 0) AS metric_value FROM oa_content t WHERE t.tenant_id = :tenantId AND t.deleted = 0', 'oa_content', 'seed-metrics', 'seed-metrics'),
(9612, 1, '点赞总量', 'CONTENT_LIKE_TOTAL', '次', 'BASIC', 1,
 'SELECT COALESCE(SUM(t.like_count), 0) AS metric_value FROM oa_content t WHERE t.tenant_id = :tenantId AND t.deleted = 0', 'oa_content', 'seed-metrics', 'seed-metrics'),
(9613, 1, '评论总量', 'CONTENT_COMMENT_TOTAL', '次', 'BASIC', 1,
 'SELECT COALESCE(SUM(t.comment_count), 0) AS metric_value FROM oa_content t WHERE t.tenant_id = :tenantId AND t.deleted = 0', 'oa_content', 'seed-metrics', 'seed-metrics'),
(9614, 1, '转发总量', 'CONTENT_FORWARD_TOTAL', '次', 'BASIC', 1,
 'SELECT COALESCE(SUM(t.forward_count), 0) AS metric_value FROM oa_content t WHERE t.tenant_id = :tenantId AND t.deleted = 0', 'oa_content', 'seed-metrics', 'seed-metrics'),
(9615, 1, '互动总量', 'CONTENT_INTERACTION_TOTAL', '次', 'BASIC', 1,
 'SELECT COALESCE(SUM(t.like_count + t.comment_count + t.forward_count), 0) AS metric_value FROM oa_content t WHERE t.tenant_id = :tenantId AND t.deleted = 0', 'oa_content', 'seed-metrics', 'seed-metrics'),
(9616, 1, '爆款内容数', 'CONTENT_HIT_COUNT', '篇', 'BASIC', 1,
 'SELECT COUNT(*) AS metric_value FROM oa_content t WHERE t.tenant_id = :tenantId AND t.deleted = 0 AND t.is_hit = 1', 'oa_content', 'seed-metrics', 'seed-metrics'),
(9617, 1, '全平台粉丝总量', 'FOLLOWER_LATEST_TOTAL', '人', 'BASIC', 1,
 'SELECT COALESCE(SUM(fd.follower_count), 0) AS metric_value FROM oa_follower_daily fd WHERE fd.tenant_id = :tenantId AND fd.deleted = 0 AND fd.stat_date = (SELECT MAX(stat_date) FROM oa_follower_daily WHERE tenant_id = :tenantId AND deleted = 0)', 'oa_follower_daily', 'seed-metrics', 'seed-metrics')
ON DUPLICATE KEY UPDATE
    metric_name = VALUES(metric_name),
    unit = VALUES(unit),
    category = VALUES(category),
    status = VALUES(status),
    metric_formula = VALUES(metric_formula),
    data_source = VALUES(data_source);

-- 补登已有分析指标（9601~9604）的真实计算公式，使其在漏斗中返回真实值
UPDATE oa_metric SET
    metric_formula = 'SELECT COALESCE(SUM(fd.follower_count), 0) AS metric_value FROM oa_follower_daily fd WHERE fd.tenant_id = :tenantId AND fd.deleted = 0 AND fd.stat_date = (SELECT MAX(stat_date) FROM oa_follower_daily WHERE tenant_id = :tenantId AND deleted = 0)',
    data_source = 'oa_follower_daily'
WHERE id = 9601 AND tenant_id = 1;
UPDATE oa_metric SET
    metric_formula = 'SELECT COALESCE(SUM(t.read_count), 0) AS metric_value FROM oa_content t WHERE t.tenant_id = :tenantId AND t.deleted = 0',
    data_source = 'oa_content'
WHERE id = 9602 AND tenant_id = 1;
UPDATE oa_metric SET
    metric_formula = 'SELECT COALESCE(SUM(cd.play_count), 0) AS metric_value FROM oa_content_daily cd WHERE cd.tenant_id = :tenantId AND cd.deleted = 0',
    data_source = 'oa_content_daily'
WHERE id = 9603 AND tenant_id = 1;
UPDATE oa_metric SET
    metric_formula = 'SELECT COALESCE(SUM(c.amount), 0) AS metric_value FROM oa_account_cost c WHERE c.tenant_id = :tenantId AND c.deleted = 0',
    data_source = 'oa_account_cost'
WHERE id = 9604 AND tenant_id = 1;

-- 演示自定义漏斗：步骤直接引用上面的真实指标 metric_code（FunnelServiceImpl 执行公式取数）
INSERT INTO oa_funnel (id, tenant_id, funnel_name, funnel_type, status, remark, creator, updater) VALUES
(9803, 1, 'SEED-内容互动漏斗(真实指标)', 'CUSTOM', 1, 'seed-metrics', 'seed-metrics', 'seed-metrics')
ON DUPLICATE KEY UPDATE funnel_name = VALUES(funnel_name), status = VALUES(status);

INSERT INTO oa_funnel_step (id, funnel_id, step_order, event_code, step_name, creator, updater) VALUES
(9831, 9803, 1, 'CONTENT_READ_TOTAL', '阅读总量', 'seed-metrics', 'seed-metrics'),
(9832, 9803, 2, 'CONTENT_INTERACTION_TOTAL', '互动总量', 'seed-metrics', 'seed-metrics'),
(9833, 9803, 3, 'CONTENT_LIKE_TOTAL', '点赞总量', 'seed-metrics', 'seed-metrics'),
(9834, 9803, 4, 'CONTENT_COMMENT_TOTAL', '评论总量', 'seed-metrics', 'seed-metrics'),
(9835, 9803, 5, 'CONTENT_HIT_COUNT', '爆款内容数', 'seed-metrics', 'seed-metrics')
ON DUPLICATE KEY UPDATE event_code = VALUES(event_code), step_name = VALUES(step_name);
