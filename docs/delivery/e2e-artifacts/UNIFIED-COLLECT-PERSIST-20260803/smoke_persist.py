# -*- coding: utf-8 -*-
"""ADR-061 follow-up: unified collect FULL_PERSIST smoke (WECHAT_VIDEO/DOUYIN/KUAISHOU)."""
import json
import sys
import urllib.error
import urllib.request
from pathlib import Path

ART = Path(__file__).resolve().parent
BASE = "http://127.0.0.1:48080"
TENANT_HEADERS = {"tenant-id": "1", "X-Tenant-Id": "1", "Content-Type": "application/json"}


def http(method, url, headers=None, data=None, timeout=120):
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
    (ART / name).write_text(raw, encoding="utf-8")


def main():
    results = {"compileOnly": False, "checks": []}

    _, login_raw = http(
        "POST",
        f"{BASE}/admin-api/system/auth/login",
        TENANT_HEADERS,
        {"username": "admin", "password": "admin123"},
    )
    save("00-login.json", login_raw)
    if login_raw.strip().startswith("{"):
        login = json.loads(login_raw)
    else:
        login = {"code": -1, "msg": login_raw[:200]}
    if login.get("code") != 0:
        results["blocked"] = "login failed — ops-server/gateway down?"
        save("RESULTS.json", json.dumps(results, ensure_ascii=False, indent=2))
        print(json.dumps(results, ensure_ascii=False, indent=2))
        return 2

    token = login["data"]["accessToken"]
    h = {**TENANT_HEADERS, "Authorization": f"Bearer {token}"}

    _, ensure_raw = http("POST", f"{BASE}/admin-api/ops/collect/task/ensure-unified", h, {})
    save("ensure-unified.json", ensure_raw)
    ensure = json.loads(ensure_raw)
    data = ensure.get("data")
    task_id = data.get("id") if isinstance(data, dict) else data
    results["taskId"] = task_id

    _, members_raw = http("GET", f"{BASE}/admin-api/ops/collect/task/{task_id}/members", h)
    save("members.json", members_raw)
    members = json.loads(members_raw).get("data") or []

    persist_members = [
        m for m in members if m.get("platformType") in ("DOUYIN", "KUAISHOU", "WECHAT_VIDEO")
    ]
    results["persistMemberCount"] = len(persist_members)

    _, run_raw = http("POST", f"{BASE}/admin-api/ops/collect/task/{task_id}/run", h, {}, timeout=300)
    save("task-run.json", run_raw)
    run = json.loads(run_raw) if run_raw.strip().startswith("{") else {"code": -1, "msg": run_raw}

    _, log_page_raw = http("GET", f"{BASE}/admin-api/ops/collect/log/page?pageNo=1&pageSize=5&taskId={task_id}", h)
    save("log-page.json", log_page_raw)
    log_page = json.loads(log_page_raw)
    latest = (log_page.get("data") or {}).get("list") or []
    log_id = latest[0]["id"] if latest else None
    results["latestLogId"] = log_id

    persist_mode = None
    type_results = []
    if log_id:
        _, detail_raw = http("GET", f"{BASE}/admin-api/ops/collect/log/{log_id}", h)
        save("log-detail.json", detail_raw)
        detail = json.loads(detail_raw)
        result = (detail.get("data") or {}).get("result") or {}
        persist_mode = result.get("persistMode")
        type_results = result.get("typeResults") or []
        save(
            "result-json.json",
            json.dumps(result, ensure_ascii=False, indent=2),
        )

    results["persistMode"] = persist_mode
    results["typeResultsWithTargetTable"] = sum(1 for t in type_results if t.get("targetTable"))
    results["runCode"] = run.get("code")

    ok_modes = persist_mode in ("FULL_PERSIST", "MIXED", "PARTIAL")  # MIXED when MP members present
    has_target = results["typeResultsWithTargetTable"] > 0
    results["pass"] = bool(log_id) and has_target and persist_mode != "PROBE_COUNT_ONLY"

    save("RESULTS.json", json.dumps(results, ensure_ascii=False, indent=2))
    print(json.dumps(results, ensure_ascii=False, indent=2))
    return 0 if results["pass"] else 1


if __name__ == "__main__":
    sys.exit(main())
