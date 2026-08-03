#!/usr/bin/env python3
"""P5-MIGRATE-5 smoke: SOP/Task list + regression on :48095; keep :48094 UP."""
import json
import os
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

OUT = Path(__file__).resolve().parent
OPS = "http://127.0.0.1:48095"
GW = "http://127.0.0.1:48080"
PROD = "http://127.0.0.1:48094"


def req(method, url, headers=None, data=None, timeout=45):
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
    results = {"slice": "P5-MIGRATE-5 SOP / Task", "port": 48095, "checks": []}

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
        ("task-list", "/admin-api/oa/task/list?pageNum=1&pageSize=5"),
        ("sop-template-list", "/admin-api/oa/sop/template/list?pageNum=1&pageSize=5"),
        ("ip-group-tree", "/admin-api/oa/ip-group/tree"),
        ("ip-group-list", "/admin-api/oa/ip-group/list?pageNum=1&pageSize=5"),
        ("content-list", "/admin-api/oa/content/list?pageNum=1&pageSize=5"),
        ("account-list", "/admin-api/oa/account/list?pageNum=1&pageSize=5"),
    ]

    all_ok = True
    for label, path in checks:
        st, raw = req("GET", OPS + path, headers)
        try:
            j = json.loads(raw)
        except Exception:
            j = {"code": None, "msg": raw[:300]}
        save(f"{label}.json", j)
        data = j.get("data") or {}
        total = data.get("total") if isinstance(data, dict) else None
        if total is None and isinstance(data, list):
            total = len(data)
        entry = {"path": path, "http": st, "code": j.get("code"), "total": total, "msg": j.get("msg")}
        results["checks"].append(entry)
        ok = j.get("code") == 0
        all_ok = all_ok and ok
        print(f"{label} HTTP={st} code={j.get('code')} total={total} msg={str(j.get('msg'))[:80]}")

    results["ok"] = all_ok and results["production_48094"] == "UP"
    results["deferred"] = [
        "task execute upload (FileApi → P5-MIGRATE-6)",
        "HomeDashboard / TodoReminder real notify",
        "绩效/Performance domain",
        "ContentPlan generator wiring (plan UI)",
    ]
    results["next"] = "P5-MIGRATE-6 System 支撑（dict/param/message/file/metadata）"
    save("RESULTS.json", results)
    print("RESULTS", results["ok"], "prod48094", results["production_48094"])
    return 0 if results["ok"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
