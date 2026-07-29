# stop-collector.ps1 — Stop unify-collector-api listener on :8000
#
# Usage (from repo root):
#   .\scripts\stop-collector.ps1

[CmdletBinding()]
param()

$ErrorActionPreference = "Continue"
$Port = 8000

function Stop-ListenersOnPort {
    param([int]$Port)
    $conns = @(Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
    if ($conns.Count -eq 0) {
        Write-Host "[ok] Port $Port free"
        return
    }
    foreach ($procId in ($conns | Select-Object -ExpandProperty OwningProcess -Unique)) {
        $proc = Get-Process -Id $procId -ErrorAction SilentlyContinue
        $label = if ($proc) { $proc.ProcessName } else { "pid" }
        Write-Host "[stop] Port $Port -> $label (PID $procId)"
        Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
    }
}

Write-Host "=== Stop unify-collector-api :$Port ==="
Stop-ListenersOnPort -Port $Port
Start-Sleep -Seconds 1
Stop-ListenersOnPort -Port $Port
Write-Host "=== Done ==="
