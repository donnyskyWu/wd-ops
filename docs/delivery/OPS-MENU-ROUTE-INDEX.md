# OPS 菜单 → 路由索引（Football :5777）

> **生成日期**：2026-07-27  
> **SSOT 来源**：`scripts/integration-config/seed-oa-system-menu.sql`（菜单树 parent_id + path）· `docs/delivery/oa-menu-permission-map.csv`（standalone / CSV football_path）  
> **重生成**：`python scripts/generate-ops-menu-route-index.py`

## 统计

- 菜单条目（6100–6999）：**71**
- CSV `football_path` 与 seed 嵌套路由不一致（type=2 页面）：**44**（见 E2E 备注列 ⚠️）

## 路由计算规则

从 `6100 运营数据`（path=`/ops`）沿 parent 链向下拼接各节点 `path` 段：

- 目录 type=1、页面 type=2 均参与拼接
- 示例：`6110 config` + `6165 config-metadata` → `/ops/config/config-metadata`
- **非** CSV 中扁平 `/ops/config-metadata`（除非与 seed 一致）
- Standalone（`:3000`）路由取自 CSV `route_path`，通常为扁平路径（不含分组前缀）

## 全量索引

| menu_id | 名称 | 模块 | seed path | Football 完整路由 | standalone 路由 | component | permission | E2E 备注 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- |
| 6100 | 运营数据 | 根目录 | `/ops` | `/ops` | `-` | `-` | `-` | 目录节点 |
| 6101 | 作品监测 | 作品监测 | `monitor` | `/ops/monitor` | `-` | `-` | `-` | 目录节点 |
| 6102 | 内容生产 | 内容生产 | `production` | `/ops/production` | `-` | `-` | `-` | 目录节点 |
| 6103 | 数据分析 | 数据分析 | `analysis` | `/ops/analysis` | `-` | `-` | `-` | 目录节点 |
| 6104 | 数据采集 | 数据采集 | `collect` | `/ops/collect` | `-` | `-` | `-` | 目录节点 |
| 6105 | 系统管理(OA) | 系统管理(OA) | `system-oa` | `/ops/system-oa` | `-` | `-` | `-` | 目录节点 |
| 6106 | 绩效核算 | 绩效核算 | `performance` | `/ops/performance` | `-` | `-` | `-` | 目录节点 |
| 6107 | 财务管理 | 财务管理 | `finance` | `/ops/finance` | `-` | `-` | `-` | 目录节点 |
| 6108 | 账号管理 | 账号管理 | `internal` | `/ops/internal` | `-` | `-` | `-` | 目录节点 |
| 6109 | 运营管理 | 运营管理 | `operations` | `/ops/operations` | `-` | `-` | `-` | 目录节点 |
| 6110 | 配置管理 | 配置管理 | `config` | `/ops/config` | `-` | `-` | `-` | 目录节点 |
| 6111 | 外部账号分析 | 作品监测 | `external-account` | `/ops/monitor/external-account` | `/external-account` | `ops/account/ExternalAccountAnalysis` | `oa:external-account:list` | ⚠️ 待修正 CSV=/ops/external-account |
| 6112 | 高粉账号分析 | 作品监测 | `high-fans-account` | `/ops/monitor/high-fans-account` | `/high-fans-account` | `ops/account/HighFansAccountAnalysis` | `oa:high-fans:list` | ⚠️ 待修正 CSV=/ops/high-fans-account |
| 6113 | 爆款作品分析 | 作品监测 | `hot-works` | `/ops/monitor/hot-works` | `/hot-works` | `ops/content/HotWorksAnalysis` | `oa:hot-works:list` | ⚠️ 待修正 CSV=/ops/hot-works |
| 6114 | IP主题数据 | 作品监测 | `ip-theme` | `/ops/monitor/ip-theme` | `/ip-theme` | `ops/content/IPThemeData` | `oa:ip-theme:list` | ⚠️ 待修正 CSV=/ops/ip-theme |
| 6115 | 低粉账号分析 | 作品监测 | `low-fans-account` | `/ops/monitor/low-fans-account` | `/low-fans-account` | `ops/account/LowFansAccountAnalysis` | `oa:low-fans:list` | ⚠️ 待修正 CSV=/ops/low-fans-account |
| 6116 | 低分作品分析 | 作品监测 | `low-score` | `/ops/monitor/low-score` | `/low-score` | `ops/content/LowScoreAnalysis` | `oa:low-score:list` | ⚠️ 待修正 CSV=/ops/low-score |
| 6117 | 内容管理 | 内容生产 | `content` | `/ops/production/content` | `/content` | `ops/production/content/index` | `oa:content:list` | ⚠️ 待修正 CSV=/ops/content |
| 6118 | 内容审核 | 内容生产 | `content/review` | `/ops/production/content/review` | `/content/review` | `ops/production/content/review` | `oa:content:list` | ⚠️ 待修正 CSV=/ops/content/review |
| 6119 | 内容知识库 | 内容生产 | `knowledge` | `/ops/production/knowledge` | `/knowledge` | `ops/production/knowledge/index` | `oa:knowledge:list` | ⚠️ 待修正 CSV=/ops/knowledge |
| 6120 | 公推模板库 | 内容生产 | `layout-template` | `/ops/production/layout-template` | `/layout-template` | `ops/production/layout-template/index` | `oa:layout-template:list` | ⚠️ 待修正 CSV=/ops/layout-template |
| 6121 | 计划管理 | 内容生产 | `plan` | `/ops/production/plan` | `/plan` | `ops/production/plan/index` | `oa:plan:list` | ⚠️ 待修正 CSV=/ops/plan |
| 6122 | SOP管理 | 内容生产 | `sop` | `/ops/production/sop` | `/sop` | `ops/production/sop/index` | `oa:sop:list` | ⚠️ 待修正 CSV=/ops/sop |
| 6123 | SOP审核 | 内容生产 | `sop/review` | `/ops/production/sop/review` | `/sop/review` | `ops/production/sop/review` | `oa:sop:list` | ⚠️ 待修正 CSV=/ops/sop/review |
| 6124 | 我的任务 | 内容生产 | `task` | `/ops/production/task` | `/task` | `ops/production/task/index` | `oa:task:list` | ⚠️ 待修正 CSV=/ops/task |
| 6194 | 工作任务管理 | 内容生产 | `work-task` | `/ops/production/work-task` | `/work-task` | `ops/production/work-task/index` | `ops:work-task:list` | FR-M2-010 · V183（6176–6190 beta 占用） |
| 6125 | 自定义查询 | 数据分析 | `custom-query` | `/ops/analysis/custom-query` | `/custom-query` | `ops/analysis/CustomQuery` | `oa:custom-query:list` |  |
| 6126 | 数据报表 | 数据分析 | `data-report` | `/ops/analysis/data-report` | `/data-report` | `ops/analysis/ReportCenter` | `oa:report:list` | ⚠️ 待修正 CSV=/ops/data-report |
| 6127 | 总体财务分析 | 数据分析 | `financial-analysis` | `/ops/analysis/financial-analysis` | `/financial-analysis` | `ops/finance/FinancialAnalysis` | `oa:financial-analysis:list` | ⚠️ 待修正 CSV=/ops/financial-analysis |
| 6128 | 漏斗分析 | 数据分析 | `funnel-analysis` | `/ops/analysis/funnel-analysis` | `/funnel-analysis` | `ops/analysis/FunnelAnalysis` | `oa:funnel-analysis:list` | ⚠️ 待修正 CSV=/ops/funnel-analysis |
| 6129 | 指标管理 | 数据分析 | `metric` | `/ops/analysis/metric` | `/metric` | `ops/analysis/MetricManage` | `oa:metric:list` |  |
| 6130 | 指标分析 | 数据分析 | `metric-analysis` | `/ops/analysis/metric-analysis` | `/metric-analysis` | `ops/analysis/MetricAnalysis` | `oa:metric-analysis:list` |  |
| 6131 | 数据大屏 | 数据分析 | `screen` | `/ops/analysis/screen` | `/screen` | `ops/screen/DataScreenFullscreen` | `oa:screen:view` |  |
| 6132 | 大屏配置 | 数据分析 | `screen-config` | `/ops/analysis/screen-config` | `/screen-config` | `ops/screen/ScreenConfig` | `oa:screen-config:list` |  |
| 6133 | 采集日志 | 数据采集 | `log` | `/ops/collect/log` | `/collect/log` | `ops/collect/log` | `oa:collect:log:list` |  |
| 6134 | 私域桥接 | 数据采集 | `private-domain-bridge` | `/ops/collect/private-domain-bridge` | `/collect/private-domain-bridge` | `ops/collect/private-domain-bridge` | `oa:collect:bridge:list` |  |
| 6135 | 数据质量 | 数据采集 | `quality` | `/ops/collect/quality` | `/collect/quality` | `ops/collect/quality` | `oa:collect:quality:list` |  |
| 6136 | 采集任务 | 数据采集 | `task` | `/ops/collect/task` | `/collect/task` | `ops/collect/task` | `oa:collect:task:list` |  |
| 6140 | 消息管理 | 系统管理(OA) | `system-message` | `/ops/system-oa/system-message` | `/system-message` | `ops/system/MessageManage` | `oa:message:list` | ⚠️ 待修正 CSV=/ops/system-message |
| 6141 | 系统参数 | 系统管理(OA) | `system-param` | `/ops/system-oa/system-param` | `/system-param` | `ops/system/ParamManage` | `oa:param:list` | ⚠️ 待修正 CSV=/ops/system-param |
| 6142 | 订单归因分析 | 绩效核算 | `order-attribution` | `/ops/performance/order-attribution` | `/order-attribution` | `ops/performance/OrderAttribution` | `oa:order-attribution:list` | ⚠️ 待修正 CSV=/ops/order-attribution |
| 6143 | 考核执行 | 绩效核算 | `perf-execution` | `/ops/performance/perf-execution` | `/perf-execution` | `ops/performance/PerfExecution` | `oa:perf:list` | ⚠️ 待修正 CSV=/ops/perf-execution |
| 6144 | 绩效结果 | 绩效核算 | `perf-result` | `/ops/performance/perf-result` | `/perf-result` | `ops/performance/PerfResult` | `oa:perf:list` | ⚠️ 待修正 CSV=/ops/perf-result |
| 6145 | 考核模板 | 绩效核算 | `perf-template` | `/ops/performance/perf-template` | `/perf-template` | `ops/performance/PerfTemplate` | `oa:perf:list` | ⚠️ 待修正 CSV=/ops/perf-template |
| 6146 | 账号成本管理 | 财务管理 | `account-cost` | `/ops/finance/account-cost` | `/account-cost` | `ops/finance/AccountCostManage` | `oa:cost:list` | ⚠️ 待修正 CSV=/ops/account-cost |
| 6147 | ROI分析 | 财务管理 | `roi-analysis` | `/ops/finance/roi-analysis` | `/roi-analysis` | `ops/finance/RoiAnalysis` | `oa:roi:list` | ⚠️ 待修正 CSV=/ops/roi-analysis |
| 6148 | 公司管理 | 账号管理 | `company` | `/ops/internal/company` | `/company` | `ops/internal/CompanyManage` | `oa:company:list` | ⚠️ 待修正 CSV=/ops/company |
| 6149 | 平台账号管理 | 账号管理 | `internal-account` | `/ops/internal/internal-account` | `/internal-account` | `ops/internal/InternalAccountManage` | `oa:platform-account:list` | ⚠️ 待修正 CSV=/ops/internal-account |
| 6151 | 手机管理 | 账号管理 | `phone` | `/ops/internal/phone` | `/phone` | `ops/internal/PhoneManage` | `oa:phone:list` | ⚠️ 待修正 CSV=/ops/phone |
| 6152 | 实名人管理 | 账号管理 | `realname` | `/ops/internal/realname` | `/realname` | `ops/internal/RealnameManage` | `oa:realname:list` | ⚠️ 待修正 CSV=/ops/realname |
| 6153 | 手机卡管理 | 账号管理 | `simcard` | `/ops/internal/simcard` | `/simcard` | `ops/internal/SimcardManage` | `oa:simcard:list` | ⚠️ 待修正 CSV=/ops/simcard |
| 6154 | 账号分析 | 运营管理 | `account-analysis` | `/ops/operations/account-analysis` | `/account-analysis` | `ops/operations/AccountAnalysis` | `oa:account-analysis:list` | ⚠️ 待修正 CSV=/ops/account-analysis |
| 6156 | 人效盘点 | 运营管理 | `efficiency` | `/ops/operations/efficiency` | `/efficiency` | `ops/operations/Efficiency` | `oa:efficiency:list` | ⚠️ 待修正 CSV=/ops/efficiency |
| 6157 | 粉丝分析 | 运营管理 | `fans-analysis` | `/ops/operations/fans-analysis` | `/fans-analysis` | `ops/operations/FansAnalysis` | `oa:fans-analysis:list` | ⚠️ 待修正 CSV=/ops/fans-analysis |
| 6158 | 内部作品分析 | 运营管理 | `internal-content` | `/ops/operations/internal-content` | `/internal-content` | `ops/operations/InternalContent` | `oa:internal-content:list` | ⚠️ 待修正 CSV=/ops/internal-content |
| 6159 | IP组管理 | 运营管理 | `ip-group` | `/ops/operations/ip-group` | `/ip-group` | `ops/operations/IpGroup` | `oa:ip-group:list` | ⚠️ 待修正 CSV=/ops/ip-group |
| 6160 | AI模型 | 配置管理 | `config-ai-model` | `/ops/config/config-ai-model` | `/config-ai-model` | `ops/config/AiModelConfig` | `oa:config:ai-model:list` | ⚠️ 待修正 CSV=/ops/config-ai-model |
| 6161 | AI提示词 | 配置管理 | `config-ai-prompt` | `/ops/config/config-ai-prompt` | `/config-ai-prompt` | `ops/config/AiPromptConfig` | `oa:config:ai-prompt:list` | ⚠️ 待修正 CSV=/ops/config-ai-prompt |
| 6162 | 外部采集配置 | 配置管理 | `config-external-collect` | `/ops/config/config-external-collect` | `/config-external-collect` | `ops/config/ExternalCollectConfig` | `oa:config:external-collect:list` | ⚠️ 待修正 CSV=/ops/config-external-collect |
| 6163 | 外部数据配置 | 配置管理 | `config-external-data` | `/ops/config/config-external-data` | `/config-external-data` | `ops/config/ExternalDataConfig` | `oa:config:external-data:list` | ⚠️ 待修正 CSV=/ops/config-external-data |
| 6164 | 内部采集配置 | 配置管理 | `config-internal-collect` | `/ops/config/config-internal-collect` | `/config-internal-collect` | `ops/config/InternalCollectConfig` | `oa:config:internal-collect:list` | ⚠️ 待修正 CSV=/ops/config-internal-collect |
| 6165 | 元数据维护 | 配置管理 | `config-metadata` | `/ops/config/config-metadata` | `/config-metadata` | `ops/config/MetadataManage` | `oa:metadata:query` |  |
| 6166 | 订单采集配置 | 配置管理 | `config-order-collect` | `/ops/config/config-order-collect` | `/config-order-collect` | `ops/config/OrderCollectConfig` | `oa:config:order-collect:list` | ⚠️ 待修正 CSV=/ops/config-order-collect |
| 6167 | 阈值规则配置 | 配置管理 | `config-threshold` | `/ops/config/config-threshold` | `/config-threshold` | `ops/config/ThresholdConfig` | `oa:config:threshold:list` | ⚠️ 待修正 CSV=/ops/config-threshold |
| 6168 | 首页仪表盘 | 首页 | `dashboard` | `/ops/dashboard` | `/dashboard` | `ops/Dashboard` | `oa:home:view` |  |
| 6170 | 公推模板创建 | 内容生产 | `` | `/ops/production/layout-template` | `-` | `-` | `oa:layout-template:create` | 按钮权限，无页面路由 |
| 6171 | 公推模板更新 | 内容生产 | `` | `/ops/production/layout-template` | `-` | `-` | `oa:layout-template:update` | 按钮权限，无页面路由 |
| 6172 | 公推模板删除 | 内容生产 | `` | `/ops/production/layout-template` | `-` | `-` | `oa:layout-template:delete` | 按钮权限，无页面路由 |
| 6173 | 公推模板导入 | 内容生产 | `` | `/ops/production/layout-template` | `-` | `-` | `oa:layout-template:import` | 按钮权限，无页面路由 |
| 6174 | 平台账号查询 | 账号管理 | `` | `/ops/internal/internal-account` | `-` | `-` | `oa:account:list` | 按钮权限，无页面路由 |
| 6175 | 全部任务 | 内容生产 | `task/all` | `/ops/production/task/all` | `/task/all` | `ops/production/task/all` | `oa:task:list` | ⚠️ 待修正 CSV=/ops/task/all |

## ⚠️ 待修正清单（CSV football_path ≠ seed 嵌套路由）

| menu_id | 名称 | seed 嵌套路由 | CSV football_path |
| ---: | --- | --- | --- |
| 6111 | 外部账号分析 | `/ops/monitor/external-account` | `/ops/external-account` |
| 6112 | 高粉账号分析 | `/ops/monitor/high-fans-account` | `/ops/high-fans-account` |
| 6113 | 爆款作品分析 | `/ops/monitor/hot-works` | `/ops/hot-works` |
| 6114 | IP主题数据 | `/ops/monitor/ip-theme` | `/ops/ip-theme` |
| 6115 | 低粉账号分析 | `/ops/monitor/low-fans-account` | `/ops/low-fans-account` |
| 6116 | 低分作品分析 | `/ops/monitor/low-score` | `/ops/low-score` |
| 6117 | 内容管理 | `/ops/production/content` | `/ops/content` |
| 6118 | 内容审核 | `/ops/production/content/review` | `/ops/content/review` |
| 6119 | 内容知识库 | `/ops/production/knowledge` | `/ops/knowledge` |
| 6120 | 公推模板库 | `/ops/production/layout-template` | `/ops/layout-template` |
| 6121 | 计划管理 | `/ops/production/plan` | `/ops/plan` |
| 6122 | SOP管理 | `/ops/production/sop` | `/ops/sop` |
| 6123 | SOP审核 | `/ops/production/sop/review` | `/ops/sop/review` |
| 6124 | 我的任务 | `/ops/production/task` | `/ops/task` |
| 6126 | 数据报表 | `/ops/analysis/data-report` | `/ops/data-report` |
| 6127 | 总体财务分析 | `/ops/analysis/financial-analysis` | `/ops/financial-analysis` |
| 6128 | 漏斗分析 | `/ops/analysis/funnel-analysis` | `/ops/funnel-analysis` |
| 6140 | 消息管理 | `/ops/system-oa/system-message` | `/ops/system-message` |
| 6141 | 系统参数 | `/ops/system-oa/system-param` | `/ops/system-param` |
| 6142 | 订单归因分析 | `/ops/performance/order-attribution` | `/ops/order-attribution` |
| 6143 | 考核执行 | `/ops/performance/perf-execution` | `/ops/perf-execution` |
| 6144 | 绩效结果 | `/ops/performance/perf-result` | `/ops/perf-result` |
| 6145 | 考核模板 | `/ops/performance/perf-template` | `/ops/perf-template` |
| 6146 | 账号成本管理 | `/ops/finance/account-cost` | `/ops/account-cost` |
| 6147 | ROI分析 | `/ops/finance/roi-analysis` | `/ops/roi-analysis` |
| 6148 | 公司管理 | `/ops/internal/company` | `/ops/company` |
| 6149 | 平台账号管理 | `/ops/internal/internal-account` | `/ops/internal-account` |
| 6151 | 手机管理 | `/ops/internal/phone` | `/ops/phone` |
| 6152 | 实名人管理 | `/ops/internal/realname` | `/ops/realname` |
| 6153 | 手机卡管理 | `/ops/internal/simcard` | `/ops/simcard` |
| 6154 | 账号分析 | `/ops/operations/account-analysis` | `/ops/account-analysis` |
| 6156 | 人效盘点 | `/ops/operations/efficiency` | `/ops/efficiency` |
| 6157 | 粉丝分析 | `/ops/operations/fans-analysis` | `/ops/fans-analysis` |
| 6158 | 内部作品分析 | `/ops/operations/internal-content` | `/ops/internal-content` |
| 6159 | IP组管理 | `/ops/operations/ip-group` | `/ops/ip-group` |
| 6160 | AI模型 | `/ops/config/config-ai-model` | `/ops/config-ai-model` |
| 6161 | AI提示词 | `/ops/config/config-ai-prompt` | `/ops/config-ai-prompt` |
| 6162 | 外部采集配置 | `/ops/config/config-external-collect` | `/ops/config-external-collect` |
| 6163 | 外部数据配置 | `/ops/config/config-external-data` | `/ops/config-external-data` |
| 6164 | 内部采集配置 | `/ops/config/config-internal-collect` | `/ops/config-internal-collect` |
| 6166 | 订单采集配置 | `/ops/config/config-order-collect` | `/ops/config-order-collect` |
| 6167 | 阈值规则配置 | `/ops/config/config-threshold` | `/ops/config-threshold` |
| 6175 | 全部任务 | `/ops/production/task/all` | `/ops/task/all` |

## 使用说明（E2E Agent）

1. **Gate 环境**：Football 壳 `http://localhost:5777/#` + 本表「Football 完整路由」列（非 Standalone `:3000`）。
2. **写 Playwright 导航**：`page.goto('http://localhost:5777/#' + football_route)` 或侧栏点击后断言 URL 含该路径。
3. **权限探针**：同路径可对照 `permission` 列与 Dev Token 角色菜单是否一致。
4. **路径不一致**：E2E 备注含 ⚠️ 时，以 **seed 嵌套路由** 为准写用例；同步提缺陷修正 CSV / Football 路由注册。
5. **隐藏/详情页**：本表仅含 system_menu 6100–6999；子页面（如 `/content/edit`）见 [`oa-menu-permission-map.csv`](./oa-menu-permission-map.csv) 中 `hide_in_menu=Y` 行。

## 一级分组速查（6101–6110）

| menu_id | 分组 | path 段 |
| ---: | --- | --- |
| 6101 | 作品监测 | `monitor` |
| 6102 | 内容生产 | `production` |
| 6103 | 数据分析 | `analysis` |
| 6104 | 数据采集 | `collect` |
| 6105 | 系统管理(OA) | `system-oa` |
| 6106 | 绩效核算 | `performance` |
| 6107 | 财务管理 | `finance` |
| 6108 | 账号管理 | `internal` |
| 6109 | 运营管理 | `operations` |
| 6110 | 配置管理 | `config` |
