#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Restore system_menu.name corrupted to literal '?' (0x3F) by non-utf8 import.

Sources (merged, utf-8 Chinese names):
  - import-football-system-tables.sql
  - V137__sync_shenyu_system_menus.sql
  - sync-shenyu-system-menus-standalone.sql

Only updates rows whose stored name still looks corrupted (contains '?').
Idempotent: already-correct / non-'?' names are left alone.
OPS menus (id>=6100) from apply-seed-oa-menu.py are normally already OK.
"""
from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

import pymysql

ROOT = Path(__file__).resolve().parents[2]
DEFAULT_SEEDS = [
    ROOT / "scripts/integration-config/import-football-system-tables.sql",
    ROOT
    / "ops-platform-server/ops-platform-module-oa/src/main/resources/db/migration"
    / "V137__sync_shenyu_system_menus.sql",
    ROOT / "scripts/integration-config/sync-shenyu-system-menus-standalone.sql",
    ROOT / "docs/sql/shenyu-system.sql",
]

# Columnar INSERT: INSERT INTO `system_menu` (...) VALUES (id, 'name', ...)
INSERT_COLS_PAT = re.compile(
    r"INSERT(?:\s+IGNORE)?\s+INTO\s+`?system_menu`?\s*\(.*?\)\s*VALUES\s*\((\d+),\s*'((?:\\'|[^'])*)'",
    re.IGNORECASE,
)
# Positional INSERT: INSERT IGNORE INTO `system_menu` VALUES (id, 'name', ...)
INSERT_POS_PAT = re.compile(
    r"INSERT(?:\s+IGNORE)?\s+INTO\s+`?system_menu`?\s+VALUES\s*\((\d+),\s*'((?:\\'|[^'])*)'",
    re.IGNORECASE,
)


def parse_names(sql_path: Path) -> dict[int, str]:
    text = sql_path.read_text(encoding="utf-8")
    names: dict[int, str] = {}
    for pat in (INSERT_COLS_PAT, INSERT_POS_PAT):
        for m in pat.finditer(text):
            menu_id = int(m.group(1))
            name = m.group(2).replace("\\'", "'")
            # Prefer first non-corrupted name; later seeds may override if better
            if menu_id not in names or looks_corrupted(names[menu_id]):
                if not looks_corrupted(name):
                    names[menu_id] = name
    return names


def parse_names_from_seeds(seeds: list[Path]) -> dict[int, str]:
    names: dict[int, str] = {}
    for seed in seeds:
        if not seed.is_file():
            print(f"skip missing seed: {seed}")
            continue
        parsed = parse_names(seed)
        print(f"  {seed.name}: {len(parsed)} names")
        for menu_id, name in parsed.items():
            if menu_id not in names or looks_corrupted(names[menu_id]):
                names[menu_id] = name
    return names


def looks_corrupted(name: str | None) -> bool:
    if name is None:
        return False
    return "?" in name or "？" in name


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--host", default="localhost")
    parser.add_argument("--port", type=int, default=3306)
    parser.add_argument("--user", default="root")
    parser.add_argument("--password", default="root")
    parser.add_argument("--database", default="shenyu-system")
    parser.add_argument(
        "--seed",
        type=Path,
        action="append",
        default=None,
        help="SQL seed path (repeatable). Default: import + V137 + standalone + docs dump",
    )
    args = parser.parse_args()

    seeds = args.seed if args.seed else DEFAULT_SEEDS
    print("Parsing menu names from seeds:")
    names = parse_names_from_seeds(seeds)
    if not names:
        print("No menu names parsed from seeds", file=sys.stderr)
        return 1
    print(f"Merged unique names: {len(names)}")

    conn = pymysql.connect(
        host=args.host,
        port=args.port,
        user=args.user,
        password=args.password,
        database=args.database,
        charset="utf8mb4",
        autocommit=False,
    )
    updated = 0
    skipped_ok = 0
    missing_in_db = 0
    try:
        with conn.cursor() as cur:
            for menu_id, good_name in names.items():
                cur.execute(
                    "SELECT name FROM system_menu WHERE id=%s AND deleted=0",
                    (menu_id,),
                )
                row = cur.fetchone()
                if row is None:
                    missing_in_db += 1
                    continue
                current = row[0]
                if current == good_name:
                    skipped_ok += 1
                    continue
                if not looks_corrupted(current):
                    # Different but not '?' — leave alone (manual edits / newer seed)
                    skipped_ok += 1
                    continue
                cur.execute(
                    "UPDATE system_menu SET name=%s WHERE id=%s AND deleted=0",
                    (good_name, menu_id),
                )
                updated += cur.rowcount
        conn.commit()
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()

    print(
        f"updated={updated} already_ok_or_skipped={skipped_ok} "
        f"missing_in_db={missing_in_db}"
    )

    # Verify key menus
    conn = pymysql.connect(
        host=args.host,
        port=args.port,
        user=args.user,
        password=args.password,
        database=args.database,
        charset="utf8mb4",
    )
    try:
        with conn.cursor() as cur:
            cur.execute(
                "SELECT id, name, HEX(name) FROM system_menu "
                "WHERE id IN (1,2,100,102,6100) ORDER BY id"
            )
            for row in cur.fetchall():
                print(row)
            cur.execute(
                "SELECT COUNT(*) FROM system_menu "
                "WHERE deleted=0 AND name LIKE '%%?%%'"
            )
            print("remaining_with_qmark", cur.fetchone()[0])
    finally:
        conn.close()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
