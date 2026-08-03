# -*- coding: utf-8 -*-
"""E2E: plan start DingTalk notify — getByIds fix for dingtalk_user_id resolution."""
import json
import subprocess
import urllib.error
import urllib.request
from pathlib import Path

AUTH = "http://127.0.0.1:48081"
DIRECT = "http://127.0.0.1:48094"
OUT = Path(__file__).resolve().parent
TARGET_USER = "2077584621618393088"
PLAN_NAME = "E2E-DINGTALK-NOTIFY-20260802"


def req(method, url, token=None, body=None):
    data = None if body is None else json.dumps(body, ensure_ascii=False).encode()
    h = {"Content-Type": "application/json", "tenant-id": "1", "X-Tenant-Id": "1"}
    if token:
        h["Authorization"] = f"Bearer {token}"
    r = urllib.request.Request(url, data=data, headers=h, method=method)
    try:
        with urllib.request.urlopen(r, timeout=120) as resp:
            return resp.status, json.loads(resp.read().decode())
    except urllib.error.HTTPError as e:
        raw = e.read().decode(errors="replace")
        try:
            return e.code, json.loads(raw)
        except Exception:
            return e.code, {"raw": raw}


def dump(name, obj):
    (OUT / name).write_text(json.dumps(obj, ensure_ascii=False, indent=2), encoding="utf-8")


def mysql_query(sql):
    cmd = [
        "mysql", "-h", "110.42.49.224", "-P", "3306",
        "-u", "shenyu-ops", "-p2PNNcKcwY3YymEFW", "shenyu-ops",
        "-N", "-e", sql,
    ]
    p = subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8", errors="replace")
    return p.stdout.strip()


def main():
    results = {"checks": [], "targetUserId": TARGET_USER, "planNameProbe": "交付计划12"}

    st, login = req("POST", f"{AUTH}/admin-api/system/auth/login",
                     body={"username": "admin", "password": "admin123"})
    dump("00-login.json", {"http": st, "body": login})
    token = (login.get("data") or {}).get("accessToken")
    assert token, login

    # DB snapshot
    plan_row = mysql_query(
        "SELECT id, plan_name, status FROM oa_content_plan "
        "WHERE plan_name LIKE '%交付计划12%' ORDER BY id DESC LIMIT 1"
    )
    dump("db-plan-12.txt", {"row": plan_row})
    user_row = subprocess.run(
        [
            "mysql", "-h", "110.42.49.224", "-P", "3306",
            "-u", "shenyu-system", "-pJ5j7RHaJ2PHB8PSp", "shenyu-system",
            "-N", "-e",
            f"SELECT id, dingtalk_user_id FROM system_users WHERE id={TARGET_USER}",
        ],
        capture_output=True, text=True, encoding="utf-8", errors="replace",
    ).stdout.strip()
    dump("db-user-dingtalk.txt", {"row": user_row})
    params = mysql_query(
        "SELECT param_key, param_value FROM sys_param "
        "WHERE param_key LIKE 'dingtalk.%' ORDER BY param_key"
    )
    dump("db-dingtalk-params.txt", {"rows": params.splitlines() if params else []})

    st, status = req("GET", f"{DIRECT}/admin-api/ops/dev/dingtalk/status", token=token)
    dump("dingtalk-status.json", {"http": st, "body": status})
    data = status.get("data") or {}
    results["checks"].append({
        "name": "dingtalk_status",
        "ok": status.get("code") == 0 and data.get("workNotifyEnabled") is True,
        "workNotifyEnabled": data.get("workNotifyEnabled"),
        "primaryChannel": data.get("primaryChannel"),
    })

    st, test = req(
        "POST",
        f"{DIRECT}/admin-api/ops/dev/dingtalk/test-work-send",
        token=token,
        body={"userId": TARGET_USER},
    )
    dump("test-work-send.json", {"http": st, "body": test})
    td = test.get("data") or {}
    results["checks"].append({
        "name": "test_work_send",
        "ok": test.get("code") == 0 and td.get("sent") and td.get("errcode") == 0,
        "dingtalkUserId": td.get("dingtalkUserId"),
        "errcode": td.get("errcode"),
        "task_id": (td.get("raw") or {}).get("task_id"),
    })

    # Plan start smoke: create DRAFT + start (exercises NotificationServiceImpl)
    st, templates = req(
        "GET",
        f"{DIRECT}/admin-api/ops/sop/template/list?pageNo=1&pageSize=5&status=1",
        token=token,
    )
    tpl_id = None
    all_nodes = []
    for row in (templates.get("data") or {}).get("list") or []:
        if row.get("id"):
            tpl_id = row["id"]
            break
    if tpl_id:
        st, nodes = req(
            "GET",
            f"{DIRECT}/admin-api/ops/sop/node/list?templateId={tpl_id}",
            token=token,
        )
        node_list = nodes.get("data") or []
        if isinstance(node_list, dict):
            node_list = node_list.get("list") or []
        all_nodes = [row.get("id") for row in node_list if row.get("id")]

    plan_start_ok = False
    if tpl_id and all_nodes:
        steps = []
        tasks = []
        for nid in all_nodes:
            steps.append({
                "nodeId": nid,
                "competitionIds": ["4486561"],
                "assigneeIds": [TARGET_USER],
                "scheduledStart": "2026-08-02 00:00:00",
                "scheduledEnd": "2026-08-03 00:00:00",
            })
            tasks.append({
                "nodeId": nid,
                "competitionId": "4486561",
                "assigneeId": TARGET_USER,
                "scheduledStart": "2026-08-02 00:00:00",
                "scheduledEnd": "2026-08-03 00:00:00",
            })
        create_body = {
            "planName": PLAN_NAME,
            "startDate": "2026-08-02",
            "endDate": "2026-08-08",
            "templateId": tpl_id,
            "ipGroupId": 9022,
            "competitions": [{
                "competitionId": "4486561",
                "competitionName": "E2E smoke",
            }],
            "steps": steps,
            "tasks": tasks,
        }
        st, created = req("POST", f"{DIRECT}/admin-api/ops/plan/create",
                          token=token, body=create_body)
        dump("plan-create.json", {"http": st, "body": created})
        plan_id = created.get("data")
        if plan_id and created.get("code") == 0:
            st, started = req("POST", f"{DIRECT}/admin-api/ops/plan/{plan_id}/start", token=token)
            dump("plan-start.json", {"http": st, "planId": plan_id, "body": started})
            plan_start_ok = started.get("code") == 0
            if plan_start_ok:
                ev = mysql_query(
                    f"SELECT id, event_type, biz_key FROM sys_notification_event "
                    f"WHERE recipient_user_id={TARGET_USER} ORDER BY id DESC LIMIT 3"
                )
                dump("db-notification-events-after-start.txt", {"rows": ev.splitlines() if ev else []})
    results["checks"].append({
        "name": "plan_create_start",
        "ok": plan_start_ok,
        "skipped": not (tpl_id and all_nodes),
        "templateId": tpl_id,
        "nodeIds": all_nodes,
    })

    # Log probe: must NOT contain skip reason for target user after fix
    log_path = Path(__file__).resolve().parents[4] / "scripts" / "logs" / "ops-server-nacos-run.log"
    log_tail = ""
    if log_path.exists():
        log_tail = log_path.read_text(encoding="utf-8", errors="replace")[-8000:]
    skip_needle = f"work notify skipped for user {TARGET_USER}: no dingtalk_user_id"
    recent_skip = skip_needle in log_tail
    dump("log-tail-snippet.txt", {"recentContainsSkip": recent_skip, "tail": log_tail[-2000:]})
    results["checks"].append({
        "name": "log_no_dingtalk_skip_after_fix",
        "ok": not recent_skip,
        "note": "false=good (no skip log for target user in recent tail)",
    })

    results["pass"] = all(c.get("ok") for c in results["checks"] if not c.get("skipped"))
    dump("RESULTS.json", results)
    print(json.dumps(results, ensure_ascii=False, indent=2))
    return 0 if results["pass"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
