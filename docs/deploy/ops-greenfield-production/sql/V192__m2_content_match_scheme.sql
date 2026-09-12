-- S-20 / ADR-076: OPS 内容生产 matchScheme + matchType SSOT
ALTER TABLE oa_production_content
    ADD COLUMN match_scheme_json JSON NULL COMMENT 'Football matchScheme SSOT（ADR-076）' AFTER competition_name,
    ADD COLUMN match_type TINYINT NULL COMMENT '1竞足2传足3北单4足球5临场' AFTER match_scheme_json,
    ADD COLUMN competition_ids_json JSON NULL COMMENT 'N场scheduleId快照' AFTER match_type;
