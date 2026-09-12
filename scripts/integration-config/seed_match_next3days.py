#!/usr/bin/env python3
"""Seed jc-match demo rows for today + next 2 days (MatchSelectDialog / list-by-date).

Usage:
  python scripts/integration-config/seed_match_next3days.py --target local
  python scripts/integration-config/seed_match_next3days.py --target test

Idempotent: replaces schedule_id 9026001–9026006 only; does not wipe other match data.
"""
from __future__ import annotations

import argparse
import os
import subprocess
import sys
from datetime import date, datetime, timedelta
from pathlib import Path
from zoneinfo import ZoneInfo

ROOT = Path(__file__).resolve().parents[2]
ENV_FILE = ROOT / "scripts/integration-config/ops-test-remote.env"
TZ = ZoneInfo("Asia/Shanghai")

# Dedicated demo schedule ids (avoid clashing with real jc data)
SCHEDULE_IDS = list(range(9026001, 9026007))

REDIS_KEYS = (
    "data:filter:competitions:schedule:flat",
    "data:filter:competitions:schedule:grouped",
    "data:filter:competitions:result:flat",
    "data:filter:competitions:live:flat",
)

WEEKDAY_ZH = ("周一", "周二", "周三", "周四", "周五", "周六", "周日")

MATCHES = (
    # (day_offset, hour, minute, sclass_id, league, home, away, issue_num)
    (0, 20, 0, 36, "英格兰超级联赛", "曼联", "切尔西", 1),
    (0, 22, 0, 31, "西班牙甲级联赛", "皇马", "巴萨", 2),
    (1, 19, 0, 34, "意大利甲级联赛", "AC米兰", "国际米兰", 1),
    (1, 21, 0, 8, "德国甲级联赛", "拜仁", "多特蒙德", 2),
    (2, 18, 0, 36, "英格兰超级联赛", "阿森纳", "利物浦", 1),
    (2, 20, 30, 34, "意大利甲级联赛", "尤文图斯", "那不勒斯", 2),
)

LEAGUE_SHORT = {
    36: "英超",
    31: "西甲",
    34: "意甲",
    8: "德甲",
}


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


def mysql_exec(host: str, port: str, user: str, password: str, database: str, sql: str) -> None:
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
            "-e",
            sql,
        ],
        capture_output=True,
        env=env,
    )
    if proc.returncode != 0:
        sys.stderr.write(proc.stderr.decode("utf-8", errors="replace"))
        sys.exit(proc.returncode)


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


def build_sql(base_day: date) -> str:
    lines = [
        "SET NAMES utf8mb4;",
        "SET CHARACTER SET utf8mb4;",
        "",
        "CREATE TABLE IF NOT EXISTS data_competition (",
        "  sclass_id INT NOT NULL PRIMARY KEY,",
        "  category_id INT DEFAULT NULL, country_id INT DEFAULT NULL,",
        "  name_en VARCHAR(128) DEFAULT NULL, name_zh VARCHAR(128) DEFAULT NULL,",
        "  name_zht VARCHAR(128) DEFAULT NULL, short_name_zh VARCHAR(64) DEFAULT NULL,",
        "  short_name_zht VARCHAR(64) DEFAULT NULL, short_name_en VARCHAR(64) DEFAULT NULL,",
        "  first_letter VARCHAR(8) DEFAULT '#', is_hot TINYINT DEFAULT 0,",
        "  logo VARCHAR(512) DEFAULT NULL, type TINYINT DEFAULT 1,",
        "  title_holder VARCHAR(128) DEFAULT NULL, most_titles VARCHAR(128) DEFAULT NULL,",
        "  newcomers VARCHAR(256) DEFAULT NULL, divisions VARCHAR(256) DEFAULT NULL,",
        "  host_country VARCHAR(64) DEFAULT NULL, host_city VARCHAR(64) DEFAULT NULL,",
        "  primary_color VARCHAR(32) DEFAULT NULL, secondary_color VARCHAR(32) DEFAULT NULL,",
        "  updated_at BIGINT DEFAULT NULL, created_at BIGINT DEFAULT NULL, deleted TINYINT DEFAULT 0",
        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;",
        "",
        "CREATE TABLE IF NOT EXISTS data_match_info (",
        "  schedule_id INT NOT NULL PRIMARY KEY, sclass_id INT DEFAULT NULL,",
        "  sclass_name VARCHAR(128) DEFAULT NULL, sclass_grade VARCHAR(32) DEFAULT NULL,",
        "  country_id INT DEFAULT NULL, country_name VARCHAR(64) DEFAULT NULL,",
        "  pinyin_country VARCHAR(64) DEFAULT NULL, issue_name VARCHAR(32) DEFAULT NULL,",
        "  match_state INT DEFAULT 1, match_state_name VARCHAR(32) DEFAULT '未开',",
        "  match_time DATETIME DEFAULT NULL, match_time_str VARCHAR(16) DEFAULT NULL,",
        "  residue_minute VARCHAR(16) DEFAULT NULL,",
        "  home_team_id INT DEFAULT NULL, home_team_name VARCHAR(128) DEFAULT NULL,",
        "  home_logo VARCHAR(512) DEFAULT NULL, home_score INT DEFAULT NULL,",
        "  home_half_score INT DEFAULT NULL, home_corner INT DEFAULT NULL,",
        "  home_red INT DEFAULT NULL, home_yellow INT DEFAULT NULL, home_order VARCHAR(16) DEFAULT NULL,",
        "  home_scores VARCHAR(64) DEFAULT NULL, guest_team_id INT DEFAULT NULL,",
        "  guest_team_name VARCHAR(128) DEFAULT NULL, guest_logo VARCHAR(512) DEFAULT NULL,",
        "  guest_score INT DEFAULT NULL, guest_half_score INT DEFAULT NULL, guest_corner INT DEFAULT NULL,",
        "  guest_red INT DEFAULT NULL, guest_yellow INT DEFAULT NULL, guest_order VARCHAR(16) DEFAULT NULL,",
        "  guest_scores VARCHAR(64) DEFAULT NULL, is_live TINYINT DEFAULT 0, is_mlive TINYINT DEFAULT 0,",
        "  is_live_room TINYINT DEFAULT 0, is_focus TINYINT DEFAULT 0, let_goal VARCHAR(32) DEFAULT NULL,",
        "  let_stop_live VARCHAR(32) DEFAULT NULL, let_stop_live_int INT DEFAULT NULL,",
        "  to_stop_live VARCHAR(32) DEFAULT NULL, index_let VARCHAR(64) DEFAULT NULL,",
        "  first_index_let VARCHAR(64) DEFAULT NULL, index_total VARCHAR(64) DEFAULT NULL,",
        "  first_index_total VARCHAR(64) DEFAULT NULL, index_let_goals VARCHAR(64) DEFAULT NULL,",
        "  neutrality TINYINT DEFAULT 0, adivce_num INT DEFAULT 0, intelligence_num INT DEFAULT 0,",
        "  is_traditional_lottery TINYINT DEFAULT 0, lottery_issue BIGINT DEFAULT NULL, deleted TINYINT DEFAULT 0",
        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;",
        "",
        "CREATE TABLE IF NOT EXISTS data_odds_jc (",
        "  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, lottery_id INT DEFAULT NULL,",
        "  lottery_type INT NOT NULL DEFAULT 1, schedule_id INT NOT NULL,",
        "  comp VARCHAR(128) DEFAULT NULL, home VARCHAR(128) DEFAULT NULL, away VARCHAR(128) DEFAULT NULL,",
        "  short_comp VARCHAR(64) DEFAULT NULL, short_home VARCHAR(64) DEFAULT NULL,",
        "  short_away VARCHAR(64) DEFAULT NULL, sport_type INT DEFAULT 1, odds_time INT DEFAULT NULL,",
        "  issue INT DEFAULT NULL, issue_num INT DEFAULT NULL, match_time BIGINT DEFAULT NULL,",
        "  sell_status VARCHAR(32) DEFAULT NULL, spf VARCHAR(128) DEFAULT NULL, rq VARCHAR(128) DEFAULT NULL,",
        "  bf VARCHAR(512) DEFAULT NULL, jq VARCHAR(128) DEFAULT NULL, bqc VARCHAR(256) DEFAULT NULL,",
        "  sf VARCHAR(64) DEFAULT NULL, rf VARCHAR(64) DEFAULT NULL, dxf VARCHAR(64) DEFAULT NULL,",
        "  sfc VARCHAR(256) DEFAULT NULL,",
        "  KEY idx_odds_jc_schedule (schedule_id),",
        "  KEY idx_odds_jc_match_time (match_time),",
        "  KEY idx_odds_jc_lottery_type (lottery_type)",
        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;",
        "",
        "INSERT INTO data_competition (sclass_id, name_zh, short_name_zh, first_letter, is_hot, deleted) VALUES",
        "  (36, '英格兰超级联赛', '英超', 'Y', 1, 0),",
        "  (31, '西班牙甲级联赛', '西甲', 'X', 1, 0),",
        "  (34, '意大利甲级联赛', '意甲', 'Y', 1, 0),",
        "  (8,  '德国甲级联赛', '德甲', 'D', 1, 0)",
        "ON DUPLICATE KEY UPDATE name_zh=VALUES(name_zh), short_name_zh=VALUES(short_name_zh), deleted=0;",
        "",
        f"DELETE FROM data_odds_jc WHERE schedule_id IN ({','.join(map(str, SCHEDULE_IDS))});",
        f"DELETE FROM data_match_info WHERE schedule_id IN ({','.join(map(str, SCHEDULE_IDS))});",
        "",
        "INSERT INTO data_match_info (",
        "  schedule_id, sclass_id, sclass_name, issue_name, match_state, match_state_name,",
        "  match_time, match_time_str, home_team_name, guest_team_name, deleted",
        ") VALUES",
    ]

    match_rows: list[str] = []
    odds_rows: list[str] = []

    for idx, (day_off, hour, minute, sclass_id, league, home, away, issue_num) in enumerate(MATCHES):
        d = base_day + timedelta(days=day_off)
        schedule_id = SCHEDULE_IDS[idx]
        dt = datetime(d.year, d.month, d.day, hour, minute, tzinfo=TZ)
        match_time_str = dt.strftime("%H:%M")
        issue_name = f"{WEEKDAY_ZH[d.weekday()]}{issue_num:03d}"
        issue_int = int(d.strftime("%Y%m%d"))
        epoch = int(dt.timestamp())

        match_rows.append(
            f"  ({schedule_id}, {sclass_id}, '{league}', '{issue_name}', 1, '未开', "
            f"'{dt.strftime('%Y-%m-%d %H:%M:%S')}', '{match_time_str}', '{home}', '{away}', 0)"
        )
        short = LEAGUE_SHORT[sclass_id]
        odds_rows.append(
            f"  (1, {schedule_id}, '{league}', '{home}', '{away}', '{short}', '{home}', '{away}', "
            f"1, {epoch}, {issue_int}, {issue_num}, {epoch})"
        )

    lines.append(",\n".join(match_rows) + ";")
    lines.append("")
    lines.append(
        "INSERT INTO data_odds_jc ("
        "lottery_type, schedule_id, comp, home, away, short_comp, short_home, short_away, "
        "sport_type, odds_time, issue, issue_num, match_time) VALUES"
    )
    lines.append(",\n".join(odds_rows) + ";")
    return "\n".join(lines)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--target", choices=("local", "test"), default="test")
    args = parser.parse_args()
    cfg = load_env()

    base_day = datetime.now(TZ).date()
    sql = build_sql(base_day)

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

    print(f"seed_match_next3days applied -> {host}/{database}")
    print(f"  base_date={base_day} (Asia/Shanghai), schedule_ids={SCHEDULE_IDS[0]}..{SCHEDULE_IDS[-1]}")
    for day_off in range(3):
        d = base_day + timedelta(days=day_off)
        print(f"  {d}: 2 matches")
    print("  redis match filter cache cleared")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
