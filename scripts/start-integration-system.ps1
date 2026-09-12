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
        [string]$ActiveProfiles,
        [string]$ExtraConfig = ""
    )
    $busy = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    if ($busy) {
        Write-Host "[ok] Port $Port already in use — assuming $Title is running"
        return
    }
    $cfg = ""
    if ($ExtraConfig) {
        $locs = @($ExtraConfig -split "," | ForEach-Object { $_.Trim() } | Where-Object { $_ })
        $cfg = "--spring.config.additional-location=" + (($locs | ForEach-Object { "optional:file:$_" }) -join ",")
    } else {
        $overlay = Join-Path $Root 'scripts\integration-config\football-integration-overlay.yml'
        $cfg = "--spring.config.additional-location=optional:file:$overlay"
    }
    $javaExe = (Get-Command java -ErrorAction SilentlyContinue).Source
    if (-not $javaExe) { $javaExe = "java" }
    Write-Host "[start] $Title :$Port -> log: $LogFile"
    $tempDir = Join-Path $env:TEMP "ops-dev-start"
    New-Item -ItemType Directory -Force -Path $tempDir | Out-Null
    $safeTitle = ($Title -replace '[^\w\-]+', '_').Trim('_')
    if (-not $safeTitle) { $safeTitle = "dev" }
    $launcher = Join-Path $tempDir "$safeTitle-$PID-$(Get-Random).ps1"
    $jarLiteral = $Jar.Replace("'", "''")
    $logLiteral = $LogFile.Replace("'", "''")
    $titleLiteral = "$Title :$Port".Replace("'", "''")
    $javaCmd = "& '$javaExe' '-Dfile.encoding=UTF-8' -jar '$jarLiteral' --spring.profiles.active=`"$ActiveProfiles`" $cfg *>&1 | Tee-Object -FilePath '$logLiteral' -Append"
    $lines = @(
        '$ErrorActionPreference = ''Continue'''
        "`$host.UI.RawUI.WindowTitle = '$titleLiteral'"
        $javaCmd
    )
    $utf8Bom = New-Object System.Text.UTF8Encoding $true
    [System.IO.File]::WriteAllLines($launcher, $lines, $utf8Bom)
    Start-Process -FilePath "powershell.exe" -ArgumentList @(
        "-NoProfile", "-ExecutionPolicy", "Bypass", "-NoExit", "-File", $launcher
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

$overlay = Join-Path $Root 'scripts\integration-config\football-integration-overlay.yml'
$mpOverlay = Join-Path $Root 'scripts\integration-config\mp-integration-overlay.yml'
$memberStackOverlay = Join-Path $Root 'scripts\integration-config\member-integration-local-stack.yml'
$memberOverlay = Join-Path $Root 'scripts\integration-config\member-integration-overlay.yml'
$mpCfg = if (Test-Path $mpOverlay) { "$overlay,$mpOverlay" } else { $overlay }
$memberCfgParts = @($memberStackOverlay)
if (Test-Path $memberOverlay) { $memberCfgParts += $memberOverlay }
$memberCfg = ($memberCfgParts -join ",")

Start-IntegrationJar -Title "mp-server" -Port 48086 -Jar $mpJar -LogFile $MpLog -ActiveProfiles $Profiles -ExtraConfig $mpCfg
Start-Sleep -Seconds 8
Start-IntegrationJar -Title "member-server" -Port 48087 -Jar $memberJar -LogFile $MemberLog -ActiveProfiles $Profiles -ExtraConfig $memberCfg
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
