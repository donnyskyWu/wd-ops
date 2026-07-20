# OPS × Football 作者管理合并分析

> **版本**：v1.0 · 2026-07-16  
> **范围**：去掉 OPS 独立「作者管理」入口，统一使用 Football 作者管理；评估改动面、功能缺口与迁移路径  
> **依据**：ADR-051 · ADR-050 · `Football-OPS-重复功能分析与合并建议.md` · 代码库实测 · `oa-menu-permission-map.csv`  
> **状态**：P0/P1 已实施（2026-07-16）— 见下方 §12

---

## 1. 执行摘要

OPS 与 Football 在**数据层已部分合并**（ADR-051 Accepted）：`author_user`（member 库）为作者 SSOT，`oa_author_ext`（wd 库）存 IP 组、作者类型、主推公号等运营扩展；`author_id` 在全链路已语义化为 `author_user.id`（V133）。

**当前重复点**在于 **UI + CRUD API 双轨**：OPS 仍保留 `Author.vue` / `AuthorDashboard.vue` 及 `/admin-api/oa/author/*` 全套接口（含 create/update/delete），与 Football 原生「作者管理」一级菜单（`system_menu.id=118`，子页「作者信息」5071 等）功能重叠。

**推荐策略（Option B+，分阶段）**：

| 阶段 | 动作 |
|------|------|
| **P0** | 隐藏/删除 OPS 菜单 6155 与 `Author.vue`；Football「作者信息」(`author/info`) 承担基础 CRUD |
| **P1** | 保留并瘦身 oa-server **扩展面 API**（`list` 供选择器、`author-ext` 写 IP 组/主推号、dashboard/ops-list）；废弃 `POST/PUT/DELETE /oa/author` 中与 Football 重复的字段写 |
| **P2** | Football 作者详情页嵌入 Ops 扩展 Tab（IP 组、主推号、运营关联）或跳转 OPS 看板；权限 `oa:author:*` 收敛为 `oa:author-ext:*` + Football `author:user:*` |

**规模评估**：**L（Large）** — 后端数据模型已对齐，但涉及跨壳路由、选择器 Facade、扩展字段写入时机、6+ 下游模块回归及 football-front（**不在本仓**）联动。

**最大风险**：Football 新建作者时**不会自动写 `oa_author_ext`**，导致 IP 组过滤、内容 `authorId` 校验、看板/首页指标全面失效。

---

## 2. 双系统清单

### 2.1 OPS 作者管理（待移除/收敛）

#### 前端（`ops-platform-ui-vue`）

| 资产 | 路径 | 说明 |
|------|------|------|
| 作者列表/CRUD | `src/views/operations/Author.vue` | 搜索、表格、新增/编辑/删除、跳转看板；~630 行 |
| 作者看板 | `src/views/operations/AuthorDashboard.vue` | KPI、粉丝/内容趋势、运营→主播 Tab；隐藏路由 |
| API 封装 | `src/api/author.ts` | 8 个 `/oa/author/*` + 4 个 `/oa/ops-anchor/*` |
| 类型 | `src/types/author.ts` | AuthorVO、Dashboard、OpsAnchor 等 |
| Mock | `src/mock/author.ts` | 开发 mock（可删） |
| 路由 | `src/router/index.ts` | `/author`、`/author/:id/dashboard` |
| 下游引用 | `ContentEditPanel.vue`、`IpGroup.vue`、`AiContentDrawer.vue` | 调用 `getAuthorPage` 作作者选择器 |

#### 后端（`ops-platform-module-oa`）

| 资产 | 路径 | 说明 |
|------|------|------|
| Controller | `controller/author/AuthorController.java` | 6 端点：list/create/update/delete/dashboard/ops-list |
| Service | `service/author/AuthorServiceImpl.java` | **已双写** member `author_user` + wd `oa_author_ext` |
| 解析/support | `AuthorResolveSupport.java`、`MemberAuthorReadService.java` | 跨库读、IP 组校验、昵称解析 |
| DO/Mapper | `AuthorUserDO`、`OaAuthorExtDO`、`AuthorUserMapper`、`OaAuthorExtMapper` | `@DS("member")` + wd |
| DTO | `AuthorVO`、`AuthorCreateReq`、`AuthorUpdateReq`、`AuthorDashboardVO`、`OpsUserVO` | |
| 关联 Controller | `controller/operations/OpsAnchorController.java` | `/oa/ops-anchor/*` 运营→主播映射 |
| 测试 | `M1AuthorS04IT`、`M1AuthorOpsListFieldIT`、`M1ContentAuthorIdIT`、`M1OpsAnchorS05IT` | |

#### 数据表（wd / member）

| 表 | 库 | 状态 | 用途 |
|----|-----|------|------|
| `author_user` | shenyu-member | **SSOT** | 昵称、粉丝、分成、推送、状态 |
| `oa_author_ext` | wd | **保留** | PK=`author_user_id`；IP 组、类型、主推公号、Ops 状态 |
| `oa_author` | wd | **已弃用** | ADR-051 停写；S4 DROP |
| `oa_ops_anchor_rel` | wd | **保留** | 运营用户 ↔ 作者（经 `author_user.user_id`） |
| `oa_ip_group_anchor_rel` | wd | **保留** | IP 组 ↔ 作者多对多绑定（与 ext 主 IP 组并存） |

#### 菜单与权限

| ID | 权限 | 路由 | 组件 |
|----|------|------|------|
| **6155** | `oa:author:list` | `/ops/author` | `ops/operations/Author` |
| 隐藏 | `oa:author:list` | `/ops/author/:id/dashboard` | `ops/operations/AuthorDashboard` |
| 快捷入口 | `oa:author:list` | — | `HomeDashboardServiceImpl` 首页「作者管理」 |

Seed 来源：`scripts/integration-config/seed-oa-system-menu.sql`、`V26__m0_home.sql`（权限点 id=18）。

---

### 2.2 Football 作者管理（目标 SSOT UI）

> **说明**：Football 前端源码在 **`football-front`（外部仓）**，本仓仅有 `docs/sql/sys_menu.sql` 菜单定义与 member 库 schema。

#### 菜单树（`system_menu.id=118` 作者管理）

| ID | 名称 | 路径/组件 | 权限前缀 | deleted |
|----|------|-----------|----------|---------|
| 118 | 作者管理（一级） | `/author` | — | 0 |
| 5071 | **作者信息** | `author/info/index` | `author:user:*` | 0 |
| 5072 | 发布的方案 | `/author/article/index` | `author:article:*` | 0 |
| 5084 | 发布的套餐 | `/author/privilegeset/index` | `author:privilege-set:*` | 0 |
| 5085 | 作者粉丝 | `/author/attention/index` | `member:attention:*` | 0 |
| 5086 | 订单记录 | `/author/order/index` | — | 0 |
| 5063 | 作者申请 | `author/apply/index` | `author:apply:*` | 0 |
| 128 | 作者审核 | `/author/audit` | — | 0 |
| 129 | 作者类型 | `/author/type` | — | 0 |
| 5392 | 渠道销售 | `author/channel-sales/index` | `member:author-channel-sales:*` | 0 |
| 125 | 新媒体账号 | `/author/media` | — | 0 |
| … | 战绩/提现/日志等 | 多条 legacy + 新菜单并存 | `author:*` / `member:*` | 部分 deleted |

#### 数据 SSOT

| 表 | 说明 |
|----|------|
| `author_user` | 作者主档（35 行 seed，ID 68028–1000008） |
| `author_user_account` | 作者账户流水 |
| `author_performance` / `author_article*` | 战绩、方案 |
| `author_channel_sales` | 渠道销售 |
| `mp_account.bind_author_id` | 公号绑定作者 |

#### API（member-server，本仓未收录源码）

Mock/集成脚本暴露的路径片段（`mock-member-author-server.py`）：

- `author/all`、`author/simple-list` — 下拉/简单列表
- `getAuthorByMobile`、`updateAuthorLoginInfo` — 登录/手机号

完整 REST 前缀一般为 **`/admin-api/member/author/**`**（经 Gateway 48080），权限 **`author:user:*`** / **`member:*`**。

---

## 3. 功能差距分析

### 3.1 能力对照表

| 能力域 | OPS Author.vue / oa API | Football 作者管理 | 合并处置 |
|--------|-------------------------|-------------------|----------|
| 基础 CRUD（昵称、状态、绑定用户） | ✅ create/update/delete | ✅ 作者信息 5071 | **Football SSOT**；废弃 OPS CRUD UI/API |
| 粉丝/内容/直播统计（列表列） | ✅ 列表聚合展示 | ✅ 作者数据/粉丝/订单 | Football 列表为主；OPS 看板可只读引用 |
| **IP 组关联** | ✅ `oa_author_ext.ip_group_id` | ❌ 无 | **必须保留 ext**；在 Football 详情加 Tab 或 IpGroup 页维护 |
| **主推公号** | ✅ `primary_mp_account_id` | ⚠️ 公号侧 `bind_author_id` 反向 | ext 字段保留；需与 mp 绑定双向一致策略 |
| **作者类型** | ✅ `dict_author_type`（Ops 字典） | ✅ 作者类型菜单 129 | 字典对齐或 Football 类型 SSOT + ext 冗余 |
| **运营→主播映射** | ✅ `oa_ops_anchor_rel` + dashboard Tab | ❌ 无 | **OPS 独有**；保留 API，UI 迁至看板或 IpGroup |
| **作者看板（运营 KPI）** | ✅ dashboard API | ⚠️ 作者数据页偏商业指标 | **保留** `AuthorDashboard.vue` + API |
| IP 组绑定（多对多） | ✅ IpGroup anchors API | ❌ 无 | 保留在 **IpGroup.vue** |
| 方案/套餐/订单/提现 | ❌ | ✅ | Football 独有，OPS 不建设 |
| 作者申请/审核 | ❌ | ✅ | Football 独有 |
| 渠道销售/战绩 | ❌ | ✅ | Football 独有 |
| 内容/计划/任务 authorId | ✅ 消费 `author_user.id` | — | **不改 FK**；选择器需 ext 过滤 |
| 绩效订单归因 | ✅ `oa_order_attribution.author_id` | ✅ pay 侧同 ID 空间 | 已对齐 ADR-051 |
| AI 内容偏好/对话 | ✅ authorId 参数 | — | 继续用 `author_user.id` |

### 3.2 OPS 有、Football 无 — 必须迁移/保留的项

1. **`oa_author_ext` 全字段**（IP 组、类型、主推号、Ops 状态、remark）
2. **`oa_ops_anchor_rel`** 运营人员与作者关系
3. **`oa_ip_group_anchor_rel`** IP 组维度作者绑定
4. **作者运营看板**（粉丝趋势来自 `oa_follower_daily`、内容统计来自 `oa_content`/`oa_production_content`、ROI 归因）
5. **按 IP 组过滤的作者选择器**（内容编辑、计划、任务、AI 内容）

### 3.3 Football 有、OPS 无 — 合并后自然获得

- 作者财务（分成、提现、账户）
- 方案/套餐/公推发布链路
- 作者粉丝、订单、日志
- 作者申请与审核流
- 渠道销售、战绩规则
- 新媒体账号矩阵（`/author/media`）

### 3.4 已对齐项（无需重复建设）

- 作者 ID 空间：`author_user.id`（V131/V133）
- 双写逻辑：`AuthorServiceImpl` 已 member + ext 分离写
- 跨库读：`MemberAuthorReadService` + `@DS("member")`

---

## 4. 下游依赖扫描

### 4.1 前端引用（`getAuthorPage` / `/oa/author`）

| 文件 | 用途 | 合并影响 |
|------|------|----------|
| `Author.vue` | 列表 CRUD | **删除** |
| `AuthorDashboard.vue` | 看板 | **保留**，改「返回」链到 Football 列表 |
| `ContentEditPanel.vue` | 按 IP 组加载作者 | **保留 Facade API** 或改 `AuthorSelect` |
| `IpGroup.vue` | IP 组内作者列表 | 同上 |
| `AiContentDrawer.vue` | authorId 上下文 | 仅 ID，无 API 变更 |
| `api/author.ts` | 统一封装 | 瘦身：去掉 create/update/delete |
| `router/index.ts` | 路由注册 | 删除 `/author` 列表路由 |
| `tests/*` | E2E `/author` | 改测 Football 路由或 ext API |

### 4.2 后端 `authorId` / `AuthorResolveSupport` 引用（约 55 文件）

**核心服务（必须回归）**：

| 模块 | 文件 | 依赖点 |
|------|------|--------|
| M2 内容 | `ProductionContentServiceImpl` | 创建/更新校验 author ∈ IP 组 |
| M2 计划 | `ContentPlanServiceImpl` | 计划作者 |
| M2 任务 | `TaskServiceImpl` | 任务 authorId |
| M2 AI | `AiContentServiceImpl` | 偏好/对话 authorId |
| M1 IP 组 | `IpGroupServiceImpl` | bind/unbind anchor、树统计 |
| M3 绩效 | `OrderAttributionServiceImpl` | 归因 author_id |
| M0 首页 | `HomeDashboardServiceImpl` | 作者数、快捷入口 |
| M1 分析 | `ProductivityReviewServiceImpl` | 人效作者 |
| 数据范围 | `ContentDataScopeSupport` | IP 组数据过滤 |
| Football 订单 | `FootballOrderReadServiceImpl` | pay 订单 author 对齐 |

**结论**：下游**不需要改 author_id 语义**；需要的是 **ext 行存在性** 与 **列表 Facade**。

### 4.3 ID 映射现状

| 问题 | 现状 |
|------|------|
| OPS author ID vs Football ID | **已统一**为 `author_user.id`；`AuthorVO.id` = `authorUserId` |
| 历史 `oa_author.id` | 已弃用，测试数据可 TRUNCATE |
| ext 缺失 | Football 侧新建作者**无 hook** 写 ext → **P0 阻塞** |
| `anchor_user_id` 命名 | DB 列名保留，语义 = `author_user.id`（API 别名 `authorId`） |

---

## 5. 合并策略选项

### Option A：去掉 OPS UI/API，选择器直连 `/member/author`

| 优点 | 缺点 |
|------|------|
| 改动直觉简单 | **违反 ADR-051 D7**（若需改 member-server 加 ipGroupId 过滤） |
| 权限统一 `author:user:*` | Football 列表**无 IP 组维度**，内容/计划选择器无法过滤 |
| | 无法写 `oa_author_ext` |
| | **不可行作为主方案** |

### Option B：保留 `oa_author_ext`，去掉 OPS 作者 CRUD UI（**推荐**）

| 优点 | 缺点 |
|------|------|
| 符合 ADR-051 已定架构 | 需设计 ext 写入入口（Football 详情 Tab / 独立「运营扩展」页） |
| oa-server Facade 可继续服务选择器 | 双菜单权限并存过渡期 |
| Football 零改动（D7） | football-front 嵌入 Tab 在本仓外实施 |

**推荐 API 形态**：

```
保留:
  GET  /oa/author/list              — 选择器 + IP 组过滤（member JOIN ext）
  GET  /oa/author/{id}/dashboard    — 运营看板
  GET  /oa/author/{id}/ops-list     — 运营关联
  CRUD /oa/ops-anchor/*             — 运营→主播

新增/替代:
  GET  /oa/author-ext/{authorUserId}
  PUT  /oa/author-ext/{authorUserId}  — 仅 ipGroupId/authorType/primaryMpAccountId/remark/status

废弃(410):
  POST   /oa/author/create
  PUT    /oa/author/update   — 基础字段改走 Football
  DELETE /oa/author/delete   — 改 Football + ext 逻辑删除
```

### Option C：Football 作者页完全吸收 OPS 扩展（长期）

- football-front `author/info` 详情增加「运营扩展」Tab，调 oa-server ext API
- OPS 仅保留 `AuthorDashboard` 为隐藏 deep-link
- 权限：`author:user:*`（读基础）+ `oa:author-ext:update`（写扩展）

---

## 6. 改动点估算

### 6.1 分类统计

| 类别 | 数量 | 文件/对象 |
|------|------|-----------|
| **前端 — 删除** | 2 | `Author.vue`、`mock/author.ts` |
| **前端 — 修改** | 6 | `author.ts`、`author.ts(types)`、`router/index.ts`、`AuthorDashboard.vue`、`ContentEditPanel.vue`、`IpGroup.vue` |
| **前端 — 测试** | 4 | `p0-modules.spec.ts`、`ux-p0-p1-p2-regression.spec.ts`、`ux-routes.ts`、`uat-browser-gap.spec.ts` |
| **前端 — football-front（仓外）** | 3~5 | 作者信息页 Tab、菜单跳转、权限指令 |
| **后端 — 修改** | 4 | `AuthorController`、`AuthorServiceImpl`、`AuthorService`、新增 `AuthorExtController`（可选） |
| **后端 — 保留不动** | 10 | ResolveSupport、MemberAuthorRead、DO/Mapper、OpsAnchor*、IpGroup anchor |
| **后端 — 测试** | 4 | M1Author*IT、GateS7E2EIT 调整 |
| **DB/Flyway** | 1~2 | 隐藏菜单 6155、`system_role_menu` 清理、可选 ext 回填脚本 |
| **菜单/Seed** | 4 | `seed-oa-system-menu.sql`、`oa-menu-permission-map.csv`、`OPS-MENU-LIST.md`、`V26` 权限点 |
| **文档/ADR** | 3 | 新 ADR-054 或修订 ADR-051、API-M1、UX-M1 |
| **脚本/验收** | 3 | `verify-ops-pages-per-menu.py`、`generate-uat-football-ops-pages.py`、`post-mdb-local-smoke.py` |

**合计（本仓可触达）**：约 **35~40 处**；含 football-front 约 **40~45 处**。

### 6.2 规模等级

| 等级 | 定义 | 本任务 |
|------|------|--------|
| S | <10 文件，无跨模块 | — |
| M | 10~25 文件，单模块 | — |
| **L** | 25~50 文件，跨 M1/M2/M3 + 仓外前端 | **✓ 当前** |
| XL | >50 文件或改 member-server | 若 Option A 强推则升至 XL |

---

## 7. 推荐分阶段方案

### Phase P0 — 菜单与 UI 去重（1~2 天）

- [x] `system_menu` 6155 设为不可见或 deleted；移除 `HomeDashboard` 快捷入口或改链 Football `/author/info`
- [x] 删除 `Author.vue`、路由 `/author`；E2E 改为测 Football 页或 skip
- [x] 文档更新 `OPS-MENU-LIST.md`、`oa-menu-permission-map.csv`

### Phase P1 — API 瘦身 + ext 写入（3~5 天）

- [x] 标记 `POST/PUT/DELETE /oa/author` → 410
- [x] 新增 `PUT /oa/author-ext/{id}`；list/dashboard lazy create ext
- [x] IpGroup bind 时 lazy create ext
- [x] 保留 `GET /oa/author/list` 供选择器

### Phase P2 — Football 壳集成（5~8 天，仓外）

- [ ] `author/info` 详情增加「运营扩展」Tab
- [ ] 看板 deep-link：`/ops/author/:id/dashboard` 从 Football 作者行操作进入
- [ ] 权限：运营角色补 `author:user:query`；组长补 `oa:author-ext:update`

### Phase P3 — 权限收敛与清理（2~3 天）

- [ ] `oa:author:list` → 拆为 `oa:author-ext:query` + `oa:author-dashboard:view`
- [ ] 角色 seed 迁移；删除 V26 冗余权限点 id=18（可选）
- [ ] 更新 ADR / API 契约 / TESTCASES P0

---

## 8. 风险与测试计划

### 8.1 关键风险

| # | 风险 | 严重度 | 缓解 |
|---|------|--------|------|
| R1 | Football 新建作者无 ext → IP 组过滤为空、内容保存 1101 | **高** | P1 必做 ext upsert；IpGroup bind 时自动 create ext |
| R2 | 主推号 `primary_mp_account_id` 与 `mp_account.bind_author_id` 不一致 | 中 | 写 ext 时校验 mp 侧；文档约定 SSOT |
| R3 | 权限双轨：用户无 `author:user:*` 但有 `oa:author:list` | 中 | 角色矩阵一次性补全 |
| R4 | football-front 不在 monorepo，联调滞后 | 中 | 先用 standalone OPS 壳验证 ext API |
| R5 | 作者类型字典双轨（Ops `dict_author_type` vs Football 类型表） | 低 | 短期 ext 继续 Ops 字典；长期 ADR 对齐 |
| R6 | 删除 OPS 作者后 E2E/验收脚本仍访问 `/author` | 低 | 同步改 4 个 spec + verify 脚本 |

### 8.2 回归范围

| 模块 | P0 用例焦点 |
|------|-------------|
| M1 IP 组 | 绑定/解绑作者、树 anchorCount |
| M1 看板 | dashboard KPI、ops-list |
| M2 内容 | ContentEditPanel 作者自动填充、authorId 校验 |
| M2 任务/计划 | authorId 创建 |
| M2 AI 内容 | preference/conversation authorId |
| M3 归因 | author_id 与 pay 订单一致 |
| M0 首页 | 作者数统计、快捷入口 |
| 集成 | Gateway 48080 双前缀 `/oa/author` + `/member/author` |

### 8.3 验证步骤（Integration Stack）

1. Flyway 至最新；member 库存在 `author_user` 行
2. Football 登录 → 作者信息 → 新建作者 → **调用 ext PUT** 写 IP 组
3. OPS 内容编辑 → 选 IP 组 → 作者下拉仅显示该组 ext 作者
4. 打开 `/ops/author/{id}/dashboard` → KPI 非空
5. IpGroup 绑定作者 → `oa_ip_group_anchor_rel` + ext 一致
6. `mvn verify -Dtest=M1Author*,M1ContentAuthorIdIT,M2ContentS13IT`
7. Playwright：移除 `/author` 列表用例；增加 Football 作者页 smoke（若环境可用）

---

## 9. 待决事项（需产品/架构确认）

| # | 问题 | 选项 |
|---|------|------|
| D1 | Football 创建作者后 ext 谁写？ | A) 前端二次调 oa-server B) IpGroup 首次绑定时 lazy create C) 改 member-server（**违反 D7**） |
| D2 | 运营扩展 UI 放哪？ | A) Football 作者详情 Tab B) OPS IpGroup 页 C) 独立「作者运营扩展」隐藏页 |
| D3 | OPS 作者看板是否保留？ | 建议 **保留**（Football 无等价运营 KPI） |
| D4 | `oa:author:list` 权限如何处理？ | 拆分为 ext + dashboard；Football 用 `author:user:*` |
| D5 | 是否物理删除 `AuthorController` create/update/delete？ | 建议先 410 过渡一个 Gate 周期 |
| D6 | 作者类型以哪边为准？ | Ops dict / Football 类型表 / 双写 |

---

## 10. 迁移检查清单

```markdown
### 数据
- [ ] member.author_user 与 wd.oa_author_ext 1:1 对账（生产）
- [ ] 无 orphan ext（author_user 已删）
- [ ] oa_content/o_task author_id 均能在 author_user 找到

### 菜单/权限
- [ ] 6155 不可见
- [ ] 运营角色有 author:user:query/create/update
- [ ] 看板 deep-link 权限 oa:author-dashboard:view

### 前端
- [ ] Author.vue 已移除
- [ ] AuthorDashboard 返回链指向 Football
- [ ] 选择器仍可用（list API）
- [ ] football-front Tab 已上线（若 P2）

### 后端
- [ ] CRUD 重复端点已废弃
- [ ] ext PUT 可用
- [ ] OpsAnchor API 正常

### 测试
- [ ] M1Author* IT 绿
- [ ] M1ContentAuthorIdIT 绿
- [ ] E2E 更新
- [ ] post-mdb-local-smoke author 探针更新
```

---

## 11. 附录：关键文件路径（绝对路径）

**OPS 前端**

- `d:\self\sy\运营数据平台\202606\wd\ops-platform-ui-vue\src\views\operations\Author.vue`
- `d:\self\sy\运营数据平台\202606\wd\ops-platform-ui-vue\src\views\operations\AuthorDashboard.vue`
- `d:\self\sy\运营数据平台\202606\wd\ops-platform-ui-vue\src\api\author.ts`

**OPS 后端**

- `d:\self\sy\运营数据平台\202606\wd\ops-platform-server\ops-platform-module-oa\src\main\java\cn\iocoder\yudao\module\oa\controller\author\AuthorController.java`
- `d:\self\sy\运营数据平台\202606\wd\ops-platform-server\ops-platform-module-oa\src\main\java\cn\iocoder\yudao\module\oa\service\author\AuthorServiceImpl.java`

**ADR / 分析**

- `d:\self\sy\运营数据平台\202606\wd\docs\adr\ADR-051-Ops与Football多库复用-作者域.md`
- `d:\self\sy\运营数据平台\202606\wd\docs\Football-OPS-重复功能分析与合并建议.md`

**Football 菜单 SSOT**

- `d:\self\sy\运营数据平台\202606\wd\docs\sql\sys_menu.sql`（id=118  subtree）
- `d:\self\sy\运营数据平台\202606\wd\docs\sql\shenyu-member.sql`（`author_user`）

---

*文档维护：合并实施前以 ADR-051 为准；实施 Slice 须单列「作者 UI 去重」一切片一会话。*

---

## 12. 实施记录（2026-07-16）

| 项 | 状态 |
|----|------|
| V145 隐藏 menu 6155 | ✅ |
| Author.vue → AuthorRedirect.vue | ✅ |
| AuthorExtController GET/PUT | ✅ |
| CRUD 410 + lazy ext | ✅ |
| P2 Football Tab | ⏳ 仓外 |
