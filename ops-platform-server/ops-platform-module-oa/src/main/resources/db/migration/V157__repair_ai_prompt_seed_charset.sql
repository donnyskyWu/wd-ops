-- V157: Repair AI prompt seed charset corruption + expand dict_ai_scene
-- Root cause: V43 seed ran without SET NAMES utf8mb4; Chinese stored as literal '?' (0x3F).

SET NAMES utf8mb4;

-- 1) Expand dict_ai_scene to match oa_ai_prompt_config.scene values and frontend DictSelect
SET @next_data_id = (SELECT COALESCE(MAX(id), 0) FROM sys_dict_data);

INSERT INTO sys_dict_data (id, dict_type, label, dict_value, sort, status) VALUES
(@next_data_id + 1, 'dict_ai_scene', '短视频文案', 'SHORT_VIDEO', 10, 'ENABLED'),
(@next_data_id + 2, 'dict_ai_scene', '直播脚本', 'LIVE_SCRIPT', 11, 'ENABLED'),
(@next_data_id + 3, 'dict_ai_scene', '小红书笔记', 'XIAOHONGSHU', 12, 'ENABLED'),
(@next_data_id + 4, 'dict_ai_scene', '公众号文章', 'WECHAT_ARTICLE', 13, 'ENABLED'),
(@next_data_id + 5, 'dict_ai_scene', '数据分析', 'DATA_ANALYSIS', 14, 'ENABLED'),
(@next_data_id + 6, 'dict_ai_scene', '周报月报', 'REPORT', 15, 'ENABLED'),
(@next_data_id + 7, 'dict_ai_scene', '竞品分析', 'COMPETITOR', 16, 'ENABLED'),
(@next_data_id + 8, 'dict_ai_scene', 'AI内容对话', 'AI_CONTENT_CHAT', 17, 'ENABLED'),
(@next_data_id + 9, 'dict_ai_scene', '内容生成', 'CONTENT_GENERATE', 18, 'ENABLED')
ON DUPLICATE KEY UPDATE label = VALUES(label), sort = VALUES(sort), status = VALUES(status);

-- 2) Repair V43 seed rows where Chinese was replaced by '?'
UPDATE oa_ai_prompt_config SET
  template_name = '短视频文案生成',
  prompt_content = '你是一位专业的短视频文案策划师。请根据以下产品信息生成一条吸引人的短视频文案：\n产品名称：{{product_name}}\n产品卖点：{{key_features}}\n目标受众：{{target_audience}}\n要求：文案简洁有力，不超过150字，突出核心卖点，结尾带上引导语。',
  variable_desc = '{{product_name}} - 产品名称; {{key_features}} - 核心卖点; {{target_audience}} - 目标受众',
  remark = '短视频脚本文案生成'
WHERE tenant_id = 1 AND scene = 'SHORT_VIDEO' AND deleted = 0
  AND template_name REGEXP '^[?]+$';

UPDATE oa_ai_prompt_config SET
  template_name = '直播带货脚本',
  prompt_content = '你是一位经验丰富的直播带货主播助手。请为以下产品生成一段直播销售脚本：\n产品：{{product_name}}\n价格：{{price}}\n核心优势：{{advantages}}\n当前促销：{{promotion}}\n要求：语言亲切自然，突出性价比，包含互动引导词，时长约3分钟。',
  variable_desc = '{{product_name}} - 产品名称; {{price}} - 价格; {{advantages}} - 核心优势; {{promotion}} - 当前促销活动',
  remark = '直播脚本生成'
WHERE tenant_id = 1 AND scene = 'LIVE_SCRIPT' AND deleted = 0
  AND template_name REGEXP '^[?]+$';

UPDATE oa_ai_prompt_config SET
  template_name = '小红书种草笔记',
  prompt_content = '你是小红书资深博主，请为以下内容生成一篇种草笔记：\n品类：{{category}}\n产品：{{product_name}}\n使用感受：{{experience}}\n要求：标题吸引眼球含emoji，正文分段清晰，结尾含话题标签，整体风格真实自然。',
  variable_desc = '{{category}} - 产品品类; {{product_name}} - 产品名称; {{experience}} - 使用感受',
  remark = '小红书种草笔记生成'
WHERE tenant_id = 1 AND scene = 'XIAOHONGSHU' AND deleted = 0
  AND template_name REGEXP '^[?]+$';

UPDATE oa_ai_prompt_config SET
  template_name = '数据分析报告摘要',
  prompt_content = '你是专业的数据分析师。请根据以下数据摘要生成分析解读：\n数据类型：{{data_type}}\n时间范围：{{time_range}}\n关键指标：{{metrics}}\n要求：客观分析数据趋势，指出异常点，给出可能的业务原因和改进建议，语言专业简洁。',
  variable_desc = '{{data_type}} - 数据类型; {{time_range}} - 时间范围; {{metrics}} - 关键指标数据',
  remark = '数据分析报告摘要生成'
WHERE tenant_id = 1 AND scene = 'DATA_ANALYSIS' AND deleted = 0
  AND template_name REGEXP '^[?]+$';

UPDATE oa_ai_prompt_config SET
  template_name = '周报月报生成',
  prompt_content = '你是运营数据专员。请根据以下数据生成一份运营周报：\n时间周期：{{period}}\n团队：{{team}}\n核心数据：{{core_data}}\n要求：包含数据摘要、亮点成绩、问题分析、下周计划四个模块，格式规范，数据呈现清晰。',
  variable_desc = '{{period}} - 报告周期; {{team}} - 所属团队; {{core_data}} - 核心业务数据',
  remark = '周报月报自动生成'
WHERE tenant_id = 1 AND scene = 'REPORT' AND deleted = 0
  AND template_name REGEXP '^[?]+$';

UPDATE oa_ai_prompt_config SET
  template_name = '竞品分析报告',
  prompt_content = '你是市场调研专家。请根据以下信息生成竞品分析报告：\n我方品牌：{{our_brand}}\n竞品：{{competitor}}\n对比维度：{{dimensions}}\n要求：客观公正，从产品功能、内容策略、粉丝数据、变现模式四个维度对比，给出差异化建议。',
  variable_desc = '{{our_brand}} - 我方品牌; {{competitor}} - 竞争对手; {{dimensions}} - 对比维度',
  remark = '竞品分析报告生成'
WHERE tenant_id = 1 AND scene = 'COMPETITOR' AND deleted = 0
  AND template_name REGEXP '^[?]+$';

-- 3) Re-insert missing WECHAT_ARTICLE seed if absent
INSERT INTO oa_ai_prompt_config
  (tenant_id, template_name, scene, prompt_content, variable_desc, temperature, status, remark)
SELECT 1, '公众号推文', 'WECHAT_ARTICLE',
  '你是一位公众号内容编辑。请根据以下主题生成一篇微信公众号文章：\n主题：{{topic}}\n核心观点：{{key_points}}\n目标读者：{{readers}}\n要求：标题有吸引力，正文1500-2000字，结构清晰，语言流畅，结尾有互动引导。',
  '{{topic}} - 文章主题; {{key_points}} - 核心观点; {{readers}} - 目标读者群体',
  0.70, 'ENABLED', '公众号推文生成'
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM oa_ai_prompt_config
  WHERE tenant_id = 1 AND scene = 'WECHAT_ARTICLE' AND deleted = 0
);
