#!/usr/bin/env python3
"""Idempotent: add DINGTALK (+ CONTENT_REVIEW if missing) to shenyu-system.system_dict_data.

For beta remote where ops-server Flyway cannot cross-DB INSERT into shenyu-system.
Loads ops-test-remote.env from repo root.
"""
from __future__ import annotations

import os
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ENV_FILE = ROOT / "scripts/integration-config/ops-test-remote.env"

ROWS = [
    (5, "钉钉配置", "DINGTALK"),
    (6, "内容审核", "CONTENT_REVIEW"),
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


def mysql_exec(host: str, port: str, user: str, password: str, database: str, sql: str) -> None:
    env = os.environ.copy()
    env["MYSQL_PWD"] = password
    cmd = [
        "mysql",
        f"-h{host}",
        f"-P{port}",
        f"-u{user}",
        "--default-character-set=utf8mb4",
        database,
    ]
    proc = subprocess.run(cmd, input=sql.encode("utf-8"), capture_output=True, env=env)
    if proc.returncode != 0:
        print(proc.stderr.decode("utf-8", errors="replace"), file=sys.stderr)
        sys.exit(proc.returncode)


def esc(val: str) -> str:
    return "'" + val.replace("\\", "\\\\").replace("'", "''") + "'"


def main() -> int:
    cfg = load_env()
    host = cfg.get("OPS_TEST_DB_HOST", "110.42.49.224")
    port = cfg.get("OPS_TEST_DB_PORT", "3306")
    sys_db = cfg.get("OPS_TEST_SYSTEM_DB", "shenyu-system")
    sys_user = cfg.get("OPS_TEST_SYSTEM_USER", "shenyu-system")
    sys_pass = cfg.get("OPS_TEST_SYSTEM_PASSWORD", "")

    parts = ["SET NAMES utf8mb4;"]
    for sort, label, value in ROWS:
        parts.append(
            "INSERT INTO system_dict_data (sort, label, value, dict_type, status, "
            "color_type, css_class, remark, creator, create_time, updater, update_time, deleted) "
            f"SELECT {sort}, {esc(label)}, {esc(value)}, 'dict_param_category', 0, "
            "'default', '', NULL, 'apply-v171-param-category', NOW(), 'apply-v171-param-category', NOW(), b'0' "
            "FROM DUAL WHERE NOT EXISTS ("
            "SELECT 1 FROM system_dict_data sd "
            "WHERE sd.dict_type='dict_param_category' AND sd.value=" + esc(value) + " AND sd.deleted=b'0'"
            ");"
        )
    sql = "\n".join(parts)
    mysql_exec(host, port, sys_user, sys_pass, sys_db, sql)
    print(f"apply_v171_param_category: synced dict_param_category -> {host}/{sys_db}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
