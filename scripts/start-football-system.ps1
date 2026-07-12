# start-football-system.ps1 - Start Football microservices for localhost:5777 (Gateway already on :48080)
# Usage: .\scripts\start-football-system.ps1 [-SkipBuild]

[CmdletBinding()]
param(
    [string]$Profiles = "local,local-nacos",
    [int]$WaitSeconds = 300,
    [switch]$SkipBuild
)

$ErrorActionPreference = "Continue"
$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$LogDir = Join-Path $PSScriptRoot "logs"
New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

$Services = @(
    @{ Name = "infra-server"; Port = 48082; Rel = "football-module-infra\football-module-infra-server"; Jar = "football-module-infra-server.jar" },
    @{ Name = "mp-server";    Port = 48086; Rel = "football-module-mp\football-module-mp-server"; Jar = "football-module-mp-server.jar" },
    @{ Name = "system-server"; Port = 48081; Rel = "football-module-system\football-module-system-server"; Jar = "football-module-system-server.jar" }
)

function Stop-Port {
    param([int]$Port)
    @(Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty OwningProcess -Unique) | ForEach-Object {
        Write-Host "[stop] Port $Port PID $_"
        Stop-Process -Id $_ -Force -ErrorAction SilentlyContinue
    }
}

function Start-FootballJar {
    param($Svc)
    $jarPath = Join-Path $Root ("football-backend-saas\" + $Svc.Rel + "\target\" + $Svc.Jar)
    $logFile = Join-Path $LogDir ($Svc.Name + "-football.log")
    if (-not (Test-Path $jarPath)) { Write-Error "Missing jar: $jarPath"; return $false }
    $busy = Get-NetTCPConnection -LocalPort $Svc.Port -State Listen -ErrorAction SilentlyContinue
    if ($busy) { Write-Host "[ok] $($Svc.Name) already on $($Svc.Port)"; return $true }
    Write-Host "[start] $($Svc.Name) :$($Svc.Port)"
    $inner = "`$host.UI.RawUI.WindowTitle = '$($Svc.Name)'; & java -jar '$jarPath' --spring.profiles.active="$Profiles" *>&1 | Tee-Object -FilePath '$logFile' -Append"
    Start-Process powershell.exe -ArgumentList @("-NoProfile","-ExecutionPolicy","Bypass","-NoExit","-Command",$inner) -WindowStyle Minimized | Out-Null
    return $true
}

Write-Host "=== Football system stack (Nacos local, MySQL wd) ==="
foreach ($s in $Services) { Stop-Port -Port $s.Port }
Start-Sleep -Seconds 2

if (-not $SkipBuild) {
    Write-Host "[build] infra + mp + system ..."
    Push-Location (Join-Path $Root "football-backend-saas")
    mvn -pl football-module-infra/football-module-infra-server,football-module-mp/football-module-mp-server,football-module-system/football-module-system-server -am package -DskipTests
    if ($LASTEXITCODE -ne 0) { Pop-Location; exit 1 }
    Pop-Location
}

foreach ($s in $Services) {
    if (-not (Start-FootballJar -Svc $s)) { exit 1 }
    Start-Sleep -Seconds 10
}

$deadline = (Get-Date).AddSeconds($WaitSeconds)
$ready = $false
while ((Get-Date) -lt $deadline -and -not $ready) {
    try {
        $h = Invoke-RestMethod "http://127.0.0.1:48081/actuator/health" -TimeoutSec 5
        $t = Invoke-RestMethod "http://localhost:48080/admin-api/system/tenant/get-id-by-name?name=”Ûµ¿‘¥¬Î" -TimeoutSec 8
        if ($h.status -eq "UP" -and $t.code -eq 0) {
            Write-Host "[ready] system-server UP; tenant API code=0 id=$($t.data)"
            $ready = $true
        }
    } catch { Start-Sleep -Seconds 5 }
}

if (-not $ready) {
    Write-Warning "Not ready - check logs in $LogDir"
    exit 1
}
Write-Host "Login: http://localhost:5777  (admin / admin123 from apps/web-ele/.env.development)"
