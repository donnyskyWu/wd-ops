# sync-local-db-s20.ps1 — 本地 MySQL 同步 S-20 / ADR-074~076 数据变更
#
# 用途: 无法连 Beta 测试库时，在 localhost 补齐今日功能相关的结构+字典+验证 seed。
# 前提: MySQL localhost:3306 root/root；库 shenyu-ops、shenyu-sys 已存在。
#
# 用法（仓库根目录）:
#   .\scripts\sync-local-db-s20.ps1
#   .\scripts\sync-local-db-s20.ps1 -RestartOps   # 同步后重启 ops-server 使 Flyway/缓存生效

[CmdletBinding()]
param(
    [switch]$RestartOps
)

$ErrorActionPreference = "Stop"
$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$SqlFile = Join-Path $PSScriptRoot "integration-config\local-sync-s20-data.sql"

if (-not (Test-Path -LiteralPath $SqlFile)) {
    Write-Error "Missing $SqlFile"
    exit 1
}

Write-Host "=== Local DB sync (S-20 / ADR-074~076) ===" -ForegroundColor Cyan
Write-Host "SQL: $SqlFile"

$mysql = (Get-Command mysql -ErrorAction SilentlyContinue).Source
if (-not $mysql) {
    Write-Error "mysql client not on PATH"
    exit 1
}

$env:MYSQL_PWD = "root"
# Do not pipe SQL through PowerShell: native stdin re-encoding turns CJK into literal '?'.
$mysqlCmd = '"' + $mysql + '" --host=127.0.0.1 --port=3306 -uroot --default-character-set=utf8mb4 < "' + $SqlFile + '"'
cmd.exe /c $mysqlCmd
if ($LASTEXITCODE -ne 0) {
    Write-Error "MySQL sync failed (exit $LASTEXITCODE)"
    exit $LASTEXITCODE
}

Write-Host "`n--- Verification ---" -ForegroundColor DarkGray
& $mysql --host=127.0.0.1 --port=3306 -uroot --default-character-set=utf8mb4 shenyu-ops -e @"
SELECT 'oa_production_content.match_scheme_json' AS check_item,
       COUNT(*) AS ok FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA='shenyu-ops' AND TABLE_NAME='oa_production_content' AND COLUMN_NAME='match_scheme_json';
SELECT version, success FROM flyway_schema_history WHERE version IN ('192','193','194','195') ORDER BY version;
SELECT id, group_name, leader_user_id FROM oa_ip_group WHERE id=92002;
"@

& $mysql --host=127.0.0.1 --port=3306 -uroot --default-character-set=utf8mb4 shenyu-sys -e @"
SELECT value, label FROM system_dict_data
 WHERE dict_type='dict_marketing_plan_type' AND deleted=0 ORDER BY sort;
"@

Write-Host "`n[ok] Local DB sync complete." -ForegroundColor Green
Write-Host "Next: .\scripts\start-ops-dev.ps1  (或 -RestartOps 已指定时自动重启 ops-server)"

if ($RestartOps) {
    Write-Host "`n--- Restart ops-server ---" -ForegroundColor Cyan
    & (Join-Path $Root "scripts\start-integration-oa.ps1") -WaitSeconds 120
}
