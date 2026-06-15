-- M2 内容手动发布工作流（ADR-022）：审核通过 → 待发布 → 手动发布 → 已发布

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_content_status', '待发布', 'PENDING_PUBLISH', 6, 'ENABLED')
ON DUPLICATE KEY UPDATE label = VALUES(label), sort = VALUES(sort);

UPDATE sys_dict_data SET sort = 7 WHERE dict_type = 'dict_content_status' AND dict_value = 'PUBLISHED';
UPDATE sys_dict_data SET sort = 8 WHERE dict_type = 'dict_content_status' AND dict_value = 'UNPUBLISHED';
UPDATE sys_dict_data SET sort = 9 WHERE dict_type = 'dict_content_status' AND dict_value = 'COMPLETED';

ALTER TABLE oa_account ADD COLUMN publish_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否配置发布权限';

-- 已有 Cookie 的账号视为已配置发布能力（Phase 2 dev stub）
-- Dev seed：租户 1 正常账号默认开启发布权限（Phase 2；生产由运营在账号详情配置）
UPDATE oa_account SET publish_enabled = 1 WHERE tenant_id = 1 AND status = 'NORMAL';

CREATE TABLE IF NOT EXISTS oa_content_publish_record (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    content_id      BIGINT       NOT NULL,
    account_id      BIGINT       NOT NULL,
    platform_type   VARCHAR(32)  NOT NULL,
    status          VARCHAR(32)  NOT NULL COMMENT 'SUCCESS / FAILED',
    external_id     VARCHAR(128) NULL COMMENT '平台侧作品 ID',
    error_message   VARCHAR(500) NULL,
    published_at    TIMESTAMP    NULL,
    creator         VARCHAR(64)  DEFAULT 'system',
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         VARCHAR(64)  DEFAULT 'system',
    update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT     NOT NULL DEFAULT 0,
    KEY idx_oa_content_publish_content (tenant_id, content_id),
    KEY idx_oa_content_publish_account (tenant_id, account_id)
);

INSERT INTO sys_permission (id, code, name, module, creator, updater) VALUES
(48, 'oa:content:publish', '内容发布', 'M2', 's15', 's15')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO sys_role_permission (role_id, permission_id) VALUES
(1, 48), (3, 48), (4, 48)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);
