#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import json
import urllib.error
import urllib.request
from pathlib import Path

OUT = Path(__file__).resolve().parent


def call(url: str, headers: dict, data: bytes | None = None):
    req = urllib.request.Request(url, data=data, headers=headers, method="POST" if data else "GET")
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            return resp.status, resp.read().decode("utf-8")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "replace")


def main():
    headers = {
        "Content-Type": "application/json",
        "tenant-id": "1",
        "X-Tenant-Id": "1",
    }
    code, body = call(
        "http://localhost:48080/admin-api/system/auth/login",
        headers,
        json.dumps({"username": "admin", "password": "admin123"}).encode(),
    )
    print("LOGIN", code, body[:300])
    token = json.loads(body)["data"]["accessToken"]
    auth = {
        "Authorization": f"Bearer {token}",
        "tenant-id": "1",
        "X-Tenant-Id": "1",
    }
    for base in ("http://localhost:48094", "http://localhost:48080"):
        code, body = call(f"{base}/admin-api/oa/content/list?pageNum=1&pageSize=5", auth)
        print("LIST", base, code)
        print(body[:1000])
        (OUT / f"_list_{base.rsplit(':', 1)[-1]}.json").write_text(body, encoding="utf-8")


if __name__ == "__main__":
    main()
