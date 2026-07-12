# run-uat-football-e2e.ps1 - Football full-stack UAT browser E2E (Playwright :5777)
#
# Usage (repo root):
#   .\scripts\run-uat-football-e2e.ps1
#   .\scripts\run-uat-football-e2e.ps1 -NoAutoStart

[CmdletBinding()]
param(
    [switch]$NoAutoStart,
    [int]$WaitSeconds = 180
)

$ErrorActionPreference = "Stop"
$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$FfDir = Join-Path $Root "football-front"
$WebEleDir = Join-Path $FfDir "apps\web-ele"
$ReportDir = Join-Path $Root "docs\delivery"
$PwCli = Join-Path $FfDir "node_modules\.bin\playwright.cmd"

function Test-PortListen([int]$Port) {
    return $null -ne (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
}

function Test-ServiceUp([string]$Url) {
    try {
        $null = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 8
        return $true
    } catch { return $false }
}

Write-Host "=== UAT Football E2E (:5777 login chain) ==="

$frontUp = Test-ServiceUp "http://localhost:5777/"
$gwUp = Test-ServiceUp "http://localhost:48080/admin-api/system/tenant/simple-list"
$oaUp = Test-ServiceUp "http://localhost:48094/actuator/health"

if (-not $frontUp -or -not $gwUp -or -not $oaUp) {
    if ($NoAutoStart) {
        throw "Services not ready. front=$frontUp gateway=$gwUp oa=$oaUp"
    }
    Write-Host "[start] Launching integration stack (SkipBuild)..."
    & (Join-Path $PSScriptRoot "start-integration-all.ps1") -SkipBuild -WaitSeconds $WaitSeconds
    $frontUp = Test-ServiceUp "http://localhost:5777/"
    $gwUp = Test-ServiceUp "http://localhost:48080/admin-api/system/tenant/simple-list"
    $oaUp = Test-ServiceUp "http://localhost:48094/actuator/health"
}

if (-not $frontUp -or -not $gwUp) {
    throw "Stack not ready. front=$frontUp gateway=$gwUp oa=$oaUp"
}

if (-not (Test-Path $PwCli)) {
    Push-Location $FfDir
    try { pnpm install } finally { Pop-Location }
}

# Ensure Chromium browser installed
& $PwCli install chromium 2>$null | Out-Null

Push-Location $WebEleDir
$exitCode = 1
try {
    $env:UAT_E2E_REPORT_DIR = $ReportDir
    Write-Host "[test] playwright tests/uat-football-ops-login.spec.ts ..."
    & $PwCli test tests/uat-football-ops-login.spec.ts --config=playwright.config.ts --grep "@uat-football" --reporter=list
    $exitCode = $LASTEXITCODE
} finally {
    Pop-Location
}

python (Join-Path $PSScriptRoot "uat-football-e2e-report.py")
if ($exitCode -ne 0) { exit $exitCode }
Write-Host "=== Done ==="
