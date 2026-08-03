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

    # 1) retry update sync for content 31
    st, raw = req(
        "POST",
        "http://localhost:48094/admin-api/oa/content/31/sync-football-scheme",
        h,
        "{}",
    )
    print("RETRY31", st, raw[:400])

    # 2) create new content with spaces in title + author
    body = {
        "title": "验证同步 VS 方案列表-2026-07-30 12:00",
        "body": "<p>paid verify</p>",
        "paidBody": "<p>paid verify</p>",
        "freeBody": "<p>free verify</p>",
        "contentType": "ARTICLE",
        "documentType": "ARTICLE",
        "ipGroupId": 9003,
        "authorId": 107156,
        "competitionId": "e2e-comp-verify",
        "competitionName": "E2E验证赛事",
        "aiGenerated": 0,
    }
    st, raw = req(
        "POST",
        "http://localhost:48094/admin-api/oa/content/create",
        h,
        json.dumps(body, ensure_ascii=False),
    )
    print("CREATE", st, raw[:300])
    created = json.loads(raw)
    cid = created.get("data")
    if not isinstance(cid, int):
        print("CREATE_FAILED", created)
        return
    st, raw = req(
        "GET",
        "http://localhost:48094/admin-api/oa/content/%s/football-scheme" % cid,
        h,
    )
    print("SCHEME_NEW", st, raw)
    scheme = json.loads(raw).get("data") or {}
    art_id = scheme.get("authorArticleId")
    if art_id:
        st, raw = req(
            "GET",
            "http://localhost:48080/admin-api/member/article/get?id=%s" % art_id,
            h,
        )
        print("ARTICLE_GET", st, raw[:350])
        st, raw = req(
            "GET",
            "http://localhost:48080/admin-api/member/article/page?pageNo=1&pageSize=5",
            h,
        )
        data = json.loads(raw).get("data") or {}
        ids = [r.get("id") for r in (data.get("list") or [])]
        print("SCHEME_LIST_TOP_IDS", ids, "contains", art_id, art_id in ids)


if __name__ == "__main__":
    main()
