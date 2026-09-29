-- V206: M4 抖音/快手 Excel 导入 — 双状态 + 运营人（ADR-078；与 V205 holder_user_id「持有人」并存）
-- password_encrypted 已存在（V86 公众号）；本迁移仅增抖音/快手展示与导入字段。
-- Excel「持有人」→ holder_user_id（V205）；Excel「运营人」→ operator_user_id（API operatorUserId）；实名人 realname_id 并行。

ALTER TABLE oa_account
    ADD COLUMN short_video_status VARCHAR(128) NULL COMMENT '短视频状态（Excel 原文，抖音/快手）' AFTER password_encrypted;

ALTER TABLE oa_account
    ADD COLUMN live_status VARCHAR(128) NULL COMMENT '直播状态（Excel 原文，抖音/快手）' AFTER short_video_status;

ALTER TABLE oa_account
    ADD COLUMN operator_user_id BIGINT NULL COMMENT '运营人 Football system_users.id（Excel 运营人）' AFTER ip_group_id;

CREATE INDEX idx_oa_account_operator_user ON oa_account (tenant_id, operator_user_id);
