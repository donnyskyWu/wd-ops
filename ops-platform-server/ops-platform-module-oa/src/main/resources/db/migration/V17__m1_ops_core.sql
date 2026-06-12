-- M1 S-03~S-10: 作者 / 运营主播 / 粉丝日表 / 作品 / 补录

CREATE TABLE IF NOT EXISTS oa_author (
    id                  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id           BIGINT       NOT NULL,
    author_name         VARCHAR(64)  NOT NULL,
    ip_group_id         BIGINT       NOT NULL,
    author_type         VARCHAR(32)  NULL,
    primary_account_id  BIGINT       NULL,
    user_id             BIGINT       NULL,
    status              TINYINT      NOT NULL DEFAULT 1,
    remark              VARCHAR(200) NULL,
    creator             VARCHAR(64)  DEFAULT 'system',
    create_time         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater             VARCHAR(64)  DEFAULT 'system',
    update_time         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             SMALLINT     NOT NULL DEFAULT 0,
    KEY idx_oa_author_tenant (tenant_id),
    KEY idx_oa_author_ip_group (tenant_id, ip_group_id)
);

CREATE TABLE IF NOT EXISTS oa_ops_anchor_rel (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    ops_user_id     BIGINT       NOT NULL,
    anchor_user_id  BIGINT       NOT NULL,
    ip_group_id     BIGINT       NULL,
    start_date      DATE         NOT NULL,
    end_date        DATE         NOT NULL,
    creator         VARCHAR(64)  DEFAULT 'system',
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         VARCHAR(64)  DEFAULT 'system',
    update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT     NOT NULL DEFAULT 0,
    KEY idx_oa_ops_anchor_ops (tenant_id, ops_user_id),
    KEY idx_oa_ops_anchor_anchor (tenant_id, anchor_user_id)
);

CREATE TABLE IF NOT EXISTS oa_follower_daily (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    account_id      BIGINT       NOT NULL,
    stat_date       DATE         NOT NULL,
    follower_count  BIGINT       NOT NULL DEFAULT 0,
    new_follower    INT          NOT NULL DEFAULT 0,
    unfollow_count  INT          NOT NULL DEFAULT 0,
    net_growth      INT          NOT NULL DEFAULT 0,
    growth_rate     DECIMAL(10,4) NULL,
    creator         VARCHAR(64)  DEFAULT 'system',
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT     NOT NULL DEFAULT 0,
    UNIQUE KEY uk_oa_follower_daily (tenant_id, account_id, stat_date, deleted),
    KEY idx_oa_follower_daily_date (tenant_id, stat_date)
);

CREATE TABLE IF NOT EXISTS oa_content (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    account_id      BIGINT       NOT NULL,
    title           VARCHAR(200) NOT NULL,
    platform_type     VARCHAR(32)  NOT NULL,
    content_type    VARCHAR(32)  NULL,
    publish_time    TIMESTAMP    NULL,
    read_count      BIGINT       NOT NULL DEFAULT 0,
    like_count      INT          NOT NULL DEFAULT 0,
    comment_count   INT          NOT NULL DEFAULT 0,
    forward_count   INT          NOT NULL DEFAULT 0,
    is_hit          TINYINT      NOT NULL DEFAULT 0,
    data_source     VARCHAR(16)  NOT NULL DEFAULT 'API',
    status          VARCHAR(32)  NOT NULL DEFAULT 'PUBLISHED',
    creator         VARCHAR(64)  DEFAULT 'system',
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         VARCHAR(64)  DEFAULT 'system',
    update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT     NOT NULL DEFAULT 0,
    KEY idx_oa_content_account (tenant_id, account_id),
    KEY idx_oa_content_publish (tenant_id, publish_time)
);

CREATE TABLE IF NOT EXISTS oa_content_data_import (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    content_id      BIGINT       NOT NULL,
    stat_date       DATE         NOT NULL,
    import_type     VARCHAR(32)  NOT NULL,
    read_count      BIGINT       NULL,
    like_count      INT          NULL,
    comment_count   INT          NULL,
    forward_count   INT          NULL,
    follower_change INT          NULL,
    review_status   TINYINT      NOT NULL DEFAULT 0 COMMENT '0待审 1通过 2驳回',
    remark          VARCHAR(500) NULL,
    reviewer_id     BIGINT       NULL,
    review_time     TIMESTAMP    NULL,
    submitter_id    BIGINT       NULL,
    creator         VARCHAR(64)  DEFAULT 'system',
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         VARCHAR(64)  DEFAULT 'system',
    update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT     NOT NULL DEFAULT 0,
    KEY idx_oa_content_import_content (tenant_id, content_id),
    KEY idx_oa_content_import_status (tenant_id, review_status)
);

INSERT INTO sys_dict_type (type, name, status) VALUES
('dict_author_type', '作者类型', 'ENABLED'),
('dict_content_import_type', '补录类型', 'ENABLED'),
('dict_data_source', '数据来源', 'ENABLED');

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_author_type', '短视频', 'SHORT_VIDEO', 1, 'ENABLED'),
('dict_author_type', '图文', 'ARTICLE', 2, 'ENABLED'),
('dict_content_import_type', '接口异常', 'API_EXCEPTION', 1, 'ENABLED'),
('dict_content_import_type', '线下补录', 'OFFLINE', 2, 'ENABLED'),
('dict_data_source', 'API采集', 'API', 1, 'ENABLED'),
('dict_data_source', '手工补录', 'IMPORT', 2, 'ENABLED');
