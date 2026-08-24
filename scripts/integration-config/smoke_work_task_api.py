#!/usr/bin/env python3
"""API smoke for work-task after V183 fix."""
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

    perms = req("GET", "/system/auth/get-permission-info", token=token)
    work_task_perms = [
        p for p in (perms.get("data", {}) or {}).get("permissions", []) if "work-task" in p
    ]
    print(f"work-task permissions: {work_task_perms}")

    sheet = req(
        "GET",
        f"/ops/work-task/sheet/get-or-create?ipGroupId={IP_GROUP}&workDate={WORK_DATE}",
        token=token,
    )
    print("get-or-create:", sheet.get("code"), "sheetId=", (sheet.get("data") or {}).get("id"))
    if sheet.get("code") != 0:
        return 1
    sheet_id = sheet["data"]["id"]
    rows = sheet["data"]["rows"][:2]

    save_one = {
        "sheetId": sheet_id,
        "rows": [
            {
                "id": rows[0]["id"],
                "rowNo": rows[0]["rowNo"],
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
    r1 = req("PUT", "/ops/work-task/sheet/save", save_one, token=token)
    print("save 1 row:", r1.get("code"), r1.get("msg"))

    save_two = {
        "sheetId": sheet_id,
        "rows": [
            {
                "id": rows[0]["id"],
                "rowNo": rows[0]["rowNo"],
                "competitionId": "4590956",
                "competitionName": "Smoke Match A",
                "authorId": AUTHOR,
                "assigneeId": ASSIGNEE,
                "workDate": WORK_DATE,
                "marketingPlan": "LIVE_PUBLIC",
                "isLive": 0,
                "salesPlatform": "DOUYIN",
            },
            {
                "id": rows[1]["id"],
                "rowNo": rows[1]["rowNo"],
                "competitionId": "4620581",
                "competitionName": "Smoke Match B",
                "authorId": AUTHOR,
                "assigneeId": ASSIGNEE,
                "workDate": WORK_DATE,
                "marketingPlan": "LIVE_PUBLIC",
                "isLive": 0,
                "salesPlatform": "DOUYIN",
            },
        ],
    }
    r2 = req("PUT", "/ops/work-task/sheet/save", save_two, token=token)
    print("save 2 rows:", r2.get("code"), r2.get("msg"))

    saved_ids = []
    if r2.get("code") == 0:
        saved_ids = [r["id"] for r in (r2.get("data") or {}).get("rows", []) if r.get("competitionId")][:2]
    if not saved_ids:
        saved_ids = [rows[0]["id"], rows[1]["id"]]

    confirm = req(
        "POST",
        "/ops/work-task/sheet/confirm",
        {"sheetId": sheet_id, "assignmentIds": saved_ids},
        token=token,
    )
    print("confirm:", confirm.get("code"), confirm.get("msg"))

    matrix = req("GET", f"/ops/work-task/matrix?workDate={WORK_DATE}&ipGroupIds={IP_GROUP}", token=token)
    mdata = matrix.get("data") or {}
    print("matrix:", matrix.get("code"), "rows=", len(mdata.get("rows") or []))

    summary = req("GET", f"/ops/work-task/matrix/summary?workDate={WORK_DATE}&ipGroupIds={IP_GROUP}", token=token)
    sdata = summary.get("data") or {}
    print("summary:", summary.get("code"), "totalTasks=", sdata.get("totalTasks"))

    # withdraw first confirmed row
    sheet_after = req(
        "GET",
        f"/ops/work-task/sheet/get-or-create?ipGroupId={IP_GROUP}&workDate={WORK_DATE}",
        token=token,
    )
    confirmed = [r for r in (sheet_after.get("data") or {}).get("rows", []) if r.get("generatedTaskId")]
    withdraw_code = None
    if confirmed:
        aid = confirmed[0]["id"]
        w = req(
            "POST",
            "/ops/work-task/sheet/withdraw",
            {"sheetId": sheet_id, "assignmentIds": [aid]},
            token=token,
        )
        withdraw_code = w.get("code")
        wr = next((r for r in (w.get("data") or {}).get("rows", []) if r["id"] == aid), None)
        print("withdraw:", withdraw_code, "genTaskId=", (wr or {}).get("generatedTaskId"))
        sheet_check = req(
            "GET",
            f"/ops/work-task/sheet/get-or-create?ipGroupId={IP_GROUP}&workDate={WORK_DATE}",
            token=token,
        )
        rc = next((r for r in (sheet_check.get("data") or {}).get("rows", []) if r["id"] == aid), None)
        print("after withdraw GET genTaskId=", (rc or {}).get("generatedTaskId"))
        w2 = req(
            "POST",
            "/ops/work-task/sheet/withdraw",
            {"sheetId": sheet_id, "assignmentIds": [aid]},
            token=token,
        )
        print("second withdraw (expect fail):", w2.get("code"), w2.get("msg"))
    else:
        print("withdraw: SKIP (no confirmed rows)")

    failed = [k for k, v in {
        "save1": r1.get("code"),
        "save2": r2.get("code"),
        "confirm": confirm.get("code"),
        "matrix": matrix.get("code"),
    }.items() if v != 0]
    if withdraw_code is not None and withdraw_code != 0:
        failed.append("withdraw")
    if failed:
        print("FAILED:", failed)
        return 1
    print("ALL OK")
    return 0


if __name__ == "__main__":
    sys.exit(main())
