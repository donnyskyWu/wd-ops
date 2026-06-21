-- M10-AO-S-04: 奥创好友快照 + 同步游标（ADR-045 §1.4 · 数据流 ②）

CREATE TABLE IF NOT EXISTS oa_aochuang_friend (
    id                          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id                   BIGINT       NOT NULL,
    personal_wechat_id          BIGINT       NOT NULL,
    aochuang_wechat_account_id  VARCHAR(64)  NOT NULL,
    aochuang_friend_id          VARCHAR(64)  NOT NULL,
    wechat_id                   VARCHAR(64)  NULL,
    alias                       VARCHAR(64)  NULL,
    nickname                    VARCHAR(200) NULL,
    avatar                      VARCHAR(512) NULL,
    remark                      VARCHAR(200) NULL,
    synced_at                   TIMESTAMP    NULL,
    creator                     VARCHAR(64)  DEFAULT 'system',
    create_time                 TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater                     VARCHAR(64)  DEFAULT 'system',
    update_time                 TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                     SMALLINT     NOT NULL DEFAULT 0,
    UNIQUE KEY uk_oa_ao_friend (tenant_id, aochuang_wechat_account_id, aochuang_friend_id),
    KEY idx_oa_ao_friend_personal (tenant_id, personal_wechat_id)
);

CREATE TABLE IF NOT EXISTS oa_aochuang_sync_cursor (
    id                          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id                   BIGINT       NOT NULL,
    sync_type                   VARCHAR(32)  NOT NULL,
    aochuang_wechat_account_id  VARCHAR(64)  NOT NULL,
    personal_wechat_id          BIGINT       NOT NULL,
    cursor_value                VARCHAR(256) NULL,
    last_sync_at                TIMESTAMP    NULL,
    creator                     VARCHAR(64)  DEFAULT 'system',
    create_time                 TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater                     VARCHAR(64)  DEFAULT 'system',
    update_time                 TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                     SMALLINT     NOT NULL DEFAULT 0,
    UNIQUE KEY uk_oa_ao_sync_cursor (tenant_id, sync_type, aochuang_wechat_account_id)
);

INSERT INTO sys_dict_type (type, name, status) VALUES
('dict_aochuang_sync_type', '奥创同步类型', 'ENABLED')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_aochuang_sync_type', '好友', 'FRIENDS', 1, 'ENABLED'),
('dict_aochuang_sync_type', '消息', 'MESSAGES', 2, 'ENABLED')
ON DUPLICATE KEY UPDATE label = VALUES(label);
