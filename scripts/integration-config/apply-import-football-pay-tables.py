#!/usr/bin/env python3
"""Apply import-football-pay-tables.sql to wd with UTF-8 (no PowerShell pipe corruption)."""
from __future__ import annotations

import argparse
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SEED = ROOT / "scripts/integration-config/import-football-pay-tables.sql"
DEFAULT_HOST = "101.37.161.136"
DEFAULT_PORT = "3306"
DEFAULT_USER = "shenyu"
DEFAULT_PASS = "Zhangwu+123456"
DEFAULT_DB = "shenyu-ops"


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
        f"--host={args.host}",
        f"--port={args.port}",
        f"--user={args.user}",
        f"--password={args.password}",
        "--default-character-set=utf8mb4",
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
            f"--host={args.host}",
            f"--port={args.port}",
            f"--user={args.user}",
            f"--password={args.password}",
            "--default-character-set=utf8mb4",
            args.database,
            "-e",
            "SHOW TABLES LIKE 'pay_%'; "
            "SELECT 'pay_all_order' AS tbl, COUNT(*) AS cnt FROM pay_all_order "
            "UNION ALL SELECT 'pay_gold_order', COUNT(*) FROM pay_gold_order;",
        ],
        capture_output=True,
    )
    print(verify.stdout.decode("utf-8", errors="replace"))
    if verify.returncode != 0:
        print(verify.stderr.decode("utf-8", errors="replace"), file=sys.stderr)
        return verify.returncode
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
