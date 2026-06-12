#!/usr/bin/env python3
"""Restore oa_dashboard 98601/98602 layout_json after UTF-8 double-encoding corruption."""
from __future__ import annotations

import json
import sys
import urllib.error
import urllib.request
from pathlib import Path

MODULE_ROOT = Path(__file__).resolve().parents[1]
V59 = MODULE_ROOT / "src/main/resources/db/migration/V59__seed_data_screen_dashboards.sql"
MIGRATION_OUT = (
    MODULE_ROOT / "src/main/resources/db/migration/V61__fix_data_screen_dashboard_encoding.sql"
)

API_BASE = "http://localhost:8080/admin-api/oa/dashboard-config/full-update"
HEADERS = {
    "Authorization": "Bearer dev-token-oa-admin",
    "X-Tenant-Id": "1",
    "Content-Type": "application/json; charset=utf-8",
}


def extract_layout_json(text: str, dash_id: int) -> str:
    marker = f"({dash_id}, 1, '"
    start = text.index(marker) + len(marker)
    name_end = text.index("', 'OPS', '", start)
    layout_start = name_end + len("', 'OPS', '")
    depth = 0
    i = layout_start
    while i < len(text):
        ch = text[i]
        if ch == "{":
            depth += 1
        elif ch == "}":
            depth -= 1
            if depth == 0:
                return text[layout_start : i + 1]
        i += 1
    raise ValueError(f"layout json not found for {dash_id}")


def load_seed_dashboards() -> list[tuple[int, str, dict]]:
    text = V59.read_text(encoding="utf-8")
    rows: list[tuple[int, str, dict]] = []
    for dash_id in (98601, 98602):
        marker = f"({dash_id}, 1, '"
        start = text.index(marker) + len(marker)
        name_end = text.index("', 'OPS', '", start)
        name = text[start:name_end]
        layout = json.loads(extract_layout_json(text, dash_id))
        if dash_id == 98601:
            for widget in layout["widgets"]:
                if widget.get("id") == "l1":
                    cols = widget.setdefault("columns", [])
                    if not any(c.get("key") == "trend_pct" for c in cols):
                        cols.append({"key": "trend_pct", "label": "趋势"})
        rows.append((dash_id, name, layout))
    return rows


def sql_escape(value: str) -> str:
    return value.replace("\\", "\\\\").replace("'", "''")


def write_migration(rows: list[tuple[int, str, dict]]) -> None:
    lines = [
        "-- Fix double-encoded UTF-8 in default data-screen dashboards (98601/98602).",
        "-- Root cause: PowerShell/curl wrote UTF-8 bytes misinterpreted as Latin-1 and re-saved.",
        "",
    ]
    for dash_id, name, layout in rows:
        layout_json = json.dumps(layout, ensure_ascii=False, separators=(",", ":"))
        lines.append("UPDATE oa_dashboard")
        lines.append(f"SET dashboard_name = '{sql_escape(name)}',")
        lines.append(f"    layout_json = '{sql_escape(layout_json)}',")
        lines.append("    updater = 'fix-dashboard-encoding'")
        lines.append(f"WHERE id = {dash_id};")
        lines.append("")
    MIGRATION_OUT.write_text("\n".join(lines), encoding="utf-8", newline="\n")
    print(f"Wrote migration: {MIGRATION_OUT}")


def api_fix(rows: list[tuple[int, str, dict]]) -> None:
    for dash_id, name, layout in rows:
        body = json.dumps(
            {
                "id": dash_id,
                "dashboardName": name,
                "dashboardType": "OPS",
                "layout": json.dumps(layout, ensure_ascii=False, separators=(",", ":")),
                "status": 1,
            },
            ensure_ascii=False,
        ).encode("utf-8")
        req = urllib.request.Request(API_BASE, data=body, headers=HEADERS, method="PUT")
        try:
            with urllib.request.urlopen(req, timeout=15) as resp:
                print(f"API fix {dash_id}: HTTP {resp.status}")
        except urllib.error.URLError as exc:
            print(f"API fix {dash_id} skipped (server unavailable): {exc}", file=sys.stderr)


def main() -> None:
    rows = load_seed_dashboards()
    if len(rows) != 2:
        raise SystemExit(f"Expected 2 dashboards from V59, got {len(rows)}")
    write_migration(rows)
    if "--api" in sys.argv:
        api_fix(rows)


if __name__ == "__main__":
    main()
