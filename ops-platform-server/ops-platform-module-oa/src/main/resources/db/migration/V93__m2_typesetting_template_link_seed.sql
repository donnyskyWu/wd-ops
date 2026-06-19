-- S-17+: TEMPLATE_LINK rules → PRESET layout templates (tenant_id=1, linkedTemplateId via subquery)

INSERT INTO oa_typesetting_rule (tenant_id, rule_code, name, description, rule_config, sort, status, creator, updater)
SELECT 1, 'TEMPLATE_LINK_READING', '套用长文导读版式', '一键套用【预置】公众号长文导读模板骨架',
  JSON_OBJECT('type', 'TEMPLATE_LINK', 'linkedTemplateId', (
    SELECT id FROM oa_wechat_layout_template WHERE tenant_id = 1 AND source_type = 'PRESET' AND template_name = '【预置】公众号长文导读' AND deleted = 0 LIMIT 1
  )),
  61, 'ENABLED', 'system', 'system'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM oa_typesetting_rule WHERE tenant_id = 1 AND rule_code = 'TEMPLATE_LINK_READING')
  AND EXISTS (SELECT 1 FROM oa_wechat_layout_template WHERE tenant_id = 1 AND source_type = 'PRESET' AND template_name = '【预置】公众号长文导读' AND deleted = 0);

INSERT INTO oa_typesetting_rule (tenant_id, rule_code, name, description, rule_config, sort, status, creator, updater)
SELECT 1, 'TEMPLATE_LINK_PREVIEW', '套用活动预告版式', '一键套用【预置】活动预告模板骨架',
  JSON_OBJECT('type', 'TEMPLATE_LINK', 'linkedTemplateId', (
    SELECT id FROM oa_wechat_layout_template WHERE tenant_id = 1 AND source_type = 'PRESET' AND template_name = '【预置】活动预告' AND deleted = 0 LIMIT 1
  )),
  62, 'ENABLED', 'system', 'system'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM oa_typesetting_rule WHERE tenant_id = 1 AND rule_code = 'TEMPLATE_LINK_PREVIEW')
  AND EXISTS (SELECT 1 FROM oa_wechat_layout_template WHERE tenant_id = 1 AND source_type = 'PRESET' AND template_name = '【预置】活动预告' AND deleted = 0);

INSERT INTO oa_typesetting_rule (tenant_id, rule_code, name, description, rule_config, sort, status, creator, updater)
SELECT 1, 'TEMPLATE_LINK_WAR_REPORT', '套用赛事战报版式', '一键套用【预置】赛事战报模板骨架',
  JSON_OBJECT('type', 'TEMPLATE_LINK', 'linkedTemplateId', (
    SELECT id FROM oa_wechat_layout_template WHERE tenant_id = 1 AND source_type = 'PRESET' AND template_name = '【预置】赛事战报' AND deleted = 0 LIMIT 1
  )),
  63, 'ENABLED', 'system', 'system'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM oa_typesetting_rule WHERE tenant_id = 1 AND rule_code = 'TEMPLATE_LINK_WAR_REPORT')
  AND EXISTS (SELECT 1 FROM oa_wechat_layout_template WHERE tenant_id = 1 AND source_type = 'PRESET' AND template_name = '【预置】赛事战报' AND deleted = 0);

INSERT INTO oa_typesetting_rule (tenant_id, rule_code, name, description, rule_config, sort, status, creator, updater)
SELECT 1, 'TEMPLATE_LINK_FAQ', '套用 FAQ 问答版式', '一键套用【预置】FAQ 问答模板骨架',
  JSON_OBJECT('type', 'TEMPLATE_LINK', 'linkedTemplateId', (
    SELECT id FROM oa_wechat_layout_template WHERE tenant_id = 1 AND source_type = 'PRESET' AND template_name = '【预置】FAQ 问答' AND deleted = 0 LIMIT 1
  )),
  64, 'ENABLED', 'system', 'system'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM oa_typesetting_rule WHERE tenant_id = 1 AND rule_code = 'TEMPLATE_LINK_FAQ')
  AND EXISTS (SELECT 1 FROM oa_wechat_layout_template WHERE tenant_id = 1 AND source_type = 'PRESET' AND template_name = '【预置】FAQ 问答' AND deleted = 0);

INSERT INTO oa_typesetting_rule (tenant_id, rule_code, name, description, rule_config, sort, status, creator, updater)
SELECT 1, 'TEMPLATE_LINK_SHORT_VIDEO', '套用短视频引流版式', '一键套用【预置】短视频引流贴片模板骨架',
  JSON_OBJECT('type', 'TEMPLATE_LINK', 'linkedTemplateId', (
    SELECT id FROM oa_wechat_layout_template WHERE tenant_id = 1 AND source_type = 'PRESET' AND template_name = '【预置】短视频引流贴片' AND deleted = 0 LIMIT 1
  )),
  65, 'ENABLED', 'system', 'system'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM oa_typesetting_rule WHERE tenant_id = 1 AND rule_code = 'TEMPLATE_LINK_SHORT_VIDEO')
  AND EXISTS (SELECT 1 FROM oa_wechat_layout_template WHERE tenant_id = 1 AND source_type = 'PRESET' AND template_name = '【预置】短视频引流贴片' AND deleted = 0);

INSERT INTO oa_typesetting_rule (tenant_id, rule_code, name, description, rule_config, sort, status, creator, updater)
SELECT 1, 'TEMPLATE_LINK_MIXED_GENERAL', '套用图文混排·通用', '一键套用【预置】图文混排·通用模板骨架',
  JSON_OBJECT('type', 'TEMPLATE_LINK', 'linkedTemplateId', (
    SELECT id FROM oa_wechat_layout_template WHERE tenant_id = 1 AND source_type = 'PRESET' AND template_name = '【预置】图文混排·通用' AND deleted = 0 LIMIT 1
  )),
  66, 'ENABLED', 'system', 'system'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM oa_typesetting_rule WHERE tenant_id = 1 AND rule_code = 'TEMPLATE_LINK_MIXED_GENERAL')
  AND EXISTS (SELECT 1 FROM oa_wechat_layout_template WHERE tenant_id = 1 AND source_type = 'PRESET' AND template_name = '【预置】图文混排·通用' AND deleted = 0);

INSERT INTO oa_typesetting_rule (tenant_id, rule_code, name, description, rule_config, sort, status, creator, updater)
SELECT 1, 'TEMPLATE_LINK_MIXED_PREVIEW', '套用图文混排·预热前瞻', '一键套用【预置】图文混排·预热前瞻模板骨架',
  JSON_OBJECT('type', 'TEMPLATE_LINK', 'linkedTemplateId', (
    SELECT id FROM oa_wechat_layout_template WHERE tenant_id = 1 AND source_type = 'PRESET' AND template_name = '【预置】图文混排·预热前瞻' AND deleted = 0 LIMIT 1
  )),
  67, 'ENABLED', 'system', 'system'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM oa_typesetting_rule WHERE tenant_id = 1 AND rule_code = 'TEMPLATE_LINK_MIXED_PREVIEW')
  AND EXISTS (SELECT 1 FROM oa_wechat_layout_template WHERE tenant_id = 1 AND source_type = 'PRESET' AND template_name = '【预置】图文混排·预热前瞻' AND deleted = 0);

INSERT INTO oa_typesetting_rule (tenant_id, rule_code, name, description, rule_config, sort, status, creator, updater)
SELECT 1, 'TEMPLATE_LINK_MIXED_WAR', '套用图文混排·赛后复盘', '一键套用【预置】图文混排·赛后复盘模板骨架',
  JSON_OBJECT('type', 'TEMPLATE_LINK', 'linkedTemplateId', (
    SELECT id FROM oa_wechat_layout_template WHERE tenant_id = 1 AND source_type = 'PRESET' AND template_name = '【预置】图文混排·赛后复盘' AND deleted = 0 LIMIT 1
  )),
  68, 'ENABLED', 'system', 'system'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM oa_typesetting_rule WHERE tenant_id = 1 AND rule_code = 'TEMPLATE_LINK_MIXED_WAR')
  AND EXISTS (SELECT 1 FROM oa_wechat_layout_template WHERE tenant_id = 1 AND source_type = 'PRESET' AND template_name = '【预置】图文混排·赛后复盘' AND deleted = 0);

INSERT INTO oa_typesetting_rule (tenant_id, rule_code, name, description, rule_config, sort, status, creator, updater)
SELECT 1, 'TEMPLATE_LINK_MIXED_PLAN', '套用图文混排·正式方案', '一键套用【预置】图文混排·正式方案模板骨架',
  JSON_OBJECT('type', 'TEMPLATE_LINK', 'linkedTemplateId', (
    SELECT id FROM oa_wechat_layout_template WHERE tenant_id = 1 AND source_type = 'PRESET' AND template_name = '【预置】图文混排·正式方案' AND deleted = 0 LIMIT 1
  )),
  69, 'ENABLED', 'system', 'system'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM oa_typesetting_rule WHERE tenant_id = 1 AND rule_code = 'TEMPLATE_LINK_MIXED_PLAN')
  AND EXISTS (SELECT 1 FROM oa_wechat_layout_template WHERE tenant_id = 1 AND source_type = 'PRESET' AND template_name = '【预置】图文混排·正式方案' AND deleted = 0);

INSERT INTO oa_typesetting_rule (tenant_id, rule_code, name, description, rule_config, sort, status, creator, updater)
SELECT 1, 'TEMPLATE_LINK_MIXED_SHORT', '套用图文混排·短视频文案', '一键套用【预置】图文混排·短视频文案模板骨架',
  JSON_OBJECT('type', 'TEMPLATE_LINK', 'linkedTemplateId', (
    SELECT id FROM oa_wechat_layout_template WHERE tenant_id = 1 AND source_type = 'PRESET' AND template_name = '【预置】图文混排·短视频文案' AND deleted = 0 LIMIT 1
  )),
  70, 'ENABLED', 'system', 'system'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM oa_typesetting_rule WHERE tenant_id = 1 AND rule_code = 'TEMPLATE_LINK_MIXED_SHORT')
  AND EXISTS (SELECT 1 FROM oa_wechat_layout_template WHERE tenant_id = 1 AND source_type = 'PRESET' AND template_name = '【预置】图文混排·短视频文案' AND deleted = 0);

INSERT INTO oa_typesetting_rule (tenant_id, rule_code, name, description, rule_config, sort, status, creator, updater)
SELECT 1, 'TEMPLATE_LINK_MIXED_TRAFFIC', '套用图文混排·新号引流', '一键套用【预置】图文混排·新号引流模板骨架',
  JSON_OBJECT('type', 'TEMPLATE_LINK', 'linkedTemplateId', (
    SELECT id FROM oa_wechat_layout_template WHERE tenant_id = 1 AND source_type = 'PRESET' AND template_name = '【预置】图文混排·新号引流' AND deleted = 0 LIMIT 1
  )),
  71, 'ENABLED', 'system', 'system'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM oa_typesetting_rule WHERE tenant_id = 1 AND rule_code = 'TEMPLATE_LINK_MIXED_TRAFFIC')
  AND EXISTS (SELECT 1 FROM oa_wechat_layout_template WHERE tenant_id = 1 AND source_type = 'PRESET' AND template_name = '【预置】图文混排·新号引流' AND deleted = 0);
