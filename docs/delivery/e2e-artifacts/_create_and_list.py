#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import json
import urllib.request
from pathlib import Path

OUT = Path(__file__).resolve().parent


def call(url, headers, data=None, method=None):
    if data is not None and method is None:
        method = "POST"
    req = urllib.request.Request(
        url,
        data=data,
        headers=headers,
        method=method or ("POST" if data is not None else "GET"),
    )
    with urllib.request.urlopen(req, timeout=60) as resp:
        return json.loads(resp.read().decode("utf-8"))


def main():
    h = {"Content-Type": "application/json", "tenant-id": "1", "X-Tenant-Id": "1"}
    login = call(
        "http://localhost:48080/admin-api/system/auth/login",
        h,
        json.dumps({"username": "admin", "password": "admin123"}).encode(),
    )
    token = login["data"]["accessToken"]
    auth = {"Authorization": f"Bearer {token}", "tenant-id": "1", "X-Tenant-Id": "1", "Content-Type": "application/json"}

    before = call("http://localhost:48080/admin-api/oa/content/list?pageNum=1&pageSize=5", auth)
    print("BEFORE total=", before["data"]["total"])

    body = {
        "title": "E2E-content-list-fix-20260730",
        "contentType": "SHORT_VIDEO",
        "creatorUserId": 1749825673829120001,
        "ipGroupId": 9004,
        "body": "e2e body after data-scope fix",
        "competitionId": "e2e-comp-20260730",
        "competitionName": "E2E 赛事占位",
    }
    create = call(
        "http://localhost:48080/admin-api/oa/content/create",
        auth,
        json.dumps(body, ensure_ascii=False).encode("utf-8"),
    )
    print("CREATE", json.dumps(create, ensure_ascii=False))
    (OUT / "_create_after_fix.json").write_text(json.dumps(create, ensure_ascii=False, indent=2), encoding="utf-8")

    after = call("http://localhost:48080/admin-api/oa/content/list?pageNum=1&pageSize=5&title=E2E-content-list-fix", auth)
    print("AFTER filter total=", after["data"]["total"], "titles=", [x.get("title") for x in after["data"]["list"]])
    (OUT / "_list_after_create.json").write_text(json.dumps(after, ensure_ascii=False, indent=2), encoding="utf-8")


if __name__ == "__main__":
    main()
