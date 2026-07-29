# start-ops-standalone.ps1 — Ops dev/QA harness only (no Football, no collector)
# 非 Gate 签收路径 — Gate / Integration 请用 start-integration-all.ps1 或 start-ops-dev.ps1
#
# ADR-049 D6: ops-platform-ui-vue (:3000) + oa-server profile dev (:8080).
# DB: application-dev.yml -> 101.37.161.136/wd (NOT dev-local-multidb / NOT Gate path).
# Auth: dev-token-oa-admin + X-Tenant-Id:1 (see OPS-STARTUP-MATRIX.md §4 if post-S0 TRUNCATE).
# SSOT matrix: docs/delivery/OPS-STARTUP-MATRIX.md (Path 1 vs Path 2).
# For Football Gate integration (:5777/:48080), use start-integration-all.ps1 instead.
#
# Usage (from repo root):
#   .\scripts\start-ops-standalone.ps1
#   .\scripts\start-ops-standalone.ps1 -NoFrontend
#
# Logs: scripts/logs/backend-dev-run.log, frontend-dev-run.log

[CmdletBinding()]
param(
    [switch]$NoFrontend,
    [int]$WaitSeconds = 60
)

$ErrorActionPreference = "Continue"

$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$LogDir = Join-Path $PSScriptRoot "logs"
New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

$BackendLog  = Join-Path $LogDir "backend-dev-run.log"
$FrontendLog = Join-Path $LogDir "frontend-dev-run.log"

function Stop-ListenersOnPort {
    param([int]$Port)
    $conns = @(Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
    foreach ($procId in ($conns | Select-Object -ExpandProperty OwningProcess -Unique)) {
        Write-Host "[stop] Port $Port -> PID $procId"
        Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
    }
}

function Start-DevWindow {
    param(
        [string]$Title,
        [string]$WorkingDirectory,
        [string]$Command,
        [string]$LogFile
    )
    if (-not (Test-Path $WorkingDirectory)) {
        Write-Warning "Skip $Title — directory not found: $WorkingDirectory"
        return
    }
    $inner = @"
Set-Location -LiteralPath '$WorkingDirectory'
`$host.UI.RawUI.WindowTitle = '$Title'
& { $Command } *>&1 | Tee-Object -FilePath '$LogFile' -Append
"@
    Start-Process -FilePath "powershell.exe" -ArgumentList @(
        "-NoProfile", "-ExecutionPolicy", "Bypass", "-NoExit", "-Command", $inner
    ) -WindowStyle Minimized | Out-Null
    Write-Host "[start] $Title -> log: $LogFile"
}

Write-Host "=== Ops standalone dev harness (ADR-049) ==="
Write-Host "Repo: $Root"

Write-Host "`n--- Stop ---"
Stop-ListenersOnPort -Port 3000
Stop-ListenersOnPort -Port 8080
Start-Sleep -Seconds 2

Write-Host "`n--- Start ---"
$backendDir = Join-Path $Root "ops-platform-server\ops-platform-module-oa"
$backendCmd = "mvn spring-boot:run '-Dspring-boot.run.profiles=dev'"
Start-DevWindow -Title "oa-server dev :8080" -WorkingDirectory $backendDir -Command $backendCmd -LogFile $BackendLog

if (-not $NoFrontend) {
    $uiDir = Join-Path $Root "ops-platform-ui-vue"
    Start-DevWindow -Title "ops-platform-ui-vue :3000" -WorkingDirectory $uiDir -Command "npm run dev" -LogFile $FrontendLog
} else {
    Write-Host "[start] Frontend skipped (-NoFrontend)"
}

Write-Host "`n--- Waiting up to ${WaitSeconds}s ---"
$urls = @(
    @{ Name = "oa-server health"; Url = "http://localhost:8080/actuator/health" }
)
if (-not $NoFrontend) {
    $urls += @{ Name = "ops UI"; Url = "http://localhost:3000" }
}

$deadline = (Get-Date).AddSeconds($WaitSeconds)
foreach ($entry in $urls) {
    $ok = $false
    while ((Get-Date) -lt $deadline -and -not $ok) {
        try {
            $null = Invoke-WebRequest -Uri $entry.Url -UseBasicParsing -TimeoutSec 5
            Write-Host "[ready] $($entry.Name): $($entry.Url)"
            $ok = $true
        } catch {
            Start-Sleep -Seconds 3
        }
    }
    if (-not $ok) {
        Write-Warning "[pending] $($entry.Name): $($entry.Url) — check logs under $LogDir"
    }
}

Write-Host "`n=== Done ==="
Write-Host "Backend:  http://localhost:8080  (profile dev, no Nacos)"
if (-not $NoFrontend) {
    Write-Host "Frontend: http://localhost:3000  (proxy /admin-api -> :8080)"
    Write-Host "Dev Token: VITE_API_TOKEN=dev-token-oa-admin  (see ops-platform-ui-vue/.env.development)"
}
Write-Host "Logs:     $LogDir"
Write-Host "Football Gate path: .\scripts\start-integration-all.ps1 -SkipBuild  (see docs/delivery/OPS-STARTUP-MATRIX.md)"
