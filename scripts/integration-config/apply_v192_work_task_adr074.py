#!/usr/bin/env python3
"""Apply V192/V193 (ADR-074 work-task) on beta when Flyway is disabled. Idempotent."""
from __future__ import annotations

import os
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ENV_FILE = ROOT / "scripts/integration-config/ops-test-remote.env"
V192_SQL = (
    ROOT
    / "football-backend-saas/football-module-ops/football-module-ops-server/src/main/resources/db/migration/V192__m2_work_task_adr074_schema.sql"
)
V193_SQL = (
    ROOT
    / "football-backend-saas/football-module-ops/football-module-ops-server/src/main/resources/db/migration/V193__m2_work_task_marketing_plan_dict_v074.sql"
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


def column_exists(host, port, user, password, db, table: str, column: str) -> bool:
    out = mysql_exec(
        host,
        port,
        user,
        password,
        db,
        f"SELECT COUNT(*) FROM information_schema.columns "
        f"WHERE table_schema='{db}' AND table_name='{table}' AND column_name='{column}'",
    ).strip()
    return out == "1"


def record_flyway(host, port, user, password, database, version: str, description: str, script: str) -> None:
    if not table_exists(host, port, user, password, database, "flyway_schema_history"):
        print(f"Skip flyway record: no flyway_schema_history on {database}")
        return
    mysql_exec(
        host,
        port,
        user,
        password,
        database,
        "INSERT INTO flyway_schema_history "
        "(installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) "
        f"SELECT COALESCE(MAX(installed_rank),0)+1, '{version}', '{description}', 'SQL', "
        f"'{script}', NULL, 'apply_v192_work_task_adr074.py', NOW(), 0, 1 "
        "FROM flyway_schema_history "
        f"WHERE NOT EXISTS (SELECT 1 FROM flyway_schema_history WHERE version='{version}')",
    )


def main() -> int:
    cfg = load_env()
    host = cfg.get("OPS_TEST_DB_HOST", "110.42.49.224")
    port = cfg.get("OPS_TEST_DB_PORT", "3306")
    master_user = cfg.get("OPS_TEST_MASTER_USER", "shenyu-ops")
    master_pwd = cfg.get("OPS_TEST_MASTER_PASSWORD", "")
    master_db = cfg.get("OPS_TEST_MASTER_DB", "shenyu-ops")
    system_user = cfg.get("OPS_TEST_SYSTEM_USER", "shenyu-system")
    system_pwd = cfg.get("OPS_TEST_SYSTEM_PASSWORD", master_pwd)
    system_db = cfg.get("OPS_TEST_SYSTEM_DB", "shenyu-system")

    if not V192_SQL.is_file() or not V193_SQL.is_file():
        print("Missing V192/V193 SQL files", file=sys.stderr)
        return 1

    needs_v192 = not table_exists(host, port, master_user, master_pwd, master_db, "oa_work_task_assignment_task")
    if needs_v192:
        print(f"[apply] V192 -> {host}/{master_db}")
        mysql_file(host, port, master_user, master_pwd, master_db, V192_SQL)
    else:
        print("[skip] oa_work_task_assignment_task already exists")

    v192_row = mysql_exec(
        host,
        port,
        master_user,
        master_pwd,
        master_db,
        "SELECT COUNT(*) FROM flyway_schema_history WHERE version='192' AND success=1",
    ).strip()
    if v192_row == "0":
        record_flyway(host, port, master_user, master_pwd, master_db, "192", "m2 work task adr074 schema", "V192__m2_work_task_adr074_schema.sql")
        print("[record] flyway V192 on master")

    dict_hit = mysql_exec(
        host,
        port,
        system_user,
        system_pwd,
        system_db,
        "SELECT COUNT(*) FROM system_dict_data WHERE dict_type='dict_marketing_plan_type' "
        "AND value='KUAISHOU_PAID_COURSE' AND deleted=b'0'",
    ).strip()
    if dict_hit == "0":
        print(f"[apply] V193 dict -> {host}/{system_db}")
        mysql_file(host, port, system_user, system_pwd, system_db, V193_SQL)
    else:
        print("[skip] dict_marketing_plan_type v074 values present")

    v193_row = mysql_exec(
        host,
        port,
        master_user,
        master_pwd,
        master_db,
        "SELECT COUNT(*) FROM flyway_schema_history WHERE version='193' AND success=1",
    ).strip()
    if v193_row == "0":
        record_flyway(host, port, master_user, master_pwd, master_db, "193", "marketing plan dict v074", "V193__m2_work_task_marketing_plan_dict_v074.sql")
        print("[record] flyway V193 on master (cross-db script marker)")

    ok_col = column_exists(host, port, master_user, master_pwd, master_db, "oa_sop_template", "marketing_plan")
    ok_junc = table_exists(host, port, master_user, master_pwd, master_db, "oa_work_task_assignment_task")
    print(f"V192 check: marketing_plan={ok_col}, assignment_task={ok_junc}")
    if not ok_col or not ok_junc:
        return 2
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
