# integration-preflight.ps1 - preflight checks for Football x Ops integration stack
# Redis password must match gateway-integration-local.yaml: 123456
# Beta remote: Import-OpsTestRemoteEnv + start-ops-dev.ps1 -Beta (see OPS-TEST-DB.md)

$script:IntegrationRedisPassword = "123456"

function Test-CommandExists {
    param([string]$Name)
    $null -ne (Get-Command $Name -ErrorAction SilentlyContinue)
}

# Load scripts/integration-config/ops-test-remote.env into process env (gitignored secrets).
# Child Start-Process inherits these so Java ${OPS_TEST_*} placeholders resolve.
function Import-OpsTestRemoteEnv {
    param(
        [string]$Root = "",
        [switch]$Required
    )
    if (-not $Root) {
        $Root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
    }
    $envFile = Join-Path $Root "scripts\integration-config\ops-test-remote.env"
    if (-not (Test-Path $envFile)) {
        $msg = "Missing $envFile — copy ops-test-remote.env.example and fill secrets (docs/delivery/OPS-TEST-DB.md)"
        if ($Required) { Write-Error $msg; return $false }
        Write-Warning $msg
        return $false
    }
    $loaded = 0
    Get-Content -LiteralPath $envFile -Encoding UTF8 | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith("#") -or $line -notmatch "=") { return }
        $k, $v = $line.Split("=", 2)
        $k = $k.Trim()
        $v = $v.Trim().Trim('"').Trim("'")
        if (-not $k) { return }
        Set-Item -Path "env:$k" -Value $v
        $loaded++
    }
    $hostHint = $env:OPS_TEST_DB_HOST
    Write-Host "[beta] Loaded $loaded keys from ops-test-remote.env (DB host=$hostHint)"
    if ($Required -and $hostHint -ne "110.42.49.224") {
        Write-Warning "OPS_TEST_DB_HOST is '$hostHint' (expected 110.42.49.224)"
    }
    return $true
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

function Wait-HttpEndpoint {
    <#
    Poll an HTTP endpoint until it responds or timeout. Returns $true on first HTTP success.
    Uses 127.0.0.1 instead of localhost to avoid Windows IPv6 (::1) connection hangs.
    #>
    param(
        [Parameter(Mandatory = $true)][string]$Url,
        [int]$TimeoutSec = 120,
        [string]$Label = "",
        [int]$PollSec = 3,
        [int]$ProgressIntervalSec = 15
    )
    if ([string]::IsNullOrWhiteSpace($Label)) { $Label = $Url }
    $probeUrl = $Url -replace '://localhost([:/])', '://127.0.0.1$1'
    $started = Get-Date
    $deadline = $started.AddSeconds($TimeoutSec)
    $lastProgress = $started

    Write-Host "Waiting up to ${TimeoutSec}s for $probeUrl ..."

    while ((Get-Date) -lt $deadline) {
        try {
            $resp = Invoke-WebRequest -Uri $probeUrl -UseBasicParsing -TimeoutSec 5
            $elapsed = [int]((Get-Date) - $started).TotalSeconds
            $snippet = $resp.Content
            if ($snippet.Length -gt 80) { $snippet = $snippet.Substring(0, 80) + "..." }
            Write-Host "[ready] $Label (${elapsed}s) $snippet"
            return $true
        } catch {
            $now = Get-Date
            if (($now - $lastProgress).TotalSeconds -ge $ProgressIntervalSec) {
                $elapsed = [int](($now - $started).TotalSeconds)
                $remain = [Math]::Max(0, [int](($deadline - $now).TotalSeconds))
                Write-Host "[wait] $Label ... ${elapsed}s elapsed, ${remain}s left"
                $lastProgress = $now
            }
            Start-Sleep -Seconds $PollSec
        }
    }
    $elapsed = [int]((Get-Date) - $started).TotalSeconds
    Write-Warning "$Label not ready after ${elapsed}s — check log: $probeUrl"
    return $false
}

function Get-IntegrationHealthRows {
    return @(
        @{ Service = "Nacos"; Port = 8848; Url = "http://127.0.0.1:8848/nacos/" },
        @{ Service = "Redis"; Port = 6379; Url = $null },
        @{ Service = "Gateway"; Port = 48080; Url = "http://127.0.0.1:48080/admin-api/system/tenant/simple-list"; Critical = $true },
        @{ Service = "system-server"; Port = 48081; Url = "http://127.0.0.1:48081/actuator/health"; Critical = $true },
        @{ Service = "infra-server"; Port = 48082; Url = "http://127.0.0.1:48082/actuator/health" },
        @{ Service = "pay-server"; Port = 48085; Url = "http://127.0.0.1:48085/actuator/health" },
        @{ Service = "mp-server"; Port = 48086; Url = "http://127.0.0.1:48086/rpc-api/mp/accountInfo/page?pageNo=1&pageSize=1"; Headers = @{ "tenant-id" = "1" }; Critical = $true },
        @{ Service = "member-server"; Port = 48087; Url = "http://127.0.0.1:48087/actuator/health" },
        @{ Service = "match-server"; Port = 48088; Url = "http://127.0.0.1:48088/actuator/health" },
        @{ Service = "oa-server"; Port = 48094; Url = "http://127.0.0.1:48094/actuator/health"; Critical = $true },
        @{ Service = "football-front"; Port = 5777; Url = "http://127.0.0.1:5777/" }
    )
}

function Get-ServiceListenStatus {
    param([int]$Port, [string]$ProbeUrl, [hashtable]$Headers = $null)
    $listen = Test-PortListen -Port $Port
    $http = "DOWN"
    if ($ProbeUrl) {
        try {
            $params = @{ Uri = $ProbeUrl; UseBasicParsing = $true; TimeoutSec = 4 }
            if ($Headers) { $params.Headers = $Headers }
            $resp = Invoke-WebRequest @params
            if ($resp.Content -match '"code"\s*:\s*0') {
                $http = "UP"
            } elseif ($listen) {
                $http = "LISTEN"
            } else {
                $http = "DOWN"
            }
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
        $st = Get-ServiceListenStatus -Port $e.Port -ProbeUrl $e.Url -Headers $e.Headers
        Write-Host ("{0,-18} {1,6} {2,8}" -f $e.Service, $e.Port, $st)
    }
}

function Get-GitBranchName {
    param([string]$RepoDir)
    if (-not (Test-Path (Join-Path $RepoDir ".git"))) { return $null }
    if (-not (Test-CommandExists "git")) { return $null }
    try {
        Push-Location -LiteralPath $RepoDir
        $branch = (& git rev-parse --abbrev-ref HEAD 2>$null).Trim()
        Pop-Location
        if ([string]::IsNullOrWhiteSpace($branch)) { return $null }
        return $branch
    } catch {
        Pop-Location -ErrorAction SilentlyContinue
        return $null
    }
}

function Assert-FootballOpsBranch {
    <#
    .SYNOPSIS
      Warn if football-front / football-backend-saas are not on Gitee ops branch.
      Does not fail the start (WIP may be on another branch); see FOOTBALL-OPS-BRANCH.md.
    #>
    param(
        [string]$Root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
    )
    $repos = @(
        @{ Name = "football-front"; Path = (Join-Path $Root "football-front") },
        @{ Name = "football-backend-saas"; Path = (Join-Path $Root "football-backend-saas") }
    )
    $allOk = $true
    foreach ($r in $repos) {
        if (-not (Test-Path $r.Path)) {
            Write-Warning "$($r.Name) missing: $($r.Path)"
            $allOk = $false
            continue
        }
        $branch = Get-GitBranchName -RepoDir $r.Path
        if ($null -eq $branch) {
            Write-Warning "$($r.Name): cannot read git branch"
            $allOk = $false
            continue
        }
        if ($branch -eq "ops") {
            Write-Host "[ok] $($r.Name) branch=ops"
        } else {
            Write-Host "[warn] $($r.Name) branch=$branch (expected ops for OPS merge / Gate)" -ForegroundColor Yellow
            Write-Host "       See docs/delivery/FOOTBALL-OPS-BRANCH.md" -ForegroundColor Yellow
            $allOk = $false
        }
    }
    return $allOk
}

function Ensure-OpsViewsMounted {
    <#
    .SYNOPSIS
      Phase A: OPS pages live under football-front .../views/ops via mount-ops-all.py.
      Gate path uses :5777 only — ops-platform-ui-vue :3000 is NOT required.
      Auto-runs mount when views/ops is missing/empty, or when -ForceMount is set.
    #>
    param(
        [string]$Root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path,
        [switch]$ForceMount,
        [switch]$SkipMount
    )
    $viewsOps = Join-Path $Root "football-front\apps\web-ele\src\views\ops"
    $mountScript = Join-Path $Root "scripts\mount-ops-all.py"
    $vueCount = 0
    if (Test-Path $viewsOps) {
        $vueCount = @(Get-ChildItem -LiteralPath $viewsOps -Recurse -Filter "*.vue" -ErrorAction SilentlyContinue).Count
    }

    if ($SkipMount) {
        if ($vueCount -gt 0) {
            Write-Host "[ok] views/ops present ($vueCount vue) — SkipMount"
        } else {
            Write-Host "[warn] views/ops missing/empty and -SkipMount set; OPS pages will 404 on :5777" -ForegroundColor Yellow
        }
        return ($vueCount -gt 0)
    }

    $needMount = $ForceMount -or ($vueCount -lt 1)
    if (-not $needMount) {
        Write-Host "[ok] football-front views/ops ready ($vueCount vue) — Gate UI :5777 (standalone :3000 not required)"
        return $true
    }

    if (-not (Test-Path $mountScript)) {
        Write-Host "[fail] mount-ops-all.py missing; cannot mount OPS pages into football-front" -ForegroundColor Red
        return $false
    }
    $py = if (Test-CommandExists "python") { "python" } elseif (Test-CommandExists "py") { "py" } else { $null }
    if (-not $py) {
        Write-Host "[fail] Python not found; cannot run mount-ops-all.py" -ForegroundColor Red
        Write-Host "       Manual: python scripts/mount-ops-all.py" -ForegroundColor Yellow
        return $false
    }

    $reason = if ($ForceMount) { "ForceMount" } else { "views/ops missing or empty" }
    Write-Host "[mount] $reason -> $py scripts/mount-ops-all.py ..."
    Push-Location -LiteralPath $Root
    try {
        & $py $mountScript
        $mountOk = $LASTEXITCODE -eq 0
    } catch {
        Write-Warning "mount-ops-all.py failed: $_"
        $mountOk = $false
    } finally {
        Pop-Location
    }

    $syncScript = Join-Path $Root "scripts\sync-ops-layout-components.py"
    if ($mountOk -and (Test-Path $syncScript)) {
        Write-Host "[mount] sync-ops-layout-components.py ..."
        try {
            & $py $syncScript
        } catch {
            Write-Warning "sync-ops-layout-components.py failed: $_"
        }
    }

    $vueCount = 0
    if (Test-Path $viewsOps) {
        $vueCount = @(Get-ChildItem -LiteralPath $viewsOps -Recurse -Filter "*.vue" -ErrorAction SilentlyContinue).Count
    }
    if ($vueCount -gt 0) {
        Write-Host "[ok] views/ops mounted ($vueCount vue)"
        return $true
    }
    Write-Host "[fail] views/ops still empty after mount" -ForegroundColor Red
    return $false
}

function Ensure-OpsFrontDeps {
    <#
    .SYNOPSIS
      football-front needs full pnpm install (vite) plus OPS chart deps (echarts via link-ops-deps.ps1).
    #>
    param(
        [string]$Root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path,
        [switch]$AutoLink
    )
    $frontDir = Join-Path $Root "football-front"
    $viteBin = Join-Path $frontDir "node_modules\.bin\vite.cmd"
    $vitePkg = Join-Path $frontDir "node_modules\vite"
    $ok = $true

    if (-not ((Test-Path $viteBin) -or (Test-Path $vitePkg))) {
        Write-Host "[fix] football-front vite missing — running pnpm install (first run may take several minutes) ..." -ForegroundColor Yellow
        if (-not (Test-CommandExists "pnpm")) {
            Write-Host "[fail] pnpm not found; install Node.js + pnpm then: cd football-front && pnpm install" -ForegroundColor Red
            return $false
        }
        if (-not (Test-Path (Join-Path $frontDir "package.json"))) {
            Write-Host "[fail] football-front/package.json missing" -ForegroundColor Red
            return $false
        }
        Push-Location $frontDir
        try {
            & pnpm install
            if ($LASTEXITCODE -ne 0) {
                Write-Host "[warn] pnpm install exited $LASTEXITCODE — trying pnpm rebuild ..." -ForegroundColor Yellow
                & pnpm rebuild 2>&1 | Out-Null
            }
        } finally {
            Pop-Location
        }
    }

    if ((Test-Path $viteBin) -or (Test-Path $vitePkg)) {
        Write-Host "[ok] football-front vite present"
    } else {
        $pnpmVite = Get-ChildItem -Path (Join-Path $frontDir "node_modules\.pnpm") -Recurse -Filter "vite.js" -ErrorAction SilentlyContinue |
            Where-Object { $_.FullName -match '\\vite\\bin\\vite\.js$' -and $_.FullName -match 'vite@7' } |
            Select-Object -First 1
        if ($pnpmVite) {
            Write-Host "[ok] football-front vite in .pnpm (bin link missing; run-football-front-dev.ps1 fallback)" -ForegroundColor Yellow
        } else {
            Write-Host "[fail] vite missing — cd football-front && pnpm install (close apps locking node_modules if EPERM)" -ForegroundColor Red
            return $false
        }
    }

    $echarts = Join-Path $frontDir "node_modules\echarts"
    $markdownIt = Join-Path $frontDir "node_modules\markdown-it"
    $tiptap = Join-Path $frontDir "node_modules\@tiptap\vue-3"
    $opsDepsOk = (Test-Path $echarts) -and (Test-Path $markdownIt) -and (Test-Path $tiptap)
    if ($opsDepsOk) {
        Write-Host "[ok] football-front OPS runtime deps present (echarts, markdown-it, @tiptap/vue-3)"
        return $ok
    }
    if (-not (Test-Path $echarts)) { Write-Host "[warn] football-front missing echarts" -ForegroundColor Yellow }
    if (-not (Test-Path $markdownIt)) { Write-Host "[warn] football-front missing markdown-it (content/AI pages blank)" -ForegroundColor Yellow }
    if (-not (Test-Path $tiptap)) { Write-Host "[warn] football-front missing @tiptap/vue-3 (content editor blank)" -ForegroundColor Yellow }
    $linkScript = Join-Path $Root "scripts\link-ops-deps.ps1"
    if ($AutoLink -and (Test-Path $linkScript)) {
        Write-Host "[fix] echarts missing — running link-ops-deps.ps1 ..."
        try {
            & $linkScript
        } catch {
            Write-Warning "link-ops-deps.ps1 failed: $_"
        }
        if (Test-Path $echarts) {
            Write-Host "[ok] echarts linked"
            return $ok
        }
    }
    Write-Host "[warn] football-front missing OPS runtime deps — content/editor pages may white-screen until:" -ForegroundColor Yellow
    Write-Host "       cd football-front && pnpm install   (preferred; see apps/web-ele/package.json)" -ForegroundColor Yellow
    Write-Host "       .\scripts\link-ops-deps.ps1         (junction fallback; then restart :5777)" -ForegroundColor Yellow
    return $false
}

function Ensure-LocalSftpKey {
    <#
    .SYNOPSIS
      Dummy SSH key so infra-server SftpConnectionPool can init locally (D:/zhengshu key absent).
    #>
    param(
        [string]$Root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
    )
    $key = Join-Path $Root "scripts\integration-config\local-sftp-id_rsa"
    if (Test-Path $key) {
        Write-Host "[ok] local SFTP key present (infra-server)"
        return $true
    }
    if (-not (Test-CommandExists "ssh-keygen")) {
        Write-Warning "ssh-keygen missing; cannot create $key — infra-server may fail to start"
        return $false
    }
    Write-Host "[create] $key (dummy, local integration only)"
    & ssh-keygen -t rsa -b 2048 -f $key -N '""' -q
    return (Test-Path $key)
}

function Ensure-FootballFrontLocalApi {
    <#
    .SYNOPSIS
      Local integration: football-front must proxy /admin-api → localhost:48080.
      apiURL is VITE_GLOB_API_URL (/admin-api) so Vite proxy target is authoritative;
      VITE_BASE_URL is secondary (ws/swagger). Remote proxy → no local OPS menus (6100+).
    #>
    param(
        [string]$Root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
    )
    $ok = $true
    $viteCfg = Join-Path $Root "football-front\apps\web-ele\vite.config.mts"
    if (Test-Path $viteCfg) {
        $vite = Get-Content -LiteralPath $viteCfg -Raw -Encoding UTF8
        # active (uncommented) proxy target line
        if ($vite -match '(?m)^\s*target:\s*[''"]([^''"]+)[''"]') {
            $target = $Matches[1]
            if ($target -match 'localhost:48080|127\.0\.0\.1:48080') {
                Write-Host "[ok] football-front vite proxy target=$target"
            } else {
                Write-Host "[fail] football-front vite proxy target=$target (expected localhost:48080)" -ForegroundColor Red
                Write-Host "       UI /admin-api is proxied away from local shenyu-system; OPS menus (6100+) hidden." -ForegroundColor Yellow
                Write-Host "       Fix: $viteCfg then restart :5777" -ForegroundColor Yellow
                $ok = $false
            }
        } else {
            Write-Warning "No proxy target found in $viteCfg"
            $ok = $false
        }
    } else {
        Write-Warning "vite.config.mts missing: $viteCfg"
        $ok = $false
    }

    $envFile = Join-Path $Root "football-front\apps\web-ele\.env.development"
    if (Test-Path $envFile) {
        $content = Get-Content -LiteralPath $envFile -Raw -Encoding UTF8
        if ($content -match '(?m)^\s*VITE_BASE_URL\s*=\s*(.+)\s*$') {
            $url = $Matches[1].Trim().Trim("'").Trim('"')
            if ($url -match 'localhost:48080|127\.0\.0\.1:48080') {
                Write-Host "[ok] football-front VITE_BASE_URL=$url"
            } else {
                Write-Host "[fix] VITE_BASE_URL=$url -> http://localhost:48080 (local Gate default)" -ForegroundColor Yellow
                $fixed = [regex]::Replace(
                    $content,
                    '(?m)^(\s*VITE_BASE_URL\s*=\s*).+$',
                    '${1}http://localhost:48080'
                )
                Set-Content -LiteralPath $envFile -Value $fixed -Encoding UTF8 -NoNewline
                Write-Host "       Restart :5777 if already running (Vite reads env only at boot)" -ForegroundColor Yellow
            }
        }
    }
    return $ok
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
    Write-Host "3. member-server :48087 DOWN or Python mock -> Football 方案列表 500 (article/page 404)"
    Write-Host "   Log: $LogDir\member-server-integration.log"
    Write-Host "   Fix: run start-ops-dev.ps1 (default FullMemberServer); avoid -UseMemberMock"
    Write-Host "4. oa-server :48094 DOWN -> OPS pages (内容管理/任务/IP组) all fail with 系统错误"
    Write-Host "   Log: $LogDir\oa-server-nacos-run.log"
    Write-Host "   Beta: Flyway 9.x vs MySQL 5.7 -> FlywayEditionUpgradeRequiredException (pin flyway 10.22+ in pom)"
    Write-Host "   Local: MySQL localhost:3306 five DBs missing -> oa API 500"
    Write-Host "5. -SkipBuild without jars -> run with -FirstRun"
    Write-Host "6. UI missing OPS menus but local API has them -> vite proxy / VITE_BASE_URL not localhost:48080"
    Write-Host "   Check: football-front/apps/web-ele/vite.config.mts + .env.development; restart :5777"
    Write-Host "7. views/ops empty -> python scripts/mount-ops-all.py  (or start-ops-dev.ps1 -MountOps)"
    Write-Host "8. football-* not on ops branch -> see docs/delivery/FOOTBALL-OPS-BRANCH.md"
    Write-Host "9. football-front :5777 DOWN -> vite missing or pnpm dev:ele crashed"
    Write-Host "   Log: $LogDir\football-front-dev.log"
    Write-Host "   Fix: cd football-front && pnpm install ; then restart start-ops-dev.ps1"
    Write-Host ""
    Write-Host "Logs: $LogDir"
    Write-Host "Stop: .\scripts\stop-integration-all.ps1"
}
