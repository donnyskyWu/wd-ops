#!/usr/bin/env python3
"""P-G legacy-archive delete smoke — ops health + account list."""
import json
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

OUT = Path(__file__).resolve().parent
OPS = "http://127.0.0.1:48094"
GW = "http://127.0.0.1:48080"


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
    (OUT / name).write_text(json.dumps(obj, ensure_ascii=False, indent=2), encoding="utf-8")


def build_headers(token=None, user_id=1):
    login_user = {
        "id": int(user_id) if str(user_id).isdigit() else user_id,
        "userType": 2,
        "tenantId": 1,
        "scopes": [],
        "expiresTime": 1893456000000,
        "info": {"username": "admin", "nickname": "admin", "isAdmin": "true"},
    }
    login_user_hdr = urllib.parse.quote(
        json.dumps(login_user, separators=(",", ":")), safe=""
    )
    headers = {
        "tenant-id": "1",
        "X-Tenant-Id": "1",
        "login-user": login_user_hdr,
    }
    if token:
        headers["Authorization"] = f"Bearer {token}"
    return headers


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    results = {
        "slice": "P-G",
        "date": "2026-07-31",
        "removed_tracked": 422,
        "removed_untracked_disk": 158,
        "removed_total_files": 580,
        "rollback_commit_with_archive": "7e5f1b709636f8c2b0a2392c178858631639870a",
        "rollback_note": (
            "git -C football-backend-saas checkout 7e5f1b709 -- "
            "football-module-ops/football-module-ops-server/legacy-archive"
        ),
        "maven_compile_path": "default src/main/java only; legacy-archive never on classpath",
        "checks": [],
    }

    st, raw = req("GET", f"{OPS}/actuator/health", timeout=10)
    save("ops-health.json", {"http": st, "body": raw[:300]})
    results["ops_health"] = st

    token = None
    user_id = 1
    st, raw = req(
        "POST",
        f"{GW}/admin-api/system/auth/login",
        {"tenant-id": "1", "X-Tenant-Id": "1"},
        json.dumps({"username": "admin", "password": "admin123"}),
        timeout=15,
    )
    save("00-login.json", {"http": st, "body": raw[:500]})
    if st == 200:
        login = json.loads(raw)
        token = (login.get("data") or {}).get("accessToken")
        user_id = (login.get("data") or {}).get("userId") or 1

    headers = build_headers(token, user_id)
    checks = [
        ("direct-ops-account-list", f"{OPS}/admin-api/ops/account/list?pageNo=1&pageSize=10"),
        ("gw-ops-account-list", f"{GW}/admin-api/ops/account/list?pageNo=1&pageSize=10"),
    ]
    for name, url in checks:
        st, raw = req("GET", url, headers, timeout=60)
        code = None
        try:
            code = json.loads(raw).get("code")
        except Exception:
            pass
        save(f"{name}.json", {"http": st, "code": code, "body": raw[:1200]})
        ok = st == 200 and code == 0
        results["checks"].append(
            {"name": name, "url": url, "http": st, "code": code, "pass": ok}
        )
        print(f"{name}: http={st} code={code} pass={ok}")

    health_ok = results.get("ops_health") == 200
    pass_n = sum(1 for c in results["checks"] if c["pass"]) + (1 if health_ok else 0)
    total = len(results["checks"]) + 1
    results["pass"] = f"{pass_n}/{total}"
    save("RESULTS.json", results)
    print(f"SUMMARY {results['pass']}")
    return 0 if pass_n == total else 1


if __name__ == "__main__":
    raise SystemExit(main())
