#!/usr/bin/env python3
"""Acceptance probe: 数据分析 + 数据采集 + 系统管理(OA)."""
from __future__ import annotations

import csv
import json
import re
import sys
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CSV_PATH = ROOT / "docs/delivery/oa-menu-permission-map.csv"
FF = ROOT / "football-front/apps/web-ele/src"
GATEWAY = "http://localhost:48080"
VITE = "http://localhost:5777"
MODULES = {"数据分析", "数据采集", "系统管理(OA)"}

PAGE_APIS: dict[str, str] = {
    "custom-query": "/admin-api/oa/query/list?pageNo=1&pageSize=10",
    "financial-analysis": "/admin-api/oa/finance/roi/analysis?startDate=2026-01-01&endDate=2026-06-30",
    "funnel-analysis": "/admin-api/oa/funnel/list?pageNo=1&pageSize=10",
    "metric": "/admin-api/oa/metric/list?pageNo=1&pageSize=10",
    "metric-analysis": "/admin-api/oa/metric/list?pageNo=1&pageSize=10",
    "screen": "/admin-api/oa/dashboard/98601/data?startDate=2026-06-26&endDate=2026-07-03",
    "screen-config": "/admin-api/oa/dashboard-config/list?pageNum=1&pageSize=10",
    "collect/log": "/admin-api/oa/collect/log/page?pageNo=1&pageSize=10",
    "collect/private-domain-bridge": "/admin-api/oa/collect/private-domain-bridge/page?pageNo=1&pageSize=10",
    "collect/quality": "/admin-api/oa/collect/quality/list?pageNo=1&pageSize=10",
    "collect/task": "/admin-api/oa/collect/task/page?pageNo=1&pageSize=10",
    "system-dict": "/admin-api/oa/system/dict/list?pageNo=1&pageSize=10",
    "system-log/login": "/admin-api/oa/system/log/login?pageNo=1&pageSize=10",
    "system-log/operation": "/admin-api/oa/system/log/operation?pageNo=1&pageSize=10",
    "system-message": "/admin-api/oa/system/message/list?pageNo=1&pageSize=10",
    "system-param": "/admin-api/oa/system/param/list?pageNo=1&pageSize=10",
}

BG_LIGHT_RE = re.compile(r"background(-color)?\s*:\s*(#fff(f{2})?|white|#f[0-9a-f]{5})\b", re.I)


def load_pages() -> list[dict[str, str]]:
    rows = list(csv.DictReader(CSV_PATH.open(encoding="utf-8")))
    return [
        r for r in rows
        if r.get("parent_group") in MODULES and r.get("hide_in_menu", "").upper() != "Y"
    ]


def route_key(row: dict[str, str]) -> str:
    return row.get("football_path", "").strip().removeprefix("/ops/")


def probe_vite(comp: str) -> tuple[bool, str]:
    url = f"{VITE}/src/views/{comp}.vue"
    try:
        req = urllib.request.Request(url, headers={"Accept": "*/*"})
        with urllib.request.urlopen(req, timeout=90) as resp:
            body = resp.read(4000).decode("utf-8", errors="replace")
            if resp.status == 200 and ("import" in body or "export" in body):
                return True, "OK"
            return False, f"HTTP {resp.status}"
    except urllib.error.HTTPError as e:
        snippet = e.read(800).decode("utf-8", errors="replace")
        for line in snippet.splitlines():
            if "Failed to resolve" in line:
                return False, line.strip()[:180]
        return False, snippet[:180].replace("\n", " ")
    except Exception as ex:
        return False, str(ex)


def login_token() -> tuple[str, str]:
    body = json.dumps({"username": "admin", "password": "admin123", "captchaVerification": ""}).encode()
    req = urllib.request.Request(
        f"{GATEWAY}/admin-api/system/auth/login",
        data=body,
        headers={"Content-Type": "application/json", "tenant-id": "1"},
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=20) as resp:
            data = json.loads(resp.read().decode())
            if data.get("code") == 0:
                return data["data"]["accessToken"], "gateway-login"
    except Exception:
        pass
    return "dev-token-oa-admin", "dev-token"


def probe_api(path: str, token: str) -> tuple[bool, str]:
    url = f"{GATEWAY}{path}"
    req = urllib.request.Request(
        url,
        headers={"Authorization": f"Bearer {token}", "X-Tenant-Id": "1", "tenant-id": "1"},
    )
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            data = json.loads(resp.read().decode())
            if data.get("code") == 0:
                return True, "code=0"
            return False, f"code={data.get('code')} msg={data.get('msg', '')[:80]}"
    except urllib.error.HTTPError as e:
        body = e.read(400).decode("utf-8", errors="replace")
        return False, f"HTTP {e.code} {body[:80]}"
    except Exception as ex:
        return False, str(ex)


def check_theme(comp: str) -> tuple[bool, list[str]]:
    path = FF / "views" / f"{comp}.vue"
    if not path.exists():
        return False, ["file missing"]
    text = path.read_text(encoding="utf-8")
    issues: list[str] = []
    if "ops-page" not in text:
        issues.append("missing ops-page class")
    for m in re.finditer(r"<style[^>]*>([\s\S]*?)</style>", text):
        for hit in BG_LIGHT_RE.findall(m.group(1)):
            issues.append(f"style hardcoded bg: {hit[0] if isinstance(hit, tuple) else hit}")
    for m in re.finditer(r'style="([^"]*)"', text):
        if BG_LIGHT_RE.search(m.group(1)):
            issues.append(f"inline hardcoded bg: {m.group(1)[:50]}")
    return len(issues) == 0, issues


def main() -> int:
    token, auth_mode = login_token()
    print(f"Auth: {auth_mode}")

    pages = load_pages()
    by_module: dict[str, list] = {}
    fails = 0

    for row in pages:
        module = row["parent_group"]
        key = route_key(row)
        comp = row["football_component"]
        title = row["menu_title"]

        vite_ok, vite_msg = probe_vite(comp)
        theme_ok, theme_issues = check_theme(comp)
        if key in PAGE_APIS:
            api_ok, api_msg = probe_api(PAGE_APIS[key], token)
        else:
            api_ok, api_msg = True, "N/A"

        page_ok = vite_ok and theme_ok and api_ok
        if not page_ok:
            fails += 1

        by_module.setdefault(module, []).append({
            "title": title, "route": key, "pass": page_ok,
            "vite": vite_msg if not vite_ok else "OK",
            "api": api_msg if not api_ok else "OK",
            "theme": ", ".join(theme_issues) if theme_issues else "OK",
        })

    for module in sorted(by_module):
        items = by_module[module]
        passed = sum(1 for i in items if i["pass"])
        print(f"\n## {module}: {passed}/{len(items)} PASS")
        for e in items:
            mark = "PASS" if e["pass"] else "FAIL"
            print(f"  [{mark}] {e['title']} ({e['route']})")
            if not e["pass"]:
                if e["vite"] != "OK":
                    print(f"         route: {e['vite']}")
                if e["api"] != "OK":
                    print(f"         api: {e['api']}")
                if e["theme"] != "OK":
                    print(f"         theme: {e['theme']}")

    print(f"\nTotal: {len(pages) - fails}/{len(pages)} pass")
    return 0 if fails == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
