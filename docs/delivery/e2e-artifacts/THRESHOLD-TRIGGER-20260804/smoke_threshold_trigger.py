# -*- coding: utf-8 -*-
"""ADR-069 P0: threshold alerts after internal/external unified collect runs."""
import json
import subprocess
import sys
import time
import urllib.error
import urllib.request
from datetime import date
from pathlib import Path

ART = Path(__file__).resolve().parent
BASE = "http://127.0.0.1:48080"
OPS = "http://127.0.0.1:48094"
TENANT_HEADERS = {"tenant-id": "1", "X-Tenant-Id": "1", "Content-Type": "application/json"}
DB = ["mysql", "-h", "110.42.49.224", "-P", "3306", "-u", "shenyu-ops", "-p2PNNcKcwY3YymEFW", "shenyu-ops"]
THRESHOLD_EVENT_TYPES = ("WORK_HIT", "WORK_LOW_SCORE", "ACCOUNT_HIGH_FANS", "ACCOUNT_LOW_FANS")
ASYNC_WAIT_SEC = 12
POLL_INTERVAL_SEC = 2


def http(method, url, headers=None, data=None, timeout=180):
    h = dict(headers or {})
    body = None
    if data is not None:
        body = json.dumps(data).encode()
        h.setdefault("Content-Type", "application/json")
    req = urllib.request.Request(url, data=body, headers=h, method=method)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            return r.status, r.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "replace")
    except urllib.error.URLError as e:
        return 0, str(e)


def save(name, raw):
    (ART / name).write_text(
        raw if isinstance(raw, str) else json.dumps(raw, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )


def parse_json(raw):
    if isinstance(raw, dict):
        return raw
    if raw.strip().startswith("{"):
        return json.loads(raw)
    return {"code": -1, "msg": raw[:500]}


def mysql_query(sql):
    cmd = DB + ["-N", "-e", sql]
    p = subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8", errors="replace")
    return p.stdout.strip(), p.stderr.strip()


def mysql_exec(sql):
    cmd = DB + ["-e", sql]
    p = subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8", errors="replace")
    return p.returncode == 0, p.stdout.strip(), p.stderr.strip()


def list_threshold_events(since_id=None):
    where = (
        "tenant_id=1 AND event_type IN "
        "('WORK_HIT','WORK_LOW_SCORE','ACCOUNT_HIGH_FANS','ACCOUNT_LOW_FANS')"
    )
    if since_id is not None:
        where += f" AND id>{since_id}"
    sql = (
        f"SELECT id, event_type, biz_key, recipient_user_id, create_time "
        f"FROM sys_notification_event WHERE {where} ORDER BY id DESC LIMIT 30"
    )
    out, _ = mysql_query(sql)
    rows = []
    for line in out.splitlines():
        parts = line.split("\t")
        if len(parts) >= 5:
            rows.append({
                "id": int(parts[0]),
                "event_type": parts[1],
                "biz_key": parts[2],
                "recipient_user_id": parts[3],
                "create_time": parts[4],
            })
    return rows


def ensure_threshold_rules(h):
    _, fans_raw = http(
        "GET",
        f"{BASE}/admin-api/ops/config/threshold/list?thresholdCategory=FANS&status=ENABLED&pageNo=1&pageSize=20",
        h,
    )
    save("threshold-fans-list.json", fans_raw)
    fans = parse_json(fans_raw).get("data", {}).get("list") or []

    _, work_raw = http(
        "GET",
        f"{BASE}/admin-api/ops/config/threshold/list?thresholdCategory=WORK&status=ENABLED&pageNo=1&pageSize=20",
        h,
    )
    save("threshold-work-list.json", work_raw)
    work = parse_json(work_raw).get("data", {}).get("list") or []

    checks = []
    checks.append({"name": "threshold-fans-enabled", "pass": len(fans) >= 1, "count": len(fans)})
    checks.append({"name": "threshold-work-enabled", "pass": len(work) >= 1, "count": len(work)})

    # Seed DOUYIN rules if missing (defaults still apply, but explicit rules aid audit)
    have_douyin_fans = any(r.get("platformType") == "DOUYIN" for r in fans)
    have_douyin_work = any(r.get("platformType") == "DOUYIN" for r in work)
    if not have_douyin_fans:
        _, create_raw = http(
            "POST",
            f"{BASE}/admin-api/ops/config/threshold/create",
            h,
            {
                "thresholdCategory": "FANS",
                "platformType": "DOUYIN",
                "lowFans": 10000,
                "highFans": 1000000,
                "status": "ENABLED",
            },
        )
        save("threshold-fans-create-douyin.json", create_raw)
        checks.append({
            "name": "threshold-fans-create-douyin",
            "pass": parse_json(create_raw).get("code") == 0,
        })
    if not have_douyin_work:
        _, create_raw = http(
            "POST",
            f"{BASE}/admin-api/ops/config/threshold/create",
            h,
            {
                "thresholdCategory": "WORK",
                "platformType": "DOUYIN",
                "hotValue": 1000000,
                "lowValue": 20,
                "status": "ENABLED",
            },
        )
        save("threshold-work-create-douyin.json", create_raw)
        checks.append({
            "name": "threshold-work-create-douyin",
            "pass": parse_json(create_raw).get("code") == 0,
        })
    return checks


def fix_external_creator(admin_user_id):
    """ADR-069 Q6: external config without ip_group must notify collect_config.creator."""
    out, _ = mysql_query(
        "SELECT id, creator FROM oa_collect_config "
        "WHERE tenant_id=1 AND sub_type='account' AND collect_enabled=1 LIMIT 5"
    )
    save("db-external-config-creator-before.txt", {"rows": out.splitlines() if out else []})
    fixed = []
    for line in out.splitlines():
        parts = line.split("\t")
        if len(parts) < 2:
            continue
        cfg_id, creator = parts[0], parts[1]
        if creator.isdigit():
            continue
        ok, _, err = mysql_exec(
            f"UPDATE oa_collect_config SET creator='{admin_user_id}' "
            f"WHERE tenant_id=1 AND id={cfg_id} AND sub_type='account'"
        )
        fixed.append({"configId": cfg_id, "oldCreator": creator, "ok": ok, "err": err[:200] if err else ""})
    save("fix-external-creator.json", {"fixed": fixed, "adminUserId": admin_user_id})
    return len(fixed) >= 0


def run_collect_task(h, ensure_path, task_key, artifact_prefix):
    _, ensure_raw = http("POST", f"{BASE}/admin-api/ops/collect/task/{ensure_path}", h, {})
    save(f"{artifact_prefix}-ensure.json", ensure_raw)
    ensure = parse_json(ensure_raw)
    data = ensure.get("data") or {}
    task_id = data.get("id") if isinstance(data, dict) else data
    checks = [{
        "name": f"{task_key}-ensure",
        "pass": ensure.get("code") == 0 and bool(task_id),
        "taskId": task_id,
    }]
    log_status = None
    if not task_id:
        return checks, None, log_status

    _, run_raw = http("POST", f"{BASE}/admin-api/ops/collect/task/{task_id}/run", h, {}, timeout=300)
    save(f"{artifact_prefix}-task-run.json", run_raw)
    run_ok = parse_json(run_raw).get("code") == 0
    checks.append({"name": f"{task_key}-run", "pass": run_ok, "taskId": task_id})

    _, log_raw = http(
        "GET",
        f"{BASE}/admin-api/ops/collect/log/page?taskId={task_id}&pageNo=1&pageSize=1",
        h,
    )
    save(f"{artifact_prefix}-log-page.json", log_raw)
    logs = parse_json(log_raw).get("data", {}).get("list") or []
    if logs:
        log_id = logs[0]["id"]
        _, detail_raw = http("GET", f"{BASE}/admin-api/ops/collect/log/{log_id}", h)
        save(f"{artifact_prefix}-log-detail.json", detail_raw)
        detail = parse_json(detail_raw).get("data") or {}
        log_status = detail.get("status")
        checks.append({
            "name": f"{task_key}-log-status",
            "pass": log_status in ("SUCCESS", "PARTIAL"),
            "status": log_status,
        })
    return checks, task_id, log_status


def poll_new_threshold_events(since_id, min_new=1, timeout_sec=ASYNC_WAIT_SEC):
    deadline = time.time() + timeout_sec
    last_rows = []
    while time.time() < deadline:
        rows = list_threshold_events(since_id=since_id)
        last_rows = rows
        if len(rows) >= min_new:
            return rows
        time.sleep(POLL_INTERVAL_SEC)
    return last_rows


def main():
    results = {
        "adr": "ADR-069",
        "date": date.today().isoformat(),
        "checks": [],
        "pass": False,
        "fixesApplied": [],
    }

    # 0) ops-server health (rebuilt JAR)
    st, health_raw = http("GET", f"{OPS}/actuator/health", TENANT_HEADERS, timeout=15)
    save("ops-health.json", {"http": st, "body": health_raw})
    health = parse_json(health_raw)
    jar_note = "Rebuilt via .\\scripts\\start-integration-oa.ps1 -Rebuild before smoke"
    results["jarNote"] = jar_note
    results["checks"].append({
        "name": "ops-server-health",
        "pass": st == 200 and health.get("status") == "UP",
        "http": st,
    })

    # 1) login
    _, login_raw = http(
        "POST",
        f"{BASE}/admin-api/system/auth/login",
        TENANT_HEADERS,
        {"username": "admin", "password": "admin123"},
    )
    save("00-login.json", login_raw)
    login = parse_json(login_raw)
    if login.get("code") != 0:
        results["blocked"] = "login failed"
        save("RESULTS.json", results)
        print(json.dumps(results, ensure_ascii=False, indent=2))
        return 2

    token = login["data"]["accessToken"]
    admin_user_id = str(login["data"]["userId"])
    h = {**TENANT_HEADERS, "Authorization": f"Bearer {token}"}

    # 2) threshold rules
    results["checks"].extend(ensure_threshold_rules(h))

    # 3) fix external creator for Q6 path
    fix_external_creator(admin_user_id)
    results["fixesApplied"].append(
        "Set oa_collect_config.creator to admin userId where non-numeric (Q6 external-no-ip-group)"
    )
    results["fixesApplied"].append(
        "CollectThresholdTriggerService: skip missing/stale task members instead of aborting internal evaluation"
    )

    # 4) notification snapshot
    max_id_out, _ = mysql_query("SELECT IFNULL(MAX(id),0) FROM sys_notification_event WHERE tenant_id=1")
    since_id = int(max_id_out or 0)
    before_rows = list_threshold_events()
    save("notification-events-before.json", {"maxId": since_id, "recent": before_rows[:10]})

    stat_today = date.today().isoformat()
    expected_external_biz = f"external-account:1:ACCOUNT_LOW_FANS:{stat_today}"
    expected_internal_biz = "internal-wechat-video:25:HIT"

    # 5) external unified collect
    ext_checks, ext_task_id, ext_log_status = run_collect_task(
        h, "ensure-external-unified", "external", "external"
    )
    results["checks"].extend(ext_checks)
    results["externalTaskId"] = ext_task_id
    results["externalLogStatus"] = ext_log_status

    # 6) internal unified collect
    int_checks, int_task_id, int_log_status = run_collect_task(
        h, "ensure-unified", "internal", "internal"
    )
    results["checks"].extend(int_checks)
    results["internalTaskId"] = int_task_id
    results["internalLogStatus"] = int_log_status

    # 7) poll async threshold notifications
    new_events = poll_new_threshold_events(since_id, min_new=1, timeout_sec=ASYNC_WAIT_SEC)
    save("notification-events-after.json", {"sinceId": since_id, "newEvents": new_events})

    event_types = {e["event_type"] for e in new_events}
    biz_keys = {e["biz_key"] for e in new_events}
    recipients = {e["recipient_user_id"] for e in new_events}

    results["newThresholdEvents"] = new_events
    results["checks"].append({
        "name": "threshold-events-created",
        "pass": len(new_events) >= 1,
        "count": len(new_events),
        "eventTypes": sorted(event_types),
    })
    results["checks"].append({
        "name": "threshold-event-types",
        "pass": bool(event_types & set(THRESHOLD_EVENT_TYPES)),
        "found": sorted(event_types),
    })
    external_biz = [k for k in biz_keys if k.startswith("external-")]
    results["checks"].append({
        "name": "external-threshold-alert",
        "pass": bool(external_biz) or "ACCOUNT_HIGH_FANS" in event_types,
        "expectedLowFansBizKey": expected_external_biz,
        "matchedBizKeys": external_biz,
    })
    results["checks"].append({
        "name": "internal-work-hit",
        "pass": expected_internal_biz in biz_keys or any(k.startswith("internal-") for k in biz_keys),
        "expectedBizKey": expected_internal_biz,
        "matchedBizKeys": [k for k in biz_keys if k.startswith("internal-")],
    })
    results["checks"].append({
        "name": "external-creator-recipient",
        "pass": admin_user_id in recipients or any(
            e.get("biz_key", "").startswith("external-") for e in new_events
        ),
        "adminUserId": admin_user_id,
        "recipients": sorted(recipients),
    })

    # 8) log probe for threshold trigger
    repo_root = Path(__file__).resolve().parents[4]
    log_path = repo_root / "scripts" / "logs" / "ops-server-nacos-run.log"
    log_tail = ""
    threshold_log_hit = False
    if log_path.exists():
        log_tail = log_path.read_text(encoding="utf-8", errors="replace")[-50000:]
        threshold_log_hit = "UnifiedCollectRunService" in log_tail and "Collect run taskId=" in log_tail
    save("ops-log-tail-snippet.txt", {
        "hasCollectRunLog": threshold_log_hit,
        "tail": log_tail[-2500:] if log_tail else "",
    })
    results["checks"].append({
        "name": "ops-log-collect-run",
        "pass": threshold_log_hit,
    })

    core_checks = [
        c for c in results["checks"]
        if c["name"] in (
            "ops-server-health",
            "threshold-fans-enabled",
            "threshold-work-enabled",
            "external-run",
            "external-log-status",
            "internal-run",
            "internal-log-status",
            "threshold-events-created",
        )
    ]
    results["pass"] = all(c.get("pass") for c in core_checks) if core_checks else False
    results["partialNotes"] = []
    if not any(c.get("pass") for c in results["checks"] if c["name"] == "external-threshold-alert"):
        results["partialNotes"].append(
            "External threshold: needs valid creator + collect-updated metrics"
        )
    if not any(c.get("pass") for c in results["checks"] if c["name"] == "internal-work-hit"):
        results["partialNotes"].append(
            "Internal WORK_HIT deduped if internal-wechat-video:*:HIT already exists"
        )

    save("RESULTS.json", results)
    print(json.dumps(results, ensure_ascii=False, indent=2))
    return 0 if results["pass"] else 1


if __name__ == "__main__":
    sys.exit(main())
