# Push oa-server-remote-multidb.yaml to Nacos (remote cutover prep — user approval required).
param(
  [string]$NacosAddr = '127.0.0.1:8848',
  [string]$Namespace = 'local',
  [string]$Group = 'DEFAULT_GROUP',
  [string]$DataId = 'oa-server-remote-multidb.yaml',
  [switch]$WhatIf
)
$ErrorActionPreference = 'Stop'
$cfg = Join-Path $PSScriptRoot 'integration-config/oa-server-remote-multidb.yaml'
if (-not (Test-Path $cfg)) { throw "Missing $cfg" }
$content = Get-Content $cfg -Raw -Encoding UTF8
Write-Host "Target: Nacos $NacosAddr namespace=$Namespace dataId=$DataId"
if ($WhatIf) {
  Write-Host "--- WhatIf: would push $(($content.Length)) bytes ---"
  Write-Host $content
  exit 0
}
$base = "http://${NacosAddr}/nacos/v1/cs/configs"
$body = @{ dataId = $DataId; group = $Group; tenant = $Namespace; type = 'yaml'; content = $content }
$ok = Invoke-RestMethod -Uri $base -Method Post -Body $body
Write-Host "Push result: $ok"
Write-Host "Merge into oa-server active profile on remote host (see mdb-s4-nacos-matrix.md)."
