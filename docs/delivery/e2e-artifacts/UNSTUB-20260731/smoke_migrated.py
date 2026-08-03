#!/usr/bin/env python3
"""Smoke newly unstubbed domains via gateway."""
import json
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

OUT = Path(__file__).resolve().parent
GW = "http://127.0.0.1:48080"


def req(method, url, headers=None, data=None, timeout=45):
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

checks = [
    ("GET", "/admin-api/ops/author/list?page=1&size=5&status=1", None),
    ("GET", "/admin-api/ops/author/page?pageNo=1&pageSize=5&ipGroupId=9000&status=1", None),
    ("GET", "/admin-api/ops/config/ai-prompt/list?pageNo=1&pageSize=5", None),
    ("GET", "/admin-api/ops/config/threshold/list?pageNo=1&pageSize=5", None),
    ("GET", "/admin-api/ops/perf/template/list?pageNum=1&pageSize=5", None),
    ("GET", "/admin-api/ops/perf/record/list?pageNum=1&pageSize=5", None),
    ("GET", "/admin-api/ops/metric/list?pageNum=1&pageSize=5", None),
    ("GET", "/admin-api/ops/internal-content/list?pageNum=1&pageSize=5", None),
    ("GET", "/admin-api/ops/productivity-review/list?pageNum=1&pageSize=5", None),
    ("POST", "/admin-api/ops/config/ai-prompt/create", json.dumps({
        "templateName": "smoke-prompt-" + __import__("uuid").uuid4().hex[:8],
        "scene": "CONTENT",
        "promptContent": "smoke test prompt",
        "status": "ENABLED",
    })),
    ("POST", "/admin-api/ops/author/create", "{}"),
    # still stubbed control
    ("POST", "/admin-api/ops/dashboard/create", "{}"),
    ("POST", "/admin-api/ops/collector-bind/create", "{}"),
]

results = []
for method, path, data in checks:
    st, raw = req(method, GW + path, hdr, data)
    try:
        b = json.loads(raw)
        code = b.get("code")
        msg = (b.get("msg") or "")[:140]
        data_obj = b.get("data")
        tot = data_obj.get("total") if isinstance(data_obj, dict) else None
        if isinstance(data_obj, list):
            tot = len(data_obj)
    except Exception:
        code, msg, tot = "?", raw[:140], None
    deferred = code == 410 and ("ADR-058" in str(msg) or "deferred" in str(msg).lower())
    author_dep = code == 410 and "作者" in str(msg)
    flag = "STUB410" if deferred else ("AUTHOR_DEP" if author_dep else ("OK" if code == 0 else f"CODE{code}"))
    line = f"{flag}\t{method}\thttp={st}\tcode={code}\ttotal={tot}\t{path}\t{msg}"
    print(line)
    results.append({
        "flag": flag, "method": method, "path": path, "http": st,
        "code": code, "msg": msg, "total": tot,
    })

(OUT / "smoke-migrated.json").write_text(
    json.dumps(results, ensure_ascii=False, indent=2), encoding="utf-8"
)
print("wrote", OUT / "smoke-migrated.json")
