#!/usr/bin/env python3
"""Verify LAYOUT HTML sync: OPS layout_html -> author_article.content with formatting tags."""
from __future__ import annotations

import json
import sys
import urllib.error
import urllib.request
from datetime import datetime

GATEWAY = "http://localhost:48080"
TENANT = "1"
HTML = "<section class=\"layout-article\"><p><strong>bold</strong></p></section>"


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
    token = login()
    print("[PASS] login")

    create_body = {
        "title": f"E2E-html-{stamp}",
        "body": "bold",
        "paidBody": "bold",
        "freeBody": f"<p>free-{stamp}</p>",
        "bodyFormat": "LAYOUT",
        "layoutHtml": HTML,
        "layoutJson": {"version": 1, "blocks": []},
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
    if r.get("code") != 0:
        print(f"[FAIL] create: {r}")
        return 1
    content_id = r["data"]
    print(f"[PASS] create contentId={content_id}")

    scheme = http_json("GET", f"/admin-api/oa/content/{content_id}/football-scheme", token)
    author_article_id = (scheme.get("data") or {}).get("authorArticleId")
    if not author_article_id:
        print(f"[FAIL] no authorArticleId: {scheme}")
        return 1
    print(f"[PASS] authorArticleId={author_article_id}")

    get_r = http_json("GET", f"/admin-api/member/article/get?id={author_article_id}", token)
    content = (get_r.get("data") or {}).get("content") or ""
    has_strong = "<strong>bold</strong>" in content
    print(f"[{'PASS' if has_strong else 'FAIL'}] article/get content has <strong>: {content[:120]!r}")

    # retry sync on existing LAYOUT record id=20 if present
    retry_r = http_json("POST", "/admin-api/oa/content/20/sync-football-scheme", token, {})
    if retry_r.get("code") == 0:
        aa_id = (retry_r.get("data") or {}).get("authorArticleId")
        if aa_id:
            retry_get = http_json("GET", f"/admin-api/member/article/get?id={aa_id}", token)
            retry_content = (retry_get.get("data") or {}).get("content") or ""
            retry_ok = "<h2>" in retry_content or "<strong>" in retry_content or "<table>" in retry_content
            print(f"[{'PASS' if retry_ok else 'FAIL'}] retry sync content id=20 -> {aa_id}: {retry_content[:120]!r}")
    else:
        print(f"[SKIP] retry sync id=20: {retry_r.get('msg')}")

    return 0 if has_strong else 1


if __name__ == "__main__":
    raise SystemExit(main())
