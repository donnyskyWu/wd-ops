#!/usr/bin/env bash
# restart-all.sh - Restart local dev stack (Git Bash / WSL / Linux / macOS)
#
# Usage (from repo root):
#   bash scripts/restart-all.sh
#   bash scripts/restart-all.sh --no-frontend
#
# On Windows Git Bash, delegates to scripts/restart-all.ps1.

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
LOG_DIR="$SCRIPT_DIR/logs"
mkdir -p "$LOG_DIR"

case "$(uname -s)" in
  MINGW*|MSYS*|CYGWIN*)
    exec powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$SCRIPT_DIR/restart-all.ps1" "$@"
    ;;
esac

kill_port() {
  local port="$1"
  if command -v lsof >/dev/null 2>&1; then
    local pids
    pids="$(lsof -ti:"$port" 2>/dev/null || true)"
    if [[ -n "$pids" ]]; then
      echo "[stop] Port $port -> killing: $pids"
      kill -TERM $pids 2>/dev/null || true
      sleep 1
      kill -KILL $pids 2>/dev/null || true
    else
      echo "[stop] Port $port is already free"
    fi
  elif command -v fuser >/dev/null 2>&1; then
    fuser -k "${port}/tcp" 2>/dev/null || echo "[stop] Port $port is already free"
  else
    echo "Need lsof or fuser to stop port $port" >&2
    exit 1
  fi
}

wait_port_free() {
  local port="$1" timeout="${2:-15}" i=0
  while [[ $i -lt $timeout ]]; do
    if ! lsof -ti:"$port" >/dev/null 2>&1; then
      return 0
    fi
    sleep 0.5
    i=$((i + 1))
  done
  echo "[warn] Port $port still in use after ${timeout}s" >&2
  return 1
}

echo "=== Restart dev stack ==="
echo "Repo: $ROOT"

echo ""
echo "--- Stop ---"
kill_port 3000
kill_port 8080
kill_port 8000
sleep 2
wait_port_free 3000 || true
wait_port_free 8080 || true
wait_port_free 8000 || true

echo ""
echo "--- Start ---"

COLLECTOR_DIR="$ROOT/unify-collector-api"
BACKEND_DIR="$ROOT/ops-platform-server/ops-platform-module-oa"
UI_DIR="$ROOT/ops-platform-ui-vue"

PYTHON="python3"
if [[ -x "$COLLECTOR_DIR/.venv/bin/python" ]]; then
  PYTHON="$COLLECTOR_DIR/.venv/bin/python"
fi

(cd "$COLLECTOR_DIR" && nohup "$PYTHON" -m uvicorn app.main:app --reload --host 127.0.0.1 --port 8000 >>"$LOG_DIR/collector-run.log" 2>&1 &)
echo "[start] unify-collector-api :8000 -> $LOG_DIR/collector-run.log"

(cd "$BACKEND_DIR" && nohup mvn spring-boot:run -Dspring-boot.run.profiles=dev >>"$LOG_DIR/backend-dev-run.log" 2>&1 &)
echo "[start] ops-platform-server :8080 -> $LOG_DIR/backend-dev-run.log"

if [[ "${1:-}" != "--no-frontend" ]]; then
  (cd "$UI_DIR" && nohup npm run dev >>"$LOG_DIR/frontend-dev-run.log" 2>&1 &)
  echo "[start] ops-platform-ui-vue :3000 -> $LOG_DIR/frontend-dev-run.log"
else
  echo "[start] Frontend skipped (--no-frontend)"
fi

echo ""
echo "--- URLs (may take a minute to become ready) ---"
echo "Collector: http://127.0.0.1:8000/docs"
echo "Backend:   http://localhost:8080/actuator/health"
echo "Frontend:  http://localhost:3000"
echo "Logs:      $LOG_DIR"
