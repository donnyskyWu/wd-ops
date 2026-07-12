# UAT 抽检报告 — 扩展模块（2026-07-04）

> **环境**：Gateway `:48080` · system-server `:48081` · oa-server `:48094` · football-front `:5777`  
> **登录**：`admin` / `admin123` · 租户 ID **1**  
> **脚本**：`scripts/uat-spotcheck-expanded-20260704.py` · 原始探针 JSON：`docs/delivery/uat-spotcheck-expanded-20260704-probe.json`

---

## 1. 抽检范围总表

| 模块 | 页数 | Route | Vite | Theme | API | **结论** |
|------|:----:|:-----:|:----:|:-----:|:---:|:--------:|
| 数据分析 | 8 | 8/8 | 8/8 | 8/8 | 8/8 | **8/8 PASS** |
| 作品监测 | 6 | 6/6 | 6/6 | 6/6 | 6/6 | **6/6 PASS** |
| 配置管理 | 8 | 8/8 | 8/8 | 8/8 | 8/8 | **8/8 PASS** |
| 绩效核算 + 财务管理 | 6 | 6/6 | 6/6 | 6/6 | 6/6 | **6/6 PASS** |
| 系统管理(OA) | 5 | 5/5 | 5/5 | 5/5 | 5/5 | **5/5 PASS** |
| 数据采集 | 4 | 4/4 | 4/4 | 4/4 | 4/4 | **4/4 PASS** |
| **合计** | **37** | **37/37** | **37/37** | **37/37** | **37/37** | **37/37 PASS** |

> 本轮探针：登录经 **system-server :48081**（Nacos 未运行时 Gateway 503）；OA API 经 **oa-server :48094**。全栈 + Nacos 就绪时脚本自动优先 Gateway。

---

## 2. 验收维度

| 维度 | 方法 |
|------|------|
| 登录 | `POST /admin-api/system/auth/login` → `code=0` + `accessToken`（Gateway 优先，fallback system-server） |
| 主 API | Bearer + `X-Tenant-Id:1` → `code=0` + 结构合理 |
| Vite 编译 | `GET :5777/src/views/{component}.vue` → HTTP 200，无 `Failed to resolve` |
| Theme | 根节点 `ops-page`；`<style>` 无 `#fff`/`#1890ff` 硬编码 |
| 搜索区 | 人工检视模板：TableSearch / Tab / 日期筛选 |

---

## 3. 数据分析（8/8 PASS）

| 页面 | Football Hash | 组件 | Route | Vite | Theme | API | 数据摘要 | **结论** |
|------|---------------|------|:-----:|:----:|:-----:|:---:|----------|:--------:|
| 自定义查询 | `#/ops/custom-query` | `ops/analysis/CustomQuery` | ✅ | ✅ | ✅ | ✅ | list=3 total=3 | **PASS** |
| 数据报表 | `#/ops/data-report` | `ops/analysis/ReportCenter` | ✅ | ✅ | ✅ | ✅ | unified-account stats 6 keys | **PASS** |
| 总体财务分析 | `#/ops/financial-analysis` | `ops/finance/FinancialAnalysis` | ✅ | ✅ | ✅ | ✅ | ROI analysis 4 keys | **PASS** |
| 漏斗分析 | `#/ops/funnel-analysis` | `ops/analysis/FunnelAnalysis` | ✅ | ✅ | ✅ | ✅ | list=5 total=5 | **PASS** |
| 指标管理 | `#/ops/metric` | `ops/analysis/MetricManage` | ✅ | ✅ | ✅ | ✅ | list=10 total=32 | **PASS** |
| 指标分析 | `#/ops/metric-analysis` | `ops/analysis/MetricAnalysis` | ✅ | ✅ | ✅ | ✅ | list=10 total=32 | **PASS** |
| 数据大屏 | `#/ops/screen` | `ops/screen/DataScreenFullscreen` | ✅ | ✅ | ✅ | ✅ | dashboard-config list=3 | **PASS** |
| 大屏配置 | `#/ops/screen-config` | `ops/screen/ScreenConfig` | ✅ | ✅ | ✅ | ✅ | dashboard-config list=3 | **PASS** |

**API 探针路径**

| 页面 | 主 API |
|------|--------|
| 自定义查询 | `GET /admin-api/oa/query/list?pageNo=1&pageSize=10` |
| 数据报表 | `GET /admin-api/oa/report/unified-account/stats` |
| 总体财务分析 | `GET /admin-api/oa/finance/roi/analysis?startDate=2026-01-01&endDate=2026-06-30` |
| 漏斗分析 | `GET /admin-api/oa/funnel/list?pageNo=1&pageSize=10` |
| 指标管理 / 指标分析 | `GET /admin-api/oa/metric/list?pageNo=1&pageSize=10` |
| 数据大屏 / 大屏配置 | `GET /admin-api/oa/dashboard-config/list?pageNum=1&pageSize=10` |

---

## 4. 作品监测（6/6 PASS）

| 页面 | Football Hash | 组件 | Route | Vite | Theme | API | 数据摘要 | **结论** |
|------|---------------|------|:-----:|:----:|:-----:|:---:|----------|:--------:|
| 外部账号分析 | `#/ops/external-account` | `ops/account/ExternalAccountAnalysis` | ✅ | ✅ | ✅ | ✅ | list=1 total=19 | **PASS** |
| 高粉账号分析 | `#/ops/high-fans-account` | `ops/account/HighFansAccountAnalysis` | ✅ | ✅ | ✅ | ✅ | list=1 total=1 | **PASS** |
| 爆款作品分析 | `#/ops/hot-works` | `ops/content/HotWorksAnalysis` | ✅ | ✅ | ✅ | ✅ | list=1 total=7 | **PASS** |
| IP主题数据 | `#/ops/ip-theme` | `ops/content/IPThemeData` | ✅ | ✅ | ✅ | ✅ | topTitles/workCount | **PASS** |
| 低粉账号分析 | `#/ops/low-fans-account` | `ops/account/LowFansAccountAnalysis` | ✅ | ✅ | ✅ | ✅ | list=0 total=0（空合法） | **PASS** |
| 低分作品分析 | `#/ops/low-score` | `ops/content/LowScoreAnalysis` | ✅ | ✅ | ✅ | ✅ | list=1 total=4 | **PASS** |

**API 探针路径**

| 页面 | 主 API |
|------|--------|
| 外部账号分析 | `GET /admin-api/oa/monitor/external/list?pageNo=1&pageSize=1` |
| 高粉账号分析 | `GET /admin-api/oa/monitor/high-follower/list?pageNo=1&pageSize=1` |
| 爆款作品分析 | `GET /admin-api/oa/monitor/hit/list?pageNo=1&pageSize=1` |
| IP主题数据 | `GET /admin-api/oa/monitor/ip-theme/1` |
| 低粉账号分析 | `GET /admin-api/oa/monitor/low-follower/list?pageNo=1&pageSize=1` |
| 低分作品分析 | `GET /admin-api/oa/monitor/low-score/list?pageNo=1&pageSize=1` |

---

## 5. 配置管理（8/8 PASS）

| 页面 | Football Hash | 组件 | Route | Vite | Theme | API | 数据摘要 | **结论** |
|------|---------------|------|:-----:|:----:|:-----:|:---:|----------|:--------:|
| AI模型 | `#/ops/config-ai-model` | `ops/config/AiModelConfig` | ✅ | ✅ | ✅ | ✅ | list=1 total=3 | **PASS** |
| AI提示词 | `#/ops/config-ai-prompt` | `ops/config/AiPromptConfig` | ✅ | ✅ | ✅ | ✅ | list=1 total=8 | **PASS** |
| 外部采集配置 | `#/ops/config-external-collect` | `ops/config/ExternalCollectConfig` | ✅ | ✅ | ✅ | ✅ | list=1 total=7 | **PASS** |
| 外部数据配置 | `#/ops/config-external-data` | `ops/config/ExternalDataConfig` | ✅ | ✅ | ✅ | ✅ | list=1 total=3 | **PASS** |
| 内部采集配置 | `#/ops/config-internal-collect` | `ops/config/InternalCollectConfig` | ✅ | ✅ | ✅ | ✅ | list=1 total=2 | **PASS** |
| 元数据维护 | `#/ops/config-metadata` | `ops/config/MetadataManage` | ✅ | ✅ | ✅ | ✅ | list=1 total=9 | **PASS** |
| 订单采集配置 | `#/ops/config-order-collect` | `ops/config/OrderCollectConfig` | ✅ | ✅ | ✅ | ✅ | list=1 total=4 | **PASS** |
| 阈值规则配置 | `#/ops/config-threshold` | `ops/config/ThresholdConfig` | ✅ | ✅ | ✅ | ✅ | list=1 total=10 | **PASS** |

**API 探针路径**

| 页面 | 主 API |
|------|--------|
| AI模型 | `GET /admin-api/oa/config/ai-model/list?pageNo=1&pageSize=1` |
| AI提示词 | `GET /admin-api/oa/config/ai-prompt/list?pageNo=1&pageSize=1` |
| 外部采集配置 | `GET /admin-api/oa/config/external-collect/list?pageNo=1&pageSize=1` |
| 外部数据配置 | `GET /admin-api/oa/config/external-source/list?pageNo=1&pageSize=1` |
| 内部采集配置 | `GET /admin-api/oa/config/internal-collect/list?pageNo=1&pageSize=1` |
| 元数据维护 | `GET /admin-api/oa/metadata/list?pageNo=1&pageSize=1` |
| 订单采集配置 | `GET /admin-api/oa/config/order-collect/list?pageNo=1&pageSize=1` |
| 阈值规则配置 | `GET /admin-api/oa/config/threshold/list?pageNo=1&pageSize=1` |

---

## 6. 绩效核算 + 财务管理（6/6 PASS）

> **脚本**：`scripts/uat-spotcheck-expanded-perf-finance-20260704.py` · 探针 JSON：`docs/delivery/uat-spotcheck-expanded-perf-finance-20260704-probe.json`  
> **P2b**：订单归因列表已接 `GET /admin-api/oa/football-order/list`（`pay_all_order` 只读 SSOT）；ROI/导出仍走 `order-attribution/*` hybrid。

| 页面 | Football Hash | 组件 | Route | Vite | Theme | API | 数据摘要 | **结论** |
|------|---------------|------|:-----:|:----:|:-----:|:---:|----------|:--------:|
| 订单归因 | `#/ops/order-attribution` | `ops/performance/OrderAttribution` | ✅ | ✅ | ✅ | ✅ | football-order list=1 total=2 | **PASS** |
| 考核执行 | `#/ops/perf-execution` | `ops/performance/PerfExecution` | ✅ | ✅ | ✅ | ✅ | perf/record list=1 total=6 | **PASS** |
| 考核模板 | `#/ops/perf-template` | `ops/performance/PerfTemplate` | ✅ | ✅ | ✅ | ✅ | perf/template list=1 total=3 | **PASS** |
| 绩效结果 | `#/ops/perf-result` | `ops/performance/PerfResult` | ✅ | ✅ | ✅ | ✅ | perf/result list=0 total=0（空合法） | **PASS** |
| 账号成本 | `#/ops/account-cost` | `ops/finance/AccountCostManage` | ✅ | ✅ | ✅ | ✅ | finance/cost list=1 total=18 | **PASS** |
| ROI分析 | `#/ops/roi-analysis` | `ops/finance/RoiAnalysis` | ✅ | ✅ | ✅ | ✅ | ROI analysis 4 keys | **PASS** |

**API 探针路径**

| 页面 | 主 API | 备注 |
|------|--------|------|
| 订单归因 | `GET /admin-api/oa/football-order/list?startDate=2026-01-01&endDate=2026-06-30&pageNum=1&pageSize=1` | P2b 只读 Football 订单 |
| 考核执行 | `GET /admin-api/oa/perf/record/list?pageNo=1&pageSize=1` | |
| 考核模板 | `GET /admin-api/oa/perf/template/list?pageNo=1&pageSize=1` | |
| 绩效结果 | `GET /admin-api/oa/perf/result/list?pageNo=1&pageSize=1` | |
| 账号成本 | `GET /admin-api/oa/finance/cost/list?pageNo=1&pageSize=1` | |
| ROI分析 | `GET /admin-api/oa/finance/roi/analysis?startDate=2026-01-01&endDate=2026-06-30` | |

**搜索区**

| 页面 | 搜索区 |
|------|--------|
| 订单归因 | TableSearch：日期范围 / IP组 |
| 考核执行 | TableSearch：考核周期 / 状态 / 模板 |
| 考核模板 | TableSearch：模板名称 / 岗位 / 状态 |
| 绩效结果 | TableSearch：考核周期 / 等级 / 经办人 |
| 账号成本 | 平台 Tab + TableSearch：账号 / 成本周期 |
| ROI分析 | 日期范围 + KPI 卡 / 趋势 / 维度分解 |

---

## 7. 系统管理(OA)（5/5 PASS）

> **菜单**：6137–6141 · **脚本**：`scripts/uat-spotcheck-expanded-system.py` · 探针 JSON：`docs/delivery/uat-spotcheck-expanded-system-probe.json`  
> **§19 字典修复复验**：`GET /admin-api/oa/dict/type/list`（legacy alias → `SystemDictService.typeList()`）· `GET /admin-api/oa/system/dict/list` · `GET /admin-api/oa/dict/data?type=dict_platform_type` — 经 oa-server :48094 均 **code=0**（Gateway 未注册 oa-server 时 code=500，非功能回归）。

| 页面 | Menu | Football Hash | 组件 | Route | Vite | Theme | API | 数据摘要 | **结论** |
|------|:----:|---------------|------|:-----:|:----:|:-----:|:---:|----------|:--------:|
| 字典配置 | 6137 | `#/ops/system-dict` | `ops/system/DictManage` | ✅ | ✅ | ✅ | ✅ 3/3 | type-list=94 · dict/list total=359 · dict/data=9 | **PASS** |
| 登录日志 | 6138 | `#/ops/system-log/login` | `ops/system/LoginLog` | ✅ | ✅ | ✅ | ✅ | log/login list=3 total=3 | **PASS** |
| 操作日志 | 6139 | `#/ops/system-log/operation` | `ops/system/LogManage` | ✅ | ✅ | ✅ | ✅ | log/operation list=10 total=686 | **PASS** |
| 消息管理 | 6140 | `#/ops/system-message` | `ops/system/MessageManage` | ✅ | ✅ | ✅ | ✅ | message/list list=10 total=92 | **PASS** |
| 系统参数 | 6141 | `#/ops/system-param` | `ops/system/ParamManage` | ✅ | ✅ | ✅ | ✅ | param/list list=10 total=11 | **PASS** |

**§19 字典 API 探针（字典配置页 + 全局 DictSelect）**

| 探针 | 路径 | code | 数据摘要 | 结论 |
|------|------|:----:|----------|:----:|
| type-list alias | `GET /admin-api/oa/dict/type/list` | 0 | array len=94 | ✅ §19 修复有效 |
| admin list | `GET /admin-api/oa/system/dict/list?pageNo=1&pageSize=10` | 0 | list=10 total=359 | ✅ |
| 业务读 | `GET /admin-api/oa/dict/data?type=dict_platform_type` | 0 | list=9 total=9 | ✅ |

**各页主 API**

| 页面 | 主 API |
|------|--------|
| 字典配置 | `GET /admin-api/oa/system/dict/list?pageNo=1&pageSize=10`（+ type-list / dict/data 见上） |
| 登录日志 | `GET /admin-api/oa/system/log/login?pageNo=1&pageSize=10` |
| 操作日志 | `GET /admin-api/oa/system/log/operation?pageNo=1&pageSize=10` |
| 消息管理 | `GET /admin-api/oa/system/message/list?pageNo=1&pageSize=10` |
| 系统参数 | `GET /admin-api/oa/system/param/list?pageNo=1&pageSize=10` |

**搜索区**

| 页面 | 搜索区 |
|------|--------|
| 字典配置 | TableSearch：字典名称 / 字典类型 / 状态 |
| 登录日志 | TableSearch：用户 / IP / 状态 / 时间 |
| 操作日志 | TableSearch：模块 / 操作人 / 类型 / 时间 |
| 消息管理 | TableSearch：标题 / 类型 / 状态 |
| 系统参数 | TableSearch：参数名称 / 键名 / 状态 |

---

## 8. 数据采集（4/4 PASS）

> **菜单 CSV**（`oa-menu-permission-map.csv`）：本批 4 页 — 采集任务、**数据质量**（口语「采集质量」）、内部采集配置、外部采集配置。后两者 parent_group=配置管理，与 M8 配置页共用组件。  
> **脚本**：`scripts/uat-spotcheck-20260704.py --section collect` · 探针 JSON：`docs/delivery/uat-spotcheck-collect-20260704-probe.json`  
> **登录 / API**：经 Gateway `:48080`（`gateway-integration-local.yaml` 增加 simple discovery + 直连路由，Nacos 不可用时仍 code=0）。

| 页面 | CSV menu_title | Football Hash | 组件 | Route | Vite | Theme | API | 数据摘要 | **结论** |
|------|----------------|---------------|------|:-----:|:----:|:-----:|:---:|----------|:--------:|
| 采集任务 | 采集任务 | `#/ops/collect/task` | `ops/collect/task` | ✅ | ✅ | ✅ | ✅ | collect/task list=1 total=7 | **PASS** |
| 数据质量 | 数据质量 | `#/ops/collect/quality` | `ops/collect/quality` | ✅ | ✅ | ✅ | ✅ | quality/list list=0 total=0（stub 空分页） | **PASS** |
| 内部采集配置 | 内部采集配置 | `#/ops/config-internal-collect` | `ops/config/InternalCollectConfig` | ✅ | ✅ | ✅ | ✅ | 默认企微 Tab → wework/list list=1 total=1 | **PASS** |
| 外部采集配置 | 外部采集配置 | `#/ops/config-external-collect` | `ops/config/ExternalCollectConfig` | ✅ | ✅ | ✅ | ✅ | external-collect/list subType=account list=4 total=4 | **PASS** |

**API 探针路径**

| 页面 | 主 API | 备注 |
|------|--------|------|
| 采集任务 | `GET /admin-api/oa/collect/task/page?pageNo=1&pageSize=1` | 列表页 `getCollectTaskPage` |
| 数据质量 | `GET /admin-api/oa/collect/quality/list?pageNo=1&pageSize=1` | M10 stub；前端 catch 时仍回 mock，API 已 code=0 |
| 内部采集配置 | `GET /admin-api/oa/internal/wework/list?pageNo=1&pageSize=1` | 默认 Tab 为企微 `WeworkAppConfigPanel` |
| 外部采集配置 | `GET /admin-api/oa/config/external-collect/list?pageNo=1&pageSize=10&subType=account` | 默认 Tab「外部账号」 |

**搜索区**

| 页面 | 搜索区 |
|------|--------|
| 采集任务 | TableSearch：任务名 / 平台 / 采集方式 / 频率 / 状态 |
| 数据质量 | TableSearch：规则名 / 检查类型 / 级别；右侧质量日志日期/级别 |
| 内部采集配置 | 企微/个微 Tab；个微 Tab 奥创表单 + 账号列表 |
| 外部采集配置 | Tab 外部账号 / 关键词；TableSearch 账号名称 / 平台 / 状态 |

---

## 9. 问题与跟进

| # | 现象 | 严重度 | 处置 |
|---|------|--------|------|
| 1 | Docker/Nacos 未运行 → Gateway login 503 | 环境 | 脚本 fallback system-server 登录 + oa-server 探针；启动 Nacos 后自动走 Gateway |
| 2 | Gateway Redis AUTH 不一致（无密码 vs 123456） | 环境 | 本地 `redis-cli CONFIG SET requirepass 123456` 后 Gateway 可启动 |
| 3 | 低粉账号分析 total=0 | 信息 | seed 无低粉样本；结构正常 |
| 4 | 漏斗分析 chart 色板 `#1890ff` 在 `<script>` | cosmetic | theme 探针仅查 `<style>`，与 P1 一致；非阻塞 |
| 5 | 绩效结果 total=0 | 信息 | seed 无已归档结果；结构正常 |
| 6 | Gateway OA 路由 code=500（Nacos 未注册 oa-server） | 环境 | 探针 fallback oa-server :48094；Nacos 就绪后走 Gateway |
| 7 | §19 复验：Gateway 上 dict/type/list 仍 code=500（Nacos） | 环境 | oa-server 直连 **code=0**；`DictController.adminTypeList()` + `@PreAuthorize('oa:dict:admin-list')` 行为正确 |
| 8 | 数据采集 Gateway OA 503/500（Nacos 未注册 + token check LB 失败） | 环境 | **`gateway-integration-local.yaml`** 增加 `spring.cloud.discovery.client.simple.instances`（system-server/oa-server 直连）+ 高优先级 direct routes；复测 **4/4 PASS** |
| 9 | 数据质量 list total=0 | 信息 | `CollectQualityController` v1 stub；结构正常 |

**前端修复**：数据采集本轮 **无**（4/4 PASS）；累计扩展 UAT **37/37** 无需改 vue/api。

---

## 10. 复跑命令

```powershell
# 确保全栈 UP（含 Nacos 时优先 Gateway）
.\scripts\start-integration-all.ps1 -SkipBuild

# 扩展 UAT 抽检（22 页：数据分析 + 作品监测 + 配置管理）
python scripts/uat-spotcheck-expanded-20260704.py

# 绩效核算 + 财务管理（6 页，含 P2b football-order/list）
python scripts/uat-spotcheck-expanded-perf-finance-20260704.py

# 系统管理(OA)（5 页，含 §19 字典 API 三探针）
python scripts/uat-spotcheck-expanded-system.py

# 数据采集（4 页：采集任务 / 数据质量 / 内部·外部采集配置）
python scripts/uat-spotcheck-20260704.py --section collect
```

---

## 11. 签核

| 角色 | 姓名 | 日期 | 结论 |
|------|------|------|------|
| 开发 | | | ⬜ |
| 测试 | | | ⬜ |
