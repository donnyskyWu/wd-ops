# Push ops-server remote multidb overlay to Nacos (remote cutover prep — user approval required).
# ADR-058 P2: canonical DataId = ops-server-remote-multidb.yaml；历史 oa-server-remote-multidb.yaml 仍可 -DataId 显式推送。
param(
  [string]$NacosAddr = '127.0.0.1:8848',
  [string]$Namespace = 'local',
  [string]$Group = 'DEFAULT_GROUP',
  [string]$DataId = 'ops-server-remote-multidb.yaml',
  [switch]$WhatIf
)
$ErrorActionPreference = 'Stop'
$cfg = Join-Path $PSScriptRoot 'integration-config/oa-server-remote-multidb.yaml'
if (-not (Test-Path $cfg)) { throw "Missing $cfg" }
$content = Get-Content $cfg -Raw -Encoding UTF8
Write-Host "Target: Nacos $NacosAddr namespace=$Namespace dataId=$DataId"
Write-Host "Note: file path kept as oa-server-remote-multidb.yaml (CLEANUP); Nacos DataId default is ops-server-* (ADR-058 P2)."
if ($WhatIf) {
  Write-Host "--- WhatIf: would push $(($content.Length)) bytes ---"
  Write-Host $content
  exit 0
}
$base = "http://${NacosAddr}/nacos/v1/cs/configs"
$body = @{ dataId = $DataId; group = $Group; tenant = $Namespace; type = 'yaml'; content = $content }
$ok = Invoke-RestMethod -Uri $base -Method Post -Body $body
Write-Host "Push result: $ok"
Write-Host "Merge into ops-server active profile on remote host (see mdb-s4-nacos-matrix.md)."
# Optional legacy alias push for short dual-read window
if ($DataId -eq 'ops-server-remote-multidb.yaml') {
  $legacy = 'oa-server-remote-multidb.yaml'
  $bodyLegacy = @{ dataId = $legacy; group = $Group; tenant = $Namespace; type = 'yaml'; content = $content }
  $ok2 = Invoke-RestMethod -Uri $base -Method Post -Body $bodyLegacy
  Write-Host "Legacy alias push ($legacy): $ok2"
}
