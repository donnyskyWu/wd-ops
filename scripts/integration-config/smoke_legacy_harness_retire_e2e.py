#!/usr/bin/env python3
"""Smoke ops APIs after legacy sys_* harness retirement (S1-S5)."""
from __future__ import annotations

import json
import sys
import urllib.error
import urllib.request

BASE = "http://localhost:48080/admin-api"


def req(method: str, path: str, body=None, token=None):
    url = BASE + path
    headers = {"Content-Type": "application/json", "tenant-id": "1", "X-Tenant-Id": "1"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    data = json.dumps(body).encode("utf-8") if body is not None else None
    request = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request, timeout=30) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", errors="replace")
        try:
            payload = json.loads(raw)
            payload["_httpStatus"] = e.code
            return payload
        except json.JSONDecodeError:
            return {"code": e.code, "msg": raw, "data": None, "_httpStatus": e.code}


def main() -> int:
    login = req("POST", "/system/auth/login", {"username": "admin", "password": "admin123"})
    if login.get("code") != 0:
        print("LOGIN FAIL", login)
        return 1
    token = login["data"]["accessToken"]
    print("LOGIN OK")

    ip_group_id = None
    ip_groups = req("GET", "/ops/ip-group/list", token=token)
    if ip_groups.get("code") == 0:
        lst = (ip_groups.get("data") or {}).get("list") or []
        if lst:
            ip_group_id = lst[0].get("id")

    checks = [
        ("author/list", "GET", "/ops/author/list?page=1&size=5&status=1"),
        ("ip-group/list", "GET", "/ops/ip-group/list"),
        ("report/monthly", "GET", "/ops/private-domain-report/monthly-achievement?month=2026-08"),
        (
            "report/weekly",
            "GET",
            "/ops/private-domain-report/weekly-funnel?weekStart=2026-08-11&weekEnd=2026-08-17",
        ),
        ("live-duration", "GET", "/ops/report/live-duration/list?pageNum=1&pageSize=5&startDate=2026-08-01&endDate=2026-08-25"),
    ]
    if ip_group_id is not None:
        checks.append(
            (
                "work-task/sheet",
                "GET",
                f"/ops/work-task/sheet/get-or-create?ipGroupId={ip_group_id}&workDate=2026-08-25",
            )
        )
    else:
        print("[SKIP] work-task/sheet: no ip group in list")

    failed = 0
    for name, method, path in checks:
        r = req(method, path, token=token)
        code = r.get("code")
        data = r.get("data")
        extra = ""
        if isinstance(data, dict):
            lst = data.get("list")
            extra = f" total={data.get('total', len(lst) if lst is not None else 0)}"
        elif isinstance(data, list):
            extra = f" rows={len(data)}"
        status = "PASS" if code == 0 else "FAIL"
        print(f"[{status}] {name}: code={code} msg={(r.get('msg') or '')[:50]}{extra}")
        if code != 0:
            failed += 1

    health = urllib.request.urlopen("http://127.0.0.1:48094/actuator/health", timeout=10)
    print(f"[PASS] ops health: {health.read().decode()}")
    return 1 if failed else 0


if __name__ == "__main__":
    raise SystemExit(main())
