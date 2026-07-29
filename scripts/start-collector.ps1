# start-collector.ps1 — Standalone unify-collector-api (:8000)
#
# Usage (from repo root):
#   .\scripts\start-collector.ps1
#   .\scripts\start-collector.ps1 -Restart    # kill existing listener on :8000 first
#   .\scripts\start-collector.ps1 -WaitSeconds 90
#
# Health: http://127.0.0.1:8000/livez
# Docs:   http://127.0.0.1:8000/docs
# Stop:   .\scripts\stop-collector.ps1
# Log:    scripts/logs/collector-run.log

[CmdletBinding()]
param(
    [switch]$Restart,
    [int]$WaitSeconds = 60
)

$ErrorActionPreference = "Continue"

$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$CollectorDir = Join-Path $Root "unify-collector-api"
$LogDir = Join-Path $PSScriptRoot "logs"
$CollectorLog = Join-Path $LogDir "collector-run.log"
$LivezUrl = "http://127.0.0.1:8000/livez"
$DocsUrl = "http://127.0.0.1:8000/docs"
$Port = 8000

function Stop-ListenersOnPort {
    param([int]$Port)
    $conns = @(Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
    if ($conns.Count -eq 0) {
        Write-Host "[stop] Port $Port is already free"
        return
    }
    foreach ($procId in ($conns | Select-Object -ExpandProperty OwningProcess -Unique)) {
        $proc = Get-Process -Id $procId -ErrorAction SilentlyContinue
        $label = if ($proc) { $proc.ProcessName } else { "pid" }
        Write-Host "[stop] Port $Port -> stopping $label (PID $procId)"
        Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
    }
}

function Test-CollectorLivez {
    try {
        $null = Invoke-WebRequest -Uri $LivezUrl -UseBasicParsing -TimeoutSec 5
        return $true
    } catch {
        return $false
    }
}

function Get-ListenerPidsOnPort {
    param([int]$Port)
    @(Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty OwningProcess -Unique)
}

function Ensure-CollectorVenv {
    param([string]$Dir)
    $venvPython = Join-Path $Dir ".venv\Scripts\python.exe"
    if (Test-Path -LiteralPath $venvPython) {
        return $venvPython
    }
    if (-not (Test-CommandExists "python")) {
        Write-Error "Python not found. Install Python 3.11+ and add to PATH, or create unify-collector-api/.venv manually."
        exit 1
    }
    Write-Host "[venv] Creating .venv under $Dir ..."
    Push-Location -LiteralPath $Dir
    try {
        & python -m venv .venv
        if ($LASTEXITCODE -ne 0) {
            Write-Error "python -m venv .venv failed (exit $LASTEXITCODE)"
            exit 1
        }
        & $venvPython -m pip install --upgrade pip
        & $venvPython -m pip install -r requirements.txt
        if ($LASTEXITCODE -ne 0) {
            Write-Error "pip install -r requirements.txt failed"
            exit 1
        }
    } finally {
        Pop-Location
    }
    return $venvPython
}

function Test-CommandExists {
    param([string]$Name)
    $null -ne (Get-Command $Name -ErrorAction SilentlyContinue)
}

New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

if (-not (Test-Path -LiteralPath $CollectorDir)) {
    Write-Error "Directory not found: $CollectorDir"
    exit 1
}

Write-Host "=== Start unify-collector-api :$Port ==="
Write-Host "Repo: $CollectorDir"

if ((Get-ListenerPidsOnPort -Port $Port).Count -gt 0) {
    if (Test-CollectorLivez) {
        $pids = Get-ListenerPidsOnPort -Port $Port
        Write-Host "[ok] Collector already running on port $Port (PID $($pids -join ', '))"
        Write-Host "Livez: $LivezUrl"
        Write-Host "Docs:  $DocsUrl"
        Write-Host "Stop:  .\scripts\stop-collector.ps1"
        exit 0
    }
    if ($Restart) {
        Write-Host "[restart] Port $Port in use but livez failed; stopping listener(s) ..."
        Stop-ListenersOnPort -Port $Port
        Start-Sleep -Seconds 2
    } else {
        Write-Warning "Port $Port is in use but http://127.0.0.1:$Port/livez is not OK. Re-run with -Restart."
        exit 2
    }
}

$pythonExe = Ensure-CollectorVenv -Dir $CollectorDir
Write-Host "[start] $pythonExe run.py (port $Port from app.config / .env)"

$inner = @"
Set-Location -LiteralPath '$CollectorDir'
`$host.UI.RawUI.WindowTitle = 'unify-collector-api :8000'
& '$pythonExe' run.py *>&1 | Tee-Object -FilePath '$CollectorLog' -Append
"@

$shell = Start-Process -FilePath "powershell.exe" -ArgumentList @(
    "-NoProfile", "-ExecutionPolicy", "Bypass", "-NoExit", "-Command", $inner
) -WindowStyle Minimized -PassThru

Write-Host "[start] Launched collector shell (PID $($shell.Id)); log: $CollectorLog"
Write-Host "`n--- Waiting up to ${WaitSeconds}s for livez ---"

$deadline = (Get-Date).AddSeconds($WaitSeconds)
$ready = $false
while ((Get-Date) -lt $deadline -and -not $ready) {
    if (Test-CollectorLivez) {
        $ready = $true
        break
    }
    Start-Sleep -Seconds 2
}

$listenerPids = Get-ListenerPidsOnPort -Port $Port
if ($ready) {
    Write-Host "[ready] $LivezUrl"
    Write-Host ""
    Write-Host "=== Collector UP ==="
    if ($listenerPids.Count -gt 0) {
        Write-Host "PID(s) on :$Port : $($listenerPids -join ', ')"
    }
    Write-Host "Livez: $LivezUrl"
    Write-Host "Docs:  $DocsUrl  (API token: unify-collector-api/.env, OA default test-key-2026)"
    Write-Host "Log:   $CollectorLog"
    Write-Host "Stop:  .\scripts\stop-collector.ps1"
    exit 0
}

Write-Warning "[pending] livez not ready after ${WaitSeconds}s. Check $CollectorLog and the minimized PowerShell window."
if ($listenerPids.Count -gt 0) {
    Write-Host "PID(s) on :$Port : $($listenerPids -join ', ')"
}
Write-Host "Stop:  .\scripts\stop-collector.ps1"
exit 2
