# -*- coding: utf-8 -*-
"""Smoke: BOOLEAN sys_param update — dingtalk.robot.enabled true/false (V171 dict_param_category fix)."""
import json
import urllib.error
import urllib.request
from pathlib import Path

AUTH = "http://127.0.0.1:48081"
DIRECT = "http://127.0.0.1:48094"
OUT = Path(__file__).resolve().parent


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


def main():
    results = {"checks": []}

    st, login = req(
        "POST",
        f"{AUTH}/admin-api/system/auth/login",
        body={"username": "admin", "password": "admin123"},
    )
    dump("00-login.json", {"http": st, "body": login})
    token = (login.get("data") or {}).get("accessToken")
    assert token, login

    for key in ("dingtalk.robot.enabled", "dingtalk.enabled"):
        row = find_param(token, key)
        assert row, f"missing param {key}"
        original = row.get("paramValue")
        toggle = "true" if original != "true" else "false"
        payload = {
            "id": row["id"],
            "paramName": row["paramName"],
            "paramKey": row["paramKey"],
            "paramValue": toggle,
            "paramType": row["paramType"],
            "category": row["category"],
            "remark": row.get("remark"),
        }
        st, upd = req("PUT", f"{DIRECT}/admin-api/ops/system/param/update", token=token, body=payload)
        dump(f"update-{key.replace('.', '-')}-{toggle}.json", {"http": st, "body": upd, "payload": payload})
        ok = st == 200 and upd.get("code") == 0
        results["checks"].append({"name": f"update_{key}_to_{toggle}", "ok": ok, "msg": upd.get("msg")})
        if ok:
            # restore
            payload["paramValue"] = original
            req("PUT", f"{DIRECT}/admin-api/ops/system/param/update", token=token, body=payload)

    results["pass"] = all(c.get("ok") for c in results["checks"])
    dump("RESULTS.json", results)
    print(json.dumps(results, ensure_ascii=False, indent=2))
    return 0 if results["pass"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
