-- FR-145: 每个文档类型增加「图文混排」预置模板（tenant_id=1）

SET @gs = '{"heading2":{"fontSize":"18px","fontWeight":"bold","color":"#1a1a1a","lineHeight":"1.4","marginBottom":"12px"},"heading3":{"fontSize":"16px","fontWeight":"bold","color":"#333333","lineHeight":"1.4"},"paragraph":{"fontSize":"16px","color":"#333333","lineHeight":"1.75","marginBottom":"16px"},"quote":{"fontSize":"15px","color":"#666666","backgroundColor":"#f7f7f7","borderLeft":"4px solid #07c160","padding":"12px 16px","lineHeight":"1.6"},"divider":{"borderColor":"#e5e5e5","margin":"24px 0"},"image":{"width":"100%","borderRadius":"4px"},"list":{"fontSize":"16px","lineHeight":"1.75","color":"#333333"}}';

SET @mixed_blocks = JSON_ARRAY(
  JSON_OBJECT('type','heading','level',2,'styleRef','heading2','slotKind','heading'),
  JSON_OBJECT('type','slot','slotKind','paragraph','styleRef','paragraph'),
  JSON_OBJECT('type','frame','slotKind','image','styleRef','image','optional',true),
  JSON_OBJECT('type','slot','slotKind','paragraph','styleRef','paragraph','repeat',true),
  JSON_OBJECT('type','frame','slotKind','image','styleRef','image','optional',true),
  JSON_OBJECT('type','slot','slotKind','paragraph','styleRef','paragraph','repeat',true),
  JSON_OBJECT('type','divider','styleRef','divider'),
  JSON_OBJECT('type','slot','slotKind','paragraph','styleRef','paragraph','repeat',true,'maxRepeat',2)
);

INSERT INTO oa_wechat_layout_template
(tenant_id, template_name, description, content_type, document_type, layout_json, layout_schema, schema_version, layout_html, source_type, status, creator_user_id, creator, updater)
SELECT 1, '【预置】图文混排·通用', '通用图文混排：标题、导语、配图与正文交替，典型公众号长文结构', 'ARTICLE', NULL,
       '{"version":1,"blocks":[]}',
       JSON_OBJECT('version', 2, 'globalStyles', CAST(@gs AS JSON), 'blocks', CAST(@mixed_blocks AS JSON)),
       2, NULL, 'PRESET', 'ENABLED', 1, 'system', 'system'
WHERE NOT EXISTS (SELECT 1 FROM oa_wechat_layout_template WHERE tenant_id=1 AND source_type='PRESET' AND template_name='【预置】图文混排·通用');

INSERT INTO oa_wechat_layout_template
(tenant_id, template_name, description, content_type, document_type, layout_json, layout_schema, schema_version, layout_html, source_type, status, creator_user_id, creator, updater)
SELECT 1, '【预置】图文混排·预热前瞻', '活动预告图文混排：时间地点导语 + 配图 + 详情段落', 'ARTICLE', 'PREHEAT_PREVIEW',
       '{"version":1,"blocks":[]}',
       JSON_OBJECT('version', 2, 'globalStyles', CAST(@gs AS JSON), 'blocks', CAST(@mixed_blocks AS JSON)),
       2, NULL, 'PRESET', 'ENABLED', 1, 'system', 'system'
WHERE NOT EXISTS (SELECT 1 FROM oa_wechat_layout_template WHERE tenant_id=1 AND source_type='PRESET' AND template_name='【预置】图文混排·预热前瞻');

INSERT INTO oa_wechat_layout_template
(tenant_id, template_name, description, content_type, document_type, layout_json, layout_schema, schema_version, layout_html, source_type, status, creator_user_id, creator, updater)
SELECT 1, '【预置】图文混排·赛后复盘', '赛后复盘图文混排：战报导语 + 赛场配图 + 解读段落', 'ARTICLE', 'POST_MATCH_REVIEW',
       '{"version":1,"blocks":[]}',
       JSON_OBJECT('version', 2, 'globalStyles', CAST(@gs AS JSON), 'blocks', CAST(@mixed_blocks AS JSON)),
       2, NULL, 'PRESET', 'ENABLED', 1, 'system', 'system'
WHERE NOT EXISTS (SELECT 1 FROM oa_wechat_layout_template WHERE tenant_id=1 AND source_type='PRESET' AND template_name='【预置】图文混排·赛后复盘');

INSERT INTO oa_wechat_layout_template
(tenant_id, template_name, description, content_type, document_type, layout_json, layout_schema, schema_version, layout_html, source_type, status, creator_user_id, creator, updater)
SELECT 1, '【预置】图文混排·正式方案', '方案说明图文混排：要点导语 + 示意图 + 分步说明', 'ARTICLE', 'OFFICIAL_PLAN',
       '{"version":1,"blocks":[]}',
       JSON_OBJECT('version', 2, 'globalStyles', CAST(@gs AS JSON), 'blocks', CAST(@mixed_blocks AS JSON)),
       2, NULL, 'PRESET', 'ENABLED', 1, 'system', 'system'
WHERE NOT EXISTS (SELECT 1 FROM oa_wechat_layout_template WHERE tenant_id=1 AND source_type='PRESET' AND template_name='【预置】图文混排·正式方案');

INSERT INTO oa_wechat_layout_template
(tenant_id, template_name, description, content_type, document_type, layout_json, layout_schema, schema_version, layout_html, source_type, status, creator_user_id, creator, updater)
SELECT 1, '【预置】图文混排·短视频文案', '短视频引流图文混排：短导语 + 封面图 + 引流文案', 'ARTICLE', 'SHORT_VIDEO_SCRIPT',
       '{"version":1,"blocks":[]}',
       JSON_OBJECT('version', 2, 'globalStyles', CAST(@gs AS JSON), 'blocks', CAST(@mixed_blocks AS JSON)),
       2, NULL, 'PRESET', 'ENABLED', 1, 'system', 'system'
WHERE NOT EXISTS (SELECT 1 FROM oa_wechat_layout_template WHERE tenant_id=1 AND source_type='PRESET' AND template_name='【预置】图文混排·短视频文案');

INSERT INTO oa_wechat_layout_template
(tenant_id, template_name, description, content_type, document_type, layout_json, layout_schema, schema_version, layout_html, source_type, status, creator_user_id, creator, updater)
SELECT 1, '【预置】图文混排·新号引流', '新号引流图文混排：种草导语 + 产品图 + 清单段落', 'ARTICLE', 'NEW_ACCOUNT_TRAFFIC',
       '{"version":1,"blocks":[]}',
       JSON_OBJECT('version', 2, 'globalStyles', CAST(@gs AS JSON), 'blocks', CAST(@mixed_blocks AS JSON)),
       2, NULL, 'PRESET', 'ENABLED', 1, 'system', 'system'
WHERE NOT EXISTS (SELECT 1 FROM oa_wechat_layout_template WHERE tenant_id=1 AND source_type='PRESET' AND template_name='【预置】图文混排·新号引流');
