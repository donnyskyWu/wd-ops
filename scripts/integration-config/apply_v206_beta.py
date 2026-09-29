#!/usr/bin/env python3
"""Idempotent apply V206 on beta test DB (shenyu-ops) + flyway_schema_history row."""
from __future__ import annotations

import os
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ENV_FILE = ROOT / "scripts/integration-config/ops-test-remote.env"
V206_SQL = (
    ROOT
    / "football-backend-saas/football-module-ops/football-module-ops-server/src/main/resources/db/migration/V206__oa_account_short_video_live_operator.sql"
)


def load_env() -> dict[str, str]:
    env: dict[str, str] = {}
    for line in ENV_FILE.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        k, v = line.split("=", 1)
        env[k.strip()] = v.strip().strip('"').strip("'")
    return env


def mysql_run(cfg: dict[str, str], sql: str) -> str:
    e = os.environ.copy()
    e["MYSQL_PWD"] = cfg["pwd"]
    p = subprocess.run(
        [
            "mysql",
            "-h",
            cfg["host"],
            "-P",
            cfg["port"],
            "-u",
            cfg["user"],
            "--default-character-set=utf8mb4",
            "-N",
            "-B",
            cfg["db"],
        ],
        input=sql,
        capture_output=True,
        text=True,
        encoding="utf-8",
        env=e,
        check=False,
    )
    if p.returncode != 0:
        print(p.stderr or p.stdout, file=sys.stderr)
        sys.exit(p.returncode)
    return p.stdout.strip()


def column_exists(cfg: dict[str, str], column: str) -> bool:
    out = mysql_run(
        cfg,
        f"""
SELECT COUNT(*) FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = '{cfg["db"]}' AND TABLE_NAME = 'oa_account' AND COLUMN_NAME = '{column}';
""",
    )
    return out == "1"


def main() -> None:
    raw = load_env()
    cfg = {
        "host": raw["OPS_TEST_DB_HOST"],
        "port": raw["OPS_TEST_DB_PORT"],
        "user": raw["OPS_TEST_MASTER_USER"],
        "pwd": raw["OPS_TEST_MASTER_PASSWORD"],
        "db": raw["OPS_TEST_MASTER_DB"],
    }
    needed = ("short_video_status", "live_status", "operator_user_id")
    missing = [c for c in needed if not column_exists(cfg, c)]
    if missing:
        ddl = V206_SQL.read_text(encoding="utf-8")
        mysql_run(cfg, ddl)
        print("Applied V206 DDL for columns:", ", ".join(missing))
    else:
        print("V206 columns already present; skipped DDL")

    hist = mysql_run(
        cfg,
        "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '206' AND success = 1;",
    )
    if hist == "0":
        mysql_run(
            cfg,
            """
INSERT INTO flyway_schema_history
  (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
SELECT COALESCE(MAX(installed_rank), 0) + 1,
       '206',
       'oa account short video live operator',
       'SQL',
       'V206__oa_account_short_video_live_operator.sql',
       NULL,
       USER(),
       NOW(),
       0,
       1
  FROM flyway_schema_history;
""",
        )
        print("Recorded flyway_schema_history version 206")
    else:
        print("flyway_schema_history already has version 206")

    verify = mysql_run(
        cfg,
        """
SELECT COLUMN_NAME FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_account'
   AND COLUMN_NAME IN ('short_video_status','live_status','operator_user_id','holder_user_id')
 ORDER BY COLUMN_NAME;
""",
    )
    print("Verified columns:\n" + verify.replace("\t", " "))


if __name__ == "__main__":
    main()
