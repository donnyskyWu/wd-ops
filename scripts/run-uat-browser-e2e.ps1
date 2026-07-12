# run-uat-browser-e2e.ps1 - Ops standalone UAT browser E2E (Playwright)
#
# Usage (repo root):
#   .\scripts\run-uat-browser-e2e.ps1
#   .\scripts\run-uat-browser-e2e.ps1 -NoAutoStart

[CmdletBinding()]
param(
    [switch]$NoAutoStart,
    [int]$WaitSeconds = 120
)

$ErrorActionPreference = "Stop"
$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$UiDir = Join-Path $Root "ops-platform-ui-vue"
$ReportDir = Join-Path $Root "docs\delivery"

function Test-ServiceUp([string]$Url) {
    try {
        $null = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 5
        return $true
    } catch { return $false }
}

Write-Host "=== UAT Browser E2E (Ops standalone) ==="

$uiUp = Test-ServiceUp "http://localhost:3000"
$apiUp = Test-ServiceUp "http://localhost:8080/actuator/health"

if (-not $uiUp -or -not $apiUp) {
    if ($NoAutoStart) {
        throw "Services not ready. UI=$uiUp API=$apiUp"
    }
    Write-Host "[start] Launching standalone stack..."
    & (Join-Path $PSScriptRoot "start-ops-standalone.ps1") -WaitSeconds $WaitSeconds
    $uiUp = Test-ServiceUp "http://localhost:3000"
    $apiUp = Test-ServiceUp "http://localhost:8080/actuator/health"
}

if (-not $uiUp -or -not $apiUp) {
    throw "Stack not ready. UI=$uiUp API=$apiUp"
}

Push-Location $UiDir
$exitCode = 1
try {
    $pwCli = Join-Path $UiDir "node_modules\.bin\playwright.cmd"
    if (-not (Test-Path $pwCli)) {
        npm install
    }
    $env:UAT_E2E_REPORT_DIR = $ReportDir
    Write-Host "[test] playwright tests/uat-browser-gap.spec.ts ..."
    & $pwCli test tests/uat-browser-gap.spec.ts --grep "@uat-gap" --reporter=list
    $exitCode = $LASTEXITCODE
} finally {
    Pop-Location
}

python (Join-Path $PSScriptRoot "uat-browser-e2e-report.py")
if ($exitCode -ne 0) { exit $exitCode }
Write-Host "=== Done ==="
