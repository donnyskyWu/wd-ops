#!/usr/bin/env python3
from __future__ import annotations

import os
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]
ENV_FILE = ROOT / "scripts/integration-config/ops-test-remote.env"


def load_env() -> dict[str, str]:
    cfg: dict[str, str] = {}
    for line in ENV_FILE.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        k, v = line.split("=", 1)
        cfg[k.strip()] = v.strip().strip('"').strip("'")
    return cfg


def mysql(host, port, user, pw, db, sql: str) -> str:
    env = os.environ.copy()
    env["MYSQL_PWD"] = pw
    p = subprocess.run(
        [
            "mysql",
            f"-h{host}",
            f"-P{port}",
            f"-u{user}",
            "--default-character-set=utf8mb4",
            "-N",
            "-B",
            db,
            "-e",
            sql,
        ],
        capture_output=True,
        env=env,
    )
    out = p.stdout.decode("utf-8", errors="replace")
    err = p.stderr.decode("utf-8", errors="replace")
    if p.returncode != 0:
        return f"ERR: {err.strip()}"
    return out.strip()


def main() -> None:
    # local
    env = os.environ.copy()
    env["MYSQL_PWD"] = "root"
    for sql, label in [
        (
            "SELECT version, checksum, success FROM flyway_schema_history "
            "WHERE version IN ('166','167')",
            "LOCAL flyway 166/167",
        ),
        (
            "SHOW COLUMNS FROM oa_collect_task LIKE 'credential_profile'",
            "LOCAL credential_profile",
        ),
    ]:
        p = subprocess.run(
            [
                "mysql",
                "-h127.0.0.1",
                "-P3306",
                "-uroot",
                "-N",
                "-B",
                "shenyu-ops",
                "-e",
                sql,
            ],
            capture_output=True,
            env=env,
        )
        print(label + ":", p.stdout.decode("utf-8", errors="replace").strip() or p.stderr.decode("utf-8", errors="replace")[:200])

    cfg = load_env()
    host = cfg["OPS_TEST_DB_HOST"]
    port = cfg.get("OPS_TEST_DB_PORT", "3306")
    u = cfg["OPS_TEST_MASTER_USER"]
    pw = cfg["OPS_TEST_MASTER_PASSWORD"]
    db = cfg["OPS_TEST_MASTER_DB"]
    checks = [
        ("credential_profile", "SHOW COLUMNS FROM oa_collect_task LIKE 'credential_profile'"),
        ("publish_enabled", "SHOW COLUMNS FROM oa_account LIKE 'publish_enabled'"),
        ("usage_status", "SHOW COLUMNS FROM oa_account_ext LIKE 'usage_status'"),
        ("max_version", "SELECT MAX(CAST(version AS UNSIGNED)) FROM flyway_schema_history WHERE version REGEXP '^[0-9]+$'"),
        ("system_menu_exists", "SHOW TABLES LIKE 'system_menu'"),
    ]
    print(f"REMOTE {host}/{db}")
    for label, sql in checks:
        print(f"  {label}:", mysql(host, port, u, pw, db, sql))


if __name__ == "__main__":
    main()
