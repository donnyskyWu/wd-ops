#!/usr/bin/env python3
"""P5-MIGRATE-7 smoke: football-order/list + regression on :48095; keep :48094 UP."""
import json
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

OUT = Path(__file__).resolve().parent
OPS = "http://127.0.0.1:48095"
GW = "http://127.0.0.1:48080"
PROD = "http://127.0.0.1:48094"


def req(method, url, headers=None, data=None, timeout=60):
    h = dict(headers or {})
    body = None
    if data is not None:
        body = data.encode() if isinstance(data, str) else data
        h.setdefault("Content-Type", "application/json")
    r = urllib.request.Request(url, data=body, headers=h, method=method)
    try:
        with urllib.request.urlopen(r, timeout=timeout) as resp:
            return resp.status, resp.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "replace")


def save(name, obj):
    path = OUT / name
    path.write_text(json.dumps(obj, ensure_ascii=False, indent=2), encoding="utf-8")
    return path


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    results = {"slice": "P5-MIGRATE-7 Analytics / 订单只读", "port": 48095, "checks": []}

    st, raw = req("GET", f"{PROD}/actuator/health")
    prod = json.loads(raw) if raw.startswith("{") else {"raw": raw}
    results["production_48094"] = prod.get("status", st)
    save("48094-health.json", {"http": st, "body": prod})

    st, raw = req(
        "POST",
        f"{GW}/admin-api/system/auth/login",
        {"tenant-id": "1", "X-Tenant-Id": "1"},
        json.dumps({"username": "admin", "password": "admin123"}),
    )
    login = json.loads(raw)
    save("00-login.json", login)
    token = (login.get("data") or {}).get("accessToken")
    user_id = (login.get("data") or {}).get("userId")
    print("login", st, login.get("code"), "token", bool(token), "userId", user_id)

    login_user = {
        "id": int(user_id) if user_id and str(user_id).isdigit() else user_id,
        "userType": 2,
        "tenantId": 1,
        "scopes": [],
        "expiresTime": 1893456000000,
        "info": {"username": "admin", "nickname": "admin", "isAdmin": "true"},
    }
    login_user_hdr = urllib.parse.quote(json.dumps(login_user, separators=(",", ":")), safe="")

    headers = {
        "Authorization": f"Bearer {token}",
        "tenant-id": "1",
        "X-Tenant-Id": "1",
        "login-user": login_user_hdr,
    }

    checks = [
        (
            "football-order-list",
            "/admin-api/oa/football-order/list?startDate=2026-01-01&endDate=2026-07-30&pageNum=1&pageSize=20",
            True,
        ),
        ("content-list", "/admin-api/oa/content/list?pageNum=1&pageSize=5", False),
        ("account-list", "/admin-api/oa/account/list?pageNum=1&pageSize=5", False),
        ("task-list", "/admin-api/oa/task/list?pageNum=1&pageSize=5", False),
    ]

    all_ok = True
    for label, path, require_total in checks:
        st, raw = req("GET", OPS + path, headers)
        try:
            body = json.loads(raw)
        except Exception:
            body = {"raw": raw[:500]}
        save(f"{label}.json", {"http": st, "body": body})
        code = body.get("code")
        data = body.get("data") or {}
        total = data.get("total")
        list_len = len(data.get("list") or []) if isinstance(data, dict) else None
        ok = st == 200 and code == 0
        if require_total:
            ok = ok and isinstance(total, (int, float)) and total > 0
        entry = {
            "path": path,
            "http": st,
            "code": code,
            "total": total,
            "list_len": list_len,
            "msg": body.get("msg"),
            "ok": ok,
        }
        results["checks"].append(entry)
        print(label, entry)
        all_ok = all_ok and ok

    results["ok"] = all_ok and results["production_48094"] == "UP"
    results["deferred"] = [
        "Analytics / ROI / dashboard full suite",
        "Screen / AI / fullscreen stubs (not needed for order list)",
        "Gateway cutover / P6 permissions",
    ]
    results["next"] = "P5-MIGRATE-8 Cutover / remaining domains"
    save("RESULTS.json", results)
    print("RESULT", "PASS" if results["ok"] else "FAIL", "48094", results["production_48094"])
    raise SystemExit(0 if results["ok"] else 1)


if __name__ == "__main__":
    main()
