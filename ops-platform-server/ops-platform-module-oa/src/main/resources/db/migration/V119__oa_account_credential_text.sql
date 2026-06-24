-- M4/M10: 公众号 Cookie/Token 经 AES-256 Base64 后常超 VARCHAR(512)，扩展为 TEXT

ALTER TABLE oa_account
    MODIFY COLUMN cookie_encrypted TEXT NULL COMMENT 'Cookie AES-256';

ALTER TABLE oa_account
    MODIFY COLUMN mp_token_encrypted TEXT NULL COMMENT '公众号后台 Token AES-256';

ALTER TABLE oa_account
    MODIFY COLUMN auth_token_encrypted TEXT NULL COMMENT '平台专用 Token AES-256';
