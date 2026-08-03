#!/usr/bin/env python3
"""One-off: check dict_param_category in beta DB."""
import subprocess
import os
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]
ENV_FILE = ROOT / "scripts/integration-config/ops-test-remote.env"


def load_env():
    env = {}
    for line in ENV_FILE.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        k, v = line.split("=", 1)
        env[k.strip()] = v.strip().strip('"').strip("'")
    return env


def q(host, port, db, user, pwd, sql):
    e = os.environ.copy()
    e["MYSQL_PWD"] = pwd
    r = subprocess.run(
        ["mysql", f"-h{host}", f"-P{port}", f"-u{user}", "--default-character-set=utf8mb4", "-N", "-B", db, "-e", sql],
        capture_output=True,
        env=e,
    )
    out = r.stdout.decode("utf-8", errors="replace")
    err = r.stderr.decode("utf-8", errors="replace")
    if r.returncode:
        print(f"ERR ({db}): {err}")
    return out


def main():
    cfg = load_env()
    host = cfg["OPS_TEST_DB_HOST"]
    port = cfg["OPS_TEST_DB_PORT"]
    print("=== shenyu-ops sys_dict_data dict_param_category ===")
    print(
        q(
            host,
            port,
            cfg["OPS_TEST_MASTER_DB"],
            cfg["OPS_TEST_MASTER_USER"],
            cfg["OPS_TEST_MASTER_PASSWORD"],
            "SELECT dict_type, label, dict_value, status FROM sys_dict_data WHERE dict_type='dict_param_category' AND deleted=0 ORDER BY sort",
        )
    )
    print("=== shenyu-system system_dict_data dict_param_category ===")
    print(
        q(
            host,
            port,
            cfg["OPS_TEST_SYSTEM_DB"],
            cfg["OPS_TEST_SYSTEM_USER"],
            cfg["OPS_TEST_SYSTEM_PASSWORD"],
            "SELECT dict_type, label, value, status FROM system_dict_data WHERE dict_type='dict_param_category' AND deleted=0 ORDER BY sort",
        )
    )
    print("=== flyway V170/V171 ===")
    print(
        q(
            host,
            port,
            cfg["OPS_TEST_MASTER_DB"],
            cfg["OPS_TEST_MASTER_USER"],
            cfg["OPS_TEST_MASTER_PASSWORD"],
            "SELECT version, description, success FROM flyway_schema_history WHERE version IN ('170','171') ORDER BY version",
        )
    )
    print("=== ops dict tables ===")
    print(
        q(
            host,
            port,
            cfg["OPS_TEST_MASTER_DB"],
            cfg["OPS_TEST_MASTER_USER"],
            cfg["OPS_TEST_MASTER_PASSWORD"],
            "SHOW TABLES LIKE '%dict%'",
        )
    )
    print("=== flyway last 8 ===")
    print(
        q(
            host,
            port,
            cfg["OPS_TEST_MASTER_DB"],
            cfg["OPS_TEST_MASTER_USER"],
            cfg["OPS_TEST_MASTER_PASSWORD"],
            "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 8",
        )
    )
    print("=== dingtalk params ===")
    print(
        q(
            host,
            port,
            cfg["OPS_TEST_MASTER_DB"],
            cfg["OPS_TEST_MASTER_USER"],
            cfg["OPS_TEST_MASTER_PASSWORD"],
            "SELECT id, param_key, category FROM sys_param WHERE param_key LIKE 'dingtalk.%' AND deleted=0",
        )
    )
    print("=== DINGTALK in system dict ===")
    print(
        q(
            host,
            port,
            cfg["OPS_TEST_SYSTEM_DB"],
            cfg["OPS_TEST_SYSTEM_USER"],
            cfg["OPS_TEST_SYSTEM_PASSWORD"],
            "SELECT id, label, value FROM system_dict_data WHERE dict_type='dict_param_category' AND value='DINGTALK' AND deleted=0",
        )
    )


if __name__ == "__main__":
    main()
