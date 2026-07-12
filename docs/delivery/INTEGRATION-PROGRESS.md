# Football × Ops 集成进度看板

> **最后更新**：2026-07-04（GATE-INT-S0/S1 **✅ 已签** · S3 ⏸ Deferred · UAT **5+37+15 browser E2E PASS** · 4-module acceptance **58/58 PASS** · P2a/P2b ✅）  
> **SSOT**：[ADR-047](../adr/ADR-047-Football-Ops平台集成决策.md) · [INTEGRATION-S0](./INTEGRATION-S0-Football-Ops.md) · [MASTER §18](./MASTER-EXECUTION-TRACKER.md#18-football--ops-平台集成2026-07-02-启动)

### 用户场景（2026-07-04）

| 场景 | 启动 | 鉴权 | 依赖 | 本看板相关待办 |
|------|------|------|------|----------------|
| **Ops standalone only**（当前用户） | `.\scripts\start-ops-standalone.ps1` → UI **:3000** + oa-server dev **:8080** | Dev Token `dev-token-oa-admin` + `X-Tenant-Id: 1` | **无** Gateway / system-server / member mock / Football · profile **`dev`**（远程 `wd`） | 见 **[OPS-STARTUP-MATRIX](./OPS-STARTUP-MATRIX.md)** · §21 Standalone |
| **Football 全栈集成** | `.\scripts\start-integration-all.ps1` | Football 登录 `admin/admin123` → Gateway **:48080** | Nacos · Redis · Gateway · system · member mock **:48087**（Hybrid C）· oa **:48094** · :5777 · **`dev-local-multidb`** | 见 **[OPS-STARTUP-MATRIX](./OPS-STARTUP-MATRIX.md)** · Gate 58/58 + smoke 4/4 |

---

## 1. 总目标

以 **Football**（`football-front` + `football-backend-saas`）为登录 / 菜单 / 鉴权壳层，将 **Ops 业务**（`ops-platform-module-oa`）迁入，统一从 **http://localhost:5777** 访问。

**硬约束**：禁止改 `football-front/`、`football-backend-saas/` Java/Vue **逻辑**；仅配置、数据、脚本。

---

## 2. 已锁定决策（ADR-047）

| # | 决策 |
|---|------|
| 1 | 微服务直连（Nacos + Gateway **48080**） |
| 2 | 单库 **101.37.161.136:3306/wd** | **Superseded locally** — localhost 五库（ADR-050）；远程单库暂保留 |
| 3 | M9 → Football `system-server`；废弃 Ops User/Role/Tenant |
| 4 | 权限前缀保留 **`oa:*`** |
| 5 | IP 组数据范围：OA 侧扩展 `biz-data-permission` |
| 6 | 前端路由：Football hash 默认 |

---

## 3. 阶段进度

| 阶段 | 状态 | 完成项 | 阻塞 | 下一步 |
|------|------|--------|------|--------|
| **S0** 基建与决策 | ✅ **Gate 已签** | ADR-047、INTEGRATION-S0、菜单映射计划、Gateway oa 路由扩展点、环境 smoke 100% | 表归属清单延至 S3 | [GATE-INT-S0](./gates/GATE-INT-S0-报告-20260703.md) ✅ 2026-07-04 |
| **S1-A** oa-server 联通 | ✅ **Gate 已签** | Nacos Discovery、`dev-nacos-local`、:48094、Gateway `oa-admin-api`、启动脚本 | — | [GATE-INT-S1](./gates/GATE-INT-S1-报告-20260703.md) ✅ 2026-07-04 |
| **S1-B** 鉴权与对齐 | ✅ **完成** | Redis/Gateway/namespace 修复、`system_*` 租户库已导入 `wd`、**登录 code=0** | — | `football-module-oa` sibling |
| **S2** 菜单路由 | ✅ **验收完成** | CSV 96 路由；seed **69** menu + **59** role_menu；**5777 全量菜单 58/58**（含首页仪表盘 + 数据大屏） | — | S3 后端 sibling 工程 |
| **S3** 后端模块迁移 | ⏸ **Deferred** | S4 前端 58/58 已满足联调；表归属分析完成 | 双部署策略 + Ops 后端未成熟（`sys_user` 引用、`UserSelect` 债务） | 见 [ADR-049](../adr/ADR-049-Ops与Football数据归属与松耦合集成.md)；`sys_dict_*`/M9 废弃范围确认后再启动 sibling 工程 |
| **S4** 前端页面迁移 | ✅ **批量完成** | `mount-ops-all.py`：**93** 菜单 + **103** vue；vite **90/90**；per-menu **58/58** | 数据质量 stub | 4-module parallel acceptance ✅ **58/58** |
| **S5** 切流 | ⬜ 待开始 | — | — | UAT、下线独立 Ops 入口 |

---

## 4. 服务矩阵（本地集成目标）

| 组件 | 端口 | 当前状态（2026-07-03 P0 smoke） | 说明 |
|------|------|--------------------------------|------|
| **Nacos** | 8848 | ✅ UP | Docker 本地 · HTTP 200 |
| **Redis** | 6379 | ✅ UP | `redis-cli ping` → PONG |
| **Gateway** | 48080 | ✅ UP | oa 路由 `Path=/admin-api/oa/**` |
| **system-server** | 48081 | ✅ UP | 登录 code=0（member 桩） |
| **member mock** | 48087 | ✅ UP | 登录 Feign 桩（非完整 member-server） |
| **member-server** | 48082 | ⬜ 未起 | 完整 member 微服务待 S5 前评估 |
| **oa-server** | 48094 | ✅ UP | 直连 + Gateway oa **code=0** |
| **football-front** | 5777 | ✅ UP | Vite dev；**103** Ops vue 已挂载 · HTTP 200 |

---

## 5. 当前阻塞（已解除项归档）

> **2026-07-03 P0 闭环**：下列历史阻塞均已修复并通过 smoke 复测。

| # | 原阻塞 | 状态 | 修复摘要 |
|---|--------|------|----------|
| 1 | Gateway 找不到 system-server → 503 | ✅ 已解除 | 全栈脚本 + mp-server 依赖 |
| 2 | Nacos namespace 不一致 | ✅ 已解除 | `dev-nacos-local` · `discovery.namespace=local` |
| 3 | 登录 code=500（member :48087 refused） | ✅ 已解除 | member-mock :48087 + Redis/im overlay + `system_menu.user_type` 补丁 |
| 4 | oa-server Flyway V129 checksum | ✅ 已解除（本轮） | `flyway_schema_history` checksum repair |

**仍开放（非 P0）**：部分 OA API stub（数据质量空列表）。member mock / 真服仅 **Football 全栈集成** 相关，standalone 见 §20。

---

## 6. 启动命令速查

```powershell
# 一键全栈（Nacos + Redis + Gateway + system/member + oa + :5777）
.\scripts\start-integration-all.ps1 -SkipBuild
# 重启: .\scripts\start-integration-all.ps1 -Restart -SkipBuild
# 停止: .\scripts\stop-integration-all.ps1

# 仅 Nacos + oa-server
.\scripts\start-integration-stack.ps1
```

**集成 Profile**：Football `local,local-nacos`；oa-server `dev,dev-nacos,dev-nacos-local`  
**Gateway 探针**：`http://localhost:48080/admin-api/system/tenant/simple-list`  
**oa 健康**：`http://localhost:48094/actuator/health`

**默认登录账号（Football 惯例）**：`admin` / `admin123`（租户 ID **1**）

---

## 7. 下一步优先级

1. ~~S4 批量挂载~~ **已完成 2026-07-03**（`mount-ops-all.py`）；
2. ~~P0 全栈 smoke + 登录验证~~ **已完成 2026-07-03**（见 §14）；
3. ~~5777 验证「运营数据」各分组菜单页（优先：内容审核、计划管理、账号管理）~~ **UAT 抽检 5/5 PASS**（见 §18）；
4. 复杂页运行时 import 修复（富文本/大屏/Layout 等）；
5. ~~S3 sibling 工程~~ **⏸ 延期** — 优先用户 ID 迁移与订单同步决策（见 §17、[OPS-FOOTBALL-DATA-OWNERSHIP-ANALYSIS](./OPS-FOOTBALL-DATA-OWNERSHIP-ANALYSIS.md)）。

---

## 8. 相关文档

| 文档 | 用途 |
|------|------|
| [ADR-047](../adr/ADR-047-Football-Ops平台集成决策.md) | 集成决策与架构 |
| [INTEGRATION-S0](./INTEGRATION-S0-Football-Ops.md) | S0 Checklist 与环境矩阵 |
| [GATE-INT-S0](./gates/GATE-INT-S0-报告-20260703.md) | S0 Gate 报告（2026-07-03） |
| [GATE-INT-S1](./gates/GATE-INT-S1-报告-20260703.md) | S1 Gate 报告（2026-07-03） |
| [FOOTBALL-PROJECT-CHANGES](./FOOTBALL-PROJECT-CHANGES.md) | Football 主仓合并变更清单 |
| [oa-menu-permission-map.csv](./oa-menu-permission-map.csv) | Ops 路由/权限映射 |
| [MASTER §18](./MASTER-EXECUTION-TRACKER.md#18-football--ops-平台集成2026-07-02-启动) | Gate 与 S1 任务索引 |
| [OPS-FOOTBALL-DATA-OWNERSHIP-ANALYSIS](./OPS-FOOTBALL-DATA-OWNERSHIP-ANALYSIS.md) | 双项目数据归属四问分析（2026-07-04） |
| [UAT-SPOTCHECK-20260704](./UAT-SPOTCHECK-20260704.md) | 优先 Ops 页 UAT 抽检（5/5 PASS） |
| [UAT-SPOTCHECK-EXPANDED-20260704](./UAT-SPOTCHECK-EXPANDED-20260704.md) | 扩展 UAT：数据分析 + 作品监测 + 配置管理（22/22 PASS） |
| [UAT-BROWSER-E2E-20260704](./UAT-BROWSER-E2E-20260704.md) | Standalone 浏览器 E2E：内容生产剩余 + 运营 + 账号管理（15/15 PASS） |
| [UAT-FOOTBALL-E2E-20260704](./UAT-FOOTBALL-E2E-20260704.md) | Football 全栈浏览器 E2E：:5777 登录链 + P0 + 内容/运营/账号（见 §18.4） |
| [OPS-STARTUP-MATRIX](./OPS-STARTUP-MATRIX.md) | **Standalone vs Integration 启动矩阵**（§23 #5 · §2b dev-token） |
| [ADR-049-Ops与Football数据归属与松耦合集成](../adr/ADR-049-Ops与Football数据归属与松耦合集成.md) | S3 延期与表归属决策（Draft） |

---

## 9. S1-B 状态检查（2026-07-03）

**已修（仅配置/数据）**

| 项 | 处理 |
|----|------|
| Redis AUTH 不一致 | 本地 Redis CONFIG SET requirepass 123456；integration-config 与 Nacos local DataId 统一 password |
| Gateway MalformedInputException | UTF-8 无 BOM gateway-server-local.yaml / gateway-integration-local.yaml；Nacos 重发；Gateway integration profile 绕过 jar GBK application-local |
| Nacos auth login 500 | 旧容器 nacos 保留（镜像拉取失败）；客户端去掉 nacos 凭证 |
| oa-server namespace | application-dev-nacos-local.yml discovery.namespace local |

**矩阵**：Nacos/Redis/Gateway/system/oa/5777/member-mock 全 UP。

日志：`scripts/logs/*-integration.log`

**验证（2026-07-03 复测 + P0 闭环）**：48080/48081/48087/48094/5777 端口 LISTEN；Gateway `tenant/simple-list` **HTTP 200 + code=0**；login **code=0 + accessToken**；`ip-group/tree` 经 Gateway **code=0**。

**Football `system_*` DDL/数据导入（租户库阻塞已解除）**：已写入 **`101.37.161.136:3306/wd`**。运行时脚本：`scripts/start-football-system.ps1`、`scripts/start-integration-system.ps1`；Nacos 覆盖配置目录 **`scripts/integration-config/`**（推荐 `scripts/push-integration-config-to-nacos.ps1`）。

**登录 500 修复（2026-07-03）**：根因链 member Feign 不可用 + Redis 密码不一致 + system_menu.user_type 缺列；已执行 `scripts/integration-config/patch-system-menu-user-type.sql`，`football-integration-overlay.yml` 统一 Redis password/im/RocketMQ exclude，登录 `POST /admin-api/system/auth/login` 返回 code=0 + accessToken。

---

**S2 seed 落库**：**是**（2026-07-03 初版 + 补录首页仪表盘 + 数据大屏 → mysql stdin → 101.37.161.136/wd；system_menu 6100–6999 = **69**；system_role_menu = **59**）。

**S2-fix9 缺失菜单（2026-07-03）**：**首页仪表盘**（工作台，`/dashboard` → `/ops/dashboard`）与 **数据大屏**（`/screen` → `/ops/screen`）未出现在 Football 侧栏。**根因**：`extract-oa-menu.py` ① `Layout.vue` 首页项使用 `<template #title>`，旧解析器未识别 `in_layout`；② `/screen` 为 standalone 路由被误标 `hide_in_menu=Y`；③ `/data-screen` redirect 块 regex 串匹配污染 CSV。**修复**：增强 Layout 解析 + standalone 仍保留侧栏项 + 路由块边界 regex；重跑 seed；新增 `system_menu` **6168**（首页仪表盘）、**6131**（数据大屏；**6132** 仍为大屏配置）。

## 10. S2 状态（2026-07-03）

**S2-A 菜单/权限**

| 产出 | 路径 | 规模 |
|------|------|------|
| 路由映射 CSV | `docs/delivery/oa-menu-permission-map.csv` | 96 路由；M9 排除；5 hide-in-menu |
| 提取脚本 | `scripts/extract-oa-menu.py` / `.ps1` | Layout.vue + router/index.ts + Flyway `oa:*` |
| system_menu seed | `scripts/integration-config/seed-oa-system-menu.sql` | **69** menu + **59** role_menu（id 6100+；含 6168 首页仪表盘、6131 数据大屏） |

**S2-B 前端试点（M1 IP组）**

| 项 | 路径 |
|----|------|
| 页面 | `football-front/apps/web-ele/src/views/ops/operations/IpGroup.vue` |
| API 适配 | `src/api/ops/client.ts` + `ip-group.ts` + `dict.ts` |
| 组件 | `src/components/ops/**` |
| 路由模块 | `src/router/routes/modules/ops.ts` |

**合并清单**：`docs/delivery/FOOTBALL-PROJECT-CHANGES.md` · **S4 批量挂载已完成**（见 §11）。

## 11. S4 批量挂载（2026-07-03）

| 产出 | 规模 |
|------|------|
| `scripts/mount-ops-all.py` | 一键复制 views/api/components/types + 生成 hide 路由 |
| `views/ops/**/*.vue` | **103** 文件（CSV 映射 **93** 唯一组件 + 10 辅助页） |
| `api/ops/*.ts` | **53** |
| `components/ops/**` | **51** |
| `types/ops/*.ts` | **19** |
| `utils/ops/*.ts` | **13** |
| `router/routes/modules/ops.ts` | **38** hide-in-menu 路由 |

## 12. S4-fix3 E2E 冒烟（2026-07-03）

| 脚本 | 结果 |
|------|------|
| `scripts/verify-ops-pages.py` | Vue **103/103** · 组件 **90/90** · API **8/10**（login OK） |
| `scripts/verify-ops-vite-modules.py` | Vite 菜单模块 **90/90** PASS |

**已知后端阻塞（非前端编译）**：~~`/admin-api/oa/collect/task/list` code=500；`/admin-api/oa/dict/type/list` HTTP 403~~ → **已修复**（见 §19）。

**复跑**：`.\scripts\link-ops-deps.ps1` → `python scripts/verify-ops-vite-modules.py`

## 13. S2 全量菜单验收（2026-07-03 close-out）

| 脚本 | 结果 |
|------|------|
| `scripts/verify-ops-vite-modules.py` | Vite compile **90/90**（含 hide-in-menu） |
| `scripts/verify-ops-pages-per-menu.py --api` | Visible menus **58/58**（route + vite + theme）；API mapped **13/13** |

**本轮修复**

1. `@/` → `#/` ops/：`api/ops/file.ts`、`plan.ts`、`utils/ops/error-handler.ts`；新增 `scripts/fix-ops-imports.py`
2. oa-server 重启（`CollectQualityController`、`FootballAuthProvider`、`PlatformAccountController`、`PerfResultServiceImpl`、`SopReviewServiceImpl`）
3. 漏斗分析 theme：`FunnelAnalysis.vue` 按钮色改 `var(--el-color-primary)`

**报告**：`docs/delivery/ops-acceptance-final-report.json` · `docs/delivery/OPS-PAGE-ACCEPTANCE-PLAN.md` §Final Summary

**残留阻塞**：数据质量后端 stub（空列表）；42 页无 primary API probe 映射（不影响 route/vite/theme PASS）。

---

## 14. P0 闭环（2026-07-03）

### 14.1 全栈 smoke 结果

| 探针 | 期望 | 实测 | 结果 |
|------|------|------|------|
| Nacos :8848 | HTTP 可达 | HTTP 200 | ✅ |
| Redis :6379 | PONG | PONG | ✅ |
| Gateway :48080 | LISTEN | LISTEN | ✅ |
| system-server :48081 | LISTEN | LISTEN | ✅ |
| member mock :48087 | LISTEN | LISTEN | ✅ |
| oa-server :48094 | health UP | `{"status":"UP"}` | ✅ |
| football-front :5777 | HTTP 200 | HTTP 200 | ✅ |
| `GET /admin-api/system/tenant/simple-list` | code=0 | code=0 | ✅ |
| `POST /admin-api/system/auth/login` admin/admin123 tenant-id:1 | code=0 + accessToken | code=0, token 32 chars | ✅ |
| `GET /admin-api/oa/ip-group/tree` Bearer + X-Tenant-Id:1 | code=0 | code=0 | ✅ |

### 14.2 Gate 报告

| Gate | 路径 | 结论 |
|------|------|------|
| GATE-INT-S0 | [gates/GATE-INT-S0-报告-20260703.md](./gates/GATE-INT-S0-报告-20260703.md) | ✅ 已签 2026-07-04 |
| GATE-INT-S1 | [gates/GATE-INT-S1-报告-20260703.md](./gates/GATE-INT-S1-报告-20260703.md) | ✅ 已签 2026-07-04 |

### 14.3 本轮修复（smoke 前）

1. `scripts/start-integration-all.ps1` — 补 `else` 块闭合 `}`（member-mock 分支）
2. Flyway V129 checksum — `UPDATE flyway_schema_history SET checksum=1566747168 WHERE version='129'`

### 14.4 待用户/团队动作

| # | 项 | 说明 |
|---|-----|------|
| 1 | ~~Gate Sign-off~~ | ✅ GATE-INT-S0 / S1 已签 2026-07-04 |
| 2 | ~~MASTER §18 状态~~ | ✅ 已同步 |
| 3 | ~~完整 member-server~~ | ~~S5 前评估替换 :48087 mock~~ → **仅全栈集成 / S5 切流**；**Ops standalone 可忽略**（见 §20） |
| 4 | collect/dict API | ~~非 P0；S4-fix3 已知后端 500/403~~ → **§19 已修复** |

---

## 15. P1 业务走查（2026-07-03）

**Scope**: 运营管理 + 配置管理（gate **13** 页 = 5+8；CSV **14** 行含内部作品分析）+ 全量 API probe 扩展。

| 项 | 结果 |
|----|------|
| P1 业务 API（gateway-login） | **13/13 PASS**（gate）；CSV 模块 **14/14 PASS** |
| `#/ops/dashboard` trend + platform-dist | **PASS** — V129 rolling seed（trend 12 点 / platform-dist 5 项） |
| `#/ops/screen` | **PASS** — Vite 200 + `dashboard-config/list` code=0 |
| API probe 映射 | **58/58**（原 13/55，no-probe gap **0**） |
| 全量 visible menu `--api` | **58/58 PASS** |

**报告**: [OPS-PAGE-ACCEPTANCE-P1](./OPS-PAGE-ACCEPTANCE-P1.md) · `docs/delivery/ops-p1-api-report.json`

**本轮修复**: `FansAnalysis.vue` / `InternalContent.vue` / `Dashboard.vue` / `FunnelAnalysis.vue` theme；`verify-ops-pages-per-menu.py` API_PROBE 13→58；`order-attribution` probe 补 `startDate`/`endDate`。

---

## 16. 4-Module Parallel Acceptance（2026-07-04）

**Scope**: 58 visible menu pages split across 4 parallel batches; each batch probes route + Vite + theme + API (`verify-ops-pages-per-menu.py --api`).

| Batch | Modules | Pages | Result |
|-------|---------|-------|--------|
| **1** | 内容生产 + 作品监测 | 14/14 | **PASS** |
| **2** | 数据分析 + 数据采集 + 系统管理(OA) | 17/17 | **PASS** |
| **3** | 绩效核算 + 财务管理 + 账号管理 | 12/12 | **PASS** |
| **4** | 运营管理 + 配置管理 + 首页 | 15/15 | **PASS** |

**Grand total: 58/58 PASS**

**Batch 4 highlights**: 首页仪表盘 (`6168` → `/ops/dashboard`) + 数据大屏 (`6131` → `/ops/screen`, covered in Batch 2). Initial Batch 4 run **12/15** — theme fails on 粉丝分析 / 内部作品分析 / 首页仪表盘 (Ant Design `#1890ff` overrides); fixed → re-run **15/15**.

**报告**: [OPS-PAGE-ACCEPTANCE-PARALLEL-20260704](./OPS-PAGE-ACCEPTANCE-PARALLEL-20260704.md) · `docs/delivery/ops-acceptance-batch{1,2,3,4}-report.json`

---

## 17. 数据归属四问摘要（2026-07-04）

> 完整分析：[OPS-FOOTBALL-DATA-OWNERSHIP-ANALYSIS](./OPS-FOOTBALL-DATA-OWNERSHIP-ANALYSIS.md) · 已签决策：[ADR-049](../adr/ADR-049-Ops与Football数据归属与松耦合集成.md)

| # | 问题 | 结论 |
|---|------|------|
| **Q1** | Ops 字典与系统参数是否迁移至 Ops 独立维护？ | **是**。`sys_dict_*` = Ops 业务字典 SSOT；`sys_param` = Ops-only；Football 保留 `system_dict_*`（平台/trade/member）与 `infra_config`；禁止合并 schema |
| **Q2** | Ops 独立 UI 入口？ | 保留 `ops-platform-ui-vue`（:3000）→ oa-server dev（:8080）作 **dev/QA harness**；生产仍 5777 + Gateway；S5 再评估下线 |
| **Q3** | `sys_*` 哪些 Ops 独立维护？ | **Ops-only**：`sys_dict_*`、`sys_param`、日志/消息/元数据；**废弃**：`sys_user`/`sys_role`/`sys_tenant`/`sys_permission`；Football 身份 SSOT = `system_*` |
| **Q4** | Ops ↔ Football 业务关联？ | `oa_*` 业务实体 Ops SSOT；订单 **同库只读** `trade_*`/`pay_*`（无 ETL）；粉丝/平台账号/计划/M4 **保持分离**；`UserSelect` → `system_users` 为 **P2a 技术债** |

### 产品确认摘要（2026-07-04）

| 域 | 决策 |
|----|------|
| 用户 | 选择器统一 `system_users`；`oa_author` 独立，FK 迁移非本期 |
| 订单 | 只读 cross-query / Mapper 读 `trade_order`、`pay_order`；Ops 不写、不 ETL |
| 粉丝 | 不合并 `oa_*` vs `member_*` |
| 平台账号 / 计划 / M4 | 保持分离，试用后优化 |
| Ops `sys_*` 页面 | 留在 Ops 壳（`ops/system/*` 6137–6141 + 元数据 6165）；M9 身份页仅 Football |

### 下一步（集成 P2）

| 项 | 动作 | 状态 |
|----|------|------|
| **P2-standalone** | ADR-049 对齐 standalone harness：`ops-platform-ui-vue` :3000 → oa-server dev :8080；侧栏 **仅 OA sys_***（6137–6141 + 6165）；M9 user/role/tenant 路由 deprecated + 侧栏隐藏；`scripts/start-ops-standalone.ps1` | ✅ 2026-07-04 |
| **P2a** | `UserSelect` 改调 **`system_users`** API（`/admin-api/system/user/simple-list`）；**不**用 `oa_author`；oa-server 未改 | ✅ 2026-07-04 |
| **P2b** | 订单列表/归因：oa-server **read-only Mapper** — 直读 **`pay_all_order`**（Football SSOT）；`GET /admin-api/oa/football-order/list`；`import-football-pay-tables.sql` 已导入 `wd`；Gateway curl **code=0**（2026-07-04）；**前端** `OrderAttribution.vue` 列表改接 football-order（ROI/导出 hybrid 保留 `order-attribution/*`） | ✅ 2026-07-04 |
| **Deferred** | 粉丝合并；`oa_author` ↔ `author_channel_sales` 映射；`oa_*`.user_id 批量迁移至 `system_users.id` | ⏸ |

**Standalone 启动**（dev/QA，无 Football）：`.\scripts\start-ops-standalone.ps1` → UI http://localhost:3000，API http://localhost:8080，Dev Token `dev-token-oa-admin`。详见 [OPS-STARTUP-MATRIX](./OPS-STARTUP-MATRIX.md) · [ADR-049](../adr/ADR-049-Ops与Football数据归属与松耦合集成.md) D6。

**S3 延期理由**（不变）：S4 前端挂载已可联调；P2a/P2b 已绿，sibling 迁移仍待产品排期。启动 S3 前需 P2b 归因 FK 语义等产品确认（见 ADR-049 待决 #3）。**Standalone 待办**见 §21。

---

## 18. UAT 抽检（2026-07-04）

### 18.1 优先页（首轮）

**Scope**：内容审核 · 计划管理 · 账号管理簇（IP组 / 平台账号 / 内部个人账号）。

| 项 | 结果 |
|----|------|
| Gateway 登录 admin/admin123 tenant 1 | ✅ code=0 |
| 自动化探针（route + vite + theme + API） | **5/5 PASS** |
| 内容审核 API | review-config ✅ · content/list ✅（待审 0 条）· dict/data ✅（9 项） |
| 计划管理 API | plan/list ✅（total=11） |
| 账号管理 API | ip-group/tree ✅（3 节点）· account/list ✅（total=7）· personal-account/list ✅（total=4） |
| 前端修复 | 无（全部绿） |
| 已知非阻塞 | 首轮 30s API 超时 → 60s 复测通过（oa 冷启动） |

**报告**：[UAT-SPOTCHECK-20260704](./UAT-SPOTCHECK-20260704.md) · `docs/delivery/uat-spotcheck-20260704-probe.json` · `scripts/uat-spotcheck-20260704.py`

### 18.2 扩展 UAT — 数据分析 + 作品监测 + 配置管理 + 绩效/财务 + 系统(OA) + 数据采集（2026-07-04）

**Scope**：CSV 可见菜单 **37 页** — 数据分析 **8** · 作品监测 **6** · 配置管理 **8** · 绩效/财务 **6** · 系统(OA) **5** · 数据采集 **4**。

| 项 | 结果 |
|----|------|
| 登录 admin/admin123 tenant 1 | ✅ code=0（本轮 Nacos 未运行 → system-server :48081 fallback） |
| 自动化探针（route + vite + theme + API） | **37/37 PASS** |
| 数据分析 | custom-query ✅ · data-report ✅ · financial-analysis ✅ · funnel ✅ · metric×2 ✅ · screen×2 ✅ |
| 作品监测 | external/high/low-fans ✅ · hot-works ✅ · ip-theme ✅ · low-score ✅ |
| 配置管理 | ai-model/prompt ✅ · external/internal/order-collect ✅ · external-data ✅ · metadata ✅ · threshold ✅ |
| 前端修复 | 无（全部绿） |
| 环境备注 | Docker/Nacos 离线时 Gateway 503；脚本 fallback oa-server :48094 探针；Redis `requirepass 123456` 对齐后可启 Gateway |

**报告**：[UAT-SPOTCHECK-EXPANDED-20260704](./UAT-SPOTCHECK-EXPANDED-20260704.md) · `docs/delivery/uat-spotcheck-expanded-20260704-probe.json` · `scripts/uat-spotcheck-expanded-20260704.py`

**累计 UAT 自动化**：优先 **5/5** + 扩展 **37/37** = **42 页 PASS**（不含 §16 全量 58 页 probe）。

### 18.3 Standalone 浏览器 E2E — 内容生产剩余 + 运营管理 + 账号管理（2026-07-04）

**Scope**：UAT 42 未覆盖的 **15 页**（`oa-menu-permission-map.csv` · `ops-platform-ui-vue` 路由）— 内容生产 **6** · 运营管理 **5** · 账号管理 **4**。

| 项 | 结果 |
|----|------|
| 栈 | `start-ops-standalone.ps1` → UI :3000 · oa-server :8080 · Dev Token |
| Playwright 探针（路由 + 无 overlay + 主内容 + `/admin-api/oa` 无 500） | **15/15 PASS** |
| 工具 | `scripts/run-uat-browser-e2e.ps1` · spec `ops-platform-ui-vue/tests/uat-browser-gap.spec.ts` |

**报告**：[UAT-BROWSER-E2E-20260704](./UAT-BROWSER-E2E-20260704.md) · `docs/delivery/uat-browser-e2e-20260704-probe.json`

**累计 UAT**：API 探针 **42/42** + 浏览器 E2E **15/15** = **57 页 PASS**（standalone AC；不含 §16 Football 58 页 vite probe）。

### 18.4 Football 全栈浏览器 E2E — :5777 登录链（2026-07-04）

**Scope**：P0 **4**（内容审核 · 计划 · IP组 · 订单归因）+ 内容生产 **5** · 运营管理 **4** · 账号管理 **4** = **17 页**（hash `#/ops/...`）。

| 项 | 结果 |
|----|------|
| 栈 | `start-integration-all.ps1 -SkipBuild` → :5777 · Gateway :48080 · oa :48094 |
| 鉴权 | 真实浏览器登录 `admin` / `admin123` · 租户 **1**（`global-setup` 持久化 session） |
| Playwright 探针（路由 + 无 overlay + 主内容 + `/admin-api/oa` 无 500） | **17/17 PASS** |
| 工具 | `scripts/run-uat-football-e2e.ps1` · spec `football-front/apps/web-ele/tests/uat-football-ops-login.spec.ts` |

**报告**：[UAT-FOOTBALL-E2E-20260704](./UAT-FOOTBALL-E2E-20260704.md) · `docs/delivery/uat-football-e2e-20260704-probe.json`

**与 Standalone 对比**：

| 路径 | UI | 鉴权 | 脚本 |
|------|-----|------|------|
| Standalone harness | :3000 | Dev Token | `scripts/run-uat-browser-e2e.ps1` |
| Football 全栈 | :5777 | Football 登录链 | `scripts/run-uat-football-e2e.ps1` |

---

## 19. API fixes — collect / dict（2026-07-04）

| API | 根因 | 修复 |
|-----|------|------|
| `GET /admin-api/oa/collect/task/list` code=500 | Spec 路径为 `/list`，实现仅 `/page`；`/list` 被 `@GetMapping("/{id}")` 捕获，`list` 无法转为 `Long` | `CollectTaskController` 增加 `@GetMapping({"/page","/list"})` 别名 |
| `GET /admin-api/oa/dict/type/list` HTTP 403 | 路径不存在（admin 规范为 `/oa/system/dict/type-list`）；oa-server 对未映射路由返回 403 | `DictController` 增加 `@GetMapping("/type/list")` + `@PreAuthorize('oa:dict:admin-list')`，委托 `SystemDictService.typeList()` |
| Football 字典 admin 403（正确路径） | 已在 S2-fix7 通过 `FootballAuthProvider.mergeOaPermissions` + `system_menu.permission` 解决；admin 角色含 menu 6137 | 无需再改 |

**变更文件**：`CollectTaskController.java` · `DictController.java` · `scripts/verify-ops-pages.py` · `scripts/verify-ops-pages-per-menu.py`

**验**（Gateway login + `X-Tenant-Id:1`）：

```bash
GET /admin-api/oa/collect/task/list?pageNo=1&pageSize=10  → code=0
GET /admin-api/oa/dict/type/list                          → code=0
GET /admin-api/oa/system/dict/list?pageNo=1&pageSize=10   → code=0
```

---

## 20. FAQ — member mock vs 真服（2026-07-04 · §23 #4 评估 2026-07-05）

> **Football 全栈集成**默认 **不** 启动完整 `member-server`，使用 Python mock（:48087）。生产切流前（S5）再评估替换。  
> **Ops standalone only**（`start-ops-standalone.ps1` · Dev Token · 无 Gateway/system）：**member mock 与真服均不相关，可完全忽略**。

### 登录 Feign 链 vs Ops 多库读（post-MDB）

| 路径 | 组件 | 端口 / 数据源 | 用途 |
|------|------|---------------|------|
| **登录** | `:5777` → Gateway → `system-server` | :48081 | `POST /admin-api/system/auth/login` |
| **登录后 author 侧写** | `system-server` Feign → `AuthorApi` | **:48087**（mock 或真 jar） | `getAuthorByMobile` · `updateAuthorLoginInfo`（admin 无绑定作者时 mock 返回 null，**不阻塞登录**） |
| **登录后权限** | `system-server` Feign → `AuthorApi` | :48087 | `getPermittedIds` 等（Ops 页走 oa-server RBAC，**不依赖** member Feign） |
| **Ops 作者/账号 CRUD** | `oa-server` `@DS("member")` | **localhost:3306/shenyu-member** | `author_user` · ext 表；**不经** member-server HTTP |
| **Gateway `/admin-api/member/**`** | Gateway 路由 | :48087 | Football 原生 member 报表等；**58 Ops 路由不调用** |

配置 SSOT：`scripts/integration-config/football-integration-overlay.yml` → `spring.cloud.openfeign.client.config.member-server.url: http://127.0.0.1:48087`；Gateway `gateway-integration-local.yaml` 同端口。

| 项 | 说明 |
|----|------|
| **Standalone** | `start-ops-standalone.ps1` 不启动 member mock / system-server / Gateway；鉴权走 oa-server dev 固定 Token（ADR-003），**无需** :48087 |
| **默认行为（全栈）** | `start-integration-all.ps1` 启动 Python **`mock-member-author-server.py`** 监听 **:48087**（桩：`getAuthorByMobile`→null · `updateAuthorLoginInfo`→true · `/actuator/health`→UP） |
| **可选真服** | `-FullMemberServer` / `-UseMemberServer`：同端口 **:48087** 起 member jar（**非**生产默认 :48082；overlay 已统一到 48087）。本地实测 **2026-07-05**：jar 启动失败 — `RocketMQTemplate` bean 缺失（overlay 已 exclude autoconfig，业务仍硬依赖 MQ） |
| **真服端口（生产）** | 完整 **`member-server`** jar 默认 **:48082**；`infra-server` Feign URL 在 overlay 指向 :48082，与 member 分离 |
| **为何 mock** | 登录链仅需 AuthorApi 桩；Ops post-MDB 作者数据已走 **shenyu-member 直连**；mock 足以支撑 Gate / E2E |
| **§23 #4 结论** | **保持 mock（方案 C / 默认）**；真服本地集成 **不可行**（无 Football 代码改动前提下 RocketMQ 阻塞）；S5 切流再评估完整 member 栈 |

**脚本**：

```powershell
# 默认（推荐）
.\scripts\start-integration-all.ps1 -SkipBuild

# 实验性真服（可能失败，见上表）
.\scripts\start-integration-all.ps1 -FullMemberServer -SkipBuild
.\scripts\start-integration-all.ps1 -UseMemberServer -SkipBuild   # 同上
```

---

## 21. 待办清单 — 按用户场景（2026-07-04 修订）

### 已完成（两场景共用）

| 项 | 状态 |
|----|------|
| GATE-INT-S0 / S1 签收 | ✅ 2026-07-04 |
| S4 前端挂载 58/58 · 4-module parallel | ✅ |
| UAT 优先 5/5 + 扩展 37/37 | ✅ |
| UAT standalone 浏览器 E2E 15/15 | ✅ `scripts/run-uat-browser-e2e.ps1` |
| UAT Football 浏览器 E2E (:5777) | ✅ **17/17** · `scripts/run-uat-football-e2e.ps1` |
| S3 sibling 工程 | ⏸ **Deferred**（ADR-049） |
| P2-standalone harness | ✅ `start-ops-standalone.ps1` |
| P2a UserSelect → `system_users` | ✅ |
| P2b 订单只读 `football-order/list` | ✅ |
| collect/dict API 修复 | ✅ §19 |

### Ops standalone only（当前用户）

| # | 待办 | 优先级 | 说明 |
|---|------|--------|------|
| 1 | 日常启动验证 | P0 | `.\scripts\start-ops-standalone.ps1` → http://localhost:3000 · API :8080 · Token `dev-token-oa-admin` |
| 2 | ~~浏览器人工走查（未覆盖页）~~ | ✅ | **15/15 PASS** — `scripts/run-uat-browser-e2e.ps1`（内容生产剩余 · 运营管理 · 账号管理）；报告 [UAT-BROWSER-E2E-20260704](./UAT-BROWSER-E2E-20260704.md) |
| 3 | 数据质量后端实现 | P2 | 当前 stub 空列表；`collect/quality` 页可开但无真实数据 |
| 4 | 复杂页运行时 import | P2 | 富文本 / 大屏 / Layout 等边缘 import（§7 #4） |
| 5 | 产品待决：订单归因 FK | P2 | `oa_order_attribution.order_id` 语义 · `author_id` 映射（ADR-049 待决 #3） |
| — | ~~member mock 替换~~ | **已取消** | standalone 不经过 member 登录链 |
| — | ~~S5 切流~~ | **不适用** | 无 Football 壳层 |
| — | ~~S3 sibling~~ | **Deferred** | 非 standalone 阻塞 |

### Football 全栈集成（非当前用户范围）

| # | 待办 | 优先级 | 说明 |
|---|------|--------|------|
| 1 | S5 切流准备 | P1 | Football E2E `run-uat-football-e2e.ps1`（§18.4）；绩效/财务/配置等剩余页可扩至 58 |
| 2 | member 真服评估 | P2 | S5 前是否将 :48087 mock 换为 `member-server` :48082（§20） |
| 3 | S3 `football-module-oa` sibling | ⏸ Deferred | 待产品排期 · `sys_dict_*`/M9 范围确认 |
| 4 | 下线独立 Ops 入口 | P3 | S5 后评估是否保留 :3000 harness（ADR-049 D6） |
| 5 | Deferred 数据合并 | — | ~~`oa_author` 映射~~ **S4 已 DROP** · 粉丝合并 · `user_id` 批量迁移 |

---

## 22. 多库复用程序（ADR-050 · 2026-07-05）

> SSOT：[OPS-FOOTBALL-MULTI-DB-EXECUTION-PLAN.md](./OPS-FOOTBALL-MULTI-DB-EXECUTION-PLAN.md) · Gate 报告 `docs/delivery/gates/GATE-MDB-S*.md`

| 阶段 | 状态 | 摘要 |
|------|------|------|
| S0 基建 | ✅ Gate 已签 | TRUNCATE · V131 · multidb profile · [GATE-MDB-S0-报告](./gates/GATE-MDB-S0-报告-20260705.md) |
| S1 作者+微信 | ✅ Gate 已签 | member SSOT · mp+ext 双写 · E2E 58/58 |
| S2 非微信+字典 | ✅ Gate 已签 | DictAdapter · IP 组 · E2E 58/58 |
| S3 日志+采集 | ✅ Gate 已签 | system/pay DS adapter · E2E 58/58 |
| **S4 Cutover** | **✅ Gate 已签** | V132 DROP `oa_author` · E2E **58/58** · 远程 matrix 文档化 |

**程序状态（2026-07-05 post-S4）**：**localhost MDB S0–S4 全部 ✅**；H2 Flyway test profile 修复（V126–V132 skip）；日常 dev **仅 localhost:3306 五库**。

**远程 cutover**：⏸ **Deferred**（2026-07-05 用户取消）— 101.37.161.136 **非部署环境**，不继续远程 DB sync / GATE-MDB-REMOTE。归档见 [GATE-MDB-REMOTE-报告](./gates/GATE-MDB-REMOTE-报告-20260705.md)。

**localhost 启动**：`start-integration-all.ps1 -SkipBuild` → `:5777` · oa-server profile `dev-local-multidb`

**Post-MDB 本地签收**（2026-07-05）：[POST-MDB-LOCAL-SIGNOFF-20260705](./gates/POST-MDB-LOCAL-SIGNOFF-20260705.md) · E2E 58/58 · DB SSOT 探针 ✅ · Gateway OA API smoke **4/4 PASS**（§23 #1+#2 2026-07-05）

**远程 cutover 脚本**（Deferred — 仅部署环境另批启用）：
- 连通性：`scripts/test-remote-mysql-connection.ps1`（需 `OA_DB_PASSWORD`）
- Nacos overlay：`scripts/integration-config/oa-server-remote-multidb.yaml` · push：`scripts/push-remote-multidb-config.ps1 -WhatIf`
- Flyway 清单：`scripts/integration-config/mdb-remote-flyway-checklist.md`
- 矩阵 SSOT：`scripts/integration-config/mdb-s4-nacos-matrix.md`

---

## 23. 本地 Football 集成路线图（post-MDB · 2026-07-05）

> **范围**：`start-integration-all.ps1` → `:5777` Gate 路径；**不含** S5 生产切流 · **不含** 101.37.161.136 远程 cutover（已 Deferred，见 §22）。

| # | 任务 | 优先级 | 说明 |
|---|------|--------|------|
| 1 | **Gateway OA API 鉴权修复** | ✅ **完成**（2026-07-05） | ~~401~~ → **API smoke 4/4 PASS**（`post-mdb-local-smoke.py`）。根因：`FootballAuthProvider` 仅查 `shenyu-system.system_oauth2_access_token`；system-server 写 token 至 **Redis** + **wd** master；`oa:*` 菜单权限在 **wd.system_menu** 不在 shenyu-system。**修复**：`FootballOAuth2TokenRedisReader`（Jedis · `oauth2_access_token:{token}`）+ `FootballOAuth2MasterTokenMapper`（wd token/user/RBAC）+ username 映射 shenyu-system 用户。配置：`application-dev-local-multidb.yml` · `oa.auth.football-redis.enabled=true`。 |
| 2 | **Gateway OA API 403 权限补全** | ✅ **完成**（2026-07-05） | §23 #1 后 smoke 仅 `author_list` 200，其余 403。根因：`wd.system_menu` 行存在但 `system_role_menu` 缺 admin 授权（6137/6138/6139/6149）；`FootballAuthProvider` 从 wd master RBAC 加载 `oa:*`。**修复**：`patch-smoke-api-permissions.sql`（+ hidden 6174 `oa:account:list`）→ `apply-smoke-api-permissions.py` 应用于 **wd** + **shenyu-system**。API smoke **4/4 PASS** · E2E **58/58**。 |
| 3 | **内容/计划 Football ID 对齐** | ✅ **完成**（2026-07-05） | `oa_production_content` / `oa_task` / `oa_content.author_id` 写入与校验统一为 `member.author_user.id`（`AuthorResolveSupport`）；计划任务生成/启动回填 `author_id`；历史行对账脚本 `patch-content-author-id-align.sql`；探针 `probe-content-author-align.py` · E2E **58/58** · API smoke **4/4**。 |
| 2b | **integration 后 dev-token / sys_user 映射** | ✅ **完成**（2026-07-05） | S0 TRUNCATE 清空 localhost `wd.sys_user_token` / `sys_user`；Gate **仅 Football 登录**；dev-token **仅 Standalone :8080**。`mergeOaPermissions` 为过渡桥接，localhost post-S0 无 sys_user 时不生效。可选手工 re-seed 见 [OPS-STARTUP-MATRIX §4](./OPS-STARTUP-MATRIX.md#4-23-2b--dev-token--sys_user-映射post-s0-truncate)。 |
| 4 | **member mock (:48087) vs member-server** | ✅ **完成**（2026-07-05） | **结论：保持 Python mock（方案 C / Hybrid C）**。登录 Feign（`AuthorApi` @48087）与 Ops `@DS("member")` 读 **shenyu-member** 已分离；58 Ops 路由不调用 `/admin-api/member/**`。真 jar 本地启动失败（`RocketMQTemplate` 缺失，无 Football 代码改动无法修）。脚本已有 `-FullMemberServer`；新增 `-UseMemberServer` 别名 + §20 Feign 链文档。**验**：login code=0 · API smoke 4/4 · E2E **58/58**（mock）。 |
| 5 | **Standalone vs Integration 文档分流** | ✅ **完成**（2026-07-05） | SSOT：[OPS-STARTUP-MATRIX.md](./OPS-STARTUP-MATRIX.md) — `start-ops-standalone.ps1`（:3000/:8080 · profile **`dev`** · 远程 `wd` · dev-token）vs `start-integration-all.ps1`（:5777/:48080 · Football 登录 · **`dev-local-multidb`** · localhost 五库 · member mock :48087）。 |

**下一 Gate 动作（本地）**：§23 #1–#5 已全部 ✅（2026-07-05）；后续见 S5 切流 / S3 sibling（§21 Football 场景）。

---

## 24. 外部竞品采集 Slice 立项（2026-07-08）

> **SSOT**：[ADR-052](../adr/ADR-052-Ops外部竞品四平台采集通道.md) · [M10-EXTERNAL-四平台竞品采集-SLICE](./M10-EXTERNAL-四平台竞品采集-SLICE.md) · MASTER §17.3

| 项 | 状态 | 摘要 |
|----|------|------|
| ADR-052 Channel-D 决策 | ✅ 草案 | Channel-D vs Channel-A INTERNAL；四平台能力矩阵 + collector 缺口 |
| Q3 运营凭账号层级 | ✅ 已确认 | **租户级** `oa_tenant_collector_credential`；任务引用 profile，不内嵌密钥 |
| Slice P0–P3 | ✅ 规划 | P0 快手 `user-videos` E2E 为首 shippable |
| Ops Adapter | ✅ P0 实现 | `ExternalCollectorAdapterImpl` + `KuaishouExternalWorkSyncService` |
| M8 配置 UI | ✅ 已有 | `/ops/config-external-collect` — 配置未驱动执行 |
| M10 执行 | 🟡 P0 开发中 | `method=EXTERNAL` 路由 + V136 表 · IT `M10ExternalKuaishouS01IT` |
| P0 字段映射 §4.4 | ✅ 2026-07-08 | `work_url` 分享链接 · `comment_count` · UK · `account_id`→`oa_external_account.id` |
| collector 外部 repo | ⬜ 缺口 | 抖音 user-videos · 视频号 external 待补 |

**下一动作**：跑通 `M10ExternalKuaishouS01IT` + 人工 Gate（M8 配置 → 任务 run → M7 hot-works）。
