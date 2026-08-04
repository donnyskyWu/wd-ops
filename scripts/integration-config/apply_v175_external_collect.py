#!/usr/bin/env python3
"""Apply V175 on beta shenyu-ops when Flyway is disabled. Idempotent."""
from __future__ import annotations

import os
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ENV_FILE = ROOT / "scripts/integration-config/ops-test-remote.env"
V175_SQL = (
    ROOT
    / "football-backend-saas/football-module-ops/football-module-ops-server/src/main/resources/db/migration/V175__m10_external_unified_collect_task.sql"
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


def param_exists(host, port, user, password, database, key: str) -> bool:
    out = mysql_exec(
        host,
        port,
        user,
        password,
        database,
        f"SELECT COUNT(*) FROM sys_param WHERE tenant_id=1 AND param_key='{key}' AND deleted=0",
    ).strip()
    return out != "0"


def record_flyway_v175(host, port, user, password, database) -> None:
    mysql_exec(
        host,
        port,
        user,
        password,
        database,
        "INSERT INTO flyway_schema_history "
        "(installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) "
        "SELECT COALESCE(MAX(installed_rank),0)+1, '175', 'm10 external unified collect task', 'SQL', "
        "'V175__m10_external_unified_collect_task.sql', NULL, 'apply_v175_external_collect.py', NOW(), 0, 1 "
        "FROM flyway_schema_history "
        "WHERE NOT EXISTS (SELECT 1 FROM flyway_schema_history WHERE version='175')",
    )


def main() -> int:
    cfg = load_env()
    host = cfg.get("OPS_TEST_DB_HOST", "110.42.49.224")
    port = cfg.get("OPS_TEST_DB_PORT", "3306")
    user = cfg.get("OPS_TEST_MASTER_USER", "shenyu-ops")
    password = cfg.get("OPS_TEST_MASTER_PASSWORD", "")
    database = cfg.get("OPS_TEST_MASTER_DB", "shenyu-ops")

    if not V175_SQL.is_file():
        print(f"Missing {V175_SQL}", file=sys.stderr)
        return 1

    has_cfg_col = column_exists(host, port, user, password, database, "oa_collect_config", "collect_enabled")
    has_kw_col = column_exists(host, port, user, password, database, "oa_config_keyword", "collect_enabled")
    has_task_cfg = table_exists(host, port, user, password, database, "oa_collect_task_config")
    has_task_kw = table_exists(host, port, user, password, database, "oa_collect_task_keyword")
    has_ext_kw_col = column_exists(host, port, user, password, database, "oa_external_work", "keyword_config_id")
    has_cron_param = param_exists(host, port, user, password, database, "collect.external.unified.cron")
    v175_row = mysql_exec(
        host,
        port,
        user,
        password,
        database,
        "SELECT COUNT(*) FROM flyway_schema_history WHERE version='175' AND success=1",
    ).strip()

    schema_ok = has_cfg_col and has_kw_col and has_task_cfg and has_task_kw and has_ext_kw_col and has_cron_param

    if not schema_ok:
        print(f"[apply] V175 SQL -> {host}/{database}")
        mysql_file(host, port, user, password, database, V175_SQL)
    else:
        print("[skip] V175 schema already present")

    if v175_row == "0":
        record_flyway_v175(host, port, user, password, database)
        print("[record] flyway V175 row inserted")

    has_cfg_col = column_exists(host, port, user, password, database, "oa_collect_config", "collect_enabled")
    has_kw_col = column_exists(host, port, user, password, database, "oa_config_keyword", "collect_enabled")
    has_task_cfg = table_exists(host, port, user, password, database, "oa_collect_task_config")
    has_task_kw = table_exists(host, port, user, password, database, "oa_collect_task_keyword")
    has_ext_kw_col = column_exists(host, port, user, password, database, "oa_external_work", "keyword_config_id")
    has_cron_param = param_exists(host, port, user, password, database, "collect.external.unified.cron")
    print(
        f"collect_enabled(cfg/kw): {has_cfg_col}/{has_kw_col}, "
        f"oa_collect_task_config: {has_task_cfg}, oa_collect_task_keyword: {has_task_kw}, "
        f"keyword_config_id: {has_ext_kw_col}, cron param: {has_cron_param}"
    )
    if not (has_cfg_col and has_kw_col and has_task_cfg and has_task_kw and has_ext_kw_col and has_cron_param):
        return 2
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
