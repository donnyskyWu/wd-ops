#!/usr/bin/env python3
"""Apply V173 on beta shenyu-ops when Flyway is disabled. Idempotent."""
from __future__ import annotations

import os
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ENV_FILE = ROOT / "scripts/integration-config/ops-test-remote.env"
V173_SQL = (
    ROOT
    / "football-backend-saas/football-module-ops/football-module-ops-server/src/main/resources/db/migration/V173__m10_live_collect_douyin_wechat_video.sql"
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


def record_flyway_v173(host, port, user, password, database) -> None:
    mysql_exec(
        host,
        port,
        user,
        password,
        database,
        "INSERT INTO flyway_schema_history "
        "(installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) "
        "SELECT COALESCE(MAX(installed_rank),0)+1, '173', 'm10 live collect douyin wechat video', 'SQL', "
        "'V173__m10_live_collect_douyin_wechat_video.sql', NULL, 'apply_v173_live_collect.py', NOW(), 0, 1 "
        "FROM flyway_schema_history "
        "WHERE NOT EXISTS (SELECT 1 FROM flyway_schema_history WHERE version='173')",
    )


def main() -> int:
    cfg = load_env()
    host = cfg.get("OPS_TEST_DB_HOST", "110.42.49.224")
    port = cfg.get("OPS_TEST_DB_PORT", "3306")
    user = cfg.get("OPS_TEST_MASTER_USER", "shenyu-ops")
    password = cfg.get("OPS_TEST_MASTER_PASSWORD", "")
    database = cfg.get("OPS_TEST_MASTER_DB", "shenyu-ops")

    if not V173_SQL.is_file():
        print(f"Missing {V173_SQL}", file=sys.stderr)
        return 1

    has_col = column_exists(host, port, user, password, database, "oa_account", "collect_live_enabled")
    has_dy = table_exists(host, port, user, password, database, "oa_douyin_live")
    has_wv = table_exists(host, port, user, password, database, "oa_wechat_video_live")
    v173_row = mysql_exec(
        host,
        port,
        user,
        password,
        database,
        "SELECT COUNT(*) FROM flyway_schema_history WHERE version='173' AND success=1",
    ).strip()

    if not has_col or not has_dy or not has_wv:
        print(f"[apply] V173 SQL -> {host}/{database}")
        mysql_file(host, port, user, password, database, V173_SQL)
    else:
        print("[skip] V173 schema already present")

    if v173_row == "0":
        record_flyway_v173(host, port, user, password, database)
        print("[record] flyway V173 row inserted")

    has_col = column_exists(host, port, user, password, database, "oa_account", "collect_live_enabled")
    has_dy = table_exists(host, port, user, password, database, "oa_douyin_live")
    has_wv = table_exists(host, port, user, password, database, "oa_wechat_video_live")
    print(f"collect_live_enabled: {has_col}, oa_douyin_live: {has_dy}, oa_wechat_video_live: {has_wv}")
    if not (has_col and has_dy and has_wv):
        return 2
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
