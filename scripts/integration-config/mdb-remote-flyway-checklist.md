# Remote Flyway V131/V132 Checklist — 101.37.161.136

> **Do NOT auto-run.** User executes after backup + four-db import. Localhost is reference.

## Pre-flight

- [ ] `mysqldump` backup: `wd` + `shenyu-member` + `shenyu-mp` + `shenyu-pay` + `shenyu-system`
- [ ] Four Football DBs created and imported from `docs/sql/shenyu-*.sql`
- [ ] Connection test: `.\scripts\test-remote-mysql-connection.ps1`
- [ ] oa-server **stopped** or on single-DB profile during Flyway

## Check current Flyway version (remote wd)

```powershell
$env:MYSQL_PWD = '<password>'
mysql -h 101.37.161.136 -P 3306 -u shenyu -e "SELECT version, description, success FROM wd.flyway_schema_history ORDER BY installed_rank DESC LIMIT 10;"
```

## V131 — author ext PK + oa_account_ext (if not applied)

```powershell
# Option A: start oa-server once (Flyway auto-migrate) — preferred
# Option B: manual via Maven (from football-module-ops-server, point datasource to remote — user only)
mvn -pl football-module-ops/football-module-ops-server flyway:migrate `
  -Dflyway.url="jdbc:mysql://101.37.161.136:3306/wd?..." `
  -Dflyway.user=shenyu -Dflyway.password=<pwd>
```

**Post V131 verify:**

```sql
-- PK = author_user_id (not id)
SHOW CREATE TABLE wd.oa_author_ext;
SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='wd' AND table_name='oa_account_ext';
```

## V132 — cutover DROP replicas (after Nacos multidb + smoke)

```sql
-- Confirm oa-server reads member/mp/pay/system @DS before DROP
SELECT COUNT(*) FROM wd.oa_author;          -- expect >0 before V132
SELECT COUNT(*) FROM shenyu-member.author_user;  -- SSOT must exist
```

Apply: restart oa-server with remote multidb profile → Flyway runs V132, OR manual:

```powershell
mysql -h 101.37.161.136 -P 3306 -u shenyu -p wd < football-backend-saas/football-module-ops/football-module-ops-server/src/main/resources/db/migration/V132__mdb_s4_cutover_drop_replicas.sql
# Then insert flyway_schema_history row if not using Flyway auto — prefer auto via oa-server start
```

**Post V132 verify:**

```sql
SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='wd' AND table_name='oa_author';  -- 0
SELECT version FROM wd.flyway_schema_history WHERE version IN ('131','132') AND success=1;
```

## Rollback

- Restore pre-cutover `mysqldump` of `wd`
- Revert oa-server Nacos to single-DB `application-dev.yml`
- V132 has no Flyway down — restore from backup only

## Acceptance (remote)

- [ ] `:5777` login admin/admin123 tenant 1
- [ ] `#/ops/author` total ≥ Football member authors
- [ ] `#/ops/internal-account` 微信 ≥ 187
- [ ] `run-uat-football-e2e.ps1` against remote stack → **58/58**
