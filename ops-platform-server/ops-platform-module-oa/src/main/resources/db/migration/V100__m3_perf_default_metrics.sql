-- M3: 绩效考核默认指标（人员 + 时间参数化，对齐 M6 指标构建器规则）
-- 更新 seed-perf 指标 9501~9505：params_json + metric_formula + data_source

-- 补全元数据字段（作者 / 运营人员）
INSERT INTO sys_metadata_field (tenant_id, entity_id, field_code, field_name, column_name, data_type, query_condition_type, dict_type, sort, creator, updater)
SELECT 1, e.id, 'author_id', '作者', 'author_id', 'BIGINT', 'USER_SELECT', NULL, 15, 'v100-perf', 'v100-perf'
FROM sys_metadata_entity e
WHERE e.tenant_id = 1 AND e.physical_table = 'oa_content'
  AND NOT EXISTS (SELECT 1 FROM sys_metadata_field f WHERE f.tenant_id = 1 AND f.entity_id = e.id AND f.field_code = 'author_id');

INSERT INTO sys_metadata_field (tenant_id, entity_id, field_code, field_name, column_name, data_type, query_condition_type, dict_type, sort, creator, updater)
SELECT 1, e.id, 'ops_user_id', '运营人员', 'ops_user_id', 'BIGINT', 'USER_SELECT', NULL, 45, 'v100-perf', 'v100-perf'
FROM sys_metadata_entity e
WHERE e.tenant_id = 1 AND e.physical_table = 'oa_order_attribution'
  AND NOT EXISTS (SELECT 1 FROM sys_metadata_field f WHERE f.tenant_id = 1 AND f.entity_id = e.id AND f.field_code = 'ops_user_id');

-- 9501 推文发布数：按作者(人员) + 发布时间(周期)
UPDATE oa_metric SET
    metric_formula = 'SELECT COUNT(*) AS metric_value FROM oa_content t WHERE t.tenant_id = :tenantId AND t.deleted = 0',
    data_source = 'oa_content',
    category = 'PERF',
    params_json = '{"builder":{"dataSource":"oa_content","calcMethod":"COUNT","calcField":"","groupByFields":[],"joinTables":[],"conditions":[{"field":"author_id","operator":"=","value":"","asParameter":true,"queryConditionType":"USER_SELECT","paramKey":"p_user_id"},{"field":"publish_time","operator":">=","value":"","asParameter":true,"queryConditionType":"DATE_RANGE","paramKey":"p_period"}]}}',
    updater = 'v100-perf'
WHERE id = 9501 AND tenant_id = 1;

-- 9502 营收贡献：按运营人员 + 统计日期
UPDATE oa_metric SET
    metric_formula = 'SELECT COALESCE(SUM(t.revenue), 0) AS metric_value FROM oa_order_attribution t WHERE t.tenant_id = :tenantId AND t.deleted = 0',
    data_source = 'oa_order_attribution',
    category = 'PERF',
    params_json = '{"builder":{"dataSource":"oa_order_attribution","calcMethod":"SUM","calcField":"revenue","groupByFields":[],"joinTables":[],"conditions":[{"field":"ops_user_id","operator":"=","value":"","asParameter":true,"queryConditionType":"USER_SELECT","paramKey":"p_user_id"},{"field":"stat_date","operator":">=","value":"","asParameter":true,"queryConditionType":"DATE_RANGE","paramKey":"p_period"}]}}',
    updater = 'v100-perf'
WHERE id = 9502 AND tenant_id = 1;

-- 9503 ROI：按运营人员 + 统计日期（期间平均 ROI）
UPDATE oa_metric SET
    metric_formula = 'SELECT COALESCE(AVG(t.roi), 0) AS metric_value FROM oa_order_attribution t WHERE t.tenant_id = :tenantId AND t.deleted = 0',
    data_source = 'oa_order_attribution',
    category = 'PERF',
    params_json = '{"builder":{"dataSource":"oa_order_attribution","calcMethod":"AVG","calcField":"roi","groupByFields":[],"joinTables":[],"conditions":[{"field":"ops_user_id","operator":"=","value":"","asParameter":true,"queryConditionType":"USER_SELECT","paramKey":"p_user_id"},{"field":"stat_date","operator":">=","value":"","asParameter":true,"queryConditionType":"DATE_RANGE","paramKey":"p_period"}]}}',
    updater = 'v100-perf'
WHERE id = 9503 AND tenant_id = 1;

-- 9504 任务完成数：按执行人 + 完成时间
UPDATE oa_metric SET
    metric_formula = 'SELECT COUNT(*) AS metric_value FROM oa_task t WHERE t.tenant_id = :tenantId AND t.deleted = 0 AND t.status = ''COMPLETED''',
    data_source = 'oa_task',
    category = 'PERF',
    params_json = '{"builder":{"dataSource":"oa_task","calcMethod":"COUNT","calcField":"","groupByFields":[],"joinTables":[],"conditions":[{"field":"status","operator":"=","value":"COMPLETED","asParameter":false},{"field":"assignee_id","operator":"=","value":"","asParameter":true,"queryConditionType":"USER_SELECT","paramKey":"p_user_id"},{"field":"complete_time","operator":">=","value":"","asParameter":true,"queryConditionType":"DATE_RANGE","paramKey":"p_period"}]}}',
    updater = 'v100-perf'
WHERE id = 9504 AND tenant_id = 1;

-- 9505 粉丝净增：按统计日期（人员维度待 ADR 明确账号-人员映射后补 USER_SELECT）
UPDATE oa_metric SET
    metric_formula = 'SELECT COALESCE(SUM(t.net_growth), 0) AS metric_value FROM oa_follower_daily t WHERE t.tenant_id = :tenantId AND t.deleted = 0',
    data_source = 'oa_follower_daily',
    category = 'PERF',
    params_json = '{"builder":{"dataSource":"oa_follower_daily","calcMethod":"SUM","calcField":"net_growth","groupByFields":[],"joinTables":[],"conditions":[{"field":"stat_date","operator":">=","value":"","asParameter":true,"queryConditionType":"DATE_RANGE","paramKey":"p_period"}]}}',
    updater = 'v100-perf'
WHERE id = 9505 AND tenant_id = 1;
