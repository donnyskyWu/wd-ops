#!/usr/bin/env python3
"""Apply today's missing Flyway SQL (V166/V167) + shenyu-system perm rename to Beta.

Credentials: scripts/integration-config/ops-test-remote.env (gitignored).
Idempotent checks before ALTER / history insert.
"""
from __future__ import annotations

import os
import re
import subprocess
import sys
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]
ENV_FILE = ROOT / "scripts/integration-config/ops-test-remote.env"
MIG_DIR = (
    ROOT
    / "football-backend-saas/football-module-ops/football-module-ops-server"
    / "src/main/resources/db/migration"
)
SYSTEM_RENAME = (
    ROOT / "scripts/integration-config/rename-permission-oa-to-ops-shenyu-system.sql"
)


def load_env() -> dict[str, str]:
    if not ENV_FILE.is_file():
        print(f"Missing {ENV_FILE}", file=sys.stderr)
        sys.exit(2)
    cfg: dict[str, str] = {}
    for line in ENV_FILE.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        k, v = line.split("=", 1)
        cfg[k.strip()] = v.strip().strip('"').strip("'")
    return cfg


def flyway_checksum(sql_bytes: bytes) -> int:
    """Flyway CRC32: per-line UTF-8 update (no newline bytes), signed 32-bit."""
    text = sql_bytes.decode("utf-8-sig")
    crc = 0
    for line in text.splitlines():
        crc = zlib.crc32(line.encode("utf-8"), crc)
    crc &= 0xFFFFFFFF
    if crc >= 0x80000000:
        return crc - 0x100000000
    return crc


def mysql_run(
    host: str,
    port: str,
    user: str,
    password: str,
    database: str,
    *,
    sql: str | None = None,
    sql_file: Path | None = None,
) -> str:
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
    ]
    if sql_file is not None:
        data = sql_file.read_bytes()
        proc = subprocess.run(cmd, input=data, capture_output=True, env=env)
    else:
        cmd.extend(["-e", sql or ""])
        proc = subprocess.run(cmd, capture_output=True, env=env)
    out = proc.stdout.decode("utf-8", errors="replace")
    err = proc.stderr.decode("utf-8", errors="replace")
    if proc.returncode != 0:
        raise RuntimeError(f"mysql rc={proc.returncode}: {err.strip() or out.strip()}")
    return out.strip()


def version_applied(host, port, user, pw, db, version: str) -> bool:
    row = mysql_run(
        host,
        port,
        user,
        pw,
        db,
        sql=(
            f"SELECT COUNT(*) FROM flyway_schema_history "
            f"WHERE version='{version}' AND success=1"
        ),
    )
    return row.strip() == "1"


def column_exists(host, port, user, pw, db, table: str, column: str) -> bool:
    row = mysql_run(
        host,
        port,
        user,
        pw,
        db,
        sql=(
            "SELECT COUNT(*) FROM information_schema.COLUMNS "
            f"WHERE TABLE_SCHEMA='{db}' AND TABLE_NAME='{table}' "
            f"AND COLUMN_NAME='{column}'"
        ),
    )
    return row.strip() != "0"


def table_exists(host, port, user, pw, db, table: str) -> bool:
    row = mysql_run(
        host,
        port,
        user,
        pw,
        db,
        sql=(
            "SELECT COUNT(*) FROM information_schema.TABLES "
            f"WHERE TABLE_SCHEMA='{db}' AND TABLE_NAME='{table}'"
        ),
    )
    return row.strip() != "0"


def next_rank(host, port, user, pw, db) -> int:
    row = mysql_run(
        host,
        port,
        user,
        pw,
        db,
        sql="SELECT COALESCE(MAX(installed_rank),0)+1 FROM flyway_schema_history",
    )
    return int(row.strip())


def insert_history(
    host,
    port,
    user,
    pw,
    db,
    *,
    version: str,
    description: str,
    script: str,
    checksum: int,
    execution_ms: int,
) -> None:
    rank = next_rank(host, port, user, pw, db)
    # escape single quotes in description/script
    desc = description.replace("'", "''")
    scr = script.replace("'", "''")
    mysql_run(
        host,
        port,
        user,
        pw,
        db,
        sql=(
            "INSERT INTO flyway_schema_history "
            "(installed_rank, version, description, type, script, checksum, "
            "installed_by, installed_on, execution_time, success) VALUES ("
            f"{rank}, '{version}', '{desc}', 'SQL', '{scr}', {checksum}, "
            f"USER(), NOW(), {execution_ms}, 1)"
        ),
    )
    print(f"[history] inserted V{version} rank={rank} checksum={checksum}")


def desc_from_filename(name: str) -> str:
    # V167__tenant_unified_collect_task.sql -> tenant unified collect task
    m = re.match(r"V(\d+)__(.+)\.sql$", name)
    if not m:
        return name
    return m.group(2).replace("_", " ")


def apply_v166(host, port, user, pw, db) -> str:
    path = MIG_DIR / "V166__rename_permission_oa_to_ops.sql"
    if version_applied(host, port, user, pw, db, "166"):
        print("[skip] V166 already in flyway_schema_history")
        return "skip-history"
    before = mysql_run(
        host,
        port,
        user,
        pw,
        db,
        sql="SELECT COUNT(*) FROM system_menu WHERE permission LIKE 'oa:%' AND deleted=0",
    )
    print(f"[V166] master oa: count before={before}")
    mysql_run(host, port, user, pw, db, sql_file=path)
    after = mysql_run(
        host,
        port,
        user,
        pw,
        db,
        sql="SELECT COUNT(*) FROM system_menu WHERE permission LIKE 'oa:%' AND deleted=0",
    )
    print(f"[V166] master oa: count after={after}")
    checksum = flyway_checksum(path.read_bytes())
    insert_history(
        host,
        port,
        user,
        pw,
        db,
        version="166",
        description=desc_from_filename(path.name),
        script=path.name,
        checksum=checksum,
        execution_ms=50,
    )
    return "applied"


def apply_v167(host, port, user, pw, db) -> str:
    path = MIG_DIR / "V167__tenant_unified_collect_task.sql"
    if version_applied(host, port, user, pw, db, "167"):
        print("[skip] V167 already in flyway_schema_history")
        return "skip-history"

    # Apply pieces with IF-NOT-EXISTS style guards (MySQL 5.7 has no ADD COLUMN IF NOT EXISTS)
    stmts: list[str] = []
    if not column_exists(host, port, user, pw, db, "oa_account", "collect_enabled"):
        stmts.append(
            "ALTER TABLE oa_account ADD COLUMN collect_enabled TINYINT NOT NULL DEFAULT 0 "
            "COMMENT '是否采集（1=加入租户统一任务成员）' AFTER publish_enabled"
        )
    else:
        print("[V167] oa_account.collect_enabled already exists")

    if not column_exists(host, port, user, pw, db, "oa_account_ext", "collect_enabled"):
        stmts.append(
            "ALTER TABLE oa_account_ext ADD COLUMN collect_enabled TINYINT NOT NULL DEFAULT 0 "
            "COMMENT '是否采集（ADR-061）' AFTER usage_status"
        )
    else:
        print("[V167] oa_account_ext.collect_enabled already exists")

    if not column_exists(host, port, user, pw, db, "oa_collect_task", "is_unified"):
        stmts.append(
            "ALTER TABLE oa_collect_task ADD COLUMN is_unified TINYINT NULL DEFAULT NULL "
            "COMMENT '1=租户统一采集任务；NULL=非统一' AFTER credential_profile"
        )
    else:
        print("[V167] oa_collect_task.is_unified already exists")

    # unique key — check index
    idx = mysql_run(
        host,
        port,
        user,
        pw,
        db,
        sql=(
            "SELECT COUNT(*) FROM information_schema.STATISTICS "
            f"WHERE TABLE_SCHEMA='{db}' AND TABLE_NAME='oa_collect_task' "
            "AND INDEX_NAME='uk_oa_collect_task_tenant_unified'"
        ),
    )
    if idx.strip() == "0":
        # ensure column exists before UK
        if not column_exists(host, port, user, pw, db, "oa_collect_task", "is_unified"):
            pass  # will be added above in same batch
        stmts.append(
            "ALTER TABLE oa_collect_task "
            "ADD UNIQUE KEY uk_oa_collect_task_tenant_unified (tenant_id, is_unified)"
        )
    else:
        print("[V167] uk_oa_collect_task_tenant_unified already exists")

    for s in stmts:
        print(f"[V167] exec: {s[:80]}...")
        mysql_run(host, port, user, pw, db, sql=s)

    if not table_exists(host, port, user, pw, db, "oa_collect_task_account"):
        print("[V167] create oa_collect_task_account")
        mysql_run(
            host,
            port,
            user,
            pw,
            db,
            sql="""
CREATE TABLE oa_collect_task_account (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    task_id         BIGINT       NOT NULL COMMENT 'FK oa_collect_task.id',
    account_id      BIGINT       NOT NULL COMMENT 'FK oa_account.id',
    creator         VARCHAR(64)  DEFAULT 'system',
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         VARCHAR(64)  DEFAULT 'system',
    update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT     NOT NULL DEFAULT 0,
    UNIQUE KEY uk_oa_collect_task_account (tenant_id, task_id, account_id),
    KEY idx_oa_collect_task_account_task (tenant_id, task_id),
    KEY idx_oa_collect_task_account_acct (tenant_id, account_id)
) COMMENT='采集任务-账号成员（统一任务多账号）'
""",
        )
    else:
        print("[V167] oa_collect_task_account already exists")

    # seed sys_param
    mysql_run(
        host,
        port,
        user,
        pw,
        db,
        sql="""
INSERT INTO sys_param (tenant_id, param_name, param_key, param_value, param_type, category, remark, creator, updater)
SELECT 1, '统一采集调度Cron', 'collect.schedule.cron', '0 0 23 * * ?', 'STRING', 'COLLECT',
       'ADR-061 租户统一采集任务默认每日 23:00', 'v167', 'v167'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM sys_param WHERE tenant_id = 1 AND param_key = 'collect.schedule.cron' AND deleted = 0
)
""",
    )
    print("[V167] sys_param seed done")

    checksum = flyway_checksum(path.read_bytes())
    insert_history(
        host,
        port,
        user,
        pw,
        db,
        version="167",
        description=desc_from_filename(path.name),
        script=path.name,
        checksum=checksum,
        execution_ms=200,
    )
    return "applied"


def apply_system_rename(host, port, user, pw, db) -> str:
    before = mysql_run(
        host,
        port,
        user,
        pw,
        db,
        sql="SELECT COUNT(*) FROM system_menu WHERE permission LIKE 'oa:%' AND deleted=b'0'",
    )
    ops_before = mysql_run(
        host,
        port,
        user,
        pw,
        db,
        sql="SELECT COUNT(*) FROM system_menu WHERE permission LIKE 'ops:%' AND deleted=b'0'",
    )
    print(f"[system] oa:={before} ops:={ops_before} before")
    if before.strip() == "0":
        print("[skip] shenyu-system already has no oa: permissions")
        return "skip"
    mysql_run(host, port, user, pw, db, sql_file=SYSTEM_RENAME)
    after = mysql_run(
        host,
        port,
        user,
        pw,
        db,
        sql="SELECT COUNT(*) FROM system_menu WHERE permission LIKE 'oa:%' AND deleted=b'0'",
    )
    ops_after = mysql_run(
        host,
        port,
        user,
        pw,
        db,
        sql="SELECT COUNT(*) FROM system_menu WHERE permission LIKE 'ops:%' AND deleted=b'0'",
    )
    print(f"[system] oa:={after} ops:={ops_after} after")
    return "applied"


def verify(host, port, master_user, master_pw, master_db, sys_user, sys_pw, sys_db) -> None:
    print("=== VERIFY ===")
    print(
        "flyway max:",
        mysql_run(
            host,
            port,
            master_user,
            master_pw,
            master_db,
            sql=(
                "SELECT MAX(CAST(version AS UNSIGNED)) FROM flyway_schema_history "
                "WHERE version REGEXP '^[0-9]+$' AND success=1"
            ),
        ),
    )
    print(
        "V166/V167:",
        mysql_run(
            host,
            port,
            master_user,
            master_pw,
            master_db,
            sql=(
                "SELECT version, success, checksum, installed_on FROM flyway_schema_history "
                "WHERE version IN ('166','167') ORDER BY version"
            ),
        ),
    )
    print(
        "collect_enabled:",
        mysql_run(
            host,
            port,
            master_user,
            master_pw,
            master_db,
            sql=(
                "SELECT TABLE_NAME, COLUMN_NAME FROM information_schema.COLUMNS "
                f"WHERE TABLE_SCHEMA='{master_db}' AND COLUMN_NAME='collect_enabled'"
            ),
        ),
    )
    print(
        "is_unified:",
        mysql_run(
            host,
            port,
            master_user,
            master_pw,
            master_db,
            sql=(
                "SELECT TABLE_NAME, COLUMN_NAME FROM information_schema.COLUMNS "
                f"WHERE TABLE_SCHEMA='{master_db}' AND COLUMN_NAME='is_unified'"
            ),
        ),
    )
    print(
        "oa_collect_task_account:",
        mysql_run(
            host,
            port,
            master_user,
            master_pw,
            master_db,
            sql="SHOW TABLES LIKE 'oa_collect_task_account'",
        ),
    )
    print(
        "sys_param cron:",
        mysql_run(
            host,
            port,
            master_user,
            master_pw,
            master_db,
            sql=(
                "SELECT tenant_id, param_key, param_value FROM sys_param "
                "WHERE param_key='collect.schedule.cron' AND deleted=0"
            ),
        ),
    )
    print(
        "master oa: leftover:",
        mysql_run(
            host,
            port,
            master_user,
            master_pw,
            master_db,
            sql="SELECT COUNT(*) FROM system_menu WHERE permission LIKE 'oa:%' AND deleted=0",
        ),
    )
    print(
        "system oa:/ops:",
        mysql_run(
            host,
            port,
            sys_user,
            sys_pw,
            sys_db,
            sql=(
                "SELECT "
                "SUM(permission LIKE 'oa:%') AS oa_cnt, "
                "SUM(permission LIKE 'ops:%') AS ops_cnt "
                "FROM system_menu WHERE deleted=b'0' AND permission IS NOT NULL"
            ),
        ),
    )
    print(
        "failed flyway:",
        mysql_run(
            host,
            port,
            master_user,
            master_pw,
            master_db,
            sql="SELECT COUNT(*) FROM flyway_schema_history WHERE success=0",
        ),
    )


def main() -> None:
    cfg = load_env()
    host = cfg.get("OPS_TEST_DB_HOST", "110.42.49.224")
    port = cfg.get("OPS_TEST_DB_PORT", "3306")
    master_user = cfg.get("OPS_TEST_MASTER_USER", "shenyu-ops")
    master_pw = cfg.get("OPS_TEST_MASTER_PASSWORD", cfg.get("OPS_WD_TEST_PASSWORD", ""))
    master_db = cfg.get("OPS_TEST_MASTER_DB", "shenyu-ops")
    sys_user = cfg.get("OPS_TEST_SYSTEM_USER", "shenyu-system")
    sys_pw = cfg.get("OPS_TEST_SYSTEM_PASSWORD", "")
    sys_db = cfg.get("OPS_TEST_SYSTEM_DB", "shenyu-system")

    if not master_pw or not sys_pw:
        print("Missing master/system passwords in ops-test-remote.env", file=sys.stderr)
        sys.exit(2)

    print(f"=== TARGET === {host}:{port} master={master_db} system={sys_db}")
    for p in [
        MIG_DIR / "V166__rename_permission_oa_to_ops.sql",
        MIG_DIR / "V167__tenant_unified_collect_task.sql",
        SYSTEM_RENAME,
    ]:
        if not p.is_file():
            print(f"Missing file: {p}", file=sys.stderr)
            sys.exit(2)

    r166 = apply_v166(host, port, master_user, master_pw, master_db)
    r167 = apply_v167(host, port, master_user, master_pw, master_db)
    rsys = apply_system_rename(host, port, sys_user, sys_pw, sys_db)
    print(f"=== RESULTS === V166={r166} V167={r167} system_rename={rsys}")
    verify(host, port, master_user, master_pw, master_db, sys_user, sys_pw, sys_db)


if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        print(f"FATAL: {e}", file=sys.stderr)
        sys.exit(1)
