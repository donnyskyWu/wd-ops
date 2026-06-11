-- Align internal collect seed sub_type with UI tabs (platform / wework / wechat)

UPDATE oa_collect_config
SET sub_type = 'platform', updater = 'system', update_time = CURRENT_TIMESTAMP
WHERE tenant_id = 1
  AND scope = 'INTERNAL'
  AND sub_type IN ('ACCOUNT_METRICS', 'CONTENT_METRICS', 'LIVE_METRICS');

INSERT INTO oa_collect_config
  (tenant_id, scope, config_name, sub_type, platform_type, collect_frequency, collect_method, api_url, request_method, collect_fields, status, remark)
SELECT 1, 'INTERNAL', '企微通讯录同步', 'wework', 'WEWORK', 'DAILY', 'API',
  'https://qyapi.weixin.qq.com/cgi-bin/user/list', 'GET', 'userid,name,department', 'ENABLED', '企微通讯录 API 采集'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM oa_collect_config WHERE tenant_id = 1 AND scope = 'INTERNAL' AND sub_type = 'wework' LIMIT 1);

INSERT INTO oa_collect_config
  (tenant_id, scope, config_name, sub_type, platform_type, collect_frequency, collect_method, api_url, request_method, collect_fields, status, remark)
SELECT 1, 'INTERNAL', '个微奥创消息同步', 'wechat', 'PERSONAL_WECHAT', 'HOURLY', 'API',
  'https://api.aochuang.example.com/v1/message/sync', 'POST', 'msg_id,sender,content,send_time', 'ENABLED', '个微-奥创 API 采集'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM oa_collect_config WHERE tenant_id = 1 AND scope = 'INTERNAL' AND sub_type = 'wechat' LIMIT 1);
