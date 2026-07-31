# start-integration-oa.ps1 — Start ops-server (ADR-058 CLEANUP)
#
# Monorepo football-module-ops-server JAR on :48094, Nacos namespace=local.
# Legacy ops-platform-server was DELETED 2026-07-31 — use git history to restore.
#
# Prerequisites: MySQL localhost:3306 wd; Nacos 127.0.0.1:8848 for Gateway grayLb.
# Gateway route: football-gateway → grayLb://ops-server · Path=/admin-api/ops/**
#
# Usage (from repo root):
#   .\scripts\start-integration-oa.ps1
#   .\scripts\start-integration-oa.ps1 -Rebuild
#   .\scripts\start-integration-stack.ps1

[CmdletBinding()]
param(
    [string]$Profiles = "dev,dev-nacos,dev-nacos-local,dev-local-multidb",
    [int]$WaitSeconds = 120,
    [switch]$SkipNacosPrerequisiteCheck,
    [switch]$UseLegacyOa,
    [switch]$Rebuild
)

$ErrorActionPreference = "Continue"
. (Join-Path $PSScriptRoot "lib\integration-preflight.ps1")
$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$LogDir = Join-Path $PSScriptRoot "logs"
New-Item -ItemType Directory -Force -Path $LogDir | Out-Null
$BackendLog = Join-Path $LogDir "ops-server-nacos-run.log"

$MonorepoServerDir = Join-Path $Root "football-backend-saas\football-module-ops\football-module-ops-server"
$MonorepoJar = Join-Path $MonorepoServerDir "target\football-module-ops-server.jar"

function Stop-ListenersOnPort {
    param([int]$Port)
    $conns = @(Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
    foreach ($procId in ($conns | Select-Object -ExpandProperty OwningProcess -Unique)) {
        Write-Host "[stop] Port $Port -> PID $procId"
        Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
    }
}

if ($UseLegacyOa) {
    Write-Error @"
-UseLegacyOa is no longer supported (ADR-058 CLEANUP 2026-07-31).
ops-platform-server was deleted; Flyway SSOT = football-module-ops-server.
Rollback: restore ops-platform-server from git history before the CLEANUP commit,
then run that tree with mvn spring-boot:run — do not use this flag.
See docs/delivery/e2e-artifacts/P5-MIGRATE-8-cutover/ROLLBACK.md
"@
    exit 2
}

if (-not $SkipNacosPrerequisiteCheck) {
    $nacosUp = $false
    try {
        $null = Invoke-WebRequest -Uri "http://127.0.0.1:8848/nacos/" -UseBasicParsing -TimeoutSec 3
        $nacosUp = $true
    } catch { }
    if (-not $nacosUp) {
        Write-Warning "Local Nacos (127.0.0.1:8848) not reachable. Run .\scripts\start-nacos-local.ps1 first (or use -SkipNacosPrerequisiteCheck)."
        Write-Host "ops-server will still start (fail-fast=false) but will not register until Nacos is up."
    }
}

Stop-ListenersOnPort -Port 48094
# dual-run migrate port; free if leftover
Stop-ListenersOnPort -Port 48095
Start-Sleep -Seconds 2

Write-Host "=== Start ops-server monorepo football-module-ops-server :48094 ==="
if ($Rebuild -or -not (Test-Path -LiteralPath $MonorepoJar)) {
    Write-Host "[build] mvn -pl football-module-ops/football-module-ops-server -am package -DskipTests"
    $saas = Join-Path $Root "football-backend-saas"
    Push-Location -LiteralPath $saas
    try {
        & mvn -pl football-module-ops/football-module-ops-server -am package -DskipTests
        if ($LASTEXITCODE -ne 0) {
            Write-Error "Maven package failed (exit $LASTEXITCODE)"
            exit $LASTEXITCODE
        }
    } finally {
        Pop-Location
    }
}
if (-not (Test-Path -LiteralPath $MonorepoJar)) {
    Write-Error "Jar not found: $MonorepoJar"
    exit 1
}
$javaExe = (Get-Command java -ErrorAction SilentlyContinue).Source
if (-not $javaExe) { Write-Error "java not on PATH"; exit 1 }
$inner = @"
`$host.UI.RawUI.WindowTitle = 'ops-server monorepo :48094 (Nacos local)'
& '$javaExe' -jar '$MonorepoJar' *>&1 | Tee-Object -FilePath '$BackendLog' -Append
"@

Start-Process -FilePath "powershell.exe" -ArgumentList @(
    "-NoProfile", "-ExecutionPolicy", "Bypass", "-NoExit", "-Command", $inner
) -WindowStyle Minimized | Out-Null

Write-Host "Log: $BackendLog"
$ok = Wait-HttpEndpoint -Url "http://127.0.0.1:48094/actuator/health" -TimeoutSec $WaitSeconds -Label "ops-server"
if (-not $ok) {
    Write-Warning "ops-server log: $BackendLog"
}
Write-Host ""
Write-Host "Verify Nacos: service ops-server in namespace local (when Nacos is up)"
Write-Host "Via Gateway: curl http://localhost:48080/admin-api/ops/... (ADR-058 P4)"
Write-Host "Rollback:    git history restore of ops-platform-server (see ROLLBACK.md)"
