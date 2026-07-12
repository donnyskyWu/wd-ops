# Ops Page Acceptance — 4-Module Parallel (2026-07-04)

> Gateway `:48080` · oa-server `:48094` · Vite `:5777` · auth=gateway-login (`admin` / `admin123`, tenant `1`)

Parallel acceptance per `docs/delivery/OPS-PAGE-ACCEPTANCE-PLAN.md`: route + Vite compile + theme (`ops-page`, dark search vars) + API (`code=0`, valid payload).

---

## Batch 1: 内容生产 + 作品监测

**Scope**: 14 visible menu pages (`hide_in_menu=N`) from `oa-menu-permission-map.csv`.

**Date**: 2026-07-04

| Module | Pages | Route | Vite | Theme | API | Result |
|--------|-------|-------|------|-------|-----|--------|
| **作品监测** | 6/6 | 6/6 | 6/6 | 6/6 | 6/6 | **PASS** |
| **内容生产** | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | **PASS** |

**Overall: 14/14 PASS**

### Special check — SOP 审核

| Item | Expect | Result |
|------|--------|--------|
| Frontend wiring | `review.vue` calls `getSopReviewPending` (not mock) | **PASS** — `#/api/ops/sop` → `GET /oa/sop/review/pending` |
| Gateway probe | `GET /admin-api/oa/sop/review/pending` → `code=0` | **PASS** — `data: []` (empty queue valid) |
| Approve/reject | `approveReview` / `rejectReview` wired | **PASS** (code review; no pending rows to exercise) |

### 作品监测（6/6）

| Menu | Route | Primary API | Data shape |
|------|-------|-------------|------------|
| 外部账号分析 | `/ops/external-account` | `GET …/monitor/external/list` | page list=1 total=19 |
| 高粉账号分析 | `/ops/high-fans-account` | `GET …/monitor/high-follower/list` | page list=1 total=1 |
| 爆款作品分析 | `/ops/hot-works` | `GET …/monitor/hit/list` | page list=1 total=7 |
| IP主题数据 | `/ops/ip-theme` | `GET …/monitor/ip-theme/1` | object (topTitles, workCount, …) |
| 低粉账号分析 | `/ops/low-fans-account` | `GET …/monitor/low-follower/list` | page list=0 total=0 |
| 低分作品分析 | `/ops/low-score` | `GET …/monitor/low-score/list` | page list=1 total=4 |

### 内容生产（8/8）

| Menu | Route | Primary API | Data shape |
|------|-------|-------------|------------|
| 内容管理 | `/ops/content` | `GET …/content/list` | page list=1 total=21 |
| 内容审核 | `/ops/content/review` | `GET …/content/list` | page list=1 total=21 |
| 内容知识库 | `/ops/knowledge` | `GET …/knowledge/list` | page list=1 total=12 |
| 公推模板库 | `/ops/layout-template` | `GET …/layout-template/list` | page list=1 total=15 |
| 计划管理 | `/ops/plan` | `GET …/plan/list` | page list=1 total=11 |
| SOP管理 | `/ops/sop` | `GET …/sop/template/list` | page list=1 total=7 |
| SOP审核 | `/ops/sop/review` | `GET …/sop/review/pending` | array len=0 |
| 任务管理 | `/ops/task` | `GET …/task/list` | page list=1 total=98 |

### Fixes applied

None — all 14 pages green on first run. Services (Gateway, oa-server, Vite) were already up.

**Probe alignment** (non-blocking): corrected `API_PROBE` for `/ops/sop/review` in `verify-ops-pages-per-menu.py` from template list → `…/sop/review/pending` to match frontend and `probe-ops-p1-content-monitor.py`.

### Reports

| File | Purpose |
|------|---------|
| `docs/delivery/ops-acceptance-batch1-report.json` | Full per-menu probe (route + vite + theme + api) |
| `docs/delivery/ops-acceptance-p1-probe.json` | Dedicated content+monitor API/theme probe |

### Runbook

```powershell
python scripts/verify-ops-pages-per-menu.py --modules 内容生产,作品监测 --api --json docs/delivery/ops-acceptance-batch1-report.json
python scripts/probe-ops-p1-content-monitor.py
```

---

<!-- Batch 2+ appended below -->

## Batch 2: 数据分析 + 数据采集 + 系统管理(OA)

**Scope**: 17 visible menu pages (`hide_in_menu=N`) from `oa-menu-permission-map.csv`.

**Date**: 2026-07-04

| Module | Pages | Route | Vite | Theme | API | Result |
|--------|-------|-------|------|-------|-----|--------|
| **数据分析** | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | **PASS** |
| **数据采集** | 4/4 | 4/4 | 4/4 | 4/4 | 4/4 | **PASS** |
| **系统管理(OA)** | 5/5 | 5/5 | 5/5 | 5/5 | 5/5 | **PASS** |

**Overall: 17/17 PASS** (initial run 16/17 — 1 theme fail on 漏斗分析; fixed and re-run green)

### 数据分析（8/8）

| Menu | Route | Primary API | Notes |
|------|-------|-------------|-------|
| 自定义查询 | `/ops/custom-query` | `GET …/query/list` | |
| 数据报表 | `/ops/data-report` | `GET …/report/unified-account/stats` | |
| 总体财务分析 | `/ops/financial-analysis` | `GET …/finance/roi/analysis` | requires `startDate`/`endDate` |
| 漏斗分析 | `/ops/funnel-analysis` | `GET …/funnel/list` | theme fix: antd `#1890ff` → `var(--el-color-primary)` |
| 指标管理 | `/ops/metric` | `GET …/metric/list` | |
| 指标分析 | `/ops/metric-analysis` | `GET …/metric/list` | |
| 数据大屏 | `/ops/screen` | `GET …/dashboard-config/list` | fullscreen page |
| 大屏配置 | `/ops/screen-config` | `GET …/dashboard-config/list` | |

### 数据采集（4/4）

| Menu | Route | Primary API | Notes |
|------|-------|-------------|-------|
| 采集日志 | `/ops/collect/log` | `GET …/collect/log/page` | |
| 私域桥接 | `/ops/collect/private-domain-bridge` | `GET …/collect/private-domain-bridge/page` | |
| 数据质量 | `/ops/collect/quality` | `GET …/collect/quality/list` | stub empty list (API-M10 §2) |
| 采集任务 | `/ops/collect/task` | `GET …/collect/task/page` | |

### 系统管理(OA)（5/5）

| Menu | Route | Primary API | Notes |
|------|-------|-------------|-------|
| 字典配置 | `/ops/system-dict` | `GET …/system/dict/list` | |
| 登录日志 | `/ops/system-log/login` | `GET …/system/log/login` | |
| 操作日志 | `/ops/system-log/operation` | `GET …/system/log/operation` | |
| 消息管理 | `/ops/system-message` | `GET …/system/message/list` | |
| 系统参数 | `/ops/system-param` | `GET …/system/param/list` | |

### Fixes applied

1. **漏斗分析 theme FAIL** — `:deep(.el-button--primary)` in `FunnelAnalysis.vue` used Ant Design `#1890ff` / `#40a9ff`; replaced with `var(--el-color-primary)` / `var(--el-color-primary-light-3)` in both `football-front` mount and `ops-platform-ui-vue` source.
2. **Mount script** — extended `COLOR_REWRITES` in `mount-ops-all.py` for `#1890ff` / `#40a9ff` → Element CSS vars (future remounts).

### Reports

| File | Purpose |
|------|---------|
| `docs/delivery/ops-acceptance-batch2-report.json` | Full per-menu probe (route + vite + theme + api) |
| `docs/delivery/ops-acceptance-batch2.log` | `verify-ops-pages-per-menu.py` stdout |
| `docs/delivery/ops-acceptance-batch2-probe.log` | `probe-ops-m6-m10-m9.py` stdout |

### Runbook

```powershell
python scripts/verify-ops-pages-per-menu.py --modules "数据分析,数据采集,系统管理(OA)" --api --json docs/delivery/ops-acceptance-batch2-report.json
python scripts/probe-ops-m6-m10-m9.py
```

---

## Batch 3: 绩效核算 + 财务管理 + 账号管理

**Scope**: 12 visible menu pages (`hide_in_menu=N`) from `oa-menu-permission-map.csv`.

**Date**: 2026-07-04

| Module | Pages | Route | Vite | Theme | API | Result |
|--------|-------|-------|------|-------|-----|--------|
| **绩效核算** | 4/4 | 4/4 | 4/4 | 4/4 | 4/4 | **PASS** |
| **财务管理** | 2/2 | 2/2 | 2/2 | 2/2 | 2/2 | **PASS** |
| **账号管理** | 6/6 | 6/6 | 6/6 | 6/6 | 6/6 | **PASS** |

**Overall: 12/12 PASS**

### Reports

| File | Purpose |
|------|---------|
| `docs/delivery/ops-acceptance-batch3-report.json` | Full per-menu probe (route + vite + theme + api) |

### Runbook

```powershell
python scripts/verify-ops-pages-per-menu.py --modules "绩效核算,财务管理,账号管理" --api --json docs/delivery/ops-acceptance-batch3-report.json
```

---

## Batch 4: 运营管理 + 配置管理 + 首页

**Scope**: 15 visible menu pages (`hide_in_menu=N`) from `oa-menu-permission-map.csv` — includes **首页仪表盘** (`system_menu` **6168**, parent 运营数据) under `parent_group=首页`.

**Date**: 2026-07-04

| Module | Pages | Route | Vite | Theme | API | Result |
|--------|-------|-------|------|-------|-----|--------|
| **运营管理** | 6/6 | 6/6 | 6/6 | 6/6 | 6/6 | **PASS** |
| **配置管理** | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | **PASS** |
| **首页** | 1/1 | 1/1 | 1/1 | 1/1 | 1/1 | **PASS** |

**Overall: 15/15 PASS** (initial run 12/15 — 3 theme fails; fixed and re-run green)

### Special checks — 首页仪表盘 + 数据大屏

| Item | Route | Primary API | Result |
|------|-------|-------------|--------|
| 首页仪表盘 (`6168`) | `/ops/dashboard` → `ops/Dashboard` | `GET …/dashboard/home/trend?type=CONTENT` | **PASS** — route + vite + theme + API |
| 数据大屏 (`6131`) | `/ops/screen` → `ops/screen/DataScreenFullscreen` | `GET …/dashboard-config/list` | **PASS** — verified in Batch 2 (数据分析 8/8) |

### 运营管理（6/6）

| Menu | Route | Primary API | Notes |
|------|-------|-------------|-------|
| 账号分析 | `/ops/account-analysis` | `GET …/account-analysis/list` | |
| 作者管理 | `/ops/author` | `GET …/author/list` | |
| 人效盘点 | `/ops/efficiency` | `GET …/productivity-review/list` | |
| 粉丝分析 | `/ops/fans-analysis` | `GET …/follower-analysis/list` | theme fix: `#1890ff` → `var(--el-color-primary)` |
| 内部作品分析 | `/ops/internal-content` | `GET …/internal-content/list` | theme fix: `#1890ff` → `var(--el-color-primary)` |
| IP组管理 | `/ops/ip-group` | `GET …/ip-group/tree` | |

### 配置管理（8/8）

| Menu | Route | Primary API | Result |
|------|-------|-------------|--------|
| AI模型 | `/ops/config-ai-model` | `GET …/config/ai-model/list` | PASS |
| AI提示词 | `/ops/config-ai-prompt` | `GET …/config/ai-prompt/list` | PASS |
| 外部采集配置 | `/ops/config-external-collect` | `GET …/config/external-collect/list` | PASS |
| 外部数据配置 | `/ops/config-external-data` | `GET …/config/external-source/list` | PASS |
| 内部采集配置 | `/ops/config-internal-collect` | `GET …/config/internal-collect/list` | PASS |
| 元数据维护 | `/ops/config-metadata` | `GET …/metadata/list` | PASS |
| 订单采集配置 | `/ops/config-order-collect` | `GET …/config/order-collect/list` | PASS |
| 阈值规则配置 | `/ops/config-threshold` | `GET …/config/threshold/list` | PASS |

### 首页（1/1）

| Menu | Route | Primary API | Notes |
|------|-------|-------------|-------|
| 首页仪表盘 | `/ops/dashboard` | `GET …/dashboard/home/trend?type=CONTENT` | theme fix: quick-action + KPI primary colors → Element CSS vars |

### Fixes applied

1. **粉丝分析 theme FAIL** — `:deep(.el-button--primary)` in `FansAnalysis.vue` used Ant Design `#1890ff` / `#40a9ff`; replaced with `var(--el-color-primary)` / `var(--el-color-primary-light-3)`.
2. **内部作品分析 theme FAIL** — same primary-button override fix in `InternalContent.vue`.
3. **首页仪表盘 theme FAIL** — `Dashboard.vue` quick-action gradient/icon and KPI card primary colors → Element CSS vars.

### Reports

| File | Purpose |
|------|---------|
| `docs/delivery/ops-acceptance-batch4-report.json` | Full per-menu probe (route + vite + theme + api) |

### Runbook

```powershell
python scripts/verify-ops-pages-per-menu.py --modules "运营管理,配置管理,首页" --api --json docs/delivery/ops-acceptance-batch4-report.json
```

---

## Final Summary — 4-Module Parallel (2026-07-04)

**Scope**: 58 visible menu pages across **10 modules** (`hide_in_menu=N`, M9 excluded).

| Batch | Modules | Pages | Route | Vite | Theme | API | Result |
|-------|---------|-------|-------|------|-------|-----|--------|
| **1** | 内容生产 + 作品监测 | 14/14 | 14/14 | 14/14 | 14/14 | 14/14 | **PASS** |
| **2** | 数据分析 + 数据采集 + 系统管理(OA) | 17/17 | 17/17 | 17/17 | 17/17 | 17/17 | **PASS** |
| **3** | 绩效核算 + 财务管理 + 账号管理 | 12/12 | 12/12 | 12/12 | 12/12 | 12/12 | **PASS** |
| **4** | 运营管理 + 配置管理 + 首页 | 15/15 | 15/15 | 15/15 | 15/15 | 15/15 | **PASS** |

**Grand total: 58/58 PASS** (route + vite + theme + API)

| Module | Pages | Batch |
|--------|-------|-------|
| 作品监测 | 6 | 1 |
| 内容生产 | 8 | 1 |
| 数据分析 | 8 | 2 |
| 数据采集 | 4 | 2 |
| 系统管理(OA) | 5 | 2 |
| 绩效核算 | 4 | 3 |
| 财务管理 | 2 | 3 |
| 账号管理 | 6 | 3 |
| 运营管理 | 6 | 4 |
| 配置管理 | 8 | 4 |
| 首页 | 1 | 4 |

**Reports**: `ops-acceptance-batch{1,2,3,4}-report.json` · [OPS-PAGE-ACCEPTANCE-PLAN](./OPS-PAGE-ACCEPTANCE-PLAN.md)

**Known non-blocking**: 数据质量后端 stub (empty list); chart config on non-menu pages may retain legacy hex colors.
