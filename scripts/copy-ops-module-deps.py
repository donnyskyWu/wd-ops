#!/usr/bin/env python3
"""RETIRED — OPS module deps live in football-front (pnpm catalog / web-ele package.json)."""
from __future__ import annotations
import sys

print(
    "[retired] scripts/copy-ops-module-deps.py — "
    "use football-front pnpm install; ops-platform-ui-vue removed.",
    file=sys.stderr,
)
raise SystemExit(1)
