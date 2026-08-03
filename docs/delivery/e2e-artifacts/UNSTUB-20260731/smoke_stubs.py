#!/usr/bin/env python3
"""Smoke still-stubbed FE-hot paths; print 410 deferred hits."""
import json
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

OUT = Path(__file__).resolve().parent
GW = "http://127.0.0.1:48080"
OPS = "http://127.0.0.1:48094"


def req(method, url, headers=None, data=None, timeout=30):
    h = dict(headers or {})
    body = None
    if data is not None:
        body = data.encode() if isinstance(data, str) else data
        h.setdefault("Content-Type", "application/json")
    r = urllib.request.Request(url, data=body, headers=h, method=method)
    try:
        with urllib.request.urlopen(r, timeout=timeout) as resp:
            return resp.status, resp.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "replace")
    except Exception as e:
        return 0, f"ERR:{e}"


st, raw = req(
    "POST",
    f"{GW}/admin-api/system/auth/login",
    {"tenant-id": "1", "X-Tenant-Id": "1"},
    json.dumps({"username": "admin", "password": "admin123"}),
)
login = json.loads(raw)
token = login["data"]["accessToken"]
uid = login["data"]["userId"]
login_user = {
    "id": int(uid) if str(uid).isdigit() else uid,
    "userType": 2,
    "tenantId": 1,
    "scopes": [],
    "expiresTime": 1893456000000,
    "info": {"username": "admin", "nickname": "admin", "isAdmin": "true"},
}
hdr = {
    "Authorization": f"Bearer {token}",
    "tenant-id": "1",
    "X-Tenant-Id": "1",
    "login-user": urllib.parse.quote(
        json.dumps(login_user, separators=(",", ":")), safe=""
    ),
}

paths_get = [
    "/admin-api/ops/dashboard/home/summary",
    "/admin-api/ops/dashboard/home/todos",
    "/admin-api/ops/dashboard/list?pageNo=1&pageSize=1",
    "/admin-api/ops/perf/template/list?pageNo=1&pageSize=1",
    "/admin-api/ops/perf/record/list?pageNo=1&pageSize=1",
    "/admin-api/ops/perf/result/list?pageNo=1&pageSize=1",
    "/admin-api/ops/account-analysis/list?pageNo=1&pageSize=1",
    "/admin-api/ops/follower-analysis/list?pageNo=1&pageSize=1",
    "/admin-api/ops/internal-content/list?pageNo=1&pageSize=1",
    "/admin-api/ops/productivity-review/list?pageNo=1&pageSize=1",
    "/admin-api/ops/author/list?pageNo=1&pageSize=1",
    "/admin-api/ops/author-ext/list?pageNo=1&pageSize=1",
    "/admin-api/ops/config/ai-prompt/list?pageNo=1&pageSize=1",
    "/admin-api/ops/config/threshold/list?pageNo=1&pageSize=1",
    "/admin-api/ops/metric/list?pageNo=1&pageSize=1",
    "/admin-api/ops/query/list?pageNo=1&pageSize=1",
    "/admin-api/ops/report/list?pageNo=1&pageSize=1",
    "/admin-api/ops/monitor/list?pageNo=1&pageSize=1",
    "/admin-api/ops/content-analysis/list?pageNo=1&pageSize=1",
    "/admin-api/ops/funnel/list?pageNo=1&pageSize=1",
    "/admin-api/ops/ops/stats",
    "/admin-api/ops/internal/personal-account/list?pageNo=1&pageSize=1",
    "/admin-api/ops/account/fan-group/list?pageNo=1&pageSize=1",
    "/admin-api/ops/account/wechat-cert-renewal/list?pageNo=1&pageSize=1",
    "/admin-api/ops/collector-bind/list?pageNo=1&pageSize=1",
    "/admin-api/ops/system/message/list?pageNo=1&pageSize=1",
    "/admin-api/ops/system/param/list?pageNo=1&pageSize=1",
    "/admin-api/ops/metadata/list?pageNo=1&pageSize=1",
]

# More accurate FE paths from prior audits
extra_get = [
    "/admin-api/ops/config/ai-prompt/page?pageNo=1&pageSize=10",
    "/admin-api/ops/config/threshold/page?pageNo=1&pageSize=10",
    "/admin-api/ops/author/page?pageNo=1&pageSize=10",
    "/admin-api/ops/perf/template/page?pageNo=1&pageSize=10",
    "/admin-api/ops/internal-content/page?pageNo=1&pageSize=10",
    "/admin-api/ops/productivity-review/page?pageNo=1&pageSize=10",
    "/admin-api/ops/account-analysis/page?pageNo=1&pageSize=10",
    "/admin-api/ops/follower-analysis/page?pageNo=1&pageSize=10",
]

paths_post = [
    "/admin-api/ops/config/ai-prompt/create",
    "/admin-api/ops/config/threshold/create",
    "/admin-api/ops/perf/template/create",
    "/admin-api/ops/perf/record/create",
    "/admin-api/ops/author/create",
    "/admin-api/ops/internal-content/create",
    "/admin-api/ops/dashboard/create",
    "/admin-api/ops/productivity-review/create",
    "/admin-api/ops/account/fan-group/create",
    "/admin-api/ops/metric/create",
    "/admin-api/ops/query/create",
    "/admin-api/ops/account-analysis/create",
    "/admin-api/ops/collector-bind/create",
]

results = []
print("=== GET smoke ===")
for p in paths_get + extra_get:
    st, raw = req("GET", GW + p, hdr)
    try:
        b = json.loads(raw)
        code = b.get("code")
        msg = (b.get("msg") or "")[:120]
        data = b.get("data")
        tot = data.get("total") if isinstance(data, dict) else None
    except Exception:
        code, msg, tot = "?", raw[:120], None
    deferred = code == 410 or "deferred" in str(msg).lower() or "ADR-058" in str(msg)
    flag = "STUB410" if deferred else ("EMPTY" if tot == 0 else "OK")
    line = f"{flag}\thttp={st}\tcode={code}\ttotal={tot}\t{p}\t{msg}"
    print(line)
    results.append({"method": "GET", "path": p, "http": st, "code": code, "msg": msg, "total": tot, "flag": flag})

print("=== POST smoke ===")
for p in paths_post:
    st, raw = req("POST", GW + p, hdr, "{}")
    try:
        b = json.loads(raw)
        code = b.get("code")
        msg = (b.get("msg") or "")[:120]
    except Exception:
        code, msg = "?", raw[:120]
    deferred = code == 410 or "deferred" in str(msg).lower() or "ADR-058" in str(msg)
    flag = "STUB410" if deferred else "OTHER"
    line = f"{flag}\thttp={st}\tcode={code}\t{p}\t{msg}"
    print(line)
    results.append({"method": "POST", "path": p, "http": st, "code": code, "msg": msg, "flag": flag})

# Also hit direct ops for a few write paths to confirm stub
print("=== DIRECT ops POST ===")
for p in [
    "/admin-api/oa/config/ai-prompt/create",
    "/admin-api/oa/author/create",
    "/admin-api/oa/perf/template/create",
    "/admin-api/oa/dashboard/create",
]:
    st, raw = req("POST", OPS + p, hdr, "{}")
    try:
        b = json.loads(raw)
        code = b.get("code")
        msg = (b.get("msg") or "")[:120]
    except Exception:
        code, msg = "?", raw[:120]
    print(f"DIRECT\thttp={st}\tcode={code}\t{p}\t{msg}")
    results.append({"method": "POST", "path": "DIRECT:" + p, "http": st, "code": code, "msg": msg})

(OUT / "smoke-results.json").write_text(
    json.dumps(results, ensure_ascii=False, indent=2), encoding="utf-8"
)
print("wrote", OUT / "smoke-results.json")
