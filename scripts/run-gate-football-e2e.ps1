# run-gate-football-e2e.ps1 — Gate Playwright against Football shell :5777
#
# Specs SSOT: football-front/apps/web-ele/tests/  (ops-platform-ui-vue deleted)
# Config:     football-front/apps/web-ele/playwright.config.ts  (baseURL :5777, no webServer)
#
# Usage (repo root):
#   .\scripts\run-gate-football-e2e.ps1
#   .\scripts\run-gate-football-e2e.ps1 -NoAutoStart
#   .\scripts\run-gate-football-e2e.ps1 -Grep "@smoke"
#   .\scripts\run-gate-football-e2e.ps1 -Spec tests/football-content-smoke.spec.ts

[CmdletBinding()]
param(
    [switch]$NoAutoStart,
    [int]$WaitSeconds = 180,
    [string]$Grep = "",
    [string]$Spec = ""
)

$ErrorActionPreference = "Stop"
$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$FfDir = Join-Path $Root "football-front"
$WebEleDir = Join-Path $FfDir "apps\web-ele"
$PwCli = Join-Path $FfDir "node_modules\.bin\playwright.cmd"
$Config = Join-Path $WebEleDir "playwright.config.ts"

function Test-ServiceUp([string]$Url) {
    try {
        $null = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 8
        return $true
    } catch { return $false }
}

Write-Host "=== Gate Football E2E (:5777) ==="
Write-Host "Specs: football-front/apps/web-ele/tests/"

if (-not (Test-Path $Config)) {
    throw "Missing $Config — Gate Playwright config not found"
}

# Prefer 127.0.0.1: on some Windows hosts `localhost` → ::1 hangs while IPv4 works.
$frontUp = Test-ServiceUp "http://127.0.0.1:5777/"
$gwUp = Test-ServiceUp "http://127.0.0.1:48080/actuator/health"

if (-not $frontUp -or -not $gwUp) {
    if ($NoAutoStart) {
        throw "Services not ready. front=$frontUp gateway=$gwUp — run .\scripts\start-ops-dev.ps1 first"
    }
    Write-Host "[start] Launching Gate stack (start-ops-dev.ps1)..."
    & (Join-Path $PSScriptRoot "start-ops-dev.ps1")
    $frontUp = Test-ServiceUp "http://localhost:5777/"
    $gwUp = Test-ServiceUp "http://localhost:48080/admin-api/system/tenant/simple-list"
}

if (-not $frontUp -or -not $gwUp) {
    throw "Stack not ready. front=$frontUp gateway=$gwUp"
}

if (-not (Test-Path $PwCli)) {
    Push-Location $FfDir
    try { pnpm install } finally { Pop-Location }
}

& $PwCli install chromium 2>$null | Out-Null

Push-Location $WebEleDir
$exitCode = 1
try {
    $args = @("test", "--config=playwright.config.ts", "--reporter=list")
    if ($Spec) { $args += $Spec }
    if ($Grep) { $args += @("--grep", $Grep) }
    Write-Host "[test] playwright $($args -join ' ') ..."
    & $PwCli @args
    $exitCode = $LASTEXITCODE
} finally {
    Pop-Location
}

if ($exitCode -ne 0) { exit $exitCode }
Write-Host "=== Gate E2E Done ==="
