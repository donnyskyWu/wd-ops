# -*- coding: utf-8 -*-
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
    token = json.loads(raw)["data"]["accessToken"]
    h = {
        "Authorization": f"Bearer {token}",
        "tenant-id": "1",
        "X-Tenant-Id": "1",
        "Content-Type": "application/json",
    }

    st, raw = req("GET", "http://localhost:48094/admin-api/oa/content/get?id=31", h)
    print("GET_CONTENT", st)
    content = json.loads(raw).get("data") or {}
    # print key fields
    keys = [
        "id",
        "title",
        "authorId",
        "ipGroupId",
        "status",
        "paidBody",
        "freeBody",
        "body",
        "bodyFormat",
        "authorArticleId",
        "footballSyncError",
    ]
    for k in keys:
        v = content.get(k)
        if isinstance(v, str) and len(v) > 80:
            v = v[:80] + "..."
        print(f"  {k}={v}")

    title = (content.get("title") or "")[:35]
    paid = content.get("paidBody") or content.get("body") or ""
    free = content.get("freeBody")
    article = {
        "authorId": content.get("authorId"),
        "title": title,
        "content": paid,
        "freeContent": free,
        "status": -1,
        "price": 88.0,
        "privilegeTypes": [2],
        "refundType": 0,
        "matchType": 1,
        "schedulePublishStatus": 0,
        "sortNum": 0,
        "publishType": 0,
        "orderDeadlineType": 0,
    }
    st, raw = req(
        "POST",
        "http://localhost:48087/rpc-api/member/article/create",
        h,
        json.dumps(article, ensure_ascii=False),
    )
    print("RPC_FROM_CONTENT31", st, raw[:500])

    # Also try without auth like Feign might
    st, raw = req(
        "POST",
        "http://localhost:48087/rpc-api/member/article/create",
        {"Content-Type": "application/json", "tenant-id": "1"},
        json.dumps(
            {
                "authorId": 107156,
                "title": "no-auth-test",
                "content": "<p>x</p>",
                "status": -1,
                "price": 88.0,
                "privilegeTypes": [2],
                "refundType": 0,
                "matchType": 1,
                "schedulePublishStatus": 0,
            },
            ensure_ascii=False,
        ),
    )
    print("RPC_NO_BEARER", st, raw[:300])


if __name__ == "__main__":
    main()
