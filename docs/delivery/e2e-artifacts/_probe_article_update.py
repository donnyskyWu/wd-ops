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
    body = {
        "id": 1000314,
        "authorId": 107156,
        "title": "更新标题测试",
        "content": "<p>paid</p>",
        "freeContent": "<p>free</p>",
        "status": -1,
        "price": 88.0,
        "privilegeTypes": [2],
        "refundType": 0,
        "matchType": 1,
        "schedulePublishStatus": 0,
        "publishType": 0,
        "orderDeadlineType": 0,
        "sortNum": 0,
    }
    st, raw = req(
        "PUT",
        "http://localhost:48087/rpc-api/member/article/update",
        h,
        json.dumps(body, ensure_ascii=False),
    )
    print("PUT_UPDATE", st, raw[:400])
    st, raw = req(
        "POST",
        "http://localhost:48087/rpc-api/member/article/update",
        h,
        json.dumps(body, ensure_ascii=False),
    )
    print("POST_UPDATE", st, raw[:400])


if __name__ == "__main__":
    main()
