#!/usr/bin/env python3
"""P5-MIGRATE-8 Cutover smoke via Gateway :48080 /admin-api/ops/** → monorepo ops-server :48094."""
import json
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

OUT = Path(__file__).resolve().parent
GW = "http://127.0.0.1:48080"
OPS = "http://127.0.0.1:48094"


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
    results = {"slice": "P5-MIGRATE-8 Cutover", "port": 48094, "via": "gateway", "checks": []}

    st, raw = req("GET", f"{OPS}/actuator/health")
    health = json.loads(raw) if raw.startswith("{") else {"raw": raw}
    results["ops_direct_health"] = health.get("status", st)
    save("48094-health.json", {"http": st, "body": health})

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
        ("ip-group-tree", "/admin-api/ops/ip-group/tree", False),
        ("content-list", "/admin-api/ops/content/list?pageNum=1&pageSize=5", False),
        ("account-list", "/admin-api/ops/account/list?pageNum=1&pageSize=5", False),
        ("task-list", "/admin-api/ops/task/list?pageNum=1&pageSize=5", False),
        (
            "football-order-list",
            "/admin-api/ops/football-order/list?startDate=2026-01-01&endDate=2026-07-30&pageNum=1&pageSize=5",
            True,
        ),
        ("dict-data", "/admin-api/ops/dict/data?type=dict_platform_type&pageNum=1&pageSize=20", False),
        ("stub-dashboard", "/admin-api/ops/dashboard/home/summary", False),
    ]

    all_ok = True
    for name, path, need_total in checks:
        st, raw = req("GET", f"{GW}{path}", headers)
        try:
            body = json.loads(raw)
        except Exception:
            body = {"raw": raw}
        code = body.get("code")
        data = body.get("data")
        total = None
        if isinstance(data, dict):
            total = data.get("total")
            if total is None and isinstance(data.get("list"), list):
                total = len(data["list"])
        elif isinstance(data, list):
            total = len(data)
        ok = st == 200 and code == 0
        if need_total:
            ok = ok and isinstance(total, (int, float)) and total > 0
        elif name == "stub-dashboard":
            ok = st == 200 and code == 0
        entry = {
            "name": name,
            "path": path,
            "http": st,
            "code": code,
            "total": total,
            "msg": body.get("msg"),
            "ok": ok,
        }
        results["checks"].append(entry)
        save(f"{name}.json", {"http": st, "body": body})
        print(name, "http", st, "code", code, "total", total, "ok", ok)
        if not ok:
            all_ok = False

    results["ok"] = all_ok and results["ops_direct_health"] == "UP"
    results["deferred"] = [
        "Dashboard/Screen/Analysis → DeferredCutoverStubController (source in legacy-archive)",
        "collector-bind / douyin-followers / message / metadata / collect OOS → stub",
        "P6 oa:* → ops:* permission rename",
        "ops-platform-server DELETED 2026-07-31 (ADR-058 CLEANUP); Flyway SSOT=football-module-ops",
    ]
    results["rollback"] = (
        "git checkout <CLEANUP_PARENT> -- ops-platform-server  # see ROLLBACK.md"
    )
    save("RESULTS.json", results)
    print("RESULTS ok=", results["ok"])
    return 0 if results["ok"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
