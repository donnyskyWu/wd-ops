#!/usr/bin/env python3
"""C-WP0 smoke: parallel system APIs return business code 410."""
from __future__ import annotations

import json
import sys
import urllib.error
import urllib.request

GATEWAY = "http://localhost:48080"
OA = "http://127.0.0.1:48094"


def req(method, url, headers=None, body=None, timeout=20):
    data = None if body is None else body.encode("utf-8")
    request = urllib.request.Request(url, data=data, method=method, headers=headers or {})
    try:
        with urllib.request.urlopen(request, timeout=timeout) as resp:
            return resp.status, resp.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "replace")
    except Exception as e:
        return 0, str(e)


def main():
    sys.stdout.reconfigure(encoding="utf-8")
    h = {"Content-Type": "application/json", "tenant-id": "1", "X-Tenant-Id": "1"}
    st, raw = req(
        "POST",
        f"{GATEWAY}/admin-api/system/auth/login",
        h,
        json.dumps({"username": "admin", "password": "admin123"}),
    )
    login = json.loads(raw)
    assert login.get("code") == 0, login
    token = login["data"]["accessToken"]
    auth = {
        "Authorization": f"Bearer {token}",
        "tenant-id": "1",
        "X-Tenant-Id": "1",
        "Content-Type": "application/json",
    }

    author_body = json.dumps(
        {
            "authorName": "CWP0",
            "ipGroupId": 9001,
            "authorType": "SHORT_VIDEO",
            "status": 1,
        }
    )

    # Gateway only forwards /admin-api/oa/** to OPS; alias /admin-api/system/** is Football.
    checks = [
        ("GW", GATEWAY, "GET", "/admin-api/oa/system/user/list?pageNo=1&pageSize=5", None, 410),
        ("GW", GATEWAY, "GET", "/admin-api/oa/system/role/list?pageNo=1&pageSize=5", None, 410),
        ("GW", GATEWAY, "GET", "/admin-api/oa/system/dept/tree", None, 410),
        ("GW", GATEWAY, "POST", "/admin-api/oa/system/dept/sync-dingtalk", "{}", 410),
        ("GW", GATEWAY, "POST", "/admin-api/oa/system/dept/sync-dingtalk-users", "{}", 410),
        ("GW", GATEWAY, "POST", "/admin-api/oa/author/create", author_body, 410),
        ("GW", GATEWAY, "GET", "/admin-api/oa/system/user/profile", None, 0),
        ("GW", GATEWAY, "GET", "/admin-api/oa/ip-group/list?pageNo=1&pageSize=5", None, 0),
        # OPS dual-mapping alias (direct only)
        ("OA", OA, "GET", "/admin-api/system/user/list?pageNo=1&pageSize=5", None, 410),
        ("OA", OA, "GET", "/admin-api/system/role/list?pageNo=1&pageSize=5", None, 410),
        ("OA", OA, "GET", "/admin-api/system/dept/tree", None, 410),
    ]

    ok = True
    for tag, base, method, path, body, expect in checks:
        st, raw = req(method, base + path, auth, body)
        try:
            obj = json.loads(raw)
            code = obj.get("code")
        except Exception:
            code = None
            obj = raw[:200]
        mark = "PASS" if code == expect else "FAIL"
        if mark == "FAIL":
            ok = False
        print(f"{mark} {tag} http={st} code={code} expect={expect} {method} {path}", flush=True)
        if mark == "FAIL":
            print("  body:", str(obj)[:300], flush=True)
    print("ALL_OK" if ok else "HAS_FAIL", flush=True)
    return 0 if ok else 1


if __name__ == "__main__":
    raise SystemExit(main())
