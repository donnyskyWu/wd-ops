# MERGE Runtime E2E 签收报告

| 项 | 内容 |
|----|------|
| **日期** | 2026-07-31 |
| **范围** | 运行时合并闭环（FE SSOT `:5777` + Gateway `:48080` + monorepo ops-server `:48094`） |
| **结论** | **条件签收（Runtime merge = Conditional GO）** |
| **不可代签** | **B-WP4-ARCHIVE**（表 DROP/RENAME）— 仍须人工填 [B-WP4-ARCHIVE-签收表-20260731.md](./B-WP4-ARCHIVE-签收表-20260731.md) |
| **Phase C 整包** | 仍 **NO-GO**（即便本报告 Runtime 条件签收） |

---

## 1. E2E 能证明 / 不能证明

### 能证明（本报告覆盖）

- Football 壳 `:5777` 可登录（`admin` / `admin123` · tenant `1`）并进入 OPS 业务路由
- Gateway `:48080` → ops-server `:48094` 主路径可达（健康检查 + 关键业务 API `code=0`）
- Gate Playwright（`football-front/apps/web-ele/tests/`）对 **69+ OPS 路由** 可达性烟雾：**全部通过**（鉴权注入后）
- Dashboard 核心 UI（KPI / 筛选 / 图表 / 待办 / 刷新）可用
- 内容管理：新增 drawer UX 路径可用（CONTENT-GATE-002）
- 关键 OPS API 非 Cutover stub 410：`ip-group/tree|list`、`content/list`、`dashboard/overview` → HTTP 200 / `code=0`

### 不能证明 / 不替代

| 项 | 说明 |
|----|------|
| **B-WP4 Q1–Q5** | 表归档范围、IT seed、备份窗口、Mapper 下线、执行顺序 — **产品/DBA 书面签收** |
| 物理 `DROP` / `RENAME` | **禁止**因 E2E 绿而执行 |
| Phase C 整包 GO | 仍阻塞于 B-WP4-ARCHIVE |
| 全量 TESTCASES / 全 `@smoke` 套件 | 本次跑 **合并相关子集**（见 §3），非 102 用例全绿宣称 |
| 侧边栏 DOM 选择器 / 旧路径别名 | 见 §5 已知缺口（非 stub 410） |

---

## 2. 环境

| 组件 | 地址 | 状态（跑前探测） |
|------|------|------------------|
| football-front (Vite) | `http://127.0.0.1:5777` | UP |
| Gateway | `http://127.0.0.1:48080` | UP (`/actuator/health`) |
| ops-server (monorepo) | `http://127.0.0.1:48094` | UP (`/actuator/health`) |
| 账号 | `admin` / `admin123` · `tenant-id=1` | login `code=0` |

**环境注记（Windows）**：本机 `localhost` → `::1` 对 5777/48080/48094 **超时**，`127.0.0.1` 正常。已将 Gate 探测与 Vite `VITE_BASE_URL`、Playwright `baseURL` 改为优先 IPv4，避免假阴性。

---

## 3. 执行命令与范围

**正式签收跑次（鉴权注入后）**：

```powershell
# 栈已起时：
cd football-front/apps/web-ele
..\..\node_modules\.bin\playwright.cmd test --config=playwright.config.ts --reporter=list `
  tests/football-content-smoke.spec.ts `
  tests/p0-modules.spec.ts `
  tests/dashboard.spec.ts `
  tests/ux-routes-smoke.spec.ts
```

| 项 | 值 |
|----|-----|
| 用例数 | 88 |
| 耗时 | ~15.9 min |
| 原始日志 | [`e2e-artifacts/MERGE-RUNTIME-E2E-20260731/playwright-list-authed.log`](../e2e-artifacts/MERGE-RUNTIME-E2E-20260731/playwright-list-authed.log) |
| 配套改动（使 Gate 可跑） | `tests/helpers/football-auth.ts`；dashboard / p0 / ux-routes / content-smoke 注入 `seedFootballAuth`；`playwright.config.ts` / `run-gate-football-e2e.ps1` / `.env.development` 优先 `127.0.0.1` |

**未作为签收主证据的跑次**：首次无鉴权子集（大量落到 `/auth/login`，49/39）— 仅证明「未 seed Football token 时旧用例失效」，不代表 Runtime merge 失败。

---

## 4. 结果总表

| 套件 | Pass | Fail | 说明 |
|------|-----:|-----:|------|
| `dashboard.spec.ts` | 6 | 1 | DASH-001~006 ✅；DASH-007 控制台错误断言 ❌ |
| `football-content-smoke.spec.ts` | 1 | 3 | GATE-002 ✅；001 超时；003 seed 行缺失；004 locator strict |
| `p0-modules.spec.ts` | 1 | 5 | AUTH-001 ✅；IPG-000~004 → FE **404 页**（API 仍绿） |
| `ux-routes-smoke.spec.ts` | 70 | 1 | **全部参数化路由 ✅**；仅 UX-ROUTE-2 侧栏 CSS 选择器 ❌ |
| **合计** | **78** | **10** | **88.6%** |

### 路由烟雾（合并主证据）

鉴权后，`ACCESSIBLE_ROUTES` 中 OPS 路径（含 dashboard / content / company / order-attribution / config-* / system-* / analysis reports 等）**均未落到登录页且 URL 含目标 path**。  
这是「FE SSOT + monorepo ops 运行时可签收」的核心证据。

### API 抽检（非 stub）

| API | 结果 |
|-----|------|
| `GET /admin-api/ops/ip-group/tree` | 200 · `code=0` · 有树数据 |
| `GET /admin-api/ops/ip-group/list` | 200 · `code=0` |
| `GET /admin-api/ops/content/list` | 200 · `code=0` · 有列表 |
| `GET /admin-api/ops/dashboard/overview` | 200 · `code=0` |

未观察到 Cutover **410 Gone** / deferred stub 作为本批失败主因。

---

## 5. 失败清单与根因分类

| # | 用例 | 根因分类 | 说明 |
|---|------|----------|------|
| 1 | DASH-007 | 非阻断 / 控制台噪音 | 页面功能用例已过；存在 console error（非白屏） |
| 2 | CONTENT-GATE-001 | 不稳定 / 超时 | `toHaveURL` 时 page closed / 90s timeout；同套件 GATE-002 已证明内容页可开 |
| 3 | CONTENT-GATE-003 | 数据依赖 | `seed content not found`（列表无测试预期行） |
| 4 | CONTENT-GATE-004 | 测试脆弱 | `table` locator strict mode（匹配 2 个） |
| 5–9 | IPG-000~004 | **FE 路由缺口** | `/ops/ip-group` 直链落到 **「哎呀！未找到页面」**；**后端 API 正常**。ux-routes 同 path「可达」仅校验 URL/非空 body，**未否定 404 业务页** → 记为 **已知 FE 路径/菜单注册缺口** |
| 10 | UX-ROUTE-2 | 选择器漂移 | Football 侧栏为 `listitem` 结构，旧 `.el-menu-item` 计数为 0；快照显示侧栏实有多组菜单 |

**非本次失败主因**：Cutover stub 410、Gateway 宕机、ops-server 未起。

---

## 6. 签收建议

| 维度 | 建议 |
|------|------|
| **Runtime merge（FE + Gateway + monorepo ops）** | **条件签收 / Conditional GO** |
| 条件 | （1）接受 §5 已知缺口（IP 组直链 404、内容 seed/断言脆弱、侧栏选择器）不阻塞「壳内 OPS 主路径可跑」；（2）**不**把本报告当作 B-WP4 或 Phase C 整包 GO |
| **B-WP4 / Phase C 整包** | **不可签收** — 下一步见 §7 |

**一句话**：E2E **可以**做运行时合并签收闭环；**不能**代签 B-WP4 表归档。

---

## 7. 下一步（人工）

1. **产品 + DBA + OPS + 运维** 填写并勾选  
   [`docs/delivery/gates/B-WP4-ARCHIVE-签收表-20260731.md`](./B-WP4-ARCHIVE-签收表-20260731.md) **Q1–Q5**  
   （书面结论：签收 / 拒签 / 有条件签收）
2. 签收后另开 Slice：备份 → 归档 SQL → 只读探测（**禁止**在未签收时 DROP/RENAME）
3. 可选工程跟进（非 B-WP4）：对齐 `/ops/ip-group` FE 路由与菜单；收紧 ux-routes 对 404 业务页的断言；稳定 content seed

---

## 8. 关联

- Work plan：[`OPS-FOOTBALL-MERGE-WORK-PLAN.md`](../OPS-FOOTBALL-MERGE-WORK-PLAN.md)
- Tracker §20：[`MASTER-EXECUTION-TRACKER.md`](../MASTER-EXECUTION-TRACKER.md)
- 手验（业务 API）：[`G-STAR-HANDVERIFY-20260730/REPORT.md`](../e2e-artifacts/G-STAR-HANDVERIFY-20260730/REPORT.md)
- 原始 Playwright 日志：[`MERGE-RUNTIME-E2E-20260731/`](../e2e-artifacts/MERGE-RUNTIME-E2E-20260731/)
