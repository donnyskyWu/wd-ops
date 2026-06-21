-- M10-AO-S-06: 个微采集状态默认值（复用 dict_collect_status）

UPDATE oa_personal_wechat_account
SET collect_status = 'PENDING'
WHERE collect_status IS NULL;
