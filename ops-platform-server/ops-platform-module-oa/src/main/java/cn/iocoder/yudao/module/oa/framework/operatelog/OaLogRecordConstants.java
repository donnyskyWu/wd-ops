package cn.iocoder.yudao.module.oa.framework.operatelog;

/**
 * OPS {@code @LogRecord} constants (AL-06/AL-07). Extend per module as workers migrate {@code @AuditLog}.
 */
public final class OaLogRecordConstants {

    private OaLogRecordConstants() {
    }

    /** bizNo when no entity id (export, hello, etc.) */
    public static final String BIZ_NO_NONE = "0";

    // ----- M4-phone -----
    public static final String M4_PHONE_TYPE = "M4-phone";
    public static final String M4_PHONE_CREATE_SUB_TYPE = "create";
    public static final String M4_PHONE_CREATE_SUCCESS = "新增了手机号【{{#phone.phoneCode}}】";
    public static final String M4_PHONE_UPDATE_SUB_TYPE = "update";
    public static final String M4_PHONE_UPDATE_SUCCESS = "修改了手机号【{{#phone.phoneCode}}】";
    public static final String M4_PHONE_DELETE_SUB_TYPE = "delete";
    public static final String M4_PHONE_DELETE_SUCCESS = "删除了手机号【{{#phone.phoneCode}}】";

    // ----- M4-simcard -----
    public static final String M4_SIMCARD_TYPE = "M4-simcard";
    public static final String M4_SIMCARD_CREATE_SUB_TYPE = "create";
    public static final String M4_SIMCARD_CREATE_SUCCESS = "新增了SIM卡【{{#simCard.id}}】";
    public static final String M4_SIMCARD_UPDATE_SUB_TYPE = "update";
    public static final String M4_SIMCARD_UPDATE_SUCCESS = "修改了SIM卡【{{#simCard.id}}】";
    public static final String M4_SIMCARD_DELETE_SUB_TYPE = "delete";
    public static final String M4_SIMCARD_DELETE_SUCCESS = "删除了SIM卡【{{#simCard.id}}】";

    // ----- M4-wework -----
    public static final String M4_WEWORK_TYPE = "M4-wework";
    public static final String M4_WEWORK_CREATE_SUB_TYPE = "create";
    public static final String M4_WEWORK_CREATE_SUCCESS = "新增了企微账号【{{#weworkAccount.accountName}}】";
    public static final String M4_WEWORK_UPDATE_SUB_TYPE = "update";
    public static final String M4_WEWORK_UPDATE_SUCCESS = "修改了企微账号【{{#weworkAccount.accountName}}】";
    public static final String M4_WEWORK_DELETE_SUB_TYPE = "delete";
    public static final String M4_WEWORK_DELETE_SUCCESS = "删除了企微账号【{{#weworkAccount.accountName}}】";

    // ----- M4-fan-group -----
    public static final String M4_FAN_GROUP_TYPE = "M4-fan-group";
    public static final String M4_FAN_GROUP_CREATE_SUB_TYPE = "create";
    public static final String M4_FAN_GROUP_CREATE_SUCCESS = "新增了粉丝群【{{#fanGroup.groupName}}】";
    public static final String M4_FAN_GROUP_UPDATE_SUB_TYPE = "update";
    public static final String M4_FAN_GROUP_UPDATE_SUCCESS = "修改了粉丝群【{{#fanGroup.groupName}}】";
    public static final String M4_FAN_GROUP_DELETE_SUB_TYPE = "delete";
    public static final String M4_FAN_GROUP_DELETE_SUCCESS = "删除了粉丝群【{{#fanGroup.groupName}}】";

    // ----- M4-wechat-cert-renewal -----
    public static final String M4_WECHAT_CERT_RENEWAL_TYPE = "M4-wechat-cert-renewal";
    public static final String M4_WECHAT_CERT_RENEWAL_CREATE_SUB_TYPE = "create";
    public static final String M4_WECHAT_CERT_RENEWAL_CREATE_SUCCESS = "新增了公众号认证续费记录【{{#renewal.id}}】";
    public static final String M4_WECHAT_CERT_RENEWAL_DELETE_SUB_TYPE = "delete";
    public static final String M4_WECHAT_CERT_RENEWAL_DELETE_SUCCESS = "删除了公众号认证续费记录【{{#renewal.id}}】";

    // ----- M4-personal-wechat -----
    public static final String M4_PERSONAL_WECHAT_TYPE = "M4-personal-wechat";
    public static final String M4_PERSONAL_WECHAT_CREATE_SUB_TYPE = "create";
    public static final String M4_PERSONAL_WECHAT_CREATE_SUCCESS = "新增了个人微信【{{#personalWechat.accountName}}】";
    public static final String M4_PERSONAL_WECHAT_UPDATE_SUB_TYPE = "update";
    public static final String M4_PERSONAL_WECHAT_UPDATE_SUCCESS = "修改了个人微信【{{#personalWechat.accountName}}】";
    public static final String M4_PERSONAL_WECHAT_DELETE_SUB_TYPE = "delete";
    public static final String M4_PERSONAL_WECHAT_DELETE_SUCCESS = "删除了个人微信【{{#personalWechat.accountName}}】";
    public static final String M4_PERSONAL_WECHAT_API_CONFIG_SUB_TYPE = "api-config";
    public static final String M4_PERSONAL_WECHAT_API_CONFIG_SUCCESS = "配置了个人微信 API【{{#personalWechat.accountName}}】";

    // ----- M9-param (AL-06 pilot) -----
    public static final String M9_PARAM_TYPE = "M9-param";
    public static final String M9_PARAM_CREATE_SUB_TYPE = "create";
    public static final String M9_PARAM_CREATE_SUCCESS = "创建了系统参数【{{#param.paramName}}】";
    public static final String M9_PARAM_UPDATE_SUB_TYPE = "update";
    public static final String M9_PARAM_UPDATE_SUCCESS = "更新了系统参数【{{#param.paramName}}】: {_DIFF{#req}}";
    public static final String M9_PARAM_DELETE_SUB_TYPE = "delete";
    public static final String M9_PARAM_DELETE_SUCCESS = "删除了系统参数【{{#param.paramName}}】";

    // ----- M1-ip-group -----
    public static final String M1_IP_GROUP_TYPE = "M1-ip-group";
    public static final String M1_IP_GROUP_CREATE_SUB_TYPE = "create";
    public static final String M1_IP_GROUP_CREATE_SUCCESS = "创建了IP组【{{#ipGroup.groupName}}】";
    public static final String M1_IP_GROUP_UPDATE_SUB_TYPE = "update";
    public static final String M1_IP_GROUP_UPDATE_SUCCESS = "更新了IP组【{{#ipGroup.groupName}}】: {_DIFF{#req}}";
    public static final String M1_IP_GROUP_UPDATE_STATUS_SUB_TYPE = "update-status";
    public static final String M1_IP_GROUP_UPDATE_STATUS_SUCCESS = "变更了IP组【{{#ipGroup.groupName}}】状态";
    public static final String M1_IP_GROUP_DELETE_SUB_TYPE = "delete";
    public static final String M1_IP_GROUP_DELETE_SUCCESS = "删除了IP组【{{#ipGroup.groupName}}】";
    public static final String M1_IP_GROUP_ADD_MEMBER_SUB_TYPE = "add-member";
    public static final String M1_IP_GROUP_ADD_MEMBER_SUCCESS = "为IP组【{{#ipGroup.groupName}}】添加了成员";
    public static final String M1_IP_GROUP_UPDATE_MEMBER_SUB_TYPE = "update-member";
    public static final String M1_IP_GROUP_UPDATE_MEMBER_SUCCESS = "更新了IP组【{{#ipGroup.groupName}}】成员";
    public static final String M1_IP_GROUP_DELETE_MEMBER_SUB_TYPE = "delete-member";
    public static final String M1_IP_GROUP_DELETE_MEMBER_SUCCESS = "删除了IP组【{{#ipGroup.groupName}}】成员";
    public static final String M1_IP_GROUP_BIND_ACCOUNTS_SUB_TYPE = "bind-accounts";
    public static final String M1_IP_GROUP_BIND_ACCOUNTS_SUCCESS = "为IP组【{{#ipGroup.groupName}}】绑定了账号";
    public static final String M1_IP_GROUP_UNBIND_ACCOUNT_SUB_TYPE = "unbind-account";
    public static final String M1_IP_GROUP_UNBIND_ACCOUNT_SUCCESS = "为IP组【{{#ipGroup.groupName}}】解绑了账号";
    public static final String M1_IP_GROUP_BIND_ANCHORS_SUB_TYPE = "bind-anchors";
    public static final String M1_IP_GROUP_BIND_ANCHORS_SUCCESS = "为IP组【{{#ipGroup.groupName}}】绑定了主播";
    public static final String M1_IP_GROUP_UNBIND_ANCHOR_SUB_TYPE = "unbind-anchor";
    public static final String M1_IP_GROUP_UNBIND_ANCHOR_SUCCESS = "为IP组【{{#ipGroup.groupName}}】解绑了主播";

    // ----- M1-author -----
    public static final String M1_AUTHOR_TYPE = "M1-author";
    public static final String M1_AUTHOR_CREATE_SUB_TYPE = "create";
    public static final String M1_AUTHOR_CREATE_SUCCESS = "创建了作者";
    public static final String M1_AUTHOR_UPDATE_SUB_TYPE = "update";
    public static final String M1_AUTHOR_UPDATE_SUCCESS = "更新了作者";
    public static final String M1_AUTHOR_DELETE_SUB_TYPE = "delete";
    public static final String M1_AUTHOR_DELETE_SUCCESS = "删除了作者";
    public static final String M1_AUTHOR_UPDATE_EXT_SUB_TYPE = "update-ext";
    public static final String M1_AUTHOR_UPDATE_EXT_SUCCESS = "更新了作者扩展信息【{{#author.nickname}}】";

    // ----- M1-internal-content -----
    public static final String M1_INTERNAL_CONTENT_TYPE = "M1-internal-content";
    public static final String M1_INTERNAL_CONTENT_IMPORT_SUBMIT_SUB_TYPE = "import-submit";
    public static final String M1_INTERNAL_CONTENT_IMPORT_SUBMIT_SUCCESS = "提交了内容导入【{{#importRecord.id}}】";
    public static final String M1_INTERNAL_CONTENT_IMPORT_REVIEW_SUB_TYPE = "import-review";
    public static final String M1_INTERNAL_CONTENT_IMPORT_REVIEW_SUCCESS = "审核了内容导入【{{#importRecord.id}}】";

    // ----- M1-ops-anchor -----
    public static final String M1_OPS_ANCHOR_TYPE = "M1-ops-anchor";
    public static final String M1_OPS_ANCHOR_CREATE_SUB_TYPE = "create";
    public static final String M1_OPS_ANCHOR_CREATE_SUCCESS = "创建了运营主播关系【{{#anchorRel.id}}】";
    public static final String M1_OPS_ANCHOR_UPDATE_SUB_TYPE = "update";
    public static final String M1_OPS_ANCHOR_UPDATE_SUCCESS = "更新了运营主播关系【{{#anchorRel.id}}】: {_DIFF{#req}}";
    public static final String M1_OPS_ANCHOR_DELETE_SUB_TYPE = "delete";
    public static final String M1_OPS_ANCHOR_DELETE_SUCCESS = "删除了运营主播关系【{{#anchorRel.id}}】";

    // ----- M1-productivity-review -----
    public static final String M1_PRODUCTIVITY_REVIEW_TYPE = "M1-productivity-review";
    public static final String M1_PRODUCTIVITY_REVIEW_EXPORT_SUB_TYPE = "export";
    public static final String M1_PRODUCTIVITY_REVIEW_EXPORT_SUCCESS = "导出了产能审核数据";

    // ----- M1-follower-analysis -----
    public static final String M1_FOLLOWER_ANALYSIS_TYPE = "M1-follower-analysis";
    public static final String M1_FOLLOWER_ANALYSIS_EXPORT_SUB_TYPE = "export";
    public static final String M1_FOLLOWER_ANALYSIS_EXPORT_SUCCESS = "导出了粉丝分析数据";

    // ----- M4-account -----
    public static final String M4_ACCOUNT_TYPE = "M4-account";
    public static final String M4_ACCOUNT_CREATE_SUB_TYPE = "create";
    public static final String M4_ACCOUNT_CREATE_SUCCESS = "创建了平台账号【{{#account.accountName}}】";
    public static final String M4_ACCOUNT_UPDATE_SUB_TYPE = "update";
    public static final String M4_ACCOUNT_UPDATE_SUCCESS = "更新了平台账号【{{#account.accountName}}】: {_DIFF{#req}}";
    public static final String M4_ACCOUNT_DELETE_SUB_TYPE = "delete";
    public static final String M4_ACCOUNT_DELETE_SUCCESS = "删除了平台账号【{{#account.accountName}}】";
    public static final String M4_ACCOUNT_REPLACE_SUB_TYPE = "replace";
    public static final String M4_ACCOUNT_REPLACE_SUCCESS = "替换了平台账号【{{#account.accountName}}】";

    // ----- M4-wework-employee -----
    public static final String M4_WEWORK_EMPLOYEE_TYPE = "M4-wework-employee";
    public static final String M4_WEWORK_EMPLOYEE_CREATE_SUB_TYPE = "create";
    public static final String M4_WEWORK_EMPLOYEE_CREATE_SUCCESS = "创建了企微员工【{{#employee.nickname}}】";
    public static final String M4_WEWORK_EMPLOYEE_UPDATE_SUB_TYPE = "update";
    public static final String M4_WEWORK_EMPLOYEE_UPDATE_SUCCESS = "更新了企微员工【{{#employee.nickname}}】";
    public static final String M4_WEWORK_EMPLOYEE_DELETE_SUB_TYPE = "delete";
    public static final String M4_WEWORK_EMPLOYEE_DELETE_SUCCESS = "删除了企微员工【{{#employee.nickname}}】";

    // ----- M4-company -----
    public static final String M4_COMPANY_TYPE = "M4-company";
    public static final String M4_COMPANY_CREATE_SUB_TYPE = "create";
    public static final String M4_COMPANY_CREATE_SUCCESS = "新增了公司【{{#company.companyName}}】";
    public static final String M4_COMPANY_UPDATE_SUB_TYPE = "update";
    public static final String M4_COMPANY_UPDATE_SUCCESS = "修改了公司【{{#company.companyName}}】: {_DIFF{#req}}";
    public static final String M4_COMPANY_DELETE_SUB_TYPE = "delete";
    public static final String M4_COMPANY_DELETE_SUCCESS = "删除了公司【{{#company.companyName}}】";
    public static final String M4_COMPANY_EXPAND_SUB_TYPE = "expand";
    public static final String M4_COMPANY_EXPAND_SUCCESS = "为公司【{{#company.companyName}}】扩容";

    // ----- M4-realname -----
    public static final String M4_REALNAME_TYPE = "M4-realname";
    public static final String M4_REALNAME_CREATE_SUB_TYPE = "create";
    public static final String M4_REALNAME_CREATE_SUCCESS = "创建了实名【{{#realname.realName}}】";
    public static final String M4_REALNAME_UPDATE_SUB_TYPE = "update";
    public static final String M4_REALNAME_UPDATE_SUCCESS = "更新了实名【{{#realname.realName}}】: {_DIFF{#req}}";
    public static final String M4_REALNAME_DELETE_SUB_TYPE = "delete";
    public static final String M4_REALNAME_DELETE_SUCCESS = "删除了实名【{{#realname.realName}}】";

    // ----- M4-intermediary -----
    public static final String M4_INTERMEDIARY_TYPE = "M4-intermediary";
    public static final String M4_INTERMEDIARY_CREATE_SUB_TYPE = "create";
    public static final String M4_INTERMEDIARY_CREATE_SUCCESS = "创建了实名中介【{{#intermediary.intermediaryName}}】";
    public static final String M4_INTERMEDIARY_UPDATE_SUB_TYPE = "update";
    public static final String M4_INTERMEDIARY_UPDATE_SUCCESS = "更新了实名中介【{{#intermediary.intermediaryName}}】";
    public static final String M4_INTERMEDIARY_DELETE_SUB_TYPE = "delete";
    public static final String M4_INTERMEDIARY_DELETE_SUCCESS = "删除了实名中介【{{#intermediary.intermediaryName}}】";

    // ----- M4-triple-rel -----
    public static final String M4_TRIPLE_REL_TYPE = "M4-triple-rel";
    public static final String M4_TRIPLE_REL_CREATE_SUB_TYPE = "create";
    public static final String M4_TRIPLE_REL_CREATE_SUCCESS = "创建了三元关系【{{#tripleRel.id}}】";
    public static final String M4_TRIPLE_REL_UNBIND_SUB_TYPE = "unbind";
    public static final String M4_TRIPLE_REL_UNBIND_SUCCESS = "解绑了三元关系【{{#tripleRel.id}}】";
    public static final String M4_TRIPLE_REL_REBIND_SUB_TYPE = "rebind";
    public static final String M4_TRIPLE_REL_REBIND_SUCCESS = "重绑了三元关系【{{#tripleRel.id}}】";

    // ----- M9-user -----
    public static final String M9_USER_TYPE = "M9-user";
    public static final String M9_USER_CREATE_SUB_TYPE = "create";
    public static final String M9_USER_CREATE_SUCCESS = "创建了用户【{{#user.username}}】";
    public static final String M9_USER_UPDATE_SUB_TYPE = "update";
    public static final String M9_USER_UPDATE_SUCCESS = "更新了用户【{{#user.username}}】: {_DIFF{#req}}";
    public static final String M9_USER_DELETE_SUB_TYPE = "delete";
    public static final String M9_USER_DELETE_SUCCESS = "删除了用户【{{#user.username}}】";

    // ----- M9-role -----
    public static final String M9_ROLE_TYPE = "M9-role";
    public static final String M9_ROLE_CREATE_SUB_TYPE = "create";
    public static final String M9_ROLE_CREATE_SUCCESS = "创建了角色【{{#role.name}}】";
    public static final String M9_ROLE_UPDATE_SUB_TYPE = "update";
    public static final String M9_ROLE_UPDATE_SUCCESS = "更新了角色【{{#role.name}}】: {_DIFF{#req}}";
    public static final String M9_ROLE_DELETE_SUB_TYPE = "delete";
    public static final String M9_ROLE_DELETE_SUCCESS = "删除了角色【{{#role.name}}】";
    public static final String M9_ROLE_ASSIGN_PERMISSION_SUB_TYPE = "assign-permission";
    public static final String M9_ROLE_ASSIGN_PERMISSION_SUCCESS = "为角色【{{#role.name}}】分配了权限";

    // ----- M9-dept -----
    public static final String M9_DEPT_TYPE = "M9-dept";
    public static final String M9_DEPT_CREATE_SUB_TYPE = "create";
    public static final String M9_DEPT_CREATE_SUCCESS = "创建了部门【{{#dept.name}}】";
    public static final String M9_DEPT_UPDATE_SUB_TYPE = "update";
    public static final String M9_DEPT_UPDATE_SUCCESS = "更新了部门【{{#dept.name}}】: {_DIFF{#req}}";
    public static final String M9_DEPT_DELETE_SUB_TYPE = "delete";
    public static final String M9_DEPT_DELETE_SUCCESS = "删除了部门【{{#dept.name}}】";
    public static final String M9_DEPT_SYNC_DINGTALK_SUB_TYPE = "sync-dingtalk";
    public static final String M9_DEPT_SYNC_DINGTALK_SUCCESS = "同步了钉钉部门";
    public static final String M9_DEPT_SYNC_DINGTALK_USERS_SUB_TYPE = "sync-dingtalk-users";
    public static final String M9_DEPT_SYNC_DINGTALK_USERS_SUCCESS = "同步了钉钉部门用户";

    // ----- M2-task -----
    public static final String M2_TASK_TYPE = "M2-task";
    public static final String M2_TASK_CREATE_SUB_TYPE = "create";
    public static final String M2_TASK_CREATE_SUCCESS = "创建了任务【{{#task.planName}}】";
    public static final String M2_TASK_START_SUB_TYPE = "start";
    public static final String M2_TASK_START_SUCCESS = "启动了任务【{{#task.planName}}】";
    public static final String M2_TASK_COMPLETE_SUB_TYPE = "complete";
    public static final String M2_TASK_COMPLETE_SUCCESS = "完成了任务【{{#task.planName}}】";
    public static final String M2_TASK_SUBMIT_REVIEW_SUB_TYPE = "submit-review";
    public static final String M2_TASK_SUBMIT_REVIEW_SUCCESS = "提交了任务审核【{{#task.id}}】";
    public static final String M2_TASK_EXECUTE_GET_SUB_TYPE = "execute-get";
    public static final String M2_TASK_EXECUTE_GET_SUCCESS = "获取了任务执行数据【{{#task.id}}】";
    public static final String M2_TASK_EXECUTE_SAVE_SUB_TYPE = "execute-save";
    public static final String M2_TASK_EXECUTE_SAVE_SUCCESS = "保存了任务执行数据【{{#task.id}}】";
    public static final String M2_TASK_EXECUTE_UPLOAD_SUB_TYPE = "execute-upload";
    public static final String M2_TASK_EXECUTE_UPLOAD_SUCCESS = "上传了任务执行附件【{{#task.id}}】";
    public static final String M2_TASK_EXECUTE_COMPLETE_SUB_TYPE = "execute-complete";
    public static final String M2_TASK_EXECUTE_COMPLETE_SUCCESS = "完成了任务执行【{{#task.planName}}】";

    // ----- M2-plan -----
    public static final String M2_PLAN_TYPE = "M2-plan";
    public static final String M2_PLAN_CREATE_SUB_TYPE = "create";
    public static final String M2_PLAN_CREATE_SUCCESS = "创建了内容计划【{{#plan.planName}}】";
    public static final String M2_PLAN_UPDATE_SUB_TYPE = "update";
    public static final String M2_PLAN_UPDATE_SUCCESS = "更新了内容计划【{{#plan.planName}}】";
    public static final String M2_PLAN_START_SUB_TYPE = "start";
    public static final String M2_PLAN_START_SUCCESS = "启动了内容计划【{{#plan.planName}}】";
    public static final String M2_PLAN_TERMINATE_SUBMIT_SUB_TYPE = "terminate-submit";
    public static final String M2_PLAN_TERMINATE_SUBMIT_SUCCESS = "提交了终止内容计划【{{#plan.planName}}】";
    public static final String M2_PLAN_TERMINATE_APPROVE_SUB_TYPE = "terminate-approve";
    public static final String M2_PLAN_TERMINATE_APPROVE_SUCCESS = "批准了终止内容计划【{{#plan.planName}}】";
    public static final String M2_PLAN_TERMINATE_REJECT_SUB_TYPE = "terminate-reject";
    public static final String M2_PLAN_TERMINATE_REJECT_SUCCESS = "驳回了终止内容计划【{{#plan.planName}}】";
    public static final String M2_PLAN_DELETE_SUB_TYPE = "delete";
    public static final String M2_PLAN_DELETE_SUCCESS = "删除了内容计划【{{#plan.planName}}】";

}
