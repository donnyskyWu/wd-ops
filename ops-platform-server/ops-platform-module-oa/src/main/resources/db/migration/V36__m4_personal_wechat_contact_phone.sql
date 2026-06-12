-- ADR-010: 个微联系电话（明文，与 phone_id 解耦）
ALTER TABLE oa_personal_wechat_account
    ADD COLUMN contact_phone VARCHAR(20) NULL COMMENT '个微联系手机号（手动填写）' AFTER wechat_id;

UPDATE oa_personal_wechat_account
SET contact_phone = '13800138001'
WHERE id = 9001 AND tenant_id = 1 AND contact_phone IS NULL;
