# -*- coding: utf-8 -*-
"""opsleader plan list smoke — expect created plans in led IP group visible."""
from __future__ import annotations

import json
import urllib.error
import urllib.request
from pathlib import Path

GW = "http://127.0.0.1:48080"
OUT = Path(__file__).resolve().parent
OUT.mkdir(parents=True, exist_ok=True)


def req(method: str, url: str, token: str | None = None, body=None, tenant: str = "1"):
    data = None if body is None else json.dumps(body, ensure_ascii=False).encode()
    headers = {
        "Content-Type": "application/json",
        "tenant-id": tenant,
        "X-Tenant-Id": tenant,
    }
    if token:
        headers["Authorization"] = f"Bearer {token}"
    request = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request, timeout=60) as resp:
            return resp.status, json.loads(resp.read().decode())
    except urllib.error.HTTPError as e:
        raw = e.read().decode(errors="replace")
        try:
            return e.code, json.loads(raw)
        except Exception:
            return e.code, {"raw": raw}


def main() -> int:
    results: dict = {"pass": False, "steps": []}

    st, login = req(
        "POST",
        f"{GW}/admin-api/system/auth/login",
        body={"username": "opsleader", "password": "admin123"},
    )
    (OUT / "00-login.json").write_text(
        json.dumps({"http": st, "body": login}, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    token = (login.get("data") or {}).get("accessToken")
    uid = (login.get("data") or {}).get("userId")
    results["steps"].append({"login": {"http": st, "code": login.get("code"), "userId": uid}})
    print("LOGIN", st, login.get("code"), "userId", uid)
    if not token:
        (OUT / "RESULTS.json").write_text(json.dumps(results, ensure_ascii=False, indent=2), encoding="utf-8")
        return 1

    st, body = req("GET", f"{GW}/admin-api/ops/plan/list?pageNo=1&pageSize=20", token=token)
    (OUT / "01-plan-list.json").write_text(
        json.dumps({"http": st, "body": body}, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    data = body.get("data") if isinstance(body, dict) else None
    lst = (data or {}).get("list") if isinstance(data, dict) else None
    total = (data or {}).get("total") if isinstance(data, dict) else None
    ids = [x.get("id") for x in (lst or [])]
    results["steps"].append(
        {
            "list": {
                "http": st,
                "code": body.get("code") if isinstance(body, dict) else None,
                "total": total,
                "ids": ids,
                "names": [x.get("planName") for x in (lst or [])],
            }
        }
    )
    print("LIST", st, body.get("code") if isinstance(body, dict) else None, "total", total, "ids", ids)

    # detail of known created plan 17
    st17, body17 = req("GET", f"{GW}/admin-api/ops/plan/get?id=17", token=token)
    (OUT / "02-plan-get-17.json").write_text(
        json.dumps({"http": st17, "body": body17}, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    results["steps"].append(
        {
            "get17": {
                "http": st17,
                "code": body17.get("code") if isinstance(body17, dict) else None,
                "id": (body17.get("data") or {}).get("id") if isinstance(body17, dict) else None,
                "ipGroupId": (body17.get("data") or {}).get("ipGroupId") if isinstance(body17, dict) else None,
            }
        }
    )
    print("GET17", st17, body17.get("code") if isinstance(body17, dict) else None)

    has_created = any(i in {17, 18, 19, 20} for i in ids)
    list_ok = (
        isinstance(body, dict)
        and body.get("code") == 0
        and isinstance(total, int)
        and total > 0
        and has_created
    )
    get_code = body17.get("code") if isinstance(body17, dict) else None
    results["pass"] = list_ok
    results["hasCreatedPlan"] = has_created
    results["get17Note"] = (
        "pre-existing DataAccessException (sys_dict_data missing on detail path); "
        "out of scope for list empty bug"
        if get_code != 0
        else "ok"
    )
    (OUT / "RESULTS.json").write_text(json.dumps(results, ensure_ascii=False, indent=2), encoding="utf-8")
    report = [
        "# Plan list IP组长 scope smoke 2026-08-02",
        "",
        f"| Login opsleader | userId={uid} |",
        f"| plan/list total | {total} ids={ids} |",
        f"| Contains created 17/18/19/20 | {has_created} |",
        f"| plan/get id=17 | code={get_code} (detail path; not list DoD) |",
        "",
        f"**Verdict**: {'PASS' if list_ok else 'FAIL'} (list shows led-group plans)",
        "",
    ]
    (OUT / "REPORT.md").write_text("\n".join(report), encoding="utf-8")
    print("PASS" if list_ok else "FAIL")
    return 0 if list_ok else 1


if __name__ == "__main__":
    raise SystemExit(main())
