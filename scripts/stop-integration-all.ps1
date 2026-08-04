# stop-integration-all.ps1 - 停止 Football x Ops 本地集成栈
#
# 用法（仓库根目录）:
#   .\scripts\stop-integration-all.ps1
#   .\scripts\stop-integration-all.ps1 -SkipDocker   # 仅杀 Java/Node 进程，保留 Nacos/Redis 容器
#
# 释放端口: 48080 48081 48082 48085 48086 48087 48094 5777 (+ 8848 if not SkipDocker)
# 注意: 默认不停止 Redis :6379，避免反复丢失 requirepass 配置

[CmdletBinding()]
param(
    [switch]$SkipDocker,
    [switch]$StopRedis
)

$ErrorActionPreference = "Continue"
$Ports = @(5777, 48094, 48088, 48087, 48085, 48082, 48081, 48086, 48080)
if ($StopRedis) { $Ports += 6379 }

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

Write-Host "=== Stop integration stack ==="

foreach ($p in $Ports) {
    Stop-ListenersOnPort -Port $p
}

Start-Sleep -Seconds 2

if (-not $SkipDocker) {
    foreach ($name in @("nacos-standalone-local", "redis-integration-local")) {
        $exists = docker ps -a --filter "name=^/${name}$" --format "{{.Names}}" 2>$null
        if ($exists -eq $name) {
            Write-Host "[stop] docker stop $name"
            docker stop $name 2>$null | Out-Null
        }
    }
    # 8848 may still be held by a non-Docker Nacos; do NOT kill :6379 here — native redis-server
    # would restart without password and break Gateway auth (see integration-preflight.ps1).
    Stop-ListenersOnPort -Port 8848
}

if ($StopRedis) {
    Stop-ListenersOnPort -Port 6379
}

Write-Host "=== Done ==="
