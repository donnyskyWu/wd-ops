package cn.iocoder.yudao.framework.common.exception;

/**
 * OA 业务错误码（GLOBAL-CONVENTIONS §5.3）
 */
public final class OaErrorCodes {

    private OaErrorCodes() {
    }

    public static final ErrorCode ENTITY_NOT_EXISTS = ErrorCode.of(1500, "关联的实体不存在");
    public static final ErrorCode ENTITY_DISABLED = ErrorCode.of(1501, "关联实体已停用或注销");
    public static final ErrorCode ENTITY_ALREADY_BOUND = ErrorCode.of(1502, "关联实体已被引用");
    public static final ErrorCode DICT_VALUE_INVALID = ErrorCode.of(1503, "字典值不合法");
    public static final ErrorCode TENANT_FORBIDDEN = ErrorCode.of(1504, "跨租户访问禁止");

    /** 业务唯一性冲突（手机号/账号 ID 等） */
    public static final ErrorCode DUPLICATE_ENTITY = ErrorCode.of(2021, "数据已存在，不可重复");

    /** M1 IP 组 */
    public static final ErrorCode IP_GROUP_NAME_INVALID = ErrorCode.of(1001, "IP 组名称不合法（1-50 字符）");
    public static final ErrorCode IP_GROUP_NAME_DUPLICATE = ErrorCode.of(1002, "同父级下名称重复");
    public static final ErrorCode IP_GROUP_PARENT_INVALID = ErrorCode.of(1003, "上级必须是大组");
    public static final ErrorCode IP_GROUP_LEADER_NOT_FOUND = ErrorCode.of(1004, "组长用户不存在");
    public static final ErrorCode IP_GROUP_HAS_DATA = ErrorCode.of(1005, "该 IP 组下存在数据，禁止删除");
    public static final ErrorCode IP_GROUP_MEMBER_HAS_TASK = ErrorCode.of(1006, "成员存在关联任务，无法删除");
    public static final ErrorCode IP_GROUP_ACCOUNT_BOUND = ErrorCode.of(1007, "账号已属于其他 IP 组");
    public static final ErrorCode IP_GROUP_ACCOUNT_SMALL_ONLY = ErrorCode.of(1008, "账号仅可关联到小组");

    public static final ErrorCode AUTHOR_IP_GROUP_MUST_SMALL = ErrorCode.of(1101, "作者 IP 组必须选择小组");
    public static final ErrorCode AUTHOR_PRIMARY_TYPE_INVALID = ErrorCode.of(1102, "主推号类型必须为 OFFICIAL_ACCOUNT");
    public static final ErrorCode AUTHOR_PRIMARY_BOUND = ErrorCode.of(1103, "该主推号已被其他作者绑定");
    public static final ErrorCode AUTHOR_HAS_TASK = ErrorCode.of(1104, "作者存在未完成任务，无法删除");

    public static final ErrorCode OPS_ANCHOR_OVERLAP = ErrorCode.of(1201, "运营→主播关联存在重叠日期段");

    public static final ErrorCode IMPORT_DATE_TOO_OLD = ErrorCode.of(1301, "仅可补录过去 90 天内的数据");
    public static final ErrorCode IMPORT_CONTENT_NOT_FOUND = ErrorCode.of(1302, "作品不存在或已删除");
    public static final ErrorCode IMPORT_ALREADY_APPROVED = ErrorCode.of(1303, "该作品当日已存在已审核的补录");
    public static final ErrorCode IMPORT_REVIEW_LOCKED = ErrorCode.of(1304, "已审核的补录不可修改/删除");

    /** M2 内容生产 */
    public static final ErrorCode SOP_DAG_CYCLE = ErrorCode.of(2001, "DAG 存在环");
    public static final ErrorCode SOP_EXECUTOR_ROLE_MISSING = ErrorCode.of(2002, "节点 executor_role 缺失");
    public static final ErrorCode SOP_REVIEWER_ROLE_MISSING = ErrorCode.of(2003, "need_review=1 但 reviewer_role 缺失");
    public static final ErrorCode SOP_PREDECESSOR_NOT_DONE = ErrorCode.of(2004, "前置节点未完成");
    public static final ErrorCode SOP_TEMPLATE_NO_NODES = ErrorCode.of(2005, "模板无节点，无法启用");
    public static final ErrorCode CONTENT_PLATFORM_MISMATCH = ErrorCode.of(2022, "账号平台类型与内容平台类型不匹配");
    public static final ErrorCode REVIEWER_POSITION_MISMATCH = ErrorCode.of(2007, "审核人岗位不匹配");
    public static final ErrorCode TASK_CONTENT_NOT_COMPLETED = ErrorCode.of(2008, "须先完成内容创作");
    public static final ErrorCode TASK_STATUS_INVALID = ErrorCode.of(2023, "任务状态不允许此操作");
    public static final ErrorCode TASK_ASSIGNEE_MISMATCH = ErrorCode.of(2024, "非任务执行人，无权操作");
    public static final ErrorCode CONTENT_STATUS_INVALID = ErrorCode.of(2010, "内容状态不允许此操作");

    /** M3 绩效核算 */
    public static final ErrorCode PERF_DUPLICATE_PERIOD = ErrorCode.of(2025, "周期内已有考核记录");
    public static final ErrorCode PERF_ADJUST_LIMIT = ErrorCode.of(2026, "人工调整幅度超过 ±20%");
    public static final ErrorCode PERF_WEIGHT_NOT_100 = ErrorCode.of(3001, "权重合计不等于 100%");
    public static final ErrorCode PERF_RANGE_GAP = ErrorCode.of(2011, "评分区间存在 gap");
    public static final ErrorCode PERF_RANGE_OVERLAP = ErrorCode.of(2012, "评分区间重叠");
    public static final ErrorCode PERF_NO_TEMPLATE = ErrorCode.of(2013, "岗位无可用考核模板");
    public static final ErrorCode PERF_RECORD_NOT_DRAFT = ErrorCode.of(2014, "考核记录非草稿状态，不可操作");

    /** M5 财务管理 */
    public static final ErrorCode FINANCE_AMOUNT_INVALID = ErrorCode.of(2015, "金额非法");
    public static final ErrorCode FINANCE_DATE_RANGE_INVALID = ErrorCode.of(2016, "日期范围非法");

    /** M6 数据分析 */
    public static final ErrorCode SQL_INJECTION_RISK = ErrorCode.of(2017, "SQL 注入风险");
    public static final ErrorCode FORMULA_SYNTAX_ERROR = ErrorCode.of(2018, "公式语法错误");

    /** M0 首页 */
    public static final ErrorCode HOME_TREND_TYPE_INVALID = ErrorCode.of(2019, "趋势类型不合法");

    /** M9 字典管理 */
    public static final ErrorCode DICT_TYPE_DUPLICATE = ErrorCode.of(2027, "字典 type 重复");
    public static final ErrorCode DICT_VALUE_DUPLICATE = ErrorCode.of(2028, "字典 value 重复");

    /** M11 字典 */
    public static final ErrorCode DICT_TYPE_NOT_FOUND = ErrorCode.of(2020, "字典 type 不存在");
    public static final ErrorCode BAD_REQUEST = ErrorCode.of(1400, "请求参数不合法");

    public static final ErrorCode UNAUTHORIZED = ErrorCode.of(401, "未登录或 Token 无效");
    public static final ErrorCode FORBIDDEN = ErrorCode.of(403, "无权限访问");
}
