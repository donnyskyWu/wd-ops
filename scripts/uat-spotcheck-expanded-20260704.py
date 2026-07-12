#!/usr/bin/env python3
"""Expanded UAT spot-check: 数据分析 + 作品监测 + 配置管理 (2026-07-04)."""
from __future__ import annotations

import csv
import json
import re
import sys
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
FF = ROOT / "football-front/apps/web-ele/src"
CSV_PATH = ROOT / "docs/delivery/oa-menu-permission-map.csv"
GATEWAY = "http://localhost:48080"
SYSTEM = "http://localhost:48081"
OA = "http://localhost:48094"
VITE = "http://localhost:5777"
OUT_JSON = ROOT / "docs/delivery/uat-spotcheck-expanded-20260704-probe.json"

MODULES = ("数据分析", "作品监测", "配置管理")

THEME_BG_PATTERNS = [
    re.compile(r"background(?:-color)?\s*:\s*#fff\b", re.I),
    re.compile(r"background(?:-color)?\s*:\s*#ffffff\b", re.I),
    re.compile(r"background(?:-color)?\s*:\s*white\b", re.I),
    re.compile(r"background(?:-color)?\s*:\s*#f5f7fa\b", re.I),
    re.compile(r"background(?:-color)?\s*:\s*#fafafa\b", re.I),
]
THEME_ANT_PRIMARY = re.compile(r"#1890ff|#40a9ff", re.I)
BARE_AT_IMPORT = re.compile(r"""from\s+['"]@/""")

API_PROBE: dict[str, tuple[str, str]] = {
    "/ops/external-account": ("GET", "/admin-api/oa/monitor/external/list?pageNo=1&pageSize=1"),
    "/ops/high-fans-account": ("GET", "/admin-api/oa/monitor/high-follower/list?pageNo=1&pageSize=1"),
    "/ops/hot-works": ("GET", "/admin-api/oa/monitor/hit/list?pageNo=1&pageSize=1"),
    "/ops/ip-theme": ("GET", "/admin-api/oa/monitor/ip-theme/1"),
    "/ops/low-fans-account": ("GET", "/admin-api/oa/monitor/low-follower/list?pageNo=1&pageSize=1"),
    "/ops/low-score": ("GET", "/admin-api/oa/monitor/low-score/list?pageNo=1&pageSize=1"),
    "/ops/custom-query": ("GET", "/admin-api/oa/query/list?pageNo=1&pageSize=10"),
    "/ops/data-report": ("GET", "/admin-api/oa/report/unified-account/stats"),
    "/ops/financial-analysis": (
        "GET",
        "/admin-api/oa/finance/roi/analysis?startDate=2026-01-01&endDate=2026-06-30",
    ),
    "/ops/funnel-analysis": ("GET", "/admin-api/oa/funnel/list?pageNo=1&pageSize=10"),
    "/ops/metric": ("GET", "/admin-api/oa/metric/list?pageNo=1&pageSize=10"),
    "/ops/metric-analysis": ("GET", "/admin-api/oa/metric/list?pageNo=1&pageSize=10"),
    "/ops/screen": ("GET", "/admin-api/oa/dashboard-config/list?pageNum=1&pageSize=10"),
    "/ops/screen-config": ("GET", "/admin-api/oa/dashboard-config/list?pageNum=1&pageSize=10"),
    "/ops/config-ai-model": ("GET", "/admin-api/oa/config/ai-model/list?pageNo=1&pageSize=1"),
    "/ops/config-ai-prompt": ("GET", "/admin-api/oa/config/ai-prompt/list?pageNo=1&pageSize=1"),
    "/ops/config-external-collect": ("GET", "/admin-api/oa/config/external-collect/list?pageNo=1&pageSize=1"),
    "/ops/config-external-data": ("GET", "/admin-api/oa/config/external-source/list?pageNo=1&pageSize=1"),
    "/ops/config-internal-collect": ("GET", "/admin-api/oa/config/internal-collect/list?pageNo=1&pageSize=1"),
    "/ops/config-metadata": ("GET", "/admin-api/oa/metadata/list?pageNo=1&pageSize=1"),
    "/ops/config-order-collect": ("GET", "/admin-api/oa/config/order-collect/list?pageNo=1&pageSize=1"),
    "/ops/config-threshold": ("GET", "/admin-api/oa/config/threshold/list?pageNo=1&pageSize=1"),
}

SEARCH_NOTES: dict[str, str] = {
    "/ops/custom-query": "TableSearch: 查询名称 / 状态",
    "/ops/data-report": "报表卡片 + 子报表入口",
    "/ops/financial-analysis": "日期范围 + ROI 维度筛选",
    "/ops/funnel-analysis": "漏斗阶段筛选 + 日期",
    "/ops/metric": "TableSearch: 指标名称 / 分类",
    "/ops/metric-analysis": "指标选择 + 趋势图",
    "/ops/screen": "全屏大屏（无 TableSearch）",
    "/ops/screen-config": "TableSearch: 大屏名称",
    "/ops/external-account": "TableSearch: 账号 / 平台",
    "/ops/high-fans-account": "TableSearch: 粉丝阈值 / 平台",
    "/ops/hot-works": "TableSearch: 作品 / 平台",
    "/ops/ip-theme": "IP 主题 Tab + 数据表",
    "/ops/low-fans-account": "TableSearch: 低粉阈值",
    "/ops/low-score": "TableSearch: 评分阈值",
    "/ops/config-ai-model": "TableSearch: 模型名称",
    "/ops/config-ai-prompt": "TableSearch: 提示词名称",
    "/ops/config-external-collect": "TableSearch: 配置名称",
    "/ops/config-external-data": "TableSearch: 数据源名称",
    "/ops/config-internal-collect": "TableSearch: 配置名称",
    "/ops/config-metadata": "TableSearch: 表名 / 字段",
    "/ops/config-order-collect": "TableSearch: 配置名称",
    "/ops/config-threshold": "TableSearch: 规则名称",
}


def http_json(method: str, url: str, headers: dict[str, str], body: dict | None = None) -> dict:
    data = json.dumps(body).encode("utf-8") if body is not None else None
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    if body is not None:
        req.add_header("Content-Type", "application/json")
    with urllib.request.urlopen(req, timeout=60) as resp:
        return json.loads(resp.read().decode("utf-8"))


def login(tenant_id: str = "1") -> tuple[str | None, str]:
    """Return (token, login_base). Prefer Gateway; fallback system-server when Nacos down."""
    headers = {"tenant-id": tenant_id, "Content-Type": "application/json"}
    body = {"username": "admin", "password": "admin123", "captchaVerification": ""}
    for base, label in ((GATEWAY, "gateway"), (SYSTEM, "system-server")):
        url = f"{base.rstrip('/')}/admin-api/system/auth/login"
        try:
            r = http_json("POST", url, headers, body)
            if r.get("code") == 0 and r.get("data", {}).get("accessToken"):
                return r["data"]["accessToken"], label
        except Exception:
            continue
    return None, ""


def api_base_for_login(login_via: str) -> str:
    if login_via == "gateway":
        return GATEWAY
    return OA


def load_pages() -> list[dict]:
    with CSV_PATH.open(encoding="utf-8") as f:
        rows = list(csv.DictReader(f))
    pages: list[dict] = []
    for row in rows:
        if row.get("hide_in_menu", "").upper() == "Y":
            continue
        if row.get("excluded_m9", "").upper() == "Y":
            continue
        group = row.get("parent_group", "").strip()
        if group not in MODULES:
            continue
        fp = row.get("football_path", "").strip()
        comp = row.get("football_component", "").strip()
        if not fp or not comp:
            continue
        probe = API_PROBE.get(fp)
        apis = []
        if probe:
            apis.append((fp.rsplit("/", 1)[-1], probe[0], probe[1]))
        pages.append(
            {
                "title": row["menu_title"].strip(),
                "group": group,
                "hash": f"#{fp}",
                "football_path": fp,
                "component": comp,
                "apis": apis,
                "search_note": SEARCH_NOTES.get(fp, "TableSearch / 筛选区"),
            }
        )
    return pages


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
    name: str, method: str, path: str, headers: dict[str, str], api_base: str
) -> dict:
    url = api_base + path
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
        return {"name": name, "path": path, "ok": ok, "code": code, "data": summary}
    except Exception as ex:
        return {"name": name, "path": path, "ok": False, "code": None, "data": str(ex)[:120]}


def main() -> int:
    token, login_via = login()
    if not token:
        print("LOGIN FAIL")
        return 1
    api_base = api_base_for_login(login_via)
    print(f"LOGIN OK via {login_via} (token len={len(token)}, api_base={api_base})")
    headers = {
        "Authorization": f"Bearer {token}",
        "X-Tenant-Id": "1",
        "tenant-id": "1",
    }

    pages = load_pages()
    results: list[dict] = []
    by_group: dict[str, list[dict]] = {m: [] for m in MODULES}

    for p in pages:
        comp = p["component"]
        route_ok, route_issues = check_route(comp)
        vite_ok, vite_issues = check_vite(comp)
        theme_ok, theme_issues = check_theme(comp)
        api_results = [probe_api(a[0], a[1], a[2], headers, api_base) for a in p["apis"]]
        api_ok = all(a["ok"] for a in api_results) if api_results else True
        issues = (
            route_issues
            + vite_issues
            + theme_issues
            + [f"{a['name']}: {a['data']}" for a in api_results if not a["ok"]]
        )
        pass_all = route_ok and vite_ok and theme_ok and api_ok
        row = {
            "page": p["title"],
            "group": p["group"],
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
        by_group[p["group"]].append(row)
        status = "PASS" if pass_all else "FAIL"
        print(f"\n[{status}] {p['title']} ({p['hash']})")
        print(f"  route={route_ok} vite={vite_ok} theme={theme_ok} api={api_ok}")
        for a in api_results:
            flag = "OK" if a["ok"] else "FAIL"
            print(f"  API {a['name']}: code={a['code']} {a['data']} -> {flag}")
        for issue in issues:
            print(f"  issue: {issue}")

    passed = sum(1 for r in results if r["pass"])
    summary = {
        "login": "ok",
        "login_via": login_via,
        "api_base": api_base,
        "modules": {g: sum(1 for r in rows if r["pass"]) for g, rows in by_group.items()},
        "total": len(results),
        "pass": passed,
        "pages": results,
    }
    OUT_JSON.write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")

    print("\n=== By module ===")
    for g in MODULES:
        rows = by_group[g]
        gp = sum(1 for r in rows if r["pass"])
        print(f"  {g}: {gp}/{len(rows)}")
    print(f"\nSUMMARY: {passed}/{len(results)} PASS")
    print(f"JSON: {OUT_JSON.relative_to(ROOT)}")
    return 0 if passed == len(results) else 1


if __name__ == "__main__":
    sys.exit(main())
