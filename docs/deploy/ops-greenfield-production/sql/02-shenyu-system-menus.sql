-- =============================================================================
-- shenyu-system — Ops menus / dicts / RBAC (Greenfield execution order)
-- Generated: 2026-08-25 by gen-ops-greenfield-sql.py — do not hand-edit
-- Schema SSOT: Beta test shenyu-system @ 110.42.49.224 (OPS-TEST-DB.md): menu.user_type, dict_data.value
-- Target DB: pass on mysql CLI, e.g. mysql -h HOST -u USER -p shenyu-system < sql/02-shenyu-system-menus.sql
-- Order:  01 → 02 → 05 → 06 → 03 → 07  (04 skipped on greenfield)
-- =============================================================================
SET NAMES utf8mb4;


-- =============================================================================
-- ===== 01_baseline_ops_menus.sql =====
-- 6100–6168 Ops baseline menus + super_admin role_menu
-- =============================================================================

BEGIN;

-- S2-A: Ops menus for Football system_menu (ADR-047)
-- SSOT: scripts/integration-config/seed-oa-system-menu.sql
-- Permission prefix: ops:* (ADR-058 P-D) ; M9 user/role/tenant excluded




INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6100, '运营数据', '', 1, 5, 0, '/ops', 'ep:data-analysis', NULL, NULL, 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6100);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6101, '作品监测', '', 1, 1, 6100, 'monitor', 'ep:folder', NULL, NULL, 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6101);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6102, '内容生产', '', 1, 2, 6100, 'production', 'ep:folder', NULL, NULL, 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6102);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6103, '数据分析', '', 1, 3, 6100, 'analysis', 'ep:folder', NULL, NULL, 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6103);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6104, '数据采集', '', 1, 4, 6100, 'collect', 'ep:folder', NULL, NULL, 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6104);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6105, '系统管理(OA)', '', 1, 5, 6100, 'system-oa', 'ep:folder', NULL, NULL, 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6105);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6106, '绩效核算', '', 1, 6, 6100, 'performance', 'ep:folder', NULL, NULL, 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6106);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6107, '财务管理', '', 1, 7, 6100, 'finance', 'ep:folder', NULL, NULL, 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6107);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6108, '账号管理', '', 1, 8, 6100, 'internal', 'ep:folder', NULL, NULL, 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6108);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6109, '运营管理', '', 1, 9, 6100, 'operations', 'ep:folder', NULL, NULL, 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6109);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6110, '配置管理', '', 1, 10, 6100, 'config', 'ep:folder', NULL, NULL, 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6110);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6111, '外部账号分析', 'ops:external-account:list', 2, 1, 6101, 'external-account', 'ep:document', 'ops/account/ExternalAccountAnalysis', 'ExternalAccount', 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6111);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6112, '高粉账号分析', 'ops:high-fans:list', 2, 2, 6101, 'high-fans-account', 'ep:document', 'ops/account/HighFansAccountAnalysis', 'HighFansAccount', 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6112);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6113, '爆款作品分析', 'ops:hot-works:list', 2, 3, 6101, 'hot-works', 'ep:document', 'ops/content/HotWorksAnalysis', 'HotWorks', 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6113);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6114, 'IP主题数据', 'ops:ip-theme:list', 2, 4, 6101, 'ip-theme', 'ep:document', 'ops/content/IPThemeData', 'IPTheme', 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6114);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6115, '低粉账号分析', 'ops:low-fans:list', 2, 5, 6101, 'low-fans-account', 'ep:document', 'ops/account/LowFansAccountAnalysis', 'LowFansAccount', 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6115);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6116, '低分作品分析', 'ops:low-score:list', 2, 6, 6101, 'low-score', 'ep:document', 'ops/content/LowScoreAnalysis', 'LowScore', 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6116);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6117, '内容管理', 'ops:content:list', 2, 1, 6102, 'content', 'ep:document', 'ops/production/content/index', 'Content', 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6117);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6118, '内容审核', 'ops:content:list', 2, 2, 6102, 'content/review', 'ep:document', 'ops/production/content/review', 'ContentReview', 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6118);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6119, '内容知识库', 'ops:knowledge:list', 2, 3, 6102, 'knowledge', 'ep:document', 'ops/production/knowledge/index', 'Knowledge', 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6119);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6120, '公推模板库', 'ops:layout-template:list', 2, 4, 6102, 'layout-template', 'ep:document', 'ops/production/layout-template/index', 'LayoutTemplate', 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6120);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6170, '公推模板创建', 'ops:layout-template:create', 3, 1, 6120, '', '', '', NULL, 0, b'0', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6170);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6171, '公推模板更新', 'ops:layout-template:update', 3, 2, 6120, '', '', '', NULL, 0, b'0', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6171);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6172, '公推模板删除', 'ops:layout-template:delete', 3, 3, 6120, '', '', '', NULL, 0, b'0', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6172);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6173, '公推模板导入', 'ops:layout-template:import', 3, 4, 6120, '', '', '', NULL, 0, b'0', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6173);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6121, '计划管理', 'ops:plan:list', 2, 5, 6102, 'plan', 'ep:document', 'ops/production/plan/index', 'Plan', 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6121);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6122, 'SOP管理', 'ops:sop:list', 2, 6, 6102, 'sop', 'ep:document', 'ops/production/sop/index', 'Sop', 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6122);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6123, 'SOP审核', 'ops:sop:list', 2, 7, 6102, 'sop/review', 'ep:document', 'ops/production/sop/review', 'SopReview', 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6123);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6124, '我的任务', 'ops:task:list', 2, 8, 6102, 'task', 'ep:document', 'ops/production/task/index', 'Task', 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6124);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6175, '全部任务', 'ops:task:list', 2, 9, 6102, 'task/all', 'ep:document', 'ops/production/task/all', 'TaskAll', 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6175);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6125, '自定义查询', 'ops:custom-query:list', 2, 1, 6103, 'custom-query', 'ep:document', 'ops/analysis/CustomQuery', 'CustomQuery', 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6125);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6126, '数据报表', 'ops:report:list', 2, 2, 6103, 'data-report', 'ep:document', 'ops/analysis/ReportCenter', 'DataReport', 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6126);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6127, '总体财务分析', 'ops:financial-analysis:list', 2, 3, 6103, 'financial-analysis', 'ep:document', 'ops/finance/FinancialAnalysis', 'FinancialAnalysis', 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6127);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6128, '漏斗分析', 'ops:funnel-analysis:list', 2, 4, 6103, 'funnel-analysis', 'ep:document', 'ops/analysis/FunnelAnalysis', 'FunnelAnalysis', 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6128);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6129, '指标管理', 'ops:metric:list', 2, 5, 6103, 'metric', 'ep:document', 'ops/analysis/MetricManage', 'Metric', 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6129);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6130, '指标分析', 'ops:metric-analysis:list', 2, 6, 6103, 'metric-analysis', 'ep:document', 'ops/analysis/MetricAnalysis', 'MetricAnalysis', 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6130);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6131, '数据大屏', 'ops:screen:view', 2, 7, 6103, 'screen', 'ep:document', 'ops/screen/DataScreenFullscreen', 'DataScreenFullscreen', 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6131);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6132, '大屏配置', 'ops:screen-config:list', 2, 8, 6103, 'screen-config', 'ep:document', 'ops/screen/ScreenConfig', 'ScreenConfig', 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6132);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6133, '采集日志', 'ops:collect:log:list', 2, 1, 6104, 'log', 'ep:document', 'ops/collect/log', 'CollectLog', 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6133);

-- 6134 私域桥接已移除：Phase 2 OOS（ADR-060 stub）；见 cleanup-m10-quality-bridge-menu.sql
-- 6135 数据质量已移除：Phase 2 OOS（ADR-060 stub）；见 cleanup-m10-quality-bridge-menu.sql
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6136, '采集任务', 'ops:collect:task:list', 2, 2, 6104, 'task', 'ep:document', 'ops/collect/task', 'CollectTask', 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6136);

-- 6137 字典配置已移除：SSOT = Football #/dict（menu 105；system:dict:query @ 1026；见 V149 / OPS-DICT-MERGE-FOOTBALL-PLAN）
-- 6138 登录日志已移除：OPS 不承载登录，SSOT = Football system/login-log（见 V146 / OPS-AUDIT-LOG-MIGRATION-PLAN AL-04）
-- 6139 操作日志已移除：SSOT = Football #/log/operate-log（system:operate-log:query @ menu 1040；见 V147 / AL-11）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6140, '消息管理', 'ops:message:list', 2, 4, 6105, 'system-message', 'ep:document', 'ops/system/MessageManage', 'SystemMessage', 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6140);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6141, '系统参数', 'ops:param:list', 2, 5, 6105, 'system-param', 'ep:document', 'ops/system/ParamManage', 'SystemParam', 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6141);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6142, '订单归因分析', 'ops:order-attribution:list', 2, 1, 6106, 'order-attribution', 'ep:document', 'ops/performance/OrderAttribution', 'OrderAttribution', 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6142);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6143, '考核执行', 'ops:perf:list', 2, 2, 6106, 'perf-execution', 'ep:document', 'ops/performance/PerfExecution', 'PerfExecution', 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6143);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6144, '绩效结果', 'ops:perf:list', 2, 3, 6106, 'perf-result', 'ep:document', 'ops/performance/PerfResult', 'PerfResult', 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6144);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6145, '考核模板', 'ops:perf:list', 2, 4, 6106, 'perf-template', 'ep:document', 'ops/performance/PerfTemplate', 'PerfTemplate', 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6145);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6146, '账号成本管理', 'ops:cost:list', 2, 1, 6107, 'account-cost', 'ep:document', 'ops/finance/AccountCostManage', 'AccountCost', 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6146);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6147, 'ROI分析', 'ops:roi:list', 2, 2, 6107, 'roi-analysis', 'ep:document', 'ops/finance/RoiAnalysis', 'RoiAnalysis', 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6147);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6148, '公司管理', 'ops:company:list', 2, 1, 6108, 'company', 'ep:document', 'ops/internal/CompanyManage', 'Company', 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6148);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6149, '平台账号管理', 'ops:platform-account:list', 2, 2, 6108, 'internal-account', 'ep:document', 'ops/internal/InternalAccountManage', 'InternalAccount', 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6149);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6174, '平台账号查询', 'ops:account:list', 3, 1, 6149, '', '', '', NULL, 0, b'0', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6174);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6150, '个人账号管理', 'ops:personal-account:list', 2, 3, 6108, 'personal-account', 'ep:document', 'ops/internal/PersonalAccountManage', 'PersonalAccount', 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6150);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6151, '手机管理', 'ops:phone:list', 2, 4, 6108, 'phone', 'ep:document', 'ops/internal/PhoneManage', 'Phone', 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6151);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6152, '实名人管理', 'ops:realname:list', 2, 5, 6108, 'realname', 'ep:document', 'ops/internal/RealnameManage', 'Realname', 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6152);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6153, '手机卡管理', 'ops:simcard:list', 2, 6, 6108, 'simcard', 'ep:document', 'ops/internal/SimcardManage', 'Simcard', 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6153);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6154, '账号分析', 'ops:account-analysis:list', 2, 1, 6109, 'account-analysis', 'ep:document', 'ops/operations/AccountAnalysis', 'AccountAnalysis', 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6154);

-- 6155 作者管理已移除：基础 CRUD 迁移 Football author/info（见 V145 / OPS-AUTHOR-MERGE-ANALYSIS）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6156, '人效盘点', 'ops:efficiency:list', 2, 3, 6109, 'efficiency', 'ep:document', 'ops/operations/Efficiency', 'Efficiency', 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6156);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6157, '粉丝分析', 'ops:fans-analysis:list', 2, 4, 6109, 'fans-analysis', 'ep:document', 'ops/operations/FansAnalysis', 'FansAnalysis', 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6157);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6158, '内部作品分析', 'ops:internal-content:list', 2, 5, 6109, 'internal-content', 'ep:document', 'ops/operations/InternalContent', 'InternalContent', 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6158);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6159, 'IP组管理', 'ops:ip-group:list', 2, 6, 6109, 'ip-group', 'ep:document', 'ops/operations/IpGroup', 'IpGroup', 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6159);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6160, 'AI模型', 'ops:config:ai-model:list', 2, 1, 6110, 'config-ai-model', 'ep:document', 'ops/config/AiModelConfig', 'ConfigAiModel', 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6160);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6161, 'AI提示词', 'ops:config:ai-prompt:list', 2, 2, 6110, 'config-ai-prompt', 'ep:document', 'ops/config/AiPromptConfig', 'ConfigAiPrompt', 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6161);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6162, '外部采集配置', 'ops:config:external-collect:list', 2, 3, 6110, 'config-external-collect', 'ep:document', 'ops/config/ExternalCollectConfig', 'ConfigExternalCollect', 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6162);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6163, '外部数据配置', 'ops:config:external-data:list', 2, 4, 6110, 'config-external-data', 'ep:document', 'ops/config/ExternalDataConfig', 'ConfigExternalData', 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6163);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6164, '内部采集配置', 'ops:config:internal-collect:list', 2, 5, 6110, 'config-internal-collect', 'ep:document', 'ops/config/InternalCollectConfig', 'ConfigInternalCollect', 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6164);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6165, '元数据维护', 'ops:metadata:query', 2, 6, 6110, 'config-metadata', 'ep:document', 'ops/config/MetadataManage', 'ConfigMetadata', 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6165);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6166, '订单采集配置', 'ops:config:order-collect:list', 2, 7, 6110, 'config-order-collect', 'ep:document', 'ops/config/OrderCollectConfig', 'ConfigOrderCollect', 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6166);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6167, '阈值规则配置', 'ops:config:threshold:list', 2, 8, 6110, 'config-threshold', 'ep:document', 'ops/config/ThresholdConfig', 'ConfigThreshold', 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6167);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, user_type)
SELECT 6168, '首页仪表盘', 'ops:home:view', 2, 1, 6100, 'dashboard', 'ep:document', 'ops/Dashboard', 'Dashboard', 0, b'1', b'1', b'1', 'integration', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6168);


-- Grant all Ops menus to super admin (role_id=1, tenant_id=1)
-- user_type=2 必填：RoleMenuMapper.selectListByRoleId 按 userType 过滤（非 super_admin 依赖本表）
-- 目录节点 6101–6110 必须授予，否则非超管侧栏树会丢父级、子菜单挂不上
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70000, 1, 6100, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70000);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70065, 1, 6101, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70065);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70066, 1, 6102, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70066);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70067, 1, 6103, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70067);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70068, 1, 6104, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70068);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70069, 1, 6105, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70069);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70070, 1, 6106, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70070);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70071, 1, 6107, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70071);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70072, 1, 6108, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70072);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70073, 1, 6109, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70073);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70074, 1, 6110, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70074);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70001, 1, 6111, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70001);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70002, 1, 6112, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70002);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70003, 1, 6113, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70003);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70004, 1, 6114, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70004);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70005, 1, 6115, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70005);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70006, 1, 6116, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70006);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70007, 1, 6117, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70007);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70008, 1, 6118, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70008);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70009, 1, 6119, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70009);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70010, 1, 6120, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70010);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70059, 1, 6170, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70059);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70060, 1, 6171, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70060);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70061, 1, 6172, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70061);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70062, 1, 6173, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70062);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70063, 1, 6174, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70063);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70011, 1, 6121, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70011);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70012, 1, 6122, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70012);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70013, 1, 6123, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70013);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70014, 1, 6124, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70014);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70064, 1, 6175, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70064);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70015, 1, 6125, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70015);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70016, 1, 6126, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70016);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70017, 1, 6127, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70017);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70018, 1, 6128, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70018);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70019, 1, 6129, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70019);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70020, 1, 6130, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70020);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70021, 1, 6131, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70021);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70022, 1, 6132, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70022);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70023, 1, 6133, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70023);

-- 70024 6134 私域桥接菜单已移除
-- 70025 6135 数据质量菜单已移除
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70026, 1, 6136, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70026);

-- 70027 6137 字典配置菜单已移除（Football 105/1026–1029 由 V149 授予原 6137 角色）
-- 70028 6138 登录日志菜单已移除
-- 70029 6139 操作日志菜单已移除（Football 1040 由 V147 授予原 6139 角色）
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70030, 1, 6140, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70030);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70031, 1, 6141, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70031);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70032, 1, 6142, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70032);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70033, 1, 6143, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70033);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70034, 1, 6144, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70034);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70035, 1, 6145, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70035);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70036, 1, 6146, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70036);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70037, 1, 6147, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70037);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70038, 1, 6148, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70038);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70039, 1, 6149, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70039);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70040, 1, 6150, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70040);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70041, 1, 6151, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70041);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70042, 1, 6152, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70042);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70043, 1, 6153, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70043);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70044, 1, 6154, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70044);

-- 70045 6155 作者管理菜单已移除
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70046, 1, 6156, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70046);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70047, 1, 6157, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70047);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70048, 1, 6158, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70048);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70049, 1, 6159, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70049);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70050, 1, 6160, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70050);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70051, 1, 6161, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70051);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70052, 1, 6162, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70052);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70053, 1, 6163, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70053);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70054, 1, 6164, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70054);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70055, 1, 6165, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70055);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70056, 1, 6166, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70056);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70057, 1, 6167, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70057);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70058, 1, 6168, 'integration', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70058);



COMMIT;

-- =============================================================================
-- ===== 02_menu_supplement.sql =====
-- 6175 all-tasks menu, collect path fixes, remove OOS menus
-- =============================================================================

UPDATE system_menu
SET name = '我的任务',
    updater = 'ops-prod-deploy',
    update_time = NOW()
WHERE id = 6124
  AND deleted = b'0';

INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, user_type
)
SELECT
    6175, '全部任务', 'ops:task:list', 2, 9, 6102, 'task/all', 'ep:document',
    'ops/production/task/all', 'TaskAll', 0, b'1', b'1', b'1', 'ops-prod-deploy', 2
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6175);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type)
SELECT 70064, 1, 6175, 'ops-prod-deploy', 1, 2
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 70064);

UPDATE system_menu SET path = 'log', updater = 'ops-prod-deploy', update_time = NOW()
WHERE id = 6133 AND parent_id = 6104 AND path IN ('collect/log', 'log') AND deleted = b'0';

-- M10 Phase 2 OOS: hide 数据质量 + 私域桥接
DELETE FROM system_role_menu WHERE menu_id IN (6134, 6135)
   OR menu_id IN (SELECT id FROM system_menu WHERE parent_id IN (6134, 6135));
DELETE FROM system_menu WHERE parent_id IN (6134, 6135);
DELETE FROM system_menu WHERE id IN (6134, 6135);

UPDATE system_menu SET path = 'task', updater = 'ops-prod-deploy', update_time = NOW()
WHERE id = 6136 AND parent_id = 6104 AND path IN ('collect/task', 'task') AND deleted = b'0';

-- 移除已迁移 Football 的冗余菜单（字典/日志/作者）
DELETE FROM system_role_menu WHERE menu_id IN (6137, 6138, 6139, 6155);
DELETE FROM system_menu WHERE id IN (6137, 6138, 6139, 6155);

-- =============================================================================
-- ===== 05_work_task_dicts_v183.sql =====
-- Work task 4 dict_type + 11 dict_data
-- =============================================================================

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '营销计划类型', 'dict_marketing_plan_type', 0, 'FR-M2-010 work-task', 'deploy-v183', NOW(), 'deploy-v183', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st
    WHERE st.type = 'dict_marketing_plan_type' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '销售平台', 'dict_sales_platform', 0, 'FR-M2-010 work-task', 'deploy-v183', NOW(), 'deploy-v183', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st
    WHERE st.type = 'dict_sales_platform' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '红黑预测', 'dict_win_prediction', 0, 'FR-M2-010 work-task', 'deploy-v183', NOW(), 'deploy-v183', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st
    WHERE st.type = 'dict_win_prediction' AND st.deleted = b'0'
);

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '工作任务登记状态', 'dict_work_task_sheet_status', 0, 'FR-M2-010 work-task', 'deploy-v183', NOW(), 'deploy-v183', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type st
    WHERE st.type = 'dict_work_task_sheet_status' AND st.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '直播公推', 'LIVE_PUBLIC', 'dict_marketing_plan_type', 0, 'success', '', NULL, 'deploy-v183', NOW(), 'deploy-v183', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd
    WHERE sd.dict_type = 'dict_marketing_plan_type' AND sd.value = 'LIVE_PUBLIC' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '付费销售', 'PAID_SALES', 'dict_marketing_plan_type', 0, 'warning', '', NULL, 'deploy-v183', NOW(), 'deploy-v183', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd
    WHERE sd.dict_type = 'dict_marketing_plan_type' AND sd.value = 'PAID_SALES' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '私域', 'PRIVATE', 'dict_sales_platform', 0, 'primary', '', NULL, 'deploy-v183', NOW(), 'deploy-v183', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd
    WHERE sd.dict_type = 'dict_sales_platform' AND sd.value = 'PRIVATE' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '快手', 'KUAISHOU', 'dict_sales_platform', 0, 'primary', '', NULL, 'deploy-v183', NOW(), 'deploy-v183', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd
    WHERE sd.dict_type = 'dict_sales_platform' AND sd.value = 'KUAISHOU' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '抖音', 'DOUYIN', 'dict_sales_platform', 0, 'primary', '', NULL, 'deploy-v183', NOW(), 'deploy-v183', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd
    WHERE sd.dict_type = 'dict_sales_platform' AND sd.value = 'DOUYIN' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 4, '无', 'NONE', 'dict_sales_platform', 0, 'info', '', NULL, 'deploy-v183', NOW(), 'deploy-v183', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd
    WHERE sd.dict_type = 'dict_sales_platform' AND sd.value = 'NONE' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '未知', 'UNKNOWN', 'dict_win_prediction', 0, 'info', '', NULL, 'deploy-v183', NOW(), 'deploy-v183', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd
    WHERE sd.dict_type = 'dict_win_prediction' AND sd.value = 'UNKNOWN' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '红', 'RED', 'dict_win_prediction', 0, 'danger', '', NULL, 'deploy-v183', NOW(), 'deploy-v183', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd
    WHERE sd.dict_type = 'dict_win_prediction' AND sd.value = 'RED' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '黑', 'BLACK', 'dict_win_prediction', 0, 'default', '', NULL, 'deploy-v183', NOW(), 'deploy-v183', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd
    WHERE sd.dict_type = 'dict_win_prediction' AND sd.value = 'BLACK' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '草稿', 'DRAFT', 'dict_work_task_sheet_status', 0, 'info', '', NULL, 'deploy-v183', NOW(), 'deploy-v183', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd
    WHERE sd.dict_type = 'dict_work_task_sheet_status' AND sd.value = 'DRAFT' AND sd.deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '已确认', 'CONFIRMED', 'dict_work_task_sheet_status', 0, 'success', '', NULL, 'deploy-v183', NOW(), 'deploy-v183', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd
    WHERE sd.dict_type = 'dict_work_task_sheet_status' AND sd.value = 'CONFIRMED' AND sd.deleted = b'0'
);

-- =============================================================================
-- ===== 06_live_drain_v188.sql =====
-- LIVE_DRAIN marketing plan dict
-- =============================================================================

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '直播引流', 'LIVE_DRAIN', 'dict_marketing_plan_type', 0, 'primary', '', NULL, 'deploy-v188', NOW(), 'deploy-v188', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data sd
    WHERE sd.dict_type = 'dict_marketing_plan_type' AND sd.value = 'LIVE_DRAIN' AND sd.deleted = b'0'
);

-- =============================================================================
-- ===== 03_work_task_menus_v183.sql =====
-- 6194–6196 work task menus + role_menu
-- =============================================================================

INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator
)
SELECT
    6194, '工作任务管理', 'ops:work-task:list', 2, 10, 6102, 'work-task', 'ep:calendar',
    'ops/production/work-task/index', 'WorkTask', 0, b'1', b'1', b'1', 'deploy-v183'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6194)
  AND NOT EXISTS (
      SELECT 1 FROM system_menu m
      WHERE m.permission = 'ops:work-task:list' AND m.deleted = b'0'
  );

INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator
)
SELECT
    6195, '工作任务登记', 'ops:work-task:register', 3, 1,
    (SELECT id FROM system_menu WHERE permission = 'ops:work-task:list' AND deleted = b'0' LIMIT 1),
    '', '', '', NULL,
    0, b'1', b'1', b'1', 'deploy-v183'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6195)
  AND NOT EXISTS (
      SELECT 1 FROM system_menu m
      WHERE m.permission = 'ops:work-task:register' AND m.deleted = b'0'
  );

INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator
)
SELECT
    6196, '工作任务管理矩阵', 'ops:work-task:manage', 3, 2,
    (SELECT id FROM system_menu WHERE permission = 'ops:work-task:list' AND deleted = b'0' LIMIT 1),
    '', '', '', NULL,
    0, b'1', b'1', b'1', 'deploy-v183'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 6196)
  AND NOT EXISTS (
      SELECT 1 FROM system_menu m
      WHERE m.permission = 'ops:work-task:manage' AND m.deleted = b'0'
  );

-- Admin role (role_id=1)
INSERT INTO system_role_menu (role_id, menu_id, creator, tenant_id)
SELECT 1, m.id, 'deploy-v183', 1
FROM system_menu m
WHERE m.permission = 'ops:work-task:list' AND m.deleted = b'0'
  AND NOT EXISTS (
      SELECT 1 FROM system_role_menu rm
      WHERE rm.role_id = 1 AND rm.menu_id = m.id AND rm.deleted = b'0'
  )
LIMIT 1;

INSERT INTO system_role_menu (role_id, menu_id, creator, tenant_id)
SELECT 1, m.id, 'deploy-v183', 1
FROM system_menu m
WHERE m.permission = 'ops:work-task:register' AND m.deleted = b'0'
  AND NOT EXISTS (
      SELECT 1 FROM system_role_menu rm
      WHERE rm.role_id = 1 AND rm.menu_id = m.id AND rm.deleted = b'0'
  )
LIMIT 1;

INSERT INTO system_role_menu (role_id, menu_id, creator, tenant_id)
SELECT 1, m.id, 'deploy-v183', 1
FROM system_menu m
WHERE m.permission = 'ops:work-task:manage' AND m.deleted = b'0'
  AND NOT EXISTS (
      SELECT 1 FROM system_role_menu rm
      WHERE rm.role_id = 1 AND rm.menu_id = m.id AND rm.deleted = b'0'
  )
LIMIT 1;

-- IP组长 role (code=ip_group_leader)
INSERT INTO system_role_menu (role_id, menu_id, creator, tenant_id)
SELECT r.id, m.id, 'deploy-v183', 1
FROM system_role r
JOIN system_menu m
  ON m.permission = 'ops:work-task:list' AND m.deleted = b'0'
WHERE r.code = 'ip_group_leader' AND r.tenant_id = 1 AND r.deleted = b'0'
  AND NOT EXISTS (
      SELECT 1 FROM system_role_menu rm
      WHERE rm.role_id = r.id AND rm.menu_id = m.id AND rm.deleted = b'0'
  )
LIMIT 1;

INSERT INTO system_role_menu (role_id, menu_id, creator, tenant_id)
SELECT r.id, m.id, 'deploy-v183', 1
FROM system_role r
JOIN system_menu m
  ON m.permission = 'ops:work-task:register' AND m.deleted = b'0'
WHERE r.code = 'ip_group_leader' AND r.tenant_id = 1 AND r.deleted = b'0'
  AND NOT EXISTS (
      SELECT 1 FROM system_role_menu rm
      WHERE rm.role_id = r.id AND rm.menu_id = m.id AND rm.deleted = b'0'
  )
LIMIT 1;

INSERT INTO system_role_menu (role_id, menu_id, creator, tenant_id)
SELECT r.id, m.id, 'deploy-v183', 1
FROM system_role r
JOIN system_menu m
  ON m.permission = 'ops:work-task:manage' AND m.deleted = b'0'
WHERE r.code = 'ip_group_leader' AND r.tenant_id = 1 AND r.deleted = b'0'
  AND NOT EXISTS (
      SELECT 1 FROM system_role_menu rm
      WHERE rm.role_id = r.id AND rm.menu_id = m.id AND rm.deleted = b'0'
  )
LIMIT 1;

-- =============================================================================
-- ===== 07_ops_six_roles_rbac.sql =====
-- ADR-064 six Ops roles + role_menu
-- =============================================================================

BEGIN;

-- ===== IP组长 (ip_group_leader) id=160 menus=48 =====
INSERT INTO system_role (
    id, name, code, sort, data_scope, data_scope_dept_ids, status, type, remark,
    creator, create_time, updater, update_time, deleted, tenant_id
)
SELECT
    160, 'IP组长', 'ip_group_leader', 20, 5, '', 0, 1,
    'ADR-064：IP组组长；一级内容审核（本组）',
    'adr-064-seed', NOW(), 'adr-064-seed', NOW(), b'0', 1
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_role x WHERE x.code = 'ip_group_leader' AND x.tenant_id = 1 AND x.deleted = b'0'
);

UPDATE system_role
SET name = 'IP组长',
    sort = 20,
    data_scope = 5,
    type = 1,
    remark = 'ADR-064：IP组组长；一级内容审核（本组）',
    updater = 'adr-064-seed',
    update_time = NOW(),
    deleted = b'0'
WHERE code = 'ip_group_leader' AND tenant_id = 1;

SET @role_id_ip_group_leader := (
    SELECT id FROM system_role WHERE code = 'ip_group_leader' AND tenant_id = 1 AND deleted = b'0' LIMIT 1
);

DELETE FROM system_role_menu
WHERE role_id = @role_id_ip_group_leader
  AND menu_id >= 6100 AND menu_id < 7000
  AND menu_id NOT IN (6194, 6195, 6196);  -- preserve work-task (03_work_task_menus_v183)

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71000, @role_id_ip_group_leader, 6100, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ip_group_leader IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71000) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ip_group_leader AND menu_id = 6100);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71001, @role_id_ip_group_leader, 6101, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ip_group_leader IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71001) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ip_group_leader AND menu_id = 6101);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71002, @role_id_ip_group_leader, 6102, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ip_group_leader IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71002) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ip_group_leader AND menu_id = 6102);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71003, @role_id_ip_group_leader, 6103, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ip_group_leader IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71003) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ip_group_leader AND menu_id = 6103);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71004, @role_id_ip_group_leader, 6106, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ip_group_leader IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71004) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ip_group_leader AND menu_id = 6106);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71005, @role_id_ip_group_leader, 6107, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ip_group_leader IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71005) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ip_group_leader AND menu_id = 6107);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71006, @role_id_ip_group_leader, 6108, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ip_group_leader IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71006) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ip_group_leader AND menu_id = 6108);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71007, @role_id_ip_group_leader, 6109, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ip_group_leader IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71007) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ip_group_leader AND menu_id = 6109);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71008, @role_id_ip_group_leader, 6112, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ip_group_leader IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71008) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ip_group_leader AND menu_id = 6112);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71009, @role_id_ip_group_leader, 6113, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ip_group_leader IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71009) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ip_group_leader AND menu_id = 6113);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71010, @role_id_ip_group_leader, 6114, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ip_group_leader IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71010) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ip_group_leader AND menu_id = 6114);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71011, @role_id_ip_group_leader, 6115, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ip_group_leader IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71011) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ip_group_leader AND menu_id = 6115);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71012, @role_id_ip_group_leader, 6116, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ip_group_leader IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71012) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ip_group_leader AND menu_id = 6116);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71013, @role_id_ip_group_leader, 6117, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ip_group_leader IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71013) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ip_group_leader AND menu_id = 6117);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71014, @role_id_ip_group_leader, 6118, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ip_group_leader IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71014) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ip_group_leader AND menu_id = 6118);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71015, @role_id_ip_group_leader, 6119, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ip_group_leader IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71015) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ip_group_leader AND menu_id = 6119);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71016, @role_id_ip_group_leader, 6120, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ip_group_leader IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71016) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ip_group_leader AND menu_id = 6120);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71017, @role_id_ip_group_leader, 6121, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ip_group_leader IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71017) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ip_group_leader AND menu_id = 6121);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71018, @role_id_ip_group_leader, 6122, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ip_group_leader IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71018) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ip_group_leader AND menu_id = 6122);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71019, @role_id_ip_group_leader, 6123, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ip_group_leader IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71019) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ip_group_leader AND menu_id = 6123);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71020, @role_id_ip_group_leader, 6124, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ip_group_leader IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71020) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ip_group_leader AND menu_id = 6124);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71021, @role_id_ip_group_leader, 6126, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ip_group_leader IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71021) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ip_group_leader AND menu_id = 6126);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71022, @role_id_ip_group_leader, 6128, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ip_group_leader IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71022) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ip_group_leader AND menu_id = 6128);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71023, @role_id_ip_group_leader, 6130, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ip_group_leader IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71023) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ip_group_leader AND menu_id = 6130);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71024, @role_id_ip_group_leader, 6142, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ip_group_leader IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71024) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ip_group_leader AND menu_id = 6142);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71025, @role_id_ip_group_leader, 6143, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ip_group_leader IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71025) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ip_group_leader AND menu_id = 6143);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71026, @role_id_ip_group_leader, 6144, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ip_group_leader IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71026) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ip_group_leader AND menu_id = 6144);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71027, @role_id_ip_group_leader, 6145, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ip_group_leader IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71027) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ip_group_leader AND menu_id = 6145);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71028, @role_id_ip_group_leader, 6146, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ip_group_leader IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71028) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ip_group_leader AND menu_id = 6146);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71029, @role_id_ip_group_leader, 6147, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ip_group_leader IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71029) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ip_group_leader AND menu_id = 6147);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71030, @role_id_ip_group_leader, 6148, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ip_group_leader IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71030) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ip_group_leader AND menu_id = 6148);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71031, @role_id_ip_group_leader, 6149, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ip_group_leader IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71031) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ip_group_leader AND menu_id = 6149);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71032, @role_id_ip_group_leader, 6150, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ip_group_leader IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71032) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ip_group_leader AND menu_id = 6150);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71033, @role_id_ip_group_leader, 6151, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ip_group_leader IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71033) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ip_group_leader AND menu_id = 6151);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71034, @role_id_ip_group_leader, 6152, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ip_group_leader IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71034) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ip_group_leader AND menu_id = 6152);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71035, @role_id_ip_group_leader, 6153, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ip_group_leader IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71035) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ip_group_leader AND menu_id = 6153);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71036, @role_id_ip_group_leader, 6154, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ip_group_leader IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71036) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ip_group_leader AND menu_id = 6154);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71037, @role_id_ip_group_leader, 6156, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ip_group_leader IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71037) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ip_group_leader AND menu_id = 6156);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71038, @role_id_ip_group_leader, 6157, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ip_group_leader IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71038) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ip_group_leader AND menu_id = 6157);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71039, @role_id_ip_group_leader, 6158, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ip_group_leader IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71039) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ip_group_leader AND menu_id = 6158);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71040, @role_id_ip_group_leader, 6159, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ip_group_leader IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71040) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ip_group_leader AND menu_id = 6159);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71041, @role_id_ip_group_leader, 6168, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ip_group_leader IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71041) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ip_group_leader AND menu_id = 6168);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71042, @role_id_ip_group_leader, 6170, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ip_group_leader IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71042) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ip_group_leader AND menu_id = 6170);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71043, @role_id_ip_group_leader, 6171, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ip_group_leader IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71043) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ip_group_leader AND menu_id = 6171);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71044, @role_id_ip_group_leader, 6172, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ip_group_leader IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71044) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ip_group_leader AND menu_id = 6172);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71045, @role_id_ip_group_leader, 6173, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ip_group_leader IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71045) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ip_group_leader AND menu_id = 6173);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71046, @role_id_ip_group_leader, 6174, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ip_group_leader IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71046) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ip_group_leader AND menu_id = 6174);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71047, @role_id_ip_group_leader, 6175, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ip_group_leader IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71047) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ip_group_leader AND menu_id = 6175);

-- ===== 运营主管 (ops_manager) id=161 menus=71 =====
INSERT INTO system_role (
    id, name, code, sort, data_scope, data_scope_dept_ids, status, type, remark,
    creator, create_time, updater, update_time, deleted, tenant_id
)
SELECT
    161, '运营主管', 'ops_manager', 21, 1, '', 0, 2,
    'ADR-064：运营主管；二级内容审核；租户 ALL',
    'adr-064-seed', NOW(), 'adr-064-seed', NOW(), b'0', 1
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_role x WHERE x.code = 'ops_manager' AND x.tenant_id = 1 AND x.deleted = b'0'
);

UPDATE system_role
SET name = '运营主管',
    sort = 21,
    data_scope = 1,
    type = 2,
    remark = 'ADR-064：运营主管；二级内容审核；租户 ALL',
    updater = 'adr-064-seed',
    update_time = NOW(),
    deleted = b'0'
WHERE code = 'ops_manager' AND tenant_id = 1;

SET @role_id_ops_manager := (
    SELECT id FROM system_role WHERE code = 'ops_manager' AND tenant_id = 1 AND deleted = b'0' LIMIT 1
);

DELETE FROM system_role_menu
WHERE role_id = @role_id_ops_manager
  AND menu_id >= 6100 AND menu_id < 7000
  AND menu_id NOT IN (6194, 6195, 6196);  -- preserve work-task (03_work_task_menus_v183)

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71200, @role_id_ops_manager, 6100, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71200) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6100);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71201, @role_id_ops_manager, 6101, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71201) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6101);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71202, @role_id_ops_manager, 6102, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71202) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6102);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71203, @role_id_ops_manager, 6103, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71203) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6103);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71204, @role_id_ops_manager, 6104, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71204) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6104);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71205, @role_id_ops_manager, 6105, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71205) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6105);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71206, @role_id_ops_manager, 6106, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71206) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6106);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71207, @role_id_ops_manager, 6107, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71207) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6107);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71208, @role_id_ops_manager, 6108, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71208) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6108);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71209, @role_id_ops_manager, 6109, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71209) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6109);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71210, @role_id_ops_manager, 6110, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71210) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6110);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71211, @role_id_ops_manager, 6111, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71211) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6111);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71212, @role_id_ops_manager, 6112, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71212) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6112);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71213, @role_id_ops_manager, 6113, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71213) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6113);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71214, @role_id_ops_manager, 6114, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71214) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6114);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71215, @role_id_ops_manager, 6115, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71215) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6115);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71216, @role_id_ops_manager, 6116, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71216) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6116);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71217, @role_id_ops_manager, 6117, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71217) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6117);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71218, @role_id_ops_manager, 6118, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71218) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6118);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71219, @role_id_ops_manager, 6119, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71219) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6119);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71220, @role_id_ops_manager, 6120, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71220) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6120);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71221, @role_id_ops_manager, 6121, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71221) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6121);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71222, @role_id_ops_manager, 6122, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71222) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6122);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71223, @role_id_ops_manager, 6123, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71223) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6123);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71224, @role_id_ops_manager, 6124, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71224) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6124);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71225, @role_id_ops_manager, 6125, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71225) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6125);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71226, @role_id_ops_manager, 6126, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71226) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6126);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71227, @role_id_ops_manager, 6127, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71227) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6127);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71228, @role_id_ops_manager, 6128, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71228) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6128);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71229, @role_id_ops_manager, 6129, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71229) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6129);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71230, @role_id_ops_manager, 6130, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71230) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6130);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71231, @role_id_ops_manager, 6131, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71231) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6131);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71232, @role_id_ops_manager, 6132, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71232) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6132);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71233, @role_id_ops_manager, 6133, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71233) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6133);
-- 71234/71235 6134 私域桥接 / 6135 数据质量 已移除（Phase 2 OOS）
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71236, @role_id_ops_manager, 6136, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71236) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6136);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71237, @role_id_ops_manager, 6140, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71237) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6140);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71238, @role_id_ops_manager, 6141, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71238) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6141);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71239, @role_id_ops_manager, 6142, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71239) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6142);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71240, @role_id_ops_manager, 6143, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71240) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6143);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71241, @role_id_ops_manager, 6144, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71241) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6144);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71242, @role_id_ops_manager, 6145, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71242) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6145);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71243, @role_id_ops_manager, 6146, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71243) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6146);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71244, @role_id_ops_manager, 6147, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71244) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6147);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71245, @role_id_ops_manager, 6148, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71245) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6148);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71246, @role_id_ops_manager, 6149, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71246) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6149);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71247, @role_id_ops_manager, 6150, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71247) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6150);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71248, @role_id_ops_manager, 6151, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71248) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6151);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71249, @role_id_ops_manager, 6152, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71249) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6152);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71250, @role_id_ops_manager, 6153, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71250) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6153);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71251, @role_id_ops_manager, 6154, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71251) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6154);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71252, @role_id_ops_manager, 6156, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71252) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6156);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71253, @role_id_ops_manager, 6157, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71253) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6157);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71254, @role_id_ops_manager, 6158, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71254) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6158);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71255, @role_id_ops_manager, 6159, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71255) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6159);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71256, @role_id_ops_manager, 6160, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71256) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6160);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71257, @role_id_ops_manager, 6161, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71257) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6161);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71258, @role_id_ops_manager, 6162, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71258) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6162);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71259, @role_id_ops_manager, 6163, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71259) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6163);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71260, @role_id_ops_manager, 6164, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71260) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6164);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71261, @role_id_ops_manager, 6165, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71261) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6165);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71262, @role_id_ops_manager, 6166, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71262) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6166);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71263, @role_id_ops_manager, 6167, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71263) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6167);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71264, @role_id_ops_manager, 6168, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71264) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6168);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71265, @role_id_ops_manager, 6170, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71265) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6170);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71266, @role_id_ops_manager, 6171, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71266) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6171);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71267, @role_id_ops_manager, 6172, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71267) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6172);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71268, @role_id_ops_manager, 6173, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71268) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6173);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71269, @role_id_ops_manager, 6174, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71269) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6174);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71270, @role_id_ops_manager, 6175, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_manager IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71270) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_manager AND menu_id = 6175);

-- ===== 财务人员 (finance) id=162 menus=34 =====
INSERT INTO system_role (
    id, name, code, sort, data_scope, data_scope_dept_ids, status, type, remark,
    creator, create_time, updater, update_time, deleted, tenant_id
)
SELECT
    162, '财务人员', 'finance', 22, 1, '', 0, 2,
    'ADR-064：财务域；成本/ROI/绩效结果',
    'adr-064-seed', NOW(), 'adr-064-seed', NOW(), b'0', 1
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_role x WHERE x.code = 'finance' AND x.tenant_id = 1 AND x.deleted = b'0'
);

UPDATE system_role
SET name = '财务人员',
    sort = 22,
    data_scope = 1,
    type = 2,
    remark = 'ADR-064：财务域；成本/ROI/绩效结果',
    updater = 'adr-064-seed',
    update_time = NOW(),
    deleted = b'0'
WHERE code = 'finance' AND tenant_id = 1;

SET @role_id_finance := (
    SELECT id FROM system_role WHERE code = 'finance' AND tenant_id = 1 AND deleted = b'0' LIMIT 1
);

DELETE FROM system_role_menu
WHERE role_id = @role_id_finance
  AND menu_id >= 6100 AND menu_id < 7000
  AND menu_id NOT IN (6194, 6195, 6196);  -- preserve work-task (03_work_task_menus_v183)

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71400, @role_id_finance, 6100, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_finance IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71400) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_finance AND menu_id = 6100);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71401, @role_id_finance, 6101, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_finance IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71401) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_finance AND menu_id = 6101);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71402, @role_id_finance, 6102, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_finance IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71402) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_finance AND menu_id = 6102);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71403, @role_id_finance, 6103, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_finance IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71403) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_finance AND menu_id = 6103);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71404, @role_id_finance, 6106, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_finance IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71404) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_finance AND menu_id = 6106);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71405, @role_id_finance, 6107, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_finance IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71405) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_finance AND menu_id = 6107);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71406, @role_id_finance, 6108, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_finance IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71406) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_finance AND menu_id = 6108);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71407, @role_id_finance, 6109, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_finance IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71407) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_finance AND menu_id = 6109);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71408, @role_id_finance, 6111, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_finance IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71408) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_finance AND menu_id = 6111);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71409, @role_id_finance, 6112, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_finance IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71409) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_finance AND menu_id = 6112);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71410, @role_id_finance, 6113, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_finance IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71410) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_finance AND menu_id = 6113);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71411, @role_id_finance, 6114, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_finance IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71411) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_finance AND menu_id = 6114);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71412, @role_id_finance, 6115, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_finance IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71412) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_finance AND menu_id = 6115);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71413, @role_id_finance, 6116, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_finance IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71413) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_finance AND menu_id = 6116);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71414, @role_id_finance, 6117, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_finance IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71414) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_finance AND menu_id = 6117);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71415, @role_id_finance, 6126, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_finance IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71415) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_finance AND menu_id = 6126);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71416, @role_id_finance, 6127, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_finance IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71416) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_finance AND menu_id = 6127);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71417, @role_id_finance, 6142, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_finance IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71417) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_finance AND menu_id = 6142);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71418, @role_id_finance, 6143, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_finance IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71418) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_finance AND menu_id = 6143);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71419, @role_id_finance, 6144, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_finance IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71419) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_finance AND menu_id = 6144);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71420, @role_id_finance, 6146, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_finance IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71420) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_finance AND menu_id = 6146);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71421, @role_id_finance, 6147, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_finance IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71421) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_finance AND menu_id = 6147);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71422, @role_id_finance, 6148, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_finance IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71422) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_finance AND menu_id = 6148);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71423, @role_id_finance, 6149, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_finance IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71423) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_finance AND menu_id = 6149);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71424, @role_id_finance, 6150, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_finance IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71424) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_finance AND menu_id = 6150);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71425, @role_id_finance, 6151, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_finance IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71425) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_finance AND menu_id = 6151);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71426, @role_id_finance, 6152, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_finance IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71426) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_finance AND menu_id = 6152);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71427, @role_id_finance, 6153, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_finance IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71427) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_finance AND menu_id = 6153);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71428, @role_id_finance, 6154, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_finance IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71428) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_finance AND menu_id = 6154);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71429, @role_id_finance, 6156, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_finance IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71429) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_finance AND menu_id = 6156);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71430, @role_id_finance, 6157, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_finance IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71430) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_finance AND menu_id = 6157);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71431, @role_id_finance, 6158, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_finance IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71431) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_finance AND menu_id = 6158);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71432, @role_id_finance, 6168, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_finance IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71432) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_finance AND menu_id = 6168);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71433, @role_id_finance, 6174, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_finance IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71433) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_finance AND menu_id = 6174);

-- ===== 内容编辑 (content_editor) id=163 menus=29 =====
INSERT INTO system_role (
    id, name, code, sort, data_scope, data_scope_dept_ids, status, type, remark,
    creator, create_time, updater, update_time, deleted, tenant_id
)
SELECT
    163, '内容编辑', 'content_editor', 23, 5, '', 0, 2,
    'ADR-064：内容编辑；SELF+本组只读；不审（无6118）',
    'adr-064-seed', NOW(), 'adr-064-seed', NOW(), b'0', 1
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_role x WHERE x.code = 'content_editor' AND x.tenant_id = 1 AND x.deleted = b'0'
);

UPDATE system_role
SET name = '内容编辑',
    sort = 23,
    data_scope = 5,
    type = 2,
    remark = 'ADR-064：内容编辑；SELF+本组只读；不审（无6118）',
    updater = 'adr-064-seed',
    update_time = NOW(),
    deleted = b'0'
WHERE code = 'content_editor' AND tenant_id = 1;

SET @role_id_content_editor := (
    SELECT id FROM system_role WHERE code = 'content_editor' AND tenant_id = 1 AND deleted = b'0' LIMIT 1
);

DELETE FROM system_role_menu
WHERE role_id = @role_id_content_editor
  AND menu_id >= 6100 AND menu_id < 7000
  AND menu_id NOT IN (6194, 6195, 6196);  -- preserve work-task (03_work_task_menus_v183)

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71600, @role_id_content_editor, 6100, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_content_editor IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71600) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_content_editor AND menu_id = 6100);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71601, @role_id_content_editor, 6101, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_content_editor IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71601) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_content_editor AND menu_id = 6101);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71602, @role_id_content_editor, 6102, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_content_editor IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71602) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_content_editor AND menu_id = 6102);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71603, @role_id_content_editor, 6103, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_content_editor IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71603) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_content_editor AND menu_id = 6103);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71604, @role_id_content_editor, 6108, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_content_editor IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71604) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_content_editor AND menu_id = 6108);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71605, @role_id_content_editor, 6109, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_content_editor IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71605) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_content_editor AND menu_id = 6109);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71606, @role_id_content_editor, 6112, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_content_editor IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71606) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_content_editor AND menu_id = 6112);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71607, @role_id_content_editor, 6113, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_content_editor IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71607) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_content_editor AND menu_id = 6113);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71608, @role_id_content_editor, 6114, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_content_editor IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71608) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_content_editor AND menu_id = 6114);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71609, @role_id_content_editor, 6115, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_content_editor IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71609) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_content_editor AND menu_id = 6115);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71610, @role_id_content_editor, 6116, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_content_editor IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71610) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_content_editor AND menu_id = 6116);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71611, @role_id_content_editor, 6117, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_content_editor IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71611) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_content_editor AND menu_id = 6117);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71612, @role_id_content_editor, 6119, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_content_editor IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71612) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_content_editor AND menu_id = 6119);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71613, @role_id_content_editor, 6120, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_content_editor IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71613) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_content_editor AND menu_id = 6120);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71614, @role_id_content_editor, 6121, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_content_editor IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71614) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_content_editor AND menu_id = 6121);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71615, @role_id_content_editor, 6124, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_content_editor IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71615) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_content_editor AND menu_id = 6124);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71616, @role_id_content_editor, 6125, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_content_editor IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71616) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_content_editor AND menu_id = 6125);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71617, @role_id_content_editor, 6128, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_content_editor IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71617) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_content_editor AND menu_id = 6128);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71618, @role_id_content_editor, 6148, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_content_editor IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71618) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_content_editor AND menu_id = 6148);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71619, @role_id_content_editor, 6149, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_content_editor IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71619) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_content_editor AND menu_id = 6149);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71620, @role_id_content_editor, 6150, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_content_editor IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71620) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_content_editor AND menu_id = 6150);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71621, @role_id_content_editor, 6151, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_content_editor IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71621) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_content_editor AND menu_id = 6151);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71622, @role_id_content_editor, 6152, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_content_editor IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71622) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_content_editor AND menu_id = 6152);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71623, @role_id_content_editor, 6153, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_content_editor IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71623) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_content_editor AND menu_id = 6153);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71624, @role_id_content_editor, 6154, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_content_editor IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71624) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_content_editor AND menu_id = 6154);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71625, @role_id_content_editor, 6157, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_content_editor IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71625) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_content_editor AND menu_id = 6157);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71626, @role_id_content_editor, 6158, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_content_editor IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71626) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_content_editor AND menu_id = 6158);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71627, @role_id_content_editor, 6168, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_content_editor IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71627) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_content_editor AND menu_id = 6168);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71628, @role_id_content_editor, 6174, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_content_editor IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71628) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_content_editor AND menu_id = 6174);

-- ===== 运营 (ops_operator) id=164 menus=34 =====
INSERT INTO system_role (
    id, name, code, sort, data_scope, data_scope_dept_ids, status, type, remark,
    creator, create_time, updater, update_time, deleted, tenant_id
)
SELECT
    164, '运营', 'ops_operator', 24, 5, '', 0, 2,
    'ADR-064：运营（含主播/快手）；IP_GROUP+SELF；无审核/无全部任务',
    'adr-064-seed', NOW(), 'adr-064-seed', NOW(), b'0', 1
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_role x WHERE x.code = 'ops_operator' AND x.tenant_id = 1 AND x.deleted = b'0'
);

UPDATE system_role
SET name = '运营',
    sort = 24,
    data_scope = 5,
    type = 2,
    remark = 'ADR-064：运营（含主播/快手）；IP_GROUP+SELF；无审核/无全部任务',
    updater = 'adr-064-seed',
    update_time = NOW(),
    deleted = b'0'
WHERE code = 'ops_operator' AND tenant_id = 1;

SET @role_id_ops_operator := (
    SELECT id FROM system_role WHERE code = 'ops_operator' AND tenant_id = 1 AND deleted = b'0' LIMIT 1
);

DELETE FROM system_role_menu
WHERE role_id = @role_id_ops_operator
  AND menu_id >= 6100 AND menu_id < 7000
  AND menu_id NOT IN (6194, 6195, 6196);  -- preserve work-task (03_work_task_menus_v183)

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71800, @role_id_ops_operator, 6100, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_operator IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71800) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_operator AND menu_id = 6100);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71801, @role_id_ops_operator, 6101, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_operator IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71801) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_operator AND menu_id = 6101);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71802, @role_id_ops_operator, 6102, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_operator IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71802) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_operator AND menu_id = 6102);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71803, @role_id_ops_operator, 6106, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_operator IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71803) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_operator AND menu_id = 6106);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71804, @role_id_ops_operator, 6107, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_operator IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71804) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_operator AND menu_id = 6107);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71805, @role_id_ops_operator, 6108, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_operator IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71805) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_operator AND menu_id = 6108);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71806, @role_id_ops_operator, 6109, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_operator IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71806) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_operator AND menu_id = 6109);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71807, @role_id_ops_operator, 6112, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_operator IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71807) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_operator AND menu_id = 6112);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71808, @role_id_ops_operator, 6113, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_operator IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71808) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_operator AND menu_id = 6113);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71809, @role_id_ops_operator, 6114, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_operator IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71809) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_operator AND menu_id = 6114);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71810, @role_id_ops_operator, 6115, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_operator IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71810) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_operator AND menu_id = 6115);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71811, @role_id_ops_operator, 6116, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_operator IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71811) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_operator AND menu_id = 6116);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71812, @role_id_ops_operator, 6117, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_operator IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71812) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_operator AND menu_id = 6117);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71813, @role_id_ops_operator, 6119, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_operator IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71813) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_operator AND menu_id = 6119);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71814, @role_id_ops_operator, 6120, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_operator IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71814) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_operator AND menu_id = 6120);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71815, @role_id_ops_operator, 6121, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_operator IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71815) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_operator AND menu_id = 6121);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71816, @role_id_ops_operator, 6122, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_operator IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71816) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_operator AND menu_id = 6122);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71817, @role_id_ops_operator, 6124, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_operator IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71817) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_operator AND menu_id = 6124);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71818, @role_id_ops_operator, 6143, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_operator IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71818) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_operator AND menu_id = 6143);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71819, @role_id_ops_operator, 6144, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_operator IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71819) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_operator AND menu_id = 6144);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71820, @role_id_ops_operator, 6146, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_operator IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71820) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_operator AND menu_id = 6146);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71821, @role_id_ops_operator, 6147, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_operator IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71821) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_operator AND menu_id = 6147);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71822, @role_id_ops_operator, 6148, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_operator IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71822) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_operator AND menu_id = 6148);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71823, @role_id_ops_operator, 6149, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_operator IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71823) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_operator AND menu_id = 6149);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71824, @role_id_ops_operator, 6150, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_operator IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71824) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_operator AND menu_id = 6150);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71825, @role_id_ops_operator, 6151, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_operator IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71825) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_operator AND menu_id = 6151);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71826, @role_id_ops_operator, 6152, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_operator IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71826) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_operator AND menu_id = 6152);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71827, @role_id_ops_operator, 6153, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_operator IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71827) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_operator AND menu_id = 6153);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71828, @role_id_ops_operator, 6154, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_operator IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71828) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_operator AND menu_id = 6154);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71829, @role_id_ops_operator, 6156, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_operator IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71829) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_operator AND menu_id = 6156);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71830, @role_id_ops_operator, 6157, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_operator IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71830) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_operator AND menu_id = 6157);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71831, @role_id_ops_operator, 6158, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_operator IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71831) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_operator AND menu_id = 6158);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71832, @role_id_ops_operator, 6168, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_operator IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71832) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_operator AND menu_id = 6168);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 71833, @role_id_ops_operator, 6174, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_ops_operator IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 71833) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_ops_operator AND menu_id = 6174);

-- ===== 数据分析 (data_analyst) id=165 menus=54 =====
INSERT INTO system_role (
    id, name, code, sort, data_scope, data_scope_dept_ids, status, type, remark,
    creator, create_time, updater, update_time, deleted, tenant_id
)
SELECT
    165, '数据分析', 'data_analyst', 25, 1, '', 0, 2,
    'ADR-064：分析域 ALL；监测/报表 RWD；采集 R；无内容审核',
    'adr-064-seed', NOW(), 'adr-064-seed', NOW(), b'0', 1
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_role x WHERE x.code = 'data_analyst' AND x.tenant_id = 1 AND x.deleted = b'0'
);

UPDATE system_role
SET name = '数据分析',
    sort = 25,
    data_scope = 1,
    type = 2,
    remark = 'ADR-064：分析域 ALL；监测/报表 RWD；采集 R；无内容审核',
    updater = 'adr-064-seed',
    update_time = NOW(),
    deleted = b'0'
WHERE code = 'data_analyst' AND tenant_id = 1;

SET @role_id_data_analyst := (
    SELECT id FROM system_role WHERE code = 'data_analyst' AND tenant_id = 1 AND deleted = b'0' LIMIT 1
);

DELETE FROM system_role_menu
WHERE role_id = @role_id_data_analyst
  AND menu_id >= 6100 AND menu_id < 7000
  AND menu_id NOT IN (6194, 6195, 6196);  -- preserve work-task (03_work_task_menus_v183)

INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 72000, @role_id_data_analyst, 6100, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_data_analyst IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 72000) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_data_analyst AND menu_id = 6100);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 72001, @role_id_data_analyst, 6101, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_data_analyst IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 72001) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_data_analyst AND menu_id = 6101);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 72002, @role_id_data_analyst, 6102, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_data_analyst IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 72002) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_data_analyst AND menu_id = 6102);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 72003, @role_id_data_analyst, 6103, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_data_analyst IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 72003) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_data_analyst AND menu_id = 6103);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 72004, @role_id_data_analyst, 6104, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_data_analyst IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 72004) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_data_analyst AND menu_id = 6104);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 72005, @role_id_data_analyst, 6106, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_data_analyst IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 72005) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_data_analyst AND menu_id = 6106);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 72006, @role_id_data_analyst, 6107, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_data_analyst IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 72006) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_data_analyst AND menu_id = 6107);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 72007, @role_id_data_analyst, 6108, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_data_analyst IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 72007) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_data_analyst AND menu_id = 6108);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 72008, @role_id_data_analyst, 6109, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_data_analyst IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 72008) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_data_analyst AND menu_id = 6109);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 72009, @role_id_data_analyst, 6110, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_data_analyst IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 72009) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_data_analyst AND menu_id = 6110);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 72010, @role_id_data_analyst, 6111, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_data_analyst IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 72010) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_data_analyst AND menu_id = 6111);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 72011, @role_id_data_analyst, 6112, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_data_analyst IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 72011) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_data_analyst AND menu_id = 6112);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 72012, @role_id_data_analyst, 6113, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_data_analyst IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 72012) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_data_analyst AND menu_id = 6113);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 72013, @role_id_data_analyst, 6114, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_data_analyst IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 72013) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_data_analyst AND menu_id = 6114);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 72014, @role_id_data_analyst, 6115, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_data_analyst IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 72014) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_data_analyst AND menu_id = 6115);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 72015, @role_id_data_analyst, 6116, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_data_analyst IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 72015) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_data_analyst AND menu_id = 6116);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 72016, @role_id_data_analyst, 6117, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_data_analyst IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 72016) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_data_analyst AND menu_id = 6117);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 72017, @role_id_data_analyst, 6119, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_data_analyst IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 72017) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_data_analyst AND menu_id = 6119);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 72018, @role_id_data_analyst, 6120, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_data_analyst IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 72018) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_data_analyst AND menu_id = 6120);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 72019, @role_id_data_analyst, 6121, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_data_analyst IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 72019) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_data_analyst AND menu_id = 6121);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 72020, @role_id_data_analyst, 6122, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_data_analyst IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 72020) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_data_analyst AND menu_id = 6122);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 72021, @role_id_data_analyst, 6124, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_data_analyst IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 72021) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_data_analyst AND menu_id = 6124);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 72022, @role_id_data_analyst, 6125, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_data_analyst IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 72022) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_data_analyst AND menu_id = 6125);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 72023, @role_id_data_analyst, 6126, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_data_analyst IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 72023) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_data_analyst AND menu_id = 6126);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 72024, @role_id_data_analyst, 6127, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_data_analyst IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 72024) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_data_analyst AND menu_id = 6127);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 72025, @role_id_data_analyst, 6128, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_data_analyst IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 72025) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_data_analyst AND menu_id = 6128);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 72026, @role_id_data_analyst, 6129, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_data_analyst IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 72026) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_data_analyst AND menu_id = 6129);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 72027, @role_id_data_analyst, 6130, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_data_analyst IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 72027) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_data_analyst AND menu_id = 6130);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 72028, @role_id_data_analyst, 6131, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_data_analyst IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 72028) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_data_analyst AND menu_id = 6131);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 72029, @role_id_data_analyst, 6132, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_data_analyst IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 72029) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_data_analyst AND menu_id = 6132);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 72030, @role_id_data_analyst, 6133, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_data_analyst IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 72030) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_data_analyst AND menu_id = 6133);
-- 72031/72032 6134 私域桥接 / 6135 数据质量 已移除（Phase 2 OOS）
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 72033, @role_id_data_analyst, 6136, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_data_analyst IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 72033) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_data_analyst AND menu_id = 6136);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 72034, @role_id_data_analyst, 6142, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_data_analyst IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 72034) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_data_analyst AND menu_id = 6142);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 72035, @role_id_data_analyst, 6143, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_data_analyst IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 72035) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_data_analyst AND menu_id = 6143);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 72036, @role_id_data_analyst, 6144, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_data_analyst IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 72036) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_data_analyst AND menu_id = 6144);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 72037, @role_id_data_analyst, 6145, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_data_analyst IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 72037) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_data_analyst AND menu_id = 6145);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 72038, @role_id_data_analyst, 6146, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_data_analyst IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 72038) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_data_analyst AND menu_id = 6146);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 72039, @role_id_data_analyst, 6147, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_data_analyst IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 72039) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_data_analyst AND menu_id = 6147);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 72040, @role_id_data_analyst, 6148, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_data_analyst IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 72040) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_data_analyst AND menu_id = 6148);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 72041, @role_id_data_analyst, 6149, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_data_analyst IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 72041) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_data_analyst AND menu_id = 6149);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 72042, @role_id_data_analyst, 6150, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_data_analyst IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 72042) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_data_analyst AND menu_id = 6150);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 72043, @role_id_data_analyst, 6151, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_data_analyst IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 72043) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_data_analyst AND menu_id = 6151);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 72044, @role_id_data_analyst, 6152, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_data_analyst IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 72044) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_data_analyst AND menu_id = 6152);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 72045, @role_id_data_analyst, 6153, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_data_analyst IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 72045) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_data_analyst AND menu_id = 6153);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 72046, @role_id_data_analyst, 6154, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_data_analyst IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 72046) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_data_analyst AND menu_id = 6154);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 72047, @role_id_data_analyst, 6156, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_data_analyst IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 72047) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_data_analyst AND menu_id = 6156);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 72048, @role_id_data_analyst, 6157, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_data_analyst IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 72048) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_data_analyst AND menu_id = 6157);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 72049, @role_id_data_analyst, 6158, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_data_analyst IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 72049) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_data_analyst AND menu_id = 6158);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 72050, @role_id_data_analyst, 6159, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_data_analyst IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 72050) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_data_analyst AND menu_id = 6159);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 72051, @role_id_data_analyst, 6165, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_data_analyst IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 72051) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_data_analyst AND menu_id = 6165);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 72052, @role_id_data_analyst, 6168, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_data_analyst IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 72052) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_data_analyst AND menu_id = 6168);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) SELECT 72053, @role_id_data_analyst, 6174, 'adr-064-seed', 1, 2 FROM DUAL WHERE @role_id_data_analyst IS NOT NULL AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = 72053) AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_data_analyst AND menu_id = 6174);

COMMIT;

-- Expected menu counts (ADR-064 §5):
--   ip_group_leader: 48
--   ops_manager: 71
--   finance: 34
--   content_editor: 29
--   ops_operator: 34
--   data_analyst: 54

-- =============================================================================
-- ===== 04_baseline_dicts.sql =====
-- SKIPPED — Greenfield production has no wd DB; confirm Football dict_* exists
-- =============================================================================

-- SKIPPED on greenfield production.
-- Reason: 04_baseline_dicts.sql merges dict_* from legacy wd DB (V152).
-- Action:  Confirm Football dict_* exists in shenyu-system (see OPERATIONS-GUIDE.md).

