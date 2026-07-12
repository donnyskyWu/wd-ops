#!/usr/bin/env python3
"""Acceptance probe for perf+finance+account menu pages."""
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
VIEWS = ROOT / "football-front/apps/web-ele/src/views"
VITE_BASE = "http://localhost:5777"
GATEWAY = "http://localhost:48080"
MODULES = {"绩效核算", "财务管理", "账号管理"}

# Primary list/load API per menu page (football_path -> GET path)
PAGE_API: dict[str, str] = {
    "/ops/order-attribution": "/admin-api/oa/football-order/list?pageNum=1&pageSize=1&startDate=2026-01-01&endDate=2026-07-03",
    "/ops/perf-execution": "/admin-api/oa/perf/record/list?pageNum=1&pageSize=1",
    "/ops/perf-result": "/admin-api/oa/perf/result/list?pageNum=1&pageSize=1",
    "/ops/perf-template": "/admin-api/oa/perf/template/list?pageNum=1&pageSize=1",
    "/ops/account-cost": "/admin-api/oa/finance/cost/list?pageNo=1&pageSize=1",
    "/ops/roi-analysis": "/admin-api/oa/finance/roi/analysis?startDate=2026-01-01&endDate=2026-07-03&dimension=IP_GROUP",
    "/ops/company": "/admin-api/oa/company/list?pageNo=1&pageSize=1",
    "/ops/internal-account": "/admin-api/oa/account/list?pageNo=1&pageSize=1",
    "/ops/personal-account": "/admin-api/oa/internal/personal-account/list?pageNo=1&pageSize=1",
    "/ops/phone": "/admin-api/oa/phone/list?pageNo=1&pageSize=1",
    "/ops/realname": "/admin-api/oa/realname/list?pageNo=1&pageSize=1",
    "/ops/simcard": "/admin-api/oa/sim-card/list?pageNo=1&pageSize=1",
}

FFF_RE = re.compile(r"(background(-color)?\s*:\s*(#fff(f{2})?|white)\b|color\s*:\s*#fff(f{2})?\b)", re.I)


def load_pages() -> list[dict[str, str]]:
    rows = list(csv.DictReader(CSV_PATH.open(encoding="utf-8")))
    return [r for r in rows if r.get("parent_group") in MODULES and r.get("hide_in_menu", "").upper() != "Y"]


def probe_vite(comp: str) -> tuple[bool, str]:
    url = f"{VITE_BASE}/src/views/{comp}.vue"
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
            if "Failed to resolve" in line or "error" in line.lower():
                return False, line.strip()[:200]
        return False, f"HTTP {e.code}"
    except Exception as ex:
        return False, str(ex)


def http_json(method: str, url: str, headers: dict[str, str]) -> dict:
    req = urllib.request.Request(url, headers=headers, method=method)
    with urllib.request.urlopen(req, timeout=30) as resp:
        return json.loads(resp.read().decode("utf-8"))


def login() -> str | None:
    url = f"{GATEWAY}/admin-api/system/auth/login"
    headers = {"tenant-id": "1", "Content-Type": "application/json"}
    body = json.dumps({"username": "admin", "password": "admin123", "captchaVerification": ""}).encode()
    req = urllib.request.Request(url, data=body, headers=headers, method="POST")
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            r = json.loads(resp.read().decode("utf-8"))
            if r.get("code") == 0:
                return r["data"]["accessToken"]
            print(f"login fail code={r.get('code')} msg={r.get('msg')}")
    except Exception as e:
        print(f"login error: {e}")
    return None


def probe_api(path: str, token: str) -> tuple[bool, str]:
    url = f"{GATEWAY}{path}"
    headers = {
        "Authorization": f"Bearer {token}",
        "X-Tenant-Id": "1",
        "tenant-id": "1",
    }
    try:
        r = http_json("GET", url, headers)
        if r.get("code") == 0:
            return True, "OK"
        return False, f"code={r.get('code')} msg={r.get('msg')}"
    except Exception as e:
        return False, str(e)


def check_theme(comp: str) -> tuple[bool, str]:
    vue = VIEWS / f"{comp}.vue"
    if not vue.exists():
        return False, "file missing"
    text = vue.read_text(encoding="utf-8")
    if "ops-page" not in text:
        return False, "missing ops-page class"
    # Allow chart borderColor: '#fff' in echarts itemStyle only
    cleaned = re.sub(r"borderColor:\s*['\"]#fff['\"]", "", text)
    cleaned = re.sub(r"color:\s*#409eff", "", cleaned)  # el primary accent ok
    if FFF_RE.search(cleaned):
        m = FFF_RE.search(cleaned)
        return False, f"hardcoded light color: {m.group(0) if m else '?'}"
    return True, "OK"


def main() -> int:
    pages = load_pages()
    token = login()
    if not token:
        print("WARN: login failed, API checks will fail")

    results = []
    print("=== Acceptance probe ===")
    for row in pages:
        title = row["menu_title"]
        comp = row["football_component"]
        fpath = row["football_path"]
        vite_ok, vite_msg = probe_vite(comp)
        api_ok, api_msg = (False, "no token") if not token else probe_api(PAGE_API.get(fpath, ""), token) if fpath in PAGE_API else (False, "no mapping")
        theme_ok, theme_msg = check_theme(comp)
        results.append({
            "module": row["parent_group"],
            "title": title,
            "path": fpath,
            "component": comp,
            "vite": vite_ok,
            "vite_msg": vite_msg,
            "api": api_ok,
            "api_msg": api_msg,
            "theme": theme_ok,
            "theme_msg": theme_msg,
        })
        v = "PASS" if vite_ok else "FAIL"
        a = "PASS" if api_ok else "FAIL"
        t = "PASS" if theme_ok else "FAIL"
        print(f"  [{v}/{a}/{t}] {title} | vite:{vite_msg} api:{api_msg} theme:{theme_msg}")

    checks = len(results) * 3
    passed = sum(r["vite"] + r["api"] + r["theme"] for r in results)
    print(f"\nTotal checks: {passed}/{checks} pass ({checks - passed} fail)")
    return 0 if passed == checks else 1


if __name__ == "__main__":
    sys.exit(main())
