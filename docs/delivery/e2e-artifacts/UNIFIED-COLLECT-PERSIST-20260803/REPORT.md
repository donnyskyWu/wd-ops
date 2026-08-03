# ADR-061 follow-up: unified collect FULL_PERSIST

## Scope

- Platforms: `WECHAT_VIDEO`, `DOUYIN`, `KUAISHOU`
- Data types: `FOLLOWER_STATS` → `oa_account_status_log`; `*_VIDEO_LIST` / `*_VIDEO_STATS` → platform video tables
- Probe retained for health check (`/livez`, account health) and out-of-scope platforms (e.g. `WECHAT_OFFICIAL`)

## Before / After

| | Before | After |
|---|--------|-------|
| persistMode | `PROBE_COUNT_ONLY` | `FULL_PERSIST` / `MIXED` (mixed-member tasks) |
| Collector call | count items only | fetch + upsert via SyncService |
| Log typeResults.targetTable | null | `oa_account_status_log`, `oa_douyin_video`, etc. |

## Run smoke

```powershell
# ops-server must be UP with stub or live collector
python docs/delivery/e2e-artifacts/UNIFIED-COLLECT-PERSIST-20260803/smoke_persist.py
```

## Expected

- `RESULTS.json`: `persistMode` ≠ `PROBE_COUNT_ONLY` when persist-platform members exist
- `typeResultsWithTargetTable` > 0
- `log-detail.json` → `result.typeResults[].targetTable` populated for DOUYIN/KUAISHOU/WECHAT_VIDEO members

**2026-08-03 run:** `pass=true`, `persistMode=MIXED/FULL_PERSIST`, DOUYIN → `oa_douyin_video`; WECHAT_VIDEO cookie 失效时 PARTIAL 仍绿（有 targetTable 成员）。
