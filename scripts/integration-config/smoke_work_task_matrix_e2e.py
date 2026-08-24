#!/usr/bin/env python3
"""E2E smoke: work-task matrix (FR-M2-010)."""
from __future__ import annotations

import json
import sys
import urllib.error
import urllib.request
from datetime import date, timedelta

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


def week_range(d: date) -> tuple[str, str]:
    dow = d.weekday()  # Mon=0
    monday = d - timedelta(days=dow)
    sunday = monday + timedelta(days=6)
    return monday.isoformat(), sunday.isoformat()


def main() -> int:
    login = req("POST", "/system/auth/login", {"username": "admin", "password": "admin123"})
    if login.get("code") != 0:
        print("LOGIN FAIL", login)
        return 1
    token = login["data"]["accessToken"]
    print("login: OK")

    date_from, date_to = week_range(date.today())
    matrix = req(
        "GET",
        f"/ops/work-task/matrix?dateFrom={date_from}&dateTo={date_to}",
        token=token,
    )
    print("matrix:", matrix.get("code"), matrix.get("msg"))
    if matrix.get("code") != 0:
        print(json.dumps(matrix, ensure_ascii=False, indent=2))
        return 1
    rows = (matrix.get("data") or {}).get("rows") or []
    print(f"matrix rows={len(rows)} authorColumns={len((matrix.get('data') or {}).get('authorColumns') or [])}")

    summary = req(
        "GET",
        f"/ops/work-task/matrix/summary?dateFrom={date_from}&dateTo={date_to}",
        token=token,
    )
    print("summary:", summary.get("code"), summary.get("msg"))
    if summary.get("code") != 0:
        print(json.dumps(summary, ensure_ascii=False, indent=2))
        return 1
    sdata = summary.get("data") or {}
    print(
        f"summary totalMatchRows={sdata.get('totalMatchRows')} totalTasks={sdata.get('totalTasks')}"
    )

    print("E2E PASS")
    return 0


if __name__ == "__main__":
    sys.exit(main())
