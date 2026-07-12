-- M2 AI 内容生成参数扩展：篇幅字典 + 占位符测试提示词模板
-- sys_dict_type / sys_dict_data 在部分环境 id 无 AUTO_INCREMENT，须显式分配 id (V128 同)

SET @next_type_id = (SELECT COALESCE(MAX(id), 0) + 1 FROM sys_dict_type);

INSERT INTO sys_dict_type (id, type, name, status)
SELECT @next_type_id, 'dict_content_length_type', '内容篇幅类型', 'ENABLED'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE type = 'dict_content_length_type');

SET @next_data_id = (SELECT COALESCE(MAX(id), 0) FROM sys_dict_data);

INSERT INTO sys_dict_data (id, dict_type, label, dict_value, sort, status) VALUES
(@next_data_id + 1, 'dict_content_length_type', '短篇500字', 'SHORT', 1, 'ENABLED'),
(@next_data_id + 2, 'dict_content_length_type', '中篇1000字', 'MEDIUM', 2, 'ENABLED'),
(@next_data_id + 3, 'dict_content_length_type', '长篇3000字', 'LONG', 3, 'ENABLED')
ON DUPLICATE KEY UPDATE label = VALUES(label), sort = VALUES(sort), status = VALUES(status);

INSERT INTO oa_ai_prompt_config
  (tenant_id, template_name, version, scene, content_type, prompt_content, variable_desc, temperature, status, remark)
SELECT 1, '内容生成-赛事文稿（测试模板）', 'v2', 'CONTENT_GENERATE', 'ARTICLE',
'你是一位专业的体育自媒体内容编辑。请根据以下信息撰写一篇运营文稿。

【赛事信息】
{{match}}

{{#author}}【作者/主播】
{{author}}
{{/author}}

{{#historicalRecord}}【历史战绩】
{{historicalRecord}}
{{/historicalRecord}}

{{#matchDirection}}【赛事方向】
{{matchDirection}}
{{/matchDirection}}

{{#streamerPersona}}【主播人设】
{{streamerPersona}}
{{/streamerPersona}}

{{#revisionFeedback}}【修改意见】
{{revisionFeedback}}
{{/revisionFeedback}}

{{#lengthType}}【篇幅要求】
{{lengthType}}
{{/lengthType}}

要求：语言流畅、结构清晰、符合公众号发布习惯；仅输出正文，不要额外解释。',
'{{match}}=赛事; {{author}}=作者/主播; {{historicalRecord}}=历史战绩; {{matchDirection}}=赛事方向; {{streamerPersona}}=主播人设; {{revisionFeedback}}=修改意见; {{lengthType}}=篇幅(短篇500字/中篇1000字/长篇3000字)；空字段对应 {{#key}}...{{/key}} 整段省略',
0.70, 'ENABLED', 'M2 AI 内容生成占位符测试模板（integration :5777）'
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM oa_ai_prompt_config
  WHERE tenant_id = 1 AND template_name = '内容生成-赛事文稿（测试模板）' AND deleted = 0
);
