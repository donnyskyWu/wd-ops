#!/usr/bin/env python3
"""Detailed P1 API probe — outputs JSON for acceptance report."""
from __future__ import annotations

import csv
import json
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CSV_PATH = ROOT / "docs/delivery/oa-menu-permission-map.csv"
GATEWAY = "http://localhost:48080"
MODULES = {"绩效核算", "财务管理", "账号管理"}

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


def login() -> tuple[str | None, dict]:
    url = f"{GATEWAY}/admin-api/system/auth/login"
    body = json.dumps({"username": "admin", "password": "admin123", "captchaVerification": ""}).encode()
    req = urllib.request.Request(url, data=body, headers={"tenant-id": "1", "Content-Type": "application/json"}, method="POST")
    with urllib.request.urlopen(req, timeout=30) as resp:
        r = json.loads(resp.read())
    if r.get("code") != 0:
        return None, r
    return r["data"]["accessToken"], r["data"]


def summarize_data(data) -> str:
    if data is None:
        return "null"
    if isinstance(data, list):
        return f"list[{len(data)}]"
    if isinstance(data, dict):
        parts = []
        for k, v in list(data.items())[:5]:
            if isinstance(v, list):
                parts.append(f"{k}:list[{len(v)}]")
            elif isinstance(v, (int, float, str, bool)):
                parts.append(f"{k}:{v!r}")
            else:
                parts.append(f"{k}:{type(v).__name__}")
        return "{" + ", ".join(parts) + "}"
    return type(data).__name__


def probe_api(path: str, token: str) -> dict:
    url = f"{GATEWAY}{path}"
    headers = {"Authorization": f"Bearer {token}", "X-Tenant-Id": "1", "tenant-id": "1"}
    req = urllib.request.Request(url, headers=headers, method="GET")
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            r = json.loads(resp.read())
        code = r.get("code")
        data = r.get("data")
        sensible = code == 0 and data is not None
        return {
            "ok": sensible,
            "code": code,
            "msg": r.get("msg"),
            "data_summary": summarize_data(data),
        }
    except Exception as ex:
        return {"ok": False, "code": None, "msg": str(ex), "data_summary": ""}


def main() -> None:
    rows = [r for r in csv.DictReader(CSV_PATH.open(encoding="utf-8")) if r.get("parent_group") in MODULES and r.get("hide_in_menu", "").upper() != "Y"]
    token, login_meta = login()
    if not token:
        print(json.dumps({"error": "login failed", "login_meta": login_meta}, ensure_ascii=False, indent=2))
        return

    results = []
    for row in rows:
        fpath = row["football_path"]
        api_path = PAGE_API.get(fpath, "")
        probe = probe_api(api_path, token) if api_path else {"ok": False, "code": None, "msg": "no mapping", "data_summary": ""}
        results.append({
            "module": row["parent_group"],
            "menu": row["menu_title"],
            "route": fpath,
            "api": api_path,
            **probe,
        })

    passed = sum(1 for r in results if r["ok"])
    print(json.dumps({
        "auth": "gateway-login (Football OAuth2 token)",
        "userId": login_meta.get("userId"),
        "pages": len(results),
        "api_pass": passed,
        "api_fail": len(results) - passed,
        "results": results,
    }, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
