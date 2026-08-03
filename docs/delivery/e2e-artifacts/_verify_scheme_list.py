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

    # Football 方案列表 Admin API
    urls = [
        "http://localhost:48080/admin-api/member/article/page?pageNo=1&pageSize=10",
        "http://localhost:48087/admin-api/member/article/page?pageNo=1&pageSize=10",
        "http://localhost:48080/admin-api/member/article/page?pageNo=1&pageSize=10&status=-1",
    ]
    for u in urls:
        st, raw = req("GET", u, h)
        print("GET", u.split("/admin-api")[-1], st, raw[:350])
        try:
            data = json.loads(raw).get("data") or {}
            rows = data.get("list") or data.get("records") or []
            for row in rows[:8]:
                print(
                    "  id=%s status=%s author=%s title=%s"
                    % (
                        row.get("id"),
                        row.get("status"),
                        row.get("authorId"),
                        (row.get("title") or "")[:40],
                    )
                )
        except Exception as ex:
            print("  parse err", ex)

    # get article 1000314
    st, raw = req(
        "GET",
        "http://localhost:48087/admin-api/member/article/get?id=1000314",
        h,
    )
    print("GET_ARTICLE", st, raw[:400])


if __name__ == "__main__":
    main()
