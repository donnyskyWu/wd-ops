# Football 项目变更清单（Ops 集成）

> **用途**：供 Football 主仓库团队合并集成改动。  
> **SSOT**：[ADR-047](../adr/ADR-047-Football-Ops平台集成决策.md) · [INTEGRATION-PROGRESS](./INTEGRATION-PROGRESS.md)  
> **最后更新**：2026-07-04（S4-fix4 回归 — Pagination `computed`）

---

## S4-fix4 回归（2026-07-04）— `mount-ops-all.py` 覆盖 Pagination 导入

### 现象

Football `:5777` 登录后 **全部 Ops 页空白**，控制台：

1. `[intlify] Not found 'OAuth 2.0' key in 'zh'`（cosmetic，不阻塞）
2. `ReferenceError: computed is not defined`（`components/ops/Pagination.vue` setup）
3. 级联 `TypeError: Cannot read properties of null (reading 'parentNode')`（RouterView / LayoutContent Transition）

### 根因

`mount-ops-all.py` 自 `ops-platform-ui-vue` 全量 remount 时 **覆盖** 了 S4-fix4 对 `Pagination.vue` 的 `import { computed } from 'vue'`。源仓 `ops-platform-ui-vue/src/components/Pagination.vue` 亦未同步该导入，故 remount 必复发。

**未回归**：`bootstrap.ts` 仍 `app.use(opsElementPlusPlugin)`；`plugins/ops-element-plus.ts` 与 `styles/ops-theme.scss` 仍在。

### 修复

| 路径 | 处理 |
|------|------|
| `football-front/.../components/ops/Pagination.vue` | 补 `import { computed } from 'vue'` |
| `ops-platform-ui-vue/src/components/Pagination.vue` | 同上（remount 源） |

### 验证（2026-07-04，重启 Vite :5777 后）

| 检查 | 结果 |
|------|------|
| `scan-ops-vue-imports.py` | Pagination ✅；IPThemeData `watch` 为注释误报 |
| Playwright P0（4 页） | **4/4 PASS** — `#/ops/content/review`、`#/ops/plan`、`#/ops/ip-group`、`#/ops/order-attribution` |

---

## P2a（2026-07-04）— UserSelect 统一 Football `system_users`

### 根因

Football 登录后 Ops 页面内 `UserSelect` 仍请求 `GET /admin-api/oa/system/user/list`（读 `sys_user`），需 `oa:user:list` 权限 → **HTTP 403**；与 [ADR-049](../adr/ADR-049-Ops与Football数据归属与松耦合集成.md) D4「身份 SSOT = Football `system_users`」冲突。

### 方案（Option A）

前端经 Gateway 直调 Football system-server **`GET /admin-api/system/user/simple-list`**（Football Bearer + `tenant-id`），客户端按 nickname / deptId 过滤；**未改 oa-server**。`oa_author` 仍走 `/oa/author/*`，与 UserSelect 分离。

### 变更

| 路径 | 处理 |
|------|------|
| `football-front/.../api/ops/football-user.ts` | **新增**：`fetchSystemUserSimpleList` + 内存缓存 + `filterSystemUsers` |
| `football-front/.../components/ops/selectors/UserSelect.vue` | 数据源改为 `simple-list`；`ipGroupId` 分支仍用 `getIpGroupMembers` |
| `scripts/probe-p2a-user-select.py` | **新增**：Gateway 探针（Football login → simple-list vs oa list） |

### 已知限制

- `role-code` prop 保留但 **simple-list 不含角色**；原 oa list 亦只支持 `roleId` 非 `roleCode`，IpGroup「添加主播」暂无法按 ANCHOR 角色过滤（非本期）。
- `UserManage.vue` / `system-user.ts` M9 CRUD 仍走 oa 路径（D7：M9 身份页仅 Football 原生菜单）。

### Gateway 探针（Football login `admin` / tenant `1`）

| API | 结果 |
|-----|------|
| `GET /admin-api/system/user/simple-list` | **code=0**，12 条 `system_users` |
| `GET /admin-api/oa/system/user/list` | **code=403**（预期；Football token 无 `oa:user:list`） |

**验**：5777 Football 登录 → 打开含 UserSelect 的 Ops 页（如 `#/ops/internal/phone`、`#/ops/operations/ip-group`）→ Network 见 `/admin-api/system/user/simple-list`，下拉有用户昵称。

---

## S4-fix8（2026-07-03）— SOP 审核页前后端集成

### 根因

`football-front/.../views/ops/production/sop/review.vue` 的 `loadData` 仍为 TODO mock（模板维度字段 `templateName` / `contentType` / `submitter`），未调用 `GET /admin-api/oa/sop/review/pending`；表格列与后端 `SopReviewVO`（`planName` / `nodeName` / `reviewerRole` / `createTime`）不一致。

### 修复

| 路径 | 处理 |
|------|------|
| `football-front/.../views/ops/production/sop/review.vue` | 接通 `getSopReviewPending` / `approveReview` / `rejectReview`；列映射对齐 `SopReviewVO`；客户端筛选 + 分页（后端 `/pending` 不分页）；通过/驳回对话框与抽屉双入口 |
| `football-front/.../api/ops/sop.ts` | 已有正确封装，无改动 |

### API 响应形态（`SopReviewVO[]`）

```json
{
  "code": 0,
  "data": [
    {
      "id": 1,
      "taskId": 100,
      "planName": "6月内容计划",
      "nodeName": "撰写",
      "reviewerId": 1,
      "reviewerRole": "OPS_LEADER",
      "status": "PENDING",
      "comment": null,
      "createTime": "2026-07-03T10:00:00"
    }
  ]
}
```

### Gateway 探针（`Bearer dev-token-oa-admin` + `X-Tenant-Id: 1`）

| API | 结果 |
|-----|------|
| `GET /admin-api/oa/sop/review/pending` | **code=0**，`data: []`（无待审 seed 时为空数组，契约正确） |
| Vite compile `ops/production/sop/review` | **90/90** PASS |

**验**：5777 → `#/ops/sop/review` → 表格列「计划名称 / 节点名称 / 审核岗位 / 提交时间 / 状态」；Network 应见 `/admin-api/oa/sop/review/pending`，无 mock 延迟。

---

## S4-fix7（2026-07-03）— 首页仪表盘趋势/平台分布空态

### 根因

1. **非前端/API 契约问题**：`GET /admin-api/oa/dashboard/home/trend` 与 `platform-dist` 经 Gateway 返回 `code=0`；Football `requestClient`（`responseReturn: 'data'`）解包正常；`Dashboard.vue` 字段映射与 `TrendPointVO` / `PlatformDistVO` 一致。
2. **数据窗口错位**：`V39__seed_dashboard_content.sql` 固定写入 **2026-06-05 ~ 2026-06-11**；页面默认「近 7 天」为 **2026-06-27 ~ 2026-07-03**（以联调日为准），`oa_content.publish_time` 不在查询区间内 → 后端正确返回 `data: []`；KPI 卡「总作者数」仍非零（不按日期筛作者），「内容总数」为 0。
3. **无 Mock 降级**：原 `ops-platform-ui-vue` 的 `mock/dashboard.ts` 仅独立开发用，生产 `Dashboard.vue` 未接 mock fallback。

### 修复

| 路径 | 处理 |
|------|------|
| `ops-platform-module-oa/.../V129__seed_dashboard_content_rolling.sql` | **新增**：将 `seed-dashboard` 内容（id 9401–9414）按 `DATEDIFF(CURDATE(), '2026-06-11')` 平移至当前近 7 天；`updater='v129-seed-dashboard-rolling'` 保证幂等 |
| `football-front/.../views/ops/Dashboard.vue` | ECharts 轴/图例/饼图边框改读 `var(--el-*)`；`MutationObserver` 监听 `html.dark` 切换后重绘 |

### Gateway 探针（Bearer + `X-Tenant-Id: 1`，默认近 7 天）

| API | 修复前 | 修复后 |
|-----|--------|--------|
| `GET .../dashboard/home/trend?startDate=2026-06-27&endDate=2026-07-03&groupBy=PLATFORM` | `data: []` | `data`: **12** 点（5 平台 × 多日期） |
| `GET .../dashboard/home/platform-dist?startDate=2026-06-27&endDate=2026-07-03` | `data: []` | `data`: WECHAT_OFFICIAL 6 / DOUYIN 4 / … |
| `GET .../dashboard/home/metrics?...` | `totalContent: 0` | `totalContent: 14` |
| `POST .../dashboard/home/refresh` | — | 清 5min 内存缓存后上述接口即时生效 |

**验**：5777 登录 → `#/ops/dashboard` → 内容发布趋势折线 + 平台分布饼图应有数据；切换暗色后轴标签/图例可读。

**落库**：联调环境已手工执行 V129 UPDATE；重启 oa-server 时 Flyway 会跳过已标记行（`updater <> 'v129-seed-dashboard-rolling'`）。

---

## S4-fix6（2026-07-03）— 列表页搜索区暗色模式

### 根因

`TableSearch.vue` / `ContentWrap.vue` / `Pagination.vue` 的 **scoped** 样式仍硬编码 `#fff` 背景，优先级高于 `ops-theme.scss` 全局规则；且 `ops-theme.scss` 未在运行时 import，全局 fallback 未生效。

### 修复

| 路径 | 处理 |
|------|------|
| `components/ops/TableSearch.vue` | 背景/阴影/分隔线/标签色 → `var(--el-bg-color)`、`var(--el-border-color-lighter)`、`var(--el-text-color-regular)`；移除硬编码 primary 按钮色 |
| `components/ops/ContentWrap.vue` | 卡片背景/边框/标题 → Element Plus 主题变量 |
| `components/ops/Pagination.vue` | 分页条背景/文字/激活色 → Element Plus 主题变量 |
| `styles/ops-theme.scss` | `.ops-page :deep(.table-search/.content-wrap/.pagination-wrap)` 全局 fallback |
| `plugins/ops-element-plus.ts` | 侧效 `import '#/styles/ops-theme.scss'`（bootstrap 未在集成包内时仍加载主题） |

**抽检**：`#/ops/operations/author`、`#/ops/collect/task` — 切换暗色后搜索区应与表格卡片同色，非白底。

---

## S4-fix5（2026-07-03）— 暗色主题全量 + API 客户端加固

### 根因

1. **暗色模式**：`mount-ops-all.py` remount 后 S2-fix8 主题改动被覆盖；79 个 vue 仍含 `#fff` / `#303133` 等硬编码；`ContentWrap` / `TableSearch` / `Pagination` 白底卡片在 `html.dark` 下不切换。
2. **API「无数据」**：Gateway 探针 8/10 通过，后端正常；前端风险点：`client.ts` 仅支持 `{ url, params }` 一种签名、`useAccessStore()` 在 Pinia 未就绪时可能抛错、误写 `/admin-api/oa/**` 双重前缀；verify 脚本采集/字典探针 URL 与后端不一致（非前端 bug）。

### 修复

| 路径 | 处理 |
|------|------|
| `scripts/fix-ops-theme.py` | **新增**：批量替换 22 种硬编码色 → `var(--el-*)`；selector 内联 `style` → `.ops-option-meta`；补 `ops-page` |
| `styles/ops-theme.scss` | 扩展 `.ops-page` `:deep()` 覆盖 el-card / el-table / el-tabs / el-descriptions；共用 wrapper 类 |
| `components/ops/ContentWrap.vue` | 卡片背景/边框/标题改主题变量 |
| `components/ops/TableSearch.vue` | 搜索区背景/标签色改主题变量；移除硬编码 primary 按钮色 |
| `components/ops/Pagination.vue` | 分页区背景/文字改主题变量 |
| `api/ops/client.ts` | `request`/`opsRequest` **双签名**（`get({url,params})` 与 `get(url,params)`）；`normalizeOpsUrl` 防双重 `/admin-api`；Pinia try/catch 回退 `X-Tenant-Id: 1` |
| `scripts/mount-ops-all.py` | 扩展 `COLOR_REWRITES`（与 fix-ops-theme 对齐） |
| `scripts/verify-ops-pages.py` | 采集探针改 `/oa/collect/task/page`；字典探针改 `/oa/dict/data?type=dict_ip_group_level` |

### 验证（2026-07-03）

| 检查项 | 结果 |
|--------|------|
| 暗色主题批量修复 | **80** 文件 / **166** 处替换；剩余 **18** 文件（ECharts JS 配置 / 品牌渐变色 / Layout 侧栏，需运行时 chart 主题） |
| Vue 结构 | **103/103** PASS |
| Gateway API 模块探针 | **10/10** PASS（fix 探针 URL 后） |
| 服务状态 | Gateway **:48080** UP · oa-server **:48094** UP · Vite **:5777** UP |

**复跑**：

```powershell
python scripts/fix-ops-theme.py      # remount 后批量主题修复
python scripts/verify-ops-pages.py --api
```

**暗色抽检**：`#/ops/operations/ip-group`、`#/ops/production/plan`、`#/ops/production/content/review` — 搜索区/卡片/树面板/表格应随 header 主题切换。

---

## S4-fix4（2026-07-03）— 运行时白屏 / `parentNode` 导航错误

### 根因（链式）

1. **`Pagination.vue` setup 崩溃**：`computed` 未从 `vue` 导入 → `ReferenceError: computed is not defined`（`use-navigation.ts:56` 的 `router.push` 仅触发导航，真正错误在子组件 setup）。
2. **Element Plus 组件未全局注册**：Ops 原站 `app.use(ElementPlus)` 全量注册；Football 仅 `unplugin-element-plus` 按需样式 + 原生页显式 `import`。迁入页模板内 `<el-drawer>` / `<el-descriptions>` / `<el-pagination>` 等无法解析。
3. **级联 DOM 错误**：`LayoutContent` 的 `Transition mode="out-in"` + `KeepAlive` 在子组件 setup 失败后继续 patch → `TypeError: Cannot read properties of null (reading 'parentNode')`，页面空白。

`[intlify] Not found 'OAuth 2.0'` 为基础设施菜单标题缺 key，**不阻塞**导航。

### 修复

| 路径 | 处理 |
|------|------|
| `components/ops/Pagination.vue` | 补 `import { computed } from 'vue'`（**2026-07-04 remount 会覆盖，须同步源仓**） |
| `ops-platform-ui-vue/src/components/Pagination.vue` | 同上，作为 `mount-ops-all.py` 拷贝源 |
| `plugins/ops-element-plus.ts` | **新增**：`app.use(ElementPlus, { locale })` 一次注册，供 Ops 模板 kebab-case 组件 |
| `bootstrap.ts` | `app.use(opsElementPlusPlugin)`（在 `createApp` 后、路由前） |
| `views/ops/content/IPThemeData.vue` | 去除模板首行 UTF-8 BOM |
| `scripts/scan-ops-vue-imports.py` | **新增**：扫描 Ops 组件缺失 Vue API 导入 |

### 验证（2026-07-03，:5783 重启 Vite 后）

| 路由 | 结果 |
|------|------|
| `/ops/production/content/review` | PASS — 面包屑「运营数据 › 内容生产 › 内容审核」，无 `parentNode` / Pagination 错误 |
| `/ops/operations/ip-group` | PASS |
| `/ops/production/plan` | PASS |
| `verify-ops-vite-modules.py` | **90/90** PASS |

**注意**：修改 `bootstrap.ts` / `plugins/*` 后须 **重启 Vite**（HMR 不会重跑 `createApp`）。

---

## S4-fix3（2026-07-03）— 模板标签 / API 客户端 / 运行时依赖

### 根因

`mount-ops-all.py` 的 `ensure_ops_page()` 在添加 `ops-page` class 时误删根节点 `<template>`（101/103 个 `.vue` 仅剩 `</template>`），Vite 编译失败、页面白屏。

### 修复

| 项 | 处理 |
|----|------|
| **101 vue 文件** | `scripts/fix-ops-templates.py` 恢复根 `<template>` |
| **mount-ops-all.py** | `ensure_ops_page` 仅改 `<div>`（保留 `<template>`）；新增 `ensure_root_template()` 防回归 |
| **api/ops/client.ts** | legacy `request` wrapper 支持 `timeout` / `params`（与 content.ts 等一致）；`tenantId` 比较改 `String()` |
| **utils/ops/error-handler.ts** | `import { router } from '#/router'`（Football 无 default export） |
| **package.json** | 声明 `echarts`、`vue-echarts`、`xlsx`（Ops 图表/导出） |
| **运行时依赖** | `scripts/link-ops-deps.ps1` 从 `ops-platform-ui-vue/node_modules` junction：`echarts`、`vue-echarts`、`xlsx`、`@logicflow`、`@tiptap` |

### 验证脚本（新增）

| 脚本 | 用途 |
|------|------|
| `scripts/verify-ops-pages.py` | Vue 结构 + CSV 组件存在 + 可选 Gateway API 冒烟（`--api`） |
| `scripts/verify-ops-vite-modules.py` | 经 Vite :5777 逐菜单 `.vue` 编译探测 |
| `scripts/scan-ops-vue.py` / `scan-ops-imports.py` | 静态扫描 template / `#/` 导入 |

### E2E 结果（2026-07-03）

| 检查项 | 结果 |
|--------|------|
| Vue 根 `<template>` | **103/103** PASS |
| CSV 菜单组件文件 | **90/90** PASS |
| Vite 模块编译（:5777） | **90/90** PASS |
| Gateway API 模块探针 | **8/10** PASS（采集任务 code=500、字典 403 — 后端/权限，非前端编译） |

**复跑**：

```powershell
.\scripts\link-ops-deps.ps1          # 首次或重装 node_modules 后
python scripts/fix-ops-templates.py  # 若 remount 后需修复
python scripts/verify-ops-pages.py --api
python scripts/verify-ops-vite-modules.py
```

---

## S4-fix2（2026-07-03）— Ops 导入别名

`mount-ops-all.py` 仅重写 `from '@/utils/`（带路径），遗漏 `from '@/utils'` 及 constants/composables/mock。Football web-ele 使用 `#/*` → `./src/*`（`tsconfig.json` + `package.json` imports），**无 `@/` 别名**。

| 原 Ops 路径 | Football 路径 |
|-------------|---------------|
| `@/utils` | `#/utils/ops/index` |
| `@/api/*` | `#/api/ops/*` |
| `@/constants/metricSchema` | `#/constants/ops/metricSchema` |
| `@/composables/useMetricSchemas` | `#/composables/ops/useMetricSchemas` |
| `@/mock/collect` | `#/mock/ops/collect` |

**新增**：`constants/ops/metricSchema.ts`、`composables/ops/useMetricSchemas.ts`、`mock/ops/collect.ts`（自 ops-platform-ui-vue 复制并改导入）。

**批量修改**：**51** 个 `views/ops/**`、`components/ops/**`、`utils/ops/**`、`api/ops/**` 文件；修复 `review.vue` 等 Vite 无法解析的 `@/utils` 导入。

`formatDateTime` 位于 `utils/ops/index.ts`（源：`ops-platform-ui-vue/src/utils/index.ts` L154）。

---

## 变更原则

| 允许 | 禁止 |
|------|------|
| 新增 `views/ops/**`、`api/ops/**`、路由模块 | 修改 `football-module-*` Java 业务逻辑 |
| 配置 / `.env` / Gateway YAML | 改动 Football 既有业务 Vue/TS 逻辑 |
| Nacos 覆盖 YAML（`scripts/integration-config/`） | 修改 `football-backend-saas/pom.xml` modules |

---

## S4（2026-07-03）— 批量挂载

### wd 仓库脚本

| 路径 | 说明 |
|------|------|
| `scripts/mount-ops-all.py` | 从 `oa-menu-permission-map.csv` + `ops-platform-ui-vue` 批量复制 **103** vue、**53** api、**51** 组件、types/utils；生成 **38** 条 hide 路由 |

### football-front — 批量新增（additive）

| 目录 | 规模 |
|------|------|
| `apps/web-ele/src/views/ops/**` | **103** `.vue`（93 CSV 唯一组件 + 辅助面板） |
| `apps/web-ele/src/api/ops/**` | **53** `.ts` |
| `apps/web-ele/src/components/ops/**` | **51** 文件 |
| `apps/web-ele/src/types/ops/**` | **19** `.ts` |
| `apps/web-ele/src/utils/ops/**` | **13** `.ts` |

### football-front — 修改

| 路径 | 说明 |
|------|------|
| `apps/web-ele/src/api/ops/dict.ts` | **S4-fix1**：`opsRequest` → legacy `request` wrapper（修复 DictSelect 调用签名） |
| `apps/web-ele/src/router/routes/modules/ops.ts` | 由空占位 → **38** 条 `hideInMenu` detail/edit 路由（生成自 CSV） |

### 验 URL（hash 模式）

- 内容审核：`http://localhost:5777/#/ops/production/content/review`
- IP组管理：`http://localhost:5777/#/ops/operations/ip-group`
- 计划管理：`http://localhost:5777/#/ops/production/plan`
- 内容编辑（hide）：`http://localhost:5777/#/ops/content/edit`

---

## S2（2026-07-03）— 试点与基建

### football-front — 新增文件

| 路径 | 类型 | 说明 |
|------|------|------|
| `apps/web-ele/src/api/ops/client.ts` | added → modified | Ops API 适配层；复用 `#/api/request`（Bearer + `tenant-id`）；**始终注入 `X-Tenant-Id`**（`accessStore.tenantId ?? 1`；oa-server 必填） |
| `apps/web-ele/src/api/ops/ip-group.ts` | added | M1 IP 组 API（`/oa/ip-group/**`）；S2-fix7 改用 legacy `request` wrapper |
| `apps/web-ele/src/api/ops/dict.ts` | added | OA 字典 API（`/oa/dict/**`，供 DictSelect/DictLabel） |
| `apps/web-ele/src/types/ops/ip-group.ts` | added | IP 组 TypeScript 类型 |
| `apps/web-ele/src/views/ops/operations/IpGroup.vue` | added | M1 IP 组管理页（试点） |
| `apps/web-ele/src/components/ops/DictSelect.vue` | added | 字典下拉（Ops 组件迁入） |
| `apps/web-ele/src/components/ops/DictLabel.vue` | added | 字典标签（Ops 组件迁入） |
| `apps/web-ele/src/components/ops/selectors/IpGroupTreeSelect.vue` | added | IP 组树选择器 |
| `apps/web-ele/src/components/ops/selectors/UserSelect.vue` | added | 用户选择器（OA API） |
| `apps/web-ele/src/components/ops/selectors/AccountSelect.vue` | added | 平台账号选择器 |
| `apps/web-ele/src/components/ops/selectors/selector-utils.ts` | added | 选择器工具函数 |
| `apps/web-ele/src/router/routes/modules/ops.ts` | added | Ops 隐藏/detail 路由模块（占位，随 S4 增量填充） |

### football-front — 修改文件（S2-fix3 / S2-fix4）

| 路径 | 类型 | 说明 |
|------|------|------|
| `apps/web-ele/src/router/access.ts` | modified | **S2-fix4**：`buildOpsPageMap()` 从 `import.meta.glob('../views/ops/**/*.vue')` 自动构建 pageMap（键经 `normalizeViewPath` 与 DB `component` 对齐）；合并进全局 pageMap，S2 批量挂载无需逐条注册 |
| `apps/web-ele/src/views/ops/production/content/review.vue` | added | M2 内容审核页（自 `ops-platform-ui-vue/views/production/content/review.vue` 迁入） |
| `apps/web-ele/src/api/ops/content.ts` | added | 内容审核 API（list / detail / review / review-config） |
| `apps/web-ele/src/types/ops/content.ts` | added | 内容审核最小类型 |
| `apps/web-ele/src/utils/ops/format.ts` | added | `formatDateTime` 工具 |
| `apps/web-ele/src/components/ops/TableSearch.vue` | added | 列表搜索区（Ops 共用） |
| `apps/web-ele/src/components/ops/ContentWrap.vue` | added | 内容卡片容器（Ops 共用） |
| `apps/web-ele/src/components/ops/Pagination.vue` | added | 分页（Ops 共用） |
| `apps/web-ele/src/components/ops/layout/LayoutViewer.vue` | added | 富版式 HTML 预览（简化版） |

### football-front — S2-fix8 明暗主题（additive）

| 路径 | 类型 | 说明 |
|------|------|------|
| `apps/web-ele/src/styles/ops-theme.scss` | added | Ops 共用主题工具类（`.ops-page`、`.ops-text-secondary` 等），映射 Element Plus `--el-*` 变量 |
| `apps/web-ele/src/bootstrap.ts` | modified | 全局 `import '#/styles/ops-theme.scss'`（在 `@vben/styles/ele` 之后） |
| `apps/web-ele/src/components/ops/ContentWrap.vue` | modified | 卡片背景/边框/标题色改 `var(--el-*)` |
| `apps/web-ele/src/components/ops/TableSearch.vue` | modified | 搜索区背景/边框/表单标签改主题变量 |
| `apps/web-ele/src/components/ops/layout/LayoutViewer.vue` | modified | blockquote 引用块改主题变量 |
| `apps/web-ele/src/components/ops/selectors/AccountSelect.vue` | modified | 下拉 secondary 文案改 `.ops-option-meta` |
| `apps/web-ele/src/components/ops/selectors/UserSelect.vue` | modified | 同上 |
| `apps/web-ele/src/components/ops/selectors/IpGroupTreeSelect.vue` | modified | 「已停用」改 `.ops-status-disabled` |
| `apps/web-ele/src/views/ops/production/content/review.vue` | modified | 根节点加 `.ops-page`；正文色改主题变量 |
| `apps/web-ele/src/views/ops/operations/IpGroup.vue` | modified | 树面板/详情面板/统计色改主题变量；hint 改 `.ops-hint-inline` |

**验**：5777 登录 → 切换 header 主题按钮（light/dark）→ 内容审核 / IP组管理页搜索区、表格卡片、表单 label、树面板背景应随主题切换，暗色下无白底卡片或浅色字不可读。

### ops-platform-server — S2-fix7 鉴权桥接（非 Football 仓库，集成环境需重启 oa-server）

| 路径 | 类型 | 说明 |
|------|------|------|
| `ops-platform-module-oa/.../FootballOAuth2TokenMapper.java` | added | 查 `system_oauth2_access_token` + `system_users` |
| `ops-platform-module-oa/.../FootballAuthProvider.java` | added | Football 登录 token → `LoginUser`（权限来自 `system_menu.permission`） |
| `ops-platform-module-oa/.../CompositeAuthProvider.java` | added | `@Primary`：dev-token 优先，Football token 兜底 |

### football-front — 配置（S0/S1 已有 + 确认）

| 路径 | 类型 | 说明 |
|------|------|------|
| `apps/web-ele/.env.development` | modified (config) | `VITE_BASE_URL=http://localhost:48080`；`VITE_GLOB_API_URL=/admin-api` |

### football-backend-saas — 配置（S1-A 已有）

| 路径 | 类型 | 说明 |
|------|------|------|
| `football-gateway/src/main/resources/application.yaml` | modified (config) | 新增 `oa-admin-api` 路由 `Path=/admin-api/oa/**` → `grayLb://oa-server`；knife4j 聚合 `oa-server` |

### wd 仓库脚本 / 数据（非 Football 仓库内，合并时执行）

| 路径 | 类型 | 说明 |
|------|------|------|
| `scripts/extract-oa-menu.py` | added | 从 Layout.vue + router 提取菜单 CSV + SQL |
| `scripts/extract-oa-menu.ps1` | added | PowerShell 入口 |
| `scripts/mount-ops-pilot.py` | added | M1 试点文件复制（已被 `mount-ops-all.py`  supersede） |
| `scripts/mount-ops-all.py` | added | S4 全量 Ops 页面/API/组件批量挂载 |
| `scripts/integration-config/seed-oa-system-menu.sql` | added | `system_menu` + `system_role_menu` seed（id 6100–6999） |
| `scripts/integration-config/apply-seed-oa-menu.py` | added | UTF-8 stdin 导入 seed，避免 PowerShell 管道破坏中文 |
| `docs/delivery/oa-menu-permission-map.csv` | added | 96 条路由映射（含 M9 排除标记） |

**Seed 规模**：`system_menu` **66** 行（1 根 + 10 分组目录 + 55 叶子菜单）；`system_role_menu` **56** 行（super admin `role_id=1`）。

**M9 排除**（不写入 seed）：`/system-user`、`/system-role`、`/system-tenant` → 沿用 Football `system:*` 菜单。

---

## S0 / S1 — 集成配置清单（football-backend-saas 侧，配置-only）

> 以下文件位于 **wd** 仓库 `scripts/integration-config/`，通过 Nacos 推送或本地 profile 覆盖 Football 微服务；**不修改 Java 源码**。

| 路径 | 服务 | 说明 |
|------|------|------|
| `gateway-integration-local.yaml` | gateway-server | 本地 Nacos `local` namespace；Redis |
| `gateway-server-local.yaml` | gateway-server | 本地 Gateway 数据源/Redis 片段 |
| `system-server-local.yaml` | system-server | 单库 `101.37.161.136:3306/wd`、Redis password 对齐 |
| `member-server-local.yaml` | member-server | 本地 member 集成配置 |
| `mp-server-local.yaml` | mp-server | 本地 mp 集成配置 |
| `football-integration-overlay.yml` | oa-server / 多服务 | Redis、RocketMQ exclude、IM 桩等 fail-soft 覆盖 |
| `import-football-system-tables.sql` | DB | Football `system_*` DDL+数据导入 `wd` |
| `import-football-system-tenant-patch.sql` | DB | 租户补丁 |
| `import-member-author-minimal.sql` | DB | member 最小桩数据 |
| `patch-system-menu-user-type.sql` | DB | `system_menu.user_type` 列补丁 |

**Gateway oa 路由**（合并到 Football 主仓时需包含）：

```yaml
- id: oa-admin-api
  uri: grayLb://oa-server
  predicates:
    - Path=/admin-api/oa/**
  filters:
    - RewritePath=/admin-api/oa/v3/api-docs, /v3/api-docs
```

---

## 合并步骤建议（Football 团队）

1. 合并 `football-gateway/.../application.yaml` 中 oa 路由 + knife4j 条目。
2. 合并 `football-front` 下 **§S2 新增文件**（保持路径一致）。
3. 在目标库执行 `scripts/integration-config/seed-oa-system-menu.sql`（或 Flyway 包装）。
4. 确认 Nacos / `.env` 指向 Gateway `48080` 与单库 `wd`。
5. 启动 `oa-server`（:48094）+ Gateway + system-server；5777 登录后应看到「运营数据」菜单树。
6. **试点页**：菜单「IP组管理」→ 组件 `ops/operations/IpGroup`（需 oa-server UP）。

---

## 待 S5 切流前验证

- 5777 登录后逐组冒烟（富文本编辑、大屏 fullscreen、Layout 仪表盘等复杂页）
- 运行时 import 缺失依赖逐项修复
- M9 三页（user/role/tenant）仍走 Football 原生菜单，**未**迁入 `views/ops/system/*`

## 变更记录

| 日期 | 阶段 | 摘要 |
|------|------|------|
| 2026-07-02 | S1-A | Gateway oa 路由、`.env.development` Gateway URL |
| 2026-07-03 | S1-B | Nacos 本地集成 YAML、system_* 导入 wd、登录链修复 |
| 2026-07-03 | S2 | 菜单 CSV/SQL、M1 IP组试点挂载、ops 路由模块占位 |
| 2026-07-03 | S2-fix | `api/ops/client.ts` 补 `X-Tenant-Id`（accessStore.tenantId）；Gateway :48080 `/oa/ip-group/tree` 验证 code=0 |
| 2026-07-03 | S2-fix2 | 菜单乱码：`seed-oa-system-menu.sql` 须 Python/`mysql --default-character-set=utf8mb4` 导入（PowerShell pipe → `????`）；新增 `apply-seed-oa-menu.py`。IP组 404：`mount-ops-pilot.py` 补齐 `views/ops/operations/IpGroup.vue` 等；`router/routes/modules/ops.ts` 占位注册；Vite 需重启以刷新 `import.meta.glob` |
| 2026-07-03 | S2-fix3 | **IP组 404 根因**：后端菜单 `component=ops/operations/IpGroup` 与 seed/DB 一致，但 `generateRoutesByBackend` 经 `pageMap`（`import.meta.glob`）解析失败时 fallback 到 `not-found.vue`；新加 Ops 视图在 Vite 未重启时 glob 可能未收录。**修复**：`apps/web-ele/src/router/access.ts` 显式注册 `OPS_PAGE_MAP['/ops/operations/IpGroup.vue']` 静态 import。**预期 URL**：`#/ops/operations/ip-group`（hash 路由；DB path 链：6100 `/ops` → 6109 `operations` → 6157 `ip-group`）。Ops 原站 flat path 为 `/ip-group`，Football 嵌套为 `/ops/operations/ip-group`。验：5777 登录 → 运营数据 → 运营管理 → IP组管理，不应出现 `not-found.vue` |
| 2026-07-03 | S2-fix4 | **内容审核 404 根因**：`system_menu` id **6118** `component=ops/production/content/review` → `normalizeViewPath` → pageMap 键 `/ops/production/content/review.vue`，但 Football 未挂载该 Vue。**修复**：挂载 `views/ops/production/content/review.vue` + `api/ops/content.ts` + 共用组件；`access.ts` 改为 `views/ops/**/*.vue` 自动 pageMap。**DB**：6118 内容审核，path 链 6100 `/ops` → 6102 `production` → `content/review`。**预期 URL**：`http://localhost:5777/#/ops/production/content/review`（hash；无 `#` 的 `/ops/...` 在 hash 模式下会 404）。Ops 原站 router path `/content/review` |
| 2026-07-03 | S2-fix5 | **esbuild 注释误解析**：`access.ts` 块注释含 glob 样例 `**/*.vue` 时 `*/` 提前闭合注释导致 Vite 编译失败；改为 `//` 行注释，`buildOpsPageMap` 的 `import.meta.glob` 不变 |
| 2026-07-03 | S2-fix6 | Ops 页 `@element-plus/icons-vue` 无法解析：`apps/web-ele/package.json` 新增 direct dep（`TableSearch.vue`、`IpGroup.vue` 等迁入页使用 Element Plus 图标） |
| 2026-07-03 | S2-fix7 | **Ops 401 根因（双因素）**：① `oa-server` `DevAuthProvider` 仅查 `sys_user_token`，Football 登录 token 在 `system_oauth2_access_token` → 401 `未登录或 Token 无效` + Vben `登录认证过期`。**修复**：`CompositeAuthProvider` + `FootballAuthProvider`（查 Football OAuth2 表 + `system_menu.permission`）。② `api/ops/client.ts` 原依赖 `isTenantEnable()` 才发 `X-Tenant-Id` → 改为始终发送（`accessStore.tenantId ?? 1`）。③ `ip-group.ts` 误用 `opsRequest` 对象签名 → 改回 legacy `request` wrapper。**验**：`POST /admin-api/system/auth/login` → Bearer + `X-Tenant-Id: 1` → `GET /admin-api/oa/content/review-config` code=0；5777 重新登录后内容审核/IP组页应正常加载 |
| 2026-07-03 | S2-fix8 | **Ops 明暗主题**：迁入组件硬编码 `#fff` / `#303133` 等浅色值，切换 Football 暗色模式后搜索区/卡片/表单标签仍为白底或浅色字。**根因**：Ops 自 `ops-platform-ui-vue` 迁入时使用固定 hex，未接 Vben/Element Plus 主题变量。**修复**：新增 `styles/ops-theme.scss`（`.ops-page`、`.ops-text-secondary` 等工具类 + `var(--el-*)` 映射）；`bootstrap.ts` 全局引入；`ContentWrap` / `TableSearch` / `LayoutViewer` / `IpGroup` / `review` / 选择器组件改为 Element Plus CSS 变量。**不改** Football 业务逻辑或 `@vben/preferences` 主题切换机制 |
| 2026-07-03 | S4 | **批量挂载**：`mount-ops-all.py` → 103 vue / 53 api / 51 组件 / 38 hide 路由。**内容审核 server error 根因**：`dict.ts` 误用 `opsRequest.get({url,params})` 对象签名 → DictSelect 字典 API 失败；改回 `request` wrapper。Gateway oa content + dict **code=0** |
| 2026-07-03 | S2-fix9 | **缺失菜单：首页仪表盘 + 数据大屏**。**根因**：`extract-oa-menu.py` 未解析 Layout 首页 `<template #title>`；`/screen` standalone 误 hide；redirect regex 串匹配。**修复**：脚本增强 → 重跑 CSV/SQL；`system_menu` **6168** `dashboard`/`ops/Dashboard`/`oa:home:view`（parent 6100）；**6131** `screen`/`ops/screen/DataScreenFullscreen`/`oa:screen:view`（parent 6103）；`apply-seed-oa-menu.py` 落库；`ops.ts` hide 路由 35 条（去掉 `/ops/screen` 重复）。**验**：重新登录 → 运营数据 → 首页仪表盘 + 数据分析 → 数据大屏 |
| 2026-07-03 | S4-fix1 | **`/workspace` 404 根因**：Football 原生工作台在 `dashboard.ts` 中作为 `Dashboard` 父级的**嵌套绝对子路由**（`path: '/workspace'`）；`generateAccessible` 去父级 `component` 后挂到 `Root` 时，子路由未正确注册，菜单可点但 router 匹配 `FallbackNotFound`。**Ops 首页**为 `/ops/dashboard`（`ops/Dashboard`），与此无关。**修复**：`dashboard.ts` 将 `Workspace` 改为**顶层**路由（`path: '/workspace'` + 直接 component）；`.env.development` 补 `VITE_ROUTER_HISTORY=hash` 与生产/集成文档一致。**预期 URL**：`http://localhost:5777/#/workspace`（hash 模式）；裸 `/workspace` 会跳转为 `#/workspace` 可渲染但 URL 形如 `/workspace#/workspace`，推荐带 `#` |
| 2026-07-04 | P2b-fe | **订单归因页接 Football 订单只读 API**：`OrderAttribution.vue` 列表改调 `GET /admin-api/oa/football-order/list`（`pay_all_order` SSOT）；新增 `api/ops/football-order.ts`；ROI/导出仍走 `order-attribution/*`（Ops 归因表 hybrid）；探针脚本 `order-attribution` → `football-order/list` |
