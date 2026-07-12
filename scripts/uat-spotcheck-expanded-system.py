#!/usr/bin/env python3
"""Expanded UAT — 系统管理(OA) menus 6137-6141 + dict §19 API checks."""
from __future__ import annotations

import json
import re
import sys
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
FF = ROOT / "football-front/apps/web-ele/src"
AUTH_BASE = "http://localhost:48081"
OA_BASE = "http://localhost:48094"
GATEWAY_BASE = "http://localhost:48080"
VITE = "http://localhost:5777"
OUT = ROOT / "docs/delivery/uat-spotcheck-expanded-system-probe.json"

THEME_BG = [
    re.compile(p, re.I)
    for p in [
        r"background(?:-color)?\s*:\s*#fff\b",
        r"background(?:-color)?\s*:\s*#ffffff\b",
        r"background(?:-color)?\s*:\s*white\b",
        r"background(?:-color)?\s*:\s*#f5f7fa\b",
        r"background(?:-color)?\s*:\s*#fafafa\b",
    ]
]
THEME_ANT = re.compile(r"#1890ff|#40a9ff", re.I)
BARE_AT = re.compile(r"""from\s+['"]@/""")

PAGES = [
    {
        "title": "字典配置",
        "menu_id": 6137,
        "hash": "#/ops/system-dict",
        "path": "/ops/system-dict",
        "component": "ops/system/DictManage",
        "apis": [
            ("dict/type/list", "GET", "/admin-api/oa/dict/type/list", lambda d: isinstance(d, list) and len(d) > 0),
            (
                "system/dict/list",
                "GET",
                "/admin-api/oa/system/dict/list?pageNo=1&pageSize=10",
                lambda d: isinstance(d, dict) and "list" in d,
            ),
            (
                "dict/data",
                "GET",
                "/admin-api/oa/dict/data?type=dict_platform_type",
                lambda d: isinstance(d, dict) and len(d.get("list") or []) > 0,
            ),
        ],
        "search": "TableSearch: 字典名称 / 字典类型 / 状态",
    },
    {
        "title": "登录日志",
        "menu_id": 6138,
        "hash": "#/ops/system-log/login",
        "path": "/ops/system-log/login",
        "component": "ops/system/LoginLog",
        "apis": [
            (
                "log/login",
                "GET",
                "/admin-api/oa/system/log/login?pageNo=1&pageSize=10",
                lambda d: isinstance(d, dict) and "list" in d,
            ),
        ],
        "search": "TableSearch: 用户 / IP / 状态 / 时间",
    },
    {
        "title": "操作日志",
        "menu_id": 6139,
        "hash": "#/ops/system-log/operation",
        "path": "/ops/system-log/operation",
        "component": "ops/system/LogManage",
        "apis": [
            (
                "log/operation",
                "GET",
                "/admin-api/oa/system/log/operation?pageNo=1&pageSize=10",
                lambda d: isinstance(d, dict) and "list" in d,
            ),
        ],
        "search": "TableSearch: 模块 / 操作人 / 类型 / 时间",
    },
    {
        "title": "消息管理",
        "menu_id": 6140,
        "hash": "#/ops/system-message",
        "path": "/ops/system-message",
        "component": "ops/system/MessageManage",
        "apis": [
            (
                "message/list",
                "GET",
                "/admin-api/oa/system/message/list?pageNo=1&pageSize=10",
                lambda d: isinstance(d, dict) and "list" in d,
            ),
        ],
        "search": "TableSearch: 标题 / 类型 / 状态",
    },
    {
        "title": "系统参数",
        "menu_id": 6141,
        "hash": "#/ops/system-param",
        "path": "/ops/system-param",
        "component": "ops/system/ParamManage",
        "apis": [
            (
                "param/list",
                "GET",
                "/admin-api/oa/system/param/list?pageNo=1&pageSize=10",
                lambda d: isinstance(d, dict) and "list" in d,
            ),
        ],
        "search": "TableSearch: 参数名称 / 键名 / 状态",
    },
]


def http_json(method: str, url: str, headers: dict[str, str], body: dict | None = None) -> dict:
    data = json.dumps(body).encode() if body else None
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    if body:
        req.add_header("Content-Type", "application/json")
    with urllib.request.urlopen(req, timeout=60) as resp:
        return json.loads(resp.read().decode())


def login(base: str) -> str | None:
    url = f"{base.rstrip('/')}/admin-api/system/auth/login"
    headers = {"tenant-id": "1", "Content-Type": "application/json"}
    body = {"username": "admin", "password": "admin123", "captchaVerification": ""}
    try:
        r = http_json("POST", url, headers, body)
        if r.get("code") == 0 and r.get("data", {}).get("accessToken"):
            return r["data"]["accessToken"]
    except Exception:
        pass
    return None


def probe_api(name, method, path, headers, check, base=OA_BASE):
    url = base.rstrip("/") + path
    try:
        r = http_json(method, url, headers)
        code = r.get("code")
        data = r.get("data")
        ok = code == 0
        summary = ""
        if ok and data is not None:
            if isinstance(data, dict):
                if "list" in data:
                    summary = f"list={len(data.get('list') or [])} total={data.get('total', '?')}"
                else:
                    summary = f"keys={list(data.keys())[:5]}"
            elif isinstance(data, list):
                summary = f"array len={len(data)}"
        elif not ok:
            summary = str(r.get("msg", ""))[:100]
        sensible = ok and (check(data) if check else True)
        return {"name": name, "path": path, "ok": ok and sensible, "code": code, "data": summary}
    except Exception as ex:
        return {"name": name, "path": path, "ok": False, "code": None, "data": str(ex)[:120]}


def check_route(comp: str) -> bool:
    return (FF / "views" / f"{comp}.vue").exists()


def check_vite(comp: str) -> bool:
    url = f"{VITE.rstrip('/')}/src/views/{comp}.vue"
    try:
        req = urllib.request.Request(url, headers={"Accept": "*/*"})
        with urllib.request.urlopen(req, timeout=60) as resp:
            body = resp.read(4000).decode("utf-8", "replace")
            return resp.status == 200 and ("import" in body or "export" in body)
    except Exception:
        return False


def check_theme(comp: str) -> bool:
    text = (FF / "views" / f"{comp}.vue").read_text(encoding="utf-8")
    if "ops-page" not in text:
        return False
    if BARE_AT.search(text):
        return False
    styles = "\n".join(re.findall(r"<style[^>]*>([\s\S]*?)</style>", text))
    if THEME_ANT.search(styles):
        return False
    return not any(p.search(styles) for p in THEME_BG)


def main() -> int:
    token = login(AUTH_BASE)
    print(f"LOGIN {'OK' if token else 'FAIL'} (system-server :48081)")
    headers = (
        {"Authorization": f"Bearer {token}", "X-Tenant-Id": "1", "tenant-id": "1"} if token else {}
    )

    gateway_checks = []
    if token:
        for gp in [
            "/admin-api/oa/dict/type/list",
            "/admin-api/oa/system/dict/list?pageNo=1&pageSize=10",
            "/admin-api/oa/dict/data?type=dict_platform_type",
        ]:
            r = probe_api(gp.split("/")[-1], "GET", gp, headers, None, base=GATEWAY_BASE)
            gateway_checks.append(r)
            print(f"GATEWAY {gp} -> code={r['code']} {r['data']}")

    results = []
    for p in PAGES:
        comp = p["component"]
        route = check_route(comp)
        vite = check_vite(comp)
        theme = check_theme(comp)
        apis = [probe_api(a[0], a[1], a[2], headers, a[3]) for a in p["apis"]] if token else []
        api_ok = all(a["ok"] for a in apis) if apis else False
        passed = route and vite and theme and api_ok
        row = {
            **p,
            "route": route,
            "vite": vite,
            "theme": theme,
            "api_ok": api_ok,
            "apis": apis,
            "pass": passed,
        }
        results.append(row)
        status = "PASS" if passed else "FAIL"
        print(f"\n[{status}] {p['title']} route={route} vite={vite} theme={theme} api={api_ok}")
        for a in apis:
            flag = "OK" if a["ok"] else "FAIL"
            print(f"  API {a['name']}: code={a['code']} {a['data']} -> {flag}")

    passed = sum(1 for r in results if r["pass"])
    payload = {
        "login": "ok" if token else "fail",
        "auth_base": AUTH_BASE,
        "oa_base": OA_BASE,
        "gateway_base": GATEWAY_BASE,
        "gateway_dict_checks": gateway_checks,
        "pages": results,
    }
    OUT.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"\nSUMMARY: {passed}/{len(results)} PASS")
    print(f"JSON: {OUT.relative_to(ROOT)}")
    return 0 if passed == len(results) else 1


if __name__ == "__main__":
    sys.exit(main())
