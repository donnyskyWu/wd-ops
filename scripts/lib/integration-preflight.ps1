# integration-preflight.ps1 - preflight checks for Football x Ops integration stack
# Redis password must match gateway-integration-local.yaml: 123456

$script:IntegrationRedisPassword = "123456"

function Test-CommandExists {
    param([string]$Name)
    $null -ne (Get-Command $Name -ErrorAction SilentlyContinue)
}

function Test-PortListen {
    param([int]$Port)
    return [bool](Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
}

function Test-RedisAuth {
    param([string]$Password = $script:IntegrationRedisPassword)
    if (-not (Test-CommandExists "redis-cli")) { return $null }
    try {
        if ($Password) {
            $out = & redis-cli -a $Password ping 2>&1
        } else {
            $out = & redis-cli ping 2>&1
        }
        return ($out -match "PONG")
    } catch {
        return $false
    }
}

# Returns: Ok | NoPassword | WrongPassword | PortClosed | NoCli
function Get-RedisAuthState {
    param([string]$Password = $script:IntegrationRedisPassword)
    if (-not (Test-PortListen -Port 6379)) { return "PortClosed" }
    if (-not (Test-CommandExists "redis-cli")) { return "NoCli" }
    if (Test-RedisAuth -Password $Password) { return "Ok" }
    if (Test-RedisAuth -Password "") { return "NoPassword" }
    return "WrongPassword"
}

function Wait-RedisPort {
    param([int]$TimeoutSec = 45)
    if (Test-PortListen -Port 6379) { return $true }
    Write-Host "[wait] Redis :6379 not listening; waiting up to ${TimeoutSec}s (Windows service may be restarting) ..."
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        if (Test-PortListen -Port 6379) { return $true }
        Start-Sleep -Seconds 2
    }
    return $false
}

function Stop-PortListeners {
    param([int]$Port)
    $conns = @(Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
    foreach ($procId in ($conns | Select-Object -ExpandProperty OwningProcess -Unique)) {
        $proc = Get-Process -Id $procId -ErrorAction SilentlyContinue
        $label = if ($proc) { $proc.ProcessName } else { "pid" }
        Write-Host "[stop] Port $Port -> $label (PID $procId)"
        Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
    }
}

function Set-LocalRedisPassword {
    param([string]$Password = $script:IntegrationRedisPassword)
    if (-not (Test-CommandExists "redis-cli")) { return $false }
    $setOut = & redis-cli CONFIG SET requirepass $Password 2>&1
    if (-not (Test-RedisAuth -Password $Password)) {
        Write-Warning "CONFIG SET failed: $setOut"
        return $false
    }
    Write-Host "[ok] Set local redis-server password to $Password"
    $rewriteOut = & redis-cli -a $Password CONFIG REWRITE 2>&1
    if ($rewriteOut -match "OK") {
        Write-Host "[ok] CONFIG REWRITE persisted password"
    } else {
        Write-Host "[warn] CONFIG REWRITE skipped (password active for this session): $rewriteOut"
    }
    return $true
}

function Start-DockerIntegrationRedis {
    param(
        [string]$Password = $script:IntegrationRedisPassword,
        [int]$TimeoutSec = 30
    )
    if (-not (Test-CommandExists "docker")) { return $false }

    $name = "redis-integration-local"
    $existing = docker ps -a --filter "name=^/${name}$" --format "{{.Names}}" 2>$null
    if ($existing -eq $name) {
        Write-Host "[start] Docker container $name ..."
        docker start $name | Out-Null
    } else {
        Write-Host "[create] Docker redis:7 --requirepass $Password on :6379 ..."
        docker run -d --name $name -p 6379:6379 redis:7 redis-server --requirepass $Password | Out-Null
    }

    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        if (Test-RedisAuth -Password $Password) {
            Write-Host "[ok] Docker Redis ready (password=$Password)"
            return $true
        }
        Start-Sleep -Seconds 2
    }
    Write-Warning "Docker Redis start timeout"
    return $false
}

function Ensure-IntegrationRedis {
    param([string]$Password = $script:IntegrationRedisPassword)

    $state = Get-RedisAuthState -Password $Password
    if ($state -eq "Ok") {
        Write-Host "[ok] Redis :6379 auth OK (password=$Password)"
        return $true
    }

    if ($state -eq "PortClosed") {
        if (-not (Wait-RedisPort -TimeoutSec 45)) {
            if (Test-CommandExists "docker") {
                return Start-DockerIntegrationRedis -Password $Password
            }
            Write-Warning "Redis not listening on :6379 and Docker unavailable"
            return $false
        }
        $state = Get-RedisAuthState -Password $Password
        if ($state -eq "Ok") {
            Write-Host "[ok] Redis :6379 auth OK (password=$Password)"
            return $true
        }
    }

    if ($state -eq "NoPassword") {
        Write-Host "[fix] Redis :6379 has no password; setting requirepass $Password ..."
        if (Set-LocalRedisPassword -Password $Password) { return $true }
    }

    if ($state -eq "WrongPassword") {
        Write-Host "[fix] Redis :6379 password mismatch (integration requires $Password)"
        if (Test-CommandExists "docker") {
            Write-Host "[fix] Replacing listener with Docker redis-integration-local ..."
            Stop-PortListeners -Port 6379
            Start-Sleep -Seconds 2
            if (Start-DockerIntegrationRedis -Password $Password) { return $true }
        }
        Write-Warning "Cannot change password. Stop local redis-server or run: redis-cli -a <current> CONFIG SET requirepass $Password"
        return $false
    }

    if ($state -eq "NoCli") {
        Write-Warning "redis-cli not in PATH but :6379 is listening; install Redis CLI or use Docker redis"
        return $false
    }

    if (Test-CommandExists "docker") {
        return Start-DockerIntegrationRedis -Password $Password
    }

    Write-Warning "Cannot ensure Redis password on :6379"
    return $false
}

function Test-MySqlLocal {
    param(
        [string]$MySqlHost = "127.0.0.1",
        [int]$Port = 3306,
        [string]$User = "root",
        [string]$Password = "root",
        [string[]]$Databases = @("wd", "shenyu-member", "shenyu-mp", "shenyu-pay", "shenyu-system")
    )

    if (-not (Test-PortListen -Port $Port)) {
        Write-Warning "MySQL not listening on ${MySqlHost}:$Port"
        return $false
    }

    if (-not (Test-CommandExists "mysql")) {
        Write-Host "[warn] mysql client not found; only checked port $Port"
        return $true
    }

    $missing = @()
    foreach ($db in $Databases) {
        $sql = "SELECT SCHEMA_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME='$db'"
        $out = & mysql -h $MySqlHost -P $Port -u $User "-p$Password" -N -e $sql 2>&1
        if ($LASTEXITCODE -ne 0) {
            Write-Warning "MySQL connect failed: $out"
            return $false
        }
        if (-not ($out -match $db)) { $missing += $db }
    }
    if ($missing.Count -gt 0) {
        Write-Warning "Missing DBs: $($missing -join ', ')"
        return $false
    }
    Write-Host "[ok] MySQL databases ready: $($Databases -join ', ')"
    return $true
}

function Wait-DockerEngine {
    param([int]$TimeoutSec = 120)
    if (-not (Test-CommandExists "docker")) {
        Write-Warning "Docker not installed; Nacos may be skipped"
        return $false
    }
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        try {
            docker info *> $null
            if ($LASTEXITCODE -eq 0) {
                Write-Host "[ok] Docker engine ready"
                return $true
            }
        } catch { }
        $remain = [int](($deadline - (Get-Date)).TotalSeconds)
        Write-Host "[wait] Docker not ready, open Docker Desktop (${remain}s left) ..."
        Start-Sleep -Seconds 5
    }
    Write-Warning "Docker not ready within ${TimeoutSec}s; continuing without Nacos"
    return $false
}

function Get-IntegrationHealthRows {
    return @(
        @{ Service = "Nacos"; Port = 8848; Url = "http://127.0.0.1:8848/nacos/" },
        @{ Service = "Redis"; Port = 6379; Url = $null },
        @{ Service = "Gateway"; Port = 48080; Url = "http://127.0.0.1:48080/admin-api/system/tenant/simple-list"; Critical = $true },
        @{ Service = "system-server"; Port = 48081; Url = "http://127.0.0.1:48081/actuator/health"; Critical = $true },
        @{ Service = "mp-server"; Port = 48086; Url = "http://127.0.0.1:48086/actuator/health"; Critical = $true },
        @{ Service = "member-mock"; Port = 48087; Url = "http://127.0.0.1:48087/actuator/health" },
        @{ Service = "oa-server"; Port = 48094; Url = "http://127.0.0.1:48094/actuator/health"; Critical = $true },
        @{ Service = "football-front"; Port = 5777; Url = "http://127.0.0.1:5777/" }
    )
}

function Get-ServiceListenStatus {
    param([int]$Port, [string]$ProbeUrl)
    $listen = Test-PortListen -Port $Port
    $http = "DOWN"
    if ($ProbeUrl) {
        try {
            $null = Invoke-WebRequest -Uri $ProbeUrl -UseBasicParsing -TimeoutSec 4
            $http = "UP"
        } catch {
            if ($listen) { $http = "LISTEN" } else { $http = "DOWN" }
        }
    } elseif ($listen) { $http = "LISTEN" }
    return $http
}

function Show-IntegrationHealthTable {
    param([switch]$SkipOa, [switch]$SkipFrontend)
    $rows = Get-IntegrationHealthRows
    Write-Host ""
    Write-Host ("{0,-18} {1,6} {2,8}" -f "Service", "Port", "Status")
    Write-Host ("-" * 36)
    foreach ($e in $rows) {
        if ($e.Service -eq "oa-server" -and $SkipOa) { continue }
        if ($e.Service -eq "football-front" -and $SkipFrontend) { continue }
        $st = Get-ServiceListenStatus -Port $e.Port -ProbeUrl $e.Url
        Write-Host ("{0,-18} {1,6} {2,8}" -f $e.Service, $e.Port, $st)
    }
}

function Show-IntegrationTroubleshooting {
    param([string]$LogDir)
    Write-Host ""
    Write-Host "=== Troubleshooting (system error in UI) ===" -ForegroundColor Yellow
    Write-Host "1. Gateway :48080 DOWN -> login/API fails (most common)"
    Write-Host "   Log: $LogDir\gateway-integration.log"
    Write-Host "   Fix: run start-ops-dev.ps1 (auto-sets Redis password 123456)"
    Write-Host "2. system-server :48081 DOWN -> cannot login"
    Write-Host "   Log: $LogDir\system-server-integration.log"
    Write-Host "3. MySQL localhost:3306 five DBs missing -> oa API 500"
    Write-Host "4. -SkipBuild without jars -> run with -FirstRun"
    Write-Host ""
    Write-Host "Logs: $LogDir"
    Write-Host "Stop: .\scripts\stop-integration-all.ps1"
}
