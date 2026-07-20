#!/usr/bin/env python3
"""E2E: content create/update -> system_operate_log + Football operate-log/page API."""
from __future__ import annotations

import json
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime

GATEWAY = "http://localhost:48080"
OA_DIRECT = "http://localhost:48094"
TENANT = "1"
# Integration stack: oa-server accepts dev-token; Football login token for system APIs.
OA_AUTH = "Bearer dev-token-oa-admin"
MYSQL = ["mysql", "-h", "localhost", "-P", "3306", "-uroot", "-proot", "-N"]


def http_json(
    method: str,
    path: str,
    token: str | None = None,
    body: dict | None = None,
    base: str = GATEWAY,
    auth: str | None = None,
) -> dict:
    url = f"{base}{path}"
    headers = {"tenant-id": TENANT, "X-Tenant-Id": TENANT, "Content-Type": "application/json"}
    bearer = auth or (f"Bearer {token}" if token else None)
    if bearer:
        headers["Authorization"] = bearer
    data = json.dumps(body).encode("utf-8") if body is not None else None
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    with urllib.request.urlopen(req, timeout=60) as resp:
        return json.loads(resp.read().decode("utf-8"))


def mysql_scalar(sql: str) -> str:
    proc = subprocess.run(MYSQL + ["-D", "shenyu-system", "-e", sql], capture_output=True, text=True)
    if proc.returncode != 0:
        raise RuntimeError(proc.stderr.strip() or proc.stdout)
    return (proc.stdout or "").strip().splitlines()[-1] if proc.stdout.strip() else "0"


def login() -> str:
    r = http_json(
        "POST",
        "/admin-api/system/auth/login",
        body={"username": "admin", "password": "admin123", "captchaVerification": ""},
    )
    if r.get("code") != 0:
        raise RuntimeError(f"login failed: {r}")
    return r["data"]["accessToken"]


def main() -> int:
    stamp = datetime.now().strftime("%H%M%S")
    results: list[tuple[str, str, str]] = []

    def record(step: str, ok: bool, detail: str) -> None:
        results.append((step, "PASS" if ok else "FAIL", detail))
        print(f"[{'PASS' if ok else 'FAIL'}] {step}: {detail}")

    before = int(mysql_scalar(f"SELECT COUNT(*) FROM system_operate_log WHERE biz_id=0"))
    record("0. Baseline (sanity)", True, f"sentinel_count={before}")

    token = login()
    record("1. Login", True, "admin tenant=1")

    title = f"E2E-oplog-{stamp}"
    create_body = {
        "title": title,
        "body": f"<p>body-{stamp}</p>",
        "schemeTypes": ["COMPREHENSIVE"],
        "authorId": 1000008,
        "ipGroupId": 9004,
        "creatorUserId": 1749825673829120001,
        "contentType": "ARTICLE",
        "documentType": "OFFICIAL_PLAN",
        "competitionId": "e2e-comp",
        "competitionName": "E2E赛事",
    }
    r = http_json("POST", "/admin-api/oa/content/create", token, create_body, base=OA_DIRECT, auth=OA_AUTH)
    ok = r.get("code") == 0 and r.get("data")
    content_id = r.get("data")
    record("2. POST create content", ok, f"contentId={content_id} code={r.get('code')} msg={r.get('msg')}")
    if not ok:
        print_summary(results)
        return 1

    time.sleep(1)
    after = int(mysql_scalar(f"SELECT COUNT(*) FROM system_operate_log WHERE biz_id={content_id}"))
    db_ok = after >= 1
    latest = mysql_scalar(
        f"SELECT CONCAT(id, '|', sub_type, '|', biz_id) FROM system_operate_log "
        f"WHERE biz_id={content_id} ORDER BY id DESC LIMIT 1"
    )
    record("3. DB system_operate_log insert", db_ok, f"rows_for_biz={after} latest={latest}")

    qs = urllib.parse.urlencode({"pageNo": 1, "pageSize": 20, "type": "M2-content"})
    page = http_json("GET", f"/admin-api/system/operate-log/page?{qs}", token)
    items = ((page.get("data") or {}).get("list")) or []
    hit = next((x for x in items if str(x.get("bizId")) == str(content_id) and x.get("subType") == "create"), None)
    user_name = (hit or {}).get("userName") if hit else None
    record(
        "4. GET operate-log/page API",
        page.get("code") == 0 and hit is not None and bool(user_name),
        f"total={(page.get('data') or {}).get('total')} hit={hit is not None} userName={user_name!r} bizId={content_id}",
    )

    upd = http_json(
        "PUT",
        "/admin-api/oa/content/update",
        token,
        {"id": content_id, "title": title + "-upd"},
        base=OA_DIRECT,
        auth=OA_AUTH,
    )
    record("5. PUT update content", upd.get("code") == 0, f"code={upd.get('code')} msg={upd.get('msg')}")

    time.sleep(1)
    upd_hit = mysql_scalar(
        f"SELECT COUNT(*) FROM system_operate_log WHERE type='M2-content' AND sub_type='update' AND biz_id={content_id}"
    )
    record("6. DB update log row", int(upd_hit) >= 1, f"update_rows={upd_hit}")

    print_summary(results)
    return 0 if all(r[1] == "PASS" for r in results) else 1


def print_summary(results: list[tuple[str, str, str]]) -> None:
    print("\n=== E2E Summary ===")
    print(f"{'Step':<40} {'Result':<6} Detail")
    print("-" * 90)
    for step, status, detail in results:
        print(f"{step:<40} {status:<6} {detail}")


if __name__ == "__main__":
    sys.exit(main())
