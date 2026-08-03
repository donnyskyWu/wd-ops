# -*- coding: utf-8 -*-
"""Probe member article create + OPS sync after fix (requires restarted member/oa)."""
import json
import urllib.error
import urllib.request


def req(method, url, headers=None, body=None):
    data = None if body is None else body.encode("utf-8")
    r = urllib.request.Request(url, data=data, method=method, headers=headers or {})
    try:
        with urllib.request.urlopen(r, timeout=60) as resp:
            return resp.status, resp.read().decode("utf-8", errors="replace")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", errors="replace")


def main():
    login_body = json.dumps({"username": "admin", "password": "admin123"})
    st, raw = req(
        "POST",
        "http://localhost:48080/admin-api/system/auth/login",
        {"Content-Type": "application/json", "tenant-id": "1", "X-Tenant-Id": "1"},
        login_body,
    )
    print("LOGIN", st)
    token = json.loads(raw)["data"]["accessToken"]
    h = {
        "Authorization": f"Bearer {token}",
        "tenant-id": "1",
        "X-Tenant-Id": "1",
        "Content-Type": "application/json",
    }

    # Minimal G-MEM-03 payload (no publishType) — expects Football defaults after fix
    article = {
        "authorId": 107156,
        "title": "RPC-draft-after-fix",
        "content": "<p>paid</p>",
        "freeContent": "<p>free</p>",
        "status": -1,
        "price": 88.0,
        "privilegeTypes": [2],
        "refundType": 0,
        "matchType": 1,
        "schedulePublishStatus": 0,
        "sortNum": 0,
    }
    st, raw = req(
        "POST",
        "http://localhost:48087/rpc-api/member/article/create",
        h,
        json.dumps(article),
    )
    print("RPC_CREATE_MINIMAL", st, raw[:400])

    # With amphipoda defaults
    article2 = dict(article)
    article2["title"] = "RPC-draft-with-defaults"
    article2["publishType"] = 0
    article2["orderDeadlineType"] = 0
    st, raw = req(
        "POST",
        "http://localhost:48087/rpc-api/member/article/create",
        h,
        json.dumps(article2),
    )
    print("RPC_CREATE_DEFAULTS", st, raw[:400])

    # Retry sync for content 31 (has author, failed previously)
    st, raw = req(
        "POST",
        "http://localhost:48094/admin-api/oa/content/31/sync-football-scheme",
        h,
        "{}",
    )
    print("RETRY_SYNC_31", st, raw[:500])

    st, raw = req(
        "GET",
        "http://localhost:48094/admin-api/oa/content/31/football-scheme",
        h,
    )
    print("SCHEME_31", st, raw[:400])


if __name__ == "__main__":
    main()
