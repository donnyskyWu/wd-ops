#!/usr/bin/env python3
"""
Generate additive-only system_menu sync from shenyu-system0708.sql dump.

Compares Football menus in the dump against:
  - Ops reserved block 6100-6999 (from seed-oa-system-menu.sql)
  - Optional baseline dump (shenyu-system.sql) when present

Outputs:
  - ops-platform-server/.../db/migration/V137__sync_shenyu_system_menus.sql
  - scripts/integration-config/sync-shenyu-system-menus-standalone.sql
"""
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DUMP_LATEST = ROOT / "docs/sql/shenyu-system0708.sql"
DUMP_BASELINE = ROOT / "docs/sql/shenyu-system.sql"
OPS_SEED = ROOT / "scripts/integration-config/seed-oa-system-menu.sql"
MIGRATION_OUT = (
    ROOT
    / "ops-platform-server/ops-platform-module-oa/src/main/resources/db/migration/V137__sync_shenyu_system_menus.sql"
)
STANDALONE_OUT = ROOT / "scripts/integration-config/sync-shenyu-system-menus-standalone.sql"

OPS_ID_MIN = 6100
OPS_ID_MAX = 7000  # exclusive

INSERT_PAT = re.compile(r"INSERT INTO `system_menu` VALUES \((.+)\);?\s*$")
SEED_ID_PAT = re.compile(r"VALUES \((\d+),")


def extract_menu_lines(sql_path: Path) -> dict[int, str]:
    rows: dict[int, str] = {}
    if not sql_path.exists():
        return rows
    with sql_path.open("r", encoding="utf-8", errors="replace") as f:
        for line in f:
            if "INSERT INTO `system_menu`" not in line:
                continue
            m = INSERT_PAT.search(line.strip())
            if not m:
                continue
            id_val = int(m.group(1).split(",", 1)[0].strip())
            rows[id_val] = line.strip().rstrip(";")
    return rows


def extract_ops_seed_ids(seed_path: Path) -> set[int]:
    ids: set[int] = set()
    with seed_path.open("r", encoding="utf-8") as f:
        for line in f:
            if "INSERT INTO system_menu" not in line:
                continue
            m = SEED_ID_PAT.search(line)
            if m:
                ids.add(int(m.group(1)))
    return ids


def menu_name_from_insert(insert_line: str) -> str:
    m = re.search(r"VALUES \(\d+,\s*'([^']*)'", insert_line)
    return m.group(1) if m else ""


def topological_sort(rows: dict[int, str]) -> list[int]:
    """Parents before children (parent_id=0 first)."""
    parent_of: dict[int, int] = {}
    for mid, line in rows.items():
        m = re.search(r"VALUES \(\d+,\s*'[^']*',\s*'[^']*',\s*\d+,\s*\d+,\s*(\d+)", line)
        parent_of[mid] = int(m.group(1)) if m else 0

    ordered: list[int] = []
    seen: set[int] = set()

    def visit(nid: int) -> None:
        if nid in seen or nid == 0:
            return
        pid = parent_of.get(nid, 0)
        if pid and pid in rows and pid not in seen:
            visit(pid)
        seen.add(nid)
        ordered.append(nid)

    for nid in sorted(rows):
        visit(nid)
    return ordered


def build_migration(
    missing_rows: dict[int, str],
    *,
    source: str,
    baseline_count: int,
    latest_count: int,
    ops_seed_count: int,
) -> str:
    ordered_ids = topological_sort(missing_rows)
    sample_names = [menu_name_from_insert(missing_rows[i]) for i in ordered_ids[:12]]

    header = f"""-- V137: Sync Football system_menu from shenyu dump (additive only)
-- Source: {source}
-- Strategy: INSERT IGNORE by id; never touch Ops reserved block {OPS_ID_MIN}-{OPS_ID_MAX - 1}
-- Counts: shenyu dump={latest_count} rows; ops seed={ops_seed_count} ids (repo baseline)
-- At apply time only rows whose id is absent are inserted (safe if DB already has partial Football menus)
-- Sample Football menus: {', '.join(sample_names[:8])}{'...' if len(sample_names) > 8 else ''}
--
-- Regenerate: python scripts/sync-shenyu-system-menus.py
-- Does NOT delete or update any existing row (Ops menus preserved).

SET NAMES utf8mb4;

"""
    body_lines = []
    for mid in ordered_ids:
        insert = missing_rows[mid]
        # Convert to INSERT IGNORE
        body_lines.append(insert.replace("INSERT INTO `system_menu`", "INSERT IGNORE INTO `system_menu`", 1) + ";")

    footer = """
-- Grant new Football menus to super admin (role_id=1, tenant_id=1) when not already granted
INSERT INTO system_role_menu (role_id, menu_id, creator, tenant_id)
SELECT 1, m.id, 'menu-sync', 1
FROM system_menu m
WHERE m.id >= 1 AND m.id < 6100
  AND m.deleted = b'0'
  AND NOT EXISTS (
    SELECT 1 FROM system_role_menu rm
    WHERE rm.role_id = 1 AND rm.menu_id = m.id AND rm.tenant_id = 1 AND rm.deleted = b'0'
  );
"""
    return header + "\n".join(body_lines) + footer


def main() -> None:
    latest = extract_menu_lines(DUMP_LATEST)
    baseline = extract_menu_lines(DUMP_BASELINE)
    ops_ids = extract_ops_seed_ids(OPS_SEED)

    # Missing = in latest dump but not in baseline AND not in ops reserved range
    missing: dict[int, str] = {}
    for mid, line in latest.items():
        if OPS_ID_MIN <= mid < OPS_ID_MAX:
            continue  # never insert into ops block
        if mid in ops_ids:
            continue  # ops seed owns this id
        if mid in baseline:
            continue  # already in baseline/current football seed
        missing[mid] = line

    # If baseline has no INSERT data, treat as "all non-ops shenyu menus are candidates"
    # but still skip ids that exist only when comparing against empty baseline
    if not baseline:
        missing = {
            mid: line
            for mid, line in latest.items()
            if not (OPS_ID_MIN <= mid < OPS_ID_MAX) and mid not in ops_ids
        }

    sql = build_migration(
        missing,
        source=DUMP_LATEST.name,
        baseline_count=len(baseline),
        latest_count=len(latest),
        ops_seed_count=len(ops_ids),
    )

    MIGRATION_OUT.parent.mkdir(parents=True, exist_ok=True)
    MIGRATION_OUT.write_text(sql, encoding="utf-8")
    STANDALONE_OUT.write_text(sql, encoding="utf-8")

    print(f"shenyu dump menus: {len(latest)}")
    print(f"baseline menus ({DUMP_BASELINE.name}): {len(baseline)}")
    print(f"ops seed menu ids: {len(ops_ids)}")
    print(f"missing to add: {len(missing)}")
    print(f"migration: {MIGRATION_OUT}")
    print(f"standalone: {STANDALONE_OUT}")
    if missing:
        samples = [(i, menu_name_from_insert(missing[i])) for i in sorted(missing)[:20]]
        print("sample missing:")
        for i, n in samples:
            print(f"  {i}: {n}")


if __name__ == "__main__":
    main()
