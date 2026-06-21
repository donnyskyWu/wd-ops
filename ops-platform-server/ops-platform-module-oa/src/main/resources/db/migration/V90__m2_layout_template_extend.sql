-- S-16: extend layout template with tags and default_params

ALTER TABLE oa_wechat_layout_template
    ADD COLUMN tags VARCHAR(200) NULL COMMENT 'comma-separated tags';

ALTER TABLE oa_wechat_layout_template
    ADD COLUMN default_params JSON NULL COMMENT 'default layout param overrides';
