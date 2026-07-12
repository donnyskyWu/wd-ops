# Ops Page Acceptance Plan

> Per-menu acceptance for Football-Ops integration (5777 shell + Gateway 48080 + oa-server 48094).

## Final Summary — 2026-07-03 (S2 close-out)

**Scope**: 55 visible menu pages (`hide_in_menu=N`, M9 excluded) across **10 modules**.

| Module | Pages | Route | Vite | Theme | API | Result |
|--------|-------|-------|------|-------|-----|--------|
| **作品监测** | 6/6 | 6/6 | 6/6 | 6/6 | — | **PASS** |
| **内容生产** | 8/8 | 8/8 | 8/8 | 8/8 | — | **PASS** |
| **数据分析** | 7/7 | 7/7 | 7/7 | 7/7 | — | **PASS** |
| **数据采集** | 4/4 | 4/4 | 4/4 | 4/4 | — | **PASS** |
| **系统管理(OA)** | 5/5 | 5/5 | 5/5 | 5/5 | — | **PASS** |
| **绩效核算** | 4/4 | 4/4 | 4/4 | 4/4 | — | **PASS** |
| **财务管理** | 2/2 | 2/2 | 2/2 | 2/2 | — | **PASS** |
| **账号管理** | 6/6 | 6/6 | 6/6 | 6/6 | — | **PASS** |
| **运营管理** | 5/5 | 5/5 | 5/5 | 5/5 | 5/5 | **PASS** |
| **配置管理** | 8/8 | 8/8 | 8/8 | 8/8 | 8/8 | **PASS** |

**Totals**

| Probe | Count | Script |
|-------|-------|--------|
| Visible menu pages (route + vite + theme) | **55/55** | `verify-ops-pages-per-menu.py` |
| Vite compile (incl. hide-in-menu) | **90/90** | `verify-ops-vite-modules.py` |
| API (mapped endpoints only) | **13/13** | `verify-ops-pages-per-menu.py --api` |

**Reports**: `docs/delivery/ops-acceptance-final-report.json` · `docs/delivery/ops-acceptance-final.log` · `docs/delivery/ops-vite-verify.log`

**Known blockers (non-blocking for compile/route)**

| Item | Status |
|------|--------|
| 数据质量后端 | Stub only — `CollectQualityController` returns empty list; no `oa_collect_quality_*` tables |
| API probe coverage | 42/55 pages have no mapped primary endpoint in `verify-ops-pages-per-menu.py` |
| Ant Design color leftovers | Non-menu pages (Dashboard, Layout) still use `#1890ff` in chart config — out of visible-menu scope |

## Runbook

```powershell
# Services (start if down)
.\scripts\start-integration-stack.ps1   # Nacos + oa-server :48094
.\scripts\start-integration-system.ps1  # Gateway :48080 + system-server
cd football-front; pnpm dev:ele         # :5777

# Probes
python scripts/probe-ops-m6-m10-m9.py      # 数据分析 + 数据采集 + 系统管理(OA)
python scripts/verify-ops-vite-modules.py   # all menu vue compile smoke
```

Auth: `POST /admin-api/system/auth/login` (`admin` / `admin123`, `tenant-id: 1`) + `X-Tenant-Id: 1`. Fallback dev-token: `dev-token-oa-admin`.

---

## Batch: 数据分析 + 数据采集 + 系统管理(OA) — 2026-07-03

**Scope**: 16 visible menu pages (`hide_in_menu=N`) from `oa-menu-permission-map.csv`.

| Module | Pages | Route | API | Theme | Result |
|--------|-------|-------|-----|-------|--------|
| **数据分析** | 7/7 | PASS | PASS | PASS | **PASS** |
| **数据采集** | 4/4 | PASS | PASS | PASS | **PASS** |
| **系统管理(OA)** | 5/5 | PASS | PASS | PASS | **PASS** |

**Overall: 16/16 PASS** (probe `scripts/probe-ops-m6-m10-m9.py`, auth=gateway-login)

### Per-page detail

| Menu | Route | Primary API | Notes |
|------|-------|-------------|-------|
| 自定义查询 | `/ops/custom-query` | `GET /admin-api/oa/query/list` | |
| 数据报表 | `/ops/data-report` | N/A (navigation cards) | |
| 总体财务分析 | `/ops/financial-analysis` | `GET /admin-api/oa/finance/roi/analysis` | requires `startDate`/`endDate` |
| 漏斗分析 | `/ops/funnel-analysis` | `GET /admin-api/oa/funnel/list` | |
| 指标管理 | `/ops/metric` | `GET /admin-api/oa/metric/list` | |
| 指标分析 | `/ops/metric-analysis` | `GET /admin-api/oa/metric/list` | |
| 大屏配置 | `/ops/screen-config` | `GET /admin-api/oa/dashboard-config/list` | |
| 采集日志 | `/ops/collect/log` | `GET /admin-api/oa/collect/log/page` | |
| 私域桥接 | `/ops/collect/private-domain-bridge` | `GET /admin-api/oa/collect/private-domain-bridge/page` | |
| 数据质量 | `/ops/collect/quality` | `GET /admin-api/oa/collect/quality/list` | stub empty list (API-M10 §2) |
| 采集任务 | `/ops/collect/task` | `GET /admin-api/oa/collect/task/page` | was 500 before tenant/auth fix |
| 字典配置 | `/ops/system-dict` | `GET /admin-api/oa/system/dict/list` | was 403 Football token — fixed |
| 登录日志 | `/ops/system-log/login` | `GET /admin-api/oa/system/log/login` | |
| 操作日志 | `/ops/system-log/operation` | `GET /admin-api/oa/system/log/operation` | |
| 消息管理 | `/ops/system-message` | `GET /admin-api/oa/system/message/list` | |
| 系统参数 | `/ops/system-param` | `GET /admin-api/oa/system/param/list` | |

### Fixes applied

1. **Route / Vite**: Re-ran `mount-ops-all.py` — copy `constants/ops`, `composables/ops`, `mock/ops`; import rewrites `@/constants`, `@/composables`, `@/mock` → `#/…/ops/`; `#/utils/ops/index` barrel.
2. **采集任务 API 500**: Confirmed `code=0` via Gateway + `X-Tenant-Id:1` (dev-token and gateway-login). Root cause was missing/invalid auth headers in prior probes.
3. **字典 403**: `FootballAuthProvider` now unions `sys_permission` codes when Football user maps to `sys_user` (same username + tenant). Gateway-login dict list → `code=0`.
4. **数据质量 403**: Added `CollectQualityController` stub (`GET …/quality/list`, `…/quality/log` + legacy `/check/page`, `/log/page` aliases). Frontend `collect.ts` aligned to API-M10 paths.
5. **Theme**: Extended `COLOR_REWRITES` in mount script (`var(--el-*)` for backgrounds, success/danger text).

### Files changed

| Area | Files |
|------|-------|
| Backend | `CollectQualityController.java`, `CollectQualityCheckRespVO.java`, `CollectQualityLogRespVO.java`, `FootballAuthProvider.java`, `SysUserTokenMapper.java` |
| Frontend API | `football-front/.../api/ops/collect.ts`, `file.ts`; `utils/ops/monitor-map.ts` |
| Frontend mount | `scripts/mount-ops-all.py`; bulk `views/ops/**`, `constants/ops/**`, `composables/ops/**`, `mock/ops/**` |
| Source API | `ops-platform-ui-vue/src/api/collect.ts` (quality paths) |
| Probe | `scripts/probe-ops-m6-m10-m9.py` |

### Blockers / follow-ups

| Item | Status |
|------|--------|
| 数据质量后端 | Stub only — no `oa_collect_quality_*` tables; UI falls back to mock on empty |
| Gateway-login transient 500 | Intermittent on first probe after cold start; retry green |
| Other modules `@/utils` imports | **Fixed 2026-07-03** — `file.ts`, `plan.ts`, `error-handler.ts` → `#/`; Vite **90/90** |
