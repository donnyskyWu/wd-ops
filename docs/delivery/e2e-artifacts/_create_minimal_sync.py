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
    st, raw = req(
        "POST",
        "http://localhost:48080/admin-api/system/auth/login",
        {"Content-Type": "application/json", "tenant-id": "1", "X-Tenant-Id": "1"},
        json.dumps({"username": "admin", "password": "admin123"}),
    )
    token = json.loads(raw)["data"]["accessToken"]
    h = {
        "Authorization": "Bearer " + token,
        "tenant-id": "1",
        "X-Tenant-Id": "1",
        "Content-Type": "application/json",
    }
    body = {
        "title": "E2E scheme sync VS list 1200",
        "body": "<p>paid</p>",
        "paidBody": "<p>paid</p>",
        "freeBody": "<p>free</p>",
        "contentType": "ARTICLE",
        "aiGenerated": 0,
        "ipGroupId": 9003,
        "authorId": 107156,
        "competitionId": "c1",
        "competitionName": "c1",
    }
    st, raw = req(
        "POST",
        "http://localhost:48094/admin-api/oa/content/create",
        h,
        json.dumps(body, ensure_ascii=False),
    )
    print("CREATE", st, raw[:500])
    data = json.loads(raw).get("data")
    if isinstance(data, int):
        st, raw = req(
            "GET",
            "http://localhost:48094/admin-api/oa/content/%s/football-scheme" % data,
            h,
        )
        print("SCHEME", st, raw)
        art = (json.loads(raw).get("data") or {}).get("authorArticleId")
        if art:
            st, raw = req(
                "GET",
                "http://localhost:48080/admin-api/member/article/page?pageNo=1&pageSize=5",
                h,
            )
            ids = [x.get("id") for x in ((json.loads(raw).get("data") or {}).get("list") or [])]
            print("LIST_HAS", art, art in ids, ids)


if __name__ == "__main__":
    main()
