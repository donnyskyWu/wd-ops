#!/usr/bin/env python3
"""UAT spot-check for Ops pages (2026-07-04).

Sections:
  priority — 内容审核 / 计划 / IP组 / 平台账号 / 内部个人账号
  collect  — 数据采集 4 页（采集任务 / 数据质量 / 内部采集配置 / 外部采集配置）
"""
from __future__ import annotations

import argparse
import json
import re
import sys
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
FF = ROOT / "football-front/apps/web-ele/src"
BASE = "http://localhost:48080"
VITE = "http://localhost:5777"
OUT_JSON_BY_SECTION = {
    "priority": ROOT / "docs/delivery/uat-spotcheck-20260704-probe.json",
    "collect": ROOT / "docs/delivery/uat-spotcheck-collect-20260704-probe.json",
}

THEME_BG_PATTERNS = [
    re.compile(r"background(?:-color)?\s*:\s*#fff\b", re.I),
    re.compile(r"background(?:-color)?\s*:\s*#ffffff\b", re.I),
    re.compile(r"background(?:-color)?\s*:\s*white\b", re.I),
    re.compile(r"background(?:-color)?\s*:\s*#f5f7fa\b", re.I),
    re.compile(r"background(?:-color)?\s*:\s*#fafafa\b", re.I),
]
THEME_ANT_PRIMARY = re.compile(r"#1890ff|#40a9ff", re.I)
BARE_AT_IMPORT = re.compile(r"""from\s+['"]@/""")


def http_json(method: str, url: str, headers: dict[str, str], body: dict | None = None) -> dict:
    data = json.dumps(body).encode("utf-8") if body is not None else None
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    if body is not None:
        req.add_header("Content-Type", "application/json")
    with urllib.request.urlopen(req, timeout=60) as resp:
        return json.loads(resp.read().decode("utf-8"))


def login(tenant_id: str = "1") -> str | None:
    url = f"{BASE.rstrip('/')}/admin-api/system/auth/login"
    headers = {"tenant-id": tenant_id, "Content-Type": "application/json"}
    body = {"username": "admin", "password": "admin123", "captchaVerification": ""}
    try:
        r = http_json("POST", url, headers, body)
        if r.get("code") == 0 and r.get("data", {}).get("accessToken"):
            return r["data"]["accessToken"]
    except Exception:
        pass
    return None


def check_route(comp: str) -> tuple[bool, list[str]]:
    vue = FF / "views" / f"{comp}.vue"
    if not vue.exists():
        return False, [f"missing component: views/{comp}.vue"]
    return True, []


def check_vite(comp: str) -> tuple[bool, list[str]]:
    url = f"{VITE.rstrip('/')}/src/views/{comp}.vue"
    try:
        req = urllib.request.Request(url, headers={"Accept": "*/*"})
        with urllib.request.urlopen(req, timeout=60) as resp:
            body = resp.read(4000).decode("utf-8", errors="replace")
            if resp.status == 200 and ("import" in body or "export" in body):
                return True, []
            return False, [f"vite HTTP {resp.status}"]
    except urllib.error.HTTPError as e:
        snippet = e.read(800).decode("utf-8", errors="replace")
        if "Failed to resolve" in snippet:
            line = next(
                (ln for ln in snippet.splitlines() if "Failed to resolve" in ln),
                snippet[:160],
            )
            return False, [line.strip()]
        return False, [f"vite HTTP {e.code}: {snippet[:120].replace(chr(10), ' ')}"]
    except Exception as ex:
        return False, [f"vite error: {ex}"]


def check_theme(comp: str) -> tuple[bool, list[str]]:
    vue = FF / "views" / f"{comp}.vue"
    text = vue.read_text(encoding="utf-8")
    issues: list[str] = []
    if "ops-page" not in text:
        issues.append("missing ops-page root class")
    if BARE_AT_IMPORT.search(text):
        issues.append("bare @/ import (Football path alias)")
    style_blocks = re.findall(r"<style[^>]*>([\s\S]*?)</style>", text)
    style_text = "\n".join(style_blocks)
    for pat in THEME_BG_PATTERNS:
        if pat.search(style_text):
            issues.append(f"hardcoded light background: {pat.pattern[:40]}")
    if THEME_ANT_PRIMARY.search(style_text):
        issues.append("antd primary override (#1890ff) — use var(--el-color-primary)")
    return len(issues) == 0, issues


def probe_api(
    name: str,
    method: str,
    path: str,
    headers: dict[str, str],
    data_check=None,
) -> dict:
    url = BASE + path
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
                elif "records" in data:
                    summary = f"records={len(data.get('records') or [])} total={data.get('total', '?')}"
                else:
                    keys = list(data.keys())[:6]
                    summary = f"object keys={keys}"
            elif isinstance(data, list):
                summary = f"array len={len(data)}"
            else:
                summary = type(data).__name__
        elif not ok:
            summary = f"msg={str(r.get('msg', ''))[:100]}"
        sensible = ok
        if data_check and ok:
            sensible = bool(data_check(data))
        return {
            "name": name,
            "path": path,
            "ok": ok and sensible,
            "code": code,
            "data": summary,
            "api_ok": ok,
            "sensible": sensible,
        }
    except Exception as ex:
        return {
            "name": name,
            "path": path,
            "ok": False,
            "code": None,
            "data": str(ex)[:120],
            "api_ok": False,
            "sensible": False,
        }


PAGES = [
    {
        "title": "内容审核",
        "hash": "#/ops/content/review",
        "football_path": "/ops/content/review",
        "component": "ops/production/content/review",
        "apis": [
            (
                "review-config",
                "GET",
                "/admin-api/oa/content/review-config",
                lambda d: isinstance(d, dict) and "level1Enabled" in d,
            ),
            (
                "content/list",
                "GET",
                "/admin-api/oa/content/list?pageNo=1&pageSize=1&status=PENDING_FIRST_REVIEW",
                lambda d: isinstance(d, dict) and "list" in d,
            ),
            (
                "dict/data",
                "GET",
                "/admin-api/oa/dict/data?type=dict_platform_type",
                lambda d: isinstance(d, dict) and len(d.get("list") or []) > 0,
            ),
        ],
        "search_note": "TableSearch: 标题 / 平台(DictSelect) / 提交人 / 提交时间",
    },
    {
        "title": "计划管理",
        "hash": "#/ops/plan",
        "football_path": "/ops/plan",
        "component": "ops/production/plan/index",
        "apis": [
            (
                "plan/list",
                "GET",
                "/admin-api/oa/plan/list?pageNo=1&pageSize=1",
                lambda d: isinstance(d, dict) and "list" in d,
            ),
        ],
        "search_note": "TableSearch on plan page",
    },
    {
        "title": "IP组管理",
        "hash": "#/ops/ip-group",
        "football_path": "/ops/ip-group",
        "component": "ops/operations/IpGroup",
        "apis": [
            (
                "ip-group/tree",
                "GET",
                "/admin-api/oa/ip-group/tree",
                lambda d: isinstance(d, list) and len(d) > 0,
            ),
        ],
        "search_note": "Left tree search (组名); ops-page root",
    },
    {
        "title": "平台账号管理",
        "hash": "#/ops/internal-account",
        "football_path": "/ops/internal-account",
        "component": "ops/internal/InternalAccountManage",
        "apis": [
            (
                "account/list",
                "GET",
                "/admin-api/oa/account/list?pageNo=1&pageSize=1&platformType=WECHAT_OFFICIAL",
                lambda d: isinstance(d, dict) and "list" in d,
            ),
        ],
        "search_note": "TableSearch: 账号名称 / 状态; platform tabs",
    },
    {
        "title": "内部个人账号",
        "hash": "#/ops/personal-account",
        "football_path": "/ops/personal-account",
        "component": "ops/internal/PersonalAccountManage",
        "apis": [
            (
                "personal-account/list",
                "GET",
                "/admin-api/oa/internal/personal-account/list?pageNo=1&pageSize=1",
                lambda d: isinstance(d, dict) and "list" in d,
            ),
        ],
        "search_note": "TableSearch on personal account page",
    },
]

# oa-menu-permission-map.csv · parent_group=数据采集 (2) + 配置管理 internal/external (2)
COLLECT_PAGES = [
    {
        "title": "采集任务",
        "menu_csv": "采集任务",
        "hash": "#/ops/collect/task",
        "football_path": "/ops/collect/task",
        "component": "ops/collect/task",
        "apis": [
            (
                "collect/task/page",
                "GET",
                "/admin-api/oa/collect/task/page?pageNo=1&pageSize=1",
                lambda d: isinstance(d, dict) and "list" in d,
            ),
        ],
        "search_note": "TableSearch: 任务名 / 平台 / 采集方式 / 频率 / 状态",
    },
    {
        "title": "数据质量",
        "menu_csv": "数据质量",
        "hash": "#/ops/collect/quality",
        "football_path": "/ops/collect/quality",
        "component": "ops/collect/quality",
        "apis": [
            (
                "collect/quality/list",
                "GET",
                "/admin-api/oa/collect/quality/list?pageNo=1&pageSize=1",
                lambda d: isinstance(d, dict) and "list" in d,
            ),
        ],
        "search_note": "TableSearch: 规则名 / 检查类型 / 级别；右侧质量日志筛选",
    },
    {
        "title": "内部采集配置",
        "menu_csv": "内部采集配置",
        "hash": "#/ops/config-internal-collect",
        "football_path": "/ops/config-internal-collect",
        "component": "ops/config/InternalCollectConfig",
        "apis": [
            (
                "internal/wework/list",
                "GET",
                "/admin-api/oa/internal/wework/list?pageNo=1&pageSize=1",
                lambda d: isinstance(d, dict) and "list" in d,
            ),
        ],
        "search_note": "默认企微 Tab → WeworkAppConfigPanel；个微 Tab → 奥创配置",
    },
    {
        "title": "外部采集配置",
        "menu_csv": "外部采集配置",
        "hash": "#/ops/config-external-collect",
        "football_path": "/ops/config-external-collect",
        "component": "ops/config/ExternalCollectConfig",
        "apis": [
            (
                "external-collect/list",
                "GET",
                "/admin-api/oa/config/external-collect/list?pageNo=1&pageSize=10&subType=account",
                lambda d: isinstance(d, dict) and "list" in d,
            ),
        ],
        "search_note": "Tab: 外部账号 TableSearch / 关键词配置 TableSearch",
    },
]

SECTION_PAGES = {
    "priority": PAGES,
    "collect": COLLECT_PAGES,
}


def run_section(section: str, pages: list[dict]) -> tuple[list[dict], int]:
    token = login()
    if not token:
        print("LOGIN FAIL")
        return [], 0
    print(f"[{section}] LOGIN OK (token len={len(token)})")
    headers = {
        "Authorization": f"Bearer {token}",
        "X-Tenant-Id": "1",
        "tenant-id": "1",
    }

    results = []
    for p in pages:
        comp = p["component"]
        route_ok, route_issues = check_route(comp)
        vite_ok, vite_issues = check_vite(comp)
        theme_ok, theme_issues = check_theme(comp)
        api_results = [
            probe_api(a[0], a[1], a[2], headers, a[3] if len(a) > 3 else None) for a in p["apis"]
        ]
        api_ok = all(a["ok"] for a in api_results)
        issues = (
            route_issues
            + vite_issues
            + theme_issues
            + [f"{a['name']}: {a['data']}" for a in api_results if not a["ok"]]
        )
        pass_all = route_ok and vite_ok and theme_ok and api_ok
        row = {
            "page": p["title"],
            "hash": p["hash"],
            "football_path": p["football_path"],
            "component": comp,
            "route": route_ok,
            "vite": vite_ok,
            "theme": theme_ok,
            "api_ok": api_ok,
            "apis": api_results,
            "search_note": p["search_note"],
            "issues": issues,
            "pass": pass_all,
        }
        results.append(row)
        status = "PASS" if pass_all else "FAIL"
        print(f"\n[{status}] {p['title']} ({p['hash']})")
        print(f"  route={route_ok} vite={vite_ok} theme={theme_ok} api={api_ok}")
        for a in api_results:
            flag = "OK" if a["ok"] else "FAIL"
            print(f"  API {a['name']}: code={a['code']} {a['data']} -> {flag}")
        for issue in issues:
            print(f"  issue: {issue}")

    out_json = OUT_JSON_BY_SECTION[section]
    out_json.write_text(
        json.dumps({"section": section, "login": "ok", "pages": results}, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    passed = sum(1 for r in results if r["pass"])
    print(f"\n[{section}] SUMMARY: {passed}/{len(results)} PASS")
    print(f"JSON: {out_json.relative_to(ROOT)}")
    return results, passed


def main() -> int:
    parser = argparse.ArgumentParser(description="UAT spot-check for Ops pages")
    parser.add_argument(
        "--section",
        choices=sorted(SECTION_PAGES),
        default="priority",
        help="page batch to probe (default: priority)",
    )
    args = parser.parse_args()
    _, passed = run_section(args.section, SECTION_PAGES[args.section])
    total = len(SECTION_PAGES[args.section])
    return 0 if passed == total else 1


if __name__ == "__main__":
    sys.exit(main())
