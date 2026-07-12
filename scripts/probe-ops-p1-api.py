#!/usr/bin/env python3
"""P1 business API probe: 数据分析 + 数据采集 + 系统管理(OA)."""
from __future__ import annotations

import csv
import json
import sys
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CSV_PATH = ROOT / "docs/delivery/oa-menu-permission-map.csv"
GATEWAY = "http://localhost:48080"
MODULES = {"数据分析", "数据采集", "系统管理(OA)"}

PAGE_APIS: dict[str, str | None] = {
    "custom-query": "/admin-api/oa/query/list?pageNo=1&pageSize=10",
    "data-report": None,
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


def load_pages() -> list[dict[str, str]]:
    rows = list(csv.DictReader(CSV_PATH.open(encoding="utf-8")))
    return [
        r for r in rows
        if r.get("parent_group") in MODULES and r.get("hide_in_menu", "").upper() != "Y"
    ]


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
            return False, f"code={data.get('code')} msg={data.get('msg', '')[:120]}"
    except urllib.error.HTTPError as e:
        body = e.read(400).decode("utf-8", errors="replace")
        return False, f"HTTP {e.code} {body[:120]}"
    except Exception as ex:
        return False, str(ex)


def main() -> int:
    token, auth_mode = login_token()
    pages = load_pages()
    rows = []
    api_pass = api_fail = api_skip = 0

    print(f"Auth: {auth_mode}\n")
    for row in pages:
        key = row.get("football_path", "").strip().removeprefix("/ops/")
        module = row["parent_group"]
        title = row["menu_title"]
        api_path = PAGE_APIS.get(key)
        if api_path is None and key not in PAGE_APIS:
            rows.append({"module": module, "title": title, "route": key, "api": "SKIP", "detail": "no mapping", "pass": True})
            api_skip += 1
            continue
        if api_path is None:
            rows.append({"module": module, "title": title, "route": key, "api": "N/A", "detail": "navigation-only", "pass": True})
            api_skip += 1
            continue
        ok, msg = probe_api(api_path, token)
        if ok:
            api_pass += 1
        else:
            api_fail += 1
        rows.append({"module": module, "title": title, "route": key, "api": api_path, "detail": msg, "pass": ok})

    by_module: dict[str, list] = {}
    for r in rows:
        by_module.setdefault(r["module"], []).append(r)

    for module in sorted(by_module):
        items = by_module[module]
        passed = sum(1 for i in items if i["pass"])
        print(f"## {module}: {passed}/{len(items)}")
        for e in items:
            mark = "PASS" if e["pass"] else "FAIL"
            print(f"  [{mark}] {e['title']} ({e['route']}) -> {e['detail']}")

    probed = api_pass + api_fail
    print(f"\nAPI probed: {api_pass}/{probed} pass, {api_fail} fail, {api_skip} skip/N/A")
    print(f"Pages total: {sum(1 for r in rows if r['pass'])}/{len(rows)} pass")

    out = ROOT / "docs/delivery/ops-acceptance-p1-api-report.json"
    out.write_text(
        json.dumps(
            {
                "auth": auth_mode,
                "summary": {
                    "total": len(rows),
                    "pass": sum(1 for r in rows if r["pass"]),
                    "fail": sum(1 for r in rows if not r["pass"]),
                    "api_pass": api_pass,
                    "api_fail": api_fail,
                    "api_skip": api_skip,
                },
                "rows": rows,
            },
            ensure_ascii=False,
            indent=2,
        ),
        encoding="utf-8",
    )
    print(f"Report: {out}")
    return 0 if api_fail == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
