#!/usr/bin/env python3
"""P5-MIGRATE-6 smoke: dict/file + regression on :48095; keep :48094 UP."""
import json
import os
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

OUT = Path(__file__).resolve().parent
OPS = "http://127.0.0.1:48095"
GW = "http://127.0.0.1:48080"
PROD = "http://127.0.0.1:48094"
INFRA = "http://127.0.0.1:48082"


def req(method, url, headers=None, data=None, timeout=45, multipart=None):
    h = dict(headers or {})
    body = None
    if multipart is not None:
        boundary = "----OpsMigrate6Boundary"
        parts = []
        for name, (filename, content, ctype) in multipart.items():
            parts.append(f"--{boundary}\r\n".encode())
            parts.append(
                f'Content-Disposition: form-data; name="{name}"; filename="{filename}"\r\n'
                f"Content-Type: {ctype}\r\n\r\n".encode()
            )
            parts.append(content)
            parts.append(b"\r\n")
        parts.append(f"--{boundary}--\r\n".encode())
        body = b"".join(parts)
        h["Content-Type"] = f"multipart/form-data; boundary={boundary}"
    elif data is not None:
        body = data.encode() if isinstance(data, str) else data
        h.setdefault("Content-Type", "application/json")
    r = urllib.request.Request(url, data=body, headers=h, method=method)
    try:
        with urllib.request.urlopen(r, timeout=timeout) as resp:
            return resp.status, resp.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "replace")


def save(name, obj):
    path = OUT / name
    path.write_text(json.dumps(obj, ensure_ascii=False, indent=2), encoding="utf-8")
    return path


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    results = {"slice": "P5-MIGRATE-6 System 支撑", "port": 48095, "checks": []}

    st, raw = req("GET", f"{PROD}/actuator/health")
    prod = json.loads(raw) if raw.startswith("{") else {"raw": raw}
    results["production_48094"] = prod.get("status", st)
    save("48094-health.json", {"http": st, "body": prod})

    st, raw = req(
        "POST",
        f"{GW}/admin-api/system/auth/login",
        {"tenant-id": "1", "X-Tenant-Id": "1"},
        json.dumps({"username": "admin", "password": "admin123"}),
    )
    login = json.loads(raw)
    save("00-login.json", login)
    token = (login.get("data") or {}).get("accessToken")
    user_id = (login.get("data") or {}).get("userId")
    print("login", st, login.get("code"), "token", bool(token), "userId", user_id)

    login_user = {
        "id": int(user_id) if user_id and str(user_id).isdigit() else user_id,
        "userType": 2,
        "tenantId": 1,
        "scopes": [],
        "expiresTime": 1893456000000,
        "info": {"username": "admin", "nickname": "admin", "isAdmin": "true"},
    }
    login_user_hdr = urllib.parse.quote(json.dumps(login_user, separators=(",", ":")), safe="")

    headers = {
        "Authorization": f"Bearer {token}",
        "tenant-id": "1",
        "X-Tenant-Id": "1",
        "login-user": login_user_hdr,
    }

    checks = [
        ("dict-platform-type", "/admin-api/oa/dict/data?type=dict_platform_type"),
        ("dict-position", "/admin-api/oa/dict/data?type=dict_position"),
        ("ip-group-list", "/admin-api/oa/ip-group/list?pageNum=1&pageSize=5"),
        ("content-list", "/admin-api/oa/content/list?pageNum=1&pageSize=5"),
        ("account-list", "/admin-api/oa/account/list?pageNum=1&pageSize=5"),
        ("task-list", "/admin-api/oa/task/list?pageNum=1&pageSize=5"),
    ]

    all_ok = True
    for label, path in checks:
        st, raw = req("GET", OPS + path, headers)
        try:
            j = json.loads(raw)
        except Exception:
            j = {"code": None, "msg": raw[:300]}
        save(f"{label}.json", j)
        data = j.get("data") or {}
        total = data.get("total") if isinstance(data, dict) else None
        if total is None and isinstance(data, list):
            total = len(data)
        list_len = None
        if isinstance(data, dict) and isinstance(data.get("list"), list):
            list_len = len(data["list"])
        entry = {
            "path": path,
            "http": st,
            "code": j.get("code"),
            "total": total,
            "list_len": list_len,
            "msg": j.get("msg"),
        }
        results["checks"].append(entry)
        ok = j.get("code") == 0
        if label.startswith("dict-"):
            ok = ok and (total or 0) > 0
        all_ok = all_ok and ok
        print(f"{label} HTTP={st} code={j.get('code')} total={total} list_len={list_len} msg={str(j.get('msg'))[:80]}")

    # File upload via OPS → FileApi Feign (tiny 1x1 png)
    png = (
        b"\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x01\x00\x00\x00\x01"
        b"\x08\x02\x00\x00\x00\x90wS\xde\x00\x00\x00\x0cIDATx\x9cc\xf8\x0f\x00"
        b"\x00\x01\x01\x00\x05\x18\xd8N\x00\x00\x00\x00IEND\xaeB`\x82"
    )
    st, raw = req(
        "POST",
        OPS + "/admin-api/oa/file/upload",
        headers,
        multipart={"file": ("smoke.png", png, "image/png")},
    )
    try:
        upload = json.loads(raw)
    except Exception:
        upload = {"code": None, "msg": raw[:300]}
    save("file-upload.json", upload)
    upload_ok = upload.get("code") == 0 and bool((upload.get("data") or {}).get("url"))
    # Accept infra unavailable message only if infra itself is down; otherwise require success
    st_i, raw_i = req("GET", f"{INFRA}/rpc-api/infra/file/presigned-url?url=http://example.com/a&expirationSeconds=60")
    try:
        infra = json.loads(raw_i)
    except Exception:
        infra = {"code": None}
    save("infra-presign.json", {"http": st_i, "body": infra})
    results["checks"].append({
        "path": "/admin-api/oa/file/upload",
        "http": st,
        "code": upload.get("code"),
        "url": (upload.get("data") or {}).get("url"),
        "msg": upload.get("msg"),
        "upload_ok": upload_ok,
        "infra_presign_code": infra.get("code"),
    })
    print(f"file-upload HTTP={st} code={upload.get('code')} ok={upload_ok} msg={str(upload.get('msg'))[:100]}")
    all_ok = all_ok and upload_ok

    results["ok"] = all_ok and results["production_48094"] == "UP"
    results["deferred"] = [
        "Message center / notification UI",
        "Metadata admin domain",
        "SystemDict admin CRUD remains 410 (Football Admin)",
    ]
    results["next"] = "P5-MIGRATE-7 Analytics / 订单只读（或 Order/Finance）→ Cutover"
    save("RESULTS.json", results)
    print("RESULTS", results["ok"], "prod48094", results["production_48094"])
    return 0 if results["ok"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
