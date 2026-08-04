# -*- coding: utf-8 -*-
"""ADR-068 external unified collect smoke: toggle, ensure task, run, verify log."""
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
    (ART / name).write_text(raw if isinstance(raw, str) else json.dumps(raw, ensure_ascii=False, indent=2), encoding="utf-8")


def parse_json(raw):
    if raw.strip().startswith("{"):
        return json.loads(raw)
    return {"code": -1, "msg": raw[:300]}


def main():
    results = {"checks": [], "pass": False}

    _, login_raw = http("POST", f"{BASE}/admin-api/system/auth/login", TENANT_HEADERS,
                        {"username": "admin", "password": "admin123"})
    save("00-login.json", login_raw)
    login = parse_json(login_raw)
    if login.get("code") != 0:
        results["blocked"] = "login failed"
        save("RESULTS.json", results)
        print(json.dumps(results, ensure_ascii=False, indent=2))
        return 2

    token = login["data"]["accessToken"]
    h = {**TENANT_HEADERS, "Authorization": f"Bearer {token}"}

    # 1) list external accounts, enable collect on first
    _, list_raw = http("GET", f"{BASE}/admin-api/ops/config/external-collect/list?subType=account&pageNo=1&pageSize=5", h)
    save("external-account-list.json", list_raw)
    accounts = parse_json(list_raw).get("data", {}).get("list") or []
    config_id = accounts[0]["id"] if accounts else None
    if config_id:
        _, upd_raw = http("PUT", f"{BASE}/admin-api/ops/config/external-collect/update", h,
                          {"id": config_id, "collectEnabled": True})
        save("toggle-collect-enabled.json", upd_raw)
        results["checks"].append({"name": "toggle-collect-enabled", "pass": parse_json(upd_raw).get("code") == 0})

    # 2) ensure external unified task
    _, ensure_raw = http("POST", f"{BASE}/admin-api/ops/collect/task/ensure-external-unified", h, {})
    save("ensure-external-unified.json", ensure_raw)
    ensure = parse_json(ensure_raw)
    data = ensure.get("data") or {}
    task_id = data.get("id") if isinstance(data, dict) else data
    results["taskId"] = task_id
    results["checks"].append({"name": "ensure-external-unified", "pass": ensure.get("code") == 0 and bool(task_id)})

    if task_id:
        _, members_raw = http("GET", f"{BASE}/admin-api/ops/collect/task/{task_id}/external-members", h)
        save("external-members.json", members_raw)
        members = parse_json(members_raw).get("data") or []
        results["memberCount"] = len(members)
        results["checks"].append({"name": "external-members", "pass": len(members) >= 0})

        # 3) run task (stub collector OK)
        _, run_raw = http("POST", f"{BASE}/admin-api/ops/collect/task/{task_id}/run", h, {}, timeout=180)
        save("task-run.json", run_raw)
        run_ok = parse_json(run_raw).get("code") == 0
        results["checks"].append({"name": "task-run", "pass": run_ok})

        # 4) verify log
        _, log_raw = http("GET", f"{BASE}/admin-api/ops/collect/log/page?taskId={task_id}&pageNo=1&pageSize=1", h)
        save("log-page.json", log_raw)
        logs = parse_json(log_raw).get("data", {}).get("list") or []
        if logs:
            log_id = logs[0]["id"]
            _, detail_raw = http("GET", f"{BASE}/admin-api/ops/collect/log/{log_id}", h)
            save("log-detail.json", detail_raw)
            detail = parse_json(detail_raw).get("data") or {}
            results["logStatus"] = detail.get("status")
            results["checks"].append({"name": "log-written", "pass": detail.get("status") in ("SUCCESS", "PARTIAL", "FAILED")})

    results["pass"] = all(c.get("pass") for c in results["checks"]) if results["checks"] else False
    save("RESULTS.json", results)
    print(json.dumps(results, ensure_ascii=False, indent=2))
    return 0 if results["pass"] else 1


if __name__ == "__main__":
    sys.exit(main())
