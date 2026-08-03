# OPS × Football 终态缺口执行计划（四标准审计跟进）

| 字段 | 值 |
|------|---|
| 文档性质 | **执行计划**（一片一会话；**不**含本会话实现） |
| 日期 | **2026-07-31** |
| 触发 | 四标准终态审计（Std1–4；Std2 达标仅记命名差） |
| 决策 / 进度 SSOT | [ADR-058](../adr/ADR-058-OPS后端单仓与football-module-ops命名.md) · [WORK-PLAN](./OPS-FOOTBALL-MERGE-WORK-PLAN.md) · [GAP-INVENTORY](./e2e-artifacts/P5-MIGRATE-8-cutover/GAP-INVENTORY.md) |
| 关联 | ADR-047 · ADR-049 · ADR-050-REV1 · ADR-056 · ADR-057 · [ADR-059](../adr/ADR-059-G-MATCH-01-external-proxy.md) · [ADR-060](../adr/ADR-060-Phase2-stub-OOS-Accept.md) · [MUST-HAVE](./OPS-FOOTBALL-RPC-MUST-HAVE.md) · [CLEANUP](./OPS-FOOTBALL-MERGE-CLEANUP-INVENTORY.md) · [FULL-MERGE G-MATCH-01](./OPS-FOOTBALL-FULL-MERGE-RPC-ANALYSIS.md) |
| 状态 | **P-A ✅ · P-B ✅ · P-C ✅ · P-D ✅ · P-E ✅ · P-F ✅ · P-G ✅ 2026-07-31**；**§3.0 本地库名=`shenyu-ops` ✅ · ADR-060 stub Accept ✅ 2026-08-01** |

> **读法**：本文只排「审计未达标项如何分片做完」。**禁止**在 Spec / ADR 未写明处发明新 API、字段或页面；沉默项记入阻塞 / 产品确认，不得用惯例补全。

---

## 1. 目标与原则

### 1.1 终态目标（对齐已有 ADR，不新增）

| # | 目标 | SSOT |
|---|------|------|
| T1 | OPS 后端在 `football-backend-saas` 内以 `football-module-ops` 承载；服务名 `ops-server` | ADR-058 D1/D3 |
| T2 | OPS 数据源 **仅** master→OPS 库（本地与 Beta 物理名均为 **`shenyu-ops`**；备份 `football-ops`/`wd`）；跨域只 Feign | ADR-058 D2 · MUST-HAVE §1 |
| T3 | HTTP 规范前缀 **`/admin-api/ops/**`**；Controller 与 Gateway 一致，无长期 Rewrite | ADR-058 D4 · P4 已关对外 `/oa`，对内仍 Rewrite |
| T4 | 权限码终态可选 `ops:*`；**独立 Slice**（勿与路径切流同会话） | ADR-058 D5 · WORK-PLAN P6 |
| T5 | 用户身份 SSOT = shenyu-system；RBAC 读路径纯 Feign；过渡 Mapper 可退 | ADR-056 · CLEANUP §1.2 |
| T6 | Cutover 后 stub 域：要么按既有 Spec 真迁，要么产品书面接受永久 stub | GAP-INVENTORY · ADR-058 §4.3 |

### 1.2 审计对照（2026-07-31）

| # | 标准 | 状态 | 缺口摘要（本计划范围） |
|---|------|------|------------------------|
| **1** | Code merged into football FE/BE | **部分达标** | 业务已在 monorepo；生产包名已 `football.module.ops`（**P-B ✅**）；权限已 `ops:*`（**P-D ✅**）；`legacy-archive/` 已删（**P-G ✅**，仅 git 历史）；余 stub = **ADR-060 Closed-Accept / Phase 2**（非开放 guilt） |
| **2** | Only wd/ops DB | **达标** | 无代码债；**本地物理库 SSOT = `shenyu-ops`**（2026-08-01 自 `football-ops` 复制；`football-ops`/`wd` 留备份）；与 Beta 同名 |
| **3** | RPC/WebAPI to other football services | **部分达标** | 核心 G-* 已 Feign；RBAC overlay **已退**（**P-E ✅**，`FootballOAuth2MasterTokenMapper` 已删）；Match 外部代理 **已接受终态**（**P-F / ADR-059**）；Phase 2 stub 域不做 Feign 化（ADR-060） |
| **4** | Routes consistent with ops | **达标** | 对外+Controller `/admin-api/ops/**` 已对齐、Gateway **无** Rewrite（**P-C ✅**）；权限已 `ops:*`（**P-D ✅**） |

### 1.3 执行铁律

1. **一片一会话**；禁止单会话混做「stub 真迁 + 路径改名 + 权限码」。
2. **Spec 驱动**：未写明的 Match Feign / 包改名时点 / 永久 stub 范围 → **停**，产品确认后写 ADR 再做。
3. **先可逆后不可逆**：路径双映射窗口 → 切流 → 去 Rewrite；`legacy-archive` 先确认无依赖再删。
4. **P6 独立**：`oa:*`→`ops:*` 不得与 P-C（路径）同 Slice（ADR-058 D5）。

---

## 2. 缺口明细（In Scope）

### 2.1 Std1 — 合并完整度

| ID | 缺口 | 依据 | 对应切片 |
|----|------|------|----------|
| G1-PKG | ~~Java 包仍 `cn.iocoder.yudao.module.oa.**`~~ → 生产已 `football.module.ops.**`；archive 已删 | ADR-058 §2.1 | **P-B ✅** |
| G1-LEG | ~~`legacy-archive/` 未进编译~~ → **已 `git rm -r`（P-G ✅）**；回滚见 git `7e5f1b709` | ADR-058 CLEANUP · GAP-INVENTORY | **P-G ✅** |
| G1-STUB | ~~Dashboard…~~ 已迁；余 M10 collect / Douyin / `/internal/**` stub | [ADR-060](../adr/ADR-060-Phase2-stub-OOS-Accept.md) Closed-Accept | **ADR-060 ✅**（非开放缺口） |
| G1-DUAL | ~~模块名 ops、包/路径段仍 oa~~ → 包/路径/权限均已 ops | WORK-PLAN A-WP1 · ADR-058 | **P-B/P-C/P-D ✅** |

### 2.2 Std2 — 仅 ops/wd 库

| ID | 缺口 | 依据 | 对应切片 |
|----|------|------|----------|
| G2-NAME | ~~本地 `football-ops` vs Beta `shenyu-ops`~~ → 本地已对齐 **`shenyu-ops`** | [OPS-TEST-DB](./OPS-TEST-DB.md)；ADR-058 D2 | **§3.0 ✅ 2026-08-01** |

### 2.3 Std3 — 跨服务 RPC

| ID | 缺口 | 依据 | 对应切片 |
|----|------|------|----------|
| ~~G3-RBAC~~ | ~~`FootballOAuth2MasterTokenMapper` overlay~~ → **已删**；`@opsPerm`→Feign `hasAnyPermissions` | WORK-PLAN §8.6 Q4 · CLEANUP §1.2 | **P-E ✅** |
| ~~G3-MATCH~~ | ~~`MatchController` 外部代理未确认~~ → **Accepted / Closed**（外部 HTTP 代理终态；**不**切 match-server Feign） | [ADR-059](../adr/ADR-059-G-MATCH-01-external-proxy.md) · BLK-M2-004 · ADR-016 §2.7 | **P-F ✅** |
| G3-STUB-FEIGN | 未迁域若真迁，凡属 Football SSOT 的读/写须 Feign（不得新开 `@DS`） | MUST-HAVE §1 · ADR-050-REV1 | 并入 **P-A** 各域 DoD |

### 2.4 Std4 — 路由与 ops 一致

| ID | 缺口 | 依据 | 对应切片 |
|----|------|------|----------|
| G4-CTRL | Controller + Gateway 均为 `/admin-api/ops/**`；无长期 Rewrite | ADR-058 D4 · **P-C ✅ 2026-07-31** | **P-C** ✅ |
| G4-PERM | ~~`@PreAuthorize` / `system_menu.permission` 仍 `oa:*`~~ → 已 `ops:*` | ADR-058 D5 · WORK-PLAN P6 | **P-D ✅** |
| G4-FE | FE API base 已 `/admin-api/ops` | ADR-058 P3 ✅ | **无新工作**（P-C 冒烟回归即可） |

---

## 3. 分阶段切片（一片一会话）

依赖总序（建议）：

```text
§3.0 命名澄清（可选文档）
    │
    ▼
P-A  stub 决策/真迁  ──►  P-G 删 legacy-archive（可晚于 P-B）
    │
    ├──────────────────► P-B 包改名（可与 P-A 后并行，勿与 P-C 混）
    │
    ▼
P-C  Controller /oa→/ops + 去 Gateway Rewrite
    │
    ▼
P-D  权限 oa:*→ops:*（P6；勿与 P-C 同会话）
    │
    ▼
P-E  RBAC 纯 Feign / 退 MasterTokenMapper（ADR-056 全量后）
    │
P-F  Match Feign 或 ADR 接受外部代理（可与 P-A 后并行；Spec 门禁）
```

### 3.0 Std2 命名澄清（本地 = Beta 物理名 · ✅ 2026-08-01）

| 项 | 内容 |
|----|------|
| **本地 SSOT** | OPS master 物理库名 = **`shenyu-ops`**（`ops-server` `application.yaml` · preflight · 本地 seed 脚本默认）。自 `football-ops` mysqldump 复制（此前 `wd`→`football-ops`）；**保留 `football-ops` 与 `wd` 作备份**，未 DROP |
| **Beta** | 仍为 **`shenyu-ops`**（`110.42.49.224`）；本任务**不**改远程 |
| **动作** | JDBC/preflight/seed/OPS-TEST-DB/部署指南 → 本地=`shenyu-ops`；Beta overlay 不变 |
| **DoD** | 本地 JDBC → `shenyu-ops`；抽样表计数与源库一致；非 `-Beta` ops 可连本地库 |
| **证据** | [LOCAL-DB-RENAME-SHENYU-OPS-20260801](./e2e-artifacts/LOCAL-DB-RENAME-SHENYU-OPS-20260801/REPORT.md) |
| **Owner** | 文档 / 运维 |

---

### P-A　剩余 stub 域：真迁 **或** 产品接受永久 stub — ✅ 2026-07-31

| 项 | 内容 |
|----|------|
| **目标** | 关闭 GAP-INVENTORY 中仍 stub 且满足 Spec 的域；或书面接受「永久空 GET / 写 410」 |
| **状态** | **✅ 完成**（用户授权本会话执行 Migrate 批量；证据 [P-A-UNSTUB-20260731](./e2e-artifacts/P-A-UNSTUB-20260731/REPORT.md)） |
| **已迁** | Dashboard · HomeDashboard · DashboardConfig · Funnel · CustomQuery · Report · Monitor · OpsAnchor/OpsStats · Account/Content/Follower Analysis · WechatAnalysis · Metadata · Param · Message |
| **仍 stub（Closed-Accept）** | 见 [ADR-060](../adr/ADR-060-Phase2-stub-OOS-Accept.md)：**OOS Accept** M10 collect/collector-bind/collect configs；**OOS Accept** DouyinFollowers；**Accept stub** `/internal/**`（奥创/M10）；平行 system CRUD 410 |
| **DoD** | ① Migrate 域 Controllers 非 stub；② ops-server 直连冒烟 list code=0（见 RESULTS.json）；③ GAP-INVENTORY 已更新；④ 2026-08-01 stub 表 Closed-Accept |
| **阻塞** | ~~Douyin / internal~~ → ADR-060 关闭；正式 Gate 仍需 Nacos+Gateway（运维项） |
| **Priority** | **P0** |
| **Owner** | OPS 后端 |

**同切片禁止**：包改名（P-B）、路径 `/oa`→`/ops`（P-C）、权限 P6（P-D）。

---

### P-B　包名 `cn.iocoder.yudao.module.oa` → `football.module.ops` — ✅ 2026-07-31

| 项 | 内容 |
|----|------|
| **目标** | 消除双包名；对齐 ADR-058 §2.1「建议」终态 |
| **状态** | **✅ 完成**（用户授权本会话；证据 [P-B-PACKAGE-20260731](./e2e-artifacts/P-B-PACKAGE-20260731/REPORT.md)） |
| **前置** | 产品确认启动时点 → 本会话已批准执行 |
| **范围落地** | 生产源 703 Java：`module.oa`→`football.module.ops`；ops-local `yudao.framework`→`football.module.ops.framework`；`scanBasePackages`/`MapperScan` 单包；**未**改路径/权限；`legacy-archive` 仍旧包 |
| **DoD** | `mvn package -DskipTests` 绿；GW ip-group/content/account/task/football-order + 直连 account/content code=0（RESULTS **7/7**）；生产路径无 `cn.iocoder.yudao.module.oa` |
| **Priority** | P1 |
| **Owner** | OPS 后端 |

**同切片禁止**：Controller 路径改名、Gateway Rewrite 删除、stub 真迁、P6。

---

### P-C　内部路径 `/admin-api/oa` → `/admin-api/ops` + 移除 Gateway Rewrite — ✅ 2026-07-31

| 项 | 内容 |
|----|------|
| **目标** | Controller 与对外规范前缀一致；废除 `RewritePath`→`/admin-api/oa/**` |
| **状态** | **✅ 完成**（全切，无 Controllers 双路由别名） |
| **证据** | [P-C-ROUTE-20260731](./e2e-artifacts/P-C-ROUTE-20260731/REPORT.md) · RESULTS 8/8 |
| **范围落地** | 62 Java 文件路径改名；Gateway jar + local/beta overlay 去 `ops→oa` Rewrite；权限码仍 `oa:*` |
| **DoD** | Gateway `/admin-api/ops/**` code=0；无 Rewrite；`/admin-api/oa/**` 业务 404；直连 `:48094/admin-api/ops/**` code=0 |
| **Priority** | P0（Std4 主缺口） |
| **Owner** | OPS 后端 + Gateway |

**同切片禁止**：`oa:*`→`ops:*`（属 P-D）；包改名（P-B）；大规模 stub 真迁（P-A）。

---

### P-D　权限码 P6：`oa:*` → `ops:*` — ✅ 2026-07-31

| 项 | 内容 |
|----|------|
| **目标** | 菜单/按钮/`@PreAuthorize` 与产品命名一致 |
| **状态** | **✅ 完成**（证据 [P-D-PERM-20260731](./e2e-artifacts/P-D-PERM-20260731/REPORT.md)） |
| **范围落地** | `@PreAuthorize` 已 `ops:*`；Flyway **V166** 改 `system_menu.permission`；本地 OPS master + `shenyu-system` 均为 oa=0/ops=60；seeds 字面量改 `ops:`；`sys_permission` 因 B-WP4 stop-write **未改**（非 PreAuthorize SSOT） |
| **DoD** | 冒烟 **5/5**（GW+直连 account/content + ip-group tree code=0）；无 403；路径仍 `/admin-api/ops/**` |
| **Priority** | P1 |
| **Owner** | OPS 后端 + 运维/Football 菜单 SSOT |

**同切片禁止**：路径改名、包改名、stub 迁、删 Mapper。

---

### P-E　RBAC 纯 Feign / 退役 `FootballOAuth2MasterTokenMapper` — ✅ 2026-07-31

| 项 | 内容 |
|----|------|
| **目标** | 生产路径不再依赖 wd master 上 system_users overlay 读；AdminUser/Permission **纯 Feign** |
| **状态** | **✅ 完成**（证据 [P-E-RBAC-FEIGN-20260731](./e2e-artifacts/P-E-RBAC-FEIGN-20260731/REPORT.md)） |
| **范围落地** | 删 `FootballOAuth2MasterTokenMapper`；`LoginUserAssemblySupport` Feign+Gateway；`@PreAuthorize`→`@opsPerm`→`hasAnyPermissions`；Validator/IpGroup/ContentReview 去 master |
| **DoD** | 冒烟 **7/7**（GW+直连 account/content/ip-group + RPC hasAnyPermissions）；overlay 临时 RENAME 后 **4/4** 无 SQL error；CLEANUP §1.2 勾删 |
| **Priority** | P1 |
| **Owner** | OPS 后端 |

**同切片禁止**：P6 权限改名、路径改名、无关 stub。

---

### P-F　Match：match-server Feign **或** ADR 接受外部代理 — ✅ 2026-07-31（路径 A）

| 项 | 内容 |
|----|------|
| **目标** | 关闭 G-MATCH-01「未确认」状态 |
| **状态** | **✅ 完成（路径 A）** — [ADR-059](../adr/ADR-059-G-MATCH-01-external-proxy.md) |
| **前置** | Spec 现状：BLK-M2-004 / ADR-016 / API-M2 §11 = **外部代理已决**；**无** Spec 要求切 `match-server` Feign |
| **范围落地** | ADR 短注：`MatchProxyService` → 配置基址（默认 `https://h5.shenyu.com/app-api/match`）= **已接受终态**；G-MATCH-01 **Accepted / Closed**；**无代码 / 无 Feign** |
| **运行时证据** | `MatchController` `@RequestMapping("/admin-api/ops/match")` → `MatchProxyService`（Hutool HTTP）；`oa.match.api-base-url` |
| **DoD** | ADR-059 + FULL-MERGE / GAP / WORK-PLAN / 本文件指针 ✅ |
| **Priority** | P2 |
| **Effort** | A：0.2 人日 ✅ |
| **Owner** | 架构 |

**同切片禁止**：发明新 Match RPC；与 P-C/P-D 混做。路径 B 仅当新产品/Spec 明文要求后再开。

---

### P-G　删除或 git-only 保留 `legacy-archive` — ✅ 2026-07-31

| 项 | 内容 |
|----|------|
| **目标** | 仓库不再长期携带未编译巨树；或明确「仅 git 历史」 |
| **状态** | **✅ 完成**（选项 ① `git rm -r`；证据 [P-G-LEGACY-ARCHIVE-20260731](./e2e-artifacts/P-G-LEGACY-ARCHIVE-20260731/REPORT.md)） |
| **范围落地** | `football-module-ops-server/legacy-archive/` 已删；**422** tracked + **158** untracked disk = **580** files；从未在 Maven compile path（默认 `src/main/java`） |
| **回滚** | `git -C football-backend-saas checkout 7e5f1b709 -- football-module-ops/football-module-ops-server/legacy-archive`（CLEANUP 引入 archive 的 commit；仅恢复当时已入库的 422 文件） |
| **DoD** | 目录不存在；策略=仅 git 历史；冒烟 health UP + account list code=0（**3/3**） |
| **Priority** | P2 |
| **Effort** | ✅ 2026-07-31 |
| **Owner** | OPS 工程 |

**同切片禁止**：同时做业务真迁（应已在 P-A 完成）；**未做 P-E**。

---

## 4. 优先级 / 工作量 / Owner 总表

| 切片 | 关闭标准 | Priority | Effort（粗） | Owner | 依赖 |
|------|----------|----------|--------------|-------|------|
| §3.0 库名文档 | Std2 命名澄清 | P3 | 0.1d | 文档 | 无 |
| **P-A** stub 决策/真迁 | Std1 stub · Std3 域 Feign | **P0** | ✅ 2026-07-31 | 后端 | — |
| **P-B** 包改名 | Std1 双包 | P1 | ✅ 2026-07-31 | 后端 | — |
| **P-C** 路径+去 Rewrite | Std4 路由 | **P0** | ✅ 2026-07-31 | 后端+GW | FE 已 ops；P-A 后 |
| **P-D** P6 权限 | Std4 权限 | P1 | ✅ 2026-07-31 | 后端+运维 | **P-C 后** |
| **P-E** RBAC 纯 Feign | Std3 RBAC | P1 | ✅ 2026-07-31 | 后端 | ADR-056 |
| **P-F** Match 决策 | Std3 Match | P2 | ✅ 2026-07-31（路径 A / ADR-059） | 架构 | Spec 门禁 |
| **P-G** 清 archive | Std1 残留 | P2 | ✅ 2026-07-31 | 工程 | P-A（+建议 P-B） |

---

## 5. 同切片禁止清单（防混做）

| 不要在同一会话… | 原因 |
|-----------------|------|
| P-A stub 真迁 + P-C 路径改名 | 回归面叠加；失败难归因 |
| P-C 路径 + P-D 权限码 | ADR-058 D5 明文禁止 |
| P-B 包改名 + P-C 路径 | 巨型机械 diff；回滚困难 |
| P-E 删 MasterTokenMapper + 任意改名 Slice | ADR-056 未全量时属抢跑 |
| P-F 在无 Spec 时新建 Match Feign | Spec 驱动；外部代理已是 BLK-M2-004 已决 |
| P-A 实现 M10 Collect* | Phase 2 Out of Scope |
| 重开 `ops-platform-ui-vue` / mount | A-WP1 已退役；FE SSOT = football-front |

---

## 6. 下一步立即动作（请用户批准一刀）

### P-A ✅ 已完成（2026-07-31）

证据：[P-A-UNSTUB-20260731/REPORT.md](./e2e-artifacts/P-A-UNSTUB-20260731/REPORT.md) · [GAP-INVENTORY](./e2e-artifacts/P5-MIGRATE-8-cutover/GAP-INVENTORY.md)

### P-C ✅ 已完成（2026-07-31）

证据：[P-C-ROUTE-20260731/REPORT.md](./e2e-artifacts/P-C-ROUTE-20260731/REPORT.md)

### P-B ✅ 已完成（2026-07-31）

证据：[P-B-PACKAGE-20260731/REPORT.md](./e2e-artifacts/P-B-PACKAGE-20260731/REPORT.md)

### P-D ✅ 已完成（2026-07-31）

证据：[P-D-PERM-20260731/REPORT.md](./e2e-artifacts/P-D-PERM-20260731/REPORT.md)

### P-F ✅ 已完成（2026-07-31，路径 A）

证据：[ADR-059](../adr/ADR-059-G-MATCH-01-external-proxy.md) · FULL-MERGE G-MATCH-01 Closed-Accept · 运行时 `MatchController` + `MatchProxyService`（`/admin-api/ops/match/**`）

### P-G ✅ 已完成（2026-07-31）

证据：[P-G-LEGACY-ARCHIVE-20260731/REPORT.md](./e2e-artifacts/P-G-LEGACY-ARCHIVE-20260731/REPORT.md) · GAP-INVENTORY 已注

### P-E ✅ 已完成（2026-07-31）

证据：[P-E-RBAC-FEIGN-20260731/REPORT.md](./e2e-artifacts/P-E-RBAC-FEIGN-20260731/REPORT.md)

本计划 P-A…P-G **均已 ✅**。Stub 产品确认见 **ADR-060 ✅**。后续仅运维归档（如物理 DROP 备份库上 `system_users` overlay）或 Phase 2 开 Slice。

### §3.0 / ADR-060 尾巴收口 ✅ 2026-08-01

| 项 | 结果 |
|----|------|
| 本地库名 | **`shenyu-ops`**（自 `football-ops` 复制；备份保留） |
| Stub disposition | M10 collect / Douyin / `/internal/**` → **Closed-Accept**（ADR-060） |
| 未做 | 不发明 M10 采集；不迁奥创-bound `/internal/**` Controllers |

---

## 7. 与 WORK-PLAN / ADR-058 的衔接

| 既有阶段 | 本计划映射 |
|----------|------------|
| ADR-058 P0–P5-MIGRATE-9 / CLEANUP | ✅ 已完成；本文件 P-A…P-G **均 ✅**（含 P-E） |
| WORK-PLAN Phase C 整包 GO | ✅ 不回退；本计划为 **Phase D/E 终态收口** |
| WORK-PLAN §8.9 P5 剩余域 / P6 | 由本文件 P-A / P-D 承接明细 |
| ADR-056 / MasterTokenMapper | 本文件 P-E；不提前 |

指针已回写：[OPS-FOOTBALL-MERGE-WORK-PLAN.md](./OPS-FOOTBALL-MERGE-WORK-PLAN.md) §8.9 / 文末「Phase E」。

---

## 8. 变更记录

| 日期 | 说明 |
|------|------|
| 2026-07-31 | 初稿：四标准审计缺口 → P-A…P-G；推荐首刀 P-A |
| 2026-07-31 | **P-A ✅**：legacy-archive 真迁 Dashboard/Analysis/Funnel/Report/Monitor/OpsAnchor/Metadata/Param/Message/WechatAnalysis；M10/Douyin/`internal/**` Controllers 仍 stub；冒烟见 P-A-UNSTUB-20260731 |
| 2026-07-31 | **P-C ✅**：全部 Controller `/admin-api/oa`→`/ops`；Gateway 去 Rewrite（直挂 ops）；全切无别名；冒烟 8/8 见 P-C-ROUTE-20260731；权限仍 `oa:*`（P-D） |
| 2026-07-31 | **P-B ✅**：生产包 `cn.iocoder.yudao.module.oa`→`football.module.ops`；ops-local framework→`football.module.ops.framework`；`scanBasePackages` 单包；`legacy-archive` 仍旧包；冒烟 **7/7** 见 P-B-PACKAGE-20260731；路径/权限未改 |
| 2026-07-31 | **P-D ✅**：权限 `oa:*`→`ops:*`；`@PreAuthorize` 已 ops；Flyway V166（仅 `system_menu`；`sys_permission` B-WP4 stop-write 跳过）；本地 football-ops + shenyu-system 菜单 oa=0/ops=60；seeds 改 ops；冒烟 **5/5** 见 P-D-PERM-20260731；路径未回归 |
| 2026-07-31 | **P-F ✅（路径 A）**：ADR-059 接受 Match 外部 HTTP 代理终态；G-MATCH-01 Accepted/Closed；无 Feign 重写；指针回写 FULL-MERGE / WORK-PLAN / GAP-INVENTORY / ADR-016 §2.7 |
| 2026-07-31 | **P-G ✅**：`git rm -r` `football-module-ops-server/legacy-archive/`（422 tracked + 158 untracked = 580 files）；未进 Maven classpath；回滚 `checkout 7e5f1b709 -- …/legacy-archive`；冒烟 **3/3** 见 P-G-LEGACY-ARCHIVE-20260731；**未**做 P-E |
| 2026-07-31 | **P-E ✅**：删 `FootballOAuth2MasterTokenMapper`；RBAC→Feign `hasAnyPermissions`/`hasAnyRoles` + Gateway login-user；`@opsPerm` 替换 PreAuthorize；冒烟 **7/7** + no-overlay **4/4** 见 P-E-RBAC-FEIGN-20260731；CLEANUP §1.2 勾删 |
| 2026-08-01 | **§3.0 库名对齐**：本地 OPS master `football-ops`→**`shenyu-ops`**（mysqldump 复制；`football-ops`/`wd` 备份未 DROP）；JDBC/preflight/seed/OPS-TEST-DB/部署指南已改；Beta 远程不变 |
| 2026-08-01 | **ADR-060 stub Accept**：M10 collect/collector-bind/collect configs = OOS Accept；DouyinFollowers = OOS Accept；`/internal/**` = Accept stub（奥创/M10）；GAP-INVENTORY + 本计划 G1-STUB 移出开放缺口 |
