# B-WP4-ARCHIVE · remote beta `110.42.49.224`

| 项 | 值 |
|----|-----|
| Host | `110.42.49.224:3306` |
| Master DB | `shenyu-ops`（原 `wd`；**不**建 `archive_wd`） |
| User（项目 env） | `shenyu-ops`（`ops-test-remote.env`） |
| Server | MySQL **5.7.44-log** |
| 归档策略（2026-07-31 澄清） | **同库** `RENAME TABLE … TO archive_<name>`（可回查）；无 SUPER → 不用 trigger |

## Scripts

| File | Purpose |
|------|---------|
| `02-stop-write-readonly.sql` | **DEPRECATED on remote** — needs SUPER / `log_bin_trust_function_creators`（ERROR 1419） |
| `03-rename-sys-operation-log.sql` | Q1 #4 → `archive_sys_operation_log`（同库） |
| `04-rename-q1-legacy-same-schema.sql` | Q1 #1/#3/#5 → `archive_sys_*` / `archive_sys_dict_*` |
| `05-rollback-same-schema.sql` | 可选回滚注释模板 |

## #7 `system_users` — SKIP RENAME

| Check | Result |
|-------|--------|
| Exists in `shenyu-ops` | Yes（19 rows） |
| SSOT | `shenyu-system.system_users`（132 rows；ADR-056） |
| App dependency | **`FootballOAuth2MasterTokenMapper` (@DS master)** 仍 SELECT `system_users` + `system_role` / `system_user_role` / `system_menu` / `system_oauth2_access_token` |
| Overlay cluster kept | `system_users`, `system_role`, `system_user_role`, `system_menu`, `system_role_menu`, `system_oauth2_access_token` |
| Decision | **不 RENAME / 不 DROP**（业务仍在读 overlay；Mapper 代码按 Q4 保留） |

## Privilege notes

`shenyu-ops@%`：`USAGE` + `ALL ON shenyu-ops.*` only — 足够同库 RENAME/DROP；**不足** CREATE DATABASE / CREATE TRIGGER（无 SUPER）。

## Execute

```powershell
# load ops-test-remote.env then:
$mysql = "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe"
# per-table runner preferred (see REPORT §5); or:
Get-Content .../03-rename-sys-operation-log.sql -Raw | & $mysql -h 110.42.49.224 -P 3306 -u shenyu-ops -p... shenyu-ops
Get-Content .../04-rename-q1-legacy-same-schema.sql -Raw | & $mysql -h 110.42.49.224 -P 3306 -u shenyu-ops -p... shenyu-ops
```
