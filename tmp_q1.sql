SELECT c.TABLE_NAME, c.COLUMN_NAME, c.COLUMN_TYPE, c.IS_NULLABLE, c.COLUMN_DEFAULT, c.EXTRA, c.COLUMN_KEY
FROM information_schema.COLUMNS c
WHERE c.TABLE_SCHEMA='wd'
AND c.TABLE_NAME IN (
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
ORDER BY c.TABLE_NAME, c.ORDINAL_POSITION;
