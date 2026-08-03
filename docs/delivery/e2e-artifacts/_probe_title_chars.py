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


def create(h, title):
    article = {
        "authorId": 107156,
        "title": title,
        "content": "<p>paid</p>",
        "freeContent": "<p>free</p>",
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
    print("TITLE=%r -> %s" % (title, raw[:200]))


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

    create(h, "nospace")
    create(h, "has space")
    create(h, "has(parens)")
    create(h, "美职联-测试 VS 对手-2026-07-30 09:00")

    # get content 31 real title
    st, raw = req("GET", "http://localhost:48094/admin-api/oa/content/31", h)
    data = json.loads(raw).get("data") or {}
    print("CONTENT31 title=%r author=%s" % (data.get("title"), data.get("authorId")))
    if data.get("title"):
        create(h, data.get("title")[:35])


if __name__ == "__main__":
    main()
