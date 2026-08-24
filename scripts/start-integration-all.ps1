# start-integration-all.ps1 - һ������ Football x Ops ���ؼ���ȫջ (Gate ·��)
#
# SSOT matrix: docs/delivery/OPS-STARTUP-MATRIX.md (Path 2 �� NOT standalone :3000/:8080).
# oa-server: DEFAULT OaProfiles = dev-local-multidb -> localhost:3306 ��� (NOT beta remote).
# member-server :48087 (DEFAULT). Gate UI :5777 via pnpm dev:ele; vite proxy -> localhost:48080.
# Preflight: ops branch warn + views/ops mount + Ensure-FootballFrontLocalApi.
#
# �Ƽ�һ���������� Redis/MySQL Ԥ�죩: .\scripts\start-ops-dev.ps1
#
# �÷����ڲֿ��Ŀ¼��:
#   .\scripts\start-integration-all.ps1
#   .\scripts\start-integration-all.ps1 -Restart
#   .\scripts\start-integration-all.ps1 -SkipNacos -SkipFrontend
#   .\scripts\start-integration-all.ps1 -SkipOa -SkipBuild
#   .\scripts\start-integration-all.ps1 -UseMemberMock      # Python mock :48087 (login only)
#   .\scripts\start-integration-all.ps1 -FullMemberServer   # explicit (default since 2026-07-20)
#   .\scripts\start-integration-all.ps1 -UseMemberServer    # alias of -FullMemberServer
#   .\scripts\start-integration-all.ps1 -MountOps           # RETIRED �� fail-fast (SSOT=football-front)
#   .\scripts\start-integration-all.ps1 -SkipMountOps
#   .\scripts\start-integration-all.ps1 -Beta               # remote test DB; Nacos still local :8848
#   .\scripts\start-integration-all.ps1 -TestRemote         # alias of -Beta
#
# member-server vs mock (INTEGRATION-PROGRESS ��20 / ��23 #4):
#   DEFAULT: football-module-member-server JAR on :48087 (+ integration-member-stub RocketMQ bean).
#   Required for Football �����б�: GET /admin-api/member/article/page (Gateway -> :48087).
#   -UseMemberMock: Python mock-member-author-server.py �� login Feign stub only; article/* -> 404.
#   Ops author CRUD: oa-server @DS("member") reads localhost:3306/shenyu-member directly.
#   -Beta: same local ports + local Nacos :8848 ns=local; MySQL/Redis -> 110.42.49.224 (ops-test-remote.env).
#
# ֹͣ: .\scripts\stop-integration-all.ps1
#
# ��¼: admin / admin123  �⻧ ID: 1
# ǰ��: http://localhost:5777  Gateway: http://localhost:48080

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
    [switch]$MountOps,
    [switch]$SkipMountOps,
    [Alias("TestRemote")]
    [switch]$Beta,
    [string]$FootballProfiles = "local,local-nacos",
    [string]$OaProfiles = "dev,dev-nacos,dev-nacos-local,dev-local-multidb",
    [int]$WaitSeconds = 180
)

$ErrorActionPreference = "Continue"
$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
if ($MountOps) {
    Write-Host "[retired] -MountOps: remount from ops-platform-ui-vue ended (A-WP1)." -ForegroundColor Red
    Write-Host "          OPS UI SSOT = football-front/apps/web-ele/src/views/ops �� edit there directly." -ForegroundColor Yellow
    exit 1
}
$LogDir = Join-Path $PSScriptRoot "logs"
New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

$preflight = Join-Path $PSScriptRoot "lib\integration-preflight.ps1"
if (Test-Path $preflight) { . $preflight }

if ($UseMemberServer) { $FullMemberServer = $true }
$WantFullMemberServer = -not $UseMemberMock
if ($FullMemberServer) { $WantFullMemberServer = $true }

if ($Beta) {
    if (-not (Get-Command Import-OpsTestRemoteEnv -ErrorAction SilentlyContinue)) {
        Write-Error "Import-OpsTestRemoteEnv missing �� ensure scripts/lib/integration-preflight.ps1 is loaded"
        exit 1
    }
    if (-not (Import-OpsTestRemoteEnv -Root $Root -Required)) { exit 1 }
    if ($OaProfiles -notmatch "dev-test-beta") {
        $OaProfiles = "$OaProfiles,dev-test-beta"
    }
    Write-Host "[beta] Mode ON - OaProfiles=$OaProfiles ; Football overlays=*-beta.yml ; Nacos=local :8848 ns=local ; DB/Redis=remote"
}

$IntegrationPorts = @(8848, 6379, 48080, 48081, 48082, 48085, 48086, 48087, 48088, 48094, 5777)

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
        Write-Warning "Skip $Title - directory not found: $WorkingDirectory"
        return
    }
    # IMPORTANT: do not pass Chinese paths via powershell -Command (console/ANSI mangles them).
    # Write a UTF-8 BOM launcher and start with -File so paths like .../��Ӫ����ƽ̨/... survive.
    $tempDir = Join-Path $env:TEMP "ops-dev-start"
    New-Item -ItemType Directory -Force -Path $tempDir | Out-Null
    $safeTitle = ($Title -replace '[^\w\-]+', '_').Trim('_')
    if (-not $safeTitle) { $safeTitle = "dev" }
    $launcher = Join-Path $tempDir "$safeTitle-$PID-$(Get-Random).ps1"
    $wdLiteral = if ($WorkingDirectory) { $WorkingDirectory.Replace("'", "''") } else { "" }
    $logLiteral = $LogFile.Replace("'", "''")
    $titleLiteral = $Title.Replace("'", "''")
    $lines = @()
    $lines += '$ErrorActionPreference = ''Continue'''
    $lines += "`$host.UI.RawUI.WindowTitle = '$titleLiteral'"
    if ($wdLiteral) { $lines += "Set-Location -LiteralPath '$wdLiteral'" }
    $lines += "& { $Command } *>&1 | Tee-Object -FilePath '$logLiteral' -Append"
    $utf8Bom = New-Object System.Text.UTF8Encoding $true
    [System.IO.File]::WriteAllLines($launcher, $lines, $utf8Bom)
    Start-Process -FilePath "powershell.exe" -ArgumentList @(
        "-NoProfile", "-ExecutionPolicy", "Bypass", "-NoExit", "-File", $launcher
    ) -WindowStyle Minimized | Out-Null
    Write-Host "[start] $Title -> log: $LogFile"
}

function Test-PortListen {
    param([int]$Port)
    return [bool](Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
}

function Wait-HttpOk {
    param([string]$Url, [int]$TimeoutSec, [string]$Label)
    if (Get-Command Wait-HttpEndpoint -ErrorAction SilentlyContinue) {
        return Wait-HttpEndpoint -Url $Url -TimeoutSec $TimeoutSec -Label $Label
    }
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


function Stop-MemberMockIfBlocking {
    param([int]$Port = 48087)
    if (-not (Test-PortListen -Port $Port)) { return }
    $listen = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $listen) { return }
    $procId = $listen.OwningProcess
    $cmd = (Get-CimInstance Win32_Process -Filter "ProcessId=$procId" -ErrorAction SilentlyContinue).CommandLine
    if ($cmd -match 'mock-member-author-server\.py') {
        Write-Host "[fix] Stopping Python member mock on :$Port (FullMemberServer needs JAR)" -ForegroundColor Yellow
        Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
        Start-Sleep -Seconds 2
    }
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
        # Support "path1,path2" -> optional:file:path1,optional:file:path2
        $locs = @($ExtraConfig -split "," | ForEach-Object { $_.Trim() } | Where-Object { $_ })
        $cfg = "--spring.config.additional-location=" + (($locs | ForEach-Object { "optional:file:$_" }) -join ",")
    }
    Write-Host "[start] $Title :$Port"
    if ($Beta -and $ExtraConfig) {
        Write-Host "        config: $ExtraConfig" -ForegroundColor DarkGray
    }
    $inner = @"
`$host.UI.RawUI.WindowTitle = '$Title :$Port'
& java '-Dfile.encoding=UTF-8' -jar '$Jar' --spring.profiles.active="$ActiveProfiles" $cfg $ExtraArgs *>&1 | Tee-Object -FilePath '$LogFile' -Append
"@
    # Inherit parent env (OPS_TEST_* when -Beta) so overlay placeholders resolve
    Start-Process -FilePath "powershell.exe" -ArgumentList @(
        "-NoProfile", "-ExecutionPolicy", "Bypass", "-NoExit", "-Command", $inner
    ) -WindowStyle Minimized | Out-Null
}

Write-Host "=== Football x Ops integration (all seevices) ==="
Write-Host "Repo: $Root"
if ($Beta) {
    Write-Host "DB mode: BETA remote $($env:OPS_TEST_DB_HOST) (MySQL/Redis remote; Nacos local :8848 ns=local)" -ForegroundColor Yellow
} else {
    Write-Host "DB mode: LOCAL localhost multidb (default)"
}

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
    # -SkipNacos: keep user-started Nacos on :8848 (do not docker stop / kill port)
    $preserveNacos = $SkipNacos
    & (Join-Path $PSScriptRoot "stop-integration-all.ps1") -SkipDocker:$preserveNacos
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
if ($Beta) {
    Write-Host "`n--- Redis beta $($env:OPS_TEST_REDIS_HOST):$($env:OPS_TEST_REDIS_PORT) db=$($env:OPS_TEST_REDIS_DATABASE) ---"
    Write-Host "[ok] Skip local Redis ensure (services use remote Redis from ops-test-remote.env)"
} else {
    Write-Host "`n--- Redis :6379 ---"
    Ensure-Redis
}

# 3.5 Local wd schema patches (localhost only �� never against beta remote)
if (-not $Beta -and $OaProfiles -match "dev-local-multidb") {
    Write-Host "`n--- Local wd schema patches ---"
    $py = if (Test-CommandExists "python") { "python" } elseif (Test-CommandExists "py") { "py" } else { $null }
    $schemaPatches = @(
        "apply-system-role-menu-user-type.py",
        "apply-system-user-author-table.py",
        "apply-system-user-data-table.py",
        "apply-member-author-user-columns.py",
        "apply-author-article-json-fields.py",
        # shenyu-system: restore menu names corrupted to '?' (0x3F) by non-utf8 import
        "apply-patch-system-menu-names-utf8.py"
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

# 4. Nacos config push (local Docker Nacos — local and beta both use namespace local)
Write-Host "`n--- Push Nacos local configs ---"
try {
    & (Join-Path $PSScriptRoot "push-integration-config-to-nacos.ps1")
} catch {
    Write-Warning "Nacos config push failed: $_ (continuing)"
}

# 5. Gateway + Football backends
Write-Host "`n--- Gateway + system stack ---"
if ($Beta) {
    $Overlay = Join-Path $Root "scripts\integration-config\football-integration-overlay-beta.yml"
    $MemberOverlay = Join-Path $Root "scripts\integration-config\member-integration-overlay-beta.yml"
    $MpOverlay = Join-Path $Root "scripts\integration-config\mp-integration-overlay-beta.yml"
    $GatewayOverlay = Join-Path $Root "scripts\integration-config\gateway-integration-beta.yaml"
} else {
    $Overlay = Join-Path $Root "scripts\integration-config\football-integration-overlay.yml"
    $MemberOverlay = Join-Path $Root "scripts\integration-config\member-integration-overlay.yml"
    $MpOverlay = Join-Path $Root "scripts\integration-config\mp-integration-overlay.yml"
    $GatewayOverlay = Join-Path $Root "scripts\integration-config\gateway-integration-local.yaml"
}
$GatewayJar = Join-Path $Root "football-backend-saas\football-gateway\target\football-gateway.jar"
$MpJar = Join-Path $Root "football-backend-saas\football-module-mp\football-module-mp-server\target\football-module-mp-server.jar"
$SystemJar = Join-Path $Root "football-backend-saas\football-module-system\football-module-system-server\target\football-module-system-server.jar"
$InfraJar = Join-Path $Root "football-backend-saas\football-module-infra\football-module-infra-server\target\football-module-infra-server.jar"
$MemberJar = Join-Path $Root "football-backend-saas\football-module-member\football-module-member-server\target\football-module-member-server.jar"
$MatchJar = Join-Path $Root "football-backend-saas\football-module-match\football-module-match-server\target\football-module-match-server.jar"
$PayJar = Join-Path $Root "football-backend-saas\football-module-pay\football-module-pay-server\target\football-module-pay-server.jar"
$MatchOverlay = if ($Beta) {
    Join-Path $Root "scripts\integration-config\match-integration-overlay-beta.yml"
} else {
    Join-Path $Root "scripts\integration-config\match-integration-overlay.yml"
}
$PayOverlay = if ($Beta) {
    Join-Path $Root "scripts\integration-config\pay-integration-overlay-beta.yml"
} else {
    Join-Path $Root "scripts\integration-config\pay-integration-overlay.yml"
}

if (-not $SkipBuild) {
    Write-Host "[build] football gateway + mp + system + infra (+ member if full) ..."
    Write-Host "        Stopping listeners on integration ports so Maven can repackage JARs (pay-server :48085 included)"
    foreach ($p in @(48080, 48081, 48082, 48085, 48086, 48087, 48088, 48094)) {
        Stop-ListenersOnPort -Port $p
    }
    Start-Sleep -Seconds 2
    Push-Location (Join-Path $Root "football-backend-saas")
    $modules = "football-gateway,football-module-mp/football-module-mp-server,football-module-system/football-module-system-server,football-module-infra/football-module-infra-server,football-module-pay/football-module-pay-server,football-module-ops/football-module-ops-server"
    if ($WantFullMemberServer) { $modules += ",football-module-member/football-module-member-server" }
    mvn -pl $modules -am package -DskipTests
    $buildOk = $LASTEXITCODE -eq 0
    Pop-Location
    if (-not $buildOk) { Write-Error "Maven build failed"; exit 1 }
}

Start-IntegrationJar -Title "gateway" -Port 48080 -Jar $GatewayJar -LogFile (Join-Path $LogDir "gateway-integration.log") `
    -ActiveProfiles "dev" -ExtraConfig $GatewayOverlay -ExtraArgs "--spring.cloud.gateway.server.webflux.httpclient.response-timeout=300s"
Start-Sleep -Seconds 5

$mpCfg = if ($MpOverlay -and (Test-Path $MpOverlay)) { "$Overlay,$MpOverlay" } else { $Overlay }
Start-IntegrationJar -Title "mp-server" -Port 48086 -Jar $MpJar -LogFile (Join-Path $LogDir "mp-server-integration.log") `
    -ActiveProfiles $FootballProfiles -ExtraConfig $mpCfg
Start-Sleep -Seconds 8

if ($WantFullMemberServer) {
    Stop-MemberMockIfBlocking -Port 48087
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

# match-server :48088 �� scheme edit needs GET /admin-api/match/lottery-schedule/getCzIssue
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

# 5.4 pay-server :48085 �� G-PAY-01 order page RPC
if (Test-PortListen -Port 48085) {
    Write-Host "[ok] Port 48085 in use - assuming pay-server is running"
} elseif (Test-Path $PayJar) {
    $payCfg = if (Test-Path $PayOverlay) { "$Overlay,$PayOverlay" } else { $Overlay }
    Start-IntegrationJar -Title "pay-server" -Port 48085 -Jar $PayJar -LogFile (Join-Path $LogDir "pay-server-integration.log") `
        -ActiveProfiles $FootballProfiles -ExtraConfig $payCfg
} else {
    Write-Warning "pay-server JAR missing (:48085). G-PAY-01 order list will fail until built."
    Write-Warning "  Fix: mvn -pl football-module-pay/football-module-pay-server -am package -DskipTests"
}

# 5.5 infra-server :48082 �� Phase A file upload (/admin-api/infra/file/*)
$InfraOverlay = Join-Path $Root "scripts\integration-config\infra-integration-overlay.yml"
$SftpKey = Join-Path $Root "scripts\integration-config\local-sftp-id_rsa"
if (Get-Command Ensure-LocalSftpKey -ErrorAction SilentlyContinue) {
    $null = Ensure-LocalSftpKey -Root $Root
}
if (Test-PortListen -Port 48082) {
    Write-Host "[ok] Port 48082 in use - assuming infra-server is running"
} elseif (Test-Path $InfraJar) {
    $infraCfg = if (Test-Path $InfraOverlay) { "$Overlay,$InfraOverlay" } else { $Overlay }
    # Absolute key path required: jar cwd is not repo root; application-local defaults to D:/zhengshu/...
    $sftpArg = if (Test-Path $SftpKey) { "--sftp.private-key=$SftpKey" } else { "" }
    if (-not (Test-Path $SftpKey)) {
        Write-Warning "Missing $SftpKey �� generate with: ssh-keygen -t rsa -f scripts/integration-config/local-sftp-id_rsa -N \"\""
    }
    Start-IntegrationJar -Title "infra-server" -Port 48082 -Jar $InfraJar -LogFile (Join-Path $LogDir "infra-server-integration.log") `
        -ActiveProfiles $FootballProfiles -ExtraConfig $infraCfg -ExtraArgs $sftpArg
} else {
    Write-Warning "infra-server JAR missing (:48082). Phase A file upload /admin-api/infra/file/* will 404 until built."
    Write-Warning "  Fix: .\scripts\start-ops-dev.ps1 -FirstRun   OR   mvn -pl football-module-infra/football-module-infra-server -am package -DskipTests"
}

# 6. football-module-ops (script filename kept: start-integration-oa.ps1; Nacos id: ops-server)
if (-not $SkipOa) {
    if (Get-Command Ensure-OpsFlywayPreflight -ErrorAction SilentlyContinue) {
        Ensure-OpsFlywayPreflight -Root $Root -Beta:$Beta
    }
    Write-Host "`n--- football-module-ops :48094 ---"
    & (Join-Path $PSScriptRoot "start-integration-oa.ps1") -Profiles $OaProfiles -WaitSeconds $WaitSeconds -SkipNacosPrerequisiteCheck
    if ($LASTEXITCODE -ne 0) {
        Write-Warning "football-module-ops start script exited $LASTEXITCODE (see ops-server-nacos-run.log)"
    }
}

# 7. football-front (Gate :5777 �� standalone :3000 not used)
if (-not $SkipFrontend) {
    Write-Host "`n--- football-front :5777 (pnpm dev:ele) ---"
    if (Get-Command Assert-FootballOpsBranch -ErrorAction SilentlyContinue) {
        $null = Assert-FootballOpsBranch -Root $Root
    }
    if (Get-Command Ensure-OpsViewsMounted -ErrorAction SilentlyContinue) {
        $mountArgs = @{ Root = $Root }
        if ($MountOps) { $mountArgs.ForceMount = $true }
        if ($SkipMountOps) { $mountArgs.SkipMount = $true }
        $null = Ensure-OpsViewsMounted @mountArgs
    }
    if (Get-Command Ensure-OpsFrontDeps -ErrorAction SilentlyContinue) {
        $null = Ensure-OpsFrontDeps -Root $Root -AutoLink
    }
    if (Get-Command Ensure-FootballFrontLocalApi -ErrorAction SilentlyContinue) {
        $apiOk = Ensure-FootballFrontLocalApi -Root $Root
        if (-not $apiOk) {
            Write-Warning "Vite proxy is not localhost:48080 �� local OPS menus may be missing after login"
        }
    }
    $frontDir = Join-Path $Root "football-front"
    if (Test-PortListen -Port 5777) {
        Write-Host "[ok] :5777 already in use"
        Write-Host "     If vite proxy / VITE_BASE_URL just changed, restart :5777 (Vite reads env only at boot)" -ForegroundColor Yellow
    } elseif (Test-Path $frontDir) {
        $frontScript = Join-Path $PSScriptRoot "run-football-front-dev.ps1"
        $frontLog = Join-Path $LogDir "football-front-dev.log"
        $frontErr = Join-Path $LogDir "football-front-dev.err.log"
        # Must use -File/-Root (Unicode-safe). Embedding ��Ӫ����ƽ̨ in -Command mangles the path;
        # Vite then prints "ready" against a broken cwd and never stays on :5777.
        Start-Process -FilePath "powershell.exe" -ArgumentList @(
            "-NoProfile", "-ExecutionPolicy", "Bypass",
            "-File", $frontScript, "-Root", $Root
        ) -WorkingDirectory $frontDir -WindowStyle Minimized `
            -RedirectStandardOutput $frontLog -RedirectStandardError $frontErr | Out-Null
        Write-Host "[start] football-front :5777 -> log: $frontLog"
    } else {
        Write-Warning "football-front not found"
    }
}

# 8. Health wait + summary
Write-Host "`n--- Waiting for key endpoints (up to ${WaitSeconds}s) ---"
# system actuator is wrapped by yudao (HTTP 200 + code:404) �� any HTTP response means process is up
$null = Wait-HttpEndpoint -Url "http://127.0.0.1:48081/actuator/health" -TimeoutSec $WaitSeconds -Label "system-server"
$null = Wait-HttpEndpoint -Url "http://127.0.0.1:48080/admin-api/system/tenant/simple-list" -TimeoutSec ([Math]::Min(60, $WaitSeconds)) -Label "gateway"
if (-not $SkipOa) {
    $null = Wait-HttpEndpoint -Url "http://127.0.0.1:48094/actuator/health" -FallbackUrl "http://127.0.0.1:48094/v3/api-docs" -Port 48094 -TimeoutSec ([Math]::Min(60, $WaitSeconds)) -Label "football-module-ops"
}
if (-not $SkipFrontend) {
    $frontReady = Wait-HttpEndpoint -Url "http://127.0.0.1:5777/" -TimeoutSec ([Math]::Max(90, [Math]::Min(120, $WaitSeconds))) -Label "football-front" -PollSec 2
    if (-not $frontReady) {
        Write-Host "[retry] football-front DOWN after wait �� restart once" -ForegroundColor Yellow
        if (Get-Command Stop-PortListeners -ErrorAction SilentlyContinue) {
            Stop-PortListeners -Port 5777
        } else {
            Stop-ListenersOnPort -Port 5777
        }
        Start-Sleep -Seconds 2
        $frontDir = Join-Path $Root "football-front"
        $frontScript = Join-Path $PSScriptRoot "run-football-front-dev.ps1"
        $frontLog = Join-Path $LogDir "football-front-dev.log"
        $frontErr = Join-Path $LogDir "football-front-dev.err.log"
        if (Test-Path $frontDir) {
            Start-Process -FilePath "powershell.exe" -ArgumentList @(
                "-NoProfile", "-ExecutionPolicy", "Bypass",
                "-File", $frontScript, "-Root", $Root
            ) -WorkingDirectory $frontDir -WindowStyle Minimized `
                -RedirectStandardOutput $frontLog -RedirectStandardError $frontErr | Out-Null
            Write-Host "[start] football-front :5777 (retry) -> log: $frontLog"
            $null = Wait-HttpEndpoint -Url "http://127.0.0.1:5777/" -TimeoutSec 120 -Label "football-front" -PollSec 2
        }
    }
}

function Get-ServiceStatus {
    param([int]$Port, [string]$ProbeUrl, [hashtable]$Headers = $null, [string]$FallbackUrl = "")
    return Get-ServiceListenStatus -Port $Port -ProbeUrl $ProbeUrl -Headers $Headers -FallbackUrl $FallbackUrl
}

$rows = Get-IntegrationHealthRows

Write-Host ""
Write-Host ("{0,-22} {1,6} {2,8} {3}" -f "Service", "Port", "Status", "URL")
Write-Host ("-" * 70)
foreach ($e in $rows) {
    if ($e.Service -eq "football-module-ops" -and $SkipOa) { continue }
    if ($e.Service -eq "football-front" -and $SkipFrontend) { continue }
    $st = Get-ServiceStatus -Port $e.Port -ProbeUrl $e.Url -Headers $e.Headers -FallbackUrl $e.FallbackUrl
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



