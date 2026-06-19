-- S-17+: SMART_OPTIMIZE rule + template-link rule for one-click typesetting

INSERT INTO oa_typesetting_rule (tenant_id, rule_code, name, description, rule_config, sort, status, creator, updater) VALUES
(1, 'SMART_OPTIMIZE', '智能优化排版', '标题识别 + 样式库默认样式 + 基础规则组合', '{"type":"SMART_OPTIMIZE","blockSequence":["UNIFY_HEADING","PARAGRAPH_SPACING","NORMALIZE_QUOTE","IMAGE_RESPONSIVE"],"styleRefs":{"heading":"H2-LEFT-BOLD","paragraph":"P-STANDARD","quote":"P-QUOTE-INLINE"}}', 5, 'ENABLED', 'system', 'system')
ON DUPLICATE KEY UPDATE name = VALUES(name), rule_config = VALUES(rule_config);
