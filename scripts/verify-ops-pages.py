#!/usr/bin/env python3
"""
Ops integration smoke verification (S4 Football × Ops).

Checks:
  1. Vue SFC structure (root <template>, no @/ imports)
  2. Menu component files exist per oa-menu-permission-map.csv
  3. Optional: login + OA API probes via Gateway :48080

Usage:
  python scripts/verify-ops-pages.py
  python scripts/verify-ops-pages.py --api          # include API smoke
  python scripts/verify-ops-pages.py --api --base http://localhost:48080
"""
from __future__ import annotations

import argparse
import csv
import json
import re
import sys
import urllib.error
import urllib.request
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
FF = ROOT / "football-front/apps/web-ele/src"
VIEWS = FF / "views/ops"
CSV_PATH = ROOT / "docs/delivery/oa-menu-permission-map.csv"

TEMPLATE_ROOT = re.compile(r"^\s*<template[\s>]")
LEADING_COMMENTS = re.compile(r"^(\s*(?:<!--[\s\S]*?-->\s*)*)")

# Sample list API per module (first menu route in group) for --api mode
MODULE_API_PROBE: dict[str, tuple[str, str]] = {
    "作品监测": ("GET", "/admin-api/oa/content-analysis/list?pageNo=1&pageSize=1"),
    "内容生产": ("GET", "/admin-api/oa/content/list?pageNo=1&pageSize=1"),
    "数据分析": ("GET", "/admin-api/oa/metric/list?pageNo=1&pageSize=1"),
    "数据采集": ("GET", "/admin-api/oa/collect/task/list?pageNo=1&pageSize=1"),
    "系统管理(OA)": ("GET", "/admin-api/oa/system/dict/list?pageNo=1&pageSize=10"),
    "绩效核算": ("GET", "/admin-api/oa/perf/template/list?pageNo=1&pageSize=1"),
    "财务管理": ("GET", "/admin-api/oa/finance/cost/list?pageNo=1&pageSize=1"),
    "账号管理": ("GET", "/admin-api/oa/company/list?pageNo=1&pageSize=1"),
    "运营管理": ("GET", "/admin-api/oa/ip-group/list?pageNo=1&pageSize=1"),
    "配置管理": ("GET", "/admin-api/oa/config/ai-model/list?pageNo=1&pageSize=1"),
}


def load_menu_rows() -> list[dict[str, str]]:
    with CSV_PATH.open(encoding="utf-8") as f:
        return list(csv.DictReader(f))


def scan_vue_structure() -> list[str]:
    issues: list[str] = []
    for f in sorted(VIEWS.rglob("*.vue")):
        rel = str(f.relative_to(VIEWS))
        t = f.read_text(encoding="utf-8")
        stripped = LEADING_COMMENTS.sub("", t, count=1)
        if not TEMPLATE_ROOT.match(stripped):
            issues.append(f"NO_ROOT_TEMPLATE: {rel}")
        if "@/" in t:
            issues.append(f"BARE_AT_IMPORT: {rel}")
        if t.count("<template>") > 1:
            head = t[:600]
            if head.count("<template>") > 1:
                issues.append(f"DUPLICATE_ROOT_TEMPLATE: {rel}")
    return issues


def scan_menu_components(rows: list[dict[str, str]]) -> tuple[list[str], dict[str, list[str]]]:
    issues: list[str] = []
    by_module: dict[str, list[str]] = defaultdict(list)
    seen: set[str] = set()
    for row in rows:
        if row.get("excluded_m9", "").upper() == "Y":
            continue
        comp = row.get("football_component", "").strip()
        if not comp or comp in seen:
            continue
        seen.add(comp)
        module = row.get("parent_group", "unknown").strip()
        vue_path = FF / "views" / f"{comp}.vue"
        by_module[module].append(comp)
        if not vue_path.exists():
            issues.append(f"MISSING_COMPONENT: {comp} ({row.get('menu_title', '')})")
    return issues, by_module


def http_json(method: str, url: str, headers: dict[str, str], body: dict | None = None) -> dict:
    data = json.dumps(body).encode("utf-8") if body is not None else None
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    if body is not None:
        req.add_header("Content-Type", "application/json")
    with urllib.request.urlopen(req, timeout=30) as resp:
        return json.loads(resp.read().decode("utf-8"))


def login(base: str, tenant_id: str = "1") -> str | None:
    url = f"{base}/admin-api/system/auth/login"
    headers = {"tenant-id": tenant_id, "Content-Type": "application/json"}
    body = {"username": "admin", "password": "admin123", "captchaVerification": ""}
    try:
        r = http_json("POST", url, headers, body)
        if r.get("code") == 0 and r.get("data", {}).get("accessToken"):
            return r["data"]["accessToken"]
        print(f"  LOGIN FAIL code={r.get('code')} msg={r.get('msg')}")
    except Exception as e:
        print(f"  LOGIN ERROR: {e}")
    return None


def probe_apis(base: str, token: str, tenant_id: str = "1") -> dict[str, bool]:
    headers = {
        "Authorization": f"Bearer {token}",
        "X-Tenant-Id": tenant_id,
        "tenant-id": tenant_id,
    }
    results: dict[str, bool] = {}
    for module, (method, path) in MODULE_API_PROBE.items():
        url = f"{base}{path}"
        try:
            r = http_json(method, url, headers)
            ok = r.get("code") == 0
            results[module] = ok
            status = "PASS" if ok else f"FAIL code={r.get('code')}"
            print(f"  [{status}] API {module}: {path}")
        except Exception as e:
            results[module] = False
            print(f"  [FAIL] API {module}: {e}")
    return results


def main() -> int:
    parser = argparse.ArgumentParser(description="Verify Ops pages integration")
    parser.add_argument("--api", action="store_true", help="Run Gateway login + OA API probes")
    parser.add_argument("--base", default="http://localhost:48080", help="Gateway base URL")
    args = parser.parse_args()

    rows = load_menu_rows()
    print("=== Ops Vue structure scan ===")
    vue_issues = scan_vue_structure()
    if vue_issues:
        for i in vue_issues:
            print(f"  FAIL {i}")
    else:
        print(f"  PASS all {len(list(VIEWS.rglob('*.vue')))} vue files")

    print("\n=== Menu component files ===")
    comp_issues, by_module = scan_menu_components(rows)
    module_stats: dict[str, dict[str, int]] = {}
    for module, comps in sorted(by_module.items()):
        missing = [c for c in comps if not (FF / "views" / f"{c}.vue").exists()]
        passed = len(comps) - len(missing)
        module_stats[module] = {"pass": passed, "fail": len(missing), "total": len(comps)}
        status = "PASS" if not missing else "FAIL"
        print(f"  [{status}] {module}: {passed}/{len(comps)} components")
        for m in missing:
            print(f"    MISSING {m}")

    api_stats: dict[str, bool] = {}
    if args.api:
        print(f"\n=== API smoke ({args.base}) ===")
        token = login(args.base)
        if token:
            print("  PASS login")
            api_stats = probe_apis(args.base, token)
        else:
            print("  SKIP API probes (login failed)")

    print("\n=== Summary ===")
    total_routes = sum(s["total"] for s in module_stats.values())
    total_pass = sum(s["pass"] for s in module_stats.values())
    print(f"Vue structure: {0 if vue_issues else len(list(VIEWS.rglob('*.vue')))} pass, {len(vue_issues)} fail")
    print(f"Menu components: {total_pass}/{total_routes} pass")
    if api_stats:
        api_pass = sum(1 for v in api_stats.values() if v)
        print(f"API modules: {api_pass}/{len(api_stats)} pass")

    fail_count = len(vue_issues) + len(comp_issues)
    if args.api and api_stats:
        fail_count += sum(1 for v in api_stats.values() if not v)
    return 0 if fail_count == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
