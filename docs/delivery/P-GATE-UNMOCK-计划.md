# P-GATE-UNMOCK：前端 mock 兜底全面清理（Patch Gate）

**Gate ID**: P-GATE-UNMOCK
**触发**: 用户在 P-GATE-DICT 验证后提出「分析添加作者/作者管理列表问题的原因，其他模块是否也有类似问题」
**日期**: 2026-06-09
**作者**: AI Agent
**状态**: 📝 计划已批准，待分片实施

---

## 1. 根因复盘（作者管理家族）

作者管理（`operations/Author.vue` / `operations/AuthorDashboard.vue`）出现"添加报错 / 列表非真数据"，
本质是前端开发期遗留的 6 类反模式：

1. **`.catch(() => mockXxx)` 静默回退** — 真接口 4xx/5xx 时吞错，UI 仍显示假数据
2. **硬编码下拉选项** — `<el-option :value="..." :label="...">` 写死，跳过 API
3. **字典值硬编码** — `el-radio-group` 写 `LIVE/BOTH/IMAGE_TEXT`，与后端 `@InDict` 不匹配
4. **API 参数命名错位** — 前端 `pageNo/pageSize/authorName`，后端 `page/size/keyword`
5. **`toFixed`/`toString` 无 null 守卫** — 后端 list VO 不返回 KPI 字段时，前端崩
6. **API 文件用裸 `axios.create`** — 绕开 `@/utils/request` 拦截器，dev token 不注入 → 401

**核心根因（一句话）**：前端"以 mock 先跑通 UI"，未在 P-GATE 做"切真接口"回归，导致 mock
静默兜底成为通用反模式蔓延到 17 处。

---

## 2. 风险盘点（17 处）

| # | 模块 | 文件 | 模式 | 风险等级 |
|---|---|---|---|---|
| 1 | M1-IP组 | `views/operations/IpGroup.vue` | 5 个接口全部 `.catch(() => mockXxx)` | 🔴 |
| 2 | M1 作者详情 | `views/operations/AuthorDashboard.vue` | `kpi.roi.toFixed(2)` + `Object.assign(kpi, {...写死...})` | 🔴 |
| 3 | M3 内容分析 | `views/operations/WorksAnalysis.vue` | `getContentStats().catch(()=>mockContentStats)` | 🔴 |
| 4 | M8 财务 | `views/finance/AccountCostManage.vue` | `processList.value = [...mockProcessList]` | 🔴 |
| 5 | API 层 | `api/knowledge.ts` | **唯一**用 `axios.create` 的 API 文件 | 🔴 |
| 6 | 通用选择器 | `components/selectors/AccountSelect.vue` | `useMockFallback(list, mockAccountList)` | 🔴 |
| 7 | 通用选择器 | `components/selectors/PhoneSelect.vue` | `useMockFallback` | 🔴 |
| 8 | 通用选择器 | `components/selectors/SimCardSelect.vue` | `useMockFallback` | 🔴 |
| 9 | 通用选择器 | `components/selectors/RealNameSelect.vue` | `useMockFallback` | 🔴 |
| 10 | 通用选择器 | `components/selectors/CompanySelect.vue` | `useMockFallback` | 🔴 |
| 11 | 通用选择器 | `components/selectors/UserSelect.vue` | `.catch(() => mockUserList)` | 🔴 |
| 12 | 通用选择器 | `components/selectors/IpGroupTreeSelect.vue` | `.catch(() => mockIpGroupTree)` | 🔴 |
| 13 | M5 报表 | `views/analysis/FunnelAnalysis.vue` | `formatNumber` 无 null 守卫 | 🟡 |
| 14 | M5 报表 | `views/analysis/DataReport.vue` | toFixed 风险（已查） | 🟡 |
| 15 | M5 报表 | `views/finance/FinancialAnalysis.vue` | toFixed 风险 | 🟡 |
| 16 | M5 报表 | `views/performance/PerfUserTrend.vue` | toFixed 风险 | 🟡 |
| 17 | M5 报表 | `views/finance/RoiAnalysis.vue` | toFixed 风险 | 🟡 |

---

## 3. 切片设计（一片一会话）

按依赖关系拆 3 片：

### S-A：6 个 selector 组件去除 mock 兜底
- **范围**: 7 个文件
  - `components/selectors/AccountSelect.vue`
  - `components/selectors/PhoneSelect.vue`
  - `components/selectors/SimCardSelect.vue`
  - `components/selectors/RealNameSelect.vue`
  - `components/selectors/CompanySelect.vue`
  - `components/selectors/UserSelect.vue`
  - `components/selectors/IpGroupTreeSelect.vue`
  - `components/selectors/selector-utils.ts`（移除/简化 `useMockFallback`）
- **行为变更**: API 失败 → `options.value = []` + `ElMessage.error('加载失败')`
- **DoD**:
  - [ ] 所有 selector 在 dev 环境关掉 mock 兜底
  - [ ] 移除 `@/mock/selectors`、`@/mock/ip-group` import（仅这些文件）
  - [ ] 保留 `mock/*.ts` 文件（M3/M4/M5 报表还在用）
  - [ ] 后端 500 时，前端下拉为空 + 顶部红条报错

### S-B：4 个业务页面接通真 API
- **范围**: 4 个文件
  - `views/operations/IpGroup.vue`（删除 5 个 `.catch mock`）
  - `views/operations/AuthorDashboard.vue`（`loadAuthor` 接真 API，加 `kpi` null 守卫）
  - `views/operations/WorksAnalysis.vue`（删除 3 个 `.catch mock`）
  - `views/finance/AccountCostManage.vue`（`loadProcessList` 接真 API）
- **前置**: 后端 API 已确认可用（用户回复 `api-ready`）
- **DoD**:
  - [ ] 所有 `.catch(() => mockXxx)` 删除
  - [ ] `AuthorDashboard.vue` KPI 渲染全部 null-safe
  - [ ] API 失败时显式 `ElMessage.error`，不留假数据
  - [ ] 各页面 `loadXxx` 函数留 `try/finally` 关闭 loading

### S-C：API 层净化 + 报表 null 守卫
- **范围**: 2 个文件
  - `api/knowledge.ts`（删除 `axios.create`，改用 `@/utils/request`）
  - `views/analysis/FunnelAnalysis.vue`（`formatNumber` 加 null 守卫）
- **DoD**:
  - [ ] `knowledge.ts` 不再有 `import axios from 'axios'`
  - [ ] 知识库 5 个接口在 dev 环境能正常调用（带 dev token）
  - [ ] `FunnelAnalysis` 在 count 为 null/undefined 时显示 `0` 或 `-`

---

## 4. Gate 通过条件

1. 三片 S-A/S-B/S-C 全部完成且对应 DoD 100% 勾选
2. 后端 `mvn verify` 无新增失败（基线 158 ITs）
3. 手动启前后端，验证 4 个业务页面（IpGroup / AuthorDashboard / WorksAnalysis / AccountCostManage）从数据库拉到真数据
4. 验证 7 个 selector 在所有调用方（Author.vue / AddIpGroupDialog / AddAccountDialog 等）均能正常拉取
5. 归档 `docs/delivery/gates/P-GATE-UNMOCK-报告-{YYYYMMDD}.md`
6. 更新 `MASTER-EXECUTION-TRACKER.md` §13 收尾

---

## 5. 范围外（Out of Scope）

- 🟡 4 个报表页（#14-17）的 toFixed 风险：本 Gate 不做，纳入 `P-GATE-UNMOCK-R`（报表专项）
- 报表 mock 兜底：用户已确认"业务页接真，报表先保留 mock"
- 后端接口实现：用户已确认 `api-ready`
- E2E Playwright 回归：本期不做

---

## 6. 风险与回退

- **风险**: S-B 中 4 个页面可能隐藏未知 API 字段依赖
- **回退**: 任意页面跑不通 → 回退到原版本（git revert 该切片 commit）
- **前置验证**: 实施 S-B 前先 `grep -n "getXxxList\|getXxxPage"` 确认 API 已存在于 `api/*.ts`
