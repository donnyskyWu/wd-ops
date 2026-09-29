# OPS 列表页分页治理计划

> **版本**：v1.1 | 2026-09-18  
> **范围**：`football-front/apps/web-ele/src/views/ops/**` + `football-module-ops` 列表 API  
> **触发背景**：M4 账号批量导入 100+ 条后，用户无法浏览完整数据  
> **状态**：Phase 0 + Phase A + Phase 2 + Phase 3 批次 A 已完成（2026-09-18）

---

## 实施进度（2026-09-18）

### 已完成

| 批次 | 内容 | 文件/页面数 |
|------|------|-------------|
| **Phase 0** | `Pagination.vue` 已绑定 `@current-change` / `@size-change` → `emit('change')`；`ElPagination` 显式 import | 1 组件 |
| **Phase A** | 6 个 M8 配置页错误事件修复：`@current-change` → `@update:current-page` + `@change="loadList"` | AiModel / AiPrompt / ExternalCollect / ExternalData / OrderCollect / Threshold |
| **Phase A 验证** | 其余 22 个 `<Pagination>` 消费页已有 `@change`，Phase 0 修复后翻页生效 | InternalAccount、content/task、ParamManage 等 |
| **Phase B** | 监测页补分页 UI（后端已有 pageNum/pageSize） | HotWorksAnalysis、LowScoreAnalysis、FansAccountAnalysis |
| **M4 迁移** | Realname / Simcard / Company / Phone 已用 `<Pagination>` + `@change` | 4 页 |
| **Phase 2（2026-09-18）** | IP 组成员/作者 Tab 前后端分页 | IpGroup.vue + IpGroupController |
| **Phase 2** | 内部作品 InternalContent | 后端已有 page/size；前端 Pagination 默认 20 |
| **Phase 2** | 人效盘点 Efficiency | ProductivityReview 分页 UI + page/size 传参 |
| **Phase 2** | 工作任务矩阵 | 前端矩阵行客户端分页（summary 仍全量） |
| **Phase 2** | 外部/高粉/低粉账号分析 | ExternalAccountAnalysis（聚合后客户端分页）、High/LowFans（服务端分页） |
| **Phase 2** | 漏斗/指标分析 | FunnelAnalysis 自定义漏斗列表、MetricAnalysis 指标明细表 |
| **Phase 3 批次 A（2026-09-18）** | 计划/公推模板/我的查询/指标/绩效三页 + 10 张报表子页 | 统一 `<Pagination>` + `handleFilterSearch`/`handlePageChange` 拆分 |

#### Phase 3 批次 A 明细

| 页面 | 文件 | 后端分页 | 状态 |
|------|------|----------|------|
| 计划管理 | `production/plan/index.vue` | ✅ `ContentPlanController` pageNo | ✅ Pagination + 翻页 bug 修复 |
| 公推模板库 | `production/layout-template/index.vue` | ✅ `LayoutTemplateController` pageNum | ✅ Pagination |
| 我的查询 | `analysis/CustomQuery.vue`（Tab2） | ✅ `CustomQueryController` | ✅ Pagination |
| 指标管理 | `analysis/MetricManage.vue` | ✅ `MetricController` pageNum | ✅ Pagination |
| 考核执行 | `performance/PerfExecution.vue` | ✅ `PerfRecordController` | ✅ Pagination |
| 绩效结果 | `performance/PerfResult.vue` | ✅ `PerfResultController` | ✅ Pagination |
| 考核模板 | `performance/PerfTemplate.vue` | ✅ `PerfTemplateController` | ✅ Pagination |
| 统一视图 | `analysis/ReportUnifiedAccount.vue` | ✅ ReportController | ✅ Pagination |
| 状态监控 | `analysis/ReportAccountStatus.vue` | ✅ | ✅ Pagination |
| 短视频产出 | `analysis/ReportVideoOutput.vue` | ✅ | ✅ Pagination |
| 直播时长 | `analysis/ReportLiveDuration.vue` | ✅ | ✅ Pagination |
| 成本分摊 | `analysis/ReportCostAllocation.vue` | ✅ | ✅ Pagination |
| ROI 分析 | `analysis/ReportRoi.vue` | ✅ | ✅ Pagination |
| 异常预警 | `analysis/ReportAccountAlert.vue` | ✅ | ✅ Pagination |
| 团队配置 | `analysis/ReportTeamConfig.vue` | ⚠️ 传 pageNum；无 PageResult 时客户端 slice | ✅ Pagination |
| IP业务月达成 | `analysis/MonthlyAchievementReport.vue` | ❌ 全量数组 | ✅ 客户端 Pagination |
| 周度私域转化 | `analysis/WeeklyFunnelReport.vue` | ❌ 全量数组 | ✅ 客户端 Pagination |

### 仍待后端分页或产品决策

| 页面 | 原因 |
|------|------|
| IP业务月达成 / 周度私域转化 | `PrivateDomainReportController` 仍返回全量数组；前端已客户端 Pagination，待后端补 pageNum |
| IP主题 IPThemeData | 多 Tab + 图表，竞品列表 API 有 pageNum 待专项 |
| 内部采集 InternalCollectConfig | 奥创账号/远程子账号列表无标准 PageResult |
| 公司详情 CompanyDetail | 关联账号子表全量 |
| 工作任务矩阵（后端） | 当前为前端 slice；跨页日期 rowspan 需产品确认是否改 API |
| 外部账号分析（服务端） | 账号由作品聚合，暂无账号级分页 API |
| 详情页内嵌表、Dashboard、财务图表页等 | 低优先级 |

### 用户验证步骤

1. **平台账号**（`/ops/internal/internal-account`）：导入或确保 >20 条 → 点击第 2 页 → 表格行变化、total 正确  
2. **实名人**（`/ops/internal/realname`）：翻页 + 改每页条数 → 数据 reload  
3. **AI 模型配置**（M8 config）：翻页 → 列表变化（本次修复的 6 页之一）  
4. **爆款作品分析**：底部分页器出现 → 翻页请求带 pageNum/pageSize  

---

## A. 问题分析

### A.1 根因（三层叠加）

| 层级 | 现象 | 影响 |
|------|------|------|
| **P0 组件缺陷** | 公共组件 `Pagination.vue` 在内部 `el-pagination` 上绑定 `@change`，而 Element Plus 实际事件为 `current-change` / `size-change`，导致 **`change` 事件永不触发** | 25 个使用 `<Pagination>` 的页面，翻页只改 `pageNo`、**不重新请求 API** |
| **P0 页面接线不完整** | 多数 `<Pagination>` 页在 `@update:current-page` 中仅赋值 `pageNo`，未调用 `loadData()`；仅改 `pageSize` 时会 reload | 与上条叠加，用户看到分页器但**点击页码数据不变** |
| **P1 无分页 UI** | 25 个含 `el-table` 的页面完全无分页组件，部分硬编码 `pageSize: 30` 或一次拉全量 | 大数据量下只能看到前 N 条 |
| **P1 后端有、前端无** | 9 个页面 API 已传 `pageNo/pageNum`，但模板无 `el-pagination` | 后端能力浪费 |
| **P2 命名不一致** | M2 SOP 等用 `pageNum`，M4 用 `pageNo`（见 SESSION-PROGRESS） | 个别页传参错位导致 total=0 或永远第一页 |
| **P2 后端缺分页** | 28 个 Controller 列表端点无 `pageNo/pageNum`（如 IP 组成员、工作任务矩阵、部分 M1 分析） | 需前后端同步改造 |

### A.2 关键澄清：M4 并非「全无分页」

| 页面 | 分页 UI | 翻页是否生效 | 说明 |
|------|---------|-------------|------|
| 平台账号 `InternalAccountManage` | ✅ `<Pagination>` | ❌ **失效** | 100+ 导入后只能看默认 10 条，**与用户反馈直接相关** |
| 实名人 `RealnameManage` | ✅ `el-pagination` | ✅ 正常 | 参考模板 |
| 手机卡 `SimcardManage` | ✅ `el-pagination` | ✅ 正常 | 参考模板 |
| 公司 `CompanyManage` | ✅ `el-pagination` | ✅ 正常 | 参考模板 |
| 手机 `PhoneManage` | ✅ `el-pagination` | ✅ 正常 | 参考模板 |

### A.3 影响范围统计

扫描 `views/ops` 共 **82** 个含表格的 Vue 页面：

| 分类 | 数量 | 说明 |
|------|------|------|
| ✅ 分页完整可用 | **~23** | 原生 `el-pagination` + `@current-change` 触发 reload |
| ⚠️ 有 UI 但翻页失效 | **~25** | 使用 `<Pagination>` 组件或 `@update:current-page` 未 reload |
| ⚠️ 后端有分页、前端无 UI | **9** | 监测/分析/配置子页 |
| ❌ 完全无分页 | **25** | 含 IP 组 Tab 子表、监测分析、详情页内嵌表等 |

后端 `football-module-ops` Controller：**67** 个，其中 **39** 个已支持 `pageNo/pageNum` + `PageResult`，**28** 个列表仍为全量或非分页结构。

---

## B. 页面清单

> 状态图例：✅ 可用 | ⚠️ UI有/翻页坏 | ⚠️ 无UI有API | ❌ 无分页  
> 后端：✅ PageResult | ⚠️ 部分/非标准 | ❌ 无分页

### B.1 P0 — 账号管理（M4）

| 模块 | 页面/路由 | 当前状态 | 后端分页 | 优先级 | 备注 |
|------|-----------|----------|----------|--------|------|
| M4 | 平台账号 `/ops/internal/internal-account` | ⚠️ UI有/翻页坏 | ✅ `GET /ops/account/list` | **P0** | `<Pagination>` 翻页不 reload；导入 100+ 条阻塞 |
| M4 | 实名人 `/ops/internal/realname` | ✅ 可用 | ✅ `GET /ops/realname/list` | P0 验证 | 参考实现，回归即可 |
| M4 | 手机卡 `/ops/internal/simcard` | ✅ 可用 | ✅ `GET /ops/sim-card/list` | P0 验证 | 同上 |
| M4 | 公司 `/ops/internal/company` | ✅ 可用 | ✅ `GET /ops/company/list` | P0 验证 | 同上 |
| M4 | 手机 `/ops/internal/phone` | ✅ 可用 | ✅ `GET /ops/phone/list` | P0 验证 | 同上 |
| M4 | 公司详情 `/company/:id` | ❌ 子表无分页 | ⚠️ 关联账号全量 | P1 | 详情页内嵌表 |

### B.2 P1 — 内容生产 / 任务 / IP 组（M2 / M1）

| 模块 | 页面/路由 | 当前状态 | 后端分页 | 优先级 | 备注 |
|------|-----------|----------|----------|--------|------|
| M2 | 内容管理 `/ops/production/content` | ⚠️ UI有/翻页坏 | ✅ | **P1** | Pagination 组件 |
| M2 | 内容审核 `/ops/production/content/review` | ⚠️ UI有/翻页坏 | ✅ | P1 | 同上 |
| M2 | 我的任务 `/ops/production/task` | ⚠️ UI有/翻页坏 | ✅ | **P1** | 同上 |
| M2 | 全部任务 `/ops/production/task/all` | ⚠️ UI有/翻页坏 | ✅ | P1 | 同上 |
| M2 | 工作任务 `/ops/production/work-task` | ❌ 无分页 | ❌ `/work-task/sheet` 非 PageResult | **P1** | 矩阵按日展示，需产品定义是否分页 |
| M2 | 计划管理 `/ops/production/plan` | ✅ 可用 | ✅ | P1 验证 | Phase 3 已统一 Pagination |
| M2 | SOP `/ops/production/sop` | ⚠️ UI有/翻页坏 | ✅ `pageNum` | P1 | 注意 pageNum 命名 |
| M2 | 知识库 `/ops/production/knowledge` | ✅ 可用 | ✅ | P1 验证 | |
| M2 | 公推模板 `/ops/production/layout-template` | ✅ 可用 | ✅ | P1 验证 | Phase 3 已统一 Pagination |
| M1 | IP组 `/ops/operations/ip-group` | ❌ 成员/作者 Tab 无分页 | ❌ members/anchors 全量 | **P1** | 12 张子表，需 Tab 级分页 |
| M1 | 内部作品 `/ops/operations/internal-content` | ⚠️ UI有/翻页坏 | ⚠️ `/operations/internal-content/list` 无 page 参数 | P1 | **前后端都要改** |

### B.3 P1 — 配置 / 系统 / 财务

| 模块 | 页面/路由 | 当前状态 | 后端分页 | 优先级 | 备注 |
|------|-----------|----------|----------|--------|------|
| M8 | AI模型/提示词/采集配置等 7 页 | ⚠️ UI有/翻页坏 | ✅ | P1 | 统一修 Pagination 即可 |
| M9 | 系统参数 `/ops/system-oa/system-param` | ⚠️ UI有/翻页坏 | ✅ | P1 | `handlePageChange` 未 reload |
| M9 | 消息管理 | ⚠️ UI有/翻页坏 | ✅ | P1 | 同上 |
| M5 | 账号成本 | ⚠️ UI有/翻页坏 | ✅ | P1 | 同上 |

### B.4 P2 — 监测 / 分析 / 其他

| 模块 | 页面/路由 | 当前状态 | 后端分页 | 优先级 | 备注 |
|------|-----------|----------|----------|--------|------|
| M7 | 外部/高粉/低粉账号分析 | ❌ 无 UI | ✅ monitor API | P2 | 硬编码 pageSize:30 |
| M7 | 爆款/IP主题/低分作品 | ⚠️ 无UI有API | ✅ | P2 | 补 el-pagination |
| M6 | 漏斗/指标分析 | ⚠️ 无UI有API | ✅ | P2 | 图表页内嵌表 |
| M6 | 10 张报表独立页 | ✅ 可用 | ✅/⚠️ | P2 验证 | Phase 3 已统一 Pagination；私域 2 页客户端分页 |
| M1 | 人效盘点 `/operations/efficiency` | ❌ 无分页 | ❌ ProductivityReview 无 page | P2 | 36 张表，工作量大 |
| M3 | 绩效各列表 | ✅ 可用 | ✅ | P2 验证 | Phase 3 已统一 Pagination |
| M6 | 自定义查询/指标管理 | ✅ 可用 | ✅ | P2 验证 | Phase 3 已统一 Pagination |
| M10 | 采集任务/日志 | ⚠️ 任务日志 Pagination 坏 | ✅ | P2 | |
| M10 | 采集质量/日志详情 | ❌ 无分页 | ❌/⚠️ | P2 | 详情页 |

（完整 82 页扫描明细见仓库 `_tmp_pagination_scan_result.txt`）

---

## C. 开发计划

### Phase 0 — 公共组件热修（0.5 人日，阻塞解除）

**目标**：一次修复，25 页翻页立即生效。

1. 修改 `football-front/apps/web-ele/src/components/ops/Pagination.vue`  
   - `el-pagination` 改用 `@current-change` + `@size-change`  
   - 统一 `emit('change', page, size)` 供父组件 reload  
2. 审计 25 个 `<Pagination>` 消费页：`@update:current-page` 仅改值处补 `loadData()` 或依赖 `@change`  
3. **P0 冒烟**：平台账号导入 >20 条 → 翻页 2/3 → 数据变化；实名人/手机卡回归

**Slice 建议**：`S-PAG-0`（不跨模块，仅 components + 冒烟）

---

### Phase 1 — P0 M4 + 高频 M2（3~4 人日）

| 任务 | 页面 | 工作项 | 人日 |
|------|------|--------|------|
| 1.1 | InternalAccountManage | 验证 Phase 0 修复；默认 pageSize 20→可配置 | 0.5 |
| 1.2 | M4 其余 4 页 | 回归 TESTCASES；统一 pageSize 默认 20 | 0.5 |
| 1.3 | content / task / review | Pagination 修复后 E2E | 1 |
| 1.4 | ParamManage / MessageManage | 补 `handlePageChange` → `loadData()` | 0.5 |
| 1.5 | 文档 + ADR | 分页参数 SSOT：`pageNo` vs `pageNum` 映射表 | 0.5 |

**Slice 建议**：`S-PAG-1a`（M4）、`S-PAG-1b`（M2 内容/任务）

**验收**：M4 五列表页 P0 用例 100%；平台账号 100+ 条可翻页浏览。

---

### Phase 2 — P1 IP 组 / 内部作品 / 配置（5~6 人日）

| 任务 | 工作项 | 前后端 |
|------|--------|--------|
| 2.1 IP 组成员/作者 Tab | 新增 `pageNo/pageSize` 查询；Tab 内 `<Pagination>` | **前后端** |
| 2.2 InternalContent | Controller 补 page 参数；前端已有 UI，Phase 0 后验证 | **前后端** |
| 2.3 work-task | 与产品确认：矩阵按日是否分页 or 虚拟滚动 | 待定 |
| 2.4 M8 七配置页 | Phase 0 后批量验证 | 前端 |
| 2.5 M5 AccountCostManage | Phase 0 后验证 | 前端 |

**Slice 建议**：`S-PAG-2a`（M1 IP组）、`S-PAG-2b`（M1 InternalContent）

---

### Phase 3 — P2 监测 / 分析 / 长尾（8~10 人日）

| 批次 | 页面群 | 方案 |
|------|--------|------|
| 3.1 | 监测 6 页（External/High/Low/HotWorks/IPTheme/LowScore） | 补 `el-pagination`；Monitor API 已支持 pageNum |
| 3.2 | 分析 PARAM_NO_UI 9 页 | 补分页 UI + 对齐 pageNum |
| 3.3 | 人效/财务图表页 | 评估：分页 vs 导出 vs 虚拟滚动 |
| 3.4 | 详情页内嵌表 | 低优先级，按详情 PRD 逐个补 |

**Slice 建议**：按 M6/M7 模块各一片。

---

### 工作量汇总

| Phase | 范围 | 预估 | 产出 |
|-------|------|------|------|
| 0 | Pagination 组件 | 0.5d | 25 页翻页恢复 |
| 1 | P0 M4 + M2 核心 | 3~4d | 账号/内容/任务可浏览全量 |
| 2 | P1 IP组/配置 | 5~6d | 运营核心链路完整 |
| 3 | P2 监测分析 | 8~10d | 全模块覆盖 |
| **合计** | | **17~20 人日** | |

---

## D. 技术方案要点

### D.1 统一分页 Composable（新建）

路径建议：`football-front/apps/web-ele/src/composables/ops/useOpsListPage.ts`

```typescript
// 伪代码 — 实施时按项目风格落地
export function useOpsListPage<T, Q extends Record<string, unknown>>(options: {
  fetcher: (query: Q & { pageNo: number; pageSize: number }) => Promise<{ list: T[]; total: number }>
  defaultPageSize?: number
  pageParam?: 'pageNo' | 'pageNum'  // 适配 SOP 等历史 API
}) {
  const pageNo = ref(1)
  const pageSize = ref(options.defaultPageSize ?? 20)
  const total = ref(0)
  const list = ref<T[]>([])
  const loading = ref(false)

  async function reload() { /* 调 fetcher，写 list/total */ }
  function onPageChange(page: number) { pageNo.value = page; reload() }
  function onSizeChange(size: number) { pageSize.value = size; pageNo.value = 1; reload() }
  function onSearch() { pageNo.value = 1; reload() }

  return { list, total, loading, pageNo, pageSize, reload, onPageChange, onSizeChange, onSearch }
}
```

**原则**：
- 新页强制用 composable，禁止复制粘贴 pagination reactive
- 现有页迁移优先级：`<Pagination>` 消费页 → 无分页高频页

### D.2 统一分页组件

- **保留** `components/ops/Pagination.vue` 作为样式包装（圆角、阴影、右对齐）
- **修复** 事件：`current-change` / `size-change` → `emit('change')`
- **模板标准写法**：

```vue
<Pagination
  :current-page="pageNo"
  :page-size="pageSize"
  :total="total"
  @update:current-page="onPageChange"
  @update:page-size="onSizeChange"
/>
```

或直接使用 `el-pagination`（M4 参考实现），二者择一 SSOT，建议 **Composable + Pagination 包装**。

### D.3 默认 pageSize

| 场景 | 默认值 | pageSizes |
|------|--------|-----------|
| 管理列表（账号/实名人/公司） | **20** | `[10, 20, 50, 100]` |
| 内容/任务 | 20 | `[10, 20, 50]` |
| 日志/监测 | 20 | `[20, 50, 100]` |
| 选择器 remote 搜索 | 20 | 不超过 50 |

### D.4 API 契约对齐

| 模块 | 分页参数 | 响应 |
|------|----------|------|
| M4 账号/公司/实名人/手机/卡 | `pageNo`, `pageSize` | `CommonResult<PageResult<T>>` `{ list, total }` |
| M2 SOP/计划/部分任务 | `pageNum`, `pageSize` | 同上 |
| M6 监测/报表 | `pageNum`, `pageSize` | 同上 |

**前端 adapter**：在 `#/api/ops/*.ts` 层做 `pageNo ↔ pageNum` 归一，页面只认 `pageNo`。

### D.5 参考实现（已验证可用）

| 类型 | 文件路径 |
|------|----------|
| **最佳列表模板** | `football-front/apps/web-ele/src/views/ops/internal/RealnameManage.vue` |
| 同类参考 | `.../internal/CompanyManage.vue`、`.../internal/SimcardManage.vue`、`.../internal/PhoneManage.vue` |
| 分页包装组件 | `football-front/apps/web-ele/src/components/ops/Pagination.vue`（需 Phase 0 修复） |
| 后端 PageResult | `football-backend-saas/.../controller/account/PlatformAccountController.java` |
| 表格搜索区 | `football-front/apps/web-ele/src/components/ops/TableSearch.vue` |

### D.6 测试要求

- 每 Phase 更新 `docs/delivery/TESTCASES-M*` 增补：**列表 > pageSize 条 → 翻页 → 行变化 → total 正确**
- Playwright：至少 M4 平台账号 + M2 内容列表 2 条 E2E
- 回归：`pageNum` 模块（SOP/计划）不因 adapter 破坏

#### Playwright 分页 P0（2026-09-18）

| 文件 | 说明 |
|------|------|
| `football-front/apps/web-ele/tests/ops-list-pagination.spec.ts` | TC-PAG-01~08 分页翻页 E2E |
| `football-front/apps/web-ele/tests/helpers/ops-list-pagination.ts` | 公共 probe / flip 辅助 |

**运行**（需 `:5777` + `:48080` 已启动）：

```powershell
cd football-front/apps/web-ele
npx playwright test tests/ops-list-pagination.spec.ts --config=playwright.config.ts

# 或 Gate 一键脚本指定 spec
.\scripts\run-gate-football-e2e.ps1 -Spec tests/ops-list-pagination.spec.ts
```

**用例映射**：

| TC-ID | 路由 | API 分页参数 |
|-------|------|-------------|
| TC-PAG-01 | `/ops/internal/realname` | `pageNo=2` |
| TC-PAG-02 | `/ops/internal/internal-account`（抖音 Tab） | `pageNo=2` |
| TC-PAG-03 | `/ops/internal/simcard` | `pageNo=2` |
| TC-PAG-04 | `/ops/operations/ip-group`（成员 Tab） | `pageNum=2` |
| TC-PAG-05 | `/ops/production/knowledge` | `pageNum=2` |
| TC-PAG-06 | `/ops/production/plan` | `pageNo=2` |
| TC-PAG-07 | `/ops/analysis/metric` | `pageNum=2` |
| TC-PAG-08 | `/ops/performance/perf-execution` | `pageNum=2` |

数据量不足（total ≤ pageSize）时自动 `test.skip` 并注明原因。

---

## E. 阻塞 / 决策项

1. **work-task 矩阵**：按日场次表格是否分页？建议保持全量展示 + 虚拟滚动，与列表分页区分。
2. **pageNo vs pageNum**：建议 ADR 统一对外 `pageNo`，模块 adapter 内部转换。
3. **IpGroup 关联账号 Tab**：若已移除需确认 Spec；若有，需补 API 分页。

---

## F. 相关文件

- 扫描脚本：`_tmp_scan_pagination.py`、`_tmp_pagination_scan_result.txt`
- 后端扫描：`_tmp_backend_pagination_scan.txt`
- 菜单路由 SSOT：`docs/delivery/OPS-MENU-ROUTE-INDEX.md`
