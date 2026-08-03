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
    st, raw = req(
        "POST",
        "http://localhost:48094/admin-api/oa/content/31/sync-football-scheme",
        h,
        "{}",
    )
    print("RETRY31", st, raw)
    st, raw = req(
        "GET",
        "http://localhost:48094/admin-api/oa/content/31/football-scheme",
        h,
    )
    print("SCHEME31", st, raw)


if __name__ == "__main__":
    main()
