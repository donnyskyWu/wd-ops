#!/usr/bin/env python3
"""Apply V194 (ADR-075 execution merge group) on beta when Flyway is disabled. Idempotent."""
from __future__ import annotations

import os
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ENV_FILE = ROOT / "scripts/integration-config/ops-test-remote.env"
V194_SQL = (
    ROOT
    / "football-backend-saas/football-module-ops/football-module-ops-server/src/main/resources/db/migration/V194__m2_work_task_execution_merge_group.sql"
)


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
        err = proc.stderr.decode("utf-8", errors="replace")
        print(err, file=sys.stderr)
        sys.exit(proc.returncode)
    return proc.stdout.decode("utf-8", errors="replace")


def mysql_file(host: str, port: str, user: str, password: str, database: str, path: Path) -> None:
    env = os.environ.copy()
    env["MYSQL_PWD"] = password
    proc = subprocess.run(
        [
            "mysql",
            f"-h{host}",
            f"-P{port}",
            f"-u{user}",
            "--default-character-set=utf8mb4",
            database,
        ],
        input=path.read_text(encoding="utf-8").encode("utf-8"),
        capture_output=True,
        env=env,
    )
    if proc.returncode != 0:
        print(proc.stderr.decode("utf-8", errors="replace"), file=sys.stderr)
        sys.exit(proc.returncode)


def column_exists(host, port, user, password, database, table, column) -> bool:
    out = mysql_exec(
        host,
        port,
        user,
        password,
        database,
        f"SELECT COUNT(*) FROM information_schema.COLUMNS "
        f"WHERE TABLE_SCHEMA='{database}' AND TABLE_NAME='{table}' AND COLUMN_NAME='{column}'",
    )
    return out.strip() == "1"


def flyway_recorded(host, port, user, password, database, version) -> bool:
    out = mysql_exec(
        host,
        port,
        user,
        password,
        database,
        f"SELECT COUNT(*) FROM flyway_schema_history WHERE version='{version}'",
    )
    return out.strip() != "0"


def record_flyway(host, port, user, password, database, version, description, script) -> None:
    mysql_exec(
        host,
        port,
        user,
        password,
        database,
        "INSERT INTO flyway_schema_history "
        "(installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) "
        f"SELECT COALESCE(MAX(installed_rank),0)+1, '{version}', '{description}', 'SQL', "
        f"'{script}', NULL, 'apply_v194_work_task_merge.py', NOW(), 0, 1 "
        "FROM flyway_schema_history "
        f"WHERE NOT EXISTS (SELECT 1 FROM flyway_schema_history WHERE version='{version}')",
    )


def main() -> None:
    cfg = load_env()
    host = cfg.get("OPS_TEST_DB_HOST", "110.42.49.224")
    port = cfg.get("OPS_TEST_DB_PORT", "3306")
    user = cfg.get("OPS_TEST_MASTER_USER", "shenyu-ops")
    password = cfg.get("OPS_TEST_MASTER_PASSWORD", "")
    ops_db = cfg.get("OPS_TEST_MASTER_DB", "shenyu-ops")

    if not V194_SQL.is_file():
        print(f"Missing {V194_SQL}", file=sys.stderr)
        sys.exit(1)

    if column_exists(host, port, user, password, ops_db, "oa_work_task_assignment", "execution_group_id"):
        print(f"[skip] {ops_db}.oa_work_task_assignment.execution_group_id already exists")
    else:
        print(f"[apply] V194 -> {host}/{ops_db}")
        mysql_file(host, port, user, password, ops_db, V194_SQL)

    if flyway_recorded(host, port, user, password, ops_db, "194"):
        print("[skip] flyway V194 already recorded")
    else:
        record_flyway(
            host,
            port,
            user,
            password,
            ops_db,
            "194",
            "m2 work task execution merge group",
            "V194__m2_work_task_execution_merge_group.sql",
        )
    check = mysql_exec(
        host,
        port,
        user,
        password,
        ops_db,
        "SELECT COUNT(*) FROM information_schema.COLUMNS "
        "WHERE TABLE_SCHEMA='shenyu-ops' AND TABLE_NAME='oa_work_task_assignment' "
        "AND COLUMN_NAME='execution_group_id'",
    )
    print(f"V194 check: execution_group_id={check.strip() == '1'}")


if __name__ == "__main__":
    main()
