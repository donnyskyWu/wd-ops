#!/usr/bin/env python3
"""Idempotent: seed dict_threshold_metric into shenyu-system.system_dict_*.

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
    (1, "播放量", "PLAY_COUNT"),
    (2, "点赞数", "LIKE_COUNT"),
    (3, "评论数", "COMMENT_COUNT"),
    (4, "转发数", "SHARE_COUNT"),
    (5, "阅读量", "READ_COUNT"),
    (6, "粉丝增长", "FAN_GROWTH"),
    (7, "粉丝数", "FAN_COUNT"),
    (8, "粉丝数", "FOLLOWER"),
    (9, "互动率", "ENGAGEMENT"),
    (10, "转化率", "CONVERSION"),
    (11, "直播在线人数", "LIVE_ONLINE"),
    (12, "负面情绪比例", "NEGATIVE_RATE"),
    (13, "发布频率", "POST_FREQUENCY"),
    (14, "爆款阈值", "HIT_THRESHOLD"),
    (15, "低分阈值", "LOW_SCORE"),
    (16, "粉丝预警", "FAN_ALERT"),
    (17, "GMV", "GMV"),
    (18, "阅读量骤降", "VIEW_DROP"),
    (19, "播放量骤降", "PLAY_DROP"),
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
    parts.append(
        "INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted) "
        "SELECT '阈值指标', 'dict_threshold_metric', 0, 'M8 阈值规则 metricType', 'apply-v176-threshold-metric', NOW(), "
        "'apply-v176-threshold-metric', NOW(), b'0' FROM DUAL WHERE NOT EXISTS ("
        "SELECT 1 FROM system_dict_type st WHERE st.type='dict_threshold_metric' AND st.deleted=b'0'"
        ");"
    )
    for sort, label, value in ROWS:
        parts.append(
            "INSERT INTO system_dict_data (sort, label, value, dict_type, status, "
            "color_type, css_class, remark, creator, create_time, updater, update_time, deleted) "
            f"SELECT {sort}, {esc(label)}, {esc(value)}, 'dict_threshold_metric', 0, "
            "'default', '', NULL, 'apply-v176-threshold-metric', NOW(), 'apply-v176-threshold-metric', NOW(), b'0' "
            "FROM DUAL WHERE NOT EXISTS ("
            "SELECT 1 FROM system_dict_data sd "
            "WHERE sd.dict_type='dict_threshold_metric' AND sd.value=" + esc(value) + " AND sd.deleted=b'0'"
            ");"
        )
    sql = "\n".join(parts)
    mysql_exec(host, port, sys_user, sys_pass, sys_db, sql)
    print(f"apply_v176_threshold_metric: synced dict_threshold_metric -> {host}/{sys_db}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
