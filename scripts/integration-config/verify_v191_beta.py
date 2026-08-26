#!/usr/bin/env python3
"""Verify V191 state on beta shenyu-ops (reads ops-test-remote.env; no secrets printed)."""
from __future__ import annotations

import os
import subprocess
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
ENV_FILE = Path(__file__).resolve().parent / "ops-test-remote.env"

DROPPED = [
    "sys_tenant",
    "sys_user",
    "sys_user_token",
    "sys_role",
    "sys_permission",
    "sys_dict_type",
    "sys_dict_data",
    "sys_operation_log",
]
KEPT = ["sys_param", "sys_message", "oa_ip_group", "oa_work_task_sheet"]


def load_env(path: Path) -> dict[str, str]:
    out: dict[str, str] = {}
    if not path.is_file():
        return out
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        k, v = line.split("=", 1)
        out[k.strip()] = v.strip()
    return out


def mysql_query(host: str, port: str, user: str, password: str, database: str, sql: str) -> str:
    env = os.environ.copy()
    env["MYSQL_PWD"] = password
    cmd = [
        "mysql",
        "-h",
        host,
        "-P",
        port,
        "-u",
        user,
        database,
        "-N",
        "-B",
        "-e",
        sql,
    ]
    try:
        proc = subprocess.run(cmd, capture_output=True, text=True, env=env, timeout=30)
    except FileNotFoundError:
        return "ERROR: mysql CLI not found"
    if proc.returncode != 0:
        return f"ERROR: {proc.stderr.strip() or proc.stdout.strip()}"
    return proc.stdout.strip()


def main() -> int:
    env = load_env(ENV_FILE)
    host = env.get("OPS_TEST_DB_HOST")
    port = env.get("OPS_TEST_DB_PORT", "3306")
    user = env.get("OPS_TEST_MASTER_USER")
    password = env.get("OPS_TEST_MASTER_PASSWORD")
    database = env.get("OPS_TEST_MASTER_DB", "shenyu-ops")
    if not all([host, user, password]):
        print("SKIP: ops-test-remote.env missing DB credentials")
        return 2

    print(f"Target: {host}:{port}/{database}")

    flyway = mysql_query(
        host,
        port,
        user,
        password,
        database,
        "SELECT version, success FROM flyway_schema_history WHERE version IN ('190','191') ORDER BY version;",
    )
    print("flyway_v190_v191:")
    print(flyway or "(empty)")

    for table in DROPPED:
        cnt = mysql_query(
            host,
            port,
            user,
            password,
            database,
            f"SELECT COUNT(*) FROM information_schema.tables "
            f"WHERE table_schema='{database}' AND table_name='{table}';",
        )
        status = "OK" if cnt == "0" else "PRESENT"
        print(f"  no_{table}: {status}")

    for table in KEPT:
        cnt = mysql_query(
            host,
            port,
            user,
            password,
            database,
            f"SELECT COUNT(*) FROM information_schema.tables "
            f"WHERE table_schema='{database}' AND table_name='{table}';",
        )
        status = "OK" if cnt == "1" else "MISSING"
        print(f"  has_{table}: {status}")

    total = mysql_query(
        host,
        port,
        user,
        password,
        database,
        "SELECT COUNT(*) FROM flyway_schema_history WHERE type='SQL' AND success=1;",
    )
    print(f"flyway_sql_count: {total}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
