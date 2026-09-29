#!/usr/bin/env python3
"""Apply M4 account import SQL with utf8mb4 stdin (avoids Windows PowerShell pipe corruption)."""
from __future__ import annotations

import argparse
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DEFAULT_SQL = ROOT / "scripts" / "import" / "import-accounts-shenyu-ops-20260918.sql"


def main() -> int:
    parser = argparse.ArgumentParser(description="Apply account import SQL with utf8mb4 encoding")
    parser.add_argument(
        "--sql",
        type=Path,
        default=DEFAULT_SQL,
        help="SQL file path (default: import-accounts-shenyu-ops-20260918.sql)",
    )
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=3306)
    parser.add_argument("--user", default="root")
    parser.add_argument("--password", default="root")
    parser.add_argument(
        "--database",
        default=None,
        help="Target database; omit when SQL contains USE db;",
    )
    args = parser.parse_args()

    if not args.sql.is_file():
        print(f"Missing SQL file: {args.sql}", file=sys.stderr)
        return 1

    text = args.sql.read_text(encoding="utf-8")
    cmd = [
        "mysql",
        f"-h{args.host}",
        f"-P{args.port}",
        f"-u{args.user}",
        f"-p{args.password}",
        "--default-character-set=utf8mb4",
    ]
    if args.database:
        cmd.append(args.database)

    print(f"Applying {args.sql.name} -> {args.host}:{args.port}/{args.database or '(from SQL USE)'} (utf8mb4 stdin)")
    proc = subprocess.run(cmd, input=text.encode("utf-8"), capture_output=True)
    if proc.returncode != 0:
        sys.stderr.write(proc.stderr.decode("utf-8", errors="replace"))
        return proc.returncode
    if proc.stdout:
        print(proc.stdout.decode("utf-8", errors="replace"))
    print("Account import applied successfully (utf8mb4)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
