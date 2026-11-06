#!/usr/bin/env python3
"""One-off generator: shenyu-ops-drop-audit-routines.sql from submodule git SSOT."""

from __future__ import annotations

import re
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]
SUB = ROOT / "football-backend-saas"
OUT = Path(__file__).resolve().parent / "shenyu-ops-drop-audit-routines.sql"

SOURCES: list[tuple[str, str]] = [
    ("1b5c77028", "sql/mysql/updates/20261026_roi_operation_logs.sql"),
    ("HEAD", "sql/mysql/updates/20261026_roi_org_directory.sql"),
    ("HEAD", "sql/mysql/updates/20261026_roi_wecom_management_restore.sql"),
    ("HEAD", "sql/mysql/updates/roi_author_cost_foundation_20261029.sql"),
    ("HEAD", "sql/mysql/updates/roi_cost_business_classification_20261030.sql"),
]

FUNCTIONS = ("roi_audit_text_v1", "roi_audit_leaf_v1", "roi_audit_project_v1")
TABLES = ("roi_operation_log", "roi_audit_migration_state")

_TRIGGER_RE = re.compile(
    r"CREATE TRIGGER IF NOT EXISTS\s+(?:`([^`]+)`|([A-Za-z0-9_]+))",
    re.IGNORECASE,
)


def git_show(rev: str, path: str) -> str:
    proc = subprocess.run(
        ["git", "-C", str(SUB), "show", f"{rev}:{path}"],
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        check=False,
    )
    if proc.returncode != 0:
        raise RuntimeError(f"git show {rev}:{path} failed: {proc.stderr.strip()}")
    return proc.stdout


def parse_triggers(text: str) -> list[str]:
    names: list[str] = []
    for m in _TRIGGER_RE.finditer(text):
        names.append(m.group(1) or m.group(2))
    return names


def main() -> None:
    triggers: list[str] = []
    for rev, rel in SOURCES:
        triggers.extend(parse_triggers(git_show(rev, rel)))
    seen: set[str] = set()
    uniq: list[str] = []
    for name in triggers:
        if name not in seen:
            seen.add(name)
            uniq.append(name)

    lines: list[str] = [
        "-- =============================================================================",
        "-- shenyu-ops · 移除 ROI 库内操作审计（ADR-085）",
        "-- 目标库：shenyu-ops · MySQL 5.7+ / MariaDB 10.x",
        "-- 幂等：DROP … IF EXISTS；可重复执行",
        "-- Binlog 环境：触发器/函数 DROP 可能报 1419，须 DBA 先设 log_bin_trust_function_creators=1（见 DBA-1419-一次性设置.md）",
        "-- 保留：roi_audit 轻量行级表（非操作日志 UI）；RoiAuditSupport 会话变量无影响",
        "-- SSOT 来源：football-backend-saas submodule git（20261026_roi_operation_logs 等）",
        f"-- 统计：触发器 {len(uniq)} · 函数 {len(FUNCTIONS)} · 表 {len(TABLES)} · 合计 {len(uniq) + len(FUNCTIONS) + len(TABLES)}",
        "-- 生成：python docs/delivery/handover/sql-release-20261105/_gen_drop_audit_routines.py",
        "-- =============================================================================",
        "",
        "SET NAMES utf8mb4;",
        "USE `shenyu-ops`;",
        "",
        "-- Section A: 业务表 / roi_operation_log 审计触发器",
        "DELIMITER $$",
    ]
    for name in uniq:
        lines.append(f"DROP TRIGGER IF EXISTS `{name}`$$")
    lines.extend(
        [
            "DELIMITER ;",
            "",
            "-- Section B: 审计 JSON 投影函数（含 IP 投影 Section 08）",
            "DELIMITER $$",
        ]
    )
    for fn in FUNCTIONS:
        lines.append(f"DROP FUNCTION IF EXISTS `{fn}`$$")
    lines.extend(["DELIMITER ;", "", "-- Section C: 操作日志及迁移状态表"])
    for tbl in TABLES:
        lines.append(f"DROP TABLE IF EXISTS `{tbl}`;")
    lines.append("")

    OUT.write_text("\n".join(lines), encoding="utf-8")
    print(f"Wrote {OUT} triggers={len(uniq)} total={len(uniq) + len(FUNCTIONS) + len(TABLES)}")


if __name__ == "__main__":
    main()
