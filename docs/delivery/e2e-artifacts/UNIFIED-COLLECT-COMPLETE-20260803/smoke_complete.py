# -*- coding: utf-8 -*-
"""ADR-061 unified collect complete sign-off: persist + bind + quality stub."""
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

    # A: ensure-unified (data.id fix)
    _, ensure_raw = http("POST", f"{BASE}/admin-api/ops/collect/task/ensure-unified", h, {})
    save("ensure-unified.json", ensure_raw)
    ensure = parse_json(ensure_raw)
    data = ensure.get("data")
    task_id = data.get("id") if isinstance(data, dict) else data
    results["taskId"] = task_id
    results["checks"].append({"name": "ensure-unified", "pass": ensure.get("code") == 0 and task_id is not None})

    _, members_raw = http("GET", f"{BASE}/admin-api/ops/collect/task/{task_id}/members", h)
    save("members.json", members_raw)
    members = parse_json(members_raw).get("data") or []
    by_platform = {}
    for m in members:
        by_platform.setdefault(m.get("platformType"), []).append(m)

    persist_platforms = {"WECHAT_VIDEO", "DOUYIN", "KUAISHOU", "WECHAT_OFFICIAL", "XIAOHONGSHU", "BILIBILI"}
    results["memberPlatforms"] = list(by_platform.keys())

    # B: bind check on first member per persist platform
    bind_checks = []
    for plat in sorted(persist_platforms):
        accs = by_platform.get(plat, [])
        if not accs:
            bind_checks.append({"platform": plat, "skipped": True, "reason": "no member"})
            continue
        acc_id = accs[0]["accountId"]
        st, bind_raw = http("GET", f"{BASE}/admin-api/ops/account/{acc_id}/collector-bind", h)
        save(f"bind-{plat}-{acc_id}.json", bind_raw)
        bind = parse_json(bind_raw)
        bind_data = bind.get("data") or {}
        bound = bind_data.get("bindStatus") == "BOUND" and bind_data.get("collectorAccountId")
        bind_checks.append({"platform": plat, "accountId": acc_id, "bound": bool(bound), "http": st})
    results["bindChecks"] = bind_checks
    results["checks"].append({"name": "bind-enforcement-ready", "pass": all(c.get("bound") or c.get("skipped") for c in bind_checks)})

    # C: run unified task
    _, run_raw = http("POST", f"{BASE}/admin-api/ops/collect/task/{task_id}/run", h, {}, timeout=300)
    save("task-run.json", run_raw)
    run = parse_json(run_raw)
    results["runCode"] = run.get("code")

    _, log_page_raw = http("GET", f"{BASE}/admin-api/ops/collect/log/page?pageNo=1&pageSize=3&taskId={task_id}", h)
    save("log-page.json", log_page_raw)
    latest = (parse_json(log_page_raw).get("data") or {}).get("list") or []
    log_id = latest[0]["id"] if latest else None
    results["latestLogId"] = log_id

    persist_mode = None
    type_results = []
    target_tables = set()
    if log_id:
        _, detail_raw = http("GET", f"{BASE}/admin-api/ops/collect/log/{log_id}", h)
        save("log-detail.json", detail_raw)
        detail = parse_json(detail_raw)
        result = (detail.get("data") or {}).get("result") or {}
        persist_mode = result.get("persistMode")
        type_results = result.get("typeResults") or []
        save("result-json.json", result)
        for t in type_results:
            if t.get("targetTable"):
                target_tables.add(t["targetTable"])

    results["persistMode"] = persist_mode
    results["targetTables"] = sorted(target_tables)

    # Per-platform persist evidence
    platform_evidence = {}
    for plat in persist_platforms:
        prefix = None
        for m in by_platform.get(plat, []):
            prefix = f"account:{m['accountId']}/"
            break
        if not prefix:
            platform_evidence[plat] = {"skipped": True}
            continue
        plat_rows = [t for t in type_results if str(t.get("dataType", "")).startswith(prefix)]
        has_target = any(t.get("targetTable") for t in plat_rows if t.get("success"))
        platform_evidence[plat] = {
            "typeCount": len(plat_rows),
            "hasTargetTable": has_target,
            "successCount": sum(1 for t in plat_rows if t.get("success")),
        }
    results["platformEvidence"] = platform_evidence

    # D: quality API stub (ADR-060 — expect empty stub, not 410)
    _, quality_raw = http("GET", f"{BASE}/admin-api/ops/collect/quality/list?pageNo=1&pageSize=10", h)
    save("quality-list.json", quality_raw)
    quality = parse_json(quality_raw)
    quality_ok = quality.get("code") == 0
    results["qualityStub"] = {"code": quality.get("code"), "listEmpty": not ((quality.get("data") or {}).get("list"))}
    results["checks"].append({"name": "quality-stub-readable", "pass": quality_ok})

    # Pass criteria
    core_persist = {"oa_account_status_log", "oa_douyin_video"} & target_tables
    mp_persist = "oa_wechat_mp_article" in target_tables
    persist_ok = persist_mode in ("FULL_PERSIST", "MIXED", "PARTIAL") and persist_mode != "PROBE_COUNT_ONLY" and bool(core_persist)
    results["checks"].append({"name": "persist-mode", "pass": persist_ok})
    results["checks"].append({"name": "wechat-mp-targetTable", "pass": mp_persist or not by_platform.get("WECHAT_OFFICIAL")})

    results["pass"] = all(c["pass"] for c in results["checks"]) and run.get("code") == 0 and bool(log_id)
    save("RESULTS.json", results)
    print(json.dumps(results, ensure_ascii=False, indent=2))
    return 0 if results["pass"] else 1


if __name__ == "__main__":
    sys.exit(main())
