# ADR-067 Live Collect E2E — 2026-08-03

## Scope

| Step | Check |
|------|-------|
| 1 | Flyway V173 applied (`collect_live_enabled`, `oa_douyin_live`, `oa_wechat_video_live`) |
| 2 | DOUYIN/WECHAT_VIDEO account: enable `collectLiveEnabled` via API |
| 3 | Unified task run → log `typeResults` contains `DOUYIN_LIVE_LIST` / `targetTable=oa_douyin_live` (when member + bind OK) |
| 4 | `GET /admin-api/ops/internal-content/list?contentType=LIVE&platformType=DOUYIN` returns live rows |

## Smoke

```bash
python docs/delivery/e2e-artifacts/LIVE-COLLECT-20260803/smoke_live_collect.py
```

**Environment**: gateway `:48080` · ops `:48094` · collector `:8000` (or stub mode)

## Expected log excerpt (collect_live_enabled=1, DOUYIN)

```json
{
  "dataType": "account:3/DOUYIN_LIVE_LIST",
  "success": true,
  "targetTable": "oa_douyin_live"
}
```

## Blockers / gaps

| Item | Status |
|------|--------|
| Collector `douyin-live/live-list` production cookie | May fail if account_id / sec_uid gap — verify bind + externalAccountId |
| WECHAT_VIDEO live | Cookie expiry → PARTIAL (same as video list) |
| Compile in CI | Run `mvn -pl football-module-ops-server -am compile` in football-backend-saas |

## Files touched (this slice)

See parent agent REPORT for full inventory.
