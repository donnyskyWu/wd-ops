-- ADR-020: layout_schema v2 columns + import job extensions

ALTER TABLE oa_wechat_layout_template
    ADD COLUMN layout_schema JSON NULL COMMENT 'ADR-020 SSOT: structure + styles + slots';

ALTER TABLE oa_wechat_layout_template
    ADD COLUMN schema_version INT NOT NULL DEFAULT 1 COMMENT '1=legacy layout_json, 2=layout_schema';

ALTER TABLE oa_wechat_layout_template
    ADD COLUMN style_css TEXT NULL;

ALTER TABLE oa_wechat_layout_template
    ADD COLUMN preview_html LONGTEXT NULL;

ALTER TABLE oa_layout_import_job
    ADD COLUMN preview_layout_schema JSON NULL;

ALTER TABLE oa_layout_import_job
    ADD COLUMN extraction_report JSON NULL;

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_layout_template_source', '系统预置', 'PRESET', 5, 'ENABLED')
ON DUPLICATE KEY UPDATE label = VALUES(label);
