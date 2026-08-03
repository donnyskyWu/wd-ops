# RETIRED — standalone stack (:3000/:8080 + ops-platform-ui-vue) ended (A-WP1).
#
# Use Football Gate path:
#   .\scripts\start-ops-dev.ps1
#   UI http://localhost:5777

$ErrorActionPreference = "Continue"
Write-Host @"
[retired] scripts/restart-all.ps1
ops-platform-ui-vue (:3000) has been removed.

Start Gate stack:
  .\scripts\start-ops-dev.ps1
  -> http://localhost:5777  admin / admin123  tenant 1

OPS UI SSOT = football-front/apps/web-ele (views/ops, components/ops).
"@ -ForegroundColor Yellow
exit 1
