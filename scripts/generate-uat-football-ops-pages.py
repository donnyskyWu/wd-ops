#!/usr/bin/env python3
"""Generate tests/fixtures/uat-football-ops-pages.ts from oa-menu-permission-map.csv."""
from __future__ import annotations

import csv
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CSV_PATH = ROOT / "docs/delivery/oa-menu-permission-map.csv"
OUT = ROOT / "football-front/apps/web-ele/tests/fixtures/uat-football-ops-pages.ts"

# Primary API path prefix per football_path (aligned with verify-ops-pages-per-menu.py)
API_PROBE: dict[str, str] = {
    "/ops/external-account": "/admin-api/oa/monitor/external/list",
    "/ops/high-fans-account": "/admin-api/oa/monitor/high-follower/list",
    "/ops/hot-works": "/admin-api/oa/monitor/hit/list",
    "/ops/ip-theme": "/admin-api/oa/monitor/ip-theme/",
    "/ops/low-fans-account": "/admin-api/oa/monitor/low-follower/list",
    "/ops/low-score": "/admin-api/oa/monitor/low-score/list",
    "/ops/content": "/admin-api/oa/content/list",
    "/ops/content/review": "/admin-api/oa/content/list",
    "/ops/knowledge": "/admin-api/oa/knowledge/list",
    "/ops/layout-template": "/admin-api/oa/layout-template/list",
    "/ops/plan": "/admin-api/oa/plan/list",
    "/ops/sop": "/admin-api/oa/sop/template/list",
    "/ops/sop/review": "/admin-api/oa/sop/review/pending",
    "/ops/task": "/admin-api/oa/task/list",
    "/ops/custom-query": "/admin-api/oa/query/list",
    "/ops/data-report": "/admin-api/oa/report/unified-account/stats",
    "/ops/financial-analysis": "/admin-api/oa/finance/roi/analysis",
    "/ops/funnel-analysis": "/admin-api/oa/funnel/list",
    "/ops/metric": "/admin-api/oa/metric/list",
    "/ops/metric-analysis": "/admin-api/oa/metric/list",
    "/ops/screen": "/admin-api/oa/dashboard-config/list",
    "/ops/screen-config": "/admin-api/oa/dashboard-config/list",
    "/ops/collect/log": "/admin-api/oa/collect/log/page",
    "/ops/collect/private-domain-bridge": "/admin-api/oa/collect/private-domain-bridge/page",
    "/ops/collect/quality": "/admin-api/oa/collect/quality/list",
    "/ops/collect/task": "/admin-api/oa/collect/task/list",
    "/ops/system-dict": "/admin-api/oa/system/dict/list",
    "/ops/system-log/login": "/admin-api/oa/system/log/login",
    "/ops/system-log/operation": "/admin-api/oa/system/log/operation",
    "/ops/system-message": "/admin-api/oa/system/message/list",
    "/ops/system-param": "/admin-api/oa/system/param/list",
    "/ops/order-attribution": "/admin-api/oa/football-order/list",
    "/ops/perf-execution": "/admin-api/oa/perf/record/list",
    "/ops/perf-result": "/admin-api/oa/perf/result/list",
    "/ops/perf-template": "/admin-api/oa/perf/template/list",
    "/ops/account-cost": "/admin-api/oa/finance/cost/list",
    "/ops/roi-analysis": "/admin-api/oa/finance/roi/analysis",
    "/ops/company": "/admin-api/oa/company/list",
    "/ops/internal-account": "/admin-api/oa/account/list",
    "/ops/personal-account": "/admin-api/oa/internal/personal-account/list",
    "/ops/phone": "/admin-api/oa/phone/list",
    "/ops/realname": "/admin-api/oa/realname/list",
    "/ops/simcard": "/admin-api/oa/sim-card/list",
    "/ops/account-analysis": "/admin-api/oa/account-analysis/list",
    "/ops/efficiency": "/admin-api/oa/productivity-review/list",
    "/ops/fans-analysis": "/admin-api/oa/follower-analysis/list",
    "/ops/internal-content": "/admin-api/oa/internal-content/list",
    "/ops/ip-group": "/admin-api/oa/ip-group/tree",
    "/ops/config-ai-model": "/admin-api/oa/config/ai-model/list",
    "/ops/config-ai-prompt": "/admin-api/oa/config/ai-prompt/list",
    "/ops/config-external-collect": "/admin-api/oa/config/external-collect/list",
    "/ops/config-external-data": "/admin-api/oa/config/external-source/list",
    "/ops/config-internal-collect": "/admin-api/oa/config/internal-collect/list",
    "/ops/config-metadata": "/admin-api/oa/metadata/list",
    "/ops/config-order-collect": "/admin-api/oa/config/order-collect/list",
    "/ops/config-threshold": "/admin-api/oa/config/threshold/list",
    "/ops/dashboard": "/admin-api/oa/dashboard/home/trend",
}


def ts_str(s: str) -> str:
    return s.replace("\\", "\\\\").replace("'", "\\'")


def main() -> int:
    pages: list[tuple[str, str, str, str]] = []
    with CSV_PATH.open(encoding="utf-8") as f:
        for row in csv.DictReader(f):
            if row.get("hide_in_menu", "").upper() == "Y":
                continue
            if row.get("excluded_m9", "").upper() == "Y":
                continue
            if not row.get("football_component", "").strip():
                continue
            fp = row["football_path"].strip()
            pages.append(
                (
                    row["parent_group"].strip(),
                    fp,
                    row["menu_title"].strip(),
                    API_PROBE.get(fp, ""),
                )
            )

    lines = [
        "/**",
        " * All visible Ops menu routes from docs/delivery/oa-menu-permission-map.csv",
        " * Regenerate: python scripts/generate-uat-football-ops-pages.py",
        " */",
        "",
        "export interface UatFootballPage {",
        "  group: string;",
        "  hash: string;",
        "  title: string;",
        "  apiPattern?: RegExp;",
        "}",
        "",
        "export const UAT_FOOTBALL_OPS_PAGES: UatFootballPage[] = [",
    ]
    for group, fp, title, api in pages:
        lines.append("  {")
        lines.append(f"    group: '{ts_str(group)}',")
        lines.append(f"    hash: '{ts_str(fp)}',")
        lines.append(f"    title: '{ts_str(title)}',")
        if api:
            pat = api.replace("/", r"\/")
            lines.append(f"    apiPattern: /{pat}/,")
        lines.append("  },")
    lines.append("];")
    lines.append("")

    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text("\n".join(lines), encoding="utf-8")
    print(f"Wrote {len(pages)} pages to {OUT.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
