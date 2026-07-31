# B-WP4-ARCHIVE · remote beta `110.42.49.224`

| 项 | 值 |
|----|-----|
| Host | `110.42.49.224:3306` |
| Master DB | `shenyu-ops`（原 `wd`；无独立 `wd` 库） |
| User（项目 env） | `shenyu-ops`（`ops-test-remote.env`） |
| Server | MySQL **5.7.44-log** |

## Scripts

| File | Purpose |
|------|---------|
| `02-stop-write-readonly.sql` | Q1 #1–3 / #5 / #7 stop-write triggers on `shenyu-ops` |
| `03-rename-sys-operation-log.sql` | Q1 #4 → `archive_wd.sys_operation_log`（utf8mb4_general_ci） |

## Privilege blockers (2026-07-31)

`shenyu-ops@%` grants: `USAGE` + `ALL ON shenyu-ops.*` only.

| Action | Error | Unblock |
|--------|-------|---------|
| `CREATE TRIGGER` | `ERROR 1419` SUPER / `log_bin_trust_function_creators` | DBA: `SET GLOBAL log_bin_trust_function_creators=1` **or** run as elevated user |
| `CREATE DATABASE archive_wd` | `ERROR 1044` Access denied | DBA: create `archive_wd` + `GRANT ALL ON archive_wd.* TO 'shenyu-ops'@'%'` **or** run rename as elevated user |

Do **not** invent alternate same-schema renames without product sign-off (localhost parity = `archive_wd`).

## Re-run (after DBA unlock)

```powershell
# load ops-test-remote.env then:
$mysql = "mysql"  # or full path
# prefer elevated user for both files:
Get-Content docs/delivery/e2e-artifacts/B-WP4-ARCHIVE-20260731/sql/remote-110/02-stop-write-readonly.sql -Raw | & $mysql -h 110.42.49.224 -P 3306 -u <elevated> 
Get-Content docs/delivery/e2e-artifacts/B-WP4-ARCHIVE-20260731/sql/remote-110/03-rename-sys-operation-log.sql -Raw | & $mysql -h 110.42.49.224 -P 3306 -u <elevated>
```
