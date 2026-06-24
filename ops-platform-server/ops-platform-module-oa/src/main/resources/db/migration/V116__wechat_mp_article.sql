-- M10 P2: 公众号图文快照 + 采集任务 data_type 扩展（Channel-A · article-list MVP）

INSERT INTO sys_dict_type (type, name, status) VALUES
('dict_collect_data_type', '采集数据类型', 'ENABLED')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_collect_data_type', '公众号粉丝', 'MP_FOLLOWER_LIST', 1, 'ENABLED'),
('dict_collect_data_type', '公众号图文', 'MP_ARTICLE_LIST', 2, 'ENABLED')
ON DUPLICATE KEY UPDATE label = VALUES(label), sort = VALUES(sort);

ALTER TABLE oa_collect_task
    ADD COLUMN data_type VARCHAR(32) NULL COMMENT '采集数据类型 dict_collect_data_type';

CREATE TABLE IF NOT EXISTS oa_wechat_mp_article (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    account_id      BIGINT       NOT NULL,
    article_id      VARCHAR(64)  NOT NULL,
    title           VARCHAR(500) NULL,
    url             VARCHAR(1024) NULL,
    cover_url       VARCHAR(1024) NULL,
    published_at    TIMESTAMP    NULL,
    read_count      INT          NULL,
    like_count      INT          NULL,
    share_count     INT          NULL,
    synced_at       TIMESTAMP    NULL,
    creator         VARCHAR(64)  DEFAULT 'system',
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         VARCHAR(64)  DEFAULT 'system',
    update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT     NOT NULL DEFAULT 0,
    UNIQUE KEY uk_oa_wx_mp_article (tenant_id, account_id, article_id),
    KEY idx_oa_wx_mp_article_account (tenant_id, account_id)
);
