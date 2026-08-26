#!/usr/bin/env python3
"""Patch work_task sys_param defaults on local/test shenyu-ops."""
from __future__ import annotations

import argparse
import os
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ENV_FILE = ROOT / "scripts/integration-config/ops-test-remote.env"
PATCH_SQL = """
UPDATE sys_param SET param_value='9402', updater='patch_v182' 
WHERE tenant_id=1 AND param_key='work_task.default_template_id' AND deleted=0;
UPDATE sys_param SET param_value='9404', updater='patch_v182' 
WHERE tenant_id=1 AND param_key='work_task.default_node_id' AND deleted=0;
INSERT INTO sys_param (tenant_id, param_name, param_key, param_value, param_type, category, remark, creator, updater)
SELECT 1, '工作任务默认 SOP 模板 ID', 'work_task.default_template_id', '9402', 'STRING', 'WORK_TASK', 'S-17 patch', 'patch', 'patch'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE tenant_id=1 AND param_key='work_task.default_template_id' AND deleted=0);
INSERT INTO sys_param (tenant_id, param_name, param_key, param_value, param_type, category, remark, creator, updater)
SELECT 1, '工作任务默认 SOP 节点 ID', 'work_task.default_node_id', '9404', 'STRING', 'WORK_TASK', 'S-17 patch', 'patch', 'patch'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE tenant_id=1 AND param_key='work_task.default_node_id' AND deleted=0);
"""


def load_env() -> dict[str, str]:
    env: dict[str, str] = {}
    if ENV_FILE.is_file():
        for line in ENV_FILE.read_text(encoding="utf-8").splitlines():
            line = line.strip()
            if line and not line.startswith("#") and "=" in line:
                k, v = line.split("=", 1)
                env[k.strip()] = v.strip().strip('"').strip("'")
    return env


def mysql(host, port, user, password, database, sql) -> int:
    env = os.environ.copy()
    env["MYSQL_PWD"] = password
    proc = subprocess.run(
        ["mysql", f"-h{host}", f"-P{port}", f"-u{user}", "--default-character-set=utf8mb4", database],
        input=sql.encode("utf-8"),
        capture_output=True,
        env=env,
    )
    if proc.returncode != 0:
        print(proc.stderr.decode("utf-8", errors="replace"), file=sys.stderr)
    return proc.returncode


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--target", choices=["local", "test"], default="local")
    args = parser.parse_args()
    cfg = load_env()
    if args.target == "local":
        host, port, user, password, db = "127.0.0.1", "3306", "root", "root", "shenyu-ops"
    else:
        host = cfg.get("OPS_TEST_DB_HOST", "110.42.49.224")
        port = cfg.get("OPS_TEST_DB_PORT", "3306")
        user = cfg.get("OPS_TEST_MASTER_USER", "shenyu-ops")
        password = cfg.get("OPS_TEST_MASTER_PASSWORD", "")
        db = cfg.get("OPS_TEST_MASTER_DB", "shenyu-ops")
    print(f"Patching sys_param on {host}:{port}/{db}")
    return mysql(host, port, user, password, db, PATCH_SQL)


if __name__ == "__main__":
    sys.exit(main())
