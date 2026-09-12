#!/usr/bin/env python3
"""Seed 竞足 jc-match rows for local content-edit → 选择赛事 → AI generate.

Window: 2026-08-27 .. 2026-09-08 (Asia/Shanghai kickoff date = list-by-date `date`).
JingZu calls list-by-date with sportType=1, dataType=2 (future kickoffs only) and
hides rows without playMethods — so SPF/RQ odds are required.

Idempotent: replaces schedule_id 9026101–9026124 only; backfills SPF on existing
lottery_type=1 rows that have empty odds. Does not wipe other match data.

Usage:
  python scripts/integration-config/seed_match_recent_week.py --target local
"""
from __future__ import annotations

import argparse
import os
import subprocess
import sys
from datetime import datetime
from pathlib import Path
from zoneinfo import ZoneInfo

ROOT = Path(__file__).resolve().parents[2]
ENV_FILE = ROOT / "scripts/integration-config/ops-test-remote.env"
SQL_OUT = ROOT / "scripts/integration-config/seed-local-shenyu-match-recent-week.sql"
TZ = ZoneInfo("Asia/Shanghai")

# Dedicated demo ids (avoid 1002xxx baseline + 9026001–9026008 next3days)
SCHEDULE_START = 9026101

REDIS_KEYS = (
    "data:filter:competitions:schedule:flat",
    "data:filter:competitions:schedule:grouped",
    "data:filter:competitions:result:flat",
    "data:filter:competitions:live:flat",
)

WEEKDAY_ZH = ("周一", "周二", "周三", "周四", "周五", "周六", "周日")

# (yyyy, m, d, hour, minute, sclass_id, league, home, away, issue_num)
# Empty calendar days in local shenyu-match plus late Sep 2 so dataType=2 still lists today.
MATCHES = (
    (2026, 8, 29, 19, 0, 36, "英格兰超级联赛", "热刺", "纽卡斯尔", 1),
    (2026, 8, 29, 21, 30, 31, "西班牙甲级联赛", "马竞", "塞维利亚", 2),
    (2026, 8, 30, 19, 0, 34, "意大利甲级联赛", "罗马", "拉齐奥", 1),
    (2026, 8, 30, 21, 0, 8, "德国甲级联赛", "勒沃库森", "莱比锡", 2),
    (2026, 8, 31, 20, 0, 36, "英格兰超级联赛", "曼城", "布莱顿", 1),
    (2026, 8, 31, 22, 0, 34, "意大利甲级联赛", "亚特兰大", "佛罗伦萨", 2),
    (2026, 9, 1, 19, 30, 31, "西班牙甲级联赛", "皇家社会", "比利亚雷亚尔", 1),
    (2026, 9, 1, 21, 30, 8, "德国甲级联赛", "勒沃库森", "法兰克福", 2),
    (2026, 9, 2, 23, 0, 36, "英格兰超级联赛", "纽卡斯尔", "维拉", 1),
    (2026, 9, 2, 23, 30, 31, "西班牙甲级联赛", "毕尔巴鄂", "赫罗纳", 2),
    (2026, 9, 3, 19, 0, 34, "意大利甲级联赛", "博洛尼亚", "都灵", 1),
    (2026, 9, 3, 21, 0, 8, "德国甲级联赛", "沃尔夫斯堡", "霍芬海姆", 2),
    (2026, 9, 4, 19, 30, 36, "英格兰超级联赛", "西汉姆", "富勒姆", 1),
    (2026, 9, 4, 21, 30, 31, "西班牙甲级联赛", "贝蒂斯", "奥萨苏纳", 2),
    (2026, 9, 5, 18, 0, 34, "意大利甲级联赛", "那不勒斯", "热那亚", 1),
    (2026, 9, 5, 20, 30, 8, "德国甲级联赛", "弗赖堡", "奥格斯堡", 2),
    (2026, 9, 6, 19, 0, 36, "英格兰超级联赛", "布伦特福德", "伯恩茅斯", 1),
    (2026, 9, 6, 21, 0, 31, "西班牙甲级联赛", "瓦伦西亚", "赫塔费", 2),
    (2026, 9, 7, 19, 30, 34, "意大利甲级联赛", "乌迪内斯", "卡利亚里", 1),
    (2026, 9, 7, 21, 30, 8, "德国甲级联赛", "美因茨", "柏林联合", 2),
    (2026, 9, 8, 19, 0, 36, "英格兰超级联赛", "水晶宫", "埃弗顿", 1),
    (2026, 9, 8, 21, 0, 31, "西班牙甲级联赛", "塞尔塔", "马略卡", 2),
)

LEAGUE_SHORT = {
    36: "英超",
    31: "西甲",
    34: "意甲",
    8: "德甲",
}

# Formats consumed by MatchDataServiceImpl.parseSpfOdds / parseRqOdds / parseJqOdds / parseBqcOdds
SPF = "1.85,3.40,4.20"
RQ = "-1,3.60,3.30,2.05"
JQ = "8.50,4.20,3.10,3.40,5.80,9.50,15.00,22.00"
BQC = "4.80,12.00,22.00,8.50,5.20,9.80,28.00,12.00,7.50"


def load_env() -> dict[str, str]:
    env: dict[str, str] = {}
    if not ENV_FILE.is_file():
        return env
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
            f"--host={host}",
            f"--port={port}",
            f"-u{user}",
            "--default-character-set=utf8mb4",
            database,
            "-e",
            sql,
        ],
        capture_output=True,
        env=env,
    )
    if proc.returncode != 0:
        sys.stderr.write(proc.stderr.decode("utf-8", errors="replace"))
        sys.exit(proc.returncode)
    return proc.stdout.decode("utf-8", errors="replace")


def flush_redis(target: str, cfg: dict[str, str]) -> None:
    if target == "test":
        host = cfg.get("OPS_TEST_REDIS_HOST", "110.42.49.224")
        port = cfg.get("OPS_TEST_REDIS_PORT", "6379")
        password = cfg.get("OPS_TEST_REDIS_PASSWORD", "")
        db = cfg.get("OPS_TEST_REDIS_DATABASE", "1")
        for key in REDIS_KEYS:
            subprocess.run(
                ["redis-cli", "-h", host, "-p", port, "-a", password, "-n", db, "DEL", key],
                capture_output=True,
                check=False,
            )
    else:
        for key in REDIS_KEYS:
            subprocess.run(
                ["redis-cli", "-a", "123456", "DEL", key],
                capture_output=True,
                check=False,
            )


def build_sql() -> tuple[str, list[int]]:
    schedule_ids = [SCHEDULE_START + i for i in range(len(MATCHES))]
    id_csv = ",".join(map(str, schedule_ids))

    match_rows: list[str] = []
    odds_rows: list[str] = []
    for idx, (y, mo, d, hour, minute, sclass_id, league, home, away, issue_num) in enumerate(MATCHES):
        schedule_id = schedule_ids[idx]
        dt = datetime(y, mo, d, hour, minute, tzinfo=TZ)
        match_time_str = dt.strftime("%H:%M")
        issue_name = f"{WEEKDAY_ZH[dt.weekday()]}{issue_num:03d}"
        issue_int = int(dt.strftime("%Y%m%d"))
        epoch = int(dt.timestamp())
        short = LEAGUE_SHORT[sclass_id]
        match_rows.append(
            f"  ({schedule_id}, {sclass_id}, '{league}', '{issue_name}', 1, '未开', "
            f"'{dt.strftime('%Y-%m-%d %H:%M:%S')}', '{match_time_str}', '{home}', '{away}', 0)"
        )
        odds_rows.append(
            f"  (1, {schedule_id}, '{league}', '{home}', '{away}', '{short}', '{home}', '{away}', "
            f"1, {epoch}, {issue_int}, {issue_num}, {epoch}, '{SPF}', '{RQ}', '{JQ}', '{BQC}')"
        )

    lines = [
        "SET NAMES utf8mb4;",
        "SET CHARACTER SET utf8mb4;",
        "SET time_zone = '+08:00';",
        "",
        "INSERT INTO data_competition (sclass_id, name_zh, short_name_zh, first_letter, is_hot, deleted) VALUES",
        "  (36, '英格兰超级联赛', '英超', 'Y', 1, 0),",
        "  (31, '西班牙甲级联赛', '西甲', 'X', 1, 0),",
        "  (34, '意大利甲级联赛', '意甲', 'Y', 1, 0),",
        "  (8,  '德国甲级联赛', '德甲', 'D', 1, 0)",
        "ON DUPLICATE KEY UPDATE name_zh=VALUES(name_zh), short_name_zh=VALUES(short_name_zh), deleted=0;",
        "",
        f"DELETE FROM data_odds_jc WHERE schedule_id IN ({id_csv});",
        f"DELETE FROM data_match_info WHERE schedule_id IN ({id_csv});",
        "",
        "INSERT INTO data_match_info (",
        "  schedule_id, sclass_id, sclass_name, issue_name, match_state, match_state_name,",
        "  match_time, match_time_str, home_team_name, guest_team_name, deleted",
        ") VALUES",
        ",\n".join(match_rows) + ";",
        "",
        "INSERT INTO data_odds_jc (",
        "lottery_type, schedule_id, comp, home, away, short_comp, short_home, short_away, ",
        "sport_type, odds_time, issue, issue_num, match_time, spf, rq, jq, bqc) VALUES",
        ",\n".join(odds_rows) + ";",
        "",
        "-- Backfill 胜平负 so existing local rows appear in JingZu (playMethods required).",
        "UPDATE data_odds_jc SET",
        f"  spf = IF(spf IS NULL OR spf = '', '{SPF}', spf),",
        f"  rq = IF(rq IS NULL OR rq = '', '{RQ}', rq),",
        f"  jq = IF(jq IS NULL OR jq = '', '{JQ}', jq),",
        f"  bqc = IF(bqc IS NULL OR bqc = '', '{BQC}', bqc)",
        "WHERE lottery_type = 1 AND sport_type = 1;",
        "",
        "-- Fix 9026007/9026008 odds epoch so list-by-date?date=2026-09-02 hits them.",
        "UPDATE data_odds_jc SET match_time = UNIX_TIMESTAMP('2026-09-02 18:00:00'),",
        "  odds_time = UNIX_TIMESTAMP('2026-09-02 18:00:00')",
        "WHERE schedule_id = 9026007;",
        "UPDATE data_odds_jc SET match_time = UNIX_TIMESTAMP('2026-09-02 20:30:00'),",
        "  odds_time = UNIX_TIMESTAMP('2026-09-02 20:30:00')",
        "WHERE schedule_id = 9026008;",
    ]
    return "\n".join(lines) + "\n", schedule_ids


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--target", choices=("local", "test"), default="local")
    parser.add_argument("--sql-only", action="store_true", help="Write SQL file only, do not execute")
    args = parser.parse_args()
    cfg = load_env()

    sql, schedule_ids = build_sql()
    SQL_OUT.write_text(sql, encoding="utf-8")
    print(f"wrote {SQL_OUT}")

    if args.sql_only:
        return 0

    if args.target == "test":
        host = cfg.get("OPS_TEST_DB_HOST", "110.42.49.224")
        port = cfg.get("OPS_TEST_DB_PORT", "3306")
        user = cfg.get("OPS_TEST_MATCH_USER", "shenyu-match")
        password = cfg.get("OPS_TEST_MATCH_PASSWORD", "")
        database = cfg.get("OPS_TEST_MATCH_DB", "shenyu-match")
    else:
        host, port, user, password, database = "127.0.0.1", "3306", "root", "root", "shenyu-match"

    mysql_exec(host, port, user, password, database, sql)
    flush_redis(args.target, cfg)

    verify = mysql_exec(
        host,
        port,
        user,
        password,
        database,
        "SELECT DATE(FROM_UNIXTIME(match_time)) AS d, COUNT(*) AS c, "
        "GROUP_CONCAT(CONCAT(home,'VS',away) ORDER BY match_time SEPARATOR '; ') AS pairs "
        "FROM data_odds_jc WHERE lottery_type=1 AND sport_type=1 "
        "AND match_time BETWEEN UNIX_TIMESTAMP('2026-08-27 00:00:00') "
        "AND UNIX_TIMESTAMP('2026-09-08 23:59:59') "
        "GROUP BY d ORDER BY d;",
    )
    print(f"seed_match_recent_week applied -> {host}/{database}")
    print(f"  schedule_ids={schedule_ids[0]}..{schedule_ids[-1]} ({len(schedule_ids)} new rows)")
    print(verify.rstrip())
    print("  redis match filter cache cleared")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
