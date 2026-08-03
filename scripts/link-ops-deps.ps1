# RETIRED — OPS runtime deps are declared in football-front/apps/web-ele/package.json (pnpm catalog).
# Junction from ops-platform-ui-vue/node_modules is no longer supported (ui-vue deleted).
$ErrorActionPreference = 'Continue'
Write-Host @"
[retired] scripts/link-ops-deps.ps1
OPS UI SSOT = football-front/apps/web-ele.
Install deps with: cd football-front && pnpm install
ops-platform-ui-vue junction fallback has ended.
"@ -ForegroundColor Yellow
exit 1
