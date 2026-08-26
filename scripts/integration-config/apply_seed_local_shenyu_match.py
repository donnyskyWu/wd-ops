#!/usr/bin/env python3
"""Apply seed-local-shenyu-match.sql with utf8mb4 (fixes Windows GBK garbling)."""
from __future__ import annotations

import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SQL = ROOT / "scripts" / "integration-config" / "seed-local-shenyu-match.sql"
REDIS_KEYS = (
    "data:filter:competitions:schedule:flat",
    "data:filter:competitions:schedule:grouped",
    "data:filter:competitions:result:flat",
    "data:filter:competitions:live:flat",
)


def flush_match_filter_cache() -> None:
    for key in REDIS_KEYS:
        subprocess.run(
            ["redis-cli", "-a", "123456", "DEL", key],
            capture_output=True,
            check=False,
        )


def main() -> int:
    if not SQL.is_file():
        print(f"Missing seed file: {SQL}", file=sys.stderr)
        return 1
    text = SQL.read_text(encoding="utf-8")
    cmd = [
        "mysql",
        "-uroot",
        "-proot",
        "--default-character-set=utf8mb4",
        "shenyu-match",
    ]
    proc = subprocess.run(cmd, input=text.encode("utf-8"), capture_output=True)
    if proc.returncode != 0:
        sys.stderr.write(proc.stderr.decode("utf-8", errors="replace"))
        return proc.returncode
    if proc.stdout:
        print(proc.stdout.decode("utf-8", errors="replace"))
    flush_match_filter_cache()
    print("seed-local-shenyu-match applied (utf8mb4); match filter redis cache cleared")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
