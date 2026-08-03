#!/usr/bin/env python3
"""Quick verify metadata + collect-log on direct ops (login-user admin)."""
import json, urllib.parse, urllib.request, urllib.error
from pathlib import Path

ART = Path(__file__).resolve().parent
OPS = "http://127.0.0.1:48094"

login_user = urllib.parse.quote(json.dumps({
    "id": 1, "userType": 2, "tenantId": 1, "scopes": [],
    "expiresTime": 1893456000000,
    "info": {"username": "admin", "nickname": "admin", "isAdmin": "true"},
}, separators=(",", ":")), safe="")
H = {"tenant-id": "1", "X-Tenant-Id": "1", "login-user": login_user}

def get(path):
    req = urllib.request.Request(OPS + path, headers=H)
    try:
        with urllib.request.urlopen(req, timeout=60) as r:
            return r.status, r.read().decode()
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode()

for name, path in [
    ("metadata-list", "/admin-api/ops/metadata/list?pageNum=1&pageSize=10"),
    ("collect-log-page", "/admin-api/ops/collect/log/page?pageNo=1&pageSize=5&taskId=8"),
]:
    st, raw = get(path)
    code = json.loads(raw).get("code") if raw.startswith("{") else None
    (ART / f"{name}.json").write_text(json.dumps({"http": st, "code": code, "body": raw[:1500]}, ensure_ascii=False, indent=2), encoding="utf-8")
    print(name, st, code)
