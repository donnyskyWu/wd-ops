# Ops Page Acceptance — P1（运营管理 + 配置管理）

> **Date**: 2026-07-03 · Gateway `:48080` · Vite `:5777` · auth=gateway-login (`admin` / `admin123`, tenant `1`)

## Summary

| Gate | Scope | Route | Vite | Theme | API | Result |
|------|-------|-------|------|-------|-----|--------|
| **P1** | 运营管理 **5** + 配置管理 **8** = **13** pages | 13/13 | 13/13 | 13/13 | 13/13 | **PASS** |
| **P1 extended** | CSV 运营管理 **6** + 配置管理 **8** = **14** rows | 14/14 | 14/14 | 14/14 | 14/14 | **PASS** |

**Special checks**

| Item | Expect | Result |
|------|--------|--------|
| `#/ops/dashboard` trend (`GET …/dashboard/home/trend`) | V129 rolling seed → non-empty | **PASS** (12 points, last 7d) |
| `#/ops/dashboard` platform-dist | non-empty | **PASS** (5 platforms) |
| `#/ops/screen` Vite SFC + config API | loads, `dashboard-config/list` code=0 | **PASS** |

**API probe coverage (global)**

| Before | After | Script |
|--------|-------|--------|
| 13/55 mapped (42 no-probe gap) | **58/58 mapped** (0 gap on visible CSV rows) | `verify-ops-pages-per-menu.py --api` |

Full visible-menu acceptance after probe extension: **58/58 PASS** (`docs/delivery/ops-acceptance-final-report.json`).

---

## 运营管理（5/5 gate · 6/6 CSV）

| Menu | Route | Primary API | Notes |
|------|-------|-------------|-------|
| 账号分析 | `/ops/account-analysis` | `GET /admin-api/oa/account-analysis/list` | |
| 作者管理 | `/ops/author` | `GET /admin-api/oa/author/list` | |
| 人效盘点 | `/ops/efficiency` | `GET /admin-api/oa/productivity-review/list` | |
| 粉丝分析 | `/ops/fans-analysis` | `GET /admin-api/oa/follower-analysis/list` | theme fix: `#1890ff` → `var(--el-color-primary)` |
| IP组管理 | `/ops/ip-group` | `GET /admin-api/oa/ip-group/tree` | |
| 内部作品分析 *(CSV only)* | `/ops/internal-content` | `GET /admin-api/oa/internal-content/list` | theme fix + new probe mapping |

## 配置管理（8/8）

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

---

## Fixes applied (P1)

1. **Theme**: `FansAnalysis.vue`, `InternalContent.vue` — primary button colors → Element Plus CSS vars.
2. **Probe script**: `scripts/verify-ops-pages-per-menu.py` — `API_PROBE` expanded **13 → 58** paths; added `internal-content`, dashboard, analytics/collect/account modules, etc.; `order-attribution` primary list → `GET /admin-api/oa/football-order/list` (P2b read-only `pay_all_order`; ROI/export仍走 `order-attribution/*`).
3. **Dashboard theme** (special verify): `Dashboard.vue` quick-action icon colors → `var(--el-color-primary*)`.
4. **FunnelAnalysis theme** (full 58 rerun): primary button override fixed.
5. **SOP review integration** (S4-fix8): `production/sop/review.vue` — mock `loadData` replaced with `getSopReviewPending`; table columns aligned to `SopReviewVO`; `approveReview` / `rejectReview` wired.

## Reports

| File | Purpose |
|------|---------|
| `docs/delivery/ops-p1-api-report.json` | P1 module probe JSON |
| `docs/delivery/ops-acceptance-final-report.json` | Full 58-page probe JSON |

## Runbook

```powershell
python scripts/verify-ops-pages-per-menu.py --modules 运营管理,配置管理 --api --json docs/delivery/ops-p1-api-report.json
python scripts/verify-ops-pages-per-menu.py --api --json docs/delivery/ops-acceptance-final-report.json
```

## 内容生产 — SOP 审核（P1 walkthrough 补验）

| Menu | Route | Primary API | Notes | Result |
|------|-------|-------------|-------|--------|
| SOP审核 | `/ops/sop/review` | `GET /admin-api/oa/sop/review/pending` | S4-fix8：`review.vue` mock → 真实 API；列对齐 `planName`/`nodeName`/`reviewerRole`/`createTime`；approve/reject 接通 | **PASS** |

Gateway probe: `code=0`, `data: []`（空队列合法）；Vite `ops/production/sop/review` compile **PASS**（90/90 全量）。

---

## Residual (non-P1)

| Item | Status |
|------|--------|
| 数据质量后端 | Stub empty list — unchanged |
| Dashboard KPI card icon colors in `<script>` | Chart palette `#1890ff` in JS config — style probe green; cosmetic only |
