-- M4 FR-149/150: 实名人身份证截图 + 公司营业执照（多图）

ALTER TABLE oa_realname ADD COLUMN id_card_front_key VARCHAR(512) NULL;
ALTER TABLE oa_realname ADD COLUMN id_card_back_key VARCHAR(512) NULL;

ALTER TABLE oa_company ADD COLUMN business_license_keys TEXT NULL;
