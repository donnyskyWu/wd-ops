#!/usr/bin/env python3
"""Generate 04_baseline_dicts.sql from the 2026-07-31 wd sys_dict dump + documented Flyway supplements.

Run from repo root:
  python scripts/integration-config/ops-greenfield-sources/system/_gen_04_baseline_dicts.py
"""
from __future__ import annotations

from datetime import date
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]
DUMP = ROOT / "docs/delivery/e2e-artifacts/B-WP4-ARCHIVE-20260731/backup/wd-q1-candidates-20260731.sql"
OUT = Path(__file__).resolve().parent / "04_baseline_dicts.sql"

SKIP_TYPES = frozenset({"dict_xx1"})

CREATOR = "deploy-dict-seed"

# CHECKLIST-M1 / GLOBAL-CONVENTIONS / API-M1 — missing from July dump (V16/V17 dict DML was stripped)
CHECKLIST_TYPES: list[tuple[str, str]] = [
    ("IP组类型", "dict_ip_group_type"),
    ("IP组状态", "dict_ip_group_status"),
    ("作者状态", "dict_author_status"),
    ("主播类型", "dict_anchor_type"),
]
CHECKLIST_DATA: list[tuple[str, int, str, str, str]] = [
    ("dict_ip_group_type", 1, "大组", "BIG", "default"),
    ("dict_ip_group_type", 2, "小组", "SMALL", "default"),
    ("dict_ip_group_status", 1, "启用", "ENABLED", "default"),
    ("dict_ip_group_status", 2, "停用", "DISABLED", "default"),
    ("dict_author_status", 1, "启用", "ENABLED", "default"),
    ("dict_author_status", 2, "停用", "DISABLED", "default"),
    ("dict_anchor_type", 1, "直播", "LIVE", "default"),
]

# V171 apply_v171_param_category.py + V181 work_task sys_param.category
PARAM_CATEGORY_EXTRA: list[tuple[int, str, str]] = [
    (6, "钉钉配置", "DINGTALK"),
    (7, "工作任务", "WORK_TASK"),
]

# V176 apply_v176_threshold_metric.py (19 rows; dump only has HIT_THRESHOLD/LOW_SCORE/FAN_ALERT)
V176_THRESHOLD: list[tuple[int, str, str]] = [
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

# M10-EXTERNAL slice §4.2 + ADR-067 (V173 dict DML skipped in 01)
COLLECT_DATA_TYPE_EXTRA: list[tuple[int, str, str]] = [
    (21, "公众号搜索", "EXT_WECHAT_MP_SEARCH"),
    (22, "公众号图文列表", "EXT_WECHAT_MP_ARTICLE_LIST"),
    (23, "抖音用户资料", "EXT_DOUYIN_USER_PROFILE"),
    (24, "抖音竞品作品列表", "EXT_DOUYIN_USER_VIDEOS"),
    (25, "视频号用户", "EXT_WECHAT_VIDEO_USER"),
    (26, "视频号作品列表", "EXT_WECHAT_VIDEO_WORK_LIST"),
    (27, "视频号粉丝统计", "EXT_WECHAT_VIDEO_FOLLOWER_STATS"),
    (30, "抖音直播列表", "DOUYIN_LIVE_LIST"),
    (31, "抖音直播明细", "DOUYIN_LIVE_STATS"),
    (32, "视频号直播列表", "WECHAT_VIDEO_LIVE_LIST"),
    (33, "视频号直播明细", "WECHAT_VIDEO_LIVE_STATS"),
]


def parse_sql_tuples(blob: str) -> list[list[str | None]]:
    """Parse MySQL VALUES (...),(...); tuples. Strings use '' escapes."""
    rows: list[list[str | None]] = []
    i = 0
    n = len(blob)
    while i < n:
        if blob[i] != "(":
            i += 1
            continue
        i += 1
        fields: list[str | None] = []
        while i < n:
            while i < n and blob[i] in " \t\r\n":
                i += 1
            if i >= n:
                break
            if blob[i] == ")":
                i += 1
                rows.append(fields)
                break
            if blob[i] == ",":
                i += 1
                continue
            if blob.startswith("NULL", i) and (i + 4 == n or blob[i + 4] in ",)"):
                fields.append(None)
                i += 4
                continue
            if blob[i] == "'":
                i += 1
                buf: list[str] = []
                while i < n:
                    if blob[i] == "'" and i + 1 < n and blob[i + 1] == "'":
                        buf.append("'")
                        i += 2
                        continue
                    if blob[i] == "'":
                        i += 1
                        break
                    buf.append(blob[i])
                    i += 1
                fields.append("".join(buf))
                continue
            j = i
            while j < n and blob[j] not in ",)":
                j += 1
            fields.append(blob[i:j].strip())
            i = j
    return rows


def extract_insert(text: str, table: str) -> str:
    needle = f"INSERT INTO `{table}` VALUES "
    start = text.find(needle)
    if start < 0:
        raise SystemExit(f"INSERT INTO `{table}` not found")
    start += len(needle)
    end = text.find(";\n", start)
    if end < 0:
        end = text.find(";", start)
    return text[start:end]


def esc(val: str) -> str:
    return "'" + val.replace("\\", "\\\\").replace("'", "''") + "'"


def type_insert(name: str, dtype: str) -> str:
    return (
        "INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)\n"
        f"SELECT {esc(name)}, {esc(dtype)}, 0, {esc('ops-greenfield:' + dtype)}, "
        f"{esc(CREATOR)}, NOW(), {esc(CREATOR)}, NOW(), b'0'\n"
        "FROM DUAL\n"
        "WHERE NOT EXISTS (\n"
        f"    SELECT 1 FROM system_dict_type st WHERE st.type = {esc(dtype)} AND st.deleted = b'0'\n"
        ");\n"
    )


def data_insert(sort: int, label: str, value: str, dtype: str, color: str) -> str:
    color_val = color.strip() or "default"
    return (
        "INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, "
        "creator, create_time, updater, update_time, deleted)\n"
        f"SELECT {sort}, {esc(label)}, {esc(value)}, {esc(dtype)}, 0, {esc(color_val)}, '', NULL, "
        f"{esc(CREATOR)}, NOW(), {esc(CREATOR)}, NOW(), b'0'\n"
        "FROM DUAL\n"
        "WHERE NOT EXISTS (\n"
        f"    SELECT 1 FROM system_dict_data sd WHERE sd.dict_type = {esc(dtype)} "
        f"AND sd.value = {esc(value)} AND sd.deleted = b'0'\n"
        ");\n"
    )


def main() -> int:
    text = DUMP.read_text(encoding="utf-8")
    type_rows = parse_sql_tuples(extract_insert(text, "sys_dict_type"))
    data_rows = parse_sql_tuples(extract_insert(text, "sys_dict_data"))

    types: dict[str, str] = {}
    for row in type_rows:
        dtype = str(row[1])
        name = str(row[2])
        if dtype in SKIP_TYPES:
            continue
        types[dtype] = name

    data: list[tuple[str, int, str, str, str]] = []
    seen_data: set[tuple[str, str]] = set()
    for row in data_rows:
        dtype = str(row[1])
        label = str(row[2])
        value = str(row[3])
        sort = int(row[4])
        color = str(row[11] or "default")
        if dtype in SKIP_TYPES:
            continue
        key = (dtype, value)
        if key in seen_data:
            continue
        seen_data.add(key)
        data.append((dtype, sort, label, value, color))
        if dtype not in types:
            types[dtype] = dtype.replace("dict_", "").replace("_", " ")

    for name, dtype in CHECKLIST_TYPES:
        types[dtype] = name
    for item in CHECKLIST_DATA:
        key = (item[0], item[3])
        if key not in seen_data:
            data.append(item)
            seen_data.add(key)

    for sort, label, value in PARAM_CATEGORY_EXTRA:
        key = ("dict_param_category", value)
        if key not in seen_data:
            data.append(("dict_param_category", sort, label, value, "default"))
            seen_data.add(key)

    for sort, label, value in V176_THRESHOLD:
        key = ("dict_threshold_metric", value)
        if key not in seen_data:
            data.append(("dict_threshold_metric", sort, label, value, "default"))
            seen_data.add(key)

    for sort, label, value in COLLECT_DATA_TYPE_EXTRA:
        key = ("dict_collect_data_type", value)
        if key not in seen_data:
            data.append(("dict_collect_data_type", sort, label, value, "default"))
            seen_data.add(key)

    parts: list[str] = [
        "-- =============================================================================",
        "-- System DB (shenyu-system) — Ops baseline dict_* (G-DICT-01 / ADR-047)",
        f"-- Generated: {date.today().isoformat()} by _gen_04_baseline_dicts.py — do not hand-edit",
        "-- Sources:",
        "--   docs/delivery/e2e-artifacts/B-WP4-ARCHIVE-20260731/backup/wd-q1-candidates-20260731.sql",
        "--   CHECKLIST-M1 / GLOBAL-CONVENTIONS (dict_ip_group_type/status, dict_author_status, dict_anchor_type)",
        "--   apply_v171_param_category.py · apply_v176_threshold_metric.py",
        "--   ADR-067 live collect + M10-EXTERNAL slice §4.2 EXT_*",
        "-- Target: {{SYSTEM_DB_HOST}}/{{SYSTEM_DB_NAME}}  (Football system_dict_*; status 0=enabled)",
        "-- Idempotent: INSERT … WHERE NOT EXISTS",
        "-- Excludes: dict_xx1 (test). Work-task 4 types + LIVE_DRAIN remain in 05/06.",
        "-- =============================================================================",
        "SET NAMES utf8mb4;",
        "",
        "-- ----- dict types -----",
        "",
    ]

    for dtype in sorted(types):
        parts.append(type_insert(types[dtype], dtype))

    parts.append("-- ----- dict data -----")
    parts.append("")
    data.sort(key=lambda r: (r[0], r[1], r[3]))
    for dtype, sort, label, value, color in data:
        parts.append(data_insert(sort, label, value, dtype, color))

    OUT.write_text("\n".join(parts) + "\n", encoding="utf-8")
    print(f"Wrote {OUT.relative_to(ROOT)} types={len(types)} data={len(data)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
