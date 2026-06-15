package cn.iocoder.yudao.module.oa.service.system;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.dal.dataobject.system.SysOperationLogDO;
import cn.iocoder.yudao.module.oa.dal.mysql.system.SysOperationLogMapper;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OperationLogRecorder {

    private static final Map<String, String> MODULE_NAME_MAP = Map.ofEntries(
            Map.entry("M1-ip-group", "IP组管理"),
            Map.entry("M1-internal-content", "内部内容管理"),
            Map.entry("M1-ops-anchor", "主播管理"),
            Map.entry("M1-follower-analysis", "粉丝分析"),
            Map.entry("M1-productivity-review", "产能复盘"),
            Map.entry("M2-content", "内容生产"),
            Map.entry("M2-knowledge", "知识库"),
            Map.entry("M2-sop", "SOP管理"),
            Map.entry("M2-task", "任务管理"),
            Map.entry("M3-order-attribution", "订单归因"),
            Map.entry("M3-perf", "绩效管理"),
            Map.entry("M4-account", "账号管理"),
            Map.entry("M4-company", "公司管理"),
            Map.entry("M4-intermediary", "实名中介管理"),
            Map.entry("M4-personal-wechat", "个人微信管理"),
            Map.entry("M4-realname", "实名人管理"),
            Map.entry("M4-simcard", "SIM卡管理"),
            Map.entry("M4-triple-rel", "三件套关系管理"),
            Map.entry("M4-wework-employee", "企微员工管理"),
            Map.entry("M5-account-cost", "账号成本管理"),
            Map.entry("M5-finance-roi", "财务ROI"),
            Map.entry("M6-custom-query", "自定义查询"),
            Map.entry("M6-metric", "指标管理"),
            Map.entry("M7-report", "报表管理"),
            Map.entry("M8-ai-model", "AI模型配置"),
            Map.entry("M8-ai-prompt", "AI提示词配置"),
            Map.entry("M8-aocreate", "AO创作配置"),
            Map.entry("M8-collect", "采集配置"),
            Map.entry("M8-keyword", "关键词配置"),
            Map.entry("M8-threshold", "阈值配置"),
            Map.entry("M9-dept", "部门管理"),
            Map.entry("M9-dict", "字典管理"),
            Map.entry("M9-message", "消息管理"),
            Map.entry("M9-param", "参数管理"),
            Map.entry("M9-role", "角色管理"),
            Map.entry("M9-tenant", "租户管理"),
            Map.entry("M9-user", "用户管理"),
            Map.entry("health", "健康检查")
    );

    private static final Map<String, String> ACTION_NAME_MAP = Map.ofEntries(
            Map.entry("activate-template", "启用模板"),
            Map.entry("add-member", "新增成员"),
            Map.entry("adjust-record", "调整记录"),
            Map.entry("api-config", "配置API"),
            Map.entry("approve-review", "审核通过"),
            Map.entry("bind-accounts", "绑定账号"),
            Map.entry("bind-anchors", "绑定主播"),
            Map.entry("calculate-record", "计算记录"),
            Map.entry("complete", "完成任务"),
            Map.entry("confirm-record", "确认记录"),
            Map.entry("create", "新增"),
            Map.entry("create-node", "新增节点"),
            Map.entry("create-record", "新增记录"),
            Map.entry("create-template", "新增模板"),
            Map.entry("delete", "删除"),
            Map.entry("delete-member", "删除成员"),
            Map.entry("delete-template", "删除模板"),
            Map.entry("expand", "扩容"),
            Map.entry("export", "导出"),
            Map.entry("export-account-alert", "导出账号预警"),
            Map.entry("export-account-status", "导出账号状态"),
            Map.entry("export-cost-allocation", "导出成本分摊"),
            Map.entry("export-live-duration", "导出直播时长"),
            Map.entry("export-result", "导出结果"),
            Map.entry("export-roi", "导出ROI"),
            Map.entry("export-unified-account", "导出统一账号"),
            Map.entry("export-video-output", "导出视频产出"),
            Map.entry("hello", "健康检查"),
            Map.entry("import-external", "导入外部数据"),
            Map.entry("import-review", "审核导入"),
            Map.entry("import-submit", "提交导入"),
            Map.entry("publish", "发布"),
            Map.entry("rebind", "重新绑定"),
            Map.entry("reject-review", "审核驳回"),
            Map.entry("replace", "强制替换"),
            Map.entry("review", "审核"),
            Map.entry("save", "保存"),
            Map.entry("send", "发送"),
            Map.entry("set-default", "设为默认"),
            Map.entry("start", "开始任务"),
            Map.entry("submit-review", "提交审核"),
            Map.entry("transfer-knowledge", "转知识库"),
            Map.entry("sync-dingtalk", "同步钉钉部门"),
            Map.entry("sync-dingtalk-users", "同步钉钉用户"),
            Map.entry("test-connection", "测试连接"),
            Map.entry("toggle-like", "切换点赞"),
            Map.entry("toggle-status", "切换状态"),
            Map.entry("unbind", "解绑"),
            Map.entry("unbind-account", "解绑账号"),
            Map.entry("unbind-anchor", "解绑主播"),
            Map.entry("update", "修改"),
            Map.entry("update-member", "修改成员"),
            Map.entry("update-node", "修改节点"),
            Map.entry("update-status", "修改状态"),
            Map.entry("update-template", "修改模板")
    );

    private final SysOperationLogMapper sysOperationLogMapper;

    public void record(AuditLog auditLog, String methodSignature) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            return;
        }
        String moduleName = toBusinessModuleName(auditLog.module());
        String actionName = toBusinessActionName(auditLog.action(), moduleName);
        SysOperationLogDO row = new SysOperationLogDO();
        row.setTenantId(tenantId);
        row.setUserId(TenantContextHolder.getUserId());
        row.setUsername(TenantContextHolder.getUsername());
        row.setModule(mapModule(auditLog.module()));
        row.setAction(actionName);
        row.setLevel("INFO");
        row.setContent(moduleName + " / " + actionName);
        row.setMethod(methodSignature);
        row.setStatus("SUCCESS");
        row.setCreateTime(LocalDateTime.now());
        sysOperationLogMapper.insert(row);
    }

    private String toBusinessModuleName(String module) {
        if (module == null) {
            return "系统";
        }
        return MODULE_NAME_MAP.getOrDefault(module, module);
    }

    private String toBusinessActionName(String action, String moduleName) {
        if (action == null) {
            return "操作";
        }
        String mappedAction = ACTION_NAME_MAP.get(action);
        if (mappedAction == null) {
            return action;
        }
        if ("create".equals(action) && moduleName.endsWith("管理")) {
            return "新增" + moduleName.substring(0, moduleName.length() - 2);
        }
        if ("update".equals(action) && moduleName.endsWith("管理")) {
            return "修改" + moduleName.substring(0, moduleName.length() - 2);
        }
        if ("delete".equals(action) && moduleName.endsWith("管理")) {
            return "删除" + moduleName.substring(0, moduleName.length() - 2);
        }
        return mappedAction;
    }

    private String mapModule(String module) {
        if (module == null) {
            return "SYSTEM";
        }
        if (module.contains("collect") || module.contains("M10")) {
            return "COLLECT";
        }
        if (module.contains("config") || module.contains("keyword") || module.contains("threshold")
                || module.contains("ai-model") || module.contains("ai-prompt") || module.contains("aocreate")
                || module.contains("M8")) {
            return "CONFIG";
        }
        if (module.contains("analytics") || module.contains("metric") || module.contains("dashboard")
                || module.contains("funnel") || module.contains("custom-query") || module.contains("M6")) {
            return "ANALYTICS";
        }
        if (module.contains("report") || module.contains("M7")) {
            return "REPORT";
        }
        if (module.contains("用户")) {
            return "USER";
        }
        if (module.contains("user")) {
            return "USER";
        }
        if (module.contains("账号") || module.contains("公司") || module.contains("实名") || module.contains("SIM")
                || module.contains("三件套") || module.contains("企微员工")
                || module.contains("account") || module.contains("M4")) {
            return "ACCOUNT";
        }
        if (module.contains("内容") || module.contains("任务") || module.contains("SOP") || module.contains("知识库")
                || module.contains("content") || module.contains("M2")) {
            return "CONTENT";
        }
        if (module.contains("财务") || module.contains("成本") || module.contains("finance") || module.contains("M5")) {
            return "FINANCE";
        }
        return "SYSTEM";
    }
}
