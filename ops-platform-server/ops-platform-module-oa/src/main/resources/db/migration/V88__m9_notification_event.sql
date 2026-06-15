-- 业务通知去重（避免监控扫描重复推送）
CREATE TABLE IF NOT EXISTS sys_notification_event (
    id                BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id         BIGINT       NOT NULL,
    event_type        VARCHAR(64)  NOT NULL,
    biz_key           VARCHAR(256) NOT NULL,
    recipient_user_id BIGINT       NULL,
    create_time       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_notification_event (tenant_id, event_type, biz_key),
    KEY idx_notification_event_tenant (tenant_id)
);
