-- M10 P2: 公众号/视频号/快手/小红书多数据类型采集扩展（Channel-A）

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_collect_data_type', '公众号粉丝统计', 'MP_FOLLOWER_STATS', 3, 'ENABLED'),
('dict_collect_data_type', '公众号图文明细', 'MP_ARTICLE_STATS', 4, 'ENABLED'),
('dict_collect_data_type', '公众号图文内容', 'MP_ARTICLE_CONTENT', 5, 'ENABLED'),
('dict_collect_data_type', '视频号作品列表', 'WECHAT_VIDEO_LIST', 7, 'ENABLED'),
('dict_collect_data_type', '视频号作品明细', 'WECHAT_VIDEO_STATS', 8, 'ENABLED'),
('dict_collect_data_type', '快手作品列表', 'KUAISHOU_VIDEO_LIST', 9, 'ENABLED'),
('dict_collect_data_type', '快手作品明细', 'KUAISHOU_VIDEO_STATS', 10, 'ENABLED'),
('dict_collect_data_type', '小红书笔记列表', 'XIAOHONGSHU_NOTE_LIST', 11, 'ENABLED'),
('dict_collect_data_type', '小红书笔记明细', 'XIAOHONGSHU_NOTE_STATS', 12, 'ENABLED')
ON DUPLICATE KEY UPDATE label = VALUES(label), sort = VALUES(sort);

ALTER TABLE oa_wechat_mp_article ADD COLUMN content_text TEXT NULL COMMENT '正文纯文本';
ALTER TABLE oa_wechat_mp_article ADD COLUMN stats_synced_at TIMESTAMP NULL COMMENT '互动数据同步时间';
ALTER TABLE oa_wechat_mp_article ADD COLUMN content_synced_at TIMESTAMP NULL COMMENT '正文同步时间';

CREATE TABLE IF NOT EXISTS oa_wechat_video_work (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    account_id      BIGINT       NOT NULL,
    video_id        VARCHAR(128) NOT NULL COMMENT 'export_id',
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
    UNIQUE KEY uk_oa_wechat_video_work (tenant_id, account_id, video_id),
    KEY idx_oa_wechat_video_work_account (tenant_id, account_id)
);

CREATE TABLE IF NOT EXISTS oa_kuaishou_video (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    account_id      BIGINT       NOT NULL,
    video_id        VARCHAR(64)  NOT NULL COMMENT 'photo_id',
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
    UNIQUE KEY uk_oa_kuaishou_video (tenant_id, account_id, video_id),
    KEY idx_oa_kuaishou_video_account (tenant_id, account_id)
);

CREATE TABLE IF NOT EXISTS oa_xiaohongshu_note (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    account_id      BIGINT       NOT NULL,
    note_id         VARCHAR(64)  NOT NULL,
    xsec_token      VARCHAR(256) NULL,
    title           VARCHAR(500) NULL,
    description     VARCHAR(2000) NULL,
    note_url        VARCHAR(1024) NULL,
    cover_url       VARCHAR(1024) NULL,
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
    UNIQUE KEY uk_oa_xiaohongshu_note (tenant_id, account_id, note_id),
    KEY idx_oa_xiaohongshu_note_account (tenant_id, account_id)
);
