package cn.iocoder.yudao.module.oa.enums;

/**
 * OPS 操作日志常量 — 对齐 Football {@code LogRecordConstants} 模式。
 * <p>
 * {@code type} → Football operate-log.type；{@code subType} → subType；{@code success} → action 模板。
 * 无业务 id 的操作（export / sync 等）使用 {@link #BIZ_NO_NONE} 作为 bizNo。
 */
public interface OaLogRecordConstants {

    /** 无业务主键时的 bizNo 占位（见迁移方案 Q5） */
    String BIZ_NO_NONE = "0";

    // ======================= M9 系统参数 =======================

    String OPS_PARAM_TYPE = "OPS 系统参数";
    String OPS_PARAM_CREATE_SUB_TYPE = "创建系统参数";
    String OPS_PARAM_CREATE_SUCCESS = "创建了系统参数【{{#param.paramName}}】";
    String OPS_PARAM_UPDATE_SUB_TYPE = "更新系统参数";
    String OPS_PARAM_UPDATE_SUCCESS = "更新了系统参数【{{#param.paramName}}】: {_DIFF{#req}}";
    String OPS_PARAM_DELETE_SUB_TYPE = "删除系统参数";
    String OPS_PARAM_DELETE_SUCCESS = "删除了系统参数【{{#param.paramName}}】";

    // ======================= M9 字典 =======================

    String OPS_DICT_TYPE = "OPS 系统字典";
    String OPS_DICT_CREATE_SUB_TYPE = "创建字典";
    String OPS_DICT_CREATE_SUCCESS = "创建了字典【{{#dict.label}}】";
    String OPS_DICT_UPDATE_SUB_TYPE = "更新字典";
    String OPS_DICT_UPDATE_SUCCESS = "更新了字典【{{#dict.label}}】: {_DIFF{#req}}";
    String OPS_DICT_DELETE_SUB_TYPE = "删除字典";
    String OPS_DICT_DELETE_SUCCESS = "删除了字典【{{#dict.label}}】";

    // ======================= M9 用户 =======================

    String OPS_USER_TYPE = "OPS 用户";
    String OPS_USER_CREATE_SUB_TYPE = "创建用户";
    String OPS_USER_CREATE_SUCCESS = "创建了用户【{{#user.username}}】";
    String OPS_USER_UPDATE_SUB_TYPE = "更新用户";
    String OPS_USER_UPDATE_SUCCESS = "更新了用户【{{#user.username}}】: {_DIFF{#req}}";
    String OPS_USER_DELETE_SUB_TYPE = "删除用户";
    String OPS_USER_DELETE_SUCCESS = "删除了用户【{{#user.username}}】";

    // ======================= M9 角色 =======================

    String OPS_ROLE_TYPE = "OPS 角色";
    String OPS_ROLE_CREATE_SUB_TYPE = "创建角色";
    String OPS_ROLE_CREATE_SUCCESS = "创建了角色【{{#role.name}}】";
    String OPS_ROLE_UPDATE_SUB_TYPE = "更新角色";
    String OPS_ROLE_UPDATE_SUCCESS = "更新了角色【{{#role.name}}】: {_DIFF{#req}}";
    String OPS_ROLE_DELETE_SUB_TYPE = "删除角色";
    String OPS_ROLE_DELETE_SUCCESS = "删除了角色【{{#role.name}}】";
    String OPS_ROLE_ASSIGN_PERMISSION_SUB_TYPE = "分配角色权限";
    String OPS_ROLE_ASSIGN_PERMISSION_SUCCESS = "为角色【{{#role.name}}】分配了权限";

    // ======================= M9 部门 =======================

    String OPS_DEPT_TYPE = "OPS 部门";
    String OPS_DEPT_CREATE_SUB_TYPE = "创建部门";
    String OPS_DEPT_CREATE_SUCCESS = "创建了部门【{{#dept.name}}】";
    String OPS_DEPT_UPDATE_SUB_TYPE = "更新部门";
    String OPS_DEPT_UPDATE_SUCCESS = "更新了部门【{{#dept.name}}】: {_DIFF{#req}}";
    String OPS_DEPT_DELETE_SUB_TYPE = "删除部门";
    String OPS_DEPT_DELETE_SUCCESS = "删除了部门【{{#dept.name}}】";
    String OPS_DEPT_SYNC_DINGTALK_SUB_TYPE = "同步钉钉部门";
    String OPS_DEPT_SYNC_DINGTALK_SUCCESS = "同步了钉钉部门";
    String OPS_DEPT_SYNC_DINGTALK_USERS_SUB_TYPE = "同步钉钉部门用户";
    String OPS_DEPT_SYNC_DINGTALK_USERS_SUCCESS = "同步了钉钉部门用户";

    // ======================= M9 租户 =======================

    String OPS_TENANT_TYPE = "OPS 租户";
    String OPS_TENANT_CREATE_SUB_TYPE = "创建租户";
    String OPS_TENANT_CREATE_SUCCESS = "创建了租户【{{#tenant.name}}】";
    String OPS_TENANT_UPDATE_SUB_TYPE = "更新租户";
    String OPS_TENANT_UPDATE_SUCCESS = "更新了租户【{{#tenant.name}}】: {_DIFF{#req}}";
    String OPS_TENANT_DELETE_SUB_TYPE = "删除租户";
    String OPS_TENANT_DELETE_SUCCESS = "删除了租户【{{#tenant.name}}】";

    // ======================= M9 消息 =======================

    String OPS_MESSAGE_TYPE = "OPS 消息";
    String OPS_MESSAGE_SEND_SUB_TYPE = "发送消息";
    String OPS_MESSAGE_SEND_SUCCESS = "发送了消息【{{#message.title}}】";
    String OPS_MESSAGE_DELETE_SUB_TYPE = "删除消息";
    String OPS_MESSAGE_DELETE_SUCCESS = "删除了消息【{{#message.title}}】";
    String OPS_MESSAGE_MARK_READ_SUB_TYPE = "标记消息已读";
    String OPS_MESSAGE_MARK_READ_SUCCESS = "标记消息已读";

    // ======================= M1 作者 =======================

    String OPS_AUTHOR_TYPE = "OPS 作者";
    String OPS_AUTHOR_CREATE_SUB_TYPE = "创建作者";
    String OPS_AUTHOR_CREATE_SUCCESS = "创建了作者【{{#author.nickname}}】";
    String OPS_AUTHOR_UPDATE_SUB_TYPE = "更新作者";
    String OPS_AUTHOR_UPDATE_SUCCESS = "更新了作者【{{#author.nickname}}】: {_DIFF{#req}}";
    String OPS_AUTHOR_DELETE_SUB_TYPE = "删除作者";
    String OPS_AUTHOR_DELETE_SUCCESS = "删除了作者【{{#author.nickname}}】";
    String OPS_AUTHOR_UPDATE_EXT_SUB_TYPE = "更新作者扩展信息";
    String OPS_AUTHOR_UPDATE_EXT_SUCCESS = "更新了作者扩展信息【{{#author.nickname}}】";

    // ======================= M1 IP 组 =======================

    String OPS_IP_GROUP_TYPE = "OPS IP 组";
    String OPS_IP_GROUP_CREATE_SUB_TYPE = "创建 IP 组";
    String OPS_IP_GROUP_CREATE_SUCCESS = "创建了 IP 组【{{#ipGroup.name}}】";
    String OPS_IP_GROUP_UPDATE_SUB_TYPE = "更新 IP 组";
    String OPS_IP_GROUP_UPDATE_SUCCESS = "更新了 IP 组【{{#ipGroup.name}}】: {_DIFF{#req}}";
    String OPS_IP_GROUP_UPDATE_STATUS_SUB_TYPE = "变更 IP 组状态";
    String OPS_IP_GROUP_UPDATE_STATUS_SUCCESS = "变更了 IP 组【{{#ipGroup.name}}】状态";
    String OPS_IP_GROUP_DELETE_SUB_TYPE = "删除 IP 组";
    String OPS_IP_GROUP_DELETE_SUCCESS = "删除了 IP 组【{{#ipGroup.name}}】";
    String OPS_IP_GROUP_ADD_MEMBER_SUB_TYPE = "添加 IP 组成员";
    String OPS_IP_GROUP_ADD_MEMBER_SUCCESS = "为 IP 组【{{#ipGroup.name}}】添加了成员";
    String OPS_IP_GROUP_UPDATE_MEMBER_SUB_TYPE = "更新 IP 组成员";
    String OPS_IP_GROUP_UPDATE_MEMBER_SUCCESS = "更新了 IP 组【{{#ipGroup.name}}】成员";
    String OPS_IP_GROUP_DELETE_MEMBER_SUB_TYPE = "删除 IP 组成员";
    String OPS_IP_GROUP_DELETE_MEMBER_SUCCESS = "删除了 IP 组【{{#ipGroup.name}}】成员";
    String OPS_IP_GROUP_BIND_ACCOUNTS_SUB_TYPE = "绑定账号";
    String OPS_IP_GROUP_BIND_ACCOUNTS_SUCCESS = "为 IP 组【{{#ipGroup.name}}】绑定了账号";
    String OPS_IP_GROUP_UNBIND_ACCOUNT_SUB_TYPE = "解绑账号";
    String OPS_IP_GROUP_UNBIND_ACCOUNT_SUCCESS = "为 IP 组【{{#ipGroup.name}}】解绑了账号";
    String OPS_IP_GROUP_BIND_ANCHORS_SUB_TYPE = "绑定主播";
    String OPS_IP_GROUP_BIND_ANCHORS_SUCCESS = "为 IP 组【{{#ipGroup.name}}】绑定了主播";
    String OPS_IP_GROUP_UNBIND_ANCHOR_SUB_TYPE = "解绑主播";
    String OPS_IP_GROUP_UNBIND_ANCHOR_SUCCESS = "为 IP 组【{{#ipGroup.name}}】解绑了主播";

    // ======================= M1 内部内容 / 运营 =======================

    String OPS_INTERNAL_CONTENT_TYPE = "OPS 内部内容";
    String OPS_INTERNAL_CONTENT_IMPORT_SUBMIT_SUB_TYPE = "提交内容导入";
    String OPS_INTERNAL_CONTENT_IMPORT_SUBMIT_SUCCESS = "提交了内容导入";
    String OPS_INTERNAL_CONTENT_IMPORT_REVIEW_SUB_TYPE = "审核内容导入";
    String OPS_INTERNAL_CONTENT_IMPORT_REVIEW_SUCCESS = "审核了内容导入";

    String OPS_PRODUCTIVITY_REVIEW_TYPE = "OPS 产能审核";
    String OPS_PRODUCTIVITY_REVIEW_EXPORT_SUB_TYPE = "导出产能审核";
    String OPS_PRODUCTIVITY_REVIEW_EXPORT_SUCCESS = "导出了产能审核数据";

    String OPS_FOLLOWER_ANALYSIS_TYPE = "OPS 粉丝分析";
    String OPS_FOLLOWER_ANALYSIS_EXPORT_SUB_TYPE = "导出粉丝分析";
    String OPS_FOLLOWER_ANALYSIS_EXPORT_SUCCESS = "导出了粉丝分析数据";

    String OPS_ANCHOR_TYPE = "OPS 运营主播";
    String OPS_ANCHOR_CREATE_SUB_TYPE = "创建运营主播";
    String OPS_ANCHOR_CREATE_SUCCESS = "创建了运营主播【{{#anchor.name}}】";
    String OPS_ANCHOR_UPDATE_SUB_TYPE = "更新运营主播";
    String OPS_ANCHOR_UPDATE_SUCCESS = "更新了运营主播【{{#anchor.name}}】: {_DIFF{#req}}";
    String OPS_ANCHOR_DELETE_SUB_TYPE = "删除运营主播";
    String OPS_ANCHOR_DELETE_SUCCESS = "删除了运营主播【{{#anchor.name}}】";

    // ======================= M2 内容生产 =======================

    String OPS_CONTENT_TYPE = "OPS 内容";
    String OPS_CONTENT_CREATE_SUB_TYPE = "创建内容";
    String OPS_CONTENT_CREATE_SUCCESS = "创建了内容【{{#content.title}}】";
    String OPS_CONTENT_UPDATE_SUB_TYPE = "更新内容";
    String OPS_CONTENT_UPDATE_SUCCESS = "更新了内容【{{#content.title}}】: {_DIFF{#req}}";
    String OPS_CONTENT_SUBMIT_REVIEW_SUB_TYPE = "提交内容审核";
    String OPS_CONTENT_SUBMIT_REVIEW_SUCCESS = "提交了内容【{{#content.title}}】审核";
    String OPS_CONTENT_DELETE_SUB_TYPE = "删除内容";
    String OPS_CONTENT_DELETE_SUCCESS = "删除了内容【{{#content.title}}】";
    String OPS_CONTENT_REVIEW_SUB_TYPE = "审核内容";
    String OPS_CONTENT_REVIEW_SUCCESS = "审核了内容【{{#content.title}}】";
    String OPS_CONTENT_CONFIRM_SUB_TYPE = "确认内容";
    String OPS_CONTENT_CONFIRM_SUCCESS = "确认了内容【{{#content.title}}】";
    String OPS_CONTENT_GENERATE_SUB_TYPE = "生成内容";
    String OPS_CONTENT_GENERATE_SUCCESS = "生成了内容";
    String OPS_CONTENT_TRANSFER_KNOWLEDGE_SUB_TYPE = "转移知识库";
    String OPS_CONTENT_TRANSFER_KNOWLEDGE_SUCCESS = "转移了内容知识库";
    String OPS_CONTENT_AI_GENERATE_SUB_TYPE = "AI 生成内容";
    String OPS_CONTENT_AI_GENERATE_SUCCESS = "AI 生成了内容";
    String OPS_CONTENT_PUBLISH_DRAFT_SUB_TYPE = "发布草稿";
    String OPS_CONTENT_PUBLISH_DRAFT_SUCCESS = "发布了内容草稿";
    String OPS_CONTENT_FORMAL_PUBLISH_SUB_TYPE = "正式发布";
    String OPS_CONTENT_FORMAL_PUBLISH_SUCCESS = "正式发布了内容";
    String OPS_CONTENT_PUBLISH_SUB_TYPE = "发布内容";
    String OPS_CONTENT_PUBLISH_SUCCESS = "发布了内容";
    String OPS_CONTENT_APPLY_LAYOUT_TEMPLATE_SUB_TYPE = "应用排版模板";
    String OPS_CONTENT_APPLY_LAYOUT_TEMPLATE_SUCCESS = "为内容应用了排版模板";

    String OPS_CONTENT_PLAN_TYPE = "OPS 内容计划";
    String OPS_CONTENT_PLAN_CREATE_SUB_TYPE = "创建内容计划";
    String OPS_CONTENT_PLAN_CREATE_SUCCESS = "创建了内容计划【{{#plan.name}}】";
    String OPS_CONTENT_PLAN_UPDATE_SUB_TYPE = "更新内容计划";
    String OPS_CONTENT_PLAN_UPDATE_SUCCESS = "更新了内容计划【{{#plan.name}}】: {_DIFF{#req}}";
    String OPS_CONTENT_PLAN_START_SUB_TYPE = "启动内容计划";
    String OPS_CONTENT_PLAN_START_SUCCESS = "启动了内容计划【{{#plan.name}}】";
    String OPS_CONTENT_PLAN_TERMINATE_SUBMIT_SUB_TYPE = "提交终止计划";
    String OPS_CONTENT_PLAN_TERMINATE_SUBMIT_SUCCESS = "提交了终止内容计划";
    String OPS_CONTENT_PLAN_TERMINATE_APPROVE_SUB_TYPE = "批准终止计划";
    String OPS_CONTENT_PLAN_TERMINATE_APPROVE_SUCCESS = "批准了终止内容计划";
    String OPS_CONTENT_PLAN_TERMINATE_REJECT_SUB_TYPE = "驳回终止计划";
    String OPS_CONTENT_PLAN_TERMINATE_REJECT_SUCCESS = "驳回了终止内容计划";
    String OPS_CONTENT_PLAN_DELETE_SUB_TYPE = "删除内容计划";
    String OPS_CONTENT_PLAN_DELETE_SUCCESS = "删除了内容计划【{{#plan.name}}】";

    String OPS_LAYOUT_STYLE_TYPE = "OPS 排版样式";
    String OPS_LAYOUT_STYLE_CREATE_SUB_TYPE = "创建排版样式";
    String OPS_LAYOUT_STYLE_CREATE_SUCCESS = "创建了排版样式【{{#style.name}}】";
    String OPS_LAYOUT_STYLE_UPDATE_SUB_TYPE = "更新排版样式";
    String OPS_LAYOUT_STYLE_UPDATE_SUCCESS = "更新了排版样式【{{#style.name}}】: {_DIFF{#req}}";
    String OPS_LAYOUT_STYLE_DELETE_SUB_TYPE = "删除排版样式";
    String OPS_LAYOUT_STYLE_DELETE_SUCCESS = "删除了排版样式【{{#style.name}}】";

    String OPS_LAYOUT_TEMPLATE_TYPE = "OPS 排版模板";
    String OPS_LAYOUT_TEMPLATE_CREATE_SUB_TYPE = "创建排版模板";
    String OPS_LAYOUT_TEMPLATE_CREATE_SUCCESS = "创建了排版模板【{{#template.name}}】";
    String OPS_LAYOUT_TEMPLATE_UPDATE_SUB_TYPE = "更新排版模板";
    String OPS_LAYOUT_TEMPLATE_UPDATE_SUCCESS = "更新了排版模板【{{#template.name}}】: {_DIFF{#req}}";
    String OPS_LAYOUT_TEMPLATE_DELETE_SUB_TYPE = "删除排版模板";
    String OPS_LAYOUT_TEMPLATE_DELETE_SUCCESS = "删除了排版模板【{{#template.name}}】";
    String OPS_LAYOUT_TEMPLATE_PUBLISH_SUB_TYPE = "发布排版模板";
    String OPS_LAYOUT_TEMPLATE_PUBLISH_SUCCESS = "发布了排版模板【{{#template.name}}】";
    String OPS_LAYOUT_TEMPLATE_DISABLE_SUB_TYPE = "停用排版模板";
    String OPS_LAYOUT_TEMPLATE_DISABLE_SUCCESS = "停用了排版模板【{{#template.name}}】";
    String OPS_LAYOUT_TEMPLATE_ENABLE_SUB_TYPE = "启用排版模板";
    String OPS_LAYOUT_TEMPLATE_ENABLE_SUCCESS = "启用了排版模板【{{#template.name}}】";
    String OPS_LAYOUT_TEMPLATE_IMPORT_PASTE_SUB_TYPE = "粘贴导入排版模板";
    String OPS_LAYOUT_TEMPLATE_IMPORT_PASTE_SUCCESS = "粘贴导入了排版模板";
    String OPS_LAYOUT_TEMPLATE_COPY_SUB_TYPE = "复制排版模板";
    String OPS_LAYOUT_TEMPLATE_COPY_SUCCESS = "复制了排版模板【{{#template.name}}】";

    String OPS_KNOWLEDGE_TYPE = "OPS 知识库";
    String OPS_KNOWLEDGE_CREATE_SUB_TYPE = "创建知识";
    String OPS_KNOWLEDGE_CREATE_SUCCESS = "创建了知识【{{#knowledge.title}}】";
    String OPS_KNOWLEDGE_UPDATE_SUB_TYPE = "更新知识";
    String OPS_KNOWLEDGE_UPDATE_SUCCESS = "更新了知识【{{#knowledge.title}}】: {_DIFF{#req}}";
    String OPS_KNOWLEDGE_DELETE_SUB_TYPE = "删除知识";
    String OPS_KNOWLEDGE_DELETE_SUCCESS = "删除了知识【{{#knowledge.title}}】";
    String OPS_KNOWLEDGE_TOGGLE_LIKE_SUB_TYPE = "切换知识点赞";
    String OPS_KNOWLEDGE_TOGGLE_LIKE_SUCCESS = "切换了知识点赞状态";

    String OPS_TYPESETTING_TYPE = "OPS 排版规则";
    String OPS_TYPESETTING_CREATE_SUB_TYPE = "创建排版规则";
    String OPS_TYPESETTING_CREATE_SUCCESS = "创建了排版规则【{{#rule.name}}】";
    String OPS_TYPESETTING_UPDATE_SUB_TYPE = "更新排版规则";
    String OPS_TYPESETTING_UPDATE_SUCCESS = "更新了排版规则【{{#rule.name}}】: {_DIFF{#req}}";
    String OPS_TYPESETTING_DELETE_SUB_TYPE = "删除排版规则";
    String OPS_TYPESETTING_DELETE_SUCCESS = "删除了排版规则【{{#rule.name}}】";

    String OPS_SOP_TYPE = "OPS SOP";
    String OPS_SOP_CREATE_NODE_SUB_TYPE = "创建 SOP 节点";
    String OPS_SOP_CREATE_NODE_SUCCESS = "创建了 SOP 节点";
    String OPS_SOP_UPDATE_NODE_SUB_TYPE = "更新 SOP 节点";
    String OPS_SOP_UPDATE_NODE_SUCCESS = "更新了 SOP 节点";
    String OPS_SOP_APPROVE_REVIEW_SUB_TYPE = "通过 SOP 审核";
    String OPS_SOP_APPROVE_REVIEW_SUCCESS = "通过了 SOP 审核";
    String OPS_SOP_REJECT_REVIEW_SUB_TYPE = "驳回 SOP 审核";
    String OPS_SOP_REJECT_REVIEW_SUCCESS = "驳回了 SOP 审核";
    String OPS_SOP_CREATE_TEMPLATE_SUB_TYPE = "创建 SOP 模板";
    String OPS_SOP_CREATE_TEMPLATE_SUCCESS = "创建了 SOP 模板【{{#template.name}}】";
    String OPS_SOP_UPDATE_TEMPLATE_SUB_TYPE = "更新 SOP 模板";
    String OPS_SOP_UPDATE_TEMPLATE_SUCCESS = "更新了 SOP 模板【{{#template.name}}】: {_DIFF{#req}}";
    String OPS_SOP_DELETE_TEMPLATE_SUB_TYPE = "删除 SOP 模板";
    String OPS_SOP_DELETE_TEMPLATE_SUCCESS = "删除了 SOP 模板【{{#template.name}}】";

    String OPS_TASK_TYPE = "OPS 任务";
    String OPS_TASK_CREATE_SUB_TYPE = "创建任务";
    String OPS_TASK_CREATE_SUCCESS = "创建了任务【{{#task.name}}】";
    String OPS_TASK_START_SUB_TYPE = "启动任务";
    String OPS_TASK_START_SUCCESS = "启动了任务【{{#task.name}}】";
    String OPS_TASK_COMPLETE_SUB_TYPE = "完成任务";
    String OPS_TASK_COMPLETE_SUCCESS = "完成了任务【{{#task.name}}】";
    String OPS_TASK_SUBMIT_REVIEW_SUB_TYPE = "提交任务审核";
    String OPS_TASK_SUBMIT_REVIEW_SUCCESS = "提交了任务审核";
    String OPS_TASK_EXECUTE_GET_SUB_TYPE = "获取任务执行";
    String OPS_TASK_EXECUTE_GET_SUCCESS = "获取了任务执行数据";
    String OPS_TASK_EXECUTE_SAVE_SUB_TYPE = "保存任务执行";
    String OPS_TASK_EXECUTE_SAVE_SUCCESS = "保存了任务执行数据";
    String OPS_TASK_EXECUTE_UPLOAD_SUB_TYPE = "上传任务执行";
    String OPS_TASK_EXECUTE_UPLOAD_SUCCESS = "上传了任务执行数据";
    String OPS_TASK_EXECUTE_COMPLETE_SUB_TYPE = "完成任务执行";
    String OPS_TASK_EXECUTE_COMPLETE_SUCCESS = "完成了任务执行";

    String OPS_AI_CONTENT_TYPE = "OPS AI 内容";
    String OPS_AI_CONTENT_GENERATE_SUB_TYPE = "AI 生成";
    String OPS_AI_CONTENT_GENERATE_SUCCESS = "AI 生成了内容";
    String OPS_AI_CONTENT_PREFERENCE_GENERATE_SUB_TYPE = "AI 偏好生成";
    String OPS_AI_CONTENT_PREFERENCE_GENERATE_SUCCESS = "AI 生成了偏好内容";
    String OPS_AI_CONTENT_CONVERSATION_SAVE_SUB_TYPE = "保存 AI 对话";
    String OPS_AI_CONTENT_CONVERSATION_SAVE_SUCCESS = "保存了 AI 对话";
    String OPS_AI_CONTENT_ADOPT_SUB_TYPE = "采纳 AI 内容";
    String OPS_AI_CONTENT_ADOPT_SUCCESS = "采纳了 AI 内容";

    // ======================= M3 绩效 =======================

    String OPS_PERF_TYPE = "OPS 绩效";
    String OPS_PERF_EXPORT_RESULT_SUB_TYPE = "导出绩效结果";
    String OPS_PERF_EXPORT_RESULT_SUCCESS = "导出了绩效结果";
    String OPS_PERF_CREATE_RECORD_SUB_TYPE = "创建绩效记录";
    String OPS_PERF_CREATE_RECORD_SUCCESS = "创建了绩效记录";
    String OPS_PERF_CALCULATE_RECORD_SUB_TYPE = "计算绩效";
    String OPS_PERF_CALCULATE_RECORD_SUCCESS = "计算了绩效";
    String OPS_PERF_ADJUST_RECORD_SUB_TYPE = "调整绩效";
    String OPS_PERF_ADJUST_RECORD_SUCCESS = "调整了绩效";
    String OPS_PERF_CONFIRM_RECORD_SUB_TYPE = "确认绩效";
    String OPS_PERF_CONFIRM_RECORD_SUCCESS = "确认了绩效";
    String OPS_PERF_CREATE_TEMPLATE_SUB_TYPE = "创建绩效模板";
    String OPS_PERF_CREATE_TEMPLATE_SUCCESS = "创建了绩效模板【{{#template.name}}】";
    String OPS_PERF_UPDATE_TEMPLATE_SUB_TYPE = "更新绩效模板";
    String OPS_PERF_UPDATE_TEMPLATE_SUCCESS = "更新了绩效模板【{{#template.name}}】: {_DIFF{#req}}";
    String OPS_PERF_ACTIVATE_TEMPLATE_SUB_TYPE = "激活绩效模板";
    String OPS_PERF_ACTIVATE_TEMPLATE_SUCCESS = "激活了绩效模板【{{#template.name}}】";

    String OPS_ORDER_ATTRIBUTION_TYPE = "OPS 订单归因";
    String OPS_ORDER_ATTRIBUTION_EXPORT_SUB_TYPE = "导出订单归因";
    String OPS_ORDER_ATTRIBUTION_EXPORT_SUCCESS = "导出了订单归因数据";

    // ======================= M4 账号资产 =======================

    String OPS_ACCOUNT_TYPE = "OPS 平台账号";
    String OPS_ACCOUNT_CREATE_SUB_TYPE = "创建平台账号";
    String OPS_ACCOUNT_CREATE_SUCCESS = "创建了平台账号【{{#account.name}}】";
    String OPS_ACCOUNT_UPDATE_SUB_TYPE = "更新平台账号";
    String OPS_ACCOUNT_UPDATE_SUCCESS = "更新了平台账号【{{#account.name}}】: {_DIFF{#req}}";
    String OPS_ACCOUNT_DELETE_SUB_TYPE = "删除平台账号";
    String OPS_ACCOUNT_DELETE_SUCCESS = "删除了平台账号【{{#account.name}}】";
    String OPS_ACCOUNT_REPLACE_SUB_TYPE = "替换平台账号";
    String OPS_ACCOUNT_REPLACE_SUCCESS = "替换了平台账号";

    String OPS_WEWORK_TYPE = "OPS 企微账号";
    String OPS_WEWORK_CREATE_SUB_TYPE = "创建企微账号";
    String OPS_WEWORK_CREATE_SUCCESS = "创建了企微账号";
    String OPS_WEWORK_UPDATE_SUB_TYPE = "更新企微账号";
    String OPS_WEWORK_UPDATE_SUCCESS = "更新了企微账号: {_DIFF{#req}}";
    String OPS_WEWORK_DELETE_SUB_TYPE = "删除企微账号";
    String OPS_WEWORK_DELETE_SUCCESS = "删除了企微账号";

    String OPS_WEWORK_EMPLOYEE_TYPE = "OPS 企微员工";
    String OPS_WEWORK_EMPLOYEE_CREATE_SUB_TYPE = "创建企微员工";
    String OPS_WEWORK_EMPLOYEE_CREATE_SUCCESS = "创建了企微员工";
    String OPS_WEWORK_EMPLOYEE_UPDATE_SUB_TYPE = "更新企微员工";
    String OPS_WEWORK_EMPLOYEE_UPDATE_SUCCESS = "更新了企微员工: {_DIFF{#req}}";
    String OPS_WEWORK_EMPLOYEE_DELETE_SUB_TYPE = "删除企微员工";
    String OPS_WEWORK_EMPLOYEE_DELETE_SUCCESS = "删除了企微员工";

    String OPS_PERSONAL_WECHAT_TYPE = "OPS 个人微信";
    String OPS_PERSONAL_WECHAT_CREATE_SUB_TYPE = "创建个人微信";
    String OPS_PERSONAL_WECHAT_CREATE_SUCCESS = "创建了个人微信";
    String OPS_PERSONAL_WECHAT_UPDATE_SUB_TYPE = "更新个人微信";
    String OPS_PERSONAL_WECHAT_UPDATE_SUCCESS = "更新了个人微信: {_DIFF{#req}}";
    String OPS_PERSONAL_WECHAT_DELETE_SUB_TYPE = "删除个人微信";
    String OPS_PERSONAL_WECHAT_DELETE_SUCCESS = "删除了个人微信";
    String OPS_PERSONAL_WECHAT_API_CONFIG_SUB_TYPE = "配置个人微信 API";
    String OPS_PERSONAL_WECHAT_API_CONFIG_SUCCESS = "配置了个人微信 API";

    String OPS_REALNAME_TYPE = "OPS 实名";
    String OPS_REALNAME_CREATE_SUB_TYPE = "创建实名";
    String OPS_REALNAME_CREATE_SUCCESS = "创建了实名【{{#realname.name}}】";
    String OPS_REALNAME_UPDATE_SUB_TYPE = "更新实名";
    String OPS_REALNAME_UPDATE_SUCCESS = "更新了实名【{{#realname.name}}】: {_DIFF{#req}}";
    String OPS_REALNAME_DELETE_SUB_TYPE = "删除实名";
    String OPS_REALNAME_DELETE_SUCCESS = "删除了实名【{{#realname.name}}】";

    String OPS_INTERMEDIARY_TYPE = "OPS 实名中介";
    String OPS_INTERMEDIARY_CREATE_SUB_TYPE = "创建实名中介";
    String OPS_INTERMEDIARY_CREATE_SUCCESS = "创建了实名中介";
    String OPS_INTERMEDIARY_UPDATE_SUB_TYPE = "更新实名中介";
    String OPS_INTERMEDIARY_UPDATE_SUCCESS = "更新了实名中介: {_DIFF{#req}}";
    String OPS_INTERMEDIARY_DELETE_SUB_TYPE = "删除实名中介";
    String OPS_INTERMEDIARY_DELETE_SUCCESS = "删除了实名中介";

    String OPS_TRIPLE_REL_TYPE = "OPS 三元关系";
    String OPS_TRIPLE_REL_CREATE_SUB_TYPE = "创建三元关系";
    String OPS_TRIPLE_REL_CREATE_SUCCESS = "创建了三元关系";
    String OPS_TRIPLE_REL_UNBIND_SUB_TYPE = "解绑三元关系";
    String OPS_TRIPLE_REL_UNBIND_SUCCESS = "解绑了三元关系";
    String OPS_TRIPLE_REL_REBIND_SUB_TYPE = "重绑三元关系";
    String OPS_TRIPLE_REL_REBIND_SUCCESS = "重绑了三元关系";

    String OPS_FAN_GROUP_TYPE = "OPS 粉丝群";
    String OPS_FAN_GROUP_CREATE_SUB_TYPE = "创建粉丝群";
    String OPS_FAN_GROUP_CREATE_SUCCESS = "创建了粉丝群";
    String OPS_FAN_GROUP_UPDATE_SUB_TYPE = "更新粉丝群";
    String OPS_FAN_GROUP_UPDATE_SUCCESS = "更新了粉丝群: {_DIFF{#req}}";
    String OPS_FAN_GROUP_DELETE_SUB_TYPE = "删除粉丝群";
    String OPS_FAN_GROUP_DELETE_SUCCESS = "删除了粉丝群";

    String OPS_WECHAT_CERT_RENEWAL_TYPE = "OPS 微信认证续期";
    String OPS_WECHAT_CERT_RENEWAL_CREATE_SUB_TYPE = "创建认证续期";
    String OPS_WECHAT_CERT_RENEWAL_CREATE_SUCCESS = "创建了微信认证续期";
    String OPS_WECHAT_CERT_RENEWAL_DELETE_SUB_TYPE = "删除认证续期";
    String OPS_WECHAT_CERT_RENEWAL_DELETE_SUCCESS = "删除了微信认证续期";

    String OPS_SIMCARD_TYPE = "OPS SIM 卡";
    String OPS_SIMCARD_CREATE_SUB_TYPE = "创建 SIM 卡";
    String OPS_SIMCARD_CREATE_SUCCESS = "创建了 SIM 卡";
    String OPS_SIMCARD_UPDATE_SUB_TYPE = "更新 SIM 卡";
    String OPS_SIMCARD_UPDATE_SUCCESS = "更新了 SIM 卡: {_DIFF{#req}}";
    String OPS_SIMCARD_DELETE_SUB_TYPE = "删除 SIM 卡";
    String OPS_SIMCARD_DELETE_SUCCESS = "删除了 SIM 卡";

    String OPS_PHONE_TYPE = "OPS 手机号";
    String OPS_PHONE_CREATE_SUB_TYPE = "创建手机号";
    String OPS_PHONE_CREATE_SUCCESS = "创建了手机号";
    String OPS_PHONE_UPDATE_SUB_TYPE = "更新手机号";
    String OPS_PHONE_UPDATE_SUCCESS = "更新了手机号: {_DIFF{#req}}";
    String OPS_PHONE_DELETE_SUB_TYPE = "删除手机号";
    String OPS_PHONE_DELETE_SUCCESS = "删除了手机号";

    // ======================= M5 财务 =======================

    String OPS_ACCOUNT_COST_TYPE = "OPS 账号成本";
    String OPS_ACCOUNT_COST_CREATE_SUB_TYPE = "创建账号成本";
    String OPS_ACCOUNT_COST_CREATE_SUCCESS = "创建了账号成本";
    String OPS_ACCOUNT_COST_UPDATE_SUB_TYPE = "更新账号成本";
    String OPS_ACCOUNT_COST_UPDATE_SUCCESS = "更新了账号成本: {_DIFF{#req}}";
    String OPS_ACCOUNT_COST_DELETE_SUB_TYPE = "删除账号成本";
    String OPS_ACCOUNT_COST_DELETE_SUCCESS = "删除了账号成本";

    String OPS_FINANCE_ROI_TYPE = "OPS 财务 ROI";
    String OPS_FINANCE_ROI_EXPORT_SUB_TYPE = "导出 ROI";
    String OPS_FINANCE_ROI_EXPORT_SUCCESS = "导出了 ROI 数据";

    // ======================= M6 分析 =======================

    String OPS_METRIC_TYPE = "OPS 指标";
    String OPS_METRIC_CREATE_SUB_TYPE = "创建指标";
    String OPS_METRIC_CREATE_SUCCESS = "创建了指标【{{#metric.name}}】";
    String OPS_METRIC_UPDATE_SUB_TYPE = "更新指标";
    String OPS_METRIC_UPDATE_SUCCESS = "更新了指标【{{#metric.name}}】: {_DIFF{#req}}";
    String OPS_METRIC_DELETE_SUB_TYPE = "删除指标";
    String OPS_METRIC_DELETE_SUCCESS = "删除了指标【{{#metric.name}}】";

    String OPS_DASHBOARD_TYPE = "OPS 仪表盘";
    String OPS_DASHBOARD_CREATE_SUB_TYPE = "创建仪表盘";
    String OPS_DASHBOARD_CREATE_SUCCESS = "创建了仪表盘";
    String OPS_DASHBOARD_UPDATE_CONFIG_SUB_TYPE = "更新仪表盘配置";
    String OPS_DASHBOARD_UPDATE_CONFIG_SUCCESS = "更新了仪表盘配置";
    String OPS_DASHBOARD_FULL_UPDATE_SUB_TYPE = "全量更新仪表盘";
    String OPS_DASHBOARD_FULL_UPDATE_SUCCESS = "全量更新了仪表盘";

    String OPS_CUSTOM_QUERY_TYPE = "OPS 自定义查询";
    String OPS_CUSTOM_QUERY_CREATE_SUB_TYPE = "创建自定义查询";
    String OPS_CUSTOM_QUERY_CREATE_SUCCESS = "创建了自定义查询【{{#query.name}}】";
    String OPS_CUSTOM_QUERY_UPDATE_SUB_TYPE = "更新自定义查询";
    String OPS_CUSTOM_QUERY_UPDATE_SUCCESS = "更新了自定义查询【{{#query.name}}】: {_DIFF{#req}}";
    String OPS_CUSTOM_QUERY_PUBLISH_SUB_TYPE = "发布自定义查询";
    String OPS_CUSTOM_QUERY_PUBLISH_SUCCESS = "发布了自定义查询【{{#query.name}}】";

    String OPS_FUNNEL_TYPE = "OPS 漏斗";
    String OPS_FUNNEL_CREATE_SUB_TYPE = "创建漏斗";
    String OPS_FUNNEL_CREATE_SUCCESS = "创建了漏斗";
    String OPS_FUNNEL_EXPORT_SUB_TYPE = "导出漏斗";
    String OPS_FUNNEL_EXPORT_SUCCESS = "导出了漏斗数据";

    // ======================= M7 报表 =======================

    String OPS_REPORT_TYPE = "OPS 报表";
    String OPS_REPORT_EXPORT_UNIFIED_ACCOUNT_SUB_TYPE = "导出统一账号报表";
    String OPS_REPORT_EXPORT_UNIFIED_ACCOUNT_SUCCESS = "导出了统一账号报表";
    String OPS_REPORT_EXPORT_ACCOUNT_STATUS_SUB_TYPE = "导出账号状态报表";
    String OPS_REPORT_EXPORT_ACCOUNT_STATUS_SUCCESS = "导出了账号状态报表";
    String OPS_REPORT_EXPORT_VIDEO_OUTPUT_SUB_TYPE = "导出视频产出报表";
    String OPS_REPORT_EXPORT_VIDEO_OUTPUT_SUCCESS = "导出了视频产出报表";
    String OPS_REPORT_EXPORT_LIVE_DURATION_SUB_TYPE = "导出直播时长报表";
    String OPS_REPORT_EXPORT_LIVE_DURATION_SUCCESS = "导出了直播时长报表";
    String OPS_REPORT_EXPORT_COST_ALLOCATION_SUB_TYPE = "导出成本分摊报表";
    String OPS_REPORT_EXPORT_COST_ALLOCATION_SUCCESS = "导出了成本分摊报表";
    String OPS_REPORT_EXPORT_ROI_SUB_TYPE = "导出 ROI 报表";
    String OPS_REPORT_EXPORT_ROI_SUCCESS = "导出了 ROI 报表";
    String OPS_REPORT_EXPORT_ACCOUNT_ALERT_SUB_TYPE = "导出账号告警报表";
    String OPS_REPORT_EXPORT_ACCOUNT_ALERT_SUCCESS = "导出了账号告警报表";

    // ======================= M8 配置 =======================

    String OPS_METADATA_TYPE = "OPS 元数据";
    String OPS_METADATA_CREATE_SUB_TYPE = "创建元数据";
    String OPS_METADATA_CREATE_SUCCESS = "创建了元数据【{{#metadata.name}}】";
    String OPS_METADATA_UPDATE_SUB_TYPE = "更新元数据";
    String OPS_METADATA_UPDATE_SUCCESS = "更新了元数据【{{#metadata.name}}】: {_DIFF{#req}}";
    String OPS_METADATA_UPDATE_FIELDS_SUB_TYPE = "更新元数据字段";
    String OPS_METADATA_UPDATE_FIELDS_SUCCESS = "更新了元数据字段";
    String OPS_METADATA_DELETE_SUB_TYPE = "删除元数据";
    String OPS_METADATA_DELETE_SUCCESS = "删除了元数据【{{#metadata.name}}】";

    String OPS_COLLECT_CONFIG_TYPE = "OPS 采集配置";
    String OPS_COLLECT_CONFIG_CREATE_SUB_TYPE = "创建采集配置";
    String OPS_COLLECT_CONFIG_CREATE_SUCCESS = "创建了采集配置";
    String OPS_COLLECT_CONFIG_UPDATE_SUB_TYPE = "更新采集配置";
    String OPS_COLLECT_CONFIG_UPDATE_SUCCESS = "更新了采集配置: {_DIFF{#req}}";
    String OPS_COLLECT_CONFIG_TOGGLE_STATUS_SUB_TYPE = "切换采集配置状态";
    String OPS_COLLECT_CONFIG_TOGGLE_STATUS_SUCCESS = "切换了采集配置状态";
    String OPS_COLLECT_CONFIG_TEST_CONNECTION_SUB_TYPE = "测试采集连接";
    String OPS_COLLECT_CONFIG_TEST_CONNECTION_SUCCESS = "测试了采集连接";
    String OPS_COLLECT_CONFIG_IMPORT_EXTERNAL_SUB_TYPE = "导入外部采集配置";
    String OPS_COLLECT_CONFIG_IMPORT_EXTERNAL_SUCCESS = "导入了外部采集配置";
    String OPS_COLLECT_CONFIG_DELETE_SUB_TYPE = "删除采集配置";
    String OPS_COLLECT_CONFIG_DELETE_SUCCESS = "删除了采集配置";

    String OPS_THRESHOLD_TYPE = "OPS 阈值配置";
    String OPS_THRESHOLD_CREATE_SUB_TYPE = "创建阈值配置";
    String OPS_THRESHOLD_CREATE_SUCCESS = "创建了阈值配置";
    String OPS_THRESHOLD_UPDATE_SUB_TYPE = "更新阈值配置";
    String OPS_THRESHOLD_UPDATE_SUCCESS = "更新了阈值配置: {_DIFF{#req}}";
    String OPS_THRESHOLD_DELETE_SUB_TYPE = "删除阈值配置";
    String OPS_THRESHOLD_DELETE_SUCCESS = "删除了阈值配置";

    String OPS_KEYWORD_TYPE = "OPS 关键词配置";
    String OPS_KEYWORD_CREATE_SUB_TYPE = "创建关键词";
    String OPS_KEYWORD_CREATE_SUCCESS = "创建了关键词";
    String OPS_KEYWORD_UPDATE_SUB_TYPE = "更新关键词";
    String OPS_KEYWORD_UPDATE_SUCCESS = "更新了关键词: {_DIFF{#req}}";
    String OPS_KEYWORD_DELETE_SUB_TYPE = "删除关键词";
    String OPS_KEYWORD_DELETE_SUCCESS = "删除了关键词";

    String OPS_AI_MODEL_TYPE = "OPS AI 模型";
    String OPS_AI_MODEL_CREATE_SUB_TYPE = "创建 AI 模型";
    String OPS_AI_MODEL_CREATE_SUCCESS = "创建了 AI 模型【{{#model.name}}】";
    String OPS_AI_MODEL_UPDATE_SUB_TYPE = "更新 AI 模型";
    String OPS_AI_MODEL_UPDATE_SUCCESS = "更新了 AI 模型【{{#model.name}}】: {_DIFF{#req}}";
    String OPS_AI_MODEL_TEST_CONNECTION_SUB_TYPE = "测试 AI 模型连接";
    String OPS_AI_MODEL_TEST_CONNECTION_SUCCESS = "测试了 AI 模型连接";
    String OPS_AI_MODEL_SET_DEFAULT_SUB_TYPE = "设置默认 AI 模型";
    String OPS_AI_MODEL_SET_DEFAULT_SUCCESS = "设置了默认 AI 模型";
    String OPS_AI_MODEL_DELETE_SUB_TYPE = "删除 AI 模型";
    String OPS_AI_MODEL_DELETE_SUCCESS = "删除了 AI 模型【{{#model.name}}】";

    String OPS_AI_PROMPT_TYPE = "OPS AI 提示词";
    String OPS_AI_PROMPT_CREATE_SUB_TYPE = "创建 AI 提示词";
    String OPS_AI_PROMPT_CREATE_SUCCESS = "创建了 AI 提示词";
    String OPS_AI_PROMPT_UPDATE_SUB_TYPE = "更新 AI 提示词";
    String OPS_AI_PROMPT_UPDATE_SUCCESS = "更新了 AI 提示词: {_DIFF{#req}}";
    String OPS_AI_PROMPT_DELETE_SUB_TYPE = "删除 AI 提示词";
    String OPS_AI_PROMPT_DELETE_SUCCESS = "删除了 AI 提示词";

    String OPS_AOCREATE_TYPE = "OPS 傲创 API";
    String OPS_AOCREATE_SAVE_SUB_TYPE = "保存傲创 API 配置";
    String OPS_AOCREATE_SAVE_SUCCESS = "保存了傲创 API 配置";

    String OPS_AOCREATE_ACCOUNT_TYPE = "OPS 傲创账号";
    String OPS_AOCREATE_ACCOUNT_CREATE_SUB_TYPE = "创建傲创账号";
    String OPS_AOCREATE_ACCOUNT_CREATE_SUCCESS = "创建了傲创账号";
    String OPS_AOCREATE_ACCOUNT_UPDATE_SUB_TYPE = "更新傲创账号";
    String OPS_AOCREATE_ACCOUNT_UPDATE_SUCCESS = "更新了傲创账号: {_DIFF{#req}}";
    String OPS_AOCREATE_ACCOUNT_DELETE_SUB_TYPE = "删除傲创账号";
    String OPS_AOCREATE_ACCOUNT_DELETE_SUCCESS = "删除了傲创账号";
    String OPS_AOCREATE_ACCOUNT_TEST_CONNECTION_SUB_TYPE = "测试傲创连接";
    String OPS_AOCREATE_ACCOUNT_TEST_CONNECTION_SUCCESS = "测试了傲创连接";
    String OPS_AOCREATE_ACCOUNT_SYNC_REMOTE_SUB_TYPE = "同步傲创远程";
    String OPS_AOCREATE_ACCOUNT_SYNC_REMOTE_SUCCESS = "同步了傲创远程数据";

    // ======================= M10 采集 =======================

    String OPS_COLLECT_TASK_TYPE = "OPS 采集任务";
    String OPS_COLLECT_TASK_CREATE_SUB_TYPE = "创建采集任务";
    String OPS_COLLECT_TASK_CREATE_SUCCESS = "创建了采集任务";
    String OPS_COLLECT_TASK_UPDATE_SUB_TYPE = "更新采集任务";
    String OPS_COLLECT_TASK_UPDATE_SUCCESS = "更新了采集任务: {_DIFF{#req}}";
    String OPS_COLLECT_TASK_DELETE_SUB_TYPE = "删除采集任务";
    String OPS_COLLECT_TASK_DELETE_SUCCESS = "删除了采集任务";
    String OPS_COLLECT_TASK_RUN_SUB_TYPE = "运行采集任务";
    String OPS_COLLECT_TASK_RUN_SUCCESS = "运行了采集任务";
    String OPS_COLLECT_TASK_START_SUB_TYPE = "启动采集任务";
    String OPS_COLLECT_TASK_START_SUCCESS = "启动了采集任务";
    String OPS_COLLECT_TASK_STOP_SUB_TYPE = "停止采集任务";
    String OPS_COLLECT_TASK_STOP_SUCCESS = "停止了采集任务";
    String OPS_COLLECT_TASK_UPDATE_STATUS_SUB_TYPE = "更新采集任务状态";
    String OPS_COLLECT_TASK_UPDATE_STATUS_SUCCESS = "更新了采集任务状态";

    String OPS_MESSAGE_SYNC_TYPE = "OPS 消息同步";
    String OPS_MESSAGE_SYNC_MESSAGES_SUB_TYPE = "同步消息";
    String OPS_MESSAGE_SYNC_MESSAGES_SUCCESS = "同步了消息";

    String OPS_DEVICE_SYNC_TYPE = "OPS 设备同步";
    String OPS_DEVICE_SYNC_DEVICES_SUB_TYPE = "同步设备";
    String OPS_DEVICE_SYNC_DEVICES_SUCCESS = "同步了设备";
    String OPS_DEVICE_SYNC_BIND_DEVICE_SUB_TYPE = "绑定设备";
    String OPS_DEVICE_SYNC_BIND_DEVICE_SUCCESS = "绑定了设备";
    String OPS_DEVICE_SYNC_CREATE_AND_BIND_SUB_TYPE = "创建并绑定设备";
    String OPS_DEVICE_SYNC_CREATE_AND_BIND_SUCCESS = "创建并绑定了设备";

    String OPS_COLLECTOR_BIND_TYPE = "OPS 采集器绑定";
    String OPS_COLLECTOR_BIND_BATCH_IMPORT_SUB_TYPE = "批量导入采集器绑定";
    String OPS_COLLECTOR_BIND_BATCH_IMPORT_SUCCESS = "批量导入了采集器绑定";
    String OPS_COLLECTOR_BIND_SAVE_SUB_TYPE = "保存采集器绑定";
    String OPS_COLLECTOR_BIND_SAVE_SUCCESS = "保存了采集器绑定";
    String OPS_COLLECTOR_BIND_BIND_SUB_TYPE = "绑定采集器";
    String OPS_COLLECTOR_BIND_BIND_SUCCESS = "绑定了采集器";
    String OPS_COLLECTOR_BIND_SYNC_CREDENTIALS_SUB_TYPE = "同步采集器凭证";
    String OPS_COLLECTOR_BIND_SYNC_CREDENTIALS_SUCCESS = "同步了采集器凭证";
    String OPS_COLLECTOR_BIND_TEST_CONNECTION_SUB_TYPE = "测试采集器连接";
    String OPS_COLLECTOR_BIND_TEST_CONNECTION_SUCCESS = "测试了采集器连接";

    String OPS_COLLECTOR_QR_TYPE = "OPS 采集器扫码";
    String OPS_COLLECTOR_QR_START_SUB_TYPE = "启动扫码登录";
    String OPS_COLLECTOR_QR_START_SUCCESS = "启动了采集器扫码登录";
    String OPS_COLLECTOR_QR_POLL_SUB_TYPE = "轮询扫码登录";
    String OPS_COLLECTOR_QR_POLL_SUCCESS = "轮询了采集器扫码登录";
    String OPS_COLLECTOR_QR_CANCEL_SUB_TYPE = "取消扫码登录";
    String OPS_COLLECTOR_QR_CANCEL_SUCCESS = "取消了采集器扫码登录";

    String OPS_PRIVATE_DOMAIN_BRIDGE_TYPE = "OPS 私域桥接";
    String OPS_PRIVATE_DOMAIN_BRIDGE_CREATE_SUB_TYPE = "创建私域桥接";
    String OPS_PRIVATE_DOMAIN_BRIDGE_CREATE_SUCCESS = "创建了私域桥接";
    String OPS_PRIVATE_DOMAIN_BRIDGE_CONFIRM_SUB_TYPE = "确认私域桥接";
    String OPS_PRIVATE_DOMAIN_BRIDGE_CONFIRM_SUCCESS = "确认了私域桥接";
    String OPS_PRIVATE_DOMAIN_BRIDGE_REJECT_SUB_TYPE = "驳回私域桥接";
    String OPS_PRIVATE_DOMAIN_BRIDGE_REJECT_SUCCESS = "驳回了私域桥接";

    String OPS_FRIEND_SYNC_TYPE = "OPS 好友同步";
    String OPS_FRIEND_SYNC_FRIENDS_SUB_TYPE = "同步好友";
    String OPS_FRIEND_SYNC_FRIENDS_SUCCESS = "同步了好友";

    String OPS_WEWORK_ADAPTER_TYPE = "OPS 企微适配";
    String OPS_WEWORK_TEST_CONNECTION_SUB_TYPE = "测试企微连接";
    String OPS_WEWORK_TEST_CONNECTION_SUCCESS = "测试了企微连接";

    // ======================= 公司管理 / 健康检查 =======================

    String OPS_COMPANY_TYPE = "OPS 公司管理";
    String OPS_COMPANY_CREATE_SUB_TYPE = "新增公司";
    String OPS_COMPANY_CREATE_SUCCESS = "新增了公司【{{#company.name}}】";
    String OPS_COMPANY_UPDATE_SUB_TYPE = "修改公司";
    String OPS_COMPANY_UPDATE_SUCCESS = "修改了公司【{{#company.name}}】: {_DIFF{#req}}";
    String OPS_COMPANY_DELETE_SUB_TYPE = "删除公司";
    String OPS_COMPANY_DELETE_SUCCESS = "删除了公司【{{#company.name}}】";
    String OPS_COMPANY_EXPAND_SUB_TYPE = "公司扩容";
    String OPS_COMPANY_EXPAND_SUCCESS = "为公司【{{#company.name}}】扩容";

    String OPS_HEALTH_TYPE = "OPS 健康检查";
    String OPS_HEALTH_HELLO_SUB_TYPE = "Hello";
    String OPS_HEALTH_HELLO_SUCCESS = "调用了健康检查接口";
}
