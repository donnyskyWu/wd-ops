# Push scripts/integration-config/*-server-local.yaml to Nacos namespace "local".
param(
  [string]$NacosAddr = '127.0.0.1:8848',
  [string]$Namespace = 'local',
  [string]$Group = 'DEFAULT_GROUP'
)
$ErrorActionPreference = 'Stop'
$cfgDir = Join-Path $PSScriptRoot 'integration-config'
$base = "http://${NacosAddr}/nacos/v1/cs/configs"
Get-ChildItem $cfgDir -Filter '*-server-local.yaml' | ForEach-Object {
  $content = Get-Content $_.FullName -Raw -Encoding UTF8
  $body = @{ dataId = $_.Name; group = $Group; tenant = $Namespace; type = 'yaml'; content = $content }
  $ok = Invoke-RestMethod -Uri $base -Method Post -Body $body
  Write-Host "$($_.Name) -> $ok"
}
