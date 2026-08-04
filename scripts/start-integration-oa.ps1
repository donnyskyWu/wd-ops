# start-integration-oa.ps1 — Start football-module-ops (ADR-058 CLEANUP)
#
# Monorepo football-module-ops-server JAR on :48094 (Nacos registry id: ops-server).
# Default: local MySQL wd + Nacos namespace=local (application.yaml).
# Beta: when -Profiles includes dev-test-beta (from start-integration-all -Beta),
#       load ops-test-remote.env + ops-test-beta-multidb.yml (remote DB/Nacos/Redis).
#
# Usage (from repo root):
#   .\scripts\start-integration-oa.ps1
#   .\scripts\start-integration-oa.ps1 -Rebuild
#   .\scripts\start-integration-all.ps1 -Beta

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
$BetaOverlay = Join-Path $Root "scripts\integration-config\ops-test-beta-multidb.yml"
$UseBeta = ($Profiles -match "dev-test-beta") -or ($env:OPS_TEST_DB_HOST -and (Test-Path -LiteralPath $BetaOverlay) -and $Profiles -match "beta")

# Prefer explicit profile token from start-integration-all -Beta
if ($Profiles -match "dev-test-beta") {
    $UseBeta = $true
}

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

if ($UseBeta) {
    if (Get-Command Import-OpsTestRemoteEnv -ErrorAction SilentlyContinue) {
        if (-not (Import-OpsTestRemoteEnv -Root $Root -Required)) { exit 1 }
    } elseif (-not $env:OPS_TEST_DB_HOST) {
        Write-Error "Beta mode requires ops-test-remote.env (OPS_TEST_DB_HOST). See docs/delivery/OPS-TEST-DB.md"
        exit 1
    }
    if (-not (Test-Path -LiteralPath $BetaOverlay)) {
        Write-Error "Missing beta overlay: $BetaOverlay"
        exit 1
    }
    Write-Host "[beta] football-module-ops (:48094, Nacos ops-server) -> $($env:OPS_TEST_DB_HOST) / $($env:OPS_TEST_MASTER_DB) ; Nacos $($env:OPS_TEST_NACOS_ADDR) ns=$($env:OPS_TEST_NACOS_NAMESPACE)" -ForegroundColor Yellow
} elseif (-not $SkipNacosPrerequisiteCheck) {
    $nacosUp = $false
    try {
        $null = Invoke-WebRequest -Uri "http://127.0.0.1:8848/nacos/" -UseBasicParsing -TimeoutSec 3
        $nacosUp = $true
    } catch { }
    if (-not $nacosUp) {
        Write-Warning "Local Nacos (127.0.0.1:8848) not reachable. Run .\scripts\start-nacos-local.ps1 first (or use -SkipNacosPrerequisiteCheck)."
        Write-Host "football-module-ops will still start (fail-fast=false) but will not register as ops-server until Nacos is up."
    }
}

Stop-ListenersOnPort -Port 48094
# dual-run migrate port; free if leftover
Stop-ListenersOnPort -Port 48095
Start-Sleep -Seconds 2

Write-Host "=== Start football-module-ops (football-module-ops-server.jar :48094) ==="
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

$extraCfg = ""
$titleNote = "Nacos local"
if ($UseBeta) {
    $extraCfg = "--spring.config.additional-location=optional:file:$BetaOverlay --spring.flyway.enabled=false"
    $titleNote = "BETA $($env:OPS_TEST_DB_HOST)"
    Write-Host "        config: $BetaOverlay" -ForegroundColor DarkGray
}

$inner = @"
`$host.UI.RawUI.WindowTitle = 'football-module-ops :48094 ($titleNote)'
& '$javaExe' '-Dfile.encoding=UTF-8' -jar '$MonorepoJar' $extraCfg *>&1 | Tee-Object -FilePath '$BackendLog' -Append
"@

Start-Process -FilePath "powershell.exe" -ArgumentList @(
    "-NoProfile", "-ExecutionPolicy", "Bypass", "-NoExit", "-Command", $inner
) -WindowStyle Minimized | Out-Null

Write-Host "Log: $BackendLog"
$opsFallback = "http://127.0.0.1:48094/v3/api-docs"
$ok = Wait-HttpEndpoint -Url "http://127.0.0.1:48094/actuator/health" -FallbackUrl $opsFallback -Port 48094 -TimeoutSec $WaitSeconds -Label "football-module-ops"
if (-not $ok) {
    Write-Warning "football-module-ops log: $BackendLog"
}
Write-Host ""
if ($UseBeta) {
    Write-Host "Verify Nacos: service ops-server in namespace $($env:OPS_TEST_NACOS_NAMESPACE) @ $($env:OPS_TEST_NACOS_ADDR)"
} else {
    Write-Host "Verify Nacos: service ops-server in namespace local (when Nacos is up)"
}
Write-Host "Via Gateway: curl http://localhost:48080/admin-api/ops/... (ADR-058 P-C; no Rewrite)"
Write-Host "Rollback:    git history restore of ops-platform-server (see ROLLBACK.md)"
if ($ok) { exit 0 } else { exit 1 }
