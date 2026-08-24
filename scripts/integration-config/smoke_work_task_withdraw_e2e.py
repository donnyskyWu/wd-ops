#!/usr/bin/env python3
"""E2E: work-task confirm then withdraw — verifies generatedTaskId cleared in DB."""
from __future__ import annotations

import json
import sys
import urllib.error
import urllib.request

BASE = "http://localhost:48080/admin-api"
WORK_DATE = "2026-08-19"
IP_GROUP = 9016
AUTHOR = 107156
ASSIGNEE = "1749825673829120001"


def req(method: str, path: str, body=None, token=None):
    url = BASE + path
    headers = {"Content-Type": "application/json", "tenant-id": "1", "X-Tenant-Id": "1"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    data = json.dumps(body).encode("utf-8") if body is not None else None
    request = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request, timeout=60) as resp:
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

    sheet = req(
        "GET",
        f"/ops/work-task/sheet/get-or-create?ipGroupId={IP_GROUP}&workDate={WORK_DATE}",
        token=token,
    )
    if sheet.get("code") != 0:
        print("get-or-create FAIL", sheet)
        return 1
    sheet_id = sheet["data"]["id"]
    rows = sheet["data"]["rows"]
    # Prefer a saved draft row (competition already set); empty rows may hit unrelated save 500 on beta.
    target = next(
        (r for r in rows if not r.get("generatedTaskId") and r.get("competitionId") and r.get("marketingPlan")),
        None,
    )
    if target is None:
        target = next((r for r in rows if not r.get("generatedTaskId")), None)
    if target is None:
        print("No unconfirmed row available")
        return 1

    if not (target.get("competitionId") and target.get("marketingPlan")):
        save = {
            "sheetId": sheet_id,
            "rows": [
                {
                    "id": target["id"],
                    "rowNo": target["rowNo"],
                    "competitionId": "4590956",
                    "competitionName": "Smoke Match A",
                    "authorId": AUTHOR,
                    "assigneeId": ASSIGNEE,
                    "workDate": WORK_DATE,
                    "marketingPlan": "LIVE_PUBLIC",
                    "isLive": 0,
                    "salesPlatform": "DOUYIN",
                }
            ],
        }
        saved = req("PUT", "/ops/work-task/sheet/save", save, token=token)
        print("save:", saved.get("code"), saved.get("msg"))
        if saved.get("code") != 0:
            return 1

    aid = target["id"]
    confirm = req(
        "POST",
        "/ops/work-task/sheet/confirm",
        {"sheetId": sheet_id, "assignmentIds": [aid]},
        token=token,
    )
    print("confirm:", confirm.get("code"), confirm.get("msg"))
    if confirm.get("code") != 0:
        return 1
    cr = next((r for r in confirm["data"]["rows"] if r["id"] == aid), None)
    task_id = (cr or {}).get("generatedTaskId")
    print("after confirm generatedTaskId=", task_id)
    if task_id is None:
        print("FAIL: generatedTaskId not set after confirm")
        return 1

    withdraw = req(
        "POST",
        "/ops/work-task/sheet/withdraw",
        {"sheetId": sheet_id, "assignmentIds": [aid]},
        token=token,
    )
    print("withdraw:", withdraw.get("code"), withdraw.get("msg"))
    if withdraw.get("code") != 0:
        return 1
    wr = next((r for r in withdraw["data"]["rows"] if r["id"] == aid), None)
    if (wr or {}).get("generatedTaskId") is not None:
        print("FAIL: withdraw response still has generatedTaskId")
        return 1

    sheet2 = req(
        "GET",
        f"/ops/work-task/sheet/get-or-create?ipGroupId={IP_GROUP}&workDate={WORK_DATE}",
        token=token,
    )
    r2 = next((r for r in sheet2["data"]["rows"] if r["id"] == aid), None)
    print("GET after withdraw generatedTaskId=", (r2 or {}).get("generatedTaskId"))
    if (r2 or {}).get("generatedTaskId") is not None:
        print("FAIL: DB still has generatedTaskId after withdraw")
        return 1

    withdraw2 = req(
        "POST",
        "/ops/work-task/sheet/withdraw",
        {"sheetId": sheet_id, "assignmentIds": [aid]},
        token=token,
    )
    print("second withdraw:", withdraw2.get("code"), withdraw2.get("msg"))
    if withdraw2.get("code") == 0:
        print("FAIL: second withdraw should be rejected")
        return 1

    print("WITHDRAW E2E PASS")
    return 0


if __name__ == "__main__":
    sys.exit(main())
