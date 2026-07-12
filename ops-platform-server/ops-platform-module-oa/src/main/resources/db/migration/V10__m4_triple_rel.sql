-- M4 S-09: 三方关联

CREATE TABLE IF NOT EXISTS oa_account_wechat_video_wework_rel (
    id                  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '关联ID',
    tenant_id           BIGINT       NOT NULL COMMENT '租户ID',
    wechat_account_id   BIGINT       NULL COMMENT '微信账号ID',
    video_account_id    BIGINT       NULL COMMENT '视频号账号ID',
    wework_account_id   BIGINT       NULL COMMENT '企业微信账号ID',
    relation_type       VARCHAR(32)  NOT NULL COMMENT '关联类型: FULL_TRIPLE-完整三方, WECHAT_VIDEO-微信+视频, WECHAT_WEWORK-微信+企微, VIDEO_WEWORK-视频+企微',
    bind_time           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
    status              TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 1-有效, 0-无效',
    creator             VARCHAR(64)  DEFAULT 'system' COMMENT '创建人',
    create_time         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater             VARCHAR(64)  DEFAULT 'system' COMMENT '更新人',
    update_time         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted             SMALLINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除标志: 0-未删除, 1-已删除',
    KEY idx_oa_triple_tenant (tenant_id),
    KEY idx_oa_triple_wechat (tenant_id, wechat_account_id),
    KEY idx_oa_triple_video (tenant_id, video_account_id),
    KEY idx_oa_triple_wework (tenant_id, wework_account_id)
) COMMENT='微信-视频号-企业微信三方关联表';

INSERT INTO sys_dict_type (type, name, status) VALUES
('dict_triple_rel_type', '三方关联类型', 'ENABLED');

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_triple_rel_type', '完整三方', 'FULL_TRIPLE', 1, 'ENABLED'),
('dict_triple_rel_type', '微信+视频', 'WECHAT_VIDEO', 2, 'ENABLED'),
('dict_triple_rel_type', '微信+企微', 'WECHAT_WEWORK', 3, 'ENABLED'),
('dict_triple_rel_type', '视频+企微', 'VIDEO_WEWORK', 4, 'ENABLED');
