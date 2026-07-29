# Seed OPS menus/dicts/roles on remote test DB (110.42.49.224).
# Requires scripts/integration-config/ops-test-remote.env (gitignored).
# See docs/delivery/OPS-TEST-DB.md and docs/delivery/OPS-TEST-SEED-RUNLOG.md

[CmdletBinding()]
param(
    [switch]$SkipMenu,
    [switch]$SkipSupplement,
    [switch]$VerifyOnly
)

$ErrorActionPreference = "Stop"
$Root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
. (Join-Path $Root "scripts\lib\integration-preflight.ps1")

if (-not (Import-OpsTestRemoteEnv -Root $Root -Required)) { exit 1 }

$dbHost = $env:OPS_TEST_DB_HOST
$port = $env:OPS_TEST_DB_PORT
$systemDb = $env:OPS_TEST_SYSTEM_DB
$systemUser = $env:OPS_TEST_SYSTEM_USER
$systemPass = $env:OPS_TEST_SYSTEM_PASSWORD
$masterDb = $env:OPS_TEST_MASTER_DB

$menuSeedPy = Join-Path $PSScriptRoot "apply-seed-oa-menu.py"
$supplement = Join-Path $PSScriptRoot "seed-ops-test-remote-shenyu-system-menus.sql"
$dictScript = Join-Path $PSScriptRoot "seed-ops-test-remote-dict.py"
$evidenceFile = Join-Path $Root "docs\delivery\OPS-TEST-SEED-RUNLOG.md"

function Invoke-MySqlFile {
    param(
        [string]$Database,
        [string]$User,
        [string]$Password,
        [string]$File
    )
    $env:MYSQL_PWD = $Password
    $sql = Get-Content -LiteralPath $File -Raw -Encoding UTF8
    $sql | & mysql -h $dbHost -P $port -u $User --default-character-set=utf8mb4 $Database
    if ($LASTEXITCODE -ne 0) {
        Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
        throw "mysql failed for $File on $Database (exit $LASTEXITCODE)"
    }
    Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
}

function Get-SeedEvidence {
    $env:MYSQL_PWD = $systemPass
    $query = @"
SELECT 'ops_menu_count', COUNT(*) FROM system_menu WHERE id>=6100 AND id<7000 AND deleted=0;
SELECT 'ops_role_menu_count', COUNT(*) FROM system_role_menu WHERE menu_id>=6100 AND menu_id<7000;
SELECT 'ops_dict_type_count', COUNT(*) FROM system_dict_type WHERE type LIKE 'dict_%' AND deleted=0;
SELECT 'ops_dict_data_count', COUNT(*) FROM system_dict_data WHERE dict_type LIKE 'dict_%' AND deleted=0;
SELECT 'sample_menu', id, name FROM system_menu WHERE id IN (6100,6117,6159) ORDER BY id;
SELECT 'corrupted_menu_rows', COUNT(*) FROM system_menu WHERE id>=6100 AND id<7000 AND deleted=0 AND name REGEXP '^[?]+$';
SELECT 'ip_group_leader_role', COUNT(*) FROM system_role WHERE code='ip_group_leader' AND deleted=0;
"@
    $out = & mysql -h $dbHost -P $port -u $systemUser --default-character-set=utf8mb4 -N $systemDb -e $query
    Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
    return ($out -join [Environment]::NewLine)
}

Write-Host ""
Write-Host "=== OPS test remote seed -> ${dbHost}/${systemDb} ===" -ForegroundColor Cyan

if ($VerifyOnly) {
    Write-Host (Get-SeedEvidence)
    exit 0
}

$stamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
$logLines = New-Object System.Collections.Generic.List[string]
$null = $logLines.Add("# OPS Test Seed Run Log")
$null = $logLines.Add("")
$null = $logLines.Add("| Item | Value |")
$null = $logLines.Add("|------|-------|")
$null = $logLines.Add("| Time | $stamp |")
$null = $logLines.Add("| Host | ${dbHost}:${port} |")
$null = $logLines.Add("| shenyu-system | $systemDb |")
$null = $logLines.Add("| shenyu-ops | $masterDb |")
$null = $logLines.Add("")
$null = $logLines.Add("## Scripts applied")
$null = $logLines.Add("")

if (-not $SkipMenu) {
    Write-Host "[1/3] Applying seed-oa-system-menu.sql (Python utf8mb4 stdin) ..."
    & python $menuSeedPy --host $dbHost --port $port --user $systemUser --password $systemPass --database $systemDb
    if ($LASTEXITCODE -ne 0) { throw "apply-seed-oa-menu.py failed (exit $LASTEXITCODE)" }
    $null = $logLines.Add("- scripts/integration-config/apply-seed-oa-menu.py -> $systemDb (seed-oa-system-menu.sql, utf8mb4)")
}

if (-not $SkipSupplement) {
    Write-Host "[2/3] Applying menu/role fixes (shenyu-system) ..."
    Invoke-MySqlFile -Database $systemDb -User $systemUser -Password $systemPass -File $supplement
    $null = $logLines.Add("- scripts/integration-config/seed-ops-test-remote-shenyu-system-menus.sql -> $systemDb")
    Write-Host "[3/3] Syncing dicts (shenyu-ops -> shenyu-system via Python) ..."
    & python $dictScript
    if ($LASTEXITCODE -ne 0) { throw "seed-ops-test-remote-dict.py failed (exit $LASTEXITCODE)" }
    $null = $logLines.Add("- scripts/integration-config/seed-ops-test-remote-dict.py -> $systemDb")
}

$evidence = Get-SeedEvidence
Write-Host ""
Write-Host "--- Verification ---" -ForegroundColor Green
Write-Host $evidence

$null = $logLines.Add("")
$null = $logLines.Add("## Verification (after seed)")
$null = $logLines.Add("")
$null = $logLines.Add('```')
foreach ($line in ($evidence -split [Environment]::NewLine)) { $null = $logLines.Add($line) }
$null = $logLines.Add('```')
$null = $logLines.Add("")
$null = $logLines.Add("## Notes")
$null = $logLines.Add("")
$null = $logLines.Add("- OPS menus live in shenyu-system.system_menu (6100-6999); @PreAuthorize reads @DS(system).")
$null = $logLines.Add("- Menu seed MUST use apply-seed-oa-menu.py (utf8mb4 stdin); PowerShell pipe corrupts Chinese.")
$null = $logLines.Add("- Flyway V164 repairs corrupted menu names in shenyu-system (name = literal '?').")
$null = $logLines.Add("- Business dicts merged from shenyu-ops.sys_dict_* to shenyu-system (V152/V158/V161 aligned).")
$null = $logLines.Add("- Flyway V161/V162/V163 on shenyu-ops apply on next oa-server start.")
$null = $logLines.Add("- Start beta stack: .\scripts\start-ops-dev.ps1 -Beta")

Set-Content -LiteralPath $evidenceFile -Value ($logLines -join [Environment]::NewLine) -Encoding UTF8
Write-Host "Evidence written: $evidenceFile" -ForegroundColor Green
