#!/usr/bin/env python3
"""
Per-menu Ops page acceptance: route + Vite compile + API + theme.

Reads docs/delivery/oa-menu-permission-map.csv; for each visible menu row
(hide_in_menu != Y, excluded_m9 != Y) runs:
  1. route  — component .vue exists under football-front
  2. vite   — dev server returns 200 for the SFC module (no compile error)
  3. api    — primary list/data endpoint returns code=0 via Gateway
  4. theme  — root has ops-page; no hardcoded light-only backgrounds

Usage:
  python scripts/verify-ops-pages-per-menu.py
  python scripts/verify-ops-pages-per-menu.py --modules 运营管理,配置管理
  python scripts/verify-ops-pages-per-menu.py --api --json report.json
  python scripts/verify-ops-pages-per-menu.py --base http://localhost:48080 --vite http://localhost:5777
"""
from __future__ import annotations

import argparse
import csv
import json
import re
import sys
import urllib.error
import urllib.request
from dataclasses import asdict, dataclass, field
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CSV_PATH = ROOT / "docs/delivery/oa-menu-permission-map.csv"
FF = ROOT / "football-front/apps/web-ele/src"

# Primary data endpoint per football_path (route slug)
API_PROBE: dict[str, tuple[str, str]] = {
    # 作品监测
    "/ops/external-account": ("GET", "/admin-api/oa/monitor/external/list?pageNo=1&pageSize=1"),
    "/ops/high-fans-account": ("GET", "/admin-api/oa/monitor/high-follower/list?pageNo=1&pageSize=1"),
    "/ops/hot-works": ("GET", "/admin-api/oa/monitor/hit/list?pageNo=1&pageSize=1"),
    "/ops/ip-theme": ("GET", "/admin-api/oa/monitor/ip-theme/1"),
    "/ops/low-fans-account": ("GET", "/admin-api/oa/monitor/low-follower/list?pageNo=1&pageSize=1"),
    "/ops/low-score": ("GET", "/admin-api/oa/monitor/low-score/list?pageNo=1&pageSize=1"),
    # 内容生产
    "/ops/content": ("GET", "/admin-api/oa/content/list?pageNo=1&pageSize=1"),
    "/ops/content/review": ("GET", "/admin-api/oa/content/list?pageNo=1&pageSize=1"),
    "/ops/knowledge": ("GET", "/admin-api/oa/knowledge/list?pageNo=1&pageSize=1"),
    "/ops/layout-template": ("GET", "/admin-api/oa/layout-template/list?pageNo=1&pageSize=1"),
    "/ops/plan": ("GET", "/admin-api/oa/plan/list?pageNo=1&pageSize=1"),
    "/ops/sop": ("GET", "/admin-api/oa/sop/template/list?pageNo=1&pageSize=1"),
    "/ops/sop/review": ("GET", "/admin-api/oa/sop/review/pending"),
    "/ops/task": ("GET", "/admin-api/oa/task/list?pageNo=1&pageSize=1"),
    # 数据分析
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
    # 数据采集
    "/ops/collect/log": ("GET", "/admin-api/oa/collect/log/page?pageNo=1&pageSize=10"),
    "/ops/collect/private-domain-bridge": (
        "GET",
        "/admin-api/oa/collect/private-domain-bridge/page?pageNo=1&pageSize=10",
    ),
    "/ops/collect/quality": ("GET", "/admin-api/oa/collect/quality/list?pageNo=1&pageSize=10"),
    "/ops/collect/task": ("GET", "/admin-api/oa/collect/task/list?pageNo=1&pageSize=10"),
    # 系统管理(OA)
    "/ops/system-dict": ("GET", "/admin-api/oa/system/dict/list?pageNo=1&pageSize=10"),
    "/ops/system-log/login": ("GET", "/admin-api/oa/system/log/login?pageNo=1&pageSize=10"),
    "/ops/system-log/operation": ("GET", "/admin-api/oa/system/log/operation?pageNo=1&pageSize=10"),
    "/ops/system-message": ("GET", "/admin-api/oa/system/message/list?pageNo=1&pageSize=10"),
    "/ops/system-param": ("GET", "/admin-api/oa/system/param/list?pageNo=1&pageSize=10"),
    # 绩效核算
    "/ops/order-attribution": (
        "GET",
        "/admin-api/oa/football-order/list?startDate=2026-01-01&endDate=2026-06-30&pageNum=1&pageSize=1",
    ),
    "/ops/perf-execution": ("GET", "/admin-api/oa/perf/record/list?pageNo=1&pageSize=1"),
    "/ops/perf-result": ("GET", "/admin-api/oa/perf/result/list?pageNo=1&pageSize=1"),
    "/ops/perf-template": ("GET", "/admin-api/oa/perf/template/list?pageNo=1&pageSize=1"),
    # 财务管理
    "/ops/account-cost": ("GET", "/admin-api/oa/finance/cost/list?pageNo=1&pageSize=1"),
    "/ops/roi-analysis": (
        "GET",
        "/admin-api/oa/finance/roi/analysis?startDate=2026-01-01&endDate=2026-06-30",
    ),
    # 账号管理
    "/ops/company": ("GET", "/admin-api/oa/company/list?pageNo=1&pageSize=1"),
    "/ops/internal-account": ("GET", "/admin-api/oa/account/list?pageNo=1&pageSize=1"),
    "/ops/personal-account": ("GET", "/admin-api/oa/internal/personal-account/list?pageNo=1&pageSize=1"),
    "/ops/phone": ("GET", "/admin-api/oa/phone/list?pageNo=1&pageSize=1"),
    "/ops/realname": ("GET", "/admin-api/oa/realname/list?pageNo=1&pageSize=1"),
    "/ops/simcard": ("GET", "/admin-api/oa/sim-card/list?pageNo=1&pageSize=1"),
    # 运营管理
    "/ops/account-analysis": ("GET", "/admin-api/oa/account-analysis/list?pageNo=1&pageSize=1"),
    "/ops/author": ("GET", "/admin-api/oa/author/list?pageNo=1&pageSize=1"),
    "/ops/efficiency": ("GET", "/admin-api/oa/productivity-review/list?pageNo=1&pageSize=1"),
    "/ops/fans-analysis": ("GET", "/admin-api/oa/follower-analysis/list?page=1&size=1"),
    "/ops/internal-content": ("GET", "/admin-api/oa/internal-content/list?pageNo=1&pageSize=1"),
    "/ops/ip-group": ("GET", "/admin-api/oa/ip-group/tree"),
    # 配置管理
    "/ops/config-ai-model": ("GET", "/admin-api/oa/config/ai-model/list?pageNo=1&pageSize=1"),
    "/ops/config-ai-prompt": ("GET", "/admin-api/oa/config/ai-prompt/list?pageNo=1&pageSize=1"),
    "/ops/config-external-collect": ("GET", "/admin-api/oa/config/external-collect/list?pageNo=1&pageSize=1"),
    "/ops/config-external-data": ("GET", "/admin-api/oa/config/external-source/list?pageNo=1&pageSize=1"),
    "/ops/config-internal-collect": ("GET", "/admin-api/oa/config/internal-collect/list?pageNo=1&pageSize=1"),
    "/ops/config-metadata": ("GET", "/admin-api/oa/metadata/list?pageNo=1&pageSize=1"),
    "/ops/config-order-collect": ("GET", "/admin-api/oa/config/order-collect/list?pageNo=1&pageSize=1"),
    "/ops/config-threshold": ("GET", "/admin-api/oa/config/threshold/list?pageNo=1&pageSize=1"),
    # 首页
    "/ops/dashboard": ("GET", "/admin-api/oa/dashboard/home/trend?type=CONTENT"),
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


@dataclass
class RowResult:
    route_path: str
    menu_title: str
    parent_group: str
    football_path: str
    football_component: str
    route: bool = False
    vite: bool = False
    api: bool | None = None
    theme: bool = False
    issues: list[str] = field(default_factory=list)

    @property
    def pass_all(self) -> bool:
        if not (self.route and self.vite and self.theme):
            return False
        if self.api is not None and not self.api:
            return False
        return True


def load_rows(modules: set[str] | None) -> list[dict[str, str]]:
    with CSV_PATH.open(encoding="utf-8") as f:
        rows = list(csv.DictReader(f))
    out: list[dict[str, str]] = []
    for row in rows:
        if row.get("hide_in_menu", "").upper() == "Y":
            continue
        if row.get("excluded_m9", "").upper() == "Y":
            continue
        if modules and row.get("parent_group", "").strip() not in modules:
            continue
        if row.get("football_component", "").strip():
            out.append(row)
    return out


def check_route(comp: str) -> tuple[bool, list[str]]:
    vue = FF / "views" / f"{comp}.vue"
    if not vue.exists():
        return False, [f"missing component: views/{comp}.vue"]
    return True, []


def check_vite(vite_base: str, comp: str) -> tuple[bool, list[str]]:
    url = f"{vite_base.rstrip('/')}/src/views/{comp}.vue"
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


def http_json(method: str, url: str, headers: dict[str, str], body: dict | None = None) -> dict:
    data = json.dumps(body).encode("utf-8") if body is not None else None
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    if body is not None:
        req.add_header("Content-Type", "application/json")
    with urllib.request.urlopen(req, timeout=30) as resp:
        return json.loads(resp.read().decode("utf-8"))


def login(base: str, tenant_id: str = "1") -> str | None:
    url = f"{base.rstrip('/')}/admin-api/system/auth/login"
    headers = {"tenant-id": tenant_id, "Content-Type": "application/json"}
    body = {"username": "admin", "password": "admin123", "captchaVerification": ""}
    try:
        r = http_json("POST", url, headers, body)
        if r.get("code") == 0 and r.get("data", {}).get("accessToken"):
            return r["data"]["accessToken"]
    except Exception:
        pass
    return None


def check_api(base: str, token: str, football_path: str, tenant_id: str = "1") -> tuple[bool | None, list[str]]:
    probe = API_PROBE.get(football_path)
    if not probe:
        return None, ["no API probe mapped"]
    method, path = probe
    headers = {
        "Authorization": f"Bearer {token}",
        "X-Tenant-Id": tenant_id,
        "tenant-id": tenant_id,
    }
    url = f"{base.rstrip('/')}{path}"
    try:
        r = http_json(method, url, headers)
        if r.get("code") == 0:
            return True, []
        return False, [f"code={r.get('code')} msg={r.get('msg', '')[:80]}"]
    except Exception as ex:
        return False, [str(ex)]


def run(
    modules: set[str] | None,
    *,
    do_api: bool,
    gateway_base: str,
    vite_base: str,
) -> list[RowResult]:
    rows = load_rows(modules)
    token: str | None = None
    if do_api:
        token = login(gateway_base)

    results: list[RowResult] = []
    for row in rows:
        comp = row["football_component"].strip()
        fp = row.get("football_path", "").strip()
        rr = RowResult(
            route_path=row.get("route_path", ""),
            menu_title=row.get("menu_title", ""),
            parent_group=row.get("parent_group", ""),
            football_path=fp,
            football_component=comp,
        )
        ok, iss = check_route(comp)
        rr.route = ok
        rr.issues.extend(iss)

        ok, iss = check_vite(vite_base, comp)
        rr.vite = ok
        rr.issues.extend(iss)

        ok, iss = check_theme(comp)
        rr.theme = ok
        rr.issues.extend(iss)

        if do_api:
            if not token:
                rr.api = False
                rr.issues.append("login failed")
            else:
                ok, iss = check_api(gateway_base, token, fp)
                rr.api = ok if ok is not None else None
                rr.issues.extend(iss)

        results.append(rr)
    return results


def print_report(results: list[RowResult], do_api: bool) -> None:
    by_module: dict[str, list[RowResult]] = {}
    for r in results:
        by_module.setdefault(r.parent_group, []).append(r)

    print("=== Ops per-menu acceptance ===")
    for module, items in sorted(by_module.items()):
        passed = sum(1 for i in items if i.pass_all)
        print(f"\n[{module}] {passed}/{len(items)} pass")
        for r in items:
            flags = [
                f"route={'Y' if r.route else 'N'}",
                f"vite={'Y' if r.vite else 'N'}",
                f"theme={'Y' if r.theme else 'N'}",
            ]
            if do_api:
                api_flag = "Y" if r.api else ("N" if r.api is False else "-")
                flags.append(f"api={api_flag}")
            status = "PASS" if r.pass_all else "FAIL"
            print(f"  [{status}] {r.menu_title} ({r.football_path}) — {', '.join(flags)}")
            if r.issues:
                for issue in r.issues:
                    print(f"         · {issue}")

    total_pass = sum(1 for r in results if r.pass_all)
    print(f"\n=== Summary: {total_pass}/{len(results)} pass ===")


def main() -> int:
    parser = argparse.ArgumentParser(description="Per-menu Ops page acceptance")
    parser.add_argument(
        "--modules",
        default="",
        help="Comma-separated parent_group filter (e.g. 运营管理,配置管理)",
    )
    parser.add_argument("--api", action="store_true", help="Probe Gateway APIs")
    parser.add_argument("--base", default="http://localhost:48080", help="Gateway base URL")
    parser.add_argument("--vite", default="http://localhost:5777", help="Vite dev server URL")
    parser.add_argument("--json", default="", help="Write JSON report to path")
    args = parser.parse_args()

    modules: set[str] | None = None
    if args.modules.strip():
        modules = {m.strip() for m in args.modules.split(",") if m.strip()}

    results = run(modules, do_api=args.api, gateway_base=args.base, vite_base=args.vite)
    print_report(results, args.api)

    if args.json:
        out = Path(args.json)
        payload = {
            "summary": {
                "total": len(results),
                "pass": sum(1 for r in results if r.pass_all),
                "fail": sum(1 for r in results if not r.pass_all),
            },
            "rows": [asdict(r) for r in results],
        }
        out.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
        try:
            print(f"JSON report: {out.resolve().relative_to(ROOT.resolve())}")
        except ValueError:
            print(f"JSON report: {out.resolve()}")

    fail_count = sum(1 for r in results if not r.pass_all)
    return 0 if fail_count == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
