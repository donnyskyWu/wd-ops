-- V51: 软删除 V43 遗留的无 account_id 内部采集配置（与 V50 按账号关联种子重复）

UPDATE oa_collect_config
SET deleted = 1, updater = 'seed-m8', update_time = CURRENT_TIMESTAMP
WHERE tenant_id = 1
  AND scope = 'INTERNAL'
  AND account_id IS NULL
  AND deleted = 0
  AND sub_type IN ('platform', 'ACCOUNT_METRICS', 'CONTENT_METRICS', 'LIVE_METRICS');
