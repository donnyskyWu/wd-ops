-- M10-API-S-05: 微信公众号粉丝快照（Channel-A · follower-list MVP）

CREATE TABLE IF NOT EXISTS oa_wechat_mp_follower (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    account_id      BIGINT       NOT NULL,
    openid          VARCHAR(64)  NOT NULL,
    nickname        VARCHAR(200) NULL,
    avatar          VARCHAR(512) NULL,
    unionid         VARCHAR(64)  NULL,
    subscribed_at   TIMESTAMP    NULL,
    synced_at       TIMESTAMP    NULL,
    creator         VARCHAR(64)  DEFAULT 'system',
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         VARCHAR(64)  DEFAULT 'system',
    update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT     NOT NULL DEFAULT 0,
    UNIQUE KEY uk_oa_wx_mp_follower (tenant_id, account_id, openid),
    KEY idx_oa_wx_mp_follower_account (tenant_id, account_id)
);
