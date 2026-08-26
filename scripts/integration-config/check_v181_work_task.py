#!/usr/bin/env python3
"""Check V181/V182 status on local/test DB."""
from __future__ import annotations

import argparse
import os
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ENV_FILE = ROOT / "scripts/integration-config/ops-test-remote.env"


def load_env() -> dict[str, str]:
    env: dict[str, str] = {}
    if not ENV_FILE.is_file():
        return env
    for line in ENV_FILE.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        k, v = line.split("=", 1)
        env[k.strip()] = v.strip().strip('"').strip("'")
    return env


def mysql_exec(host, port, user, password, database, sql) -> tuple[str, int]:
    env = os.environ.copy()
    env["MYSQL_PWD"] = password
    proc = subprocess.run(
        ["mysql", f"-h{host}", f"-P{port}", f"-u{user}", "-N", "-B", database, "-e", sql],
        capture_output=True,
        env=env,
    )
    out = proc.stdout.decode("utf-8", errors="replace").strip()
    err = proc.stderr.decode("utf-8", errors="replace").strip()
    if proc.returncode != 0:
        print(err or out, file=sys.stderr)
    return out, proc.returncode


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--target", choices=["local", "test"], default="local")
    args = parser.parse_args()
    cfg = load_env()
    if args.target == "local":
        host, port, user, password = "127.0.0.1", "3306", "root", "root"
        ops_db, sys_db = "shenyu-ops", "shenyu-sys"
        sys_user, sys_pwd = user, password
    else:
        host = cfg.get("OPS_TEST_DB_HOST", "110.42.49.224")
        port = cfg.get("OPS_TEST_DB_PORT", "3306")
        user = cfg.get("OPS_TEST_MASTER_USER", "shenyu-ops")
        password = cfg.get("OPS_TEST_MASTER_PASSWORD", "")
        ops_db = cfg.get("OPS_TEST_MASTER_DB", "shenyu-ops")
        sys_db = cfg.get("OPS_TEST_SYSTEM_DB", "shenyu-system")
        sys_user = cfg.get("OPS_TEST_SYSTEM_USER", "shenyu-system")
        sys_pwd = cfg.get("OPS_TEST_SYSTEM_PASSWORD", "")

    print(f"=== {args.target} {host}:{port} ===")
    for ver in ("181", "182"):
        out, code = mysql_exec(host, port, user, password, ops_db,
                               f"SELECT COUNT(*) FROM flyway_schema_history WHERE version='{ver}' AND success=1")
        print(f"flyway V{ver} on {ops_db}: {out if code == 0 else 'ERR'}")

    out, code = mysql_exec(host, port, user, password, ops_db,
                           "SELECT COUNT(*) FROM information_schema.tables "
                           f"WHERE table_schema='{ops_db}' AND table_name='oa_work_task_sheet'")
    print(f"table oa_work_task_sheet: {out if code == 0 else 'ERR'}")

    out, code = mysql_exec(host, port, user, password, ops_db,
                           "SELECT param_value FROM sys_param WHERE param_key='work_task.default_template_id' AND deleted=0 LIMIT 1")
    print(f"work_task.default_template_id: {out if code == 0 else 'ERR'}")

    out, code = mysql_exec(host, port, sys_user, sys_pwd, sys_db,
                           "SELECT COUNT(*) FROM system_menu WHERE id=6176")
    print(f"menu 6176 on {sys_db}: {out if code == 0 else 'ERR'}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
