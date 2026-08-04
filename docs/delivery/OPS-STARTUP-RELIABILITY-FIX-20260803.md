# OPS Startup Reliability Fix (2026-08-03)

Permanent fixes for recurring `start-ops-dev.ps1` / `start-ops-dev.ps1 -Beta` false failures and crash loops.

## Root causes (ranked)

| Rank | Blocker | Evidence | Fix |
|------|---------|----------|-----|
| 1 | **Actuator 503 false negative** | `/actuator/health` returns HTTP 503 when Nacos/Redis down; `Invoke-WebRequest` threw → `Wait-HttpEndpoint` timed out at 120s while ops-server was serving on :48094 | `Invoke-IntegrationHttpProbe` reads 503 body; accept degraded health + fallback `/v3/api-docs` |
| 2 | **Maven JAR lock on `-FirstRun`** | Terminal 21: `Unable to rename football-module-pay-server.jar` — pay-server :48085 still running | `stop-integration-all.ps1` now stops **48085**; pre-build port cleanup in `start-integration-all.ps1` |
| 3 | **Integration build fail → misleading final table** | Terminal 21: Maven failed but script continued to frontend retry + `NOT READY` | `start-ops-dev.ps1` **fail-fast** when `start-integration-all` exits non-zero |
| 4 | **Flyway V173/V175 crash loop** | Prior: `sys_dict_data` insert on read-only beta/local; failed row blocks boot | V173/V175 SQL: no dict inserts; V175 idempotent ALTERs; auto `repair-flyway-failed` preflight |
| 5 | **Beta Flyway disabled + schema drift** | Beta ops uses `--spring.flyway.enabled=false` | Preflight: `apply_v173_live_collect.py` + `apply_v175_external_collect.py` on `-Beta` |
| 6 | **Redis password / Gateway** | Existing preflight (unchanged) | `Ensure-IntegrationRedis` → password 123456 |
| 7 | **member-server mock vs JAR** | Python mock :48087 → article 404 | Default FullMemberServer; `-UseMemberMock` opt-in |
| 8 | **`-FirstRun` vs default `-SkipBuild`** | Daily run skips Maven; stale/missing JAR after big pull | Docs + fail-fast hint on build failure |

## Before / after

### Health wait (`start-integration-oa.ps1`)

**Before:** Only HTTP 2xx counted as ready → 503 = timeout → exit 1.

**After:**

1. Probe `/actuator/health` (accept 503 with JSON `"status"` when :48094 listens)
2. Fallback `/v3/api-docs` (permitAll in ops SecurityConfig)
3. Final table treats degraded ops as **UP**

### Stop + build

**Before:** `stop-integration-all.ps1` omitted pay-server **48085** → Maven repackage failed.

**After:** Port 48085 stopped on restart; explicit pre-build listener cleanup.

### Flyway

**Before:** Manual `DELETE FROM flyway_schema_history WHERE success=0` on local/beta.

**After:** Automatic preflight each start:

- **Local:** `python scripts/integration-config/repair-flyway-failed.py --local`
- **Beta:** `repair-flyway-failed.py` + `apply_v173_live_collect.py` + `apply_v175_external_collect.py`

## Files changed

| File | Change |
|------|--------|
| `scripts/lib/integration-preflight.ps1` | Degraded health probes, `Ensure-OpsFlywayPreflight`, ops fallback URL |
| `scripts/start-ops-dev.ps1` | Fail-fast on integration failure; pass FallbackUrl to health checks |
| `scripts/start-integration-all.ps1` | Flyway preflight, pre-build port stop, ops wait with fallback |
| `scripts/start-integration-oa.ps1` | Ops wait with fallback + port |
| `scripts/stop-integration-all.ps1` | Include **48085** pay-server |
| `scripts/integration-config/repair-flyway-failed.py` | `--local` for localhost |
| `football-backend-saas/.../V175__m10_external_unified_collect_task.sql` | Idempotent ALTERs (beta manual apply safe) |

## Commands

### Daily local (default)

```powershell
.\scripts\start-ops-dev.ps1
```

Skips Maven (`-SkipBuild`), restarts stack, auto Flyway failed-row repair on local DB.

### After backend pull / first machine

```powershell
.\scripts\start-ops-dev.ps1 -FirstRun
```

Runs Maven (stops :48085 first), longer health wait (240s cap).

### Beta remote DB

```powershell
.\scripts\start-ops-dev.ps1 -Beta
```

Requires `scripts/integration-config/ops-test-remote.env`. Preflight applies V173/V175 on 110.42.49.224 when Flyway is off.

### One-time repair (only if ops-server still crash-loops on Flyway)

**Local:**

```powershell
python scripts/integration-config/repair-flyway-failed.py --local
# If V173/V175 partially applied on beta manually:
python scripts/integration-config/apply_v173_live_collect.py
python scripts/integration-config/apply_v175_external_collect.py
```

**Beta (Flyway disabled):**

```powershell
python scripts/integration-config/repair-flyway-failed.py
python scripts/integration-config/apply_v173_live_collect.py
python scripts/integration-config/apply_v175_external_collect.py
```

### Stop / clean restart

```powershell
.\scripts\stop-integration-all.ps1
.\scripts\start-ops-dev.ps1
```

## Verification (2026-08-03)

- Local `repair-flyway-failed.py --local` → `[ok] No failed Flyway migrations`
- Health probe against running :48094 → `ready=True`, `status=UP`
- Terminal 1 prior run: `start-ops-dev.ps1 -NoRestart` → `=== START OK ===` (all critical UP)

## Expected behavior now

- **503 actuator** with ops listening → **START OK** (degraded Nacos/Redis is non-blocking for dev Gate path)
- **Maven lock** on `-FirstRun` → prevented by stopping 48085
- **Build failure** → immediate `START FAILED` with actionable hint (no false frontend-only success)
- **Beta** → schema preflight before ops-server JVM start

## Related docs

- `docs/delivery/OPS-DEV-DEPLOY-GUIDE.md`
- `docs/delivery/OPS-TEST-DB.md`
- `docs/adr/ADR-067-M10-直播采集-DOUYIN-WECHAT_VIDEO.md`
- `docs/adr/ADR-068-M10-统一外部数据采集任务.md`
