-- S-17: typesetting rules for one-click formatting

CREATE TABLE IF NOT EXISTS oa_typesetting_rule (
    id                  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id           BIGINT       NOT NULL,
    rule_code           VARCHAR(64)  NOT NULL,
    name                VARCHAR(100) NOT NULL,
    description         VARCHAR(500) NULL,
    rule_config         JSON         NOT NULL,
    sort                INT          NOT NULL DEFAULT 0,
    status              VARCHAR(20)  NOT NULL DEFAULT 'ENABLED',
    creator             VARCHAR(64)  DEFAULT 'system',
    create_time         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater             VARCHAR(64)  DEFAULT 'system',
    update_time         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             SMALLINT     NOT NULL DEFAULT 0,
    UNIQUE KEY uk_oa_typesetting_rule_code (tenant_id, rule_code),
    KEY idx_oa_typesetting_rule_tenant (tenant_id, status)
);

INSERT INTO sys_permission (id, code, name, module, creator, updater) VALUES
(54, 'oa:typesetting-rule:query', 'Typesetting rule query', 'M2', 's17', 's17'),
(55, 'oa:typesetting-rule:create', 'Typesetting rule create', 'M2', 's17', 's17'),
(56, 'oa:typesetting-rule:update', 'Typesetting rule update', 'M2', 's17', 's17'),
(57, 'oa:typesetting-rule:delete', 'Typesetting rule delete', 'M2', 's17', 's17'),
(58, 'oa:content:typeset', 'Content typeset apply', 'M2', 's17', 's17')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO sys_role_permission (role_id, permission_id) VALUES
(1, 54), (1, 55), (1, 56), (1, 57), (1, 58)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO oa_typesetting_rule (tenant_id, rule_code, name, description, rule_config, sort, status, creator, updater) VALUES
(1, 'UNIFY_HEADING', '统一标题层级', '将 h1/h4 规范为 h2/h3，保留文字', '{"type":"UNIFY_HEADING","heading2Size":"18px","heading3Size":"16px"}', 10, 'ENABLED', 'system', 'system'),
(1, 'PARAGRAPH_SPACING', '段落间距优化', '为段落添加标准行高与间距', '{"type":"PARAGRAPH_SPACING","lineHeight":"1.75","marginBottom":"16px","fontSize":"16px"}', 20, 'ENABLED', 'system', 'system'),
(1, 'NORMALIZE_QUOTE', '引用块规范化', '统一 blockquote 样式', '{"type":"NORMALIZE_QUOTE","borderColor":"#07c160","backgroundColor":"#f7f7f7"}', 30, 'ENABLED', 'system', 'system'),
(1, 'IMAGE_RESPONSIVE', '图片自适应', '图片宽度 100% 自适应', '{"type":"IMAGE_RESPONSIVE","maxWidth":"100%"}', 40, 'ENABLED', 'system', 'system'),
(1, 'STRIP_INLINE_STYLE', '清除冗余内联样式', '保留文字，清除多余 span style', '{"type":"STRIP_INLINE_STYLE","preserveBold":true}', 50, 'ENABLED', 'system', 'system')
ON DUPLICATE KEY UPDATE name = VALUES(name);
