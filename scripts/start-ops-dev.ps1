# start-ops-dev.ps1 - One-click Ops dev stack (Football integration Gate path)
#
# ADR-058 P5-MIGRATE-8: ops-server = monorepo football-module-ops-server JAR :48094
# (via start-integration-all → start-integration-oa.ps1). Legacy ops-platform-server deleted (ADR-058 CLEANUP).
#
# Fixes recurring "system error" in UI: auto-check Redis password, MySQL, Docker,
# then restart full integration stack with health table.
#
# Gate UI: http://localhost:5777 (football-front pnpm dev:ele → Gateway :48080).
# OPS UI SSOT: football-front :5777 (views/ops). ops-platform-ui-vue / :3000 retired (A-WP1).
# Preflight: warn if football-* not on ops branch; verify views/ops present (remount retired).
# Default DB: localhost five schemas (dev-local-multidb). Beta remote is a separate profile.
#
# member-server (:48087): DEFAULT = real football-module-member-server JAR (FullMemberServer).
# Required for Football 方案列表 (GET /admin-api/member/article/page). Python mock on :48087
# only stubs login Feign and returns HTTP 404 for article/* -> UI "服务器内部错误".
# Opt out (login smoke only): -UseMemberMock
#
# Usage (from repo root):
#   .\scripts\start-ops-dev.ps1              # daily: restart, skip Maven build, member JAR
#   .\scripts\start-ops-dev.ps1 -FirstRun    # first run / big changes: Maven build (+ member)
#   .\scripts\start-ops-dev.ps1 -NoRestart   # start missing services only
#   .\scripts\start-ops-dev.ps1 -UseMemberMock  # Python mock :48087 (no 方案列表)
#   .\scripts\start-ops-dev.ps1 -MountOps   # RETIRED — fails with SSOT message
#   .\scripts\start-ops-dev.ps1 -SkipMountOps
#   .\scripts\start-ops-dev.ps1 -Beta       # remote test DB 110.42.49.224 (ops-test-remote.env)
#   .\scripts\start-ops-dev.ps1 -TestRemote # alias of -Beta
#
# Login: http://localhost:5777  admin / admin123  tenant 1
# Docs:  docs/delivery/OPS-DEV-DEPLOY-GUIDE.md · FOOTBALL-OPS-BRANCH.md · OPS-TEST-DB.md
# Stop:  .\scripts\stop-integration-all.ps1

[CmdletBinding()]
param(
    [switch]$FirstRun,
    [switch]$NoRestart,
    [switch]$SkipFrontend,
    [switch]$SkipNacos,
    [switch]$UseMemberMock,
    [switch]$MountOps,
    [switch]$SkipMountOps,
    [Alias("TestRemote")]
    [switch]$Beta,
    [int]$WaitSeconds = 180
)

$ErrorActionPreference = "Continue"
$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
if ($MountOps) {
    Write-Host "[retired] -MountOps: remount from ops-platform-ui-vue ended (A-WP1)." -ForegroundColor Red
    Write-Host "          OPS UI SSOT = football-front/apps/web-ele/src/views/ops — edit there directly." -ForegroundColor Yellow
    exit 1
}
$LogDir = Join-Path $PSScriptRoot "logs"
New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

. (Join-Path $PSScriptRoot "lib\integration-preflight.ps1")

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
if ($Beta) {
    Write-Host "  Ops Dev Stack (BETA remote DB)" -ForegroundColor Yellow
} else {
    Write-Host "  Ops Dev Stack (Football Gate Path)" -ForegroundColor Cyan
}
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Repo: $Root"
if ($Beta) {
    Write-Host "Mode: BETA -> 110.42.49.224 (ops-test-remote.env + *-overlay-beta.yml)" -ForegroundColor Yellow
} else {
    Write-Host "Mode: LOCAL localhost multidb (default)"
}
Write-Host ""

Write-Host "--- [1/6] Prerequisites ---"
if (-not (Test-CommandExists "java")) {
    Write-Error "Java not found. Install JDK 17+ and add to PATH."
    exit 1
}
Write-Host "[ok] $(java -version 2>&1 | Select-Object -First 1)"

if (-not $SkipFrontend -and -not (Test-CommandExists "pnpm")) {
    Write-Warning "pnpm not found; using -SkipFrontend. Install Node.js + pnpm for UI."
    $SkipFrontend = $true
}

if ($Beta) {
    if (-not (Import-OpsTestRemoteEnv -Root $Root -Required)) { exit 1 }
    # Beta uses remote Nacos (110.42.49.224:8848); local Docker Nacos optional
    if (-not $SkipNacos) {
        Write-Host "`n--- [2/6] Skip local Nacos (beta uses remote) ---"
        $SkipNacos = $true
    } else {
        Write-Host "`n--- [2/6] Skip Nacos (-SkipNacos) ---"
    }
    Write-Host "`n--- [3/6] Redis remote $($env:OPS_TEST_REDIS_HOST):$($env:OPS_TEST_REDIS_PORT) db=$($env:OPS_TEST_REDIS_DATABASE) ---"
    Write-Host "[ok] Using beta Redis from env (skip local requirepass 123456)"
    Write-Host "`n--- [4/6] MySQL beta $($env:OPS_TEST_DB_HOST):$($env:OPS_TEST_DB_PORT) ---"
    Write-Host "[ok] master=$($env:OPS_TEST_MASTER_DB) system=$($env:OPS_TEST_SYSTEM_DB) (skip localhost check)"
} else {
    if (-not $SkipNacos) {
        Write-Host "`n--- [2/6] Docker / Nacos ---"
        $null = Wait-DockerEngine -TimeoutSec 90
    } else {
        Write-Host "`n--- [2/6] Skip Nacos (-SkipNacos) ---"
    }

    Write-Host "`n--- [3/6] Redis :6379 (password 123456) ---"
    $redisOk = Ensure-IntegrationRedis
    if (-not $redisOk) {
        Write-Error "Redis not ready - Gateway/system cannot start (UI shows system error)"
        Write-Host "Preflight could not set password 123456 on :6379. Ensure redis-cli is in PATH, or start Docker Desktop for redis-integration-local."
        exit 1
    }

    Write-Host "`n--- [4/6] MySQL localhost:3306 ---"
    $mysqlOk = Test-MySqlLocal
    if (-not $mysqlOk) {
        Write-Warning "MySQL check failed; oa-server may start but APIs may error"
    }
}

Write-Host "`n--- [5/6] Football OPS front preflight (branch + mount + vite) ---"
$null = Assert-FootballOpsBranch -Root $Root
$null = Ensure-LocalSftpKey -Root $Root
if (-not $SkipFrontend) {
    $mountArgs = @{ Root = $Root }
    if ($MountOps) { $mountArgs.ForceMount = $true }
    if ($SkipMountOps) { $mountArgs.SkipMount = $true }
    $null = Ensure-OpsViewsMounted @mountArgs
    $null = Ensure-OpsFrontDeps -Root $Root -AutoLink
    $null = Ensure-FootballFrontLocalApi -Root $Root
} else {
    Write-Host "[skip] frontend preflight (-SkipFrontend)"
}

Write-Host "`n--- [6/6] Start integration stack ---"
$effectiveWait = if ($FirstRun) { [Math]::Max($WaitSeconds, 240) } else { $WaitSeconds }
$allArgs = @{
    WaitSeconds = $effectiveWait
}
if ($FirstRun) {
    Write-Host "[build] First run: Maven build for Football modules (slow; health wait up to ${effectiveWait}s)"
} else {
    $allArgs.SkipBuild = $true
}
if ($SkipFrontend) { $allArgs.SkipFrontend = $true }
if ($SkipNacos) { $allArgs.SkipNacos = $true }
if ($UseMemberMock) { $allArgs.UseMemberMock = $true }
if ($MountOps) { $allArgs.MountOps = $true }
if ($SkipMountOps) { $allArgs.SkipMountOps = $true }
if ($Beta) { $allArgs.Beta = $true }
if (-not $NoRestart) { $allArgs.Restart = $true }

$allScript = Join-Path $PSScriptRoot "start-integration-all.ps1"
& $allScript @allArgs
$exitCode = $LASTEXITCODE

Write-Host "`n--- Final health check ---"
Show-IntegrationHealthTable -SkipFrontend:$SkipFrontend

$critical = @(Get-IntegrationHealthRows | Where-Object { $_.Critical })
if (-not $SkipFrontend) {
    $critical += @{ Service = "football-front"; Port = 5777; Url = "http://127.0.0.1:5777/" }
}
$down = @()
foreach ($e in $critical) {
    $st = Get-ServiceListenStatus -Port $e.Port -ProbeUrl $e.Url
    if ($st -ne "UP") { $down += $e.Service }
}

Write-Host ""
if ($down.Count -eq 0) {
    Write-Host "=== START OK ===" -ForegroundColor Green
    Write-Host "UI:      http://localhost:5777  (Gate; NOT :3000)"
    Write-Host "Gateway: http://localhost:48080/admin-api"
    Write-Host "Login:   admin / admin123   tenant: 1"
} else {
    Write-Host "=== NOT READY: $($down -join ', ') ===" -ForegroundColor Red
    Show-IntegrationTroubleshooting -LogDir $LogDir
    if ($exitCode -eq 0) { exit 2 }
}

Write-Host ""
Write-Host "Quick restart: .\scripts\start-ops-dev.ps1$(if ($Beta) { ' -Beta' })"
Write-Host "Beta remote:   .\scripts\start-ops-dev.ps1 -Beta"
Write-Host "FE edits:      football-front/apps/web-ele/src/views/ops (remount retired)"
Write-Host "Stop:          .\scripts\stop-integration-all.ps1"
