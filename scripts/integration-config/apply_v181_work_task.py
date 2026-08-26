#!/usr/bin/env python3
"""Apply V181/V182 on shenyu-ops when Flyway is disabled or manual sync needed. Idempotent."""
from __future__ import annotations

import argparse
import os
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ENV_FILE = ROOT / "scripts/integration-config/ops-test-remote.env"
MIGRATION_DIR = (
    ROOT
    / "football-backend-saas/football-module-ops/football-module-ops-server/src/main/resources/db/migration"
)
V181_SQL = MIGRATION_DIR / "V181__m2_work_task_foundation.sql"
V182_SQL = MIGRATION_DIR / "V182__m2_work_task_default_params.sql"


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


def table_exists(host, port, user, password, db, name: str) -> bool:
    out = mysql_exec(
        host,
        port,
        user,
        password,
        db,
        f"SELECT COUNT(*) FROM information_schema.tables "
        f"WHERE table_schema='{db}' AND table_name='{name}'",
    ).strip()
    return out == "1"


def record_flyway(host, port, user, password, database, version: str, description: str, script: str) -> None:
    mysql_exec(
        host,
        port,
        user,
        password,
        database,
        "INSERT INTO flyway_schema_history "
        "(installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) "
        f"SELECT COALESCE(MAX(installed_rank),0)+1, '{version}', '{description}', 'SQL', "
        f"'{script}', NULL, 'apply_v181_work_task.py', NOW(), 0, 1 "
        "FROM flyway_schema_history "
        f"WHERE NOT EXISTS (SELECT 1 FROM flyway_schema_history WHERE version='{version}')",
    )


def main() -> int:
    parser = argparse.ArgumentParser(description="Apply V181/V182 work-task migrations")
    parser.add_argument("--target", choices=["local", "test"], default="local")
    args = parser.parse_args()

    cfg = load_env()
    if args.target == "local":
        host = "127.0.0.1"
        port = "3306"
        user = "root"
        password = "root"
        database = "shenyu-ops"
    else:
        host = cfg.get("OPS_TEST_DB_HOST", "110.42.49.224")
        port = cfg.get("OPS_TEST_DB_PORT", "3306")
        user = cfg.get("OPS_TEST_MASTER_USER", "shenyu-ops")
        password = cfg.get("OPS_TEST_MASTER_PASSWORD", "")
        database = cfg.get("OPS_TEST_MASTER_DB", "shenyu-ops")
        if not password:
            print("Missing OPS_TEST_MASTER_PASSWORD in ops-test-remote.env", file=sys.stderr)
            return 1

    for path in (V181_SQL, V182_SQL):
        if not path.is_file():
            print(f"Missing {path}", file=sys.stderr)
            return 1

    print(f"Target: {host}:{port}/{database} as {user}")

    if not table_exists(host, port, user, password, database, "oa_work_task_sheet"):
        print("Applying V181...")
        mysql_file(host, port, user, password, database, V181_SQL)
        record_flyway(host, port, user, password, database, "181", "m2 work task foundation", "V181__m2_work_task_foundation.sql")
        print("V181 applied.")
    else:
        print("V181 skip: oa_work_task_sheet already exists.")

    v182_row = mysql_exec(
        host,
        port,
        user,
        password,
        database,
        "SELECT COUNT(*) FROM flyway_schema_history WHERE version='182' AND success=1",
    ).strip()
    if v182_row == "0":
        print("Applying V182...")
        mysql_file(host, port, user, password, database, V182_SQL)
        record_flyway(host, port, user, password, database, "182", "m2 work task default params", "V182__m2_work_task_default_params.sql")
        print("V182 applied.")
    else:
        print("V182 skip: already recorded in flyway_schema_history.")

    menu_count = "n/a"
    if table_exists(host, port, user, password, database, "system_menu"):
        menu_count = mysql_exec(
            host,
            port,
            user,
            password,
            database,
            "SELECT COUNT(*) FROM system_menu WHERE id=6176",
        ).strip()
    sheet_count = mysql_exec(
        host,
        port,
        user,
        password,
        database,
        "SELECT COUNT(*) FROM oa_work_task_sheet",
    ).strip()
    print(f"Verify: system_menu 6176 rows={menu_count}, oa_work_task_sheet rows={sheet_count}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
