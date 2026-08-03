# P5-MIGRATE-9 Analytics / ROI · Report

Date: 2026-07-31 · SSOT: ADR-058 §4.3

## Scope

Replace stubs for UI-hot ROI / finance / order-attribution paths in monorepo `football-module-ops-server` (:48094).

**In scope**

- `FinanceRoiController` + service（`/admin-api/oa/finance/roi/**`）
- `AccountCostController` + service（`/admin-api/oa/finance/cost/**`）
- `OrderAttributionController` + service（`/admin-api/oa/order-attribution/**`）
- DO/Mapper：`oa_account_cost` / `oa_order_attribution` / `oa_order`（wd only）
- Remove matching paths from `DeferredCutoverStubController`

**Out of scope（仍 stub）**

- Dashboard / HomeDashboard / Metric / Funnel / Report / Monitor / Screen
- AccountAnalysis / ContentAnalysis / FollowerAnalysis / WechatAnalysis
- PerfRecord / PerfResult / PerfTemplate
- P6 权限码、旧模块删除、B-WP4 DROP

## Fix during smoke

`OrderAttributionServiceImpl.roi`：空 `byGroup` 时跳过 `selectBatchIds([])`，避免 MySQL `IN ()` 语法错误 → code=500。

## Smoke（Gateway `/admin-api/ops/...`）

| API | code | note |
|-----|------|------|
| finance/roi/analysis | 0 | totalCost=379.02（账号成本） |
| finance/roi/trend | 0 | |
| finance/roi/breakdown | 0 | |
| finance/cost/list | 0 | total=5 |
| order-attribution/list | 0 | total=0（表空/范围内无归因） |
| order-attribution/roi | 0 | 空结果安全 |
| football-order/list | 0 | total=26341（回归） |
| content/list | 0 | total=37 |
| account/list | 0 | total=182 |

Artifacts: 本目录 `*.json` / `RESULTS.json`。

## Runtime

- Nacos：`ops-server` · namespace=`local` · `version=cutover-p5-migrate-9`
- 回滚：`.\scripts\start-integration-oa.ps1 -UseLegacyOa`
- Build：`mvn -pl football-module-ops/football-module-ops-server -am package` 绿

## Next

建议下一刀：**Dashboard / Home** 或 **Perf template/record**（按菜单优先级）；勿跨域。
