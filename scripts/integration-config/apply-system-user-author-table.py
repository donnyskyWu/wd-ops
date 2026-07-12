#!/usr/bin/env python3
"""Apply patch-system-user-author-table.sql to localhost wd (Gate integration path)."""
from __future__ import annotations

import argparse
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PATCH = ROOT / "scripts/integration-config/patch-system-user-author-table.sql"


def apply(host: str, port: str, user: str, password: str, database: str) -> int:
    if not PATCH.is_file():
        print(f"Missing patch file: {PATCH}", file=sys.stderr)
        return 1

    sql = PATCH.read_text(encoding="utf-8")
    cmd = [
        "mysql",
        f"-h{host}",
        f"-P{port}",
        f"-u{user}",
        f"-p{password}",
        "--default-character-set=utf8mb4",
        database,
    ]
    print(f"Applying {PATCH.name} -> {host}:{port}/{database}")
    proc = subprocess.run(cmd, input=sql.encode("utf-8"), capture_output=True)
    if proc.returncode != 0:
        print(proc.stderr.decode("utf-8", errors="replace"), file=sys.stderr)
        return proc.returncode

    verify = subprocess.run(
        cmd
        + [
            "-e",
            "SELECT table_name FROM information_schema.tables "
            "WHERE table_schema=DATABASE() AND table_name='system_user_author';",
        ],
        capture_output=True,
    )
    print(verify.stdout.decode("utf-8", errors="replace"))
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--host", default="localhost")
    parser.add_argument("--port", default="3306")
    parser.add_argument("--user", default="root")
    parser.add_argument("--password", default="root")
    parser.add_argument("--database", default="wd")
    args = parser.parse_args()
    return apply(args.host, args.port, args.user, args.password, args.database)


if __name__ == "__main__":
    raise SystemExit(main())
