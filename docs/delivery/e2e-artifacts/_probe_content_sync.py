# -*- coding: utf-8 -*-
import json
import urllib.error
import urllib.request


def req(method, url, headers=None, body=None):
    data = None if body is None else body.encode("utf-8")
    r = urllib.request.Request(url, data=data, method=method, headers=headers or {})
    try:
        with urllib.request.urlopen(r, timeout=30) as resp:
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
    print("LOGIN", st, raw[:200])
    token = json.loads(raw)["data"]["accessToken"]
    h = {
        "Authorization": f"Bearer {token}",
        "tenant-id": "1",
        "X-Tenant-Id": "1",
        "Content-Type": "application/json",
    }

    st, raw = req(
        "GET",
        "http://localhost:48094/admin-api/oa/content/list?pageNum=1&pageSize=10",
        h,
    )
    print("LIST", st)
    data = json.loads(raw)
    print(
        "code",
        data.get("code"),
        "msg",
        data.get("msg"),
        "total",
        (data.get("data") or {}).get("total"),
    )
    rows = (data.get("data") or {}).get("list") or []
    for row in rows:
        title = row.get("title") or ""
        print(
            "id=%s author=%s artId=%s shelf=%s syncErr=%s title=%s"
            % (
                row.get("id"),
                row.get("authorId"),
                row.get("authorArticleId"),
                row.get("shelfStatus"),
                row.get("footballSyncError"),
                title[:40],
            )
        )

    for cid in [r.get("id") for r in rows[:3]]:
        st, raw = req(
            "GET",
            "http://localhost:48094/admin-api/oa/content/%s/football-scheme" % cid,
            h,
        )
        print("SCHEME", cid, st, raw[:400])

    # create a minimal content with author 107156 / ip group 9003 if possible
    create_body = {
        "title": "E2E-scheme-sync-20260730",
        "body": "<p>paid body for sync test</p>",
        "paidBody": "<p>paid body for sync test</p>",
        "freeBody": "<p>free</p>",
        "contentType": "ARTICLE",
        "documentType": "SCHEME",
        "ipGroupId": 9003,
        "authorId": 107156,
        "competitionId": "e2e-comp-1",
        "competitionName": "E2E赛事",
        "aiGenerated": 0,
    }
    st, raw = req(
        "POST",
        "http://localhost:48094/admin-api/oa/content/create",
        h,
        json.dumps(create_body, ensure_ascii=False),
    )
    print("CREATE", st, raw[:500])
    created = json.loads(raw)
    cid = (created.get("data") if isinstance(created.get("data"), int) else None) or (
        created.get("data") or {}
    )
    if isinstance(cid, dict):
        cid = cid.get("id")
    if cid:
        st, raw = req(
            "GET",
            "http://localhost:48094/admin-api/oa/content/%s/football-scheme" % cid,
            h,
        )
        print("AFTER_CREATE_SCHEME", st, raw)
        st, raw = req(
            "POST",
            "http://localhost:48094/admin-api/oa/content/%s/sync-football-scheme" % cid,
            h,
            "{}",
        )
        print("RETRY_SYNC", st, raw[:500])

    # probe member RPC create directly
    article = {
        "authorId": 107156,
        "title": "RPC-direct-create-test",
        "content": "<p>x</p>",
        "freeContent": "<p>y</p>",
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
    print("RPC_CREATE", st, raw[:500])


if __name__ == "__main__":
    main()
