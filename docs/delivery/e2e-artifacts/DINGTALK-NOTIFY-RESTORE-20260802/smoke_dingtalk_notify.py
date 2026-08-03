# -*- coding: utf-8 -*-
"""Smoke: ADR-026 DingTalk notification restore — params + dev status + plan start graceful."""
import json
import urllib.error
import urllib.request
from pathlib import Path

GW = "http://127.0.0.1:48080"
AUTH = "http://127.0.0.1:48081"
DIRECT = "http://127.0.0.1:48094"
OUT = Path(__file__).resolve().parent

PARAM_KEYS = [
    "dingtalk.enabled",
    "dingtalk.client-id",
    "dingtalk.client-secret",
    "dingtalk.corp-id",
    "dingtalk.agent-id",
    "dingtalk.robot.enabled",
    "dingtalk.robot.webhook-url",
    "dingtalk.robot.secret",
    "notification.platform-base-url",
]


def req(method, url, token=None, body=None):
    data = None if body is None else json.dumps(body, ensure_ascii=False).encode()
    h = {
        "Content-Type": "application/json",
        "tenant-id": "1",
        "X-Tenant-Id": "1",
    }
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


def main():
    results = {"checks": []}

    st, login = req(
        "POST",
        f"{AUTH}/admin-api/system/auth/login",
        body={"username": "admin", "password": "admin123"},
    )
    dump("00-login.json", {"http": st, "body": login})
    token = (login.get("data") or {}).get("accessToken")
    assert token, f"login failed: {login}"

    # V170 param definitions (category DINGTALK / NOTIFICATION)
    st, params = req(
        "GET",
        f"{DIRECT}/admin-api/ops/system/param/list?pageNo=1&pageSize=50&category=DINGTALK",
        token=token,
    )
    dump("param-dingtalk-list.json", {"http": st, "body": params})
    found_keys = {
        row.get("paramKey")
        for row in (params.get("data") or {}).get("list") or []
    }
    missing = [k for k in PARAM_KEYS if k not in found_keys and not k.startswith("notification.")]
    results["checks"].append({"name": "dingtalk_params_seeded", "ok": len(missing) == 0, "missing": missing})

    st, notif_params = req(
        "GET",
        f"{DIRECT}/admin-api/ops/system/param/list?pageNo=1&pageSize=10&paramKey=notification.platform-base-url",
        token=token,
    )
    dump("param-notification-list.json", {"http": st, "body": notif_params})

    # dev status — empty params => graceful skip
    for base, label in [(DIRECT, "direct")]:
        st, status = req("GET", f"{base}/admin-api/ops/dev/dingtalk/status", token=token)
        dump(f"dingtalk-status-{label}.json", {"http": st, "body": status})
        data = status.get("data") or {}
        results["checks"].append({
            "name": f"dev_status_{label}",
            "http": st,
            "code": status.get("code"),
            "primaryChannel": data.get("primaryChannel"),
            "sendEnabled": data.get("sendEnabled"),
            "skipReason": data.get("skipReason") or data.get("workNotifySkipReason"),
        })

    # Find a DRAFT plan to start (or skip)
    st, plans = req(
        "GET",
        f"{DIRECT}/admin-api/ops/plan/list?pageNo=1&pageSize=5&status=DRAFT",
        token=token,
    )
    dump("plan-draft-list.json", {"http": st, "body": plans})
    draft = None
    for row in (plans.get("data") or {}).get("list") or []:
        if row.get("status") == "DRAFT":
            draft = row
            break

    if draft:
        plan_id = draft.get("id")
        st, start = req("POST", f"{DIRECT}/admin-api/ops/plan/{plan_id}/start", token=token)
        dump("plan-start.json", {"http": st, "planId": plan_id, "body": start})
        results["checks"].append({
            "name": "plan_start",
            "planId": plan_id,
            "http": st,
            "code": start.get("code"),
            "msg": start.get("msg"),
        })
    else:
        results["checks"].append({"name": "plan_start", "skipped": True, "reason": "no DRAFT plan"})

    # Verify NotificationServiceImpl active: in-app message may exist after plan start
    st, msgs = req(
        "GET",
        f"{DIRECT}/admin-api/ops/system/message/unread?pageNo=1&pageSize=5",
        token=token,
    )
    dump("message-unread.json", {"http": st, "body": msgs})

    ok = (
        all(c.get("ok", True) for c in results["checks"] if "ok" in c)
        and results["checks"][1].get("code") == 0
    )
    results["pass"] = ok
    dump("RESULTS.json", results)
    print(json.dumps(results, ensure_ascii=False, indent=2))
    return 0 if ok else 1


if __name__ == "__main__":
    raise SystemExit(main())
