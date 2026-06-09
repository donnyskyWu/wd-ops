-- M4 S-09: 三方关联

CREATE TABLE IF NOT EXISTS oa_account_wechat_video_wework_rel (
    id                  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id           BIGINT       NOT NULL,
    wechat_account_id   BIGINT       NULL,
    video_account_id    BIGINT       NULL,
    wework_account_id   BIGINT       NULL,
    relation_type       VARCHAR(32)  NOT NULL,
    bind_time           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status              TINYINT      NOT NULL DEFAULT 1,
    creator             VARCHAR(64)  DEFAULT 'system',
    create_time         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater             VARCHAR(64)  DEFAULT 'system',
    update_time         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             SMALLINT     NOT NULL DEFAULT 0,
    KEY idx_oa_triple_tenant (tenant_id),
    KEY idx_oa_triple_wechat (tenant_id, wechat_account_id),
    KEY idx_oa_triple_video (tenant_id, video_account_id),
    KEY idx_oa_triple_wework (tenant_id, wework_account_id)
);

INSERT INTO sys_dict_type (type, name, status) VALUES
('dict_triple_rel_type', '三方关联类型', 'ENABLED');

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_triple_rel_type', '完整三方', 'FULL_TRIPLE', 1, 'ENABLED'),
('dict_triple_rel_type', '微信+视频', 'WECHAT_VIDEO', 2, 'ENABLED'),
('dict_triple_rel_type', '微信+企微', 'WECHAT_WEWORK', 3, 'ENABLED'),
('dict_triple_rel_type', '视频+企微', 'VIDEO_WEWORK', 4, 'ENABLED');
