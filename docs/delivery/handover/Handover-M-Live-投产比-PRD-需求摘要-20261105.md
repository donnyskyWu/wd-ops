# M-Live 抖音投产比 · PRD 需求摘要（团队测试交接）

| 项 | 值 |
|---|---|
| 模块 | **M-Live** 抖音投产管理 |
| 文档日期 | 2026-11-05 |
| 受众 | 产品、测试、运营、财务 |
| SSOT 来源 | 见 §12；**未在 Spec 写明的规则不在本文推断** |
| 关联 ADR | [ADR-084](../adr/ADR-084-ROI-unify-ops-server.md)（方案 B 运行时）· [ADR-056](../adr/ADR-056-Football用户身份SSOT.md)（用户 SSOT） |

---

## 1. 产品目标

### 1.1 愿景（FR-M-Live 总述）

以**每一场抖音/快手直播**为主线，把账号采购与分配、排期与场次指标、费用申请与钉钉审批、财务确认成本、出纳付款、企微获客与现金营收归因串成可核对、可留痕的投产链；管理者与财务在同一租户内按岗位看到**授权范围内**的经营总览、**原口径投入产出报表**，以及**作者 vs 运营责任**两套 ROI 分析。

### 1.2 交接期核心问题（handoff 01 §2）

| 问题 | 系统应能回答 |
|------|----------------|
| 作者维度 | 所选时间内**有效现金收入**与**已确认唯一成本**是多少，且可穿透到订单/费用 |
| 运营维度 | 责任运营在其**有效责任期间**内承担多少收入与成本（**非因果证明**） |

### 1.3 可观测目标（PRD-M-Live §1.4）

- **闭环**：场次 → 费用 → 审批 → 确认 → 付款状态分离（审批通过 ≠ 财务确认 ≠ 出纳付款）
- **权限**：岗位 + 授权主播/作者范围；列表/详情/导出同范围
- **可审计**：业务写操作与只读操作日志一致
- **分析**：原 17 列报表三比例 + 作者/运营分析（现金回收倍数等），与 M5 聚合 ROI **并存、不互相覆盖**

---

## 2. 角色与权限

### 2.1 岗位与数据范围（PRD-M-Live §2 · DesignNotes / PermissionsGuide 镜像）

| 岗位 / 角色代码 | 主要职责 | 典型数据范围 | 默认入口倾向 |
|-----------------|----------|--------------|--------------|
| ADMIN / OWNER | 全租户配置与经营 | 当前租户全部 | 投产分析 / 治理 |
| ACCOUNT | 采购账号、分配主播 | 账号岗位 + 授权范围 | 直播作业 · 账号 |
| MEDIA | 排期、场次指标、费用草稿、下播 OCR | **仅分配给自己的主播/作者** | 直播作业 · 场次 |
| FINANCE | 成本确认、核销、应付 | 全部主播财务域 | 资金与成本 · 财务 |
| CASHIER | 付款登记、退款凭证 | 已核准应付范围 | 资金与成本 · 出纳 |
| MANAGER | 跨主播经营查看 | 全部主播（写另授） | 投产分析 |
| VIEW_ALL / GROUP_VIEW | 只读经营 | 全部或组内已授权 | 投产分析 / 报表 |
| PROJECT_VIEW | 人事协作 | 经营另授 | 多无投产 Tab |
| HOST | 主播视角 | 授权场次 | 场次（隐藏部分治理 Tab） |

**capabilities SSOT**：`GET /admin-api/ops/roi/bootstrap` 返回 `roles`、`capabilities`（如 `report`、`finance`、`session`、`manage`、`auditRead` 等）；Tab/菜单可见性以前端 + **服务端鉴权**为准。

### 2.2 权限铁律（handoff 01 §3 · 02 §7）

- 查看权、经营责任、财务操作权**分别管理**；能看某作者 ≠ 收入归该登录人。
- **未配置原生范围** → 明确拒绝，**不回退全站数据**（handoff P17）。
- 旧 ROI 的 OWNER/ALL 等标记**不能单独**授予全量权；以 Football 原生身份 + 动作权限为准。
- MEDIA 仅见**已分类、结构完整的抖音直播局部成本**；不展示全成本余额/回收倍数，**无 ROI 导出**（handoff 01 §7）。

### 2.3 用户身份 SSOT（ADR-056）

- 后台用户 ID SSOT = **shenyu-system `system_users.id`**；`LoginUser.userId` 同此。
- 写入运营/领用/责任等字段前须服务端校验；API JSON 中用户 id 用**字符串**传 snowflake。
- 全表 **`tenant_id`** 隔离。

---

## 3. 菜单与路由

### 3.1 Football 菜单（运行时注入 · `roi-tools.ts`）

| 菜单 | 路径 | 组件/说明 |
|------|------|-----------|
| **直播 → 抖音投产管理** | `/ops/roi` | 默认重定向 **`/ops/roi/insight`** |
| 投产分析 | `/ops/roi/insight?tab=` | `RoiInsight.vue` · Tab：`company` / `report` / `author` / `operator` / `session` |
| 直播作业 | `/ops/roi/operations?workbench=` | `index.vue` · 场次、账号等 |
| 资金与成本 | `/ops/roi/finance?workbench=` | `expense` / `finance` / `cashier` / 费用总账等 |
| 治理与权限 | `/ops/roi/governance?workbench=` | `scope` / `audit` / `responsibility` / `permissions` 等 |
| 授权业务数据（独立） | `/ops/authorized-data` | 原生只读边界，**非** ROI bootstrap |

**兼容深链**：`/ops/roi?workbench=<key>` 仍映射到 insight 或域内 Tab（如 `report` → insight `tab=report`，`author-roi` → `tab=author`）。

### 3.2 与 M5 路由隔离（必读）

| 模块 | 菜单 | 路由 | 权限示例 |
|------|------|------|----------|
| **M-Live** | 直播 → 抖音投产管理 | `/ops/roi/**` | bootstrap `capabilities` |
| **M5** | 财务管理 → ROI 分析 | `/ops/finance/roi-analysis` | `oa:roi:list` |

二者**不**自动互授；仅有 M5 菜单不能打开 M-Live（AC-M-Live-P003）。

---

## 4. 模块划分（产品四域 + 测试心智）

> 信息架构 SSOT：[PRD-M-Live-v2-信息架构](../product/PRD-M-Live-抖音投产管理-v2-信息架构.md)。实现上为 **侧栏四域 + 投产分析独立页**，字段/API 不增不减。

### 4.1 投产分析（域 A · `/ops/roi/insight`）

| 能力 | Tab / 入口 | FR | 说明 |
|------|-------------|-----|------|
| **总 ROI** | `tab=company` | FR-M-Live-004 | 作者维度汇总；**独立营收/成本/注册三周期**（高级模式） |
| **投入产出报表** | `tab=report` | FR-M-Live-003 | 三周期筛选；**汇总 / 明细 / 趋势**；原三比例字段 |
| **作者 ROI** | `tab=author` | FR-M-Live-004 | 排行含 **直播场次** 列，可下钻场次列表 |
| **运营责任 ROI** | `tab=operator` | FR-M-Live-004 | 责任区间下钻 |
| **直播场次列表** | `tab=session` | FR-M-Live-002/003 | 按开播日查场次成本/营收 |

筛选链（2026-11-05 已实现）：**作者 → 运营人员 → 账号 → 场次**；选运营后作者/账号/场次限定于责任区间（`responsibility/page`，pageSize 最大 **200**）。

### 4.2 直播作业（域 B · `/ops/roi/operations`）

| 能力 | workbench | FR | 说明 |
|------|-----------|-----|------|
| 新媒体运营（场次） | `session` | FR-M-Live-006 | 排期、指标、附件、**录入直播数据**抽屉 |
| 下播 OCR + AI | 场次表单内 | FR-M-Live-006 · API §9 | `POST /ops/roi/ocr/recognize` → **双列表格核对**后带入 |
| 账号管理 | `account` | FR-M-Live-005 | 采购档案、**平台**（抖音/快手）、**平台账号 ID** 列 |
| 费用申请 | `expense` | FR-M-Live-007 | OA finance 嵌入 |
| 开播 IP / 企微 | `ip` / `wecom` | FR-M-Live-008/009 | P1 |

**平台列（2026-11-05）**：场次列表展示 **平台**（抖音/快手）；账号/主档列表含平台与 **平台账号 ID**；账号名称解析用 `platformAccounts`（`oa_account` DOUYIN/KUAISHOU）。

### 4.3 资金与成本（域 C · `/ops/roi/finance`）

| 能力 | workbench | FR |
|------|-----------|-----|
| 财务工作台 | `finance` | FR-M-Live-007 |
| 出纳工作台 | `cashier` | FR-M-Live-007 |
| 费用总账与来源核对 | `cost-ledger` / author-roi 迁出 | FR-M-Live-004（费用部分） |

状态链：审批通过 → 财务确认 → 出纳付款（DesignNotes §五）。

### 4.4 治理与权限（域 D · `/ops/roi/governance`）

| 能力 | workbench | FR |
|------|-----------|-----|
| 人员与主播范围 | `scope` | FR-M-Live-010 |
| 领用与交接 | `responsibility` | FR-M-Live-004 |
| 操作日志 | `audit` | FR-M-Live-011（只读） |
| 岗位权限说明 | `permissions` | FR-M-Live-012 |
| 新媒体只读委派 | `media-delegations` | handoff 补丁 |

### 4.5 投入产出报表（产品面 · 与 insight/report 及作业内 report Tab 同口径）

- **三独立周期**：营收周期、成本周期、注册周期（Asia/Shanghai，结束日含整天）。
- **汇总行**：营收、成本、投入产出比、成本收益率、现金回收倍数（分母为 0 显示「—」）。
- **明细**：场次级营收/成本（不展开过程指标列）。
- **趋势**：按营收周期逐日调 `GET /ops/roi/report` 聚合（不改订单归属规则）。

---

## 5. 数据 SSOT

| 数据域 | 权威来源 | 用途 |
|--------|----------|------|
| **平台账号** | `oa_account`（`platform_type` DOUYIN/KUAISHOU，`external_account_id`） | 采购主档、平台 ID、与 ROI 账号 `masterAccountId` 关联 |
| **ROI 主档** | `shenyu-ops.roi_record`（kind=`account`/`session`/`expense`/…） | 场次、费用草稿、JSON payload |
| **现金订单** | `shenyu-pay.pay_all_order` | 收入主线；`author_id` 为作者维度 SSOT |
| **用户/作者/关注** | `shenyu-member.*` | 注册、新老客、关注解释 |
| **账号费用** | `oa_account_cost` + 审批镜像 | 手工/已确认来源 |
| **责任与领用** | `roi_author_operator_assignment`、`roi_account_*_assignment` | 运营责任区间 `[startsAt, endsAt)` |
| **费用去重/归属** | `roi_cost_source`、`roi_cost_attribution`、`roi_cost_classification` | 唯一成本、直播分类 |
| **后台用户** | `shenyu-system.system_users` | ADR-056；非 wd master 新写 |

**场次订单归因（2026-11-05 实现）**：同作者订单按支付时间落入 `[本场开播, min(下一场开播, 本场开播+12小时))`；跨账号重叠时归**离开播更近**的一场；等距则不归入任一场（`RoiSessionOrderAttribution`）。

---

## 6. 指标与口径摘要

### 6.1 投入产出报表（FR-M-Live-003 · DesignNotes §七）

| 指标 | 公式 | 分母为 0 |
|------|------|----------|
| 投入产出比 | (营收 − 成本) ÷ 营收 | — |
| 成本收益率 | (营收 − 成本) ÷ 成本 | — |
| 现金回收倍数 | 营收 ÷ 成本 | — |

### 6.2 作者 / 运营分析（handoff 01 §5 · FR-M-Live-004）

| 指标 | 说明 |
|------|------|
| 有效现金收入 | 租户内、未删、支付成功、**未退款**、微信/支付宝、实付>0；按 **pay_time** |
| 已确认唯一成本 | 有来源且已确认；按费用发生日；预支与付款流水不重复计 |
| 费用后余额 | 收入 − 确认成本（**非**公司净利润） |
| 现金回收倍数 | 收入 ÷ 确认成本；零成本/待核对/局部成本 → **—** |
| 直播场次 | 计入统计的场次数；可下钻列表（与 `revenueStart/End` 一致） |

**退款（handoff 02 §3）**：查询排除 `is_refund=1` 的整笔订单；**无**按退款发生日冲减/部分退款分摊；事后退款会导致历史区间重查结果变化。

---

## 7. 方案 B 运行时（ADR-084）

| 项 | 约定 |
|----|------|
| **运行时 SSOT** | ROI / OA 桥接 / approval-costs 与 OPS 同属 **`ops-server` (:48094)** |
| **Gateway** | 浏览器 → **:48080** → `grayLb://ops-server`；前缀 `/admin-api/ops/roi/**` 等 |
| **废弃** | 本地 **:48180** selfhost ROI 独立进程、gateway-roi-selfhost 优先路由（非 Gate 必需） |
| **冒烟** | Token 有效时 `GET bootstrap`、`GET report`、`POST ocr/recognize` 经 **48080→48094** 非 500 |

本地一键：`scripts/start-ops-dev.ps1`（见文末测试环境）。

---

## 8. 与 M5 财务管理边界

| 维度 | M-Live | M5 |
|------|--------|-----|
| 入口 | `/ops/roi` | `/ops/finance/account-cost`、`/ops/finance/roi-analysis` |
| 主线 | 直播场次、OA 闭环、作者/运营责任 | `oa_account` 台账、IP 组/账号/人员聚合 |
| ROI 公式 | 原报表三比例 + 分析域现金回收倍数 | `SUM(营收)/SUM(购买成本+过程成本)`（TESTCASES-M5-002） |
| API | `/admin-api/ops/roi/**` | `/admin-api/ops/finance/**` |

**禁止**：在 M-Live 页面宣称 M5 分母公式；在 M5 页面宣称包含钉钉清款/场次级状态。

---

## 9. Out of Scope

1. M5 账号成本台账 CRUD、M5 页内公式替代本模块（PRD §3.2）
2. M10 采集、外部 SSO、独立登录页（Phase Gate 全局）
3. 操作日志增删改（只读 GET）
4. 自动创建兼职 MEDIA 后台账号
5. 人事 OA 主流程（`/ops/hr-oa`）
6. 单账号/单场精确营收排行、完整因果获客（handoff 01 §2、§6）
7. 按退款发生日冲减、部分退款分摊（handoff 02 §3 · v2 §6）
8. 在生产环境试写角色/财务完成验收（handoff 01 §9）
9. v2 提案的**全部**独立子路由（如 `/ops/roi/analytics/report`）若未落地，仍以当前 insight/operations 为准（v2 为 IA SSOT，非全部已拆分部署）

---

## 10. FR / AC 索引（测试追溯）

| FR | 名称 | 优先级 |
|----|------|--------|
| FR-M-Live-001 | 工作台壳、bootstrap | P0 |
| FR-M-Live-002 | 经营总览 / 场次视图 | P0 |
| FR-M-Live-003 | 投入产出报表（三周期） | P0 |
| FR-M-Live-004 | 作者/运营 ROI、责任、费用核对 | P0 |
| FR-M-Live-005～012 | 账号、场次、OA、IP、企微、权限、审计 | P0/P1/P2 |

**页级 AC（节选）**：AC-M-Live-P001 无权限空态 · AC-M-Live-P002 workbench 深链 · AC-M-Live-P003 M5 隔离 · AC-M-Live-004-1～3 分析三周期与下钻。

**交接验收表**：handoff 01 §8 **P01–P18**（与模块 TESTCASES 映射见《功能测试用例》文档）。

---

## 11. 与现有 PRD 文档的差异说明（本文合并策略）

| 处理 | 说明 |
|------|------|
| **合并** | `PRD-M-Live-抖音投产管理.md`（FR/API 壳）+ `PRD-M-Live-v2-信息架构.md`（四域导航）+ `handoff/roi-20261030/01、02`（核算与接口 SSOT）+ **ADR-084**（运行时）+ **2026-11-05 已交付** insight/场次/OCR/平台列/12h 归因（代码与注释 v20261105） |
| **省略** | 完整 API 请求体字段表（仍以 v1 PRD §9 + handoff 02 §6 为准）；UX 线框全文；旧 xht 分支提交号与 SSH 发布细节；MASTER-EXECUTION-TRACKER **无 M-Live 专章**（未单独 Gate） |
| **未新建 Spec** | 仓库尚无 `CHECKLIST-M-Live` / `TESTCASES-M-Live`（v1 PRD 脚注为后续 Slice）；M1/M3/M5 的 CHECKLIST/TESTCASES 仅 **M5 ROI 边界** 被本文引用 |
| **冲突优先级** | 指标与核算：**handoff 01/02** > v1 PRD；导航：**v2 IA** > 单页 14 Tab 旧描述；运行时：**ADR-084** > handoff 02 §8 中 selfhost 主承载描述 |

---

## 12. 引用文档

- [PRD-M-Live-抖音投产管理.md](../../product/PRD-M-Live-抖音投产管理.md)
- [PRD-M-Live-抖音投产管理-v2-信息架构.md](../../product/PRD-M-Live-抖音投产管理-v2-信息架构.md)
- [handoff/roi-20261030/01-产品PRD.md](../../handoff/roi-20261030/01-产品PRD.md)
- [handoff/roi-20261030/02-技术逻辑开发方案.md](../../handoff/roi-20261030/02-技术逻辑开发方案.md)
- [handoff/roi-20261030/03-页面UIUX规范.md](../../handoff/roi-20261030/03-页面UIUX规范.md)
- [ADR-084-ROI-unify-ops-server.md](../../adr/ADR-084-ROI-unify-ops-server.md)
- [ADR-056-Football用户身份SSOT.md](../../adr/ADR-056-Football用户身份SSOT.md)
- [OPS-MENU-ROUTE-INDEX.md](../OPS-MENU-ROUTE-INDEX.md)
- [OPS-DEV-DEPLOY-GUIDE.md](../OPS-DEV-DEPLOY-GUIDE.md)

---

## 测试环境

| 项 | 值 |
|----|-----|
| 启动 | 仓库根目录 `.\scripts\start-ops-dev.ps1`（日常可加 `-NoRestart`；大改 `-FirstRun`） |
| Gateway | **http://localhost:48080**（`/admin-api` 前缀） |
| ops-server | **:48094**（ROI 域 Bean，ADR-084） |
| 前端 | **http://localhost:5777**（`pnpm dev:ele` · Hash `#/ops/...`） |
| 登录 | 账号 **admin** / **admin123**，租户 **1**；请求头 **X-Tenant-Id: 1**（与 Gate 联调一致） |
| OCR 依赖 | **配置管理 → AI 模型**：须配置可用 Vision 模型；未配置时报错「AI 模型 API 地址未配置…」（`RoiOcrAiRecognizer`） |
| 勿依赖 | **:48180** selfhost ROI（已 deprecated，非方案 B 路径） |

---

*本文档为测试交接摘要，不触发部署、人员授权或真实财务操作。*
