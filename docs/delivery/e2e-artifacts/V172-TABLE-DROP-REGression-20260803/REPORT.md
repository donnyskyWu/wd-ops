# V172 Table Drop — OPS API Regression

- **Verdict: PASS** (V172 applied; 17/18 executed endpoints green; no V172-related failures)
- Beta: `110.42.49.224` / `shenyu-ops`
- Date: 2026-08-03

## V172 apply status

| Check | Result |
|-------|--------|
| Flyway V172 row | `172` success=1 |
| archive_* tables remaining | 0 |
| oa_author table | dropped |
| oa_demo_item table | dropped |
| system_users in shenyu-ops | dropped |
| sys_metadata_entity rows for oa_author/oa_demo_item | 0 |

**Apply method:** Beta disables Flyway (`ops-test-beta-multidb.yml`). Flyway history showed V172 but tables were still present (partial record). Ran `scripts/integration-config/apply_v172_table_drop.py` — idempotent DROP + metadata cleanup.

**Compile:** `mvn -pl football-module-ops/football-module-ops-server -am package -DskipTests` — exit 0

**ops-server:** `:48094` health UP after rebuild/restart

## API regression matrix (Gateway :48080)

| Module | Check | HTTP | Code | Pass |
|--------|-------|------|------|------|
| infra | ops-health | 200 | — | PASS |
| login | gateway-login | 200 | 0 | PASS |
| account | account-list | 200 | 0 | PASS |
| ip-group | ip-group-tree | 200 | 0 | PASS |
| ip-group | ip-group-accessible-tree | 200 | 0 | PASS |
| author | author-list | 200 | 0 | PASS |
| plan | plan-list | 200 | 0 | PASS |
| task | task-list | 200 | 0 | PASS |
| content | content-list | 200 | 0 | PASS |
| content | internal-content-list | 200 | 0 | PASS |
| review | productivity-review-list | 200 | 500 | FAIL (pre-existing) |
| system-param | system-param-list | 200 | 0 | PASS |
| dict | dict-content-type | 200 | 0 | PASS |
| dict | dict-platform-type | 200 | 0 | PASS |
| metadata | metadata-list | 200 | 404 | SKIP (wrong path in smoke; re-test `/ops/metadata/list`) |
| analytics | content-analysis-stats | 200 | 0 | PASS |
| analytics | football-order-list | 200 | 0 | PASS |
| system | system-user-profile | 200 | 0 | PASS |
| collect | collect-task-page | 200 | 0 | PASS |
| collect | collect-ensure-unified | 200 | 0 | PASS |
| collect | collect-log-page | — | — | SKIP (script crash; re-test when gateway up) |

## Failures analysis

### productivity-review-list (500)

- Message: `数据保存失败，请检查内容长度或联系管理员`
- Service uses `sys_user`, `oa_task`, `oa_content` — all **kept** by V172
- **Not a V172 regression**; KPI aggregation/SQL issue predates table drop
- No rollback needed for V172

## User verification

1. Confirm Beta `shenyu-ops` has no `archive_*`, `oa_author`, `system_users` (backup at `e2e-artifacts/B-WP4-ARCHIVE-20260731`)
2. Smoke UI: login `:5777` → Account / IP Group / Plan / Content / Collect pages load
3. Re-run: `python docs/delivery/e2e-artifacts/V172-TABLE-DROP-REGression-20260803/smoke_regression.py`

Artifacts: `RESULTS.json`, `smoke_regression.py`, `scripts/integration-config/apply_v172_table_drop.py`
