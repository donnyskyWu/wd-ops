# RETIRED — Ops standalone :3000 + :8080 harness ended with ops-platform-ui-vue (A-WP1).
#
# Gate / daily path:
#   .\scripts\start-ops-dev.ps1
#   UI: http://localhost:5777  (football-front)
#
# See docs/delivery/OPS-FOOTBALL-MERGE-WORK-PLAN.md A-WP1 · OPS-STARTUP-MATRIX.md

$ErrorActionPreference = "Continue"
Write-Host @"
[retired] scripts/start-ops-standalone.ps1
ops-platform-ui-vue (:3000) has been removed.
Use Football Gate stack instead:

  .\scripts\start-ops-dev.ps1
  -> http://localhost:5777  admin / admin123  tenant 1

OPS UI SSOT = football-front/apps/web-ele (views/ops, components/ops).
"@ -ForegroundColor Yellow
exit 1
