#!/usr/bin/env python3
"""FOLLOWER-ANALYSIS-20260806: API smoke for 粉丝分析 collected-data fallback fix."""
from __future__ import annotations

import json
import urllib.error
import urllib.parse
import urllib.request
from datetime import date, timedelta
from pathlib import Path

ART = Path(__file__).resolve().parent
GW = "http://127.0.0.1:48080"


def http(method, url, headers=None, data=None, timeout=120):
    h = dict(headers or {})
    body = None
    if data is not None:
        body = data.encode() if isinstance(data, str) else data
        h.setdefault("Content-Type", "application/json")
    req = urllib.request.Request(url, data=body, headers=h, method=method)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            return r.status, r.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "replace")
    except urllib.error.URLError as e:
        return 0, str(e)


def save(name, obj):
    text = json.dumps(obj, ensure_ascii=False, indent=2) if isinstance(obj, (dict, list)) else obj
    (ART / name).write_text(text, encoding="utf-8")


def build_headers(token, user_id):
    login_user = {
        "id": int(user_id) if str(user_id).isdigit() else user_id,
        "userType": 2,
        "tenantId": 1,
        "scopes": [],
        "expiresTime": 1893456000000,
        "info": {"username": "admin", "nickname": "admin", "isAdmin": "true"},
    }
    return {
        "Authorization": f"Bearer {token}",
        "tenant-id": "1",
        "X-Tenant-Id": "1",
        "login-user": urllib.parse.quote(json.dumps(login_user, separators=(",", ":")), safe=""),
    }


def probe(name, method, path, hdr, expect_code=0, min_total=None, min_rows=None):
    url = GW + path
    st, raw = http(method, url, hdr)
    code = msg = None
    total = row_count = None
    snippet = raw[:2000]
    try:
        body = json.loads(raw)
        code = body.get("code")
        msg = (body.get("msg") or "")[:300]
        data = body.get("data")
        if isinstance(data, dict):
            total = data.get("total")
            lst = data.get("list")
            if isinstance(lst, list):
                row_count = len(lst)
        elif isinstance(data, list):
            row_count = len(data)
    except Exception:
        msg = raw[:300]
    ok = st == 200 and code == expect_code
    if min_total is not None and (total or 0) < min_total:
        ok = False
    if min_rows is not None and (row_count or 0) < min_rows:
        ok = False
    rec = {
        "name": name,
        "method": method,
        "path": path,
        "http": st,
        "code": code,
        "msg": msg,
        "total": total,
        "row_count": row_count,
        "pass": ok,
    }
    save(f"{name}.json", {"http": st, "code": code, "body": snippet})
    print(f"{'PASS' if ok else 'FAIL'}\t{name}\thttp={st}\tcode={code}\ttotal={total}\trows={row_count}")
    return rec, raw


def main():
    end = date.today().isoformat()
    start = (date.today() - timedelta(days=29)).isoformat()
    q = f"startDate={start}&endDate={end}"

    st, raw = http(
        "POST",
        GW + "/admin-api/system/auth/login",
        {"Content-Type": "application/json", "tenant-id": "1"},
        json.dumps({"username": "admin", "password": "admin123"}),
    )
    login = json.loads(raw)
    save("00-login.json", {"http": st, "body": login})
    token = login["data"]["accessToken"]
    uid = login["data"].get("userId") or 1
    hdr = build_headers(token, uid)

    results = []
    probes = [
        ("follower-analysis-list", "GET", f"/admin-api/ops/follower-analysis/list?page=1&size=10&{q}", {"min_total": 1}),
        ("follower-analysis-stats", "GET", f"/admin-api/ops/follower-analysis/stats?{q}", {}),
        ("follower-analysis-trend", "GET", f"/admin-api/ops/follower-analysis/trend?{q}", {"min_rows": 1}),
        ("follower-analysis-account-1000006", "GET", f"/admin-api/ops/follower-analysis/list?accountId=1000006&page=1&size=10&{q}", {"min_total": 1}),
        ("account-analysis-followers-1000006", "GET", f"/admin-api/ops/account-analysis/1000006/followers?{q}", {"min_rows": 1}),
    ]
    for name, method, path, opts in probes:
        rec, _ = probe(name, method, path, hdr, min_total=opts.get("min_total"), min_rows=opts.get("min_rows"))
        results.append(rec)

    passed = sum(1 for r in results if r["pass"])
    summary = {
        "artifact": "FOLLOWER-ANALYSIS-20260806",
        "when": date.today().isoformat(),
        "gateway": GW,
        "login": "admin/admin123 tenant=1",
        "passed": passed,
        "total": len(results),
        "all_pass": passed == len(results),
        "results": results,
    }
    save("RESULTS.json", summary)

    report = f"""# FOLLOWER-ANALYSIS E2E — 20260806

## Root cause
`FollowerAnalysisServiceImpl` only queried `oa_follower_daily`. Collected follower data lives in
`account_status_log` / platform follower tables and was already used by `AccountAnalysisServiceImpl`
via `CollectedDataQueryService`, but not by the standalone 粉丝分析 page.

WeChat official accounts (mp_account SSOT, e.g. id 1000006) were also missing from bulk account
resolution because `resolveAccountIds` did not use `WechatOfficialAccountResolver`.

## Fix
- Merge collected follower stats when `oa_follower_daily` is empty for an account (same as account analysis).
- Resolve single/bulk account IDs via `WechatOfficialAccountResolver` + `PlatformAccountService` for WECHAT_OFFICIAL.

## API smoke ({passed}/{len(results)} pass)
"""
    for r in results:
        mark = "PASS" if r["pass"] else "FAIL"
        report += f"- [{mark}] `{r['name']}` total={r.get('total')} rows={r.get('row_count')}\n"
    report += """
## Manual UI verify
1. Login http://localhost:5777 — admin / admin123, tenant 1
2. 运营管理 → 粉丝分析
3. Default 近30日 range → KPI cards and 粉丝趋势/粉丝列表 should show rows (not all zeros)
4. Optional: filter IP组 功能测试A → should include account 1000006 with followerCount=3
"""
    save("REPORT.md", report)
    print(f"\nSummary: {passed}/{len(results)} PASS -> {ART}")
    return 0 if summary["all_pass"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
