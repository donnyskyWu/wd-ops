#!/usr/bin/env python3
"""Delete failed Flyway rows from shenyu-ops.flyway_schema_history.

Usage:
  python repair-flyway-failed.py          # beta (ops-test-remote.env)
  python repair-flyway-failed.py --local  # localhost multidb root/root
"""
from __future__ import annotations

import argparse
import os
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ENV_FILE = ROOT / "scripts/integration-config/ops-test-remote.env"


def load_beta_env() -> dict[str, str]:
    env: dict[str, str] = {}
    if not ENV_FILE.is_file():
        print(f"Missing {ENV_FILE}", file=sys.stderr)
        sys.exit(1)
    for line in ENV_FILE.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        k, v = line.split("=", 1)
        env[k.strip()] = v.strip().strip('"').strip("'")
    return env


def mysql_exec(host: str, port: str, user: str, password: str, database: str, sql: str) -> str:
    env = os.environ.copy()
    env["MYSQL_PWD"] = password
    proc = subprocess.run(
        [
            "mysql",
            f"-h{host}",
            f"-P{port}",
            f"-u{user}",
            "--default-character-set=utf8mb4",
            "-N",
            "-B",
            database,
            "-e",
            sql,
        ],
        capture_output=True,
        env=env,
    )
    if proc.returncode != 0:
        print(proc.stderr.decode("utf-8", errors="replace"), file=sys.stderr)
        sys.exit(proc.returncode)
    return proc.stdout.decode("utf-8", errors="replace")


def repair(host: str, port: str, user: str, password: str, database: str) -> None:
    rows = mysql_exec(
        host,
        port,
        user,
        password,
        database,
        "SELECT installed_rank, version, success FROM flyway_schema_history WHERE success = 0 ORDER BY installed_rank",
    ).strip()
    if not rows:
        print(f"[ok] No failed Flyway migrations on {host}/{database}")
        return

    print(f"Failed migrations on {host}/{database}:")
    print(rows)
    for line in rows.splitlines():
        rank, version, _success = line.split("\t")
        mysql_exec(
            host,
            port,
            user,
            password,
            database,
            f"DELETE FROM flyway_schema_history WHERE installed_rank = {rank} AND success = 0",
        )
        print(f"[repair] removed failed V{version} (rank {rank})")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--local", action="store_true", help="localhost:3306/shenyu-ops root/root")
    args = parser.parse_args()

    if args.local:
        repair("127.0.0.1", "3306", "root", "root", "shenyu-ops")
        return

    cfg = load_beta_env()
    host = cfg.get("OPS_TEST_DB_HOST", "110.42.49.224")
    port = cfg.get("OPS_TEST_DB_PORT", "3306")
    user = cfg.get("OPS_TEST_MASTER_USER", "shenyu-ops")
    password = cfg.get("OPS_TEST_MASTER_PASSWORD", cfg.get("OPS_WD_TEST_PASSWORD", ""))
    repair(host, port, user, password, "shenyu-ops")


if __name__ == "__main__":
    main()
