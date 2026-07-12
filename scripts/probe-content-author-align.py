#!/usr/bin/env python3
"""§23 #3: Content author_id alignment smoke — create draft with author from list."""
from __future__ import annotations

import json
import subprocess
import sys
import urllib.error
import urllib.request
from pathlib import Path

GATEWAY = "http://localhost:48080"
ROOT = Path(__file__).resolve().parents[1]


def post_json(url: str, payload: dict) -> dict:
    data = json.dumps(payload).encode()
    req = urllib.request.Request(
        url,
        data=data,
        headers={"Content-Type": "application/json", "tenant-id": "1"},
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=30) as resp:
        return json.loads(resp.read())


def request_json(url: str, token: str, method: str = "GET", payload: dict | None = None) -> tuple[int, dict | str]:
    headers = {
        "Authorization": f"Bearer {token}",
        "tenant-id": "1",
        "X-Tenant-Id": "1",
    }
    data = json.dumps(payload).encode() if payload is not None else None
    if payload is not None:
        headers["Content-Type"] = "application/json"
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
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
    report: dict = {"gateway": GATEWAY, "checks": []}

    login = post_json(
        f"{GATEWAY}/admin-api/system/auth/login",
        {"username": "admin", "password": "admin123"},
    )
    if login.get("code") != 0:
        report["login"] = {"pass": False, "body": login}
        print(json.dumps(report, ensure_ascii=False, indent=2))
        return 1

    token = login["data"]["accessToken"]
    report["login"] = {"pass": True}

    status, author_body = request_json(
        f"{GATEWAY}/admin-api/oa/author/list?pageNo=1&pageSize=1&status=1&ipGroupId=9001",
        token,
    )
    author_check = {"name": "author_list_for_content", "httpStatus": status, "pass": False}
    author_id = None
    ip_group_id = None
    if isinstance(author_body, dict) and author_body.get("code") == 0:
        lst = (author_body.get("data") or {}).get("list") or []
        if lst:
            author_id = lst[0].get("id") or lst[0].get("authorUserId")
            ip_group_id = lst[0].get("ipGroupId")
            author_check["authorId"] = author_id
            author_check["ipGroupId"] = ip_group_id
            author_check["pass"] = author_id is not None and ip_group_id is not None
    else:
        author_check["body"] = author_body
    report["checks"].append(author_check)

    if not author_check["pass"]:
        report["all_pass"] = False
        print(json.dumps(report, ensure_ascii=False, indent=2))
        return 2

    create_payload = {
        "title": "§23-probe-内容作者对齐",
        "contentType": "ARTICLE",
        "documentType": "POST_MATCH_REVIEW",
        "body": "integration probe body",
        "ipGroupId": ip_group_id,
        "authorId": author_id,
        "creatorUserId": 1,
        "competitionId": "probe-cmp-001",
        "competitionName": "Probe Match",
    }
    create_status, create_body = request_json(
        f"{GATEWAY}/admin-api/oa/content/create",
        token,
        method="POST",
        payload=create_payload,
    )
    content_id = None
    create_check = {
        "name": "content_create_with_author",
        "httpStatus": create_status,
        "pass": isinstance(create_body, dict) and create_body.get("code") == 0,
    }
    if isinstance(create_body, dict):
        create_check["code"] = create_body.get("code")
        create_check["msg"] = create_body.get("msg")
        content_id = create_body.get("data")
    report["checks"].append(create_check)

    if content_id:
        db_author = mysql_scalar(
            f"SELECT author_id FROM wd.oa_production_content WHERE id={int(content_id)} LIMIT 1"
        )
        member_ok = mysql_scalar(
            f"SELECT COUNT(*) FROM `shenyu-member`.author_user "
            f"WHERE id={int(author_id)} AND tenant_id=1 AND deleted=0"
        )
        report["db"] = {
            "content_id": content_id,
            "stored_author_id": db_author,
            "member_exists": member_ok,
            "aligned": db_author == str(author_id) and member_ok == "1",
        }
        report["checks"].append({"name": "author_id_member_ssot", "pass": report["db"]["aligned"]})

    report["all_pass"] = all(c.get("pass") for c in report["checks"])
    out_path = ROOT / "docs/delivery/post-mdb-content-author-probe.json"
    out_path.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False, indent=2))
    return 0 if report["all_pass"] else 2


if __name__ == "__main__":
    sys.exit(main())
