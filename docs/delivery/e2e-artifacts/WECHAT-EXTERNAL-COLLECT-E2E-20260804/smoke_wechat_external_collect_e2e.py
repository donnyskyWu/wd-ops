# -*- coding: utf-8 -*-
"""E2E: sys_param collect.external.wechat_official.cookie + external WECHAT collect."""
from __future__ import annotations

import json
import os
import re
import sys
import urllib.error
import urllib.request
from pathlib import Path

ART = Path(__file__).resolve().parent
ROOT = ART.parents[3]
GW = os.environ.get("E2E_GATEWAY", "http://127.0.0.1:48080")
PARAM_KEY = "collect.external.wechat_official.cookie"
# Beta drift: id 41 was QcloudCommunity; current WECHAT_OFFICIAL external config is 45 (Agent_AI_Z).
WECHAT_CONFIG_ID_FALLBACK = 45
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
    if isinstance(raw, (dict, list)):
        text = json.dumps(raw, ensure_ascii=False, indent=2)
    else:
        text = raw
    (ART / name).write_text(text, encoding="utf-8")


def parse_json(raw):
    if raw.strip().startswith("{"):
        return json.loads(raw)
    return {"code": -1, "msg": raw[:500]}


def mask_cookie(cookie: str) -> str:
    if not cookie:
        return "(empty)"
    if len(cookie) <= 16:
        return "***"
    return cookie[:8] + "..." + cookie[-6:]


def load_cookie_meta() -> tuple[str, str]:
    meta = ROOT / "scripts" / "_tmp_wechat_cookie_meta.txt"
    tmp = ROOT / "scripts" / "_tmp_wechat_cookie.txt"
    if meta.is_file():
        source = "unknown"
        for line in meta.read_text(encoding="utf-8").splitlines():
            if line.startswith("source="):
                source = line.split("=", 1)[1]
        if tmp.is_file():
            val = tmp.read_text(encoding="utf-8").strip()
            if val:
                return val, source
    if tmp.is_file():
        val = tmp.read_text(encoding="utf-8").strip()
        if val:
            return val, "scripts/_tmp_wechat_cookie.txt"
    raise RuntimeError("No cookie meta; run scripts/_tmp_copy_wechat_cookie_to_param.py first")


def find_wechat_config(accounts: list) -> tuple[dict | None, dict | None]:
    cfg41 = next((a for a in accounts if a.get("id") == 41), None)
    cfg_wechat = next(
        (a for a in accounts if a.get("platformType") == "WECHAT_OFFICIAL"),
        None,
    )
    return cfg41, cfg_wechat


def sanitize_type_result(tr: dict | None) -> dict | None:
    if not tr:
        return None
    return {
        k: tr.get(k)
        for k in ("dataType", "memberKey", "platform", "skipped", "success", "recordCount", "errorMessage", "status")
    }


def redact_log_detail(detail: dict) -> dict:
    text = json.dumps(detail, ensure_ascii=False)
    text = re.sub(r"(sessionid|slave_sid|data_ticket|wxuin)=[^;\"\\s]+", r"\1=***", text, flags=re.I)
    return json.loads(text)


def main():
    results = {
        "checks": [],
        "pass": False,
        "paramKey": PARAM_KEY,
        "note": "Beta: config 41 is no longer WECHAT_OFFICIAL; active WECHAT external config resolved dynamically.",
    }

    try:
        cookie, cookie_source = load_cookie_meta()
        results["cookieSource"] = cookie_source
        results["cookieMasked"] = mask_cookie(cookie)
        results["cookieLen"] = len(cookie)
        results["checks"].append({"name": "cookie-source-resolved", "pass": True})
    except Exception as ex:
        results["blocked"] = str(ex)
        save("RESULTS.json", results)
        print(json.dumps(results, ensure_ascii=False, indent=2))
        return 2

    _, login_raw = http(
        "POST",
        f"{GW}/admin-api/system/auth/login",
        TENANT_HEADERS,
        {"username": "admin", "password": "admin123", "tenantId": 1},
    )
    save("00-login.json", redact_login(login_raw))
    login = parse_json(login_raw)
    if login.get("code") != 0:
        results["blocked"] = "login failed"
        save("RESULTS.json", results)
        print(json.dumps(results, ensure_ascii=False, indent=2))
        return 2

    token = login["data"]["accessToken"]
    h = {**TENANT_HEADERS, "Authorization": f"Bearer {token}"}

    _, param_raw = http(
        "GET",
        f"{GW}/admin-api/ops/system/param/list?pageNo=1&pageSize=50&paramKey={PARAM_KEY}",
        h,
    )
    save("param-list.json", parse_json(param_raw))
    param_list = (parse_json(param_raw).get("data") or {}).get("list") or []
    param_row = next((p for p in param_list if p.get("paramKey") == PARAM_KEY), None)
    results["paramId"] = param_row.get("id") if param_row else None
    results["checks"].append({"name": "sys-param-present", "pass": param_row is not None})

    _, cfg_raw = http(
        "GET",
        f"{GW}/admin-api/ops/config/external-collect/list?subType=account&pageNo=1&pageSize=50",
        h,
    )
    save("external-account-list.json", parse_json(cfg_raw))
    accounts = (parse_json(cfg_raw).get("data") or {}).get("list") or []
    cfg41, cfg_wechat = find_wechat_config(accounts)
    target = cfg_wechat or cfg41
    target_id = (target or {}).get("id") or WECHAT_CONFIG_ID_FALLBACK
    results["config41"] = {
        k: cfg41.get(k)
        for k in ("id", "platformType", "configName", "accountIdentifier", "collectEnabled", "status")
    } if cfg41 else None
    results["wechatConfig"] = {
        k: (target or {}).get(k)
        for k in ("id", "platformType", "configName", "accountIdentifier", "collectEnabled", "status")
    }
    results["targetConfigId"] = target_id
    results["checks"].append({
        "name": "wechat-external-config-found",
        "pass": target is not None and target.get("platformType") == "WECHAT_OFFICIAL",
    })

    if target and not target.get("collectEnabled"):
        _, toggle_raw = http(
            "PUT",
            f"{GW}/admin-api/ops/config/external-collect/update",
            h,
            {"id": target_id, "collectEnabled": True},
        )
        save("toggle-collect-enabled.json", parse_json(toggle_raw))
        toggle = parse_json(toggle_raw)
        results["toggleCode"] = toggle.get("code")
        results["checks"].append({"name": "toggle-collect-enabled", "pass": toggle.get("code") == 0})

    _, ensure_raw = http("POST", f"{GW}/admin-api/ops/collect/task/ensure-external-unified", h, {})
    save("ensure-external-unified.json", parse_json(ensure_raw))
    ensure = parse_json(ensure_raw)
    task_id = (ensure.get("data") or {}).get("id") if isinstance(ensure.get("data"), dict) else ensure.get("data")
    results["taskId"] = task_id
    results["checks"].append({
        "name": "ensure-external-unified",
        "pass": ensure.get("code") == 0 and bool(task_id),
    })

    if task_id:
        _, members_raw = http("GET", f"{GW}/admin-api/ops/collect/task/{task_id}/external-members", h)
        save("external-members.json", parse_json(members_raw))
        members = parse_json(members_raw).get("data") or []
        wechat_member = next(
            (m for m in members if m.get("platformType") == "WECHAT_OFFICIAL"),
            None,
        )
        results["wechatMember"] = wechat_member
        results["checks"].append({
            "name": "wechat-in-task",
            "pass": wechat_member is not None and wechat_member.get("collectEnabled"),
        })

        _, run_raw = http(
            "POST",
            f"{GW}/admin-api/ops/collect/task/{task_id}/run",
            h,
            {},
            timeout=300,
        )
        save("task-run.json", parse_json(run_raw))
        run_ok = parse_json(run_raw).get("code") == 0
        results["checks"].append({"name": "task-run", "pass": run_ok})

        _, log_raw = http(
            "GET",
            f"{GW}/admin-api/ops/collect/log/page?taskId={task_id}&pageNo=1&pageSize=1",
            h,
        )
        save("log-page.json", parse_json(log_raw))
        logs = (parse_json(log_raw).get("data") or {}).get("list") or []
        if logs:
            log_id = logs[0]["id"]
            _, detail_raw = http("GET", f"{GW}/admin-api/ops/collect/log/{log_id}", h)
            detail = parse_json(detail_raw).get("data") or {}
            save("log-detail.json", redact_log_detail(detail))
            results["logStatus"] = detail.get("status")
            results["errorMessage"] = detail.get("errorMessage")
            type_results = (detail.get("result") or {}).get("typeResults") or []
            wechat_result = next(
                (t for t in type_results if str(t.get("dataType") or "").startswith("config:")),
                None,
            )
            if not wechat_result:
                wechat_result = next(
                    (t for t in type_results if t.get("platform") == "WECHAT_OFFICIAL"),
                    None,
                )
            results["wechatTypeResult"] = sanitize_type_result(wechat_result)
            err_text = json.dumps(detail, ensure_ascii=False)
            cred_missing = (
                "凭账号" in err_text
                or "credential" in err_text.lower()
                or "oa_tenant_collector_credential" in err_text
            )
            skipped = bool(wechat_result and wechat_result.get("skipped"))
            cookie_expired = "失效" in err_text or "relogin" in err_text.lower()
            wechat_ok = (
                wechat_result is not None
                and not skipped
                and not cred_missing
                and wechat_result.get("success") is True
                and (wechat_result.get("recordCount") or 0) > 0
            )
            results["checks"].append({
                "name": "wechat-not-credential-missing",
                "pass": wechat_result is not None and not skipped and not cred_missing,
            })
            results["checks"].append({
                "name": "wechat-collect-success",
                "pass": wechat_ok,
            })
            if cookie_expired and not cred_missing:
                results["cookieExpired"] = True
            results["checks"].append({
                "name": "log-written",
                "pass": detail.get("status") in ("SUCCESS", "PARTIAL", "FAILED"),
            })

    acct_id = (target or {}).get("accountIdentifier")
    if acct_id:
        _, work_raw = http(
            "GET",
            f"{GW}/admin-api/ops/monitor/external-work/page?pageNo=1&pageSize=5&accountIdentifier={acct_id}",
            h,
        )
        work_body = parse_json(work_raw)
        save("external-work-sample.json", work_body)
        results["externalWorkTotalSample"] = (work_body.get("data") or {}).get("total")

    results["pass"] = all(c.get("pass") for c in results["checks"]) if results["checks"] else False
    save("RESULTS.json", results)
    print(json.dumps(results, ensure_ascii=False, indent=2))
    return 0 if results["pass"] else 1


def redact_login(raw: str) -> dict:
    body = parse_json(raw)
    if isinstance(body.get("data"), dict) and body["data"].get("accessToken"):
        body["data"] = {k: v for k, v in body["data"].items() if k != "accessToken"}
        body["data"]["accessToken"] = "(redacted)"
    return body


if __name__ == "__main__":
    sys.exit(main())
