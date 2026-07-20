# start-integration-all.ps1 - 一键启动 Football x Ops 本地集成全栈 (Gate 路径)
#
# SSOT matrix: docs/delivery/OPS-STARTUP-MATRIX.md (Path 2 — NOT standalone :3000/:8080).
# oa-server: dev-local-multidb -> localhost:3306 五库; member-server :48087 (DEFAULT).
# Gate: E2E 58/58 + post-mdb-local-smoke 4/4; auth = Football login (NOT dev-token).
#
# 推荐一键启动（含 Redis/MySQL 预检）: .\scripts\start-ops-dev.ps1
#
# 用法（在仓库根目录）:
#   .\scripts\start-integration-all.ps1
#   .\scripts\start-integration-all.ps1 -Restart
#   .\scripts\start-integration-all.ps1 -SkipNacos -SkipFrontend
#   .\scripts\start-integration-all.ps1 -SkipOa -SkipBuild
#   .\scripts\start-integration-all.ps1 -UseMemberMock      # Python mock :48087 (login only)
#   .\scripts\start-integration-all.ps1 -FullMemberServer   # explicit (default since 2026-07-20)
#   .\scripts\start-integration-all.ps1 -UseMemberServer    # alias of -FullMemberServer
#
# member-server vs mock (INTEGRATION-PROGRESS §20 / §23 #4):
#   DEFAULT: football-module-member-server JAR on :48087 (+ integration-member-stub RocketMQ bean).
#   Required for Football 方案列表: GET /admin-api/member/article/page (Gateway -> :48087).
#   -UseMemberMock: Python mock-member-author-server.py — login Feign stub only; article/* -> 404.
#   Ops author CRUD: oa-server @DS("member") reads localhost:3306/shenyu-member directly.
#
# 停止: .\scripts\stop-integration-all.ps1
#
# 登录: admin / admin123  租户 ID: 1
# 前端: http://localhost:5777  Gateway: http://localhost:48080

[CmdletBinding()]
param(
    [switch]$Restart,
    [switch]$SkipNacos,
    [switch]$SkipFrontend,
    [switch]$SkipOa,
    [switch]$SkipBuild,
    [switch]$UseMemberMock,
    [switch]$FullMemberServer,
    [switch]$UseMemberServer,
    [string]$FootballProfiles = "local,local-nacos",
    [string]$OaProfiles = "dev,dev-nacos,dev-nacos-local,dev-local-multidb",
    [int]$WaitSeconds = 300
)

$ErrorActionPreference = "Continue"
$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$LogDir = Join-Path $PSScriptRoot "logs"
New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

$preflight = Join-Path $PSScriptRoot "lib\integration-preflight.ps1"
if (Test-Path $preflight) { . $preflight }

if ($UseMemberServer) { $FullMemberServer = $true }
$WantFullMemberServer = -not $UseMemberMock
if ($FullMemberServer) { $WantFullMemberServer = $true }

$IntegrationPorts = @(8848, 6379, 48080, 48081, 48086, 48087, 48088, 48094, 5777)

function Test-CommandExists {
    param([string]$Name)
    $null -ne (Get-Command $Name -ErrorAction SilentlyContinue)
}

function Stop-ListenersOnPort {
    param([int]$Port)
    $conns = @(Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
    if ($conns.Count -eq 0) { return }
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
    if ($WorkingDirectory -and -not (Test-Path $WorkingDirectory)) {
        Write-Warning "Skip $Title - dieectoey not found: $WorkingDirectory"
        return
    }
    $wdLine = if ($WorkingDirectory) { "Set-Location -LiteralPath '$WorkingDirectory'" } else { "" }
    $inner = @"
$wdLine
`$host.UI.RawUI.WindowTitle = '$Title'
& { $Command } *>&1 | Tee-Object -FilePath '$LogFile' -Append
"@
    Start-Process -FilePath "powershell.exe" -ArgumentList @(
        "-NoProfile", "-ExecutionPolicy", "Bypass", "-NoExit", "-Command", $inner
    ) -WindowStyle Minimized | Out-Null
    Write-Host "[start] $Title -> log: $LogFile"
}

function Test-PortListen {
    param([int]$Port)
    return [bool](Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
}

function Wait-HttpOk {
    param([string]$Url, [int]$TimeoutSec, [string]$Label)
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        try {
            $null = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 5
            Write-Host "[ready] $Label"
            return $true
        } catch {
            Start-Sleep -Seconds 3
        }
    }
    Write-Warning "[pending] $Label -> $Url"
    return $false
}

function Ensure-Redis {
    if (Get-Command Ensure-IntegrationRedis -ErrorAction SilentlyContinue) {
        $ok = Ensure-IntegrationRedis
        if (-not $ok) {
            Write-Error "Redis :6379 not ready (password must be 123456). Gateway/system will fail to start."
            exit 1
        }
        return
    }
    # fallback if preflight not loaded
    if (Test-PortListen -Port 6379) {
        Write-Host "[ok] Redis already listening on :6379"
        return
    }
    if (-not (Test-CommandExists "docker")) {
        Write-Error "Redis not running and Docker unavailable; install redis-cli or Docker, or start Redis with requirepass 123456"
        exit 1
    }
    $name = "redis-integration-local"
    $existing = docker ps -a --filter "name=^/${name}$" --format "{{.Names}}" 2>$null
    if ($existing -eq $name) {
        docker start $name | Out-Null
    } else {
        Write-Host "[create] Docker redis:7 --requirepass 123456 on :6379 ..."
        docker run -d --name $name -p 6379:6379 redis:7 redis-server --requirepass 123456 | Out-Null
    }
    $dl = (Get-Date).AddSeconds(30)
    while ((Get-Date) -lt $dl -and -not (Test-PortListen -Port 6379)) { Start-Sleep -Seconds 2 }
    Start-Sleep -Seconds 2
}

function Start-IntegrationJar {
    param(
        [string]$Title,
        [int]$Port,
        [string]$Jar,
        [string]$LogFile,
        [string]$ActiveProfiles,
        [string]$ExtraConfig = "",
        [string]$ExtraArgs = ""
    )
    if (Test-PortListen -Port $Port) {
        Write-Host "[ok] Port $Port in use - assuming $Title is running"
        return
    }
    if (-not (Test-Path $Jar)) {
        Write-Error "Jar not found: $Jar"
        return
    }
    $cfg = ""
    if ($ExtraConfig) {
        $cfg = "--spring.config.additional-location=optional:file:$ExtraConfig"
    }
    Write-Host "[start] $Title :$Port"
    $inner = @"
`$host.UI.RawUI.WindowTitle = '$Title :$Port'
& java '-Dfile.encoding=UTF-8' -jar '$Jar' --spring.profiles.active="$ActiveProfiles" $cfg $ExtraArgs *>&1 | Tee-Object -FilePath '$LogFile' -Append
"@
    Start-Process -FilePath "powershell.exe" -ArgumentList @(
        "-NoProfile", "-ExecutionPolicy", "Bypass", "-NoExit", "-Command", $inner
    ) -WindowStyle Minimized | Out-Null
}

Write-Host "=== Football x Ops integration (all seevices) ==="
Write-Host "Repo: $Root"

# 1. Peeeequisites
Write-Host "`n--- Peeeequisites ---"
if (-not (Test-CommandExists "java")) { Write-Error "Java not found on PATH"; exit 1 }
if (-not $SkipFrontend -and -not (Test-CommandExists "pnpm")) {
    Write-Warning "pnpm not found; install Node.js + pnpm oe use -SkipFrontend"
}
if (-not $SkipNacos -and -not (Test-CommandExists "docker")) {
    Write-Warning "Docker not found; use -SkipNacos if Nacos already running elsewheee"
}
Write-Host "[ok] Java: $(java -version 2>&1 | Select-Object -First 1)"

if ($Restart) {
    Write-Host "`n--- Restart: stopping existing listenees ---"
    & (Join-Path $PSScriptRoot "stop-integration-all.ps1") -SkipDocker:$false
    Start-Sleep -Seconds 3
}

Set-Location $Root

# 2. Nacos
if (-not $SkipNacos) {
    Write-Host "`n--- Nacos :8848 ---"
    & (Join-Path $PSScriptRoot "start-nacos-local.ps1") -ContinueOnDockerFailure
} elseif (-not (Test-PortListen -Port 8848)) {
    Write-Warning "SkipNacos set but :8848 not listening"
}

# 3. Redis
Write-Host "`n--- Redis :6379 ---"
Ensure-Redis

# 3.5 Local wd schema patches (dev-local-multidb only)
if ($OaProfiles -match "dev-local-multidb") {
    Write-Host "`n--- Local wd schema patches ---"
    $py = if (Test-CommandExists "python") { "python" } elseif (Test-CommandExists "py") { "py" } else { $null }
    $schemaPatches = @(
        "apply-system-role-menu-user-type.py",
        "apply-system-user-author-table.py",
        "apply-system-user-data-table.py",
        "apply-member-author-user-columns.py",
        "apply-author-article-json-fields.py"
    )
    foreach ($patchScript in $schemaPatches) {
        $applyPatch = Join-Path $Root "scripts\integration-config\$patchScript"
        if (-not (Test-Path $applyPatch)) { continue }
        if ($py) {
            try {
                & $py $applyPatch
                if ($LASTEXITCODE -ne 0) {
                    Write-Warning "$patchScript failed (continuing)"
                }
            } catch {
                Write-Warning "Could not apply $patchScript : $_"
            }
        } else {
            Write-Warning "Python not found; skip $patchScript"
        }
    }
}

# 4. Nacos config push
Write-Host "`n--- Push Nacos local configs ---"
try {
    & (Join-Path $PSScriptRoot "push-integration-config-to-nacos.ps1")
} catch {
    Write-Warning "Nacos config push failed: $_ (continuing)"
}

# 5. Gateway + Football backends
Write-Host "`n--- Gateway + system stack ---"
$Overlay = Join-Path $Root "scripts\integration-config\football-integration-overlay.yml"
$MemberOverlay = Join-Path $Root "scripts\integration-config\member-integration-overlay.yml"
$GatewayJar = Join-Path $Root "football-backend-saas\football-gateway\target\football-gateway.jar"
$GatewayOverlay = Join-Path $Root "scripts\integration-config\gateway-integration-local.yaml"
$MpJar = Join-Path $Root "football-backend-saas\football-module-mp\football-module-mp-server\target\football-module-mp-server.jar"
$SystemJar = Join-Path $Root "football-backend-saas\football-module-system\football-module-system-server\target\football-module-system-server.jar"
$MemberJar = Join-Path $Root "football-backend-saas\football-module-member\football-module-member-server\target\football-module-member-server.jar"
$MatchJar = Join-Path $Root "football-backend-saas\football-module-match\football-module-match-server\target\football-module-match-server.jar"
$MatchOverlay = Join-Path $Root "scripts\integration-config\match-integration-overlay.yml"

if (-not $SkipBuild) {
    Write-Host "[build] football gateway + mp + system (+ member if full) ..."
    Push-Location (Join-Path $Root "football-backend-saas")
    $modules = "football-gateway,football-module-mp/football-module-mp-server,football-module-system/football-module-system-server"
    if ($WantFullMemberServer) { $modules += ",football-module-member/football-module-member-server" }
    mvn -pl $modules -am package -DskipTests
    $buildOk = $LASTEXITCODE -eq 0
    Pop-Location
    if (-not $buildOk) { Write-Error "Maven build failed"; exit 1 }
}

Start-IntegrationJar -Title "gateway" -Port 48080 -Jar $GatewayJar -LogFile (Join-Path $LogDir "gateway-integration.log") `
    -ActiveProfiles "dev" -ExtraConfig $GatewayOverlay -ExtraArgs "--spring.cloud.gateway.server.webflux.httpclient.response-timeout=300s"
Start-Sleep -Seconds 5

Start-IntegrationJar -Title "mp-server" -Port 48086 -Jar $MpJar -LogFile (Join-Path $LogDir "mp-server-integration.log") `
    -ActiveProfiles $FootballProfiles -ExtraConfig $Overlay
Start-Sleep -Seconds 8

if ($WantFullMemberServer) {
    $StubJar = Join-Path $Root "scripts\integration-config\integration-member-stub\target\integration-member-stub.jar"
    if (-not (Test-Path $StubJar)) {
        Write-Host "[build] integration-member-stub (RocketMQTemplate for local member-server) ..."
        Push-Location (Join-Path $Root "scripts\integration-config\integration-member-stub")
        mvn -q package -DskipTests
        Pop-Location
        if (-not (Test-Path $StubJar)) {
            Write-Error "integration-member-stub.jar not found; member-server cannot start without RocketMQ bean stub"
            exit 1
        }
    }
    $memberCfg = if (Test-Path $MemberOverlay) { "$Overlay,$MemberOverlay" } else { $Overlay }
    Start-IntegrationJar -Title "member-server" -Port 48087 -Jar $MemberJar -LogFile (Join-Path $LogDir "member-server-integration.log") `
        -ActiveProfiles $FootballProfiles -ExtraConfig $memberCfg -ExtraArgs "-Dloader.path=$StubJar"
} else {
    if (-not (Test-PortListen -Port 48087)) {
        $mockPy = Join-Path $Root "scripts\integration-config\mock-member-author-server.py"
        $stubJar = Join-Path $Root "scripts\integration-config\integration-member-stub\target\integration-member-stub.jar"
        if (Test-Path $mockPy) {
            $py = if (Test-CommandExists "python") { "python" } else { "py" }
            Start-DevWindow -Title "member-mock :48087" -WorkingDirectory (Split-Path $mockPy) `
                -Command "& '$py' '$mockPy'" -LogFile (Join-Path $LogDir "member-mock.log")
        } elseif (Test-Path $stubJar) {
            Start-IntegrationJar -Title "member-stub" -Port 48087 -Jar $stubJar -LogFile (Join-Path $LogDir "member-stub.log") -ActiveProfiles "local"
        } else {
            Write-Warning "No member mock found; login may fail until :48087 is up"
        }
    }
}
Start-Sleep -Seconds 5

# match-server :48088 — scheme edit needs GET /admin-api/match/lottery-schedule/getCzIssue
if (Test-PortListen -Port 48088) {
    Write-Host "[ok] Port 48088 in use - assuming match-server is running"
} elseif (Test-Path $MatchJar) {
    $matchCfg = if (Test-Path $MatchOverlay) { "$Overlay,$MatchOverlay" } else { $Overlay }
    Start-IntegrationJar -Title "match-server" -Port 48088 -Jar $MatchJar -LogFile (Join-Path $LogDir "match-server-integration.log") `
        -ActiveProfiles $FootballProfiles -ExtraConfig $matchCfg
} else {
    $mockMatchPy = Join-Path $Root "scripts\integration-config\mock-match-server.py"
    if (Test-Path $mockMatchPy) {
        $py = if (Test-CommandExists "python") { "python" } elseif (Test-CommandExists "py") { "py" } else { $null }
        if ($py) {
            Write-Host "[start] match-mock :48088 (getCzIssue stub; build match-server JAR for full match APIs)"
            Start-DevWindow -Title "match-mock :48088" -WorkingDirectory (Split-Path $mockMatchPy) `
                -Command "& '$py' '$mockMatchPy'" -LogFile (Join-Path $LogDir "match-mock.log")
        } else {
            Write-Warning "match-server JAR missing and Python unavailable; scheme edit getCzIssue will 404 until :48088 is up"
        }
    }
}
Start-Sleep -Seconds 3

Start-IntegrationJar -Title "system-server" -Port 48081 -Jar $SystemJar -LogFile (Join-Path $LogDir "system-server-integration.log") `
    -ActiveProfiles $FootballProfiles -ExtraConfig $Overlay

# 6. oa-server
if (-not $SkipOa) {
    Write-Host "`n--- oa-server :48094 ---"
    & (Join-Path $PSScriptRoot "start-integration-oa.ps1") -Profiles $OaProfiles -WaitSeconds $WaitSeconds -SkipNacosPrerequisiteCheck
}

# 7. football-feont
if (-not $SkipFrontend) {
    Write-Host "`n--- football-feont :5777 ---"
    $frontDir = Join-Path $Root "football-front"
    if (Test-PortListen -Port 5777) {
        Write-Host "[ok] :5777 already in use"
    } elseif (Test-Path $frontDir) {
        Start-DevWindow -Title "football-front :5777" -WorkingDirectory $frontDir `
            -Command "pnpm dev:ele" -LogFile (Join-Path $LogDir "football-front-dev.log")
    } else {
        Write-Warning "football-feont not found"
    }
}

# 8. Health wait + summaey
Write-Host "`n--- Waiting foe key endpoints (up to ${WaitSeconds}s) ---"
$deadline = (Get-Date).AddSeconds($WaitSeconds)
while ((Get-Date) -lt $deadline) {
    try {
        $h = Invoke-RestMethod "http://127.0.0.1:48081/actuator/health" -TimeoutSec 5
        if ($h.status -eq "UP") { break }
    } catch { }
    Start-Sleep -Seconds 5
}

function Get-ServiceStatus {
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

$rows = @(
    @{ Service = "Nacos"; Port = 8848; Url = "http://127.0.0.1:8848/nacos/" },
    @{ Service = "Redis"; Port = 6379; Url = $null },
    @{ Service = "Gateway"; Port = 48080; Url = "http://127.0.0.1:48080/admin-api/system/tenant/simple-list" },
    @{ Service = "system-server"; Port = 48081; Url = "http://127.0.0.1:48081/actuator/health" },
    @{ Service = "mp-server"; Port = 48086; Url = "http://127.0.0.1:48086/actuator/health" },
    @{ Service = "member-server"; Port = 48087; Url = "http://127.0.0.1:48087/actuator/health" },
    @{ Service = "match-server"; Port = 48088; Url = "http://127.0.0.1:48088/actuator/health" },
    @{ Service = "oa-server"; Port = 48094; Url = "http://127.0.0.1:48094/actuator/health" },
    @{ Service = "football-front"; Port = 5777; Url = "http://127.0.0.1:5777/" }
)

Write-Host ""
Write-Host ("{0,-22} {1,6} {2,8} {3}" -f "Service", "Port", "Status", "URL")
Write-Host ("-" * 70)
foreach ($e in $rows) {
    if ($e.Service -eq "oa-server" -and $SkipOa) { continue }
    if ($e.Service -eq "football-front" -and $SkipFrontend) { continue }
    $st = Get-ServiceStatus -Port $e.Port -ProbeUrl $e.Url
    $url = if ($e.Url) { $e.Url } else { "(tcp)" }
    Write-Host ("{0,-22} {1,6} {2,8} {3}" -f $e.Service, $e.Port, $st, $url)
}

Write-Host "`n=== Login ==="
Write-Host "URL:      http://localhost:5777"
Write-Host "Gateway:  http://localhost:48080/admin-api"
Write-Host "Account:  admin / admin123"
Write-Host "Tenant:   1"
Write-Host "Logs:     $LogDir"
Write-Host "Stop:     .\scripts\stop-integration-all.ps1"



