-- M8 配置管理种子数据
-- 幂等保护：每张表的 INSERT 均用 WHERE NOT EXISTS 包裹，重复执行安全

-- ================================================================
-- 1. AI 模型配置 (oa_ai_model_config)
-- ================================================================
INSERT INTO oa_ai_model_config
  (tenant_id, model_name, model_type, api_endpoint, api_key_encrypted, max_tokens, temperature, top_p, status, remark)
SELECT * FROM (
  SELECT 1, '通义千问-Turbo', 'QWEN',     'https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation', NULL, 8192, 0.70, 0.90, 'ENABLED', '阿里通义千问 Turbo 模型' UNION ALL
  SELECT 1, '通义千问-Plus',  'QWEN',     'https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation', NULL, 8192, 0.50, 0.85, 'ENABLED', '阿里通义千问 Plus 模型' UNION ALL
  SELECT 1, '文心一言-4.0',  'ERNIE',    'https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/completions_pro', NULL, 4096, 0.70, 0.95, 'ENABLED', '百度文心一言 4.0' UNION ALL
  SELECT 1, 'GPT-3.5-Turbo', 'GPT',      'https://api.openai.com/v1/chat/completions', NULL, 4096, 0.80, 1.00, 'DISABLED', 'OpenAI GPT-3.5 Turbo' UNION ALL
  SELECT 1, '智谱 GLM-4',    'GLM',      'https://open.bigmodel.cn/api/paas/v4/chat/completions', NULL, 8192, 0.70, 0.90, 'ENABLED', '智谱 AI GLM-4' UNION ALL
  SELECT 1, '月之暗面 Moonshot', 'MOONSHOT', 'https://api.moonshot.cn/v1/chat/completions', NULL, 32768, 0.60, 0.90, 'DISABLED', '月之暗面 Moonshot-v1-32k'
) AS t
WHERE NOT EXISTS (SELECT 1 FROM oa_ai_model_config WHERE tenant_id = 1 LIMIT 1);

-- ================================================================
-- 2. AI 提示词配置 (oa_ai_prompt_config)
-- ================================================================
INSERT INTO oa_ai_prompt_config
  (tenant_id, template_name, scene, prompt_content, variable_desc, temperature, status, remark)
SELECT * FROM (
  SELECT 1, '短视频文案生成', 'SHORT_VIDEO',
    '你是一位专业的短视频文案策划师。请根据以下产品信息生成一条吸引人的短视频文案：\n产品名称：{{product_name}}\n产品卖点：{{key_features}}\n目标受众：{{target_audience}}\n要求：文案简洁有力，不超过150字，突出核心卖点，结尾带上引导语。',
    '{{product_name}} - 产品名称; {{key_features}} - 核心卖点; {{target_audience}} - 目标受众',
    0.80, 'ENABLED', '短视频脚本文案生成' UNION ALL
  SELECT 1, '直播带货脚本', 'LIVE_SCRIPT',
    '你是一位经验丰富的直播带货主播助手。请为以下产品生成一段直播销售脚本：\n产品：{{product_name}}\n价格：{{price}}\n核心优势：{{advantages}}\n当前促销：{{promotion}}\n要求：语言亲切自然，突出性价比，包含互动引导词，时长约3分钟。',
    '{{product_name}} - 产品名称; {{price}} - 价格; {{advantages}} - 核心优势; {{promotion}} - 当前促销活动',
    0.90, 'ENABLED', '直播脚本生成' UNION ALL
  SELECT 1, '小红书种草笔记', 'XIAOHONGSHU',
    '你是小红书资深博主，请为以下内容生成一篇种草笔记：\n品类：{{category}}\n产品：{{product_name}}\n使用感受：{{experience}}\n要求：标题吸引眼球含emoji，正文分段清晰，结尾含话题标签，整体风格真实自然。',
    '{{category}} - 产品品类; {{product_name}} - 产品名称; {{experience}} - 使用感受',
    0.85, 'ENABLED', '小红书种草笔记生成' UNION ALL
  SELECT 1, '公众号推文', 'WECHAT_ARTICLE',
    '你是一位公众号内容编辑。请根据以下主题生成一篇微信公众号文章：\n主题：{{topic}}\n核心观点：{{key_points}}\n目标读者：{{readers}}\n要求：标题有吸引力，正文1500-2000字，结构清晰，语言流畅，结尾有互动引导。',
    '{{topic}} - 文章主题; {{key_points}} - 核心观点; {{readers}} - 目标读者群体',
    0.70, 'ENABLED', '公众号推文生成' UNION ALL
  SELECT 1, '数据分析报告摘要', 'DATA_ANALYSIS',
    '你是专业的数据分析师。请根据以下数据摘要生成分析解读：\n数据类型：{{data_type}}\n时间范围：{{time_range}}\n关键指标：{{metrics}}\n要求：客观分析数据趋势，指出异常点，给出可能的业务原因和改进建议，语言专业简洁。',
    '{{data_type}} - 数据类型; {{time_range}} - 时间范围; {{metrics}} - 关键指标数据',
    0.50, 'ENABLED', '数据分析报告摘要生成' UNION ALL
  SELECT 1, '周报月报生成', 'REPORT',
    '你是运营数据专员。请根据以下数据生成一份运营周报：\n时间周期：{{period}}\n团队：{{team}}\n核心数据：{{core_data}}\n要求：包含数据摘要、亮点成绩、问题分析、下周计划四个模块，格式规范，数据呈现清晰。',
    '{{period}} - 报告周期; {{team}} - 所属团队; {{core_data}} - 核心业务数据',
    0.60, 'ENABLED', '周报月报自动生成' UNION ALL
  SELECT 1, '竞品分析报告', 'COMPETITOR',
    '你是市场调研专家。请根据以下信息生成竞品分析报告：\n我方品牌：{{our_brand}}\n竞品：{{competitor}}\n对比维度：{{dimensions}}\n要求：客观公正，从产品功能、内容策略、粉丝数据、变现模式四个维度对比，给出差异化建议。',
    '{{our_brand}} - 我方品牌; {{competitor}} - 竞争对手; {{dimensions}} - 对比维度',
    0.60, 'ENABLED', '竞品分析报告生成'
) AS t
WHERE NOT EXISTS (SELECT 1 FROM oa_ai_prompt_config WHERE tenant_id = 1 LIMIT 1);

-- ================================================================
-- 3. 阈值配置 (oa_threshold_config)
-- ================================================================
INSERT INTO oa_threshold_config
  (tenant_id, metric_name, metric_type, platform_type, compare_operator, threshold_value, notify_methods, status, remark)
SELECT * FROM (
  SELECT 1, '视频播放量下降预警',   'PLAY_COUNT',    'DOUYIN',      'LT',  500000, 'DINGTALK,EMAIL', 'ENABLED', '抖音视频单日播放量低于50万时告警' UNION ALL
  SELECT 1, '粉丝增长率异常',       'FAN_GROWTH',    NULL,          'LT',  0.001,  'DINGTALK',       'ENABLED', '任意平台账号单日粉丝增长率低于0.1%告警' UNION ALL
  SELECT 1, '互动率红线',           'ENGAGEMENT',    'DOUYIN',      'LT',  0.02,   'DINGTALK',       'ENABLED', '抖音互动率低于2%触发预警' UNION ALL
  SELECT 1, '商品转化率预警',       'CONVERSION',    NULL,          'LT',  0.005,  'DINGTALK,EMAIL', 'ENABLED', '带货商品转化率低于0.5%预警' UNION ALL
  SELECT 1, '直播在线人数低谷',     'LIVE_ONLINE',   'DOUYIN',      'LT',  100,    'DINGTALK',       'ENABLED', '直播间在线人数低于100时提醒' UNION ALL
  SELECT 1, '评论负面情绪超标',     'NEGATIVE_RATE', NULL,          'GTE', 0.20,   'DINGTALK,EMAIL', 'ENABLED', '评论负面情绪比例超过20%预警' UNION ALL
  SELECT 1, '小红书笔记点赞下限',   'LIKE_COUNT',    'XIAOHONGSHU', 'LT',  500,    'DINGTALK',       'ENABLED', '小红书笔记24h点赞低于500时告警' UNION ALL
  SELECT 1, '内容发布频率不足',     'POST_FREQUENCY','DOUYIN',      'LT',  3,      'EMAIL',          'DISABLED', '每周发布视频数少于3条时提醒'
) AS t
WHERE NOT EXISTS (SELECT 1 FROM oa_threshold_config WHERE tenant_id = 1 LIMIT 1);

-- ================================================================
-- 4. 采集配置 (oa_collect_config)
-- 幂等：仅在各 scope 无数据时插入
-- ================================================================
INSERT INTO oa_collect_config
  (tenant_id, scope, config_name, sub_type, platform_type, collect_frequency, collect_method, api_url, request_method, collect_fields, status, remark)
SELECT * FROM (
  SELECT 1, 'INTERNAL', '抖音账号数据采集',   'ACCOUNT_METRICS', 'DOUYIN',      'HOURLY',   'API', 'https://open.douyin.com/api/v1/data/user/item_list', 'GET', 'fans_count,like_count,video_count', 'ENABLED', '采集抖音账号核心指标' UNION ALL
  SELECT 1, 'INTERNAL', '小红书账号数据采集', 'ACCOUNT_METRICS', 'XIAOHONGSHU', 'DAILY',    'API', 'https://api.xiaohongshu.com/v1/user/info',           'GET', 'fans_count,note_count,like_count',  'ENABLED', '采集小红书账号指标' UNION ALL
  SELECT 1, 'INTERNAL', '视频内容数据采集',   'CONTENT_METRICS', 'DOUYIN',      'HOURLY',   'API', 'https://open.douyin.com/api/v1/data/video/list',     'GET', 'play_count,like_count,comment_count,share_count', 'ENABLED', '采集视频播放互动数据' UNION ALL
  SELECT 1, 'INTERNAL', '直播数据采集',       'LIVE_METRICS',    'DOUYIN',      'REALTIME', 'API', 'https://open.douyin.com/api/v1/live/room/stats',     'GET', 'online_count,total_view,gift_count', 'ENABLED', '实时采集直播间数据'
) AS t
WHERE NOT EXISTS (SELECT 1 FROM oa_collect_config WHERE tenant_id = 1 AND scope = 'INTERNAL' LIMIT 1);

INSERT INTO oa_collect_config
  (tenant_id, scope, config_name, sub_type, platform_type, collect_frequency, collect_method, api_url, request_method, collect_fields, status, remark)
SELECT * FROM (
  SELECT 1, 'EXTERNAL', '竞品抖音账号监控', 'COMPETITOR_MONITOR', 'DOUYIN',      'DAILY', 'CRAWLER', NULL, 'GET', 'fans_count,avg_play,video_count',   'ENABLED', '监控主要竞品抖音账号数据' UNION ALL
  SELECT 1, 'EXTERNAL', '竞品小红书监控',   'COMPETITOR_MONITOR', 'XIAOHONGSHU', 'DAILY', 'CRAWLER', NULL, 'GET', 'fans_count,note_count,engage_rate', 'ENABLED', '监控竞品小红书运营数据' UNION ALL
  SELECT 1, 'EXTERNAL', '热点话题采集',     'HOT_TOPIC',          NULL,          'HOURLY','API',     'https://api.tikhub.io/v1/douyin/trending', 'GET', 'topic_name,play_count,video_count', 'ENABLED', '采集平台热门话题数据'
) AS t
WHERE NOT EXISTS (SELECT 1 FROM oa_collect_config WHERE tenant_id = 1 AND scope = 'EXTERNAL' LIMIT 1);

INSERT INTO oa_collect_config
  (tenant_id, scope, config_name, sub_type, platform_type, collect_frequency, collect_method, api_url, request_method, response_mapping, collect_fields, status, remark)
SELECT * FROM (
  SELECT 1, 'EXTERNAL_SOURCE', '第三方数据服务-新榜', 'THIRD_PARTY', NULL,     'DAILY', 'API', 'https://api.newrank.cn/v2/account/basic',         'POST', '{"total":".data.total","list":".data.list"}', 'account_id,fans,avg_play,score',      'ENABLED',  '新榜账号影响力数据' UNION ALL
  SELECT 1, 'EXTERNAL_SOURCE', '第三方数据服务-飞瓜', 'THIRD_PARTY', 'DOUYIN', 'DAILY', 'API', 'https://api.feigua.cn/douyin/author/info',          'GET',  '{"total":".count","list":".items"}',          'uid,fans_count,like_total,video_count','ENABLED',  '飞瓜抖音账号数据' UNION ALL
  SELECT 1, 'EXTERNAL_SOURCE', '电商平台商品数据',     'ECOMMERCE',   NULL,     'DAILY', 'API', 'https://api.shop.example.com/v1/products',         'GET',  '{"total":".data.total","list":".data.items"}', 'product_id,title,price,sales_count',  'DISABLED', '电商商品销售数据源（待配置）'
) AS t
WHERE NOT EXISTS (SELECT 1 FROM oa_collect_config WHERE tenant_id = 1 AND scope = 'EXTERNAL_SOURCE' LIMIT 1);

INSERT INTO oa_collect_config
  (tenant_id, scope, config_name, sub_type, platform_type, collect_frequency, collect_method, api_url, request_method, collect_fields, status, remark)
SELECT * FROM (
  SELECT 1, 'GENERAL', '抖音小店订单采集', 'ORDER', 'DOUYIN',   'HOURLY', 'API', 'https://open.douyin.com/api/v1/shop/order/list',         'GET',  'order_id,product_id,amount,status,create_time',    'ENABLED',  '抖音小店订单数据同步' UNION ALL
  SELECT 1, 'GENERAL', '淘宝直播订单采集', 'ORDER', 'TAOBAO',   'HOURLY', 'API', 'https://eco.taobao.com/router/rest',                      'POST', 'order_id,item_id,payment,status,created',          'ENABLED',  '淘宝直播联动订单采集' UNION ALL
  SELECT 1, 'GENERAL', '京东联盟订单采集', 'ORDER', 'JD',       'DAILY',  'API', 'https://api.jd.com/routerjson',                           'POST', 'order_id,sku_id,actual_fee,order_time,status',     'DISABLED', '京东联盟佣金订单（暂停）' UNION ALL
  SELECT 1, 'GENERAL', '快手小店订单采集', 'ORDER', 'KUAISHOU', 'HOURLY', 'API', 'https://open.kuaishou.com/openapi/shop/order/list',       'GET',  'order_id,item_id,total_amount,state,created_at',   'ENABLED',  '快手小店订单同步'
) AS t
WHERE NOT EXISTS (SELECT 1 FROM oa_collect_config WHERE tenant_id = 1 AND scope = 'GENERAL' LIMIT 1);
