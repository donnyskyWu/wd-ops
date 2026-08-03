#!/usr/bin/env python3
"""V172 table-drop full OPS API regression via Gateway + direct ops health."""
from __future__ import annotations

import json
import os
import subprocess
import sys
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime, timezone
from pathlib import Path

ART = Path(__file__).resolve().parent
ROOT = ART.parents[3]
ENV_FILE = ROOT / "scripts/integration-config/ops-test-remote.env"
GW = "http://127.0.0.1:48080"
OPS = "http://127.0.0.1:48094"
API_BASE = GW

DROP_TARGETS = [
    "archive_sys_user",
    "oa_author",
    "oa_demo_item",
    "system_users",
    "system_menu",
]


def http(method, url, headers=None, data=None, timeout=90):
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
    if isinstance(obj, (dict, list)):
        text = json.dumps(obj, ensure_ascii=False, indent=2)
    else:
        text = obj
    (ART / name).write_text(text, encoding="utf-8")


def load_env() -> dict[str, str]:
    cfg: dict[str, str] = {}
    for line in ENV_FILE.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        k, v = line.split("=", 1)
        cfg[k.strip()] = v.strip().strip('"').strip("'")
    return cfg


def mysql_table_exists(cfg: dict[str, str], table: str) -> bool:
    env = os.environ.copy()
    env["MYSQL_PWD"] = cfg["OPS_TEST_MASTER_PASSWORD"]
    host = cfg.get("OPS_TEST_DB_HOST", "110.42.49.224")
    port = cfg.get("OPS_TEST_DB_PORT", "3306")
    user = cfg.get("OPS_TEST_MASTER_USER", "shenyu-ops")
    db = cfg.get("OPS_TEST_MASTER_DB", "shenyu-ops")
    proc = subprocess.run(
        [
            "mysql",
            f"-h{host}",
            f"-P{port}",
            f"-u{user}",
            "--default-character-set=utf8mb4",
            "-N",
            "-B",
            db,
            "-e",
            f"SELECT COUNT(*) FROM information_schema.tables "
            f"WHERE table_schema='{db}' AND table_name='{table}'",
        ],
        capture_output=True,
        env=env,
    )
    return proc.stdout.decode().strip() == "1"


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
        "login-user": urllib.parse.quote(
            json.dumps(login_user, separators=(",", ":")), safe=""
        ),
    }


def probe(name, module, method, path, hdr, data=None, expect_code=0, timeout=90):
    url = API_BASE + path
    st, raw = http(method, url, hdr, data, timeout=timeout)
    code = msg = total = None
    try:
        body = json.loads(raw)
        code = body.get("code")
        msg = (body.get("msg") or "")[:200]
        data_obj = body.get("data")
        if isinstance(data_obj, dict):
            total = data_obj.get("total")
        elif isinstance(data_obj, list):
            total = len(data_obj)
    except Exception:
        msg = raw[:200]
    ok = st == 200 and code == expect_code
    rec = {
        "module": module,
        "name": name,
        "method": method,
        "path": path,
        "http": st,
        "code": code,
        "msg": msg,
        "total": total,
        "pass": ok,
    }
    save(f"{name}.json", {"http": st, "code": code, "body": raw[:2000]})
    print(f"{'PASS' if ok else 'FAIL'}\t{module}\t{name}\thttp={st}\tcode={code}\ttotal={total}")
    return rec


def main() -> int:
    ART.mkdir(parents=True, exist_ok=True)
    cfg = load_env()
    results = {
        "task": "V172-TABLE-DROP-REGression",
        "date": "2026-08-03",
        "gateway": GW,
        "ops_direct": OPS,
        "beta_host": cfg.get("OPS_TEST_DB_HOST"),
        "v172": {},
        "checks": [],
    }

    # V172 verify
    drop_verify = {t: mysql_table_exists(cfg, t) for t in DROP_TARGETS}
    flyway_172 = subprocess.run(
        [
            "mysql",
            f"-h{cfg.get('OPS_TEST_DB_HOST')}",
            f"-P{cfg.get('OPS_TEST_DB_PORT', '3306')}",
            f"-u{cfg.get('OPS_TEST_MASTER_USER')}",
            "--default-character-set=utf8mb4",
            "-N",
            "-B",
            cfg.get("OPS_TEST_MASTER_DB", "shenyu-ops"),
            "-e",
            "SELECT version,success FROM flyway_schema_history WHERE version='172'",
        ],
        capture_output=True,
        env={**os.environ, "MYSQL_PWD": cfg["OPS_TEST_MASTER_PASSWORD"]},
    ).stdout.decode().strip()
    meta_count = subprocess.run(
        [
            "mysql",
            f"-h{cfg.get('OPS_TEST_DB_HOST')}",
            f"-P{cfg.get('OPS_TEST_DB_PORT', '3306')}",
            f"-u{cfg.get('OPS_TEST_MASTER_USER')}",
            "--default-character-set=utf8mb4",
            "-N",
            "-B",
            cfg.get("OPS_TEST_MASTER_DB", "shenyu-ops"),
            "-e",
            "SELECT COUNT(*) FROM sys_metadata_entity WHERE physical_table IN ('oa_author','oa_demo_item')",
        ],
        capture_output=True,
        env={**os.environ, "MYSQL_PWD": cfg["OPS_TEST_MASTER_PASSWORD"]},
    ).stdout.decode().strip()
    still = [t for t, ex in drop_verify.items() if ex]
    results["v172"] = {
        "flyway_172": flyway_172,
        "metadata_oa_rows": int(meta_count or 0),
        "drop_targets_still_present": still,
        "applied_ok": not still and meta_count.strip() == "0",
    }
    save("v172-verify.json", results["v172"])

    st, raw = http("GET", f"{OPS}/actuator/health", timeout=15)
    ops_up = st == 200
    save("ops-health.json", {"http": st, "body": raw[:300]})
    results["checks"].append(
        {"module": "infra", "name": "ops-health", "http": st, "pass": ops_up}
    )

    st, raw = http(
        "POST",
        f"{GW}/admin-api/system/auth/login",
        {"tenant-id": "1", "X-Tenant-Id": "1"},
        json.dumps({"username": "admin", "password": "admin123"}),
    )
    save("00-login.json", {"http": st, "body": raw[:800]})
    token = None
    uid = 1
    login_ok = False
    if st == 200:
        try:
            login = json.loads(raw)
            if login.get("code") == 0:
                token = login["data"]["accessToken"]
                uid = login["data"]["userId"]
                login_ok = True
        except Exception:
            pass
    global API_BASE
    if login_ok:
        API_BASE = GW
        results["auth_mode"] = "gateway"
    else:
        API_BASE = OPS
        results["auth_mode"] = "direct-ops-login-user"
        results["auth_note"] = "gateway login failed; using ops :48094 with login-user header (beta dev)"
    hdr = build_headers(token, uid)

    checks = [
        ("account-list", "account", "GET", "/admin-api/ops/account/list?pageNo=1&pageSize=10"),
        ("ip-group-tree", "ip-group", "GET", "/admin-api/ops/ip-group/tree"),
        ("ip-group-accessible-tree", "ip-group", "GET", "/admin-api/ops/ip-group/accessible-tree"),
        ("author-list", "author", "GET", "/admin-api/ops/author/list?page=1&size=10&status=1"),
        ("plan-list", "plan", "GET", "/admin-api/ops/plan/list?pageNo=1&pageSize=10"),
        ("task-list", "task", "GET", "/admin-api/ops/task/list?pageNo=1&pageSize=10"),
        ("content-list", "content", "GET", "/admin-api/ops/content/list?pageNo=1&pageSize=10"),
        ("internal-content-list", "content", "GET", "/admin-api/ops/internal-content/list?page=1&size=10"),
        ("productivity-review-list", "review", "GET", "/admin-api/ops/productivity-review/list?page=1&size=10&startDate=2026-07-01&endDate=2026-08-03"),
        ("system-param-list", "system-param", "GET", "/admin-api/ops/system/param/list?pageNo=1&pageSize=10"),
        ("dict-content-type", "dict", "GET", "/admin-api/ops/dict/data?type=dict_content_type"),
        ("dict-platform-type", "dict", "GET", "/admin-api/ops/dict/data?type=dict_platform_type"),
        ("metadata-list", "metadata", "GET", "/admin-api/ops/metadata/list?pageNum=1&pageSize=10"),
        ("content-analysis-stats", "analytics", "GET", "/admin-api/ops/content-analysis/stats"),
        ("football-order-list", "analytics", "GET", "/admin-api/ops/football-order/list?startDate=2026-07-01&endDate=2026-08-03&pageNo=1&pageSize=10"),
        ("system-user-profile", "system", "GET", "/admin-api/ops/system/user/profile"),
        ("collect-task-page", "collect", "GET", "/admin-api/ops/collect/task/page?pageNo=1&pageSize=10"),
    ]
    for name, module, method, path in checks:
        results["checks"].append(probe(name, module, method, path, hdr))

    # collect ensure-unified (POST)
    st, ensure_raw = http("POST", f"{API_BASE}/admin-api/ops/collect/task/ensure-unified", hdr, "{}", timeout=120)
    ensure_code = None
    task_id = None
    try:
        ensure_body = json.loads(ensure_raw)
        ensure_code = ensure_body.get("code")
        data = ensure_body.get("data")
        if isinstance(data, dict):
            task_id = data.get("id")
        else:
            task_id = data
    except Exception:
        pass
    save("collect-ensure-unified.json", {"http": st, "body": ensure_raw[:1000]})
    ensure_ok = st == 200 and ensure_code == 0 and task_id
    results["checks"].append(
        {
            "module": "collect",
            "name": "collect-ensure-unified",
            "http": st,
            "code": ensure_code,
            "taskId": task_id,
            "pass": bool(ensure_ok),
        }
    )
    print(f"{'PASS' if ensure_ok else 'FAIL'}\tcollect\tcollect-ensure-unified\ttaskId={task_id}")

    if task_id:
        st, log_raw = http(
            "GET",
            f"{API_BASE}/admin-api/ops/collect/log/page?pageNo=1&pageSize=5&taskId={task_id}",
            hdr,
        )
        log_code = None
        try:
            log_code = json.loads(log_raw).get("code")
        except Exception:
            pass
        save("collect-log-page.json", {"http": st, "body": log_raw[:1000]})
        log_ok = st == 200 and log_code == 0
        results["checks"].append(
            {
                "module": "collect",
                "name": "collect-log-page",
                "http": st,
                "code": log_code,
                "pass": log_ok,
            }
        )
        print(f"{'PASS' if log_ok else 'FAIL'}\tcollect\tcollect-log-page\tcode={log_code}")

    api_pass = sum(1 for c in results["checks"] if c.get("pass"))
    api_total = len(results["checks"])
    v172_ok = results["v172"].get("applied_ok")
    results["api_base"] = API_BASE
    results["summary"] = {
        "v172_applied": v172_ok,
        "api_pass": f"{api_pass}/{api_total}",
        "modules": sorted({c.get("module") for c in results["checks"] if c.get("module")}),
    }
    results["verdict"] = "PASS" if v172_ok and api_pass == api_total else "FAIL"
    results["timestamp"] = datetime.now(timezone.utc).astimezone().isoformat()
    save("RESULTS.json", results)

    # REPORT.md
    lines = [
        "# V172 Table Drop — OPS API Regression",
        "",
        f"- Date: 2026-08-03",
        f"- Beta: {cfg.get('OPS_TEST_DB_HOST')} / shenyu-ops",
        f"- Verdict: **{results['verdict']}**",
        "",
        "## V172 apply",
        "",
        f"- Flyway V172 row: `{flyway_172 or 'missing'}`",
        f"- Metadata oa_author/oa_demo_item rows: {meta_count}",
        f"- Drop targets still present: {still or 'none'}",
        "",
        "## API matrix",
        "",
        "| Module | Check | HTTP | Code | Pass |",
        "|--------|-------|------|------|------|",
    ]
    for c in results["checks"]:
        lines.append(
            f"| {c.get('module','?')} | {c.get('name','?')} | {c.get('http','?')} | "
            f"{c.get('code','?')} | {'✅' if c.get('pass') else '❌'} |"
        )
    lines.extend(["", f"Summary: {results['summary']}", ""])
    (ART / "REPORT.md").write_text("\n".join(lines), encoding="utf-8")

    print(f"\nVERDICT {results['verdict']} v172={v172_ok} api={api_pass}/{api_total}")
    return 0 if results["verdict"] == "PASS" else 1


if __name__ == "__main__":
    raise SystemExit(main())
