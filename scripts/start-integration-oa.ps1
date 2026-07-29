# start-integration-oa.ps1 — Start oa-server with Nacos registration (S1-A)
#
# Profiles:
#   dev,dev-nacos,dev-nacos-local  — default: local Docker Nacos (127.0.0.1:8848); run .\scripts\start-nacos-local.ps1 first
#   dev,dev-nacos                   — remote Nacos (192.168.10.47:8848); app still starts on 48094 if Nacos is down (fail-fast: false)
#
# Prerequisites: MySQL localhost:3306 five DBs (dev-local-multidb profile; NOT 101.37.161.136 for daily dev).
# Nacos optional for local HTTP; required for Gateway grayLb discovery.
# Gateway route: football-gateway application.yaml → grayLb://oa-server
#
# Usage (from repo root):
#   .\scripts\start-integration-oa.ps1
#   .\scripts\start-integration-oa.ps1 -Profiles "dev,dev-nacos"
#   .\scripts\start-integration-stack.ps1   # Nacos + oa-server
#
# Full Football stack (Gateway 48080, system-server, etc.) is started separately
# from football-backend-saas; this script only boots Ops OA in integration mode.

[CmdletBinding()]
param(
    [string]$Profiles = "dev,dev-nacos,dev-nacos-local,dev-local-multidb",
    [int]$WaitSeconds = 120,
    [switch]$SkipNacosPrerequisiteCheck
)

$ErrorActionPreference = "Continue"
. (Join-Path $PSScriptRoot "lib\integration-preflight.ps1")
$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$LogDir = Join-Path $PSScriptRoot "logs"
New-Item -ItemType Directory -Force -Path $LogDir | Out-Null
$BackendLog = Join-Path $LogDir "oa-server-nacos-run.log"
$backendDir = Join-Path $Root "ops-platform-server\ops-platform-module-oa"

function Stop-ListenersOnPort {
    param([int]$Port)
    $conns = @(Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
    foreach ($procId in ($conns | Select-Object -ExpandProperty OwningProcess -Unique)) {
        Write-Host "[stop] Port $Port -> PID $procId"
        Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
    }
}

if ($Profiles -match "dev-nacos-local" -and -not $SkipNacosPrerequisiteCheck) {
    $nacosUp = $false
    try {
        $null = Invoke-WebRequest -Uri "http://127.0.0.1:8848/nacos/" -UseBasicParsing -TimeoutSec 3
        $nacosUp = $true
    } catch { }
    if (-not $nacosUp) {
        Write-Warning "Local Nacos (127.0.0.1:8848) not reachable. Run .\scripts\start-nacos-local.ps1 first (or use -SkipNacosPrerequisiteCheck)."
        Write-Host "oa-server will still start (fail-fast=false) but will not register until Nacos is up."
    }
}

Write-Host "=== Start oa-server ($Profiles) ==="
Write-Host "Note: Nacos unreachable does not abort startup (spring.cloud.nacos.discovery.fail-fast=false)."
if ($Profiles -match "dev-test-beta") {
    $repairScript = Join-Path $Root "scripts\integration-config\repair-flyway-failed.py"
    if (Test-Path $repairScript) {
        Write-Host "[beta] Repair failed Flyway rows (if any) before startup..."
        & python $repairScript
    }
}
Stop-ListenersOnPort -Port 48094
Start-Sleep -Seconds 2

$backendCmd = "mvn spring-boot:run '-Dspring-boot.run.profiles=$Profiles'"
$inner = @"
Set-Location -LiteralPath '$backendDir'
`$host.UI.RawUI.WindowTitle = 'oa-server :48094 (Nacos)'
& { $backendCmd } *>&1 | Tee-Object -FilePath '$BackendLog' -Append
"@
Start-Process -FilePath "powershell.exe" -ArgumentList @(
    "-NoProfile", "-ExecutionPolicy", "Bypass", "-NoExit", "-Command", $inner
) -WindowStyle Minimized | Out-Null

Write-Host "Log: $BackendLog"
$ok = Wait-HttpEndpoint -Url "http://127.0.0.1:48094/actuator/health" -TimeoutSec $WaitSeconds -Label "oa-server"
if (-not $ok) {
    Write-Warning "oa-server log (Nacos may be down; startup should still succeed if fail-fast is off): $BackendLog"
}
Write-Host ""
Write-Host "Verify Nacos console: service oa-server in namespace dev (when Nacos is up)"
Write-Host "Via Gateway: curl http://localhost:48080/admin-api/oa/... (after gateway-server running)"
