-- M10-WECOM-S-01: 企微日聚合事实表 + 采集 data_type（ADR-048 · Channel-C）

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_collect_data_type', '企微日统计', 'WECOM_DAILY_STATS', 3, 'ENABLED')
ON DUPLICATE KEY UPDATE label = VALUES(label), sort = VALUES(sort);

CREATE TABLE IF NOT EXISTS oa_wework_daily_stats (
    id                          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id                   BIGINT       NOT NULL,
    wework_account_id           BIGINT       NOT NULL COMMENT 'FK oa_wework_account.id（任务 account_id 语义）',
    stat_date                   DATE         NOT NULL COMMENT '统计日',
    total_friends               INT          NOT NULL DEFAULT 0 COMMENT '外部联系人总数（92113 汇总）',
    today_friend_interactions   INT          NOT NULL DEFAULT 0 COMMENT '今日互动（92132 chat_cnt 映射）',
    today_messages_sent         INT          NOT NULL DEFAULT 0 COMMENT '今日发消息（92132 message_cnt）',
    synced_at                   TIMESTAMP    NULL COMMENT '采集写入时间',
    creator                     VARCHAR(64)  DEFAULT 'system',
    create_time                 TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater                     VARCHAR(64)  DEFAULT 'system',
    update_time                 TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                     SMALLINT     NOT NULL DEFAULT 0,
    UNIQUE KEY uk_oa_wework_daily_stats (tenant_id, wework_account_id, stat_date),
    KEY idx_oa_wework_daily_stats_account (tenant_id, wework_account_id)
);
