#!/usr/bin/env python3
"""Apply patch-layout-template-permissions.sql to Football DB."""
from __future__ import annotations

import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PATCH = ROOT / "scripts/integration-config/patch-layout-template-permissions.sql"
DEFAULT_HOST = "101.37.161.136"
DEFAULT_PORT = "3306"
DEFAULT_USER = "shenyu"
DEFAULT_PASS = "Zhangwu+123456"
DEFAULT_DB = "shenyu-ops"


def main() -> int:
    if not PATCH.is_file():
        print(f"Missing patch file: {PATCH}", file=sys.stderr)
        return 1

    sql = PATCH.read_text(encoding="utf-8")
    cmd = [
        "mysql",
        f"-h{DEFAULT_HOST}",
        f"-P{DEFAULT_PORT}",
        f"-u{DEFAULT_USER}",
        f"-p{DEFAULT_PASS}",
        "--default-character-set=utf8mb4",
        DEFAULT_DB,
    ]
    print(f"Applying {PATCH.name} -> {DEFAULT_HOST}/{DEFAULT_DB}")
    proc = subprocess.run(cmd, input=sql.encode("utf-8"), capture_output=True)
    if proc.returncode != 0:
        print(proc.stderr.decode("utf-8", errors="replace"), file=sys.stderr)
        return proc.returncode

    verify = subprocess.run(
        cmd
        + [
            "-e",
            "SELECT m.id, m.name, m.permission FROM system_menu m "
            "WHERE m.id IN (6120,6170,6171,6172,6173) ORDER BY m.id; "
            "SELECT rm.role_id, rm.menu_id FROM system_role_menu rm "
            "WHERE rm.menu_id IN (6170,6171,6172,6173) AND rm.deleted=0 ORDER BY rm.role_id, rm.menu_id;",
        ],
        capture_output=True,
    )
    print(verify.stdout.decode("utf-8", errors="replace"))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
