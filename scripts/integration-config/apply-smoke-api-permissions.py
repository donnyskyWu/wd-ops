#!/usr/bin/env python3
"""Apply patch-smoke-api-permissions.sql to Football system_menu DB(s)."""
from __future__ import annotations

import argparse
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PATCH = ROOT / "scripts/integration-config/patch-smoke-api-permissions.sql"


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
            "SELECT m.id, m.permission, rm.role_id "
            "FROM system_menu m "
            "LEFT JOIN system_role_menu rm ON rm.menu_id=m.id AND rm.role_id=1 AND rm.deleted=0 "
            "WHERE m.id IN (6137,6138,6139,6149,6174) ORDER BY m.id;",
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
    parser.add_argument(
        "--databases",
        default="wd,shenyu-system",
        help="Comma-separated DB names (default: wd,shenyu-system)",
    )
    args = parser.parse_args()

    rc = 0
    for db in [d.strip() for d in args.databases.split(",") if d.strip()]:
        rc |= apply(args.host, args.port, args.user, args.password, db)
    return rc


if __name__ == "__main__":
    raise SystemExit(main())
