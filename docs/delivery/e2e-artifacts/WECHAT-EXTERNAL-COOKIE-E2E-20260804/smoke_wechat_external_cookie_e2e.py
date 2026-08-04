# -*- coding: utf-8 -*-
"""E2E: sys_param collect.external.wechat_official.cookie + config 41 external collect."""
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
CONFIG_ID = 41
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


def load_internal_wechat_cookie() -> tuple[str, str]:
    """Resolve internal mp.weixin cookie without writing secrets to artifacts."""
    tmp = ROOT / "scripts" / "_tmp_wechat_cookie.txt"
    if tmp.is_file():
        val = tmp.read_text(encoding="utf-8").strip()
        if val:
            return val, f"scripts/_tmp_wechat_cookie.txt (from oa_account decrypt)"

    env_file = ROOT / "unify-collector-api" / ".env"
    if env_file.is_file():
        for line in env_file.read_text(encoding="utf-8").splitlines():
            line = line.strip()
            if line.startswith("WECHAT_MP_COOKIE="):
                val = line.split("=", 1)[1].strip().strip('"').strip("'")
                if val:
                    return val, "unify-collector-api/.env WECHAT_MP_COOKIE (Channel-A internal session)"

    raise RuntimeError("No internal WeChat cookie source found (.env or _tmp_wechat_cookie.txt)")


def find_param(token: str) -> dict | None:
    _, raw = http(
        "GET",
        f"{GW}/admin-api/ops/system/param/list?pageNo=1&pageSize=50&paramKey={PARAM_KEY}",
        {**TENANT_HEADERS, "Authorization": f"Bearer {token}"},
    )
    save("param-list-before.json", parse_json(raw))
    for row in (parse_json(raw).get("data") or {}).get("list") or []:
        if row.get("paramKey") == PARAM_KEY:
            return row
    return None


def ensure_param(token: str, cookie: str) -> dict:
    row = find_param(token)
    if row is None:
        payload = {
            "paramName": "外部公众号采集 Cookie",
            "paramKey": PARAM_KEY,
            "paramValue": cookie,
            "paramType": "STRING",
            "category": "COLLECT",
            "remark": "E2E 20260804 — internal cookie bridged to external collect",
        }
        _, raw = http(
            "POST",
            f"{GW}/admin-api/ops/system/param/create",
            {**TENANT_HEADERS, "Authorization": f"Bearer {token}"},
            payload,
        )
        save("param-create.json", parse_json(raw))
        created = parse_json(raw)
        if created.get("code") != 0:
            raise RuntimeError(f"param create failed: {created}")
        row = find_param(token)
        if not row:
            raise RuntimeError("param not found after create")
        return row

    payload = {
        "id": row["id"],
        "paramName": row["paramName"],
        "paramKey": row["paramKey"],
        "paramValue": cookie,
        "paramType": row["paramType"],
        "category": row["category"],
        "remark": row.get("remark"),
    }
    _, raw = http(
        "PUT",
        f"{GW}/admin-api/ops/system/param/update",
        {**TENANT_HEADERS, "Authorization": f"Bearer {token}"},
        payload,
    )
    save("param-update.json", parse_json(raw))
    upd = parse_json(raw)
    if upd.get("code") != 0:
        raise RuntimeError(f"param update failed: {upd}")
    return row


def main():
    results = {
        "checks": [],
        "pass": False,
        "configId": CONFIG_ID,
        "paramKey": PARAM_KEY,
    }

    try:
        cookie, cookie_source = load_internal_wechat_cookie()
        results["cookieSource"] = cookie_source
        results["cookieMasked"] = mask_cookie(cookie)
        results["cookieLen"] = len(cookie)
    except Exception as ex:
        results["blocked"] = str(ex)
        save("RESULTS.json", results)
        print(json.dumps(results, ensure_ascii=False, indent=2))
        return 2

    _, login_raw = http(
        "POST",
        f"{GW}/admin-api/system/auth/login",
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
    h = {**TENANT_HEADERS, "Authorization": f"Bearer {token}"}

    # apply sys_param
    try:
        param_row = ensure_param(token, cookie)
        results["paramId"] = param_row.get("id")
        results["checks"].append({"name": "sys-param-set", "pass": True})
    except Exception as ex:
        results["checks"].append({"name": "sys-param-set", "pass": False, "error": str(ex)})
        save("RESULTS.json", results)
        print(json.dumps(results, ensure_ascii=False, indent=2))
        return 1

    # verify param list masks value
    _, verify_raw = http(
        "GET",
        f"{GW}/admin-api/ops/system/param/list?pageNo=1&pageSize=50&paramKey={PARAM_KEY}",
        h,
    )
    save("param-list-after.json", parse_json(verify_raw))
    verify_list = (parse_json(verify_raw).get("data") or {}).get("list") or []
    param_after = next((p for p in verify_list if p.get("paramKey") == PARAM_KEY), None)
    results["checks"].append({
        "name": "param-visible-in-list",
        "pass": param_after is not None,
    })

    # config 41 snapshot + enable collect
    _, cfg_raw = http(
        "GET",
        f"{GW}/admin-api/ops/config/external-collect/list?subType=account&pageNo=1&pageSize=50",
        h,
    )
    save("external-account-list.json", parse_json(cfg_raw))
    accounts = (parse_json(cfg_raw).get("data") or {}).get("list") or []
    cfg41 = next((a for a in accounts if a.get("id") == CONFIG_ID), None)
    results["config41"] = {
        k: cfg41.get(k)
        for k in ("id", "platformType", "configName", "accountIdentifier", "collectEnabled", "status")
    } if cfg41 else None
    results["checks"].append({
        "name": "config41-wechat-official",
        "pass": cfg41 is not None and cfg41.get("platformType") == "WECHAT_OFFICIAL",
    })

    if cfg41:
        _, toggle_raw = http(
            "PUT",
            f"{GW}/admin-api/ops/config/external-collect/update",
            h,
            {"id": CONFIG_ID, "collectEnabled": True},
        )
        save("toggle-collect-enabled.json", parse_json(toggle_raw))
        toggle = parse_json(toggle_raw)
        results["toggleCode"] = toggle.get("code")
        results["toggleMsg"] = toggle.get("msg")
        results["checks"].append({
            "name": "toggle-collect-enabled",
            "pass": toggle.get("code") == 0,
        })

    # ensure + run unified external task
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
        members = (parse_json(members_raw).get("data") or [])
        results["memberCount"] = len(members)
        cfg41_member = next((m for m in members if m.get("collectConfigId") == CONFIG_ID), None)
        results["config41InTask"] = cfg41_member is not None

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
            result_json = detail.get("result") or {}
            type_results = result_json.get("typeResults") or []
            cfg41_result = next(
                (t for t in type_results if f"config:{CONFIG_ID}" in str(t.get("memberKey") or "")),
                None,
            )
            results["config41TypeResult"] = sanitize_type_result(cfg41_result)
            err_text = json.dumps(detail, ensure_ascii=False)
            cred_missing = "凭账号" in err_text or "credential" in err_text.lower()
            skipped = any(t.get("skipped") for t in type_results)
            wechat_ok = (
                detail.get("status") in ("SUCCESS", "PARTIAL")
                and not cred_missing
                and (cfg41_result is None or not cfg41_result.get("skipped"))
            )
            results["checks"].append({
                "name": "wechat-not-credential-missing",
                "pass": wechat_ok or (detail.get("status") == "SUCCESS" and not skipped),
            })
            results["checks"].append({
                "name": "log-written",
                "pass": detail.get("status") in ("SUCCESS", "PARTIAL", "FAILED"),
            })

    # external work count for config 41 account (monitor API if available)
    if cfg41 and cfg41.get("accountIdentifier"):
        _, work_raw = http(
            "GET",
            f"{GW}/admin-api/ops/monitor/external-work/page?pageNo=1&pageSize=5&accountIdentifier={cfg41['accountIdentifier']}",
            h,
        )
        work_body = parse_json(work_raw)
        save("external-work-sample.json", work_body)
        work_total = (work_body.get("data") or {}).get("total")
        results["externalWorkTotalSample"] = work_total

    results["pass"] = all(c.get("pass") for c in results["checks"]) if results["checks"] else False
    save("RESULTS.json", results)
    print(json.dumps(results, ensure_ascii=False, indent=2))
    return 0 if results["pass"] else 1


def sanitize_type_result(tr: dict | None) -> dict | None:
    if not tr:
        return None
    return {k: tr.get(k) for k in ("memberKey", "platform", "skipped", "recordCount", "errorMessage", "status")}


def redact_log_detail(detail: dict) -> dict:
    """Remove any cookie-like substrings from saved log detail."""
    text = json.dumps(detail, ensure_ascii=False)
    text = re.sub(r"(sessionid|slave_sid|data_ticket|wxuin)=[^;\"\\s]+", r"\1=***", text, flags=re.I)
    return json.loads(text)


if __name__ == "__main__":
    sys.exit(main())
