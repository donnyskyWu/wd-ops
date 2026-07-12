# S2-A: Extract Ops menu/permission map -> CSV + system_menu seed SQL
# SSOT: ops-platform-ui-vue Layout.vue + router/index.ts
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent $PSScriptRoot
Push-Location $Root
try {
    python (Join-Path $PSScriptRoot 'extract-oa-menu.py')
    if ($LASTEXITCODE -ne 0) { throw "extract-oa-menu.py failed with exit code $LASTEXITCODE" }
    Write-Host "OK: docs/delivery/oa-menu-permission-map.csv"
    Write-Host "OK: scripts/integration-config/seed-oa-system-menu.sql"
}
finally {
    Pop-Location
}
