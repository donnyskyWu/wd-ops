#!/usr/bin/env python3
"""Apply V177 on beta shenyu-ops when Flyway is disabled. Idempotent."""
from __future__ import annotations

import os
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ENV_FILE = ROOT / "scripts/integration-config/ops-test-remote.env"
V177_SQL = (
    ROOT
    / "football-backend-saas/football-module-ops/football-module-ops-server/src/main/resources/db/migration/V177__wechat_external_collect_cookie_param.sql"
)
PARAM_KEY = "collect.external.wechat_official.cookie"


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


def param_exists(host, port, user, password, db, param_key: str) -> bool:
    out = mysql_exec(
        host,
        port,
        user,
        password,
        db,
        f"SELECT COUNT(*) FROM sys_param WHERE tenant_id=1 AND param_key='{param_key}' AND deleted=0",
    ).strip()
    return out == "1"


def record_flyway_v177(host, port, user, password, database) -> None:
    mysql_exec(
        host,
        port,
        user,
        password,
        database,
        "INSERT INTO flyway_schema_history "
        "(installed_rank, version, description, type, script, checksum, installed_by, execution_time, success) "
        "SELECT COALESCE(MAX(installed_rank),0)+1, '177', 'wechat external collect cookie param', 'SQL', "
        "'V177__wechat_external_collect_cookie_param.sql', NULL, USER(), 0, 1 "
        "FROM flyway_schema_history "
        "WHERE NOT EXISTS (SELECT 1 FROM flyway_schema_history WHERE version='177' AND success=1)",
    )


def main() -> int:
    cfg = load_env()
    host = cfg.get("OPS_TEST_DB_HOST", "110.42.49.224")
    port = cfg.get("OPS_TEST_DB_PORT", "3306")
    user = cfg.get("OPS_TEST_MASTER_USER", "shenyu-ops")
    password = cfg.get("OPS_TEST_MASTER_PASSWORD", "")
    database = cfg.get("OPS_TEST_MASTER_DB", "shenyu-ops")

    if not V177_SQL.is_file():
        print(f"Missing {V177_SQL}", file=sys.stderr)
        return 1

    has_param = param_exists(host, port, user, password, database, PARAM_KEY)
    v177_row = mysql_exec(
        host,
        port,
        user,
        password,
        database,
        "SELECT COUNT(*) FROM flyway_schema_history WHERE version='177' AND success=1",
    ).strip()

    if not has_param:
        print(f"[apply] V177 SQL -> {host}/{database}")
        mysql_file(host, port, user, password, database, V177_SQL)
    else:
        print("[skip] wechat cookie param already present")

    if v177_row == "0":
        record_flyway_v177(host, port, user, password, database)
        print("[record] flyway V177 row inserted")

    has_param = param_exists(host, port, user, password, database, PARAM_KEY)
    print(f"param {PARAM_KEY}: {has_param}")
    return 0 if has_param else 2


if __name__ == "__main__":
    raise SystemExit(main())
