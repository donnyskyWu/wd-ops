#!/usr/bin/env python3
"""Probe beta shenyu-ops / shenyu-system for Flyway + today's schema (no secrets printed)."""
from __future__ import annotations

import os
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]  # .../wd
ENV_FILE = ROOT / "scripts/integration-config/ops-test-remote.env"


def load_env() -> dict[str, str]:
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


def mysql(host: str, port: str, user: str, password: str, database: str, sql: str) -> str:
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
    out = proc.stdout.decode("utf-8", errors="replace")
    err = proc.stderr.decode("utf-8", errors="replace")
    if proc.returncode != 0:
        return f"ERR {proc.returncode}: {err.strip()}"
    return out.strip()


def main() -> None:
    cfg = load_env()
    host = cfg.get("OPS_TEST_DB_HOST", "110.42.49.224")
    port = cfg.get("OPS_TEST_DB_PORT", "3306")
    master_user = cfg.get("OPS_TEST_MASTER_USER", "shenyu-ops")
    master_pw = cfg.get("OPS_TEST_MASTER_PASSWORD", cfg.get("OPS_WD_TEST_PASSWORD", ""))
    master_db = cfg.get("OPS_TEST_MASTER_DB", "shenyu-ops")
    sys_user = cfg.get("OPS_TEST_SYSTEM_USER", "shenyu-system")
    sys_pw = cfg.get("OPS_TEST_SYSTEM_PASSWORD", "")
    sys_db = cfg.get("OPS_TEST_SYSTEM_DB", "shenyu-system")

    print(f"=== TARGET === host={host} port={port} master_db={master_db} user={master_user}")
    print("=== FLYWAY TOP 25 ===")
    print(
        mysql(
            host,
            port,
            master_user,
            master_pw,
            master_db,
            "SELECT installed_rank, version, description, success, installed_on "
            "FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 25",
        )
    )
    print("=== MAX VERSION ===")
    print(
        mysql(
            host,
            port,
            master_user,
            master_pw,
            master_db,
            "SELECT MAX(CAST(version AS UNSIGNED)) FROM flyway_schema_history "
            "WHERE version REGEXP '^[0-9]+$'",
        )
    )
    print("=== V165-V167 ROWS ===")
    print(
        mysql(
            host,
            port,
            master_user,
            master_pw,
            master_db,
            "SELECT version, description, success, installed_on FROM flyway_schema_history "
            "WHERE version IN ('165','166','167') ORDER BY version",
        )
    )
    print("=== FAILED ===")
    print(
        mysql(
            host,
            port,
            master_user,
            master_pw,
            master_db,
            "SELECT installed_rank, version, success FROM flyway_schema_history WHERE success=0",
        )
    )
    print("=== collect_enabled COLUMNS ===")
    print(
        mysql(
            host,
            port,
            master_user,
            master_pw,
            master_db,
            "SELECT TABLE_NAME, COLUMN_NAME FROM information_schema.COLUMNS "
            f"WHERE TABLE_SCHEMA='{master_db}' AND COLUMN_NAME='collect_enabled'",
        )
    )
    print("=== is_unified / oa_collect_task_account ===")
    print(
        mysql(
            host,
            port,
            master_user,
            master_pw,
            master_db,
            "SELECT TABLE_NAME, COLUMN_NAME FROM information_schema.COLUMNS "
            f"WHERE TABLE_SCHEMA='{master_db}' AND COLUMN_NAME='is_unified'",
        )
    )
    print(
        mysql(
            host,
            port,
            master_user,
            master_pw,
            master_db,
            "SHOW TABLES LIKE 'oa_collect_task_account'",
        )
    )
    print("=== sys_param collect.schedule.cron ===")
    print(
        mysql(
            host,
            port,
            master_user,
            master_pw,
            master_db,
            "SELECT id, tenant_id, param_key, param_value FROM sys_param "
            "WHERE param_key='collect.schedule.cron' AND deleted=0",
        )
    )
    print("=== master system_menu oa: leftover ===")
    print(
        mysql(
            host,
            port,
            master_user,
            master_pw,
            master_db,
            "SELECT COUNT(*) FROM system_menu WHERE permission LIKE 'oa:%' AND deleted=0",
        )
    )
    print("=== shenyu-system system_menu oa: leftover ===")
    print(
        mysql(
            host,
            port,
            sys_user,
            sys_pw,
            sys_db,
            "SELECT COUNT(*) FROM system_menu WHERE permission LIKE 'oa:%' AND deleted=b'0'",
        )
    )
    print("=== shenyu-system sample ops: count ===")
    print(
        mysql(
            host,
            port,
            sys_user,
            sys_pw,
            sys_db,
            "SELECT COUNT(*) FROM system_menu WHERE permission LIKE 'ops:%' AND deleted=b'0'",
        )
    )


if __name__ == "__main__":
    main()
