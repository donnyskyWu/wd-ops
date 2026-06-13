-- V70 made platform/account multi-select optional; relax legacy NOT NULL columns
ALTER TABLE oa_production_content
    MODIFY COLUMN account_id BIGINT NULL COMMENT 'legacy single account; optional when account_ids_json used',
    MODIFY COLUMN platform_type VARCHAR(32) NULL COMMENT 'legacy single platform; optional when platform_types_json used';
