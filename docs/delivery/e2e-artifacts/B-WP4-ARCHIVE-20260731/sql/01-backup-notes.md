# 01 Backup

```powershell
# From repo root — localhost only
mysqldump -h 127.0.0.1 -P 3306 -u root -proot `
  --single-transaction --triggers --set-gtid-purged=OFF `
  --result-file=docs/delivery/e2e-artifacts/B-WP4-ARCHIVE-20260731/backup/wd-q1-candidates-20260731.sql `
  wd sys_user sys_user_token sys_user_role sys_role sys_role_permission `
     sys_permission sys_operation_log sys_dict_type sys_dict_data system_users
```

Restore example:

```powershell
mysql -h 127.0.0.1 -P 3306 -u root -proot wd < docs/delivery/e2e-artifacts/B-WP4-ARCHIVE-20260731/backup/wd-q1-candidates-20260731.sql
```

> After `#4` RENAME, restoring `sys_operation_log` into `wd` may conflict with `archive_wd.sys_operation_log` — drop/rename archive first or restore only needed tables.
