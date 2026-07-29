#!/usr/bin/env python3
"""Generate V164 / repair SQL for corrupted OPS system_menu names."""
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SEED = ROOT / "scripts/integration-config/seed-oa-system-menu.sql"
OUT = (
    ROOT
    / "ops-platform-server/ops-platform-module-oa/src/main/resources/db/migration/V164__repair_ops_system_menu_charset.sql"
)

PATTERN = re.compile(r"INSERT INTO system_menu .*? VALUES \((\d+), '([^']*)'", re.MULTILINE)


def main() -> None:
    seed = SEED.read_text(encoding="utf-8")
    lines = [
        "-- V164: Repair OPS system_menu charset in shenyu-system",
        "-- Root cause: seed ran without SET NAMES utf8mb4; Chinese stored as literal '?' (0x3F).",
        "-- Re-seed: python scripts/integration-config/apply-seed-oa-menu.py --host ... --database shenyu-system",
        "",
        "SET NAMES utf8mb4;",
        "",
    ]
    count = 0
    for mid, name in PATTERN.findall(seed):
        name_esc = name.replace("'", "''")
        lines.append(
            f"UPDATE `shenyu-system`.system_menu SET name = '{name_esc}', "
            f"updater = 'v164', update_time = NOW() "
            f"WHERE id = {mid} AND deleted = b'0' "
            f"AND (name REGEXP '^[?]+$' OR name LIKE '%?%');"
        )
        count += 1
    lines.append("")
    lines.append(f"-- total: {count} conditional menu name repairs")
    OUT.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"Wrote {count} UPDATEs -> {OUT.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
