#!/usr/bin/env python3
"""API smoke for M6 private-domain report MVP."""
from __future__ import annotations

import json
import sys
import urllib.error
import urllib.parse
import urllib.request

BASE = "http://localhost:48080/admin-api"
ARTIFACT_DIR = None


def req(method: str, path: str, body=None, token=None):
    url = BASE + path
    headers = {"Content-Type": "application/json", "tenant-id": "1", "X-Tenant-Id": "1"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    data = json.dumps(body).encode("utf-8") if body is not None else None
    request = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request, timeout=120) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", errors="replace")
        try:
            payload = json.loads(raw)
            payload["_httpStatus"] = e.code
            return payload
        except json.JSONDecodeError:
            return {"code": e.code, "msg": raw, "data": None, "_httpStatus": e.code}


def save(name: str, payload) -> None:
    if not ARTIFACT_DIR:
        return
    path = ARTIFACT_DIR / f"{name}.json"
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")


def main() -> int:
    global ARTIFACT_DIR
    if len(sys.argv) > 1:
        from pathlib import Path

        ARTIFACT_DIR = Path(sys.argv[1])
        ARTIFACT_DIR.mkdir(parents=True, exist_ok=True)

    login = req("POST", "/system/auth/login", {"username": "admin", "password": "admin123"})
    save("00-login", login)
    if login.get("code") != 0:
        print("LOGIN FAIL", login)
        return 1
    token = login["data"]["accessToken"]

    authors = req("GET", "/ops/private-domain-report/authors", token=token)
    save("01-authors", authors)
    print("authors:", authors.get("code"), "count=", len(authors.get("data") or []))

    monthly = req("GET", "/ops/private-domain-report/monthly-achievement?month=2026-08", token=token)
    save("02-monthly-achievement", monthly)
    print("monthly:", monthly.get("code"), "rows=", len(monthly.get("data") or []))

    weekly = req(
        "GET",
        "/ops/private-domain-report/weekly-funnel?weekStart=2026-08-11&weekEnd=2026-08-17",
        token=token,
    )
    save("03-weekly-funnel", weekly)
    print("weekly:", weekly.get("code"), "rows=", len(weekly.get("data") or []))

    feedback_save = None
    feedback_get = None
    weekly_rows = weekly.get("data") or []
    if weekly.get("code") == 0 and weekly_rows:
        row = weekly_rows[0]
        wl = row.get("weekLabel")
        aid = row.get("authorId")
        ch = row.get("channel") or "SUMMARY"
        feedback_save = req(
            "PUT",
            "/ops/private-domain-report/weekly-feedback",
            {
                "weekLabel": wl,
                "authorId": aid,
                "channel": ch,
                "feedbackText": "API smoke feedback",
            },
            token=token,
        )
        save("04-weekly-feedback-save", feedback_save)
        q = urllib.parse.urlencode({"weekLabel": wl, "authorId": aid, "channel": ch})
        feedback_get = req("GET", f"/ops/private-domain-report/weekly-feedback?{q}", token=token)
        save("05-weekly-feedback-get", feedback_get)
        print("feedback save/get:", feedback_save.get("code"), feedback_get.get("code"))

    failed = []
    for name, resp in [
        ("authors", authors),
        ("monthly", monthly),
        ("weekly", weekly),
        ("feedback_save", feedback_save),
        ("feedback_get", feedback_get),
    ]:
        if resp is not None and resp.get("code") != 0:
            failed.append(name)

    if failed:
        print("FAILED:", failed)
        return 1
    print("ALL OK")
    return 0


if __name__ == "__main__":
    sys.exit(main())
