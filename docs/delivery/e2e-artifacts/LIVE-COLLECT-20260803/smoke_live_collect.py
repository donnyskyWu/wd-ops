# -*- coding: utf-8 -*-
"""ADR-067 live collect smoke: toggle + unified run + internal-content LIVE filter."""
import json
import sys
import urllib.error
import urllib.parse
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

    # Find DOUYIN member with collect enabled
    _, ensure_raw = http("POST", f"{BASE}/admin-api/ops/collect/task/ensure-unified", h, {})
    save("ensure-unified.json", ensure_raw)
    ensure = parse_json(ensure_raw)
    task_id = (ensure.get("data") or {}).get("id") if isinstance(ensure.get("data"), dict) else ensure.get("data")

    _, members_raw = http("GET", f"{BASE}/admin-api/ops/collect/task/{task_id}/members", h)
    save("members.json", members_raw)
    members = parse_json(members_raw).get("data") or []
    douyin = next((m for m in members if m.get("platformType") == "DOUYIN"), None)
    results["douyinMember"] = douyin

    if not douyin:
        results["checks"].append({"name": "douyin-member", "pass": False, "reason": "no DOUYIN member"})
        save("RESULTS.json", results)
        return 1

    acc_id = douyin["accountId"]

    # Enable live collect toggle
    _, toggle_raw = http("PUT", f"{BASE}/admin-api/ops/platform-account/update",
                         h, {"id": acc_id, "collectLiveEnabled": True})
    save("toggle-live-enabled.json", toggle_raw)
    toggle = parse_json(toggle_raw)
    results["checks"].append({"name": "toggle-collectLiveEnabled", "pass": toggle.get("code") == 0})

    # Run unified collect
    _, run_raw = http("POST", f"{BASE}/admin-api/ops/collect/task/{task_id}/run", h, {})
    save("task-run.json", run_raw)
    run = parse_json(run_raw)

    # Latest log
    _, log_page_raw = http("GET", f"{BASE}/admin-api/ops/collect/log/page?taskId={task_id}&pageNo=1&pageSize=1", h)
    save("log-page.json", log_page_raw)
    log_page = parse_json(log_page_raw)
    log_id = None
    if log_page.get("code") == 0:
        lst = (log_page.get("data") or {}).get("list") or []
        if lst:
            log_id = lst[0].get("id")

    live_target = False
    if log_id:
        _, detail_raw = http("GET", f"{BASE}/admin-api/ops/collect/log/{log_id}", h)
        save("log-detail.json", detail_raw)
        detail = parse_json(detail_raw)
        result = (detail.get("data") or {}).get("result") or {}
        if isinstance(result, str):
            result = json.loads(result)
        for row in result.get("typeResults") or []:
            tt = row.get("targetTable") or ""
            dt = str(row.get("dataType") or "")
            if tt in ("oa_douyin_live", "oa_wechat_video_live") or "LIVE" in dt.upper():
                live_target = True
                break
    results["checks"].append({"name": "log-live-targetTable", "pass": live_target})

    # Internal content LIVE filter
    q = urllib.parse.urlencode({"platformType": "DOUYIN", "contentType": "LIVE", "pageNo": 1, "pageSize": 20})
    _, ic_raw = http("GET", f"{BASE}/admin-api/ops/internal-content/list?{q}", h)
    save("internal-content-live.json", ic_raw)
    ic = parse_json(ic_raw)
    live_rows = ((ic.get("data") or {}).get("list") or []) if ic.get("code") == 0 else []
    results["checks"].append({"name": "internal-content-LIVE", "pass": len(live_rows) > 0, "count": len(live_rows)})

    results["pass"] = all(c.get("pass") for c in results["checks"])
    save("RESULTS.json", results)
    print(json.dumps(results, ensure_ascii=False, indent=2))
    return 0 if results["pass"] else 1


if __name__ == "__main__":
    sys.exit(main())
