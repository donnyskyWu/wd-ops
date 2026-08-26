# Legacy sys_* Harness Retirement — REPORT (2026-08-25)

## Slices completed

| Slice | Status | Notes |
|-------|--------|-------|
| S1 sys_tenant → TenantCommonApi | ✅ | `OpsTenantFrameworkService` Feign; `TenantCommonApi` registered in `OpsContentConfiguration` |
| S2 position → role Feign | ✅ | `ContentPlanServiceImpl.requireOpsLeader()`; `SopReviewServiceImpl` uses `resolveMemberPosition` |
| S3 Feign-only user bridge | ✅ | `ContentReviewConfigService`, `IpGroupAccessSupport`, `ProductivityReviewServiceImpl`, `AuthorServiceImpl`, perf/order/internal content |
| S4 IT harness @Profile(test) | ✅ | `SysUserMapper`, `SysUserTokenMapper`, `SysRoleMapper`, `SysTenantMapper` → `@Profile("test")`; optional injection in validator/login assembly |
| S5 V191 + deploy pack | ✅ | Migration SQL, `ops-flyway-migration-order.txt`, `gen-ops-greenfield-sql.py`, `01-shenyu-ops-schema.sql`, `verify-schema.sql`, `drop-ops-legacy-sys-tables.sql`, `OPERATIONS-GUIDE.md` |

## Build / test

| Check | Result |
|-------|--------|
| `mvn -pl football-module-ops-server -am compile` | **PASS** |
| `mvn -pl football-module-ops-server -am test` | **6 errors** — pre-existing `WorkTaskServiceImplTest` / `WorkTaskWinPredictionServiceTest` (MybatisPlus lambda cache + Mockito stubbing); **not introduced by this slice** |
| `mvn package` | **BLOCKED** — `football-module-ops-server.jar` locked by running `:48094` process |

## E2E

Stack was **already running** (prior build) at session start — **does not include this diff** until rebuild + restart.

**Follow-up (2026-08-25):** Rebuilt JAR + restarted Beta stack; smoke via `scripts/integration-config/smoke_legacy_harness_retire_e2e.py`.

| Check | Result |
|-------|--------|
| Login (admin/admin123) | PASS |
| `GET /ops/author/list` | PASS (total=33) |
| `GET /ops/ip-group/list` | PASS (total=6) |
| `GET /ops/private-domain-report/monthly-achievement` | PASS (rows=2) |
| `GET /ops/private-domain-report/weekly-funnel` | **PASS** (6 rows; 需 `weekStart`+`weekEnd`，非 `weekLabel`) |
| `GET /ops/report/live-duration/list` | PASS (total=30 stub) |
| `GET /ops/work-task/sheet/get-or-create` | **PASS** (动态 ipGroupId) |
| `GET /actuator/health` :48094 | PASS UP |

**Note:** Beta profile has **Flyway off** — V191 DROP 需手工跑 `drop-ops-legacy-sys-tables.sql` §2 或启用 Flyway 后重启。

**User must run:**

```powershell
# Stop stack (releases JAR lock)
.\scripts\stop-integration-all.ps1

# Rebuild + restart Beta stack
cd football-backend-saas
mvn -pl football-module-ops/football-module-ops-server -am package -DskipTests
cd ..
.\scripts\start-ops-dev.ps1 -Beta
```

**Smoke APIs (after restart, Gateway + tenant 1):**

- Author list: `GET /admin-api/ops/author/list`
- IP group: `GET /admin-api/ops/ip-group/list`
- Data report: `GET /admin-api/ops/report/private-domain/monthly-achievement`
- Work task: `GET /admin-api/ops/work-task/sheet/list`

Flyway on existing DB: JAR startup applies **V191** (DROP identity harness). Greenfield: `01-shenyu-ops-schema.sql` now includes V191.

## Key files changed (football-module-ops only + deploy docs)

- `framework/common/biz/system/tenant/TenantCommonApi.java` (new)
- `framework/tenant/job/OpsTenantFrameworkService.java`
- `config/OpsContentConfiguration.java`
- `service/plan/ContentPlanServiceImpl.java`
- `service/sop/SopReviewServiceImpl.java`
- `service/content/ContentReviewConfigService.java`
- `service/ipgroup/IpGroupAccessSupport.java`
- `service/support/FootballSystemUserValidator.java`
- `service/auth/LoginUserAssemblySupport.java`
- `service/operations/ProductivityReviewServiceImpl.java`
- `service/author/AuthorServiceImpl.java`
- `service/perf/*`, `service/operations/InternalContentServiceImpl.java`
- `dal/mysql/auth/*`, `dal/mysql/system/SysTenantMapper.java` (@Profile test)
- `db/migration/V191__drop_legacy_sys_identity_harness.sql`
- Deploy: `docs/deploy/ops-greenfield-production/*`, `scripts/integration-config/*`
