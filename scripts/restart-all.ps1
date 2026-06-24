# restart-all.ps1 — One-click restart of local dev stack (Windows PowerShell)
#
# Usage (from repo root):
#   .\scripts\restart-all.ps1
#
# Optional:
#   .\scripts\restart-all.ps1 -NoFrontend
#   .\scripts\restart-all.ps1 -WaitSeconds 90
#
# Stops listeners on ports 8000 (collector), 8080 (backend), 3000 (frontend),
# then starts unify-collector-api, ops-platform-server (dev profile), and ops-platform-ui-vue.
# Logs: scripts/logs/collector-run.log, backend-dev-run.log, frontend-dev-run.log

[CmdletBinding()]
param(
    [switch]$NoFrontend,
    [int]$WaitSeconds = 45
)

$ErrorActionPreference = "Continue"

if ($args -contains '--no-frontend') {
    $NoFrontend = $true
}

$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$LogDir = Join-Path $PSScriptRoot "logs"
New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

$CollectorLog = Join-Path $LogDir "collector-run.log"
$BackendLog   = Join-Path $LogDir "backend-dev-run.log"
$FrontendLog  = Join-Path $LogDir "frontend-dev-run.log"

function Stop-ListenersOnPort {
    param([int]$Port)
    $conns = @(Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
    if ($conns.Count -eq 0) {
        Write-Host "[stop] Port $Port is already free"
        return
    }
    $pids = $conns | Select-Object -ExpandProperty OwningProcess -Unique
    foreach ($procId in $pids) {
        $proc = Get-Process -Id $procId -ErrorAction SilentlyContinue
        $label = if ($proc) { $proc.ProcessName } else { "pid" }
        Write-Host "[stop] Port $Port -> stopping $label (PID $procId)"
        Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
    }
}

function Wait-PortFree {
    param([int]$Port, [int]$TimeoutSec = 15)
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        $busy = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
        if (-not $busy) { return $true }
        Start-Sleep -Milliseconds 500
    }
    Write-Warning "Port $Port still in use after ${TimeoutSec}s"
    return $false
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

Write-Host "=== Restart dev stack ==="
Write-Host "Repo: $Root"

Write-Host "`n--- Stop ---"
Stop-ListenersOnPort -Port 3000
Stop-ListenersOnPort -Port 8080
Stop-ListenersOnPort -Port 8000
Start-Sleep -Seconds 2
Wait-PortFree -Port 3000 | Out-Null
Wait-PortFree -Port 8080 | Out-Null
Wait-PortFree -Port 8000 | Out-Null

Write-Host "`n--- Start ---"

$collectorDir = Join-Path $Root "unify-collector-api"
$venvPython = Join-Path $collectorDir ".venv\Scripts\python.exe"
$pythonExe = if (Test-Path -LiteralPath $venvPython) { $venvPython } else { "python" }
$collectorCmd = "& '$pythonExe' run.py"
Start-DevWindow -Title "unify-collector-api :8000" -WorkingDirectory $collectorDir -Command $collectorCmd -LogFile $CollectorLog

$backendDir = Join-Path $Root "ops-platform-server\ops-platform-module-oa"
$backendCmd = "mvn spring-boot:run `"-Dspring-boot.run.profiles=dev`""
Start-DevWindow -Title "ops-platform-server :8080" -WorkingDirectory $backendDir -Command $backendCmd -LogFile $BackendLog

if (-not $NoFrontend) {
    $uiDir = Join-Path $Root "ops-platform-ui-vue"
    $frontendCmd = "npm run dev"
    Start-DevWindow -Title "ops-platform-ui-vue :3000" -WorkingDirectory $uiDir -Command $frontendCmd -LogFile $FrontendLog
} else {
    Write-Host "[start] Frontend skipped (-NoFrontend)"
}

Write-Host "`n--- Waiting up to ${WaitSeconds}s for services ---"
$urls = @(
    @{ Name = "Collector livez"; Url = "http://127.0.0.1:8000/livez" },
    @{ Name = "Backend health"; Url = "http://localhost:8080/actuator/health" }
)
if (-not $NoFrontend) {
    $urls += @{ Name = "Frontend"; Url = "http://localhost:3000" }
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
        Write-Warning "[pending] $($entry.Name): $($entry.Url) — still starting; check logs under $LogDir"
    }
}

Write-Host "`n=== Done ==="
Write-Host "Collector: http://127.0.0.1:8000/docs  (token: see unify-collector-api/.env, OA default test-key-2026)"
Write-Host "Backend:   http://localhost:8080"
if (-not $NoFrontend) {
    Write-Host "Frontend:  http://localhost:3000"
}
Write-Host "Logs:      $LogDir"

