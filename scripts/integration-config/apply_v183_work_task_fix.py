#!/usr/bin/env python3
"""Apply V183 work-task menu (6179-6181) + dict SSOT to shenyu-system. Idempotent.

Usage:
  python scripts/integration-config/apply_v183_work_task_fix.py --target local
  python scripts/integration-config/apply_v183_work_task_fix.py --target test

Local uses root@127.0.0.1 / shenyu-sys (fallback shenyu-system).
Test uses ops-test-remote.env credentials.
Also records flyway V183 on shenyu-ops when table exists.
"""
from __future__ import annotations

import argparse
import os
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ENV_FILE = ROOT / "scripts/integration-config/ops-test-remote.env"
V183_SQL = (
    ROOT
    / "football-backend-saas/football-module-ops/football-module-ops-server/src/main/resources/db/migration/V183__m2_work_task_menu_dict_fix.sql"
)


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


def record_flyway(host, port, user, password, database) -> None:
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
        "SELECT COALESCE(MAX(installed_rank),0)+1, '183', 'm2 work task menu dict fix', 'SQL', "
        "'V183__m2_work_task_menu_dict_fix.sql', NULL, 'apply_v183_work_task_fix.py', NOW(), 0, 1 "
        "FROM flyway_schema_history "
        "WHERE NOT EXISTS (SELECT 1 FROM flyway_schema_history WHERE version='183')",
    )


def resolve_local_system_db(host, port, user, password) -> str:
    # SSOT: football-integration-overlay.yml → localhost master = shenyu-sys (since 2026-08-18)
    for db in ("shenyu-sys", "shenyu-system"):
        if table_exists(host, port, user, password, db, "system_menu"):
            return db
    return "shenyu-sys"


def main() -> int:
    parser = argparse.ArgumentParser(description="Apply V183 work-task menu+dict fix")
    parser.add_argument("--target", choices=["local", "test"], default="local")
    args = parser.parse_args()

    if not V183_SQL.is_file():
        print(f"Missing {V183_SQL}", file=sys.stderr)
        return 1

    cfg = load_env()
    if args.target == "local":
        host, port = "127.0.0.1", "3306"
        ops_user, ops_pwd = "root", "root"
        ops_db = "shenyu-ops"
        sys_user, sys_pwd = "root", "root"
        sys_db = resolve_local_system_db(host, port, sys_user, sys_pwd)
    else:
        host = cfg.get("OPS_TEST_DB_HOST", "110.42.49.224")
        port = cfg.get("OPS_TEST_DB_PORT", "3306")
        ops_user = cfg.get("OPS_TEST_MASTER_USER", "shenyu-ops")
        ops_pwd = cfg.get("OPS_TEST_MASTER_PASSWORD", "")
        ops_db = cfg.get("OPS_TEST_MASTER_DB", "shenyu-ops")
        sys_db = cfg.get("OPS_TEST_SYSTEM_DB", "shenyu-system")
        sys_user = cfg.get("OPS_TEST_SYSTEM_USER", "shenyu-system")
        sys_pwd = cfg.get("OPS_TEST_SYSTEM_PASSWORD", "")
        if not ops_pwd or not sys_pwd:
            print("Missing OPS_TEST_* passwords in ops-test-remote.env", file=sys.stderr)
            return 1

    print(f"Target: {host}:{port} ops={ops_db} system={sys_db}")

    # V183 uses fully-qualified `shenyu-system`.table — rewrite for local db name if different
    sql = V183_SQL.read_text(encoding="utf-8")
    if sys_db != "shenyu-system":
        sql = sql.replace("`shenyu-system`.", f"`{sys_db}`.")

    tmp = ROOT / "scripts/integration-config/.tmp_v183_apply.sql"
    tmp.write_text(sql, encoding="utf-8")
    mysql_file(host, port, sys_user, sys_pwd, sys_db, tmp)
    tmp.unlink(missing_ok=True)
    print("V183 menu+dict applied on system DB.")

    record_flyway(host, port, ops_user, ops_pwd, ops_db)

    menu = mysql_exec(
        host,
        port,
        sys_user,
        sys_pwd,
        sys_db,
        "SELECT COUNT(*) FROM system_menu WHERE id IN (6194,6195,6196) AND deleted=b'0'",
    ).strip()
    perm = mysql_exec(
        host,
        port,
        sys_user,
        sys_pwd,
        sys_db,
        "SELECT COUNT(*) FROM system_menu WHERE permission LIKE 'ops:work-task:%' AND deleted=b'0'",
    ).strip()
    dict_types = mysql_exec(
        host,
        port,
        sys_user,
        sys_pwd,
        sys_db,
        "SELECT COUNT(*) FROM system_dict_type WHERE type IN "
        "('dict_marketing_plan_type','dict_sales_platform','dict_win_prediction','dict_work_task_sheet_status') "
        "AND deleted=b'0'",
    ).strip()
    dict_data = mysql_exec(
        host,
        port,
        sys_user,
        sys_pwd,
        sys_db,
        "SELECT COUNT(*) FROM system_dict_data WHERE dict_type IN "
        "('dict_marketing_plan_type','dict_sales_platform','dict_win_prediction','dict_work_task_sheet_status') "
        "AND deleted=b'0'",
    ).strip()
    print(f"Verify: menus 6194-6196={menu}, ops:work-task perms={perm}, dict_types={dict_types}, dict_data={dict_data}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
