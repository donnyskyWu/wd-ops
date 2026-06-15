/**
 * DictSelect 通用 mock 字典数据
 * 覆盖业务常见的 6 类字典
 */
export const mockDictMap: Record<string, Array<{ label: string; value: string; colorType?: string }>> = {
  // 通用
  dict_yes_no: [
    { label: '是', value: '1', colorType: 'success' },
    { label: '否', value: '0', colorType: 'info' },
  ],
  // 平台
  dict_platform_type: [
    { label: '全部', value: 'ALL', colorType: 'info' },
    { label: '微信公众号', value: 'WECHAT_OFFICIAL', colorType: 'success' },
    { label: '抖音', value: 'DOUYIN', colorType: 'warning' },
    { label: '企业微信', value: 'WEWORK', colorType: 'primary' },
    { label: '视频号', value: 'WECHAT_VIDEO', colorType: 'success' },
    { label: '快手', value: 'KUAISHOU', colorType: 'warning' },
    { label: '小红书', value: 'XIAOHONGSHU', colorType: 'danger' },
    { label: '个微', value: 'WECHAT_PERSONAL', colorType: 'primary' },
  ],
  // 账号状态
  dict_account_status: [
    { label: '启用', value: '1', colorType: 'success' },
    { label: '停用', value: '0', colorType: 'info' },
  ],
  // 公司状态
  dict_company_status: [
    { label: '启用', value: 'ENABLED', colorType: 'success' },
    { label: '停用', value: 'DISABLED', colorType: 'info' },
  ],
  dict_id_type: [
    { label: '身份证', value: 'ID_CARD' },
    { label: '护照', value: 'PASSPORT' },
  ],
  dict_gender: [
    { label: '男', value: 'MALE' },
    { label: '女', value: 'FEMALE' },
  ],
  dict_realname_status: [
    { label: '启用', value: 'ENABLED', colorType: 'success' },
    { label: '停用', value: 'DISABLED', colorType: 'info' },
  ],
  dict_intermediary_relation: [
    { label: '直签', value: 'DIRECT' },
    { label: '中介代理', value: 'INTERMEDIARY' },
    { label: '机构合作', value: 'AGENCY' },
  ],
  dict_phone_status: [
    { label: '在用', value: 'ENABLED', colorType: 'success' },
    { label: '停用', value: 'DISABLED', colorType: 'info' },
  ],
  dict_phone_type: [
    { label: 'Android', value: 'ANDROID' },
    { label: 'iPhone', value: 'IPHONE' },
  ],
  dict_sim_operator: [
    { label: '中国移动', value: 'MOBILE' },
    { label: '中国联通', value: 'UNICOM' },
    { label: '中国电信', value: 'TELECOM' },
  ],
  dict_sim_status: [
    { label: '在用', value: 'ENABLED', colorType: 'success' },
    { label: '停用', value: 'DISABLED', colorType: 'info' },
    { label: '损坏', value: 'DAMAGED', colorType: 'danger' },
    { label: '丢失', value: 'LOST', colorType: 'warning' },
  ],
  dict_account_type: [
    { label: '官方账号', value: 'OFFICIAL_ACCOUNT' },
    { label: '个人账号', value: 'PERSONAL_ACCOUNT' },
    { label: '服务号', value: 'SERVICE_ACCOUNT' },
  ],
  // 行业
  dict_industry: [
    { label: '新媒体', value: 'new_media' },
    { label: 'MCN', value: 'mcn' },
    { label: '文化传媒', value: 'media' },
    { label: '教育', value: 'education' },
    { label: '电商', value: 'ecommerce' },
  ],
  // 运营商
  dict_carrier: [
    { label: '中国移动', value: 'mobile' },
    { label: '中国联通', value: 'unicom' },
    { label: '中国电信', value: 'telecom' },
  ],
  // 时间维度
  dict_time_dimension: [
    { label: '日', value: 'day' },
    { label: '周', value: 'week' },
    { label: '月', value: 'month' },
    { label: '小时', value: 'hour' },
  ],
  // 岗位（IP 组 / SOP）
  dict_position: [
    { label: '运营', value: 'OPERATOR' },
    { label: '编辑', value: 'EDITOR' },
    { label: '主播', value: 'ANCHOR' },
    { label: '销售', value: 'SALES' },
    { label: '运营组长', value: 'OPS_LEADER' },
    { label: '直播运营', value: 'LIVE_OPERATOR' },
  ],
  // 文档类型（M2 内容生产）
  dict_document_type: [
    { label: '短视频文案', value: 'SHORT_VIDEO_SCRIPT' },
    { label: '新号引流', value: 'NEW_ACCOUNT_TRAFFIC' },
    { label: '赛后复盘', value: 'POST_MATCH_REVIEW' },
    { label: '正式方案', value: 'OFFICIAL_PLAN' },
    { label: '预热前瞻', value: 'PREHEAT_PREVIEW' },
  ],
  // 内容类型
  dict_content_type: [
    { label: '文章', value: 'ARTICLE' },
    { label: '短视频', value: 'SHORT_VIDEO' },
    { label: '直播', value: 'LIVE' },
  ],
  // 绩效等级
  dict_perf_grade: [
    { label: 'S（优秀）', value: 'S', colorType: 'success' },
    { label: 'A（良好）', value: 'A', colorType: 'success' },
    { label: 'B（合格）', value: 'B', colorType: 'primary' },
    { label: 'C（待改进）', value: 'C', colorType: 'warning' },
    { label: 'D（不合格）', value: 'D', colorType: 'danger' },
  ],
  // 成本类型
  dict_cost_type: [
    { label: '购买成本', value: 'PURCHASE' },
    { label: '人力成本', value: 'PROCESS_HUMAN' },
    { label: '投放成本', value: 'AD_SPEND' },
  ],
  dict_cost_pay_method: [
    { label: '微信', value: 'WECHAT' },
    { label: '支付宝', value: 'ALIPAY' },
    { label: '银行卡', value: 'BANK' },
    { label: '对公转账', value: 'CORPORATE' },
  ],
  dict_cost_period: [
    { label: '一次性', value: 'ONCE' },
    { label: '月度', value: 'MONTH' },
    { label: '季度', value: 'QUARTER' },
  ],
  dict_perf_metric_type: [
    { label: '基础指标', value: 'BASIC' },
    { label: '复合指标', value: 'COMPOSITE' },
  ],
  // ROI 分析维度（与后端 FinanceRoiService dimension 一致）
  dict_roi_dimension: [
    { label: 'IP组', value: 'IP_GROUP' },
    { label: '账号', value: 'ACCOUNT' },
    { label: '人员', value: 'PERSON' },
  ],
  // 通用启用/停用
  dict_status_enabled: [
    { label: '启用', value: 'ENABLED', colorType: 'success' },
    { label: '停用', value: 'DISABLED', colorType: 'info' },
  ],
  dict_config_status: [
    { label: '启用', value: 'ENABLED', colorType: 'success' },
    { label: '停用', value: 'DISABLED', colorType: 'info' },
  ],
  // 同步频率
  dict_sync_frequency: [
    { label: '每小时', value: 'HOURLY' },
    { label: '每 4 小时', value: 'EVERY_4H' },
    { label: '每日', value: 'DAILY' },
    { label: '每周', value: 'WEEKLY' },
    { label: '每月', value: 'MONTHLY' },
  ],
  // 告警级别
  dict_alert_level: [
    { label: '严重', value: 'CRITICAL', colorType: 'danger' },
    { label: '警告', value: 'WARNING', colorType: 'warning' },
    { label: '提示', value: 'INFO', colorType: 'info' },
  ],
  // AI 提示词应用场景
  dict_ai_scene: [
    { label: '短视频文案', value: 'SHORT_VIDEO' },
    { label: '直播脚本', value: 'LIVE_SCRIPT' },
    { label: '小红书笔记', value: 'XIAOHONGSHU' },
    { label: '公众号文章', value: 'WECHAT_ARTICLE' },
    { label: '数据分析', value: 'DATA_ANALYSIS' },
    { label: '周报月报', value: 'REPORT' },
    { label: '竞品分析', value: 'COMPETITOR' },
  ],
  // AI 模型类型
  dict_ai_model_type: [
    { label: '通义千问', value: 'QWEN' },
    { label: '文心一言', value: 'ERNIE' },
    { label: '智谱 AI', value: 'GLM' },
    { label: 'DeepSeek', value: 'DEEPSEEK' },
    { label: 'Kimi', value: 'KIMI' },
    { label: '豆包', value: 'DOUBAO' },
    { label: 'OpenAI GPT', value: 'GPT' },
    { label: 'Claude', value: 'CLAUDE' },
    { label: 'Gemini', value: 'GEMINI' },
    { label: '月之暗面', value: 'MOONSHOT' },
  ],
  // 阈值指标类型
  dict_threshold_metric: [
    { label: '播放量', value: 'PLAY_COUNT' },
    { label: '点赞数', value: 'LIKE_COUNT' },
    { label: '评论数', value: 'COMMENT_COUNT' },
    { label: '转发数', value: 'SHARE_COUNT' },
    { label: '粉丝增长', value: 'FANS_GROWTH' },
    { label: '转化率', value: 'CONVERSION_RATE' },
    { label: 'GMV', value: 'GMV' },
  ],
  // 通知方式
  dict_notify_method: [
    { label: '邮件', value: 'EMAIL' },
    { label: '短信', value: 'SMS' },
    { label: '企业微信', value: 'WECHAT' },
    { label: '钉钉', value: 'DINGTALK' },
  ],
  // 三方采集平台（蝉妈妈/飞瓜等）
  dict_third_platform: [
    { label: '蝉妈妈', value: 'CHANMAMA' },
    { label: '飞瓜数据', value: 'FEIGUA' },
    { label: '新榜', value: 'XINBANG' },
    { label: '考古加', value: 'KAOGUJIA' },
    { label: '灰豚数据', value: 'HUITUN' },
  ],
  // 电商平台
  dict_ecom_platform: [
    { label: '抖音小店', value: 'DOUYIN_SHOP' },
    { label: '快手小店', value: 'KUAISHOU_SHOP' },
    { label: '淘宝', value: 'TAOBAO' },
    { label: '京东', value: 'JD' },
    { label: '拼多多', value: 'PDD' },
  ],
  // IP 组类型
  dict_ip_group_type: [
    { label: '大组', value: '1' },
    { label: '小组', value: '2' },
  ],
  // 关系类型
  dict_relation_type: [
    { label: '直签', value: 'DIRECT' },
    { label: '中介代理', value: 'INTERMEDIARY' },
    { label: '机构合作', value: 'AGENCY' },
  ],
  // 三方关联类型
  dict_triple_rel_type: [
    { label: '完整三方', value: 'full' },
    { label: '微信+视频', value: 'wechat_video' },
    { label: '微信+企微', value: 'wechat_wework' },
    { label: '视频+企微', value: 'video_wework' },
  ],
  // 数据来源(补录)
  dict_import_source: [
    { label: 'API', value: 'API', colorType: 'primary' },
    { label: '接口异常补录', value: 'API_EXCEPTION', colorType: 'warning' },
    { label: '账号封禁补录', value: 'ACCOUNT_BANNED', colorType: 'danger' },
    { label: '线下练习数据', value: 'OFFLINE_PRACTICE', colorType: 'warning' },
    { label: '其他补录', value: 'OTHER', colorType: 'info' },
  ],
  // 审核状态
  dict_review_status: [
    { label: '待审核', value: 'PENDING', colorType: 'warning' },
    { label: '已通过', value: 'APPROVED', colorType: 'success' },
    { label: '已驳回', value: 'REJECTED', colorType: 'danger' },
  ],
  // 内容审核阶段
  dict_review_stage: [
    { label: '初审核', value: 'FIRST' },
    { label: '复审核', value: 'SECOND' },
    { label: '终审核', value: 'FINAL' },
  ],
  // 采集方式
  dict_collect_method: [
    { label: 'API', value: 'API' },
    { label: '爬虫', value: 'SCRAPER' },
    { label: '手工', value: 'MANUAL' },
  ],
  // 采集数据源
  dict_collect_source: [
    { label: '官方接口', value: 'OFFICIAL' },
    { label: '三方数据', value: 'THIRD_PARTY' },
    { label: '内部 ERP', value: 'INTERNAL_ERP' },
  ],
  // 采集频率
  dict_collect_frequency: [
    { label: '半小时', value: 'HALF_HOUR' },
    { label: '小时', value: 'HOUR' },
    { label: '天', value: 'DAY' },
    { label: '周', value: 'WEEK' },
  ],
  // 采集状态（STATE-M10 / GLOBAL-CONVENTIONS：5 态执行状态）
  dict_collect_status: [
    { label: '待执行', value: 'PENDING', colorType: 'info' },
    { label: '执行中', value: 'RUNNING', colorType: 'primary' },
    { label: '成功', value: 'SUCCESS', colorType: 'success' },
    { label: '失败', value: 'FAILED', colorType: 'danger' },
    { label: '部分成功', value: 'PARTIAL', colorType: 'warning' },
  ],
  // 质量检查类型（PRD-M10 §4.2.3 / API-M10 COMPLETENESS 示例）
  dict_quality_check_type: [
    { label: '完整性', value: 'COMPLETENESS' },
    { label: '准确性', value: 'ACCURACY' },
    { label: '一致性', value: 'CONSISTENCY' },
    { label: '时效性', value: 'TIMELINESS' },
    { label: '唯一性', value: 'UNIQUENESS' },
  ],
  // 质量级别
  dict_quality_level: [
    { label: '错误', value: 'ERROR', colorType: 'danger' },
    { label: '警告', value: 'WARN', colorType: 'warning' },
    { label: '信息', value: 'INFO', colorType: 'info' },
  ],
  // AI 模型
  dict_ai_model: [
    { label: 'GPT-4', value: 'GPT-4' },
    { label: 'GPT-3.5', value: 'GPT-3.5' },
    { label: '文心一言', value: 'ERNIE' },
    { label: '通义千问', value: 'QWEN' },
    { label: '讯飞星火', value: 'SPARK' },
  ],
  // 内容阶段
  dict_content_stage: [
    { label: '选题', value: 'TOPIC' },
    { label: '撰写', value: 'WRITE' },
    { label: '审核中', value: 'REVIEWING' },
    { label: '已发布', value: 'PUBLISHED' },
  ],
  // 任务状态
  dict_task_status: [
    { label: '待开始', value: 'PENDING', colorType: 'info' },
    { label: '进行中', value: 'IN_PROGRESS', colorType: 'primary' },
    { label: '已完成', value: 'DONE', colorType: 'success' },
    { label: '已超期', value: 'OVERDUE', colorType: 'danger' },
  ],
  // 绩效状态
  dict_perf_status: [
    { label: '草稿', value: 'DRAFT', colorType: 'info' },
    { label: '执行中', value: 'EXECUTING', colorType: 'primary' },
    { label: '已确认', value: 'CONFIRMED', colorType: 'success' },
    { label: '已驳回', value: 'REJECTED', colorType: 'danger' },
  ],
  // SOP 状态
  dict_sop_status: [
    { label: '启用', value: 'ENABLED', colorType: 'success' },
    { label: '草稿', value: 'DRAFT', colorType: 'info' },
    { label: '已停用', value: 'DISABLED', colorType: 'warning' },
  ],
}
