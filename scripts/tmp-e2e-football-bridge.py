#!/usr/bin/env python3
"""E2E: OPS create content -> Football author_article sync -> member article get/page."""
from __future__ import annotations

import json
import sys
import time
import urllib.error
import urllib.request
from datetime import datetime

GATEWAY = "http://localhost:48080"
TENANT = "1"


def http_json(method: str, path: str, token: str | None = None, body: dict | None = None) -> dict:
    url = f"{GATEWAY}{path}"
    headers = {"tenant-id": TENANT, "X-Tenant-Id": TENANT, "Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    data = json.dumps(body).encode("utf-8") if body is not None else None
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    with urllib.request.urlopen(req, timeout=60) as resp:
        return json.loads(resp.read().decode("utf-8"))


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

    token = login()
    record("1. Login", True, "admin tenant=1")

    create_body = {
        "title": f"E2E-bridge-{stamp}",
        "body": f"<p>body-{stamp}</p>",
        "paidBody": f"<p>paid-{stamp}</p>",
        "freeBody": f"<p>free-{stamp}</p>",
        "schemeTypes": ["COMPREHENSIVE"],
        "authorId": 1000008,
        "ipGroupId": 9004,
        "creatorUserId": 1749825673829120001,
        "contentType": "ARTICLE",
        "documentType": "OFFICIAL_PLAN",
        "competitionId": "e2e-comp",
        "competitionName": "E2E赛事",
    }
    r = http_json("POST", "/admin-api/oa/content/create", token, create_body)
    ok = r.get("code") == 0 and r.get("data")
    content_id = r.get("data")
    record("2. POST create content", ok, f"contentId={content_id} code={r.get('code')} msg={r.get('msg')}")
    if not ok:
        print_summary(results)
        return 1

    scheme = http_json("GET", f"/admin-api/oa/content/{content_id}/football-scheme", token)
    author_article_id = (scheme.get("data") or {}).get("authorArticleId")
    sync_ok = scheme.get("code") == 0 and author_article_id
    record(
        "3. football-scheme ext",
        sync_ok,
        f"authorArticleId={author_article_id} err={(scheme.get('data') or {}).get('footballSyncError')}",
    )
    if not sync_ok:
        print_summary(results)
        return 1

    get_r = http_json("GET", f"/admin-api/member/article/get?id={author_article_id}", token)
    get_ok = get_r.get("code") == 0
    record("4. GET member/article/get", get_ok, f"id={author_article_id} code={get_r.get('code')} msg={get_r.get('msg')}")

    page_r = http_json("GET", f"/admin-api/member/article/page?pageNo=1&pageSize=50", token)
    page_list = (page_r.get("data") or {}).get("list") or []
    in_page = any(row.get("id") == author_article_id for row in page_list)
    record(
        "5. GET member/article/page",
        page_r.get("code") == 0 and in_page,
        f"id={author_article_id} in_page={in_page} total={(page_r.get('data') or {}).get('total')}",
    )

    shelf_r = http_json("POST", f"/admin-api/oa/content/{content_id}/shelf-on", token, {})
    shelf_ok = shelf_r.get("code") == 0 and (shelf_r.get("data") or {}).get("shelfStatus") == 1
    record(
        "6. shelf-on (optional)",
        shelf_ok,
        f"shelfStatus={(shelf_r.get('data') or {}).get('shelfStatus')} code={shelf_r.get('code')}",
    )

    print_summary(results)
    failed = sum(1 for _, status, _ in results if status == "FAIL")
    return 1 if failed else 0


def print_summary(results: list[tuple[str, str, str]]) -> None:
    print("\n=== E2E Summary ===")
    print("| Step | Result | Detail |")
    print("|------|--------|--------|")
    for step, status, detail in results:
        print(f"| {step} | {status} | {detail} |")


if __name__ == "__main__":
    raise SystemExit(main())
