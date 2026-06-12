-- E2E 修复：监测爆款列表要求 is_external=1
UPDATE oa_external_work SET is_external = 1, updater = 'e2e-df'
WHERE id IN (96101, 96102) AND tenant_id = 1;
