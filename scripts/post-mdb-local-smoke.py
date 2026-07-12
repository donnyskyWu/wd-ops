#!/usr/bin/env python3
"""POST-MDB local signoff API smoke via Gateway (Football admin login)."""
from __future__ import annotations

import json
import subprocess
import sys
import urllib.error
import urllib.request

GATEWAY = "http://localhost:48080"


def post_json(url: str, payload: dict) -> dict:
    data = json.dumps(payload).encode()
    req = urllib.request.Request(
        url,
        data=data,
        headers={"Content-Type": "application/json", "tenant-id": "1"},
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=20) as resp:
        return json.loads(resp.read())


def get_json(url: str, token: str) -> tuple[int, dict | str]:
    req = urllib.request.Request(
        url,
        headers={
            "Authorization": f"Bearer {token}",
            "tenant-id": "1",
            "X-Tenant-Id": "1",
        },
    )
    try:
        with urllib.request.urlopen(req, timeout=20) as resp:
            return resp.status, json.loads(resp.read())
    except urllib.error.HTTPError as exc:
        body = exc.read().decode("utf-8", errors="replace")
        try:
            return exc.code, json.loads(body)
        except json.JSONDecodeError:
            return exc.code, body


def mysql_scalar(sql: str) -> str:
    proc = subprocess.run(
        ["mysql", "-h", "localhost", "-uroot", "-proot", "-N", "-e", sql],
        capture_output=True,
        text=True,
    )
    if proc.returncode != 0:
        return f"ERR:{proc.stderr.strip()}"
    return proc.stdout.strip()


def main() -> int:
    report: dict = {"gateway": GATEWAY, "db": {}, "checks": []}

    report["db"]["dev_token"] = mysql_scalar(
        "SELECT CONCAT(user_id) FROM wd.sys_user_token WHERE token='dev-token-oa-admin' LIMIT 1"
    )
    report["db"]["author_user"] = mysql_scalar(
        "SELECT COUNT(*) FROM `shenyu-member`.author_user"
    )
    report["db"]["login_log"] = mysql_scalar(
        "SELECT COUNT(*) FROM `shenyu-system`.system_login_log"
    )
    report["db"]["mp_account"] = mysql_scalar(
        "SELECT COUNT(*) FROM `shenyu-mp`.mp_account"
    )
    report["db"]["dict_type"] = mysql_scalar(
        "SELECT COUNT(*) FROM `shenyu-system`.system_dict_type WHERE deleted=0"
    )

    login = post_json(f"{GATEWAY}/admin-api/system/auth/login", {"username": "admin", "password": "admin123"})
    if login.get("code") != 0:
        report["login"] = {"pass": False, "body": login}
        print(json.dumps(report, ensure_ascii=False, indent=2))
        return 1

    token = login["data"]["accessToken"]
    report["login"] = {"pass": True, "token_prefix": token[:12] + "..."}
    report["db"]["token_in_wd"] = mysql_scalar(
        f"SELECT COUNT(*) FROM wd.system_oauth2_access_token WHERE access_token='{token}'"
    )
    report["db"]["token_in_system"] = mysql_scalar(
        f"SELECT COUNT(*) FROM `shenyu-system`.system_oauth2_access_token WHERE access_token='{token}'"
    )

    probes = [
        (
            "author_list",
            f"{GATEWAY}/admin-api/oa/author/list?pageNo=1&pageSize=10",
            lambda b: isinstance(b, dict) and b.get("code") == 0 and (b.get("data") or {}).get("total", 0) >= 35,
            "total",
        ),
        (
            "wechat_account_list",
            f"{GATEWAY}/admin-api/oa/account/list?pageNo=1&pageSize=10",
            lambda b: isinstance(b, dict) and b.get("code") == 0,
            "total",
        ),
        (
            "login_log",
            f"{GATEWAY}/admin-api/oa/system/log/login?pageNo=1&pageSize=10",
            lambda b: isinstance(b, dict) and b.get("code") == 0 and (b.get("data") or {}).get("total", 0) >= 3000,
            "total",
        ),
        (
            "dict_list",
            f"{GATEWAY}/admin-api/oa/system/dict/list?pageNo=1&pageSize=10",
            lambda b: isinstance(b, dict) and b.get("code") == 0,
            "total",
        ),
    ]

    all_pass = True
    for name, url, pred, metric_key in probes:
        entry = {"name": name, "url": url}
        status, body = get_json(url, token)
        entry["httpStatus"] = status
        if isinstance(body, dict):
            entry["code"] = body.get("code")
            entry["msg"] = body.get("msg")
            data = body.get("data")
            if isinstance(data, dict) and metric_key in data:
                entry[metric_key] = data.get(metric_key)
        else:
            entry["raw"] = str(body)[:200]
        entry["pass"] = pred(body) if isinstance(body, dict) else False
        if not entry["pass"]:
            all_pass = False
        report["checks"].append(entry)

    report["all_pass"] = all_pass
    print(json.dumps(report, ensure_ascii=False, indent=2))
    return 0 if all_pass else 2


if __name__ == "__main__":
    sys.exit(main())
