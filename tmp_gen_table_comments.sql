-- Generate table comments
SELECT CONCAT('ALTER TABLE ', t.TABLE_NAME, ' COMMENT=', QUOTE(
  CASE t.TABLE_NAME
    WHEN 'oa_account_status_log' THEN '账号状态日志表'
    WHEN 'oa_ai_model_config' THEN 'AI模型配置表'
    WHEN 'oa_ai_prompt_config' THEN 'AI提示词配置表'
    WHEN 'oa_aochuang_friend' THEN '奥创好友列表'
    WHEN 'oa_aochuang_message' THEN '奥创消息记录表'
    WHEN 'oa_aochuang_sync_cursor' THEN '奥创同步游标表'
    WHEN 'oa_aocreate_account' THEN '奥创账号表'
    WHEN 'oa_aocreate_api' THEN '奥创API配置表'
    WHEN 'oa_collect_config' THEN '采集配置表'
    WHEN 'oa_collect_log' THEN '采集日志表'
    WHEN 'oa_collect_task' THEN '采集任务表'
    WHEN 'oa_collector_account_bind' THEN '采集器账号绑定表'
    WHEN 'oa_config_keyword' THEN '关键词配置表'
    WHEN 'oa_content' THEN '内容表'
    WHEN 'oa_content_daily' THEN '内容日数据表'
    WHEN 'oa_content_data_import' THEN '内容数据导入表'
    WHEN 'oa_content_plan' THEN '内容计划表'
    WHEN 'oa_content_plan_competition' THEN '内容计划竞品分析表'
    WHEN 'oa_content_plan_step' THEN '内容计划步骤表'
    WHEN 'oa_content_publish_record' THEN '内容发布记录表'
    WHEN 'oa_custom_query' THEN '自定义查询表'
    WHEN 'oa_demo_item' THEN '演示项目表'
    WHEN 'oa_douyin_follower' THEN '抖音粉丝表'
    WHEN 'oa_douyin_video' THEN '抖音视频表'
    WHEN 'oa_external_work' THEN '外部作品表'
    WHEN 'oa_follower_daily' THEN '粉丝日数据表'
    WHEN 'oa_funnel_step' THEN '漏斗步骤表'
    WHEN 'oa_home_alert' THEN '首页告警表'
    WHEN 'oa_ip_group_anchor_rel' THEN 'IP组与主播关联表'
    WHEN 'oa_knowledge_base' THEN '知识库表'
    WHEN 'oa_kuaishou_video' THEN '快手视频表'
    WHEN 'oa_layout_import_job' THEN '版式导入任务表'
    WHEN 'oa_layout_style' THEN '版式样式表'
    WHEN 'oa_ops_anchor_rel' THEN '运营主播关联表'
    WHEN 'oa_order' THEN '订单表'
    WHEN 'oa_order_attribution' THEN '订单归因表'
    WHEN 'oa_perf_item_record' THEN '绩效项目记录表'
    WHEN 'oa_perf_record' THEN '绩效记录表'
    WHEN 'oa_perf_template_item' THEN '绩效模板项目表'
    WHEN 'oa_personal_wechat_daily_stats' THEN '个微日统计表'
    WHEN 'oa_platform_account_fan_group' THEN '平台账号粉丝分组表'
    WHEN 'oa_private_domain_conversion_bridge' THEN '私域转化桥接表'
    WHEN 'oa_production_content' THEN '生产内容表'
    WHEN 'oa_realname_intermediary' THEN '实名中间人表'
    WHEN 'oa_review_record' THEN '审核记录表'
    WHEN 'oa_sop_review' THEN 'SOP审核表'
    WHEN 'oa_threshold_config' THEN '阈值配置表'
    WHEN 'oa_typesetting_rule' THEN '排版规则表'
    WHEN 'oa_wechat_layout_template' THEN '微信版式模板表'
    WHEN 'oa_wechat_mp_article' THEN '微信公众号文章表'
    WHEN 'oa_wechat_mp_follower' THEN '微信公众号粉丝表'
    WHEN 'oa_wechat_official_cert_renewal' THEN '公众号认证续期表'
    WHEN 'oa_wechat_video_work' THEN '微信视频号作品表'
    WHEN 'oa_wework_daily_stats' THEN '企微日统计表'
    WHEN 'oa_wework_employee' THEN '企微员工表'
    WHEN 'oa_xiaohongshu_note' THEN '小红书笔记表'
    WHEN 'sys_dept' THEN '系统部门表'
    WHEN 'sys_login_log' THEN '系统登录日志表'
    WHEN 'sys_message' THEN '系统消息表'
    WHEN 'sys_metadata_entity' THEN '系统元数据实体表'
    WHEN 'sys_metadata_field' THEN '系统元数据字段表'
    WHEN 'sys_notification_event' THEN '系统通知事件表'
    WHEN 'sys_operation_log' THEN '系统操作日志表'
    WHEN 'sys_param' THEN '系统参数表'
    WHEN 'sys_permission' THEN '系统权限表'
    WHEN 'sys_role_permission' THEN '系统角色权限关联表'
  END
), ';') AS stmt
FROM information_schema.TABLES t
WHERE t.TABLE_SCHEMA='wd'
AND t.TABLE_NAME IN (
'oa_account_status_log','oa_ai_model_config','oa_ai_prompt_config',
'oa_aochuang_friend','oa_aochuang_message','oa_aochuang_sync_cursor',
'oa_aocreate_account','oa_aocreate_api','oa_collect_config',
'oa_collect_log','oa_collect_task','oa_collector_account_bind',
'oa_config_keyword','oa_content','oa_content_daily',
'oa_content_data_import','oa_content_plan','oa_content_plan_competition',
'oa_content_plan_step','oa_content_publish_record','oa_custom_query',
'oa_demo_item','oa_douyin_follower','oa_douyin_video',
'oa_external_work','oa_follower_daily','oa_funnel_step',
'oa_home_alert','oa_ip_group_anchor_rel','oa_knowledge_base',
'oa_kuaishou_video','oa_layout_import_job','oa_layout_style',
'oa_ops_anchor_rel','oa_order','oa_order_attribution',
'oa_perf_item_record','oa_perf_record','oa_perf_template_item',
'oa_personal_wechat_daily_stats','oa_platform_account_fan_group',
'oa_private_domain_conversion_bridge','oa_production_content',
'oa_realname_intermediary','oa_review_record','oa_sop_review',
'oa_threshold_config','oa_typesetting_rule','oa_wechat_layout_template',
'oa_wechat_mp_article','oa_wechat_mp_follower',
'oa_wechat_official_cert_renewal','oa_wechat_video_work',
'oa_wework_daily_stats','oa_wework_employee','oa_xiaohongshu_note',
'sys_dept','sys_login_log','sys_message',
'sys_metadata_entity','sys_metadata_field','sys_notification_event',
'sys_operation_log','sys_param','sys_permission','sys_role_permission'
)
ORDER BY t.TABLE_NAME;
