-- ADR-046 / M6: seed all metricSchema tables as metadata entities (tenant_id=1)
-- entity_code matches metricSchema keys (= physical_table name)

-- Align V96 seed: entity_code content -> oa_content
UPDATE sys_metadata_entity
SET entity_code = 'oa_content', entity_name = '内容表', updater = 'v98-seed', update_time = CURRENT_TIMESTAMP
WHERE tenant_id = 1 AND entity_code = 'content' AND physical_table = 'oa_content';

-- Entities (idempotent)
INSERT INTO sys_metadata_entity (tenant_id, entity_code, entity_name, physical_table, status, remark, creator, updater)
SELECT 1, 'oa_content', '内容表', 'oa_content', 'ENABLED', 'M6 metricSchema seed', 'v98-seed', 'v98-seed'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_metadata_entity WHERE tenant_id = 1 AND physical_table = 'oa_content')
UNION ALL
SELECT 1, 'oa_content_daily', '内容日统计', 'oa_content_daily', 'ENABLED', 'M6 metricSchema seed', 'v98-seed', 'v98-seed'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_metadata_entity WHERE tenant_id = 1 AND physical_table = 'oa_content_daily')
UNION ALL
SELECT 1, 'oa_account', '账号表', 'oa_account', 'ENABLED', 'M6 metricSchema seed', 'v98-seed', 'v98-seed'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_metadata_entity WHERE tenant_id = 1 AND physical_table = 'oa_account')
UNION ALL
SELECT 1, 'oa_follower_daily', '粉丝日统计', 'oa_follower_daily', 'ENABLED', 'M6 metricSchema seed', 'v98-seed', 'v98-seed'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_metadata_entity WHERE tenant_id = 1 AND physical_table = 'oa_follower_daily')
UNION ALL
SELECT 1, 'oa_order', '订单表', 'oa_order', 'ENABLED', 'M6 metricSchema seed', 'v98-seed', 'v98-seed'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_metadata_entity WHERE tenant_id = 1 AND physical_table = 'oa_order')
UNION ALL
SELECT 1, 'oa_order_attribution', '订单归因', 'oa_order_attribution', 'ENABLED', 'M6 metricSchema seed', 'v98-seed', 'v98-seed'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_metadata_entity WHERE tenant_id = 1 AND physical_table = 'oa_order_attribution')
UNION ALL
SELECT 1, 'oa_account_cost', '账号成本', 'oa_account_cost', 'ENABLED', 'M6 metricSchema seed', 'v98-seed', 'v98-seed'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_metadata_entity WHERE tenant_id = 1 AND physical_table = 'oa_account_cost');

-- oa_content fields
INSERT INTO sys_metadata_field (tenant_id, entity_id, field_code, field_name, column_name, data_type, query_condition_type, dict_type, sort, creator, updater)
SELECT 1, e.id, v.field_code, v.field_name, v.column_name, v.data_type, v.query_condition_type, v.dict_type, v.sort, 'v98-seed', 'v98-seed'
FROM sys_metadata_entity e
CROSS JOIN (
    SELECT 'id' AS field_code, 'ID' AS field_name, 'id' AS column_name, 'BIGINT' AS data_type, 'NUMBER' AS query_condition_type, NULL AS dict_type, 1 AS sort UNION ALL
    SELECT 'account_id', '账号ID', 'account_id', 'BIGINT', 'ACCOUNT_SELECT', NULL, 10 UNION ALL
    SELECT 'title', '标题', 'title', 'VARCHAR', 'TEXT', NULL, 20 UNION ALL
    SELECT 'platform_type', '平台', 'platform_type', 'VARCHAR', 'PLATFORM_SELECT', 'dict_platform_type', 30 UNION ALL
    SELECT 'content_type', '内容类型', 'content_type', 'VARCHAR', 'DICT', 'dict_content_type', 40 UNION ALL
    SELECT 'publish_time', '发布时间', 'publish_time', 'DATETIME', 'DATE_RANGE', NULL, 50 UNION ALL
    SELECT 'read_count', '阅读数', 'read_count', 'BIGINT', 'NUMBER', NULL, 60 UNION ALL
    SELECT 'like_count', '点赞数', 'like_count', 'BIGINT', 'NUMBER', NULL, 70 UNION ALL
    SELECT 'comment_count', '评论数', 'comment_count', 'BIGINT', 'NUMBER', NULL, 80 UNION ALL
    SELECT 'forward_count', '转发数', 'forward_count', 'BIGINT', 'NUMBER', NULL, 90 UNION ALL
    SELECT 'is_hit', '是否爆款', 'is_hit', 'TINYINT', 'NUMBER', NULL, 100 UNION ALL
    SELECT 'status', '状态', 'status', 'VARCHAR', 'DICT', NULL, 110
) v
WHERE e.tenant_id = 1 AND e.physical_table = 'oa_content'
  AND NOT EXISTS (SELECT 1 FROM sys_metadata_field f WHERE f.tenant_id = 1 AND f.entity_id = e.id AND f.field_code = v.field_code);

-- oa_content_daily fields
INSERT INTO sys_metadata_field (tenant_id, entity_id, field_code, field_name, column_name, data_type, query_condition_type, dict_type, sort, creator, updater)
SELECT 1, e.id, v.field_code, v.field_name, v.column_name, v.data_type, v.query_condition_type, v.dict_type, v.sort, 'v98-seed', 'v98-seed'
FROM sys_metadata_entity e
CROSS JOIN (
    SELECT 'id' AS field_code, 'ID' AS field_name, 'id' AS column_name, 'BIGINT' AS data_type, 'NUMBER' AS query_condition_type, NULL AS dict_type, 1 AS sort UNION ALL
    SELECT 'content_id', '内容ID', 'content_id', 'BIGINT', 'NUMBER', NULL, 10 UNION ALL
    SELECT 'stat_date', '统计日期', 'stat_date', 'DATE', 'DATE', NULL, 20 UNION ALL
    SELECT 'read_count', '阅读数', 'read_count', 'BIGINT', 'NUMBER', NULL, 30 UNION ALL
    SELECT 'play_count', '播放数', 'play_count', 'BIGINT', 'NUMBER', NULL, 40
) v
WHERE e.tenant_id = 1 AND e.physical_table = 'oa_content_daily'
  AND NOT EXISTS (SELECT 1 FROM sys_metadata_field f WHERE f.tenant_id = 1 AND f.entity_id = e.id AND f.field_code = v.field_code);

-- oa_account fields
INSERT INTO sys_metadata_field (tenant_id, entity_id, field_code, field_name, column_name, data_type, query_condition_type, dict_type, sort, creator, updater)
SELECT 1, e.id, v.field_code, v.field_name, v.column_name, v.data_type, v.query_condition_type, v.dict_type, v.sort, 'v98-seed', 'v98-seed'
FROM sys_metadata_entity e
CROSS JOIN (
    SELECT 'id' AS field_code, 'ID' AS field_name, 'id' AS column_name, 'BIGINT' AS data_type, 'NUMBER' AS query_condition_type, NULL AS dict_type, 1 AS sort UNION ALL
    SELECT 'platform_type', '平台', 'platform_type', 'VARCHAR', 'PLATFORM_SELECT', 'dict_platform_type', 10 UNION ALL
    SELECT 'account_name', '账号名称', 'account_name', 'VARCHAR', 'TEXT', NULL, 20 UNION ALL
    SELECT 'external_account_id', '外部账号ID', 'external_account_id', 'VARCHAR', 'TEXT', NULL, 30 UNION ALL
    SELECT 'status', '状态', 'status', 'VARCHAR', 'DICT', NULL, 40
) v
WHERE e.tenant_id = 1 AND e.physical_table = 'oa_account'
  AND NOT EXISTS (SELECT 1 FROM sys_metadata_field f WHERE f.tenant_id = 1 AND f.entity_id = e.id AND f.field_code = v.field_code);

-- oa_follower_daily fields
INSERT INTO sys_metadata_field (tenant_id, entity_id, field_code, field_name, column_name, data_type, query_condition_type, dict_type, sort, creator, updater)
SELECT 1, e.id, v.field_code, v.field_name, v.column_name, v.data_type, v.query_condition_type, v.dict_type, v.sort, 'v98-seed', 'v98-seed'
FROM sys_metadata_entity e
CROSS JOIN (
    SELECT 'id' AS field_code, 'ID' AS field_name, 'id' AS column_name, 'BIGINT' AS data_type, 'NUMBER' AS query_condition_type, NULL AS dict_type, 1 AS sort UNION ALL
    SELECT 'account_id', '账号ID', 'account_id', 'BIGINT', 'ACCOUNT_SELECT', NULL, 10 UNION ALL
    SELECT 'stat_date', '统计日期', 'stat_date', 'DATE', 'DATE', NULL, 20 UNION ALL
    SELECT 'follower_count', '粉丝数', 'follower_count', 'BIGINT', 'NUMBER', NULL, 30 UNION ALL
    SELECT 'new_follower', '新增粉丝', 'new_follower', 'BIGINT', 'NUMBER', NULL, 40 UNION ALL
    SELECT 'unfollow_count', '取关数', 'unfollow_count', 'BIGINT', 'NUMBER', NULL, 50 UNION ALL
    SELECT 'net_growth', '净增', 'net_growth', 'BIGINT', 'NUMBER', NULL, 60 UNION ALL
    SELECT 'growth_rate', '增长率', 'growth_rate', 'DECIMAL', 'NUMBER', NULL, 70
) v
WHERE e.tenant_id = 1 AND e.physical_table = 'oa_follower_daily'
  AND NOT EXISTS (SELECT 1 FROM sys_metadata_field f WHERE f.tenant_id = 1 AND f.entity_id = e.id AND f.field_code = v.field_code);

-- oa_order fields
INSERT INTO sys_metadata_field (tenant_id, entity_id, field_code, field_name, column_name, data_type, query_condition_type, dict_type, sort, creator, updater)
SELECT 1, e.id, v.field_code, v.field_name, v.column_name, v.data_type, v.query_condition_type, v.dict_type, v.sort, 'v98-seed', 'v98-seed'
FROM sys_metadata_entity e
CROSS JOIN (
    SELECT 'id' AS field_code, 'ID' AS field_name, 'id' AS column_name, 'BIGINT' AS data_type, 'NUMBER' AS query_condition_type, NULL AS dict_type, 1 AS sort UNION ALL
    SELECT 'order_no', '订单号', 'order_no', 'VARCHAR', 'TEXT', NULL, 10 UNION ALL
    SELECT 'order_amount', '订单金额', 'order_amount', 'DECIMAL', 'NUMBER', NULL, 20 UNION ALL
    SELECT 'order_time', '下单时间', 'order_time', 'DATETIME', 'DATE_RANGE', NULL, 30 UNION ALL
    SELECT 'account_id', '账号ID', 'account_id', 'BIGINT', 'ACCOUNT_SELECT', NULL, 40 UNION ALL
    SELECT 'ip_group_id', 'IP组ID', 'ip_group_id', 'BIGINT', 'IP_GROUP_SELECT', NULL, 50
) v
WHERE e.tenant_id = 1 AND e.physical_table = 'oa_order'
  AND NOT EXISTS (SELECT 1 FROM sys_metadata_field f WHERE f.tenant_id = 1 AND f.entity_id = e.id AND f.field_code = v.field_code);

-- oa_order_attribution fields
INSERT INTO sys_metadata_field (tenant_id, entity_id, field_code, field_name, column_name, data_type, query_condition_type, dict_type, sort, creator, updater)
SELECT 1, e.id, v.field_code, v.field_name, v.column_name, v.data_type, v.query_condition_type, v.dict_type, v.sort, 'v98-seed', 'v98-seed'
FROM sys_metadata_entity e
CROSS JOIN (
    SELECT 'id' AS field_code, 'ID' AS field_name, 'id' AS column_name, 'BIGINT' AS data_type, 'NUMBER' AS query_condition_type, NULL AS dict_type, 1 AS sort UNION ALL
    SELECT 'order_id', '订单ID', 'order_id', 'BIGINT', 'NUMBER', NULL, 10 UNION ALL
    SELECT 'account_id', '账号ID', 'account_id', 'BIGINT', 'ACCOUNT_SELECT', NULL, 20 UNION ALL
    SELECT 'ip_group_id', 'IP组ID', 'ip_group_id', 'BIGINT', 'IP_GROUP_SELECT', NULL, 30 UNION ALL
    SELECT 'author_id', '作者ID', 'author_id', 'BIGINT', 'USER_SELECT', NULL, 40 UNION ALL
    SELECT 'revenue', '收入', 'revenue', 'DECIMAL', 'NUMBER', NULL, 50 UNION ALL
    SELECT 'cost', '成本', 'cost', 'DECIMAL', 'NUMBER', NULL, 60 UNION ALL
    SELECT 'roi', 'ROI', 'roi', 'DECIMAL', 'NUMBER', NULL, 70 UNION ALL
    SELECT 'stat_date', '统计日期', 'stat_date', 'DATE', 'DATE', NULL, 80
) v
WHERE e.tenant_id = 1 AND e.physical_table = 'oa_order_attribution'
  AND NOT EXISTS (SELECT 1 FROM sys_metadata_field f WHERE f.tenant_id = 1 AND f.entity_id = e.id AND f.field_code = v.field_code);

-- oa_account_cost fields
INSERT INTO sys_metadata_field (tenant_id, entity_id, field_code, field_name, column_name, data_type, query_condition_type, dict_type, sort, creator, updater)
SELECT 1, e.id, v.field_code, v.field_name, v.column_name, v.data_type, v.query_condition_type, v.dict_type, v.sort, 'v98-seed', 'v98-seed'
FROM sys_metadata_entity e
CROSS JOIN (
    SELECT 'id' AS field_code, 'ID' AS field_name, 'id' AS column_name, 'BIGINT' AS data_type, 'NUMBER' AS query_condition_type, NULL AS dict_type, 1 AS sort UNION ALL
    SELECT 'account_id', '账号ID', 'account_id', 'BIGINT', 'ACCOUNT_SELECT', NULL, 10 UNION ALL
    SELECT 'cost_type', '成本类型', 'cost_type', 'VARCHAR', 'DICT', NULL, 20 UNION ALL
    SELECT 'amount', '金额', 'amount', 'DECIMAL', 'NUMBER', NULL, 30 UNION ALL
    SELECT 'pay_method', '支付方式', 'pay_method', 'VARCHAR', 'TEXT', NULL, 40 UNION ALL
    SELECT 'pay_date', '支付日期', 'pay_date', 'DATE', 'DATE', NULL, 50 UNION ALL
    SELECT 'period', '周期', 'period', 'VARCHAR', 'TEXT', NULL, 60
) v
WHERE e.tenant_id = 1 AND e.physical_table = 'oa_account_cost'
  AND NOT EXISTS (SELECT 1 FROM sys_metadata_field f WHERE f.tenant_id = 1 AND f.entity_id = e.id AND f.field_code = v.field_code);
