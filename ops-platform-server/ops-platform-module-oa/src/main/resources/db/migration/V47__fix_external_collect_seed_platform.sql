-- 修正 V43 外部采集种子数据的 platform_type，使其符合 dict_third_platform（NEWRANK/FEIGUA）
-- 页面 ExternalCollectConfig.vue 使用 dict_third_platform 筛选/展示

UPDATE oa_collect_config
SET platform_type = 'FEIGUA',
    config_name   = '飞瓜抖音竞品监控',
    api_url       = 'https://api.feigua.cn/douyin/author/info',
    collect_method = 'API',
    remark        = '飞瓜数据 - 竞品抖音账号监控'
WHERE tenant_id = 1 AND scope = 'EXTERNAL' AND sub_type = 'COMPETITOR_MONITOR' AND platform_type = 'DOUYIN';

UPDATE oa_collect_config
SET platform_type = 'NEWRANK',
    config_name   = '新榜小红书竞品监控',
    api_url       = 'https://api.newrank.cn/v2/account/basic',
    collect_method = 'API',
    remark        = '新榜 - 竞品小红书运营数据'
WHERE tenant_id = 1 AND scope = 'EXTERNAL' AND sub_type = 'COMPETITOR_MONITOR' AND platform_type = 'XIAOHONGSHU';

UPDATE oa_collect_config
SET platform_type = 'FEIGUA',
    config_name   = '飞瓜热点话题采集',
    remark        = '飞瓜 - 平台热门话题数据'
WHERE tenant_id = 1 AND scope = 'EXTERNAL' AND sub_type = 'HOT_TOPIC';
