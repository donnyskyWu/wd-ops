-- M10 P2: 抖音粉丝/作品采集快照 + dict_collect_data_type 扩展（Channel-A · douyin MVP）

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_collect_data_type', '抖音粉丝列表', 'DOUYIN_FOLLOWER_LIST', 4, 'ENABLED'),
('dict_collect_data_type', '抖音作品列表', 'DOUYIN_VIDEO_LIST', 5, 'ENABLED'),
('dict_collect_data_type', '抖音作品明细', 'DOUYIN_VIDEO_STATS', 6, 'ENABLED')
ON DUPLICATE KEY UPDATE label = VALUES(label), sort = VALUES(sort);

CREATE TABLE IF NOT EXISTS oa_douyin_follower (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    account_id      BIGINT       NOT NULL,
    follower_id     VARCHAR(128) NOT NULL COMMENT '抖音 sec_uid',
    nickname        VARCHAR(200) NULL,
    avatar          VARCHAR(512) NULL,
    followed_at     TIMESTAMP    NULL,
    synced_at       TIMESTAMP    NULL,
    creator         VARCHAR(64)  DEFAULT 'system',
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         VARCHAR(64)  DEFAULT 'system',
    update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT     NOT NULL DEFAULT 0,
    UNIQUE KEY uk_oa_douyin_follower (tenant_id, account_id, follower_id),
    KEY idx_oa_douyin_follower_account (tenant_id, account_id)
);

CREATE TABLE IF NOT EXISTS oa_douyin_video (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    account_id      BIGINT       NOT NULL,
    video_id        VARCHAR(64)  NOT NULL,
    title           VARCHAR(500) NULL,
    description     VARCHAR(2000) NULL,
    video_url       VARCHAR(1024) NULL,
    cover_url       VARCHAR(1024) NULL,
    duration        INT          NULL,
    published_at    TIMESTAMP    NULL,
    play_count      INT          NULL,
    like_count      INT          NULL,
    share_count     INT          NULL,
    comment_count   INT          NULL,
    collect_count   INT          NULL,
    synced_at       TIMESTAMP    NULL,
    stats_synced_at TIMESTAMP    NULL,
    creator         VARCHAR(64)  DEFAULT 'system',
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         VARCHAR(64)  DEFAULT 'system',
    update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT     NOT NULL DEFAULT 0,
    UNIQUE KEY uk_oa_douyin_video (tenant_id, account_id, video_id),
    KEY idx_oa_douyin_video_account (tenant_id, account_id)
);
