#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Generate V126 migration script for remaining table/column comments."""

import pymysql
import re

conn = pymysql.connect(
    host='101.37.161.136',
    port=3306,
    user='shenyu',
    password='Zhangwu+123456',
    database='wd',
    charset='utf8mb4'
)

# Table comments mapping
table_comments = {
    'oa_account_status_log': '账号状态日志表',
    'oa_ai_model_config': 'AI模型配置表',
    'oa_ai_prompt_config': 'AI提示词配置表',
    'oa_aochuang_friend': '奥创好友列表',
    'oa_aochuang_message': '奥创消息记录表',
    'oa_aochuang_sync_cursor': '奥创同步游标表',
    'oa_aocreate_account': '奥创账号表',
    'oa_aocreate_api': '奥创API配置表',
    'oa_collect_config': '采集配置表',
    'oa_collect_log': '采集日志表',
    'oa_collect_task': '采集任务表',
    'oa_collector_account_bind': '采集器账号绑定表',
    'oa_config_keyword': '关键词配置表',
    'oa_content': '内容表',
    'oa_content_daily': '内容日数据表',
    'oa_content_data_import': '内容数据导入表',
    'oa_content_plan': '内容计划表',
    'oa_content_plan_competition': '内容计划竞品分析表',
    'oa_content_plan_step': '内容计划步骤表',
    'oa_content_publish_record': '内容发布记录表',
    'oa_custom_query': '自定义查询表',
    'oa_demo_item': '演示项目表',
    'oa_douyin_follower': '抖音粉丝表',
    'oa_douyin_video': '抖音视频表',
    'oa_external_work': '外部作品表',
    'oa_follower_daily': '粉丝日数据表',
    'oa_funnel_step': '漏斗步骤表',
    'oa_home_alert': '首页告警表',
    'oa_ip_group_anchor_rel': 'IP组与主播关联表',
    'oa_knowledge_base': '知识库表',
    'oa_kuaishou_video': '快手视频表',
    'oa_layout_import_job': '版式导入任务表',
    'oa_layout_style': '版式样式表',
    'oa_ops_anchor_rel': '运营主播关联表',
    'oa_order': '订单表',
    'oa_order_attribution': '订单归因表',
    'oa_perf_item_record': '绩效项目记录表',
    'oa_perf_record': '绩效记录表',
    'oa_perf_template_item': '绩效模板项目表',
    'oa_personal_wechat_daily_stats': '个微日统计表',
    'oa_platform_account_fan_group': '平台账号粉丝分组表',
    'oa_private_domain_conversion_bridge': '私域转化桥接表',
    'oa_production_content': '生产内容表',
    'oa_realname_intermediary': '实名中间人表',
    'oa_review_record': '审核记录表',
    'oa_sop_review': 'SOP审核表',
    'oa_threshold_config': '阈值配置表',
    'oa_typesetting_rule': '排版规则表',
    'oa_wechat_layout_template': '微信版式模板表',
    'oa_wechat_mp_article': '微信公众号文章表',
    'oa_wechat_mp_follower': '微信公众号粉丝表',
    'oa_wechat_official_cert_renewal': '公众号认证续期表',
    'oa_wechat_video_work': '微信视频号作品表',
    'oa_wework_daily_stats': '企微日统计表',
    'oa_wework_employee': '企微员工表',
    'oa_xiaohongshu_note': '小红书笔记表',
    'sys_dept': '系统部门表',
    'sys_login_log': '系统登录日志表',
    'sys_message': '系统消息表',
    'sys_metadata_entity': '系统元数据实体表',
    'sys_metadata_field': '系统元数据字段表',
    'sys_notification_event': '系统通知事件表',
    'sys_operation_log': '系统操作日志表',
    'sys_param': '系统参数表',
    'sys_permission': '系统权限表',
    'sys_role_permission': '系统角色权限关联表',
}

# Additional specific column comments
specific_column_comments = {
    'model_name': '模型名称', 'model_id': '模型ID', 'model_type': '模型类型',
    'api_endpoint': 'API端点地址', 'max_tokens': '最大Token数',
    'temperature': '温度参数', 'top_p': 'Top-P采样',
    'prompt_content': '提示词内容', 'variable_desc': '变量描述',
    'content_type': '内容类型', 'document_type': '文档类型',
    'personal_wechat_id': '个人微信ID',
    'aochuang_wechat_account_id': '奥创微信账号ID',
    'aochuang_friend_id': '奥创好友ID', 'aochuang_message_id': '奥创消息ID',
    'msg_type': '消息类型', 'direction': '消息方向',
    'message_time': '消息时间',
    'sync_type': '同步类型', 'cursor_value': '游标值',
    'aocreate_api_id': '奥创API配置ID', 'aocreate_account_id': '奥创账号ID',
    'last_device_sync_at': '最后设备同步时间',
    'daily_quota': '每日配额', 'current_usage': '当前用量',
    'oa_account_id': '运营平台账号ID', 'collector_account_id': '采集器账号ID',
    'bind_status': '绑定状态',
    'keyword': '关键词', 'match_type': '匹配类型',
    'word_type': '词汇类型',
    'content_id': '内容ID', 'content_title': '内容标题',
    'content_url': '内容链接', 'content_type': '内容类型',
    'content_category': '内容分类', 'content_status': '内容状态',
    'data_source': '数据来源', 'import_status': '导入状态',
    'import_time': '导入时间', 'import_count': '导入数量',
    'row_count': '行数', 'column_count': '列数',
    'raw_data': '原始数据', 'parsed_data': '解析后数据',
    'plan_id': '计划ID', 'plan_name': '计划名称',
    'step_index': '步骤序号', 'step_name': '步骤名称',
    'step_status': '步骤状态', 'step_type': '步骤类型',
    'competitor_name': '竞品名称', 'competitor_data': '竞品数据',
    'publish_status': '发布状态', 'publish_platform': '发布平台',
    'publish_url': '发布链接', 'publish_result': '发布结果',
    'query_name': '查询名称', 'query_sql': '查询SQL',
    'query_config': '查询配置', 'query_result': '查询结果',
    'demo_name': '演示名称', 'demo_data': '演示数据',
    'video_id': '视频ID', 'video_url': '视频URL',
    'video_title': '视频标题', 'video_desc': '视频描述',
    'video_status': '视频状态', 'video_cover': '视频封面',
    'author_id': '作者ID', 'author_name': '作者名称',
    'author_avatar': '作者头像', 'author_url': '作者主页',
    'work_id': '作品ID', 'work_title': '作品标题',
    'work_url': '作品链接', 'work_type': '作品类型',
    'work_status': '作品状态', 'work_cover': '作品封面',
    'date': '日期', 'stat_date': '统计日期',
    'delta_followers': '粉丝增量', 'delta_likes': '点赞增量',
    'delta_comments': '评论增量', 'delta_shares': '分享增量',
    'step_number': '步骤编号', 'step_desc': '步骤描述',
    'alert_type': '告警类型', 'alert_level': '告警级别',
    'alert_message': '告警消息', 'alert_status': '告警状态',
    'ip_group_id': 'IP组ID', 'anchor_id': '主播ID',
    'anchor_name': '主播名称', 'anchor_account_id': '主播账号ID',
    'kb_name': '知识库名称', 'kb_type': '知识库类型',
    'kb_content': '知识库内容', 'kb_source': '知识库来源',
    'layout_id': '版式ID', 'layout_name': '版式名称',
    'layout_type': '版式类型', 'layout_data': '版式数据',
    'style_name': '样式名称', 'style_data': '样式数据',
    'style_type': '样式类型', 'style_config': '样式配置',
    'job_id': '作业ID', 'job_type': '作业类型',
    'job_status': '作业状态', 'job_result': '作业结果',
    'order_id': '订单ID', 'order_no': '订单号',
    'order_type': '订单类型', 'order_status': '订单状态',
    'order_amount': '订单金额', 'order_time': '订单时间',
    'attribution_type': '归因类型', 'attribution_source': '归因来源',
    'attribution_value': '归因值', 'attribution_rate': '归因比率',
    'perf_id': '绩效ID', 'perf_type': '绩效类型',
    'perf_score': '绩效分数', 'perf_status': '绩效状态',
    'item_name': '项目名称', 'item_type': '项目类型',
    'item_value': '项目值', 'item_score': '项目得分',
    'item_weight': '项目权重', 'item_target': '项目目标',
    'threshold_min': '阈值下限', 'threshold_max': '阈值上限',
    'threshold_value': '阈值', 'threshold_type': '阈值类型',
    'rule_name': '规则名称', 'rule_type': '规则类型',
    'rule_content': '规则内容', 'rule_config': '规则配置',
    'template_content': '模板内容', 'template_config': '模板配置',
    'template_status': '模板状态', 'template_data': '模板数据',
    'article_id': '文章ID', 'article_title': '文章标题',
    'article_url': '文章链接', 'article_content': '文章内容',
    'article_status': '文章状态', 'article_author': '文章作者',
    'openid': 'OpenID', 'unionid': 'UnionID',
    'subscribe_time': '关注时间', 'unsubscribe_time': '取关时间',
    'subscribe_status': '关注状态', 'language': '语言',
    'country': '国家', 'province': '省份', 'city': '城市',
    'cert_type': '认证类型', 'cert_status': '认证状态',
    'cert_no': '证书编号', 'cert_start': '认证开始日期',
    'cert_end': '认证结束日期', 'renewal_status': '续期状态',
    'renewal_time': '续期时间',
    'note_id': '笔记ID', 'note_title': '笔记标题',
    'note_url': '笔记链接', 'note_content': '笔记内容',
    'note_type': '笔记类型', 'note_status': '笔记状态',
    'employee_id': '员工ID', 'employee_name': '员工名称',
    'employee_status': '员工状态', 'department_name': '部门名称',
    'intermediary_name': '中间人名称', 'intermediary_id': '中间人ID',
    'bridge_type': '桥接类型', 'bridge_config': '桥接配置',
    'bridge_status': '桥接状态', 'source_account': '源账号',
    'target_account': '目标账号',
    'review_status': '审核状态', 'reviewer_id': '审核者ID',
    'reviewer_name': '审核者名称', 'review_time': '审核时间',
    'review_comment': '审核意见', 'review_result': '审核结果',
    'sop_id': 'SOP ID', 'sop_name': 'SOP名称',
    'sop_status': 'SOP状态', 'sop_type': 'SOP类型',
    'entity_code': '实体编码', 'physical_table': '物理表名',
    'entity_id': '实体ID', 'field_code': '字段编码',
    'column_name': '列名', 'data_type': '数据类型',
    'query_condition_type': '查询条件类型',
    'dict_type': '字典类型', 'selector_config': '选择器配置',
    'event_type': '事件类型', 'biz_key': '业务键',
    'recipient_user_id': '接收用户ID',
    'username': '用户名', 'action': '操作动作',
    'level': '级别', 'request_params': '请求参数',
    'response_body': '响应体', 'ip': 'IP地址',
    'param_name': '参数名称', 'param_key': '参数键',
    'param_value': '参数值', 'param_type': '参数类型',
    'category': '分类',
    'code': '权限编码', 'permission_id': '权限ID',
    'role_id': '角色ID',
    'read_time': '阅读时间',
    'fan_group_id': '粉丝分组ID', 'fan_group_name': '粉丝分组名称',
    'group_name': '分组名称', 'group_id': '分组ID',
    'wechat_account_id': '微信账号ID',
    'mp_account_id': '公众号账号ID',
    'wework_account_id': '企微账号ID',
}

# Column comment mapping (generic, used across tables)
column_comments = {
    'id': '主键ID',
    'tenant_id': '租户ID',
    'creator': '创建者',
    'create_time': '创建时间',
    'updater': '更新者',
    'update_time': '更新时间',
    'deleted': '逻辑删除标记(0=未删除,1=已删除)',
    'status': '状态',
    'remark': '备注',
    'name': '名称',
    'type': '类型',
    'platform_type': '平台类型',
    'account_id': '账号ID',
    'account_name': '账号名称',
    'description': '描述',
    'config_json': '配置JSON',
    'extra_json': '扩展JSON',
    'data_json': '数据JSON',
    'result_json': '结果JSON',
    'params_json': '参数JSON',
    'settings_json': '设置JSON',
    'properties_json': '属性JSON',
    'metadata_json': '元数据JSON',
    'content': '内容',
    'title': '标题',
    'url': 'URL地址',
    'cover_url': '封面URL',
    'source': '来源',
    'method': '方法',
    'frequency': '频率',
    'cron': 'Cron表达式',
    'start_at': '开始时间',
    'end_at': '结束时间',
    'last_run_at': '最后运行时间',
    'next_run_at': '下次运行时间',
    'run_count': '运行次数',
    'fail_count': '失败次数',
    'error_message': '错误信息',
    'duration_ms': '耗时(毫秒)',
    'record_count': '记录数',
    'api_url': 'API地址',
    'api_key_encrypted': 'API密钥(加密)',
    'api_key': 'API密钥',
    'app_id': '应用ID',
    'app_secret_encrypted': '应用密钥(加密)',
    'token_encrypted': '令牌(加密)',
    'cookie': 'Cookie',
    'cookie_encrypted': 'Cookie(加密)',
    'auth_token_encrypted': '认证令牌(加密)',
    'field_mapping': '字段映射',
    'request_method': '请求方法',
    'request_params': '请求参数',
    'response_mapping': '响应映射',
    'collect_fields': '采集字段',
    'conn_status': '连接状态',
    'is_live': '是否直播',
    'db_host': '数据库主机',
    'db_port': '数据库端口',
    'db_name': '数据库名',
    'db_username': '数据库用户名',
    'db_password_encrypted': '数据库密码(加密)',
    'table_name': '表名',
    'sync_mode': '同步模式',
    'collect_frequency': '采集频率',
    'collect_method': '采集方式',
    'scope': '范围',
    'config_name': '配置名称',
    'sub_type': '子类型',
    'account_identifier': '账号标识',
    'data_type': '数据类型',
    'task_id': '任务ID',
    'task_name': '任务名称',
    'stat_date': '统计日期',
    'follower_count': '粉丝数',
    'like_count': '点赞数',
    'comment_count': '评论数',
    'share_count': '分享数',
    'play_count': '播放数',
    'collect_count': '收藏数',
    'download_count': '下载数',
    'forward_count': '转发数',
    'read_count': '阅读数',
    'view_count': '浏览数',
    'new_follower_count': '新增粉丝数',
    'unfollow_count': '取关数',
    'total_follower_count': '总粉丝数',
    'revenue': '收入',
    'amount': '金额',
    'price': '价格',
    'cost': '成本',
    'budget': '预算',
    'roi': '投资回报率',
    'conversion_rate': '转化率',
    'click_rate': '点击率',
    'engagement_rate': '互动率',
    'completion_rate': '完成率',
    'publish_time': '发布时间',
    'publish_at': '发布时间',
    'synced_at': '同步时间',
    'last_sync_at': '最后同步时间',
    'video_id': '视频ID',
    'video_url': '视频URL',
    'duration': '时长',
    'width': '宽度',
    'height': '高度',
    'size': '大小',
    'format': '格式',
    'tags': '标签',
    'category': '分类',
    'keyword': '关键词',
    'platform': '平台',
    'platform_account_id': '平台账号ID',
    'external_id': '外部ID',
    'external_work_id': '外部作品ID',
    'wechat_id': '微信ID',
    'nickname': '昵称',
    'avatar': '头像',
    'alias': '别名',
    'gender': '性别',
    'phone': '电话',
    'email': '邮箱',
    'address': '地址',
    'company': '公司',
    'department': '部门',
    'position': '职位',
    'level': '级别',
    'parent_id': '父级ID',
    'sort_order': '排序',
    'sort': '排序',
    'priority': '优先级',
    'enabled': '是否启用',
    'is_default': '是否默认',
    'is_enabled': '是否启用',
    'is_active': '是否激活',
    'is_deleted': '是否删除',
    'version': '版本',
    'template_id': '模板ID',
    'template_name': '模板名称',
    'template_type': '模板类型',
    'scene': '场景',
    'scene_type': '场景类型',
    'content_type': '内容类型',
    'media_type': '媒体类型',
    'biz_type': '业务类型',
    'biz_id': '业务ID',
    'order_no': '订单号',
    'order_id': '订单ID',
    'order_type': '订单类型',
    'order_status': '订单状态',
    'pay_status': '支付状态',
    'pay_time': '支付时间',
    'pay_amount': '支付金额',
    'pay_method': '支付方式',
    'refund_status': '退款状态',
    'refund_amount': '退款金额',
    'refund_time': '退款时间',
    'settle_status': '结算状态',
    'settle_amount': '结算金额',
    'settle_time': '结算时间',
    'attribution_type': '归因类型',
    'attribution_source': '归因来源',
    'attribution_value': '归因值',
    'threshold_min': '阈值下限',
    'threshold_max': '阈值上限',
    'threshold_value': '阈值',
    'alert_type': '告警类型',
    'alert_level': '告警级别',
    'alert_message': '告警消息',
    'alert_status': '告警状态',
    'trigger_time': '触发时间',
    'resolve_time': '解决时间',
    'resolved_by': '解决者',
    'resolve_note': '解决备注',
    'rule_type': '规则类型',
    'rule_name': '规则名称',
    'rule_content': '规则内容',
    'condition_json': '条件JSON',
    'action_json': '动作JSON',
    'notify_type': '通知类型',
    'notify_target': '通知目标',
    'notify_content': '通知内容',
    'notify_time': '通知时间',
    'notify_status': '通知状态',
    'user_id': '用户ID',
    'user_name': '用户名',
    'role_id': '角色ID',
    'role_name': '角色名称',
    'permission_id': '权限ID',
    'dept_id': '部门ID',
    'dept_name': '部门名称',
    'login_ip': '登录IP',
    'login_time': '登录时间',
    'login_location': '登录地点',
    'browser': '浏览器',
    'os': '操作系统',
    'user_agent': '用户代理',
    'operation': '操作',
    'operation_type': '操作类型',
    'module': '模块',
    'module_name': '模块名称',
    'target_type': '目标类型',
    'target_id': '目标ID',
    'before_value': '变更前值',
    'after_value': '变更后值',
    'diff': '变更差异',
    'reason': '原因',
    'result': '结果',
    'message': '消息',
    'code': '编码',
    'label': '标签',
    'value': '值',
    'key': '键',
    'field_name': '字段名',
    'field_type': '字段类型',
    'field_label': '字段标签',
    'required': '是否必填',
    'default_value': '默认值',
    'options': '选项',
    'entity_name': '实体名称',
    'entity_type': '实体类型',
    'display_name': '显示名称',
    'icon': '图标',
    'path': '路径',
    'component': '组件',
    'redirect': '重定向',
    'hidden': '是否隐藏',
    'keep_alive': '是否缓存',
    'affix': '是否固定',
    'always_show': '是否总是显示',
    'active_menu': '激活菜单',
    'access_control': '访问控制',
    'white_list': '白名单',
    'black_list': '黑名单',
    'ip_range': 'IP范围',
    'start_date': '开始日期',
    'end_date': '结束日期',
    'date_range': '日期范围',
    'time_range': '时间范围',
    'period': '周期',
    'period_type': '周期类型',
    'year': '年份',
    'month': '月份',
    'week': '周',
    'day': '日',
    'hour': '小时',
    'minute': '分钟',
    'second': '秒',
    'count': '计数',
    'total': '总计',
    'average': '平均值',
    'max_value': '最大值',
    'min_value': '最小值',
    'sum_value': '合计值',
    'ratio': '比率',
    'percent': '百分比',
    'delta': '增量',
    'growth_rate': '增长率',
    'yoy': '同比',
    'mom': '环比',
    'base_value': '基准值',
    'target_value': '目标值',
    'actual_value': '实际值',
    'progress': '进度',
    'plan_id': '计划ID',
    'plan_name': '计划名称',
    'plan_type': '计划类型',
    'step_id': '步骤ID',
    'step_name': '步骤名称',
    'step_index': '步骤序号',
    'step_type': '步骤类型',
    'step_status': '步骤状态',
    'deadline': '截止时间',
    'completed_at': '完成时间',
    'completed_by': '完成者',
    'assignee': '负责人',
    'assignee_id': '负责人ID',
    'review_status': '审核状态',
    'reviewer': '审核者',
    'reviewer_id': '审核者ID',
    'review_time': '审核时间',
    'review_comment': '审核意见',
    'review_result': '审核结果',
    'approval_status': '审批状态',
    'approver': '审批者',
    'approval_time': '审批时间',
    'approval_comment': '审批意见',
    'import_status': '导入状态',
    'import_time': '导入时间',
    'import_count': '导入数量',
    'success_count': '成功数量',
    'fail_reason': '失败原因',
    'file_url': '文件URL',
    'file_name': '文件名',
    'file_type': '文件类型',
    'file_size': '文件大小',
    'batch_no': '批次号',
    'batch_id': '批次ID',
    'job_id': '任务ID',
    'job_type': '任务类型',
    'job_status': '任务状态',
    'progress_rate': '进度比率',
    'retry_count': '重试次数',
    'max_retry': '最大重试次数',
    'timeout': '超时时间',
    'expire_time': '过期时间',
    'effective_time': '生效时间',
    'start_time': '开始时间',
    'end_time': '结束时间',
}

# Tables to process
tables = list(table_comments.keys())

cursor = conn.cursor()

sql_lines = []
sql_lines.append('-- V126: Add comments for remaining tables and columns')
sql_lines.append('-- Generated by script')
sql_lines.append('')

for table in tables:
    # Table comment
    sql_lines.append(f"-- ========== {table} ==========")
    sql_lines.append(f"ALTER TABLE {table} COMMENT='{table_comments[table]}';")
    
    # Get columns
    cursor.execute("""
        SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT, EXTRA, COLUMN_KEY
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA='wd' AND TABLE_NAME=%s
        ORDER BY ORDINAL_POSITION
    """, (table,))
    
    columns = cursor.fetchall()
    for col_name, col_type, is_nullable, col_default, extra, col_key in columns:
        # Skip if column already has a comment (check current comment)
        cursor.execute("""
            SELECT COLUMN_COMMENT FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA='wd' AND TABLE_NAME=%s AND COLUMN_NAME=%s
        """, (table, col_name))
        existing_comment = cursor.fetchone()[0]
        if existing_comment and existing_comment.strip():
            continue
        
        # Build comment text
        comment = None
        # Check specific comments first
        if col_name in specific_column_comments:
            comment = specific_column_comments[col_name]
        # Check generic match
        elif col_name in column_comments:
            comment = column_comments[col_name]
        else:
            # Try to derive from column name patterns
            parts = col_name.split('_')
            # Common suffixes
            if col_name.endswith('_id'):
                prefix = '_'.join(parts[:-1])
                # Map common prefixes
                id_map = {
                    'account': '账号', 'tenant': '租户', 'user': '用户',
                    'role': '角色', 'dept': '部门', 'task': '任务',
                    'plan': '计划', 'template': '模板', 'order': '订单',
                    'content': '内容', 'video': '视频', 'follower': '粉丝',
                    'friend': '好友', 'message': '消息', 'config': '配置',
                    'keyword': '关键词', 'metric': '指标', 'funnel': '漏斗',
                    'author': '作者', 'company': '公司', 'ip_group': 'IP组',
                    'anchor': '主播', 'employee': '员工', 'permission': '权限',
                    'notification': '通知', 'alert': '告警', 'threshold': '阈值',
                    'layout': '版式', 'style': '样式', 'article': '文章',
                    'note': '笔记', 'work': '作品', 'job': '任务',
                    'record': '记录', 'log': '日志', 'stats': '统计',
                    'bridge': '桥接', 'conversion': '转化', 'domain': '域',
                    'intermediary': '中间人', 'realname': '实名',
                    'competition': '竞品', 'step': '步骤',
                    'weixin': '微信', 'wechat': '微信', 'wework': '企微',
                    'douyin': '抖音', 'kuaishou': '快手', 'xiaohongshu': '小红书',
                    'aochuang': '奥创', 'aocreate': '奥创',
                    'mp': '公众号', 'official': '官方', 'cert': '认证',
                    'renewal': '续期', 'fan': '粉丝', 'group': '分组',
                    'daily': '日', 'publish': '发布', 'import': '导入',
                    'collect': '采集', 'collector': '采集器', 'bind': '绑定',
                    'ops': '运营', 'external': '外部', 'demo': '演示',
                    'item': '项目', 'production': '生产', 'review': '审核',
                    'perf': '绩效', 'sop': 'SOP', 'ai': 'AI',
                    'model': '模型', 'prompt': '提示词',
                    'parent': '父级', 'base': '基础',
                    'fan_group': '粉丝分组', 'private_domain': '私域',
                }
                if prefix in id_map:
                    comment = f'{id_map[prefix]}ID'
                else:
                    comment = f'{prefix.replace("_", " ").title()}ID'
            elif col_name.endswith('_time') or col_name.endswith('_at'):
                action = '_'.join(parts[:-1]).replace('_', ' ')
                action_map = {
                    'create': '创建', 'update': '更新', 'delete': '删除',
                    'login': '登录', 'logout': '登出', 'publish': '发布',
                    'sync': '同步', 'last_sync': '最后同步',
                    'start': '开始', 'end': '结束', 'deadline': '截止',
                    'complete': '完成', 'completed': '完成', 'resolve': '解决',
                    'trigger': '触发', 'notify': '通知', 'import': '导入',
                    'run': '运行', 'last_run': '最后运行', 'next_run': '下次运行',
                    'pay': '支付', 'settle': '结算', 'refund': '退款',
                    'expire': '过期', 'effective': '生效',
                    'message': '消息', 'alert': '告警',
                    'device_sync': '设备同步', 'last_device_sync': '最后设备同步',
                    'cert_renewal': '认证续期',
                }
                time_str = '_'.join(parts[:-1])
                if time_str in action_map:
                    comment = f'{action_map[time_str]}时间'
                else:
                    comment = f'{time_str}时间'
            elif col_name.endswith('_encrypted'):
                prefix = '_'.join(parts[:-1])
                enc_map = {
                    'api_key': 'API密钥', 'app_secret': '应用密钥',
                    'token': '令牌', 'cookie': 'Cookie',
                    'auth_token': '认证令牌', 'db_password': '数据库密码',
                    'api_config': 'API配置',
                }
                if prefix in enc_map:
                    comment = f'{enc_map[prefix]}(加密存储)'
                else:
                    comment = f'{prefix}(加密存储)'
            elif col_name.endswith('_json'):
                prefix = '_'.join(parts[:-1])
                json_map = {
                    'config': '配置', 'extra': '扩展', 'data': '数据',
                    'result': '结果', 'params': '参数', 'settings': '设置',
                    'properties': '属性', 'metadata': '元数据',
                    'condition': '条件', 'action': '动作',
                    'field_mapping': '字段映射', 'response_mapping': '响应映射',
                    'request_params': '请求参数', 'collect_fields': '采集字段',
                    'positions': '位置',
                }
                if prefix in json_map:
                    comment = f'{json_map[prefix]}JSON数据'
                else:
                    comment = f'{prefix}JSON数据'
            elif col_name.endswith('_count'):
                prefix = '_'.join(parts[:-1])
                count_map = {
                    'follower': '粉丝', 'like': '点赞', 'comment': '评论',
                    'share': '分享', 'play': '播放', 'collect': '收藏',
                    'download': '下载', 'forward': '转发', 'read': '阅读',
                    'view': '浏览', 'new_follower': '新增粉丝',
                    'unfollow': '取关', 'total_follower': '总粉丝',
                    'record': '记录', 'run': '运行', 'fail': '失败',
                    'success': '成功', 'import': '导入', 'retry': '重试',
                }
                if prefix in count_map:
                    comment = f'{count_map[prefix]}数量'
                else:
                    comment = f'{prefix}数量'
            elif col_name.startswith('is_'):
                suffix = col_name[3:]
                bool_map = {
                    'default': '是否默认', 'enabled': '是否启用',
                    'active': '是否激活', 'deleted': '是否删除',
                    'live': '是否直播', 'required': '是否必填',
                    'hidden': '是否隐藏',
                }
                if suffix in bool_map:
                    comment = bool_map[suffix]
                else:
                    comment = f'是否{suffix}'
            elif col_name == 'deleted':
                comment = '逻辑删除标记(0=未删除,1=已删除)'
            elif col_name == 'positions_json':
                comment = '位置JSON数据'
            
        if comment is None:
            # Fallback: use column name as-is with basic translation
            comment = col_name  # Keep English name as comment
        
        # Build MODIFY COLUMN statement
        nullable_str = '' if is_nullable == 'NO' else ' NULL'
        not_null_str = ' NOT NULL' if is_nullable == 'NO' else ''
        default_str = ''
        if col_default is not None:
            default_str = f" DEFAULT '{col_default}'"
        elif is_nullable == 'YES':
            default_str = ' DEFAULT NULL'
        
        extra_str = ''
        if 'auto_increment' in (extra or ''):
            extra_str = ' AUTO_INCREMENT'
        elif 'DEFAULT_GENERATED' in (extra or ''):
            extra_str = ' DEFAULT CURRENT_TIMESTAMP'
        elif 'on update CURRENT_TIMESTAMP' in (extra or ''):
            extra_str = ' DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP'
        
        # For columns with DEFAULT_GENERATED and auto_increment, handle carefully
        if 'auto_increment' in (extra or ''):
            sql_lines.append(
                f"ALTER TABLE {table} MODIFY COLUMN {col_name} {col_type} NOT NULL AUTO_INCREMENT COMMENT '{comment}';"
            )
        elif 'DEFAULT_GENERATED' in (extra or ''):
            if is_nullable == 'YES':
                sql_lines.append(
                    f"ALTER TABLE {table} MODIFY COLUMN {col_name} {col_type} DEFAULT CURRENT_TIMESTAMP COMMENT '{comment}';"
                )
            else:
                sql_lines.append(
                    f"ALTER TABLE {table} MODIFY COLUMN {col_name} {col_type} NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '{comment}';"
                )
        else:
            if is_nullable == 'NO':
                if col_default is not None:
                    sql_lines.append(
                        f"ALTER TABLE {table} MODIFY COLUMN {col_name} {col_type} NOT NULL DEFAULT '{col_default}' COMMENT '{comment}';"
                    )
                else:
                    sql_lines.append(
                        f"ALTER TABLE {table} MODIFY COLUMN {col_name} {col_type} NOT NULL COMMENT '{comment}';"
                    )
            else:
                if col_default is not None:
                    sql_lines.append(
                        f"ALTER TABLE {table} MODIFY COLUMN {col_name} {col_type} DEFAULT '{col_default}' COMMENT '{comment}';"
                    )
                else:
                    sql_lines.append(
                        f"ALTER TABLE {table} MODIFY COLUMN {col_name} {col_type} DEFAULT NULL COMMENT '{comment}';"
                    )
    
    sql_lines.append('')

cursor.close()
conn.close()

# Write output
output_path = r'd:\self\sy\运营数据平台\202606\wd\ops-platform-server\ops-platform-module-oa\src\main\resources\db\migration\V126__add_remaining_table_column_comments.sql'
with open(output_path, 'w', encoding='utf-8') as f:
    f.write('\n'.join(sql_lines))

print(f"Generated {output_path}")
print(f"Total lines: {len(sql_lines)}")
