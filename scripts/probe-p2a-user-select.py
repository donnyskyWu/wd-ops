#!/usr/bin/env python3
"""P2a probe: UserSelect data source — Football system/user/simple-list vs oa/system/user/list."""
from __future__ import annotations

import json
import sys
import urllib.error
import urllib.request

GATEWAY = "http://localhost:48080"


def post_json(url: str, payload: dict, headers: dict | None = None) -> dict:
    data = json.dumps(payload).encode()
    h = {"Content-Type": "application/json", "tenant-id": "1", **(headers or {})}
    req = urllib.request.Request(url, data=data, headers=h, method="POST")
    with urllib.request.urlopen(req, timeout=10) as resp:
        return json.loads(resp.read())


def get_json(url: str, token: str) -> dict:
    req = urllib.request.Request(
        url,
        headers={
            "Authorization": f"Bearer {token}",
            "tenant-id": "1",
            "X-Tenant-Id": "1",
        },
    )
    with urllib.request.urlopen(req, timeout=10) as resp:
        return json.loads(resp.read())


def main() -> int:
    report: dict = {"gateway": GATEWAY, "checks": []}

    try:
        login = post_json(f"{GATEWAY}/admin-api/system/auth/login", {"username": "admin", "password": "admin123"})
    except Exception as exc:
        print(json.dumps({"ok": False, "error": f"login failed: {exc}"}, ensure_ascii=False, indent=2))
        return 1

    if login.get("code") != 0:
        print(json.dumps({"ok": False, "error": "login code != 0", "login": login}, ensure_ascii=False, indent=2))
        return 1

    token = login["data"]["accessToken"]
    report["auth"] = "gateway-login"

    probes = [
        ("football_simple_list", f"{GATEWAY}/admin-api/system/user/simple-list"),
        ("oa_user_list", f"{GATEWAY}/admin-api/oa/system/user/list?pageNo=1&pageSize=10&status=ENABLED"),
    ]

    ok = True
    for name, url in probes:
        entry = {"name": name, "url": url}
        try:
            body = get_json(url, token)
            entry["code"] = body.get("code")
            entry["msg"] = body.get("msg")
            data = body.get("data")
            if name == "football_simple_list":
                entry["count"] = len(data) if isinstance(data, list) else 0
                entry["pass"] = body.get("code") == 0 and isinstance(data, list) and len(data) > 0
                if entry["pass"] and data:
                    entry["sample"] = {"id": data[0].get("id"), "nickname": data[0].get("nickname")}
            else:
                entry["pass"] = body.get("code") == 0
            if not entry["pass"]:
                ok = False
        except urllib.error.HTTPError as exc:
            entry["pass"] = False
            entry["httpStatus"] = exc.code
            ok = False
        except Exception as exc:
            entry["pass"] = False
            entry["error"] = str(exc)
            ok = False
        report["checks"].append(entry)

    football_ok = report["checks"][0].get("pass") is True
    oa_blocked = report["checks"][1].get("code") == 403 if len(report["checks"]) > 1 else False
    report["ok"] = football_ok
    report["oa_list_blocked_as_expected"] = oa_blocked
    report["verdict"] = (
        "P2a PASS: Football /system/user/simple-list returns users; oa list 403 for Football token"
        if football_ok
        else "P2a FAIL: Football simple-list unavailable"
    )
    print(json.dumps(report, ensure_ascii=False, indent=2))
    return 0 if football_ok else 1


if __name__ == "__main__":
    sys.exit(main())
