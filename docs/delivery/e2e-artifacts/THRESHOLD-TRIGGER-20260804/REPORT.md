# ADR-069 P0 E2E — Threshold Trigger After Collect

**Date:** 2026-08-04  
**Environment:** Beta remote DB `110.42.49.224` / `shenyu-ops` · Gateway `:48080` · ops-server `:48094`  
**Login:** admin / admin123 · tenant 1

## Summary

| Verdict | Scope |
|---------|--------|
| **PASS (P0 core)** | Post-collect async threshold hook fires; `sys_notification_event` rows created with dedup `biz_key` |
| **PASS** | External unified collect → fan threshold alert → `collect_config.creator` recipient (Q6) |
| **PASS (after fix)** | Internal unified collect → `WORK_HIT` for `internal-wechat-video:25:HIT` → IP group leader |
| **PARTIAL** | External `WORK_HIT` not observed (beta `oa_external_work.play_count=0`) |
| **FIX applied** | Skip stale `oa_collect_task_account` members so one bad id does not abort internal evaluation |

## Preconditions

1. Rebuilt ops-server JAR (ADR-069 code was newer than running JAR):
   ```powershell
   .\scripts\start-integration-oa.ps1 -Rebuild -Profiles "dev,dev-nacos,dev-nacos-local,dev-local-multidb,dev-test-beta"
   ```
   Or full stack: `.\scripts\start-ops-dev.ps1 -Beta`

2. M8 threshold rules: tenant 1 has ENABLED **FANS** (id 9, WECHAT_VIDEO) and **WORK** (id 10). Defaults apply for DOUYIN.

## E2E runs

### Run 1 — external path + Q6 creator

| Check | Result | Notes |
|-------|--------|-------|
| ops-server health | PASS | `:48094/actuator/health` UP |
| FANS + WORK rules | PASS | 1 ENABLED each |
| External task 9 run | PASS | log `SUCCESS`, 200 records |
| Internal task 8 run | PASS | log `PARTIAL` (cookie/API skips) |
| Threshold notification | PASS | `ACCOUNT_HIGH_FANS` id **11911** |
| biz_key | PASS | `external-account:1:ACCOUNT_HIGH_FANS:2026-08-04` |
| recipient | PASS | `1749825673829120001` (admin / collect_config.creator) |
| Internal WORK_HIT | **FAIL** | Async error: stale member `1000112` → `ServiceException` |

**Data fix (E2E):** `oa_collect_config.creator` for config 42 updated from `system` → admin userId so Q6 recipient resolves.

**Collect side-effect:** external account follower_count updated to ~56.7M during collect → HIGH_FANS (not LOW_FANS).

### Run 2 — after code fix (skip stale members)

| Check | Result | Notes |
|-------|--------|-------|
| Code fix | applied | `CollectThresholdTriggerService.evaluateInternal` skips missing/forbidden account ids |
| Internal task 8 run | PASS | log `PARTIAL` |
| Threshold notification | PASS | `WORK_HIT` id **11913** |
| biz_key | PASS | `internal-wechat-video:25:HIT` (play_count 1,068,733 ≥ 1M default) |
| recipient | PASS | `9160` (IP group 9022 leader) |
| External new event | deduped | Same-day `external-account:1:ACCOUNT_HIGH_FANS:2026-08-04` already claimed in run 1 |

## Pass / fail matrix (final smoke)

| # | Check | Pass |
|---|-------|------|
| 1 | ops-server latest JAR + UP | ✅ |
| 2 | M8 FANS + WORK ENABLED | ✅ |
| 3 | External unified collect SUCCESS | ✅ |
| 4 | Internal unified collect SUCCESS/PARTIAL | ✅ |
| 5 | Post-collect `sys_notification_event` | ✅ |
| 6 | Event type WORK_HIT / ACCOUNT_* | ✅ |
| 7 | Dedup biz_key | ✅ (2nd run no duplicate external key) |
| 8 | External creator notification (Q6) | ✅ (run 1) |
| 9 | Internal WORK_HIT | ✅ (run 2 after fix) |
| 10 | External WORK_HIT | ⚠️ no hit data (play_count=0) |

## Fixes applied during E2E

1. **Data:** `UPDATE oa_collect_config SET creator='<adminUserId>'` for enabled external account configs with non-numeric creator (automated in smoke).
2. **Code:** `CollectThresholdTriggerService.java` — do not call `getRequiredInTenant` on stale task member ids; log and continue.

## Blockers / notes

- **DOUYIN threshold create via API** returned error (validation); existing WECHAT_VIDEO rules + code defaults sufficient for P0.
- **Internal task members** include orphan ids (`1000109`, `1000111`, `1000112`) — caused full internal trigger failure before fix; recommend syncing `oa_collect_task_account` with live `oa_account`.
- **Async delay:** ~2–12s after collect run before events appear; smoke polls up to 12s.
- **Dedup:** Re-running same day does not re-insert same `biz_key`.

## Artifacts

Directory: `docs/delivery/e2e-artifacts/THRESHOLD-TRIGGER-20260804/`

| File | Purpose |
|------|---------|
| `smoke_threshold_trigger.py` | Reusable smoke |
| `00-login.json` | Auth token |
| `RESULTS.json` | Machine-readable checks (latest run) |
| `threshold-fans-list.json` / `threshold-work-list.json` | M8 rules |
| `external-*` / `internal-*` | ensure, task-run, log-page, log-detail |
| `notification-events-before.json` / `notification-events-after.json` | DB snapshot |
| `fix-external-creator.json` | Q6 data fix audit |
| `ops-health.json` | JAR health probe |

## Re-run commands

```powershell
# From repo root — ensure Beta stack + latest ops JAR
.\scripts\start-ops-dev.ps1 -Beta
# Or ops-only rebuild + beta DB:
.\scripts\start-integration-oa.ps1 -Rebuild -Profiles "dev,dev-nacos,dev-nacos-local,dev-local-multidb,dev-test-beta" -SkipNacosPrerequisiteCheck

# E2E smoke
python docs/delivery/e2e-artifacts/THRESHOLD-TRIGGER-20260804/smoke_threshold_trigger.py
```

Manual DB check:

```sql
SELECT id, event_type, biz_key, recipient_user_id, create_time
FROM sys_notification_event
WHERE tenant_id=1
  AND event_type IN ('WORK_HIT','WORK_LOW_SCORE','ACCOUNT_HIGH_FANS','ACCOUNT_LOW_FANS')
ORDER BY id DESC LIMIT 20;
```
