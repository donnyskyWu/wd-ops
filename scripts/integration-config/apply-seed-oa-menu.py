#!/usr/bin/env python3
"""Apply seed-oa-system-menu.sql with UTF-8 stdin (no PowerShell pipe corruption).

ADR-056 local integration: target localhost ``shenyu-system`` (Football system-server
master). Remote beta test: use ops-test-remote.env credentials.

Example (remote beta test)::

    python scripts/integration-config/apply-seed-oa-menu.py \\
      --host 110.42.49.224 --user shenyu-system --password <见 env> --database shenyu-system

Example (local five-DB)::

    python scripts/integration-config/apply-seed-oa-menu.py \\
      --host localhost --user root --password root --database shenyu-system
"""
from __future__ import annotations

import argparse
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SEED = ROOT / "scripts/integration-config/seed-oa-system-menu.sql"
DEFAULT_HOST = "101.37.161.136"
DEFAULT_PORT = "3306"
DEFAULT_USER = "shenyu"
DEFAULT_PASS = "Zhangwu+123456"
DEFAULT_DB = "wd"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--host", default=DEFAULT_HOST)
    parser.add_argument("--port", default=DEFAULT_PORT)
    parser.add_argument("--user", default=DEFAULT_USER)
    parser.add_argument("--password", default=DEFAULT_PASS)
    parser.add_argument("--database", default=DEFAULT_DB)
    parser.add_argument("--seed", type=Path, default=SEED)
    args = parser.parse_args()

    if not args.seed.is_file():
        print(f"Missing seed file: {args.seed}", file=sys.stderr)
        return 1

    sql = args.seed.read_text(encoding="utf-8")
    cmd = [
        "mysql",
        f"-h{args.host}",
        f"-P{args.port}",
        f"-u{args.user}",
        f"-p{args.password}",
        f"--default-character-set=utf8mb4",
        args.database,
    ]
    print(f"Applying {args.seed.name} -> {args.host}/{args.database} (utf8mb4 stdin)")
    proc = subprocess.run(cmd, input=sql.encode("utf-8"), capture_output=True)
    if proc.returncode != 0:
        print(proc.stderr.decode("utf-8", errors="replace"), file=sys.stderr)
        return proc.returncode

    verify = subprocess.run(
        [
            "mysql",
            f"-h{args.host}",
            f"-P{args.port}",
            f"-u{args.user}",
            f"-p{args.password}",
            f"--default-character-set=utf8mb4",
            args.database,
            "-e",
            "SELECT id, name, HEX(name) FROM system_menu WHERE id IN (6100,6157) ORDER BY id;",
        ],
        capture_output=True,
    )
    print(verify.stdout.decode("utf-8", errors="replace"))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
