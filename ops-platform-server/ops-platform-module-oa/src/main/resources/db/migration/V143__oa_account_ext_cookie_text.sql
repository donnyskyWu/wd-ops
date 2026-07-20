-- M4: 公众号扩展表 cookie_encrypted 与 oa_account 对齐（V119），AES-256 Base64 后常超 VARCHAR(512)

ALTER TABLE oa_account_ext
    MODIFY COLUMN cookie_encrypted TEXT NULL COMMENT 'Cookie AES-256';
