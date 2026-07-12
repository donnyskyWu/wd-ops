# start-integration-system.ps1 锟?Start Football member-server + system-server for local Gateway integration
#
# Gateway (football-gateway.jar, profile local) routes /admin-api/system/** 锟?system-server via Nacos namespace "local".
# system-server requires member-server (AuthorApi Feign). If member jar fails (RocketMQ/Redis/im), run scripts/integration-config/mock-member-author-server.py on :48087 for login smoke.
#
# Usage (from repo root):
#   .\scripts\start-integration-system.ps1
#   .\scripts\start-integration-system.ps1 -SkipBuild

[CmdletBinding()]
param(
    [string]$Profiles = "local,local-nacos",
    [int]$WaitSeconds = 240,
    [switch]$SkipBuild
)

$ErrorActionPreference = "Continue"
$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$SystemDir = Join-Path $Root "football-backend-saas\football-module-system\football-module-system-server"
$MemberDir = Join-Path $Root "football-backend-saas\football-module-member\football-module-member-server"
$MpDir = Join-Path $Root "football-backend-saas\football-module-mp\football-module-mp-server"
$LogDir = Join-Path $PSScriptRoot "logs"
New-Item -ItemType Directory -Force -Path $LogDir | Out-Null
$SystemLog = Join-Path $LogDir "system-server-integration.log"
$MemberLog = Join-Path $LogDir "member-server-integration.log"
$MpLog = Join-Path $LogDir "mp-server-integration.log"

function Start-IntegrationJar {
    param(
        [string]$Title,
        [int]$Port,
        [string]$Jar,
        [string]$LogFile,
        [string]$ActiveProfiles
    )
    $busy = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    if ($busy) {
        Write-Host "[ok] Port $Port already in use 锟?assuming $Title is running"
        return
    }
    $overlay = Join-Path $Root 'scripts\integration-config\football-integration-overlay.yml'
    Write-Host "[start] $Title :$Port -> log: $LogFile"
    $inner = @"
`$host.UI.RawUI.WindowTitle = '$Title :$Port'
& java -jar '$Jar' --spring.profiles.active="$ActiveProfiles" --spring.config.additional-location=optional:file:$overlay *>&1 | Tee-Object -FilePath '$LogFile' -Append
"@
    Start-Process -FilePath "powershell.exe" -ArgumentList @(
        "-NoProfile", "-ExecutionPolicy", "Bypass", "-NoExit", "-Command", $inner
    ) -WindowStyle Minimized | Out-Null
}

if (-not (Test-Path $SystemDir)) {
    Write-Error "system-server module not found: $SystemDir"
    exit 1
}

Write-Host "=== Start mp-server + member-server + system-server (profile: $Profiles) ==="

if (-not $SkipBuild) {
    Write-Host "[build] mvn mp/member/system servers package -DskipTests ..."
    Push-Location (Join-Path $Root "football-backend-saas")
    mvn -pl football-module-mp/football-module-mp-server,football-module-member/football-module-member-server,football-module-system/football-module-system-server -am package -DskipTests
    $buildOk = $LASTEXITCODE -eq 0
    Pop-Location
    if (-not $buildOk) {
        Write-Error "Maven build failed"
        exit 1
    }
}

$memberJar = Join-Path $MemberDir "target\football-module-member-server.jar"
$mpJar = Join-Path $MpDir "target\football-module-mp-server.jar"
$systemJar = Join-Path $SystemDir "target\football-module-system-server.jar"
if (-not (Test-Path $mpJar)) { Write-Error "Jar not found: $mpJar"; exit 1 }
if (-not (Test-Path $memberJar)) { Write-Error "Jar not found: $memberJar"; exit 1 }
if (-not (Test-Path $systemJar)) { Write-Error "Jar not found: $systemJar"; exit 1 }

Start-IntegrationJar -Title "mp-server" -Port 48086 -Jar $mpJar -LogFile $MpLog -ActiveProfiles $Profiles
Start-Sleep -Seconds 8
# member-server: local-nacos clears jar Redis password; explicit password for requirepass=123456
Start-IntegrationJar -Title "member-server" -Port 48087 -Jar $memberJar -LogFile $MemberLog -ActiveProfiles $Profiles
Start-Sleep -Seconds 8
Start-IntegrationJar -Title "system-server" -Port 48081 -Jar $systemJar -LogFile $SystemLog -ActiveProfiles $Profiles

Write-Host "Waiting up to ${WaitSeconds}s for system-server + gateway route ..."
$deadline = (Get-Date).AddSeconds($WaitSeconds)
$ready = $false
while ((Get-Date) -lt $deadline -and -not $ready) {
    try {
        $health = Invoke-RestMethod -Uri "http://127.0.0.1:48081/actuator/health" -TimeoutSec 5
        if ($health.status -eq "UP") {
            $tenant = Invoke-RestMethod -Uri "http://localhost:48080/admin-api/system/tenant/simple-list" -TimeoutSec 5
            if ($tenant.code -eq 0 -or $tenant.code -eq 401) {
                Write-Host "[ready] system-server UP; gateway tenant API code=$($tenant.code)"
                $ready = $true
            }
        }
    } catch {
        Start-Sleep -Seconds 5
    }
}

if (-not $ready) {
    Write-Warning "system-server not confirmed ready; check $SystemLog / $MemberLog / $MpLog and Nacos namespace 'local'"
} else {
    Write-Host "=== system-server integration ready ==="
}
