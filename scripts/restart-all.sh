#!/usr/bin/env bash
# RETIRED — standalone :3000 stack ended with ops-platform-ui-vue (A-WP1).
set -euo pipefail
cat <<'EOF' >&2
[retired] scripts/restart-all.sh
ops-platform-ui-vue (:3000) has been removed.

Use Football Gate path (Windows):
  ./scripts/start-ops-dev.ps1
  -> http://localhost:5777

OPS UI SSOT = football-front/apps/web-ele (views/ops, components/ops).
EOF
exit 1
