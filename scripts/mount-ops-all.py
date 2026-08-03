#!/usr/bin/env python3
"""RETIRED — OPS UI SSOT is football-front (A-WP1 cutover).

Do not remount from ops-platform-ui-vue. Edit pages under:
  football-front/apps/web-ele/src/views/ops/**
  football-front/apps/web-ele/src/components/ops/**

Exit non-zero so CI/humans do not silently remount a deleted tree.
"""
from __future__ import annotations
import sys

MSG = """[retired] scripts/mount-ops-all.py
OPS UI SSOT = football-front/apps/web-ele (views/ops, components/ops).
ops-platform-ui-vue has been removed — remount is no longer supported.
Edit football-front directly. See docs/delivery/OPS-FOOTBALL-MERGE-WORK-PLAN.md A-WP1.
"""


def main() -> int:
    print(MSG, file=sys.stderr)
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
