# -*- coding: utf-8 -*-
import json
import urllib.error
import urllib.request


def req(method, url, headers=None, body=None):
    data = None if body is None else body.encode("utf-8")
    r = urllib.request.Request(url, data=data, method=method, headers=headers or {})
    try:
        with urllib.request.urlopen(r, timeout=90) as resp:
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
    token = json.loads(raw)["data"]["accessToken"]
    h = {
        "Authorization": f"Bearer {token}",
        "tenant-id": "1",
        "X-Tenant-Id": "1",
        "Content-Type": "application/json",
    }

    st, raw = req("GET", "http://localhost:48094/admin-api/oa/content/31", h)
    print("GET31", st, raw[:600])
    content = json.loads(raw).get("data") or {}
    print(
        "author",
        content.get("authorId"),
        "title",
        (content.get("title") or "")[:40],
        "art",
        content.get("authorArticleId"),
        "err",
        content.get("footballSyncError"),
    )

    # create new content with author via API (taskless)
    # discover a working create payload from previous E2E if needed
    body = {
        "title": "E2E-scheme-sync-verify-20260730b",
        "body": "<p>paid body for sync verify</p>",
        "paidBody": "<p>paid body for sync verify</p>",
        "freeBody": "<p>free</p>",
        "contentType": "ARTICLE",
        "documentType": "SCHEME",
        "ipGroupId": 9003,
        "authorId": 107156,
        "competitionId": "e2e-comp-1",
        "competitionName": "E2E赛事",
        "aiGenerated": 0,
        "platformType": "WECHAT",
        "schemeTypes": ["ANALYSIS"],
    }
    st, raw = req(
        "POST",
        "http://localhost:48094/admin-api/oa/content/create",
        h,
        json.dumps(body, ensure_ascii=False),
    )
    print("CREATE", st, raw[:500])
    created = json.loads(raw)
    cid = created.get("data")
    if isinstance(cid, int):
        st, raw = req(
            "GET",
            "http://localhost:48094/admin-api/oa/content/%s/football-scheme" % cid,
            h,
        )
        print("SCHEME", st, raw)
        if '"authorArticleId":null' in raw or '"authorArticleId": null' in raw:
            st, raw = req(
                "POST",
                "http://localhost:48094/admin-api/oa/content/%s/sync-football-scheme" % cid,
                h,
                "{}",
            )
            print("RETRY", st, raw[:500])


if __name__ == "__main__":
    main()
