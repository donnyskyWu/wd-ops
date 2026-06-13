-- Req A: optional multi platform / multi publish account on production content

ALTER TABLE oa_production_content
    ADD COLUMN platform_types_json JSON NULL COMMENT 'multi dict_platform_type values' AFTER platform_type,
    ADD COLUMN account_ids_json JSON NULL COMMENT 'multi oa_account ids' AFTER account_id;

UPDATE oa_production_content
SET platform_types_json = JSON_ARRAY(platform_type)
WHERE platform_type IS NOT NULL AND platform_types_json IS NULL;

UPDATE oa_production_content
SET account_ids_json = JSON_ARRAY(account_id)
WHERE account_id IS NOT NULL AND account_ids_json IS NULL;
