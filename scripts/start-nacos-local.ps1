# start-nacos-local.ps1 — Standalone Nacos 2.x for local Football/OA integration
#
# Requires Docker Desktop (or docker CLI on PATH).
# Console: http://127.0.0.1:8848/nacos  (username/password: nacos / nacos)
#
# Usage (from repo root):
#   .\scripts\start-nacos-local.ps1
#   .\scripts\start-nacos-local.ps1 -ContinueOnDockerFailure   # warn only (for stack script)
#
# Then start oa-server with:
#   .\scripts\start-integration-oa.ps1
#   or mvn spring-boot:run "-Dspring-boot.run.profiles=dev,dev-nacos,dev-nacos-local"

[CmdletBinding()]
param(
    [string]$ContainerName = "nacos-standalone-local",
    [int]$Port = 8848,
    [switch]$ContinueOnDockerFailure
)

$ErrorActionPreference = "Stop"

function Test-DockerAvailable {
    try {
        docker info *> $null
        return $LASTEXITCODE -eq 0
    } catch {
        return $false
    }
}

if (-not (Test-DockerAvailable)) {
    $msg = "Docker is not available. Install Docker Desktop or add docker to PATH."
    if ($ContinueOnDockerFailure) {
        Write-Warning $msg
        Write-Host "Continuing without local Nacos; oa-server can still run on 48094 (fail-fast=false)."
        exit 0
    }
    Write-Error $msg
    exit 1
}

$existing = docker ps -a --filter "name=^/${ContainerName}$" --format "{{.Names}}" 2>$null
if ($existing -eq $ContainerName) {
    $running = docker ps --filter "name=^/${ContainerName}$" --format "{{.Names}}" 2>$null
    if ($running -ne $ContainerName) {
        Write-Host "[start] Starting existing container $ContainerName ..."
        docker start $ContainerName | Out-Null
    } else {
        Write-Host "[ok] Container $ContainerName already running"
    }
} else {
    Write-Host "[create] Pulling/running nacos/nacos-server:v2.3.2 (standalone, port $Port) ..."
    docker run -d `
        --name $ContainerName `
        -e MODE=standalone `
        -e NACOS_AUTH_ENABLE=false `
        -p "${Port}:8848" `
        -p "9848:9848" `
        nacos/nacos-server:v2.3.2 | Out-Null
}

Write-Host ""
Write-Host "Nacos console: http://127.0.0.1:${Port}/nacos"
Write-Host "Credentials:   nacos / nacos"
Write-Host "oa-server:     profiles dev,dev-nacos,dev-nacos-local (server-addr 127.0.0.1:8848)"
Write-Host "Stop:          docker stop $ContainerName"

Write-Host "Waiting for Nacos HTTP (up to 90s) ..."
$deadline = (Get-Date).AddSeconds(90)
$ready = $false
while ((Get-Date) -lt $deadline -and -not $ready) {
    try {
        $null = Invoke-WebRequest -Uri "http://127.0.0.1:${Port}/nacos/" -UseBasicParsing -TimeoutSec 5
        $ready = $true
        Write-Host "[ready] Nacos is up"
    } catch {
        Start-Sleep -Seconds 3
    }
}
if (-not $ready) {
    Write-Warning "Nacos container started but HTTP not ready yet; check: docker logs $ContainerName"
}
