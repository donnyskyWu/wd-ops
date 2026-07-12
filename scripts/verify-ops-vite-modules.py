#!/usr/bin/env python3
"""
Browser-level smoke: request each menu vue module from Vite dev server (:5777).
Vite returns 500 + error body when a module fails to compile.
"""
from __future__ import annotations

import csv
import sys
import urllib.error
import urllib.request
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CSV_PATH = ROOT / "docs/delivery/oa-menu-permission-map.csv"
VITE_BASE = "http://localhost:5777"


def load_rows() -> list[dict[str, str]]:
    with CSV_PATH.open(encoding="utf-8") as f:
        return list(csv.DictReader(f))


def probe_vite_module(component: str) -> tuple[bool, str]:
    # football_component e.g. ops/account/ExternalAccountAnalysis
    url = f"{VITE_BASE}/src/views/{component}.vue"
    try:
        req = urllib.request.Request(url, headers={"Accept": "*/*"})
        with urllib.request.urlopen(req, timeout=60) as resp:
            body = resp.read(2000).decode("utf-8", errors="replace")
            if resp.status == 200 and ("import" in body or "export" in body):
                return True, "OK"
            return False, f"HTTP {resp.status}"
    except urllib.error.HTTPError as e:
        snippet = e.read(500).decode("utf-8", errors="replace")
        # Extract vite error message if present
        if "Failed to resolve" in snippet:
            line = next((l for l in snippet.splitlines() if "Failed to resolve" in l), snippet[:120])
            return False, line.strip()
        if "error" in snippet.lower():
            return False, snippet[:200].replace("\n", " ")
        return False, f"HTTP {e.code}"
    except Exception as ex:
        return False, str(ex)


def main() -> int:
    rows = load_rows()
    seen: set[str] = set()
    by_module: dict[str, list[tuple[str, bool, str]]] = defaultdict(list)
    fails = 0

    print(f"=== Vite module smoke ({VITE_BASE}) ===")
    for row in rows:
        if row.get("excluded_m9", "").upper() == "Y":
            continue
        comp = row.get("football_component", "").strip()
        if not comp or comp in seen:
            continue
        seen.add(comp)
        module = row.get("parent_group", "").strip() or "(root)"
        title = row.get("menu_title", comp)
        ok, detail = probe_vite_module(comp)
        by_module[module].append((title, ok, detail))
        status = "PASS" if ok else "FAIL"
        if not ok:
            fails += 1
            print(f"  [{status}] {module}/{title}: {detail}")
        else:
            print(f"  [{status}] {module}/{title}")

    print("\n=== Per module ===")
    for module, items in sorted(by_module.items()):
        passed = sum(1 for _, ok, _ in items if ok)
        print(f"  {module}: {passed}/{len(items)} pass")

    print(f"\nTotal: {len(seen) - fails}/{len(seen)} pass, {fails} fail")
    return 0 if fails == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
