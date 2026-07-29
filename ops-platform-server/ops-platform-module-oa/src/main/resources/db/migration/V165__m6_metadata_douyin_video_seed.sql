-- M6 E2E: seed oa_douyin_video metadata (Douyin collector target table)
-- entity_code matches physical_table name (metricSchema / M8 convention)

INSERT INTO sys_metadata_entity (tenant_id, entity_code, entity_name, physical_table, status, remark, creator, updater)
SELECT 1, 'oa_douyin_video', '抖音视频表', 'oa_douyin_video', 'ENABLED', 'M6 E2E douyin seed', 'v165-seed', 'v165-seed'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_metadata_entity WHERE tenant_id = 1 AND physical_table = 'oa_douyin_video');

INSERT INTO sys_metadata_field (tenant_id, entity_id, field_code, field_name, column_name, data_type, query_condition_type, dict_type, sort, creator, updater)
SELECT 1, e.id, v.field_code, v.field_name, v.column_name, v.data_type, v.query_condition_type, v.dict_type, v.sort, 'v165-seed', 'v165-seed'
FROM sys_metadata_entity e
CROSS JOIN (
    SELECT 'id' AS field_code, 'ID' AS field_name, 'id' AS column_name, 'BIGINT' AS data_type, 'NUMBER' AS query_condition_type, NULL AS dict_type, 1 AS sort UNION ALL
    SELECT 'account_id', '账号ID', 'account_id', 'BIGINT', 'ACCOUNT_SELECT', NULL, 10 UNION ALL
    SELECT 'title', '标题', 'title', 'VARCHAR', 'TEXT', NULL, 20 UNION ALL
    SELECT 'play_count', '播放数', 'play_count', 'BIGINT', 'NUMBER', NULL, 30 UNION ALL
    SELECT 'like_count', '点赞数', 'like_count', 'BIGINT', 'NUMBER', NULL, 40 UNION ALL
    SELECT 'published_at', '发布时间', 'published_at', 'DATETIME', 'DATE_RANGE', NULL, 50
) v
WHERE e.tenant_id = 1 AND e.physical_table = 'oa_douyin_video'
  AND NOT EXISTS (SELECT 1 FROM sys_metadata_field f WHERE f.tenant_id = 1 AND f.entity_id = e.id AND f.field_code = v.field_code);
