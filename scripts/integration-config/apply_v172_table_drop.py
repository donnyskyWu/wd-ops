#!/usr/bin/env python3
"""Apply V172 on beta shenyu-ops (Flyway disabled in beta overlay). Idempotent."""
from __future__ import annotations

import os
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ENV_FILE = ROOT / "scripts/integration-config/ops-test-remote.env"
V172_SQL = (
    ROOT
    / "football-backend-saas/football-module-ops/football-module-ops-server/src/main/resources/db/migration/V172__drop_archive_and_legacy_unused_tables.sql"
)

DROP_TARGETS = [
    "archive_sys_user",
    "archive_sys_user_token",
    "archive_sys_user_role",
    "archive_sys_role",
    "archive_sys_role_permission",
    "archive_sys_permission",
    "archive_sys_operation_log",
    "archive_sys_dict_type",
    "archive_sys_dict_data",
    "oa_demo_item",
    "oa_author",
    "system_users",
    "system_user_role",
    "system_role",
    "system_menu",
    "system_oauth2_access_token",
    "system_user_author",
    "system_user_data",
    "sys_audit_log",
    "sys_dept",
    "sys_login_log",
]


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


def main() -> int:
    cfg = load_env()
    host = cfg.get("OPS_TEST_DB_HOST", "110.42.49.224")
    port = cfg.get("OPS_TEST_DB_PORT", "3306")
    user = cfg.get("OPS_TEST_MASTER_USER", "shenyu-ops")
    password = cfg.get("OPS_TEST_MASTER_PASSWORD", "")
    database = cfg.get("OPS_TEST_MASTER_DB", "shenyu-ops")

    if not V172_SQL.is_file():
        print(f"Missing {V172_SQL}", file=sys.stderr)
        return 1

    before = {t: table_exists(host, port, user, password, database, t) for t in DROP_TARGETS}
    flyway_before = mysql_exec(
        host,
        port,
        user,
        password,
        database,
        "SELECT version, success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5",
    ).strip()

    still_before = [t for t in DROP_TARGETS if table_exists(host, port, user, password, database, t)]
    v172_row = mysql_exec(
        host,
        port,
        user,
        password,
        database,
        "SELECT COUNT(*) FROM flyway_schema_history WHERE version='172' AND success=1",
    ).strip()

    if still_before:
        print(f"[apply] V172 SQL -> {host}/{database} (tables still present: {len(still_before)})")
        mysql_file(host, port, user, password, database, V172_SQL)
        if v172_row == "0":
            mysql_exec(
                host,
                port,
                user,
                password,
                database,
                "INSERT INTO flyway_schema_history "
                "(installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) "
                "SELECT COALESCE(MAX(installed_rank),0)+1, '172', 'drop archive and legacy unused tables', 'SQL', "
                "'V172__drop_archive_and_legacy_unused_tables.sql', NULL, 'apply_v172_table_drop.py', NOW(), 0, 1 "
                "FROM flyway_schema_history "
                "WHERE NOT EXISTS (SELECT 1 FROM flyway_schema_history WHERE version='172')",
            )
    elif v172_row != "0":
        print("[skip] V172 flyway row exists and drop targets already gone")
    else:
        print("[record] tables already gone; inserting flyway V172 row only")
        mysql_exec(
            host,
            port,
            user,
            password,
            database,
            "INSERT INTO flyway_schema_history "
            "(installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) "
            "SELECT COALESCE(MAX(installed_rank),0)+1, '172', 'drop archive and legacy unused tables', 'SQL', "
            "'V172__drop_archive_and_legacy_unused_tables.sql', NULL, 'apply_v172_table_drop.py', NOW(), 0, 1 "
            "FROM flyway_schema_history "
            "WHERE NOT EXISTS (SELECT 1 FROM flyway_schema_history WHERE version='172')",
        )

    after = {t: table_exists(host, port, user, password, database, t) for t in DROP_TARGETS}
    meta_oa = mysql_exec(
        host,
        port,
        user,
        password,
        database,
        "SELECT COUNT(*) FROM sys_metadata_entity WHERE physical_table IN ('oa_author','oa_demo_item')",
    ).strip()

    still_present = [t for t, ex in after.items() if ex]
    print(f"flyway_before:\n{flyway_before or '(empty)'}")
    print(f"metadata_oa_author_demo: {meta_oa}")
    print(f"still_present: {still_present or 'none'}")
    if still_present:
        return 2
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
