# Run M-Live ROI handover SQL against a test MySQL instance.
# Default: shenyu-ops-schema + shenyu-system (no Error 1419).
# With trust: also shenyu-ops-routines (or set HANDOVER_MYSQL_USE_FULL=1 for shenyu-ops-full.sql).
#
# Env: HANDOVER_MYSQL_* or scripts/integration-config/ops-test-remote.env
#   MYSQL_TRUST_CREATORS=1  — run routines after schema (or full ops if HANDOVER_MYSQL_USE_FULL=1)
#   HANDOVER_MYSQL_SET_BINLOG_TRUST=1 — try SET GLOBAL before routines (needs SUPER; default 0)

$ErrorActionPreference = "Stop"
$Root = Split-Path $PSScriptRoot -Parent
$ReleaseDir = Join-Path $Root "docs\delivery\handover\sql-release-20261105"
$OpsSchemaSql = Join-Path $ReleaseDir "shenyu-ops-schema.sql"
$OpsRoutinesSql = Join-Path $ReleaseDir "shenyu-ops-routines.sql"
$OpsFullSql = Join-Path $ReleaseDir "shenyu-ops-full.sql"
$SysSql = Join-Path $ReleaseDir "shenyu-system.sql"
$EnvFile = Join-Path $Root "scripts\integration-config\ops-test-remote.env"

function Read-DotEnv([string]$Path) {
    $map = @{}
    if (-not (Test-Path $Path)) { return $map }
    Get-Content $Path -Encoding UTF8 | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith("#") -or $line -notmatch "=") { return }
        $i = $line.IndexOf("=")
        $k = $line.Substring(0, $i).Trim()
        $v = $line.Substring($i + 1).Trim().Trim('"').Trim("'")
        $map[$k] = $v
    }
    return $map
}

$fileEnv = Read-DotEnv $EnvFile
function Get-Cfg([string]$Key, [string]$Fallback = "") {
    $v = [Environment]::GetEnvironmentVariable($Key)
    if ($v) { return $v }
    if ($fileEnv.ContainsKey($Key)) { return $fileEnv[$Key] }
    return $Fallback
}

$hostName = Get-Cfg "HANDOVER_MYSQL_HOST" (Get-Cfg "OPS_TEST_DB_HOST" "127.0.0.1")
$port = Get-Cfg "HANDOVER_MYSQL_PORT" (Get-Cfg "OPS_TEST_DB_PORT" "3306")
$opsUser = Get-Cfg "HANDOVER_MYSQL_USER" (Get-Cfg "OPS_TEST_MASTER_USER" "root")
$opsPwd = Get-Cfg "HANDOVER_MYSQL_PASSWORD" (Get-Cfg "OPS_TEST_MASTER_PASSWORD" "root")
$sysUser = Get-Cfg "HANDOVER_MYSQL_SYSTEM_USER" (Get-Cfg "OPS_TEST_SYSTEM_USER" $opsUser)
$sysPwd = Get-Cfg "HANDOVER_MYSQL_SYSTEM_PASSWORD" (Get-Cfg "OPS_TEST_SYSTEM_PASSWORD" $opsPwd)
$trustCreators = Get-Cfg "MYSQL_TRUST_CREATORS" "0"
$useFull = Get-Cfg "HANDOVER_MYSQL_USE_FULL" "0"
$setTrust = Get-Cfg "HANDOVER_MYSQL_SET_BINLOG_TRUST" "0"

if (-not (Test-Path $OpsSchemaSql)) { throw "Missing $OpsSchemaSql — run _build_sql_release_20261105.py first" }
if (-not (Test-Path $SysSql)) { throw "Missing $SysSql" }

$mysql = Get-Command mysql -ErrorAction SilentlyContinue
if (-not $mysql) { throw "mysql CLI not found on PATH" }

function Invoke-MysqlFile([string]$User, [string]$Pwd, [string]$Database, [string]$SqlPath) {
    $prev = $env:MYSQL_PWD
    $env:MYSQL_PWD = $Pwd
    try {
        $log = Join-Path $env:TEMP ("handover-" + [IO.Path]::GetFileName($SqlPath) + ".log")
        New-Item -Path $log -ItemType File -Force | Out-Null
        $q = "`"$SqlPath`""
        $cmd = "mysql --host=$hostName --port=$port --user=$User --database=$Database --default-character-set=utf8mb4 --force < $q 2>&1"
        cmd /c $cmd | Tee-Object -FilePath $log -Append
        if (-not (Test-Path $log)) { throw "mysql log missing: $log" }
        $errLines = @(Select-String -Path $log -Pattern "ERROR" -SimpleMatch | ForEach-Object { $_.Line })
        if ($errLines.Count -eq 0) {
            Write-Host "OK: $SqlPath -> $Database (log: $log)"
            return
        }
        Write-Host "---- ERROR lines in $log ($($errLines.Count)) ----"
        $errLines | Select-Object -First 15 | ForEach-Object { Write-Host $_ }
        $only1419 = ($errLines | Where-Object { $_ -notmatch "ERROR 1419 \(HY000\)" }).Count -eq 0
        if ($only1419) {
            Write-Host "BLOCKER: Error 1419 — DBA must set log_bin_trust_function_creators=1 (see DBA-1419-一次性设置.md)."
            exit 2
        }
        throw "mysql reported non-1419 ERROR(s) — see $log"
    }
    finally {
        if ($null -ne $prev) { $env:MYSQL_PWD = $prev } else { Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue }
    }
}

function Invoke-MysqlSql([string]$User, [string]$Pwd, [string]$Sql) {
    $prev = $env:MYSQL_PWD
    $env:MYSQL_PWD = $Pwd
    try {
        & mysql --host=$hostName --port=$port --user=$User --default-character-set=utf8mb4 -e $Sql
        if ($LASTEXITCODE -ne 0) { throw "mysql -e failed ($LASTEXITCODE)" }
    }
    finally {
        if ($null -ne $prev) { $env:MYSQL_PWD = $prev } else { Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue }
    }
}

function Test-LogBinTrustCreators([string]$User, [string]$Pwd) {
    $prev = $env:MYSQL_PWD
    $env:MYSQL_PWD = $Pwd
    try {
        $raw = & mysql --host=$hostName --port=$port --user=$User --default-character-set=utf8mb4 -N -s `
            -e "SHOW VARIABLES LIKE 'log_bin_trust_function_creators';" 2>$null
        if ($LASTEXITCODE -ne 0) { return $false }
        $val = ($raw | Select-Object -Last 1).ToString().Trim()
        return ($val -eq "1" -or $val -eq "ON" -or $val -eq "on")
    }
    finally {
        if ($null -ne $prev) { $env:MYSQL_PWD = $prev } else { Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue }
    }
}

Write-Host "Target: ${hostName}:${port} (ops user=$opsUser, system user=$sysUser)"
Invoke-MysqlSql $opsUser $opsPwd "CREATE DATABASE IF NOT EXISTS ``shenyu-ops`` DEFAULT CHARACTER SET utf8mb4;"
Invoke-MysqlSql $sysUser $sysPwd "CREATE DATABASE IF NOT EXISTS ``shenyu-system`` DEFAULT CHARACTER SET utf8mb4;"

$runRoutines = ($trustCreators -eq "1")
if (-not $runRoutines) {
    $runRoutines = Test-LogBinTrustCreators $opsUser $opsPwd
    if ($runRoutines) { Write-Host "Detected log_bin_trust_function_creators=ON — will run routines/full ops." }
}

if ($useFull -eq "1" -and $runRoutines) {
    if (-not (Test-Path $OpsFullSql)) { throw "Missing $OpsFullSql" }
    if ($setTrust -eq "1") {
        $prev = $env:MYSQL_PWD
        $env:MYSQL_PWD = $opsPwd
        cmd /c "mysql --host=$hostName --port=$port --user=$opsUser --default-character-set=utf8mb4 -e `"SET GLOBAL log_bin_trust_function_creators = 1;`" 2>&1" | Out-Null
        if ($null -ne $prev) { $env:MYSQL_PWD = $prev } else { Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue }
    }
    Invoke-MysqlFile $opsUser $opsPwd "shenyu-ops" $OpsFullSql
}
else {
    Invoke-MysqlFile $opsUser $opsPwd "shenyu-ops" $OpsSchemaSql
    if ($runRoutines) {
        if (-not (Test-Path $OpsRoutinesSql)) { throw "Missing $OpsRoutinesSql" }
        if ($setTrust -eq "1") {
            $prev = $env:MYSQL_PWD
            $env:MYSQL_PWD = $opsPwd
            $trustOut = cmd /c "mysql --host=$hostName --port=$port --user=$opsUser --default-character-set=utf8mb4 -e `"SET GLOBAL log_bin_trust_function_creators = 1;`" 2>&1"
            if ($null -ne $prev) { $env:MYSQL_PWD = $prev } else { Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue }
            if ($LASTEXITCODE -ne 0) {
                Write-Warning "Could not SET GLOBAL log_bin_trust_function_creators."
                if ($trustOut) { Write-Warning ($trustOut | Out-String).Trim() }
            }
        }
        Invoke-MysqlFile $opsUser $opsPwd "shenyu-ops" $OpsRoutinesSql
    }
    else {
        Write-Host "Skipped shenyu-ops-routines.sql (set MYSQL_TRUST_CREATORS=1 or ask DBA for log_bin_trust_function_creators)."
    }
}

Invoke-MysqlFile $sysUser $sysPwd "shenyu-system" $SysSql
Write-Host "Handover SQL test finished."
