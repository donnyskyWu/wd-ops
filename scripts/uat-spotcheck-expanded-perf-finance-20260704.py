#!/usr/bin/env python3
"""UAT spot-check — 绩效核算 + 财务管理 (6 pages, 2026-07-04)."""
from __future__ import annotations

import json
import re
import sys
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
FF = ROOT / "football-front/apps/web-ele/src"
GATEWAY = "http://localhost:48080"
SYSTEM = "http://localhost:48081"
OA = "http://localhost:48094"
VITE = "http://localhost:5777"
OUT_JSON = ROOT / "docs/delivery/uat-spotcheck-expanded-perf-finance-20260704-probe.json"

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


def login(tenant_id: str = "1") -> tuple[str | None, str]:
    headers = {"tenant-id": tenant_id, "Content-Type": "application/json"}
    body = {"username": "admin", "password": "admin123", "captchaVerification": ""}
    for label, base in (("gateway", GATEWAY), ("system-server", SYSTEM)):
        url = f"{base.rstrip('/')}/admin-api/system/auth/login"
        try:
            r = http_json("POST", url, headers, body)
            if r.get("code") == 0 and r.get("data", {}).get("accessToken"):
                return r["data"]["accessToken"], label
        except Exception:
            pass
    return None, ""


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
    last_err = ""
    r = None
    via = "none"
    for base in (GATEWAY, OA):
        url = base.rstrip("/") + path
        try:
            candidate = http_json(method, url, headers)
            if candidate.get("code") == 0:
                r = candidate
                via = "gateway" if base == GATEWAY else "oa-server"
                break
            last_err = f"code={candidate.get('code')} msg={str(candidate.get('msg', ''))[:80]}"
        except Exception as ex:
            last_err = str(ex)[:120]
    if r is None:
        return {
            "name": name,
            "path": path,
            "ok": False,
            "code": None,
            "data": last_err,
            "api_ok": False,
            "sensible": False,
            "via": "none",
        }
    try:
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
        via = via
        return {
            "name": name,
            "path": path,
            "ok": ok and sensible,
            "code": code,
            "data": summary,
            "api_ok": ok,
            "sensible": sensible,
            "via": via if ok else via,
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
            "via": "error",
        }


PAGES = [
    {
        "title": "订单归因",
        "hash": "#/ops/order-attribution",
        "football_path": "/ops/order-attribution",
        "component": "ops/performance/OrderAttribution",
        "apis": [
            (
                "football-order/list",
                "GET",
                "/admin-api/oa/football-order/list?startDate=2026-01-01&endDate=2026-06-30&pageNum=1&pageSize=1",
                lambda d: isinstance(d, dict) and "list" in d,
            ),
        ],
        "search_note": "TableSearch: 日期范围 / IP组；列表 P2b → football-order/list（pay_all_order 只读）",
    },
    {
        "title": "考核执行",
        "hash": "#/ops/perf-execution",
        "football_path": "/ops/perf-execution",
        "component": "ops/performance/PerfExecution",
        "apis": [
            (
                "perf/record/list",
                "GET",
                "/admin-api/oa/perf/record/list?pageNo=1&pageSize=1",
                lambda d: isinstance(d, dict) and "list" in d,
            ),
        ],
        "search_note": "TableSearch: 考核周期 / 状态 / 模板",
    },
    {
        "title": "考核模板",
        "hash": "#/ops/perf-template",
        "football_path": "/ops/perf-template",
        "component": "ops/performance/PerfTemplate",
        "apis": [
            (
                "perf/template/list",
                "GET",
                "/admin-api/oa/perf/template/list?pageNo=1&pageSize=1",
                lambda d: isinstance(d, dict) and "list" in d,
            ),
        ],
        "search_note": "TableSearch: 模板名称 / 岗位 / 状态",
    },
    {
        "title": "绩效结果",
        "hash": "#/ops/perf-result",
        "football_path": "/ops/perf-result",
        "component": "ops/performance/PerfResult",
        "apis": [
            (
                "perf/result/list",
                "GET",
                "/admin-api/oa/perf/result/list?pageNo=1&pageSize=1",
                lambda d: isinstance(d, dict) and "list" in d,
            ),
        ],
        "search_note": "TableSearch: 考核周期 / 等级 / 经办人",
    },
    {
        "title": "账号成本",
        "hash": "#/ops/account-cost",
        "football_path": "/ops/account-cost",
        "component": "ops/finance/AccountCostManage",
        "apis": [
            (
                "finance/cost/list",
                "GET",
                "/admin-api/oa/finance/cost/list?pageNo=1&pageSize=1",
                lambda d: isinstance(d, dict) and "list" in d,
            ),
        ],
        "search_note": "平台 Tab + TableSearch: 账号 / 成本周期",
    },
    {
        "title": "ROI分析",
        "hash": "#/ops/roi-analysis",
        "football_path": "/ops/roi-analysis",
        "component": "ops/finance/RoiAnalysis",
        "apis": [
            (
                "finance/roi/analysis",
                "GET",
                "/admin-api/oa/finance/roi/analysis?startDate=2026-01-01&endDate=2026-06-30",
                lambda d: isinstance(d, dict),
            ),
        ],
        "search_note": "日期范围筛选 + KPI 卡 / 趋势 / 维度分解",
    },
]


def main() -> int:
    token, login_via = login()
    if not token:
        print("LOGIN FAIL")
        return 1
    print(f"LOGIN OK via {login_via} (token len={len(token)})")
    headers = {
        "Authorization": f"Bearer {token}",
        "X-Tenant-Id": "1",
        "tenant-id": "1",
    }

    results = []
    for p in PAGES:
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

    OUT_JSON.write_text(
        json.dumps(
            {"login": "ok", "login_via": login_via, "module": "绩效核算+财务管理", "pages": results},
            ensure_ascii=False,
            indent=2,
        ),
        encoding="utf-8",
    )
    passed = sum(1 for r in results if r["pass"])
    print(f"\nSUMMARY: {passed}/{len(results)} PASS")
    print(f"JSON: {OUT_JSON.relative_to(ROOT)}")
    return 0 if passed == len(results) else 1


if __name__ == "__main__":
    sys.exit(main())
