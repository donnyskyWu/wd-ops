#!/usr/bin/env python3
"""Generate UAT-BROWSER-E2E-20260704.md from probe JSON."""
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JSON_PATH = ROOT / "docs/delivery/uat-browser-e2e-20260704-probe.json"
MD_PATH = ROOT / "docs/delivery/UAT-BROWSER-E2E-20260704.md"


def main() -> int:
    if not JSON_PATH.exists():
        print(f"[warn] missing {JSON_PATH}")
        return 1

    data = json.loads(JSON_PATH.read_text(encoding="utf-8"))
    lines = [
        "# UAT Browser E2E — Ops Standalone (2026-07-04)",
        "",
        "> **Tool**: Playwright · **Stack**: `start-ops-standalone.ps1` → UI :3000 · API :8080 · Dev Token",
        "> **Script**: `scripts/run-uat-browser-e2e.ps1` · **RETIRED** (A-WP1) — use `run-gate-football-e2e.ps1` / `football-front/apps/web-ele/tests/`",
        "",
        "## Summary",
        "",
        "| Item | Value |",
        "|------|-------|",
        f"| Scope | {data.get('scope', '')} |",
        f"| Total | {data.get('total', 0)} |",
        f"| **PASS** | **{data.get('passed', 0)}/{data.get('total', 0)}** |",
        f"| Failed | {data.get('failed', 0)} |",
        f"| Generated | {data.get('generatedAt', '')} |",
        "",
        "## Pages",
        "",
        "| Group | Route | Title | Result | API | Notes |",
        "|-------|-------|-------|--------|-----|-------|",
    ]
    for p in data.get("pages", []):
        status = "PASS" if p.get("pass") else "FAIL"
        api = f"HTTP {p['apiStatus']} code={p.get('apiCode')}" if p.get("apiStatus") else "—"
        errs = p.get("errors") or []
        notes = "; ".join(errs)[:80] if errs else "—"
        lines.append(
            f"| {p.get('group', '')} | `{p.get('path', '')}` | {p.get('title', '')} | {status} | {api} | {notes} |"
        )
    lines += [
        "",
        "## Re-run",
        "",
        "```powershell",
        ".\\scripts\\start-ops-standalone.ps1",
        ".\\scripts\\run-uat-browser-e2e.ps1",
        "```",
        "",
        f"JSON: `{JSON_PATH.relative_to(ROOT).as_posix()}`",
    ]
    MD_PATH.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"[report] {MD_PATH}")
    print(f"[report] {JSON_PATH}")
    print(f"Result: {data.get('passed', 0)}/{data.get('total', 0)} PASS")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
