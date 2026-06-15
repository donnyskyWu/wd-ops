-- S-14e: seed 5 PRESET layout templates (ADR-020 §2.9, tenant_id=1)

SET @gs = '{"heading2":{"fontSize":"18px","fontWeight":"bold","color":"#1a1a1a","lineHeight":"1.4","marginBottom":"12px"},"heading3":{"fontSize":"16px","fontWeight":"bold","color":"#333333","lineHeight":"1.4"},"paragraph":{"fontSize":"16px","color":"#333333","lineHeight":"1.75","marginBottom":"16px"},"quote":{"fontSize":"15px","color":"#666666","backgroundColor":"#f7f7f7","borderLeft":"4px solid #07c160","padding":"12px 16px","lineHeight":"1.6"},"divider":{"borderColor":"#e5e5e5","margin":"24px 0"},"image":{"width":"100%","borderRadius":"4px"},"list":{"fontSize":"16px","lineHeight":"1.75","color":"#333333"}}';

INSERT INTO oa_wechat_layout_template
(tenant_id, template_name, description, content_type, document_type, layout_json, layout_schema, schema_version, layout_html, source_type, status, creator_user_id, creator, updater)
SELECT 1, '【预置】公众号长文导读', '适用于日常长文与导读引用，套用后保留您的正文', 'ARTICLE', NULL,
       '{"version":1,"blocks":[]}',
       JSON_OBJECT('version', 2, 'globalStyles', CAST(@gs AS JSON), 'blocks', JSON_ARRAY(
         JSON_OBJECT('type','heading','level',2,'styleRef','heading2','slotKind','heading'),
         JSON_OBJECT('type','slot','slotKind','quote','styleRef','quote'),
         JSON_OBJECT('type','slot','slotKind','paragraph','styleRef','paragraph','repeat',true),
         JSON_OBJECT('type','divider','styleRef','divider'),
         JSON_OBJECT('type','slot','slotKind','paragraph','styleRef','paragraph','repeat',true)
       )),
       2, NULL, 'PRESET', 'ENABLED', 1, 'system', 'system'
WHERE NOT EXISTS (SELECT 1 FROM oa_wechat_layout_template WHERE tenant_id=1 AND source_type='PRESET' AND template_name='【预置】公众号长文导读');

INSERT INTO oa_wechat_layout_template
(tenant_id, template_name, description, content_type, document_type, layout_json, layout_schema, schema_version, layout_html, source_type, status, creator_user_id, creator, updater)
SELECT 1, '【预置】活动预告', '赛事/直播/线下活动预告版式', 'ARTICLE', 'PREHEAT_PREVIEW',
       '{"version":1,"blocks":[]}',
       JSON_OBJECT('version', 2, 'globalStyles', CAST(@gs AS JSON), 'blocks', JSON_ARRAY(
         JSON_OBJECT('type','heading','level',2,'styleRef','heading2','slotKind','heading','align','center'),
         JSON_OBJECT('type','slot','slotKind','quote','styleRef','quote'),
         JSON_OBJECT('type','slot','slotKind','paragraph','styleRef','paragraph','repeat',true),
         JSON_OBJECT('type','frame','slotKind','image','styleRef','image','optional',true),
         JSON_OBJECT('type','divider','styleRef','divider'),
         JSON_OBJECT('type','slot','slotKind','paragraph','styleRef','paragraph','repeat',true,'maxRepeat',2)
       )),
       2, NULL, 'PRESET', 'ENABLED', 1, 'system', 'system'
WHERE NOT EXISTS (SELECT 1 FROM oa_wechat_layout_template WHERE tenant_id=1 AND source_type='PRESET' AND template_name='【预置】活动预告');

INSERT INTO oa_wechat_layout_template
(tenant_id, template_name, description, content_type, document_type, layout_json, layout_schema, schema_version, layout_html, source_type, status, creator_user_id, creator, updater)
SELECT 1, '【预置】赛事战报', '赛后速报、比分通报版式', 'ARTICLE', 'POST_MATCH_REVIEW',
       '{"version":1,"blocks":[]}',
       JSON_OBJECT('version', 2, 'globalStyles', CAST(@gs AS JSON), 'blocks', JSON_ARRAY(
         JSON_OBJECT('type','heading','level',2,'styleRef','heading2','slotKind','heading'),
         JSON_OBJECT('type','fixed','fixedType','score-highlight','styleRef','quote'),
         JSON_OBJECT('type','slot','slotKind','paragraph','styleRef','paragraph','repeat',true),
         JSON_OBJECT('type','slot','slotKind','quote','styleRef','quote','optional',true),
         JSON_OBJECT('type','frame','slotKind','image','styleRef','image','optional',true)
       )),
       2, NULL, 'PRESET', 'ENABLED', 1, 'system', 'system'
WHERE NOT EXISTS (SELECT 1 FROM oa_wechat_layout_template WHERE tenant_id=1 AND source_type='PRESET' AND template_name='【预置】赛事战报');

INSERT INTO oa_wechat_layout_template
(tenant_id, template_name, description, content_type, document_type, layout_json, layout_schema, schema_version, layout_html, source_type, status, creator_user_id, creator, updater)
SELECT 1, '【预置】FAQ 问答', '官方方案说明、常见问答版式', 'ARTICLE', 'OFFICIAL_PLAN',
       '{"version":1,"blocks":[]}',
       JSON_OBJECT('version', 2, 'globalStyles', CAST(@gs AS JSON), 'blocks', JSON_ARRAY(
         JSON_OBJECT('type','heading','level',2,'styleRef','heading2','slotKind','heading'),
         JSON_OBJECT('type','section','repeat',true,'maxRepeat',10,'children', JSON_ARRAY(
           JSON_OBJECT('type','heading','level',3,'styleRef','heading3','slotKind','heading'),
           JSON_OBJECT('type','slot','slotKind','paragraph','styleRef','paragraph')
         )),
         JSON_OBJECT('type','divider','styleRef','divider')
       )),
       2, NULL, 'PRESET', 'ENABLED', 1, 'system', 'system'
WHERE NOT EXISTS (SELECT 1 FROM oa_wechat_layout_template WHERE tenant_id=1 AND source_type='PRESET' AND template_name='【预置】FAQ 问答');

INSERT INTO oa_wechat_layout_template
(tenant_id, template_name, description, content_type, document_type, layout_json, layout_schema, schema_version, layout_html, source_type, status, creator_user_id, creator, updater)
SELECT 1, '【预置】短视频引流贴片', '短文案引流、关注引导版式', 'ARTICLE', 'SHORT_VIDEO_SCRIPT',
       '{"version":1,"blocks":[]}',
       JSON_OBJECT('version', 2, 'globalStyles', CAST(@gs AS JSON), 'blocks', JSON_ARRAY(
         JSON_OBJECT('type','heading','level',2,'styleRef','heading2','slotKind','heading','align','center'),
         JSON_OBJECT('type','slot','slotKind','quote','styleRef','quote','align','center'),
         JSON_OBJECT('type','slot','slotKind','paragraph','styleRef','paragraph','maxRepeat',2),
         JSON_OBJECT('type','divider','styleRef','divider'),
         JSON_OBJECT('type','fixed','fixedType','brand-footer','styleRef','paragraph')
       )),
       2, NULL, 'PRESET', 'ENABLED', 1, 'system', 'system'
WHERE NOT EXISTS (SELECT 1 FROM oa_wechat_layout_template WHERE tenant_id=1 AND source_type='PRESET' AND template_name='【预置】短视频引流贴片');

-- Migrate existing v1 templates: extract schema from layout_json (best-effort placeholder)
UPDATE oa_wechat_layout_template
SET schema_version = 1
WHERE layout_schema IS NULL AND schema_version = 1 AND layout_json IS NOT NULL;
