# RETIRED — Standalone UAT browser E2E (:3000) ended with ops-platform-ui-vue (A-WP1).
#
# Gate / Football E2E:
#   .\scripts\run-gate-football-e2e.ps1
#   .\scripts\run-uat-football-e2e.ps1
#
# Specs live under: football-front/apps/web-ele/tests/

$ErrorActionPreference = "Continue"
Write-Host @"
[retired] scripts/run-uat-browser-e2e.ps1
Standalone :3000 Playwright path removed with ops-platform-ui-vue.

Use Gate path against :5777:
  .\scripts\start-ops-dev.ps1
  .\scripts\run-gate-football-e2e.ps1

Or content smoke only:
  cd football-front/apps/web-ele
  npx playwright test tests/football-content-smoke.spec.ts --config=playwright.config.ts
"@ -ForegroundColor Yellow
exit 1
