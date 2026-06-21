-- M10-AO-S-05: 奥创消息明细 + 个微日统计（ADR-045 §1.4 · 数据流 ③）

CREATE TABLE IF NOT EXISTS oa_aochuang_message (
    id                          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id                   BIGINT       NOT NULL,
    personal_wechat_id          BIGINT       NOT NULL,
    aochuang_wechat_account_id  VARCHAR(64)  NOT NULL,
    aochuang_message_id         VARCHAR(64)  NOT NULL,
    aochuang_friend_id          VARCHAR(64)  NULL,
    msg_type                    VARCHAR(32)  NULL,
    direction                   VARCHAR(16)  NOT NULL,
    content                     TEXT         NULL,
    message_time                TIMESTAMP    NOT NULL,
    synced_at                   TIMESTAMP    NULL,
    creator                     VARCHAR(64)  DEFAULT 'system',
    create_time                 TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater                     VARCHAR(64)  DEFAULT 'system',
    update_time                 TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                     SMALLINT     NOT NULL DEFAULT 0,
    UNIQUE KEY uk_oa_ao_message (tenant_id, aochuang_wechat_account_id, aochuang_message_id),
    KEY idx_oa_ao_message_personal (tenant_id, personal_wechat_id),
    KEY idx_oa_ao_message_time (tenant_id, personal_wechat_id, message_time)
);

CREATE TABLE IF NOT EXISTS oa_personal_wechat_daily_stats (
    id                          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id                   BIGINT       NOT NULL,
    personal_wechat_id          BIGINT       NOT NULL,
    stat_date                   DATE         NOT NULL,
    total_friends               INT          NULL,
    new_friends                 INT          NOT NULL DEFAULT 0,
    deleted_friends             INT          NOT NULL DEFAULT 0,
    messages_sent               INT          NOT NULL DEFAULT 0,
    messages_received           INT          NOT NULL DEFAULT 0,
    group_count                 INT          NOT NULL DEFAULT 0,
    creator                     VARCHAR(64)  DEFAULT 'system',
    create_time                 TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater                     VARCHAR(64)  DEFAULT 'system',
    update_time                 TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                     SMALLINT     NOT NULL DEFAULT 0,
    UNIQUE KEY uk_oa_pwa_daily_stats (tenant_id, personal_wechat_id, stat_date)
);

INSERT INTO sys_dict_type (type, name, status) VALUES
('dict_aochuang_message_direction', '奥创消息方向', 'ENABLED'),
('dict_aochuang_message_type', '奥创消息类型', 'ENABLED')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_aochuang_message_direction', '发送', 'SENT', 1, 'ENABLED'),
('dict_aochuang_message_direction', '接收', 'RECEIVED', 2, 'ENABLED'),
('dict_aochuang_message_type', '文本', 'TEXT', 1, 'ENABLED'),
('dict_aochuang_message_type', '图片', 'IMAGE', 2, 'ENABLED'),
('dict_aochuang_message_type', '语音', 'VOICE', 3, 'ENABLED'),
('dict_aochuang_message_type', '视频', 'VIDEO', 4, 'ENABLED'),
('dict_aochuang_message_type', '其他', 'OTHER', 5, 'ENABLED')
ON DUPLICATE KEY UPDATE label = VALUES(label);
