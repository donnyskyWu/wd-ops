# start-integration-stack.ps1 — Local integration: Docker Nacos + oa-server (S1-A)
#
# Usage (from repo root):
#   .\scripts\start-integration-stack.ps1
#
# If Docker fails, oa-server still starts on 48094 (Nacos fail-fast=false).

[CmdletBinding()]
param(
    [string]$Profiles = "dev,dev-nacos,dev-nacos-local",
    [int]$WaitSeconds = 120
)

$ErrorActionPreference = "Continue"
$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Set-Location $Root

Write-Host "=== Local integration stack (Nacos + oa-server) ==="
& (Join-Path $PSScriptRoot "start-nacos-local.ps1") -ContinueOnDockerFailure
& (Join-Path $PSScriptRoot "start-integration-oa.ps1") -Profiles $Profiles -WaitSeconds $WaitSeconds -SkipNacosPrerequisiteCheck
