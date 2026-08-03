# -*- coding: utf-8 -*-
"""E2E smoke: DINGTALK sys_param update — all keys + @InDict category validation (V171 fix)."""
import json
import urllib.error
import urllib.request
from pathlib import Path

AUTH = "http://127.0.0.1:48081"
DIRECT = "http://127.0.0.1:48094"
OUT = Path(__file__).resolve().parent

DINGTALK_KEYS = [
    "dingtalk.enabled",
    "dingtalk.client-id",
    "dingtalk.client-secret",
    "dingtalk.corp-id",
    "dingtalk.agent-id",
    "dingtalk.robot.enabled",
    "dingtalk.robot.webhook-url",
    "dingtalk.robot.secret",
]

# Minimal non-destructive test values (restored after each update)
TEST_VALUES = {
    "dingtalk.enabled": "false",
    "dingtalk.client-id": "e2e-test-client-id",
    "dingtalk.client-secret": "e2e-test-secret",
    "dingtalk.corp-id": "e2e-corp",
    "dingtalk.agent-id": "1234567890",
    "dingtalk.robot.enabled": "false",
    "dingtalk.robot.webhook-url": "https://example.com/webhook",
    "dingtalk.robot.secret": "e2e-robot-secret",
}


def req(method, url, token=None, body=None):
    data = None if body is None else json.dumps(body, ensure_ascii=False).encode()
    h = {"Content-Type": "application/json", "tenant-id": "1", "X-Tenant-Id": "1"}
    if token:
        h["Authorization"] = f"Bearer {token}"
    r = urllib.request.Request(url, data=data, headers=h, method=method)
    try:
        with urllib.request.urlopen(r, timeout=60) as resp:
            return resp.status, json.loads(resp.read().decode())
    except urllib.error.HTTPError as e:
        raw = e.read().decode(errors="replace")
        try:
            return e.code, json.loads(raw)
        except Exception:
            return e.code, {"raw": raw}


def dump(name, obj):
    (OUT / name).write_text(json.dumps(obj, ensure_ascii=False, indent=2), encoding="utf-8")


def find_param(token, param_key):
    st, body = req(
        "GET",
        f"{DIRECT}/admin-api/ops/system/param/list?pageNo=1&pageSize=50&paramKey={param_key}",
        token=token,
    )
    assert st == 200 and body.get("code") == 0, body
    for row in (body.get("data") or {}).get("list") or []:
        if row.get("paramKey") == param_key:
            return row
    return None


def update_param(token, row, new_value):
    payload = {
        "id": row["id"],
        "paramName": row["paramName"],
        "paramKey": row["paramKey"],
        "paramValue": new_value,
        "paramType": row["paramType"],
        "category": row["category"],
        "remark": row.get("remark"),
    }
    return req("PUT", f"{DIRECT}/admin-api/ops/system/param/update", token=token, body=payload), payload


def main():
    results = {"checks": [], "category": "DINGTALK"}

    st, login = req(
        "POST",
        f"{AUTH}/admin-api/system/auth/login",
        body={"username": "admin", "password": "admin123"},
    )
    dump("00-login.json", {"http": st, "body": login})
    token = (login.get("data") or {}).get("accessToken")
    assert token, login

    # List by category DINGTALK
    st, list_body = req(
        "GET",
        f"{DIRECT}/admin-api/ops/system/param/list?pageNo=1&pageSize=50&category=DINGTALK",
        token=token,
    )
    dump("param-dingtalk-list.json", {"http": st, "body": list_body})
    found = {r.get("paramKey") for r in (list_body.get("data") or {}).get("list") or []}
    missing = [k for k in DINGTALK_KEYS if k not in found]
    results["checks"].append({"name": "list_by_category", "ok": st == 200 and list_body.get("code") == 0, "missing": missing})

    # Update each dingtalk param
    for key in DINGTALK_KEYS:
        row = find_param(token, key)
        if not row:
            results["checks"].append({"name": f"update_{key}", "ok": False, "msg": "param not found"})
            continue
        original = row.get("paramValue")
        test_val = TEST_VALUES.get(key, original)
        if row.get("paramType") == "BOOLEAN":
            test_val = "true" if original != "true" else "false"
        (st, upd), payload = update_param(token, row, test_val)
        dump(f"update-{key.replace('.', '-')}.json", {"http": st, "body": upd, "payload": payload})
        ok = st == 200 and upd.get("code") == 0
        err_msg = upd.get("msg") or ""
        is_1503 = "1503" in str(upd.get("code", "")) or "字典" in err_msg
        results["checks"].append({
            "name": f"update_{key}",
            "ok": ok,
            "code": upd.get("code"),
            "msg": err_msg,
            "category_error": is_1503,
        })
        if ok:
            update_param(token, row, original)

    # Priority params called out in task
    priority = [c for c in results["checks"] if c["name"] in ("update_dingtalk.client-id", "update_dingtalk.enabled", "update_dingtalk.robot.enabled")]
    results["priority_pass"] = all(c.get("ok") for c in priority)

    # Optional dev status
    st, status = req("GET", f"{DIRECT}/admin-api/ops/dev/dingtalk/status", token=token)
    dump("dingtalk-status.json", {"http": st, "body": status})
    results["dingtalk_status"] = {"http": st, "code": status.get("code")}

    results["pass"] = all(c.get("ok") for c in results["checks"])
    dump("RESULTS.json", results)
    print(json.dumps(results, ensure_ascii=False, indent=2))
    return 0 if results["pass"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
