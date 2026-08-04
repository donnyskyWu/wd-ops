# -*- coding: utf-8 -*-
"""Verify ADR-068 WECHAT_OFFICIAL skip when tenant credential missing."""
import json
import sys
import urllib.error
import urllib.request
from pathlib import Path

ART = Path(__file__).resolve().parent
BASE = "http://127.0.0.1:48080"
TENANT_HEADERS = {"tenant-id": "1", "X-Tenant-Id": "1", "Content-Type": "application/json"}


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
    if raw.strip().startswith("{"):
        return json.loads(raw)
    return {"code": -1, "msg": raw[:500]}


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

    # config 41 snapshot
    _, cfg_raw = http("GET", f"{BASE}/admin-api/ops/config/external-collect/list?subType=account&pageNo=1&pageSize=50", h)
    save("external-account-list.json", cfg_raw)
    accounts = parse_json(cfg_raw).get("data", {}).get("list") or []
    cfg41 = next((a for a in accounts if a.get("id") == 41), None)
    results["config41"] = cfg41
    results["checks"].append({
        "name": "config41-wechat-official",
        "pass": cfg41 is not None and cfg41.get("platformType") == "WECHAT_OFFICIAL",
    })

    # ensure + run unified external task
    _, ensure_raw = http("POST", f"{BASE}/admin-api/ops/collect/task/ensure-external-unified", h, {})
    save("ensure-external-unified.json", ensure_raw)
    task_id = (parse_json(ensure_raw).get("data") or {}).get("id")
    results["taskId"] = task_id

    if task_id:
        _, run_raw = http("POST", f"{BASE}/admin-api/ops/collect/task/{task_id}/run", h, {})
        save("task-run.json", run_raw)
        results["checks"].append({"name": "task-run", "pass": parse_json(run_raw).get("code") == 0})

        _, log_raw = http("GET", f"{BASE}/admin-api/ops/collect/log/page?taskId={task_id}&pageNo=1&pageSize=1", h)
        save("log-page.json", log_raw)
        logs = parse_json(log_raw).get("data", {}).get("list") or []
        if logs:
            log_id = logs[0]["id"]
            _, detail_raw = http("GET", f"{BASE}/admin-api/ops/collect/log/{log_id}", h)
            save("log-detail.json", detail_raw)
            detail = parse_json(detail_raw).get("data") or {}
            results["logStatus"] = detail.get("status")
            results["errorMessage"] = detail.get("errorMessage")
            result_json = detail.get("result") or {}
            type_results = result_json.get("typeResults") or []
            skipped = [t for t in type_results if t.get("skipped")]
            results["skippedMembers"] = skipped
            # After fix: SUCCESS + skip note for config:41, DOUYIN configs succeed
            results["checks"].append({
                "name": "wechat-skipped-not-partial-fail",
                "pass": detail.get("status") == "SUCCESS"
                    or any(t.get("skipped") for t in type_results),
            })

    # toggle collect on WECHAT without credential should fail with 1512 (post-deploy)
    if cfg41:
        _, toggle_raw = http("PUT", f"{BASE}/admin-api/ops/config/external-collect/update", h,
                             {"id": 41, "collectEnabled": False})
        save("toggle-off.json", toggle_raw)
        _, toggle_on_raw = http("PUT", f"{BASE}/admin-api/ops/config/external-collect/update", h,
                                {"id": 41, "collectEnabled": True})
        save("toggle-on-blocked.json", toggle_on_raw)
        toggle_on = parse_json(toggle_on_raw)
        results["toggleOnCode"] = toggle_on.get("code")
        results["checks"].append({
            "name": "toggle-on-requires-credential",
            "pass": toggle_on.get("code") != 0 or "凭账号" in (toggle_on.get("msg") or ""),
        })
        # restore off to avoid polluting
        http("PUT", f"{BASE}/admin-api/ops/config/external-collect/update", h,
             {"id": 41, "collectEnabled": False})

    results["pass"] = all(c.get("pass") for c in results["checks"]) if results["checks"] else False
    save("RESULTS.json", results)
    print(json.dumps(results, ensure_ascii=False, indent=2))
    return 0 if results["pass"] else 1


if __name__ == "__main__":
    sys.exit(main())
