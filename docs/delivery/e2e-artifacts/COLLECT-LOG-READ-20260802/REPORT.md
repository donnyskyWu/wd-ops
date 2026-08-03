# COLLECT-LOG-READ-20260802

**Status**: FIXED · ADR-060 §5.2 carve-out  
**Date**: 2026-08-02 · Beta `shenyu-ops` + ops-server `:48094` · Gateway `:48080`

## Root cause

| 层 | 事实 |
|----|------|
| FE | 「立即执行」成功后提示可查看日志；「查看日志」→ `CollectLog` · `GET /ops/collect/log/page?taskId=` |
| BE run | `UnifiedCollectRunService` **已写** `oa_collect_log`（含 `result_json` / `PROBE_COUNT_ONLY`） |
| BE read | `DeferredCutoverStubController` 仍 stub `GET /admin-api/ops/collect/log/**` → **空分页** |

故 DB 有行、UI 无数据。

## Fix

- 新增 `CollectLogController` / `CollectLogService*`（page + detail，解析 `result_json`）
- stub 卸掉 `/collect/log/**`
- ADR-060 §5.2 · ADR-061 follow-up 已记

## Smoke

| 检查 | 结果 |
|------|------|
| `POST …/task/ensure-unified` | ✅ taskId=`8` |
| `POST …/task/8/run` | ✅ code=0 |
| `GET …/collect/log/page?taskId=8` | ✅ total **3**（run 后 +1）· 非 stub 空页 |
| `GET …/collect/log/18` | ✅ `result.persistMode=PROBE_COUNT_ONLY` · `typeResults`=3 |

本地 unify-collector `:8000` 不可达 → 日志 status=`FAILED`（预期有行即可）。

Artifacts：`00-login.json` · `ensure-unified.json` · `task-run.json` · `log-page-before/after.json` · `log-detail.json` · `RESULTS.json`。

## Residual

- SyncService 全量落库仍 `PROBE_COUNT_ONLY`
- collect quality / bridge / EXTERNAL config 仍 ADR-060 stub
