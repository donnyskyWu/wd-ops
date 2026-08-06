#!/usr/bin/env python3
"""PRODUCTIVITY-E2E-20260806: post-dev API smoke for productivity-review, layout-template, order attribution."""
from __future__ import annotations

import json
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime, timezone
from pathlib import Path

ART = Path(__file__).resolve().parent
GW = "http://127.0.0.1:48080"
OPS = "http://127.0.0.1:48094"


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


def probe(name, method, path, hdr, expect_code=0, timeout=120):
    url = GW + path
    st, raw = http(method, url, hdr, timeout=timeout)
    code = msg = None
    snippet = raw[:1500]
    extra = {}
    try:
        body = json.loads(raw)
        code = body.get("code")
        msg = (body.get("msg") or "")[:300]
        data = body.get("data")
        if isinstance(data, dict):
            extra["total"] = data.get("total")
            lst = data.get("list")
            if isinstance(lst, list) and lst:
                extra["first_keys"] = sorted(lst[0].keys())[:20]
                extra["first_row"] = {k: lst[0].get(k) for k in list(lst[0].keys())[:8]}
        elif data is not None:
            extra["data_type"] = type(data).__name__
    except Exception:
        msg = raw[:300]
    ok = st == 200 and code == expect_code
    rec = {"name": name, "method": method, "path": path, "http": st, "code": code, "msg": msg, "pass": ok, **extra}
    save(f"{name}.json", {"http": st, "code": code, "body": snippet})
    print(f"{'PASS' if ok else 'FAIL'}\t{name}\thttp={st}\tcode={code}\t{msg or ''}")
    return rec, raw


def compare_tab_fields(wechat_raw, video_raw):
    try:
        w = json.loads(wechat_raw).get("data", {}).get("list") or []
        v = json.loads(video_raw).get("data", {}).get("list") or []
        w0 = w[0] if w else {}
        v0 = v[0] if v else {}
        keys = sorted(set(w0.keys()) | set(v0.keys()))
        diffs = {}
        for k in ("contentOutput", "avgRead", "avgPlay", "hitCount", "platformType"):
            if k in keys:
                diffs[k] = {"wechat": w0.get(k), "video": v0.get(k)}
        return {"wechat_rows": len(w), "video_rows": len(v), "sample_diffs": diffs, "same_schema": set(w0.keys()) == set(v0.keys())}
    except Exception as e:
        return {"error": str(e)}


def main() -> int:
    ART.mkdir(parents=True, exist_ok=True)
    results = {
        "task": "PRODUCTIVITY-E2E-20260806",
        "started_at": datetime.now(timezone.utc).isoformat(),
        "gateway": GW,
        "checks": [],
        "features": {},
    }

    st, raw = http("GET", f"{OPS}/actuator/health", timeout=15)
    save("ops-health.json", {"http": st, "body": raw[:400]})
    results["ops_health"] = st == 200

    st, raw = http(
        "POST",
        f"{GW}/admin-api/system/auth/login",
        {"tenant-id": "1", "X-Tenant-Id": "1"},
        json.dumps({"username": "admin", "password": "admin123"}),
    )
    save("00-login.json", {"http": st, "body": raw[:800]})
    token = uid = None
    login_ok = False
    if st == 200:
        login = json.loads(raw)
        if login.get("code") == 0:
            token = login["data"]["accessToken"]
            uid = login["data"]["userId"]
            login_ok = True
    results["login_ok"] = login_ok
    if not login_ok:
        save("RESULTS.json", results)
        print("FATAL: login failed")
        return 2

    hdr = build_headers(token, uid)
    checks = []

    paths = [
        ("productivity-list-week", "GET", "/admin-api/ops/productivity-review/list?timeDimension=WEEK&page=1&size=10"),
        ("productivity-list-month", "GET", "/admin-api/ops/productivity-review/list?timeDimension=MONTH&page=1&size=10"),
        (
            "productivity-list-daterange",
            "GET",
            "/admin-api/ops/productivity-review/list?timeDimension=WEEK&page=1&size=10&startDate=2026-06-01&endDate=2026-06-30",
        ),
        ("productivity-tab-wechat", "GET", "/admin-api/ops/productivity-review/list?timeDimension=WEEK&page=1&size=10&tabType=wechat"),
        ("productivity-tab-video", "GET", "/admin-api/ops/productivity-review/list?timeDimension=WEEK&page=1&size=10&tabType=video"),
        ("layout-template-list", "GET", "/admin-api/ops/layout-template/list?pageNo=1&pageSize=10"),
        (
            "football-order-list",
            "GET",
            "/admin-api/ops/football-order/list?startDate=2026-06-01&endDate=2026-06-30&pageNum=1&pageSize=10",
        ),
        (
            "order-attribution-list",
            "GET",
            "/admin-api/ops/order-attribution/list?startDate=2026-06-01&endDate=2026-06-30&pageNum=1&pageSize=10",
        ),
        (
            "order-attribution-roi",
            "GET",
            "/admin-api/ops/order-attribution/roi?startDate=2026-06-01&endDate=2026-06-30",
        ),
    ]

    tab_wechat_raw = tab_video_raw = None
    detail_user_id = None

    for name, method, path in paths:
        rec, body_raw = probe(name, method, path, hdr)
        checks.append(rec)
        if name == "productivity-tab-wechat":
            tab_wechat_raw = body_raw
        if name == "productivity-tab-video":
            tab_video_raw = body_raw
        if name == "productivity-list-week" and rec.get("pass"):
            try:
                lst = json.loads(body_raw).get("data", {}).get("list") or []
                if lst:
                    detail_user_id = lst[0].get("userId")
            except Exception:
                pass

    if tab_wechat_raw and tab_video_raw:
        tab_cmp = compare_tab_fields(tab_wechat_raw, tab_video_raw)
        save("productivity-tab-compare.json", tab_cmp)
        results["tab_compare"] = tab_cmp

    if detail_user_id:
        detail_path = (
            f"/admin-api/ops/productivity-review/detail/{detail_user_id}"
            "?startDate=2026-06-01&endDate=2026-06-30&timeDimension=WEEK&tabType=wechat"
        )
        rec, _ = probe("productivity-detail-user", "GET", detail_path, hdr)
        checks.append(rec)
    else:
        checks.append({"name": "productivity-detail-user", "pass": False, "msg": "no userId from list"})

    results["checks"] = checks
    prod_checks = [c for c in checks if c["name"].startswith("productivity")]
    layout_checks = [c for c in checks if "layout" in c["name"]]
    order_checks = [c for c in checks if "order" in c["name"] or "football" in c["name"]]

    results["features"] = {
        "productivity_review": all(c.get("pass") for c in prod_checks),
        "layout_template": all(c.get("pass") for c in layout_checks),
        "order_attribution": all(c.get("pass") for c in order_checks if c["name"] != "order-attribution-roi"),
        "order_attribution_roi_api": next((c.get("pass") for c in checks if c["name"] == "order-attribution-roi"), False),
        "order_attribution_roi_ui_width": "skipped_manual",
    }
    results["all_pass"] = all(c.get("pass") for c in checks)
    results["finished_at"] = datetime.now(timezone.utc).isoformat()
    save("RESULTS.json", results)

    # REPORT.md
    lines = [
        "# PRODUCTIVITY E2E — 2026-08-06",
        "",
        f"- Gateway: `{GW}` | Ops health: {'OK' if results['ops_health'] else 'FAIL'}",
        f"- Login: {'OK' if login_ok else 'FAIL'} (admin / tenant 1)",
        "",
        "## Feature summary",
        "",
        "| Feature | Result |",
        "|---------|--------|",
    ]
    for feat, ok in results["features"].items():
        if ok == "skipped_manual":
            lines.append(f"| {feat} | SKIP (manual visual) |")
        else:
            lines.append(f"| {feat} | {'PASS' if ok else 'FAIL'} |")
    lines.extend(["", "## Checks", ""])
    for c in checks:
        status = "PASS" if c.get("pass") else "FAIL"
        lines.append(f"- **{status}** `{c.get('name')}` http={c.get('http')} code={c.get('code')} — {c.get('msg') or ''}")
    if results.get("tab_compare"):
        lines.extend(["", "## tabType wechat vs video", "", "```json", json.dumps(results["tab_compare"], ensure_ascii=False, indent=2), "```"])
    fails = [c for c in checks if not c.get("pass")]
    if fails:
        lines.extend(["", "## Failures", ""])
        for c in fails:
            fn = f"{c.get('name')}.json"
            p = ART / fn
            if p.is_file():
                lines.append(f"See `{fn}` for response snippet.")
    (ART / "REPORT.md").write_text("\n".join(lines) + "\n", encoding="utf-8")
    return 0 if results["all_pass"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
