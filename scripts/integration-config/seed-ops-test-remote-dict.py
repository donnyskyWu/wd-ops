#!/usr/bin/env python3
"""Copy OPS business dicts from shenyu-ops.sys_dict_* to shenyu-system.system_dict_* on test remote.

Uses separate DB credentials (no cross-DB SQL). Loads ops-test-remote.env from repo root.
"""
from __future__ import annotations

import os
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ENV_FILE = ROOT / "scripts/integration-config/ops-test-remote.env"


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


def mysql_query(host: str, port: str, user: str, password: str, database: str, sql: str) -> str:
    env = os.environ.copy()
    env["MYSQL_PWD"] = password
    cmd = [
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
    ]
    proc = subprocess.run(cmd, capture_output=True, env=env)
    if proc.returncode != 0:
        print(proc.stderr.decode("utf-8", errors="replace"), file=sys.stderr)
        sys.exit(proc.returncode)
    return proc.stdout.decode("utf-8", errors="replace")


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


def esc(val: str | None) -> str:
    if val is None:
        return "NULL"
    return "'" + val.replace("\\", "\\\\").replace("'", "''") + "'"


def main() -> int:
    cfg = load_env()
    host = cfg.get("OPS_TEST_DB_HOST", "110.42.49.224")
    port = cfg.get("OPS_TEST_DB_PORT", "3306")
    ops_db = cfg.get("OPS_TEST_MASTER_DB", "shenyu-ops")
    ops_user = cfg.get("OPS_TEST_MASTER_USER", "shenyu-ops")
    ops_pass = cfg.get("OPS_TEST_MASTER_PASSWORD", "")
    sys_db = cfg.get("OPS_TEST_SYSTEM_DB", "shenyu-system")
    sys_user = cfg.get("OPS_TEST_SYSTEM_USER", "shenyu-system")
    sys_pass = cfg.get("OPS_TEST_SYSTEM_PASSWORD", "")

    existing_types = set(
        mysql_query(
            host,
            port,
            sys_user,
            sys_pass,
            sys_db,
            "SELECT type FROM system_dict_type WHERE deleted=0 AND type LIKE 'dict_%';",
        )
        .strip()
        .splitlines()
    )
    existing_types.discard("")

    type_rows = mysql_query(
        host,
        port,
        ops_user,
        ops_pass,
        ops_db,
        """
        SELECT name, type, status
        FROM sys_dict_type
        WHERE deleted=0 AND type LIKE 'dict\\_%'
        ORDER BY type;
        """,
    ).strip()

    type_inserts: list[str] = []
    types_to_copy: list[str] = []
    for line in type_rows.splitlines():
        if not line:
            continue
        parts = line.split("\t")
        if len(parts) < 3:
            continue
        name, dtype, status = parts[0], parts[1], parts[2]
        if dtype in existing_types:
            continue
        st = "1" if status.upper().strip() == "DISABLED" else "0"
        type_inserts.append(
            "INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted) "
            f"VALUES ({esc(name)}, {esc(dtype)}, {st}, {esc('ops-dict-merge:' + dtype)}, 'ops-test-seed', NOW(), 'ops-test-seed', NOW(), b'0');"
        )
        types_to_copy.append(dtype)

    data_inserts: list[str] = []
    if types_to_copy:
        in_list = ",".join(esc(t) for t in types_to_copy)
        data_rows = mysql_query(
            host,
            port,
            ops_user,
            ops_pass,
            ops_db,
            f"""
            SELECT dict_type, label, dict_value, sort, status, IFNULL(color_type,'')
            FROM sys_dict_data
            WHERE deleted=0 AND dict_type IN ({in_list})
            ORDER BY dict_type, sort, dict_value;
            """,
        ).strip()
        for line in data_rows.splitlines():
            if not line:
                continue
            parts = line.split("\t")
            if len(parts) < 6:
                continue
            dtype, label, value, sort, status, color = parts
            st = "1" if status.upper().strip() == "DISABLED" else "0"
            color_val = color.strip() or "default"
            data_inserts.append(
                "INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, "
                "creator, create_time, updater, update_time, deleted) "
                f"VALUES ({sort}, {esc(label)}, {esc(value)}, {esc(dtype)}, {st}, {esc(color_val)}, '', NULL, "
                "'ops-test-seed', NOW(), 'ops-test-seed', NOW(), b'0');"
            )

    sql_parts = ["SET NAMES utf8mb4;"]
    sql_parts.extend(type_inserts)
    sql_parts.extend(data_inserts)
    batch = "\n".join(sql_parts)
    if type_inserts or data_inserts:
        mysql_exec(host, port, sys_user, sys_pass, sys_db, batch)
    print(
        f"dict sync: +{len(type_inserts)} types, +{len(data_inserts)} data rows -> {host}/{sys_db}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
