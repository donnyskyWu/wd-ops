# start-ops-dev.ps1 - One-click Ops dev stack (Football integration Gate path)
#
# Fixes recurring "system error" in UI: auto-check Redis password, MySQL, Docker,
# then restart full integration stack with health table.
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
#
# Login: http://localhost:5777  admin / admin123  tenant 1
# Docs:  docs/delivery/OPS-STARTUP-MATRIX.md
# Stop:  .\scripts\stop-integration-all.ps1

[CmdletBinding()]
param(
    [switch]$FirstRun,
    [switch]$NoRestart,
    [switch]$SkipFrontend,
    [switch]$SkipNacos,
    [switch]$UseMemberMock,
    [int]$WaitSeconds = 300
)

$ErrorActionPreference = "Continue"
$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$LogDir = Join-Path $PSScriptRoot "logs"
New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

. (Join-Path $PSScriptRoot "lib\integration-preflight.ps1")

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Ops Dev Stack (Football Gate Path)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Repo: $Root"
Write-Host ""

Write-Host "--- [1/5] Prerequisites ---"
if (-not (Test-CommandExists "java")) {
    Write-Error "Java not found. Install JDK 17+ and add to PATH."
    exit 1
}
Write-Host "[ok] $(java -version 2>&1 | Select-Object -First 1)"

if (-not $SkipFrontend -and -not (Test-CommandExists "pnpm")) {
    Write-Warning "pnpm not found; using -SkipFrontend. Install Node.js + pnpm for UI."
    $SkipFrontend = $true
}

if (-not $SkipNacos) {
    Write-Host "`n--- [2/5] Docker / Nacos ---"
    $null = Wait-DockerEngine -TimeoutSec 90
} else {
    Write-Host "`n--- [2/5] Skip Nacos (-SkipNacos) ---"
}

Write-Host "`n--- [3/5] Redis :6379 (password 123456) ---"
$redisOk = Ensure-IntegrationRedis
if (-not $redisOk) {
    Write-Error "Redis not ready - Gateway/system cannot start (UI shows system error)"
    Write-Host "Preflight could not set password 123456 on :6379. Ensure redis-cli is in PATH, or start Docker Desktop for redis-integration-local."
    exit 1
}

Write-Host "`n--- [4/5] MySQL localhost:3306 ---"
$mysqlOk = Test-MySqlLocal
if (-not $mysqlOk) {
    Write-Warning "MySQL check failed; oa-server may start but APIs may error"
}

Write-Host "`n--- [5/5] Start integration stack ---"
$allArgs = @{
    WaitSeconds = $WaitSeconds
}
if ($FirstRun) {
    Write-Host "[build] First run: Maven build for Football modules (slow)"
} else {
    $allArgs.SkipBuild = $true
}
if ($SkipFrontend) { $allArgs.SkipFrontend = $true }
if ($SkipNacos) { $allArgs.SkipNacos = $true }
if ($UseMemberMock) { $allArgs.UseMemberMock = $true }
if (-not $NoRestart) { $allArgs.Restart = $true }

$allScript = Join-Path $PSScriptRoot "start-integration-all.ps1"
& $allScript @allArgs
$exitCode = $LASTEXITCODE

Write-Host "`n--- Final health check ---"
Show-IntegrationHealthTable -SkipFrontend:$SkipFrontend

$critical = Get-IntegrationHealthRows | Where-Object { $_.Critical }
$down = @()
foreach ($e in $critical) {
    $st = Get-ServiceListenStatus -Port $e.Port -ProbeUrl $e.Url
    if ($st -ne "UP") { $down += $e.Service }
}

Write-Host ""
if ($down.Count -eq 0) {
    Write-Host "=== START OK ===" -ForegroundColor Green
    Write-Host "UI:      http://localhost:5777"
    Write-Host "Gateway: http://localhost:48080/admin-api"
    Write-Host "Login:   admin / admin123   tenant: 1"
} else {
    Write-Host "=== NOT READY: $($down -join ', ') ===" -ForegroundColor Red
    Show-IntegrationTroubleshooting -LogDir $LogDir
    if ($exitCode -eq 0) { exit 2 }
}

Write-Host ""
Write-Host "Quick restart: .\scripts\start-ops-dev.ps1"
Write-Host "Stop:          .\scripts\stop-integration-all.ps1"
