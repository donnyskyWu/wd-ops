# -*- coding: utf-8 -*-
"""Collect OPS engineering/process markdown for delivery zip."""
from __future__ import annotations

import re
from dataclasses import dataclass
from datetime import date
from pathlib import Path

# Resolved by importer: REPO = wd repo root
REPO: Path | None = None

ARC_PROCESS = "process-docs"

EXCLUDE_NAME_PREFIXES = ("_",)
EXCLUDE_NAMES = frozenset(
    {
        "SEED-AUTH-TOKENS.md",
        "application-local.yaml",
        "application-local.yml",
    }
)
EXCLUDE_PATH_PARTS = frozenset(
    {
        "e2e-artifacts",
        "_tmp_",
        ".codegraph",
        "BOOT-INF",
        "node_modules",
        "virtual-asset-hub",
    }
)
SENSITIVE_YAML_PATTERN = re.compile(r"application-(local|dev)\.(ya?ml)$", re.I)


@dataclass(frozen=True)
class ProcessEntry:
    source: Path
    arc_subdir: str  # under process-docs/, e.g. "api"


def _repo() -> Path:
    if REPO is None:
        raise RuntimeError("REPO not set; import and assign _pack_process_docs.REPO")
    return REPO


def _under_repo(path: Path) -> bool:
    try:
        path.resolve().relative_to(_repo().resolve())
        return True
    except ValueError:
        return False


def should_exclude(path: Path) -> bool:
    if not path.is_file():
        return True
    if path.suffix.lower() not in {".md", ".sql"}:
        return True
    if path.name in EXCLUDE_NAMES:
        return True
    if any(path.name.startswith(p) for p in EXCLUDE_NAME_PREFIXES):
        if path.suffix.lower() == ".md" and path.parent.name == "product-handover":
            pass  # allow README in handover only via other collector
        elif path.suffix.lower() == ".md":
            return True
    parts = set(path.parts)
    if parts & EXCLUDE_PATH_PARTS:
        return True
    if SENSITIVE_YAML_PATTERN.search(path.name):
        return True
    if path.suffix.lower() == ".sql" and path.name.endswith(".log"):
        return True
    return False


REPORT_NAME_MARKERS = ("报告", "REPORT", "SIGNOFF", "签收")
WALKTHROUGH_NAME_MARKERS = ("walkthrough", "走查", "UAT-")


def _delivery_rel_excluded(rel: Path) -> bool:
    return any(part in EXCLUDE_PATH_PARTS for part in rel.parts)


def _looks_like_test_report(name: str) -> bool:
    upper = name.upper()
    if any(m in name for m in REPORT_NAME_MARKERS):
        return True
    if upper.startswith("UAT-") or upper.startswith("TEST-"):
        return True
    if "WALKTHROUGH" in upper:
        return True
    for m in WALKTHROUGH_NAME_MARKERS:
        if m.upper() in upper or m in name:
            return True
    return False


def _collect_delivery_report_files(delivery: Path) -> list[Path]:
    """Gate reports, module reports, UAT/walkthrough markdown under docs/delivery (no e2e-artifacts)."""
    found: dict[Path, None] = {}
    tracker = delivery / "MASTER-EXECUTION-TRACKER.md"
    if tracker.is_file() and not should_exclude(tracker):
        found[tracker.resolve()] = None

    gates = delivery / "gates"
    if gates.is_dir():
        for p in sorted(gates.glob("*.md")):
            if p.is_file() and not should_exclude(p):
                found[p.resolve()] = None

    if delivery.is_dir():
        for p in sorted(delivery.rglob("*.md")):
            if not p.is_file() or should_exclude(p):
                continue
            try:
                rel = p.relative_to(delivery)
            except ValueError:
                continue
            if _delivery_rel_excluded(rel):
                continue
            if p.name.startswith("_"):
                continue
            if p.resolve() in found:
                continue
            if rel.parts[0] == "gates":
                continue
            if p.name == "MASTER-EXECUTION-TRACKER.md":
                continue
            if _looks_like_test_report(p.name):
                found[p.resolve()] = None

    return [Path(k) for k in found.keys()]


PLAYWRIGHT_TESTS_ROOTS = (
    "football-front/apps/web-ele/tests",
    "ops-platform-ui-vue/tests",
)
PLAYWRIGHT_SPEC_GLOBS = (
    "ops-*.spec.ts",
    "*ops*.spec.ts",
    "*uat*.spec.ts",
    "uat-football-ops*.spec.ts",
)
PLAYWRIGHT_ARC_PREFIX = f"{ARC_PROCESS}/test/automation/playwright"
# Minimal helpers referenced by ops-list-pagination.spec.ts (under tests/)
PLAYWRIGHT_HELPER_RELPATHS = (
    "helpers/football-auth.ts",
    "helpers/ops-list-pagination.ts",
)


def playwright_tests_bases(repo: Path) -> list[Path]:
    return [repo / Path(rel) for rel in PLAYWRIGHT_TESTS_ROOTS]


def discover_playwright_ops_specs(repo: Path) -> tuple[list[Path], str]:
    """Return (spec paths under repo, note for index)."""
    specs: list[Path] = []
    missing_roots: list[str] = []
    for base in playwright_tests_bases(repo):
        if not base.is_dir():
            missing_roots.append(f"{base.relative_to(repo).as_posix()}（未检出/不存在）")
            continue
        for pattern in PLAYWRIGHT_SPEC_GLOBS:
            for p in sorted(base.glob(pattern)):
                if p.is_file():
                    specs.append(p)
    seen: set[Path] = set()
    unique: list[Path] = []
    for p in specs:
        rp = p.resolve()
        if rp in seen:
            continue
        seen.add(rp)
        unique.append(p)
    if unique:
        note = (
            f"已在仓库内找到 {len(unique)} 个 Playwright spec；"
            f"源码副本位于 Zip 内 `{PLAYWRIGHT_ARC_PREFIX}/`（相对 `tests/` 目录结构）。"
        )
    else:
        note = (
            "未在本地检出 `football-front/apps/web-ele/tests` 或 `ops-platform-ui-vue/tests` 下的 ops/uat spec；"
            " E2E 以文档中引用的脚本路径为准，Zip 内无 `.ts` 副本。"
        )
        if missing_roots:
            note += " 缺失目录：" + "；".join(missing_roots) + "。"
    return unique, note


def arcname_playwright_under_tests(tests_root: Path, path_under_tests: Path) -> str:
    rel = path_under_tests.relative_to(tests_root)
    return f"{PLAYWRIGHT_ARC_PREFIX}/{rel.as_posix()}"


def collect_playwright_automation_files(repo: Path) -> list[tuple[Path, str]]:
    """(source path, zip arcname) for spec + minimal helpers."""
    out: list[tuple[Path, str]] = []
    seen_arc: set[str] = set()
    specs, _ = discover_playwright_ops_specs(repo)

    def add(path: Path, tests_root: Path) -> None:
        if not path.is_file():
            return
        arc = arcname_playwright_under_tests(tests_root, path)
        if arc in seen_arc:
            return
        seen_arc.add(arc)
        out.append((path, arc))

    for spec in specs:
        tests_root = spec.parent
        while tests_root.name != "tests" and tests_root != tests_root.parent:
            tests_root = tests_root.parent
        if tests_root.name != "tests":
            tests_root = spec.parent
        add(spec, tests_root)

    web_ele_tests = repo / "football-front" / "apps" / "web-ele" / "tests"
    if web_ele_tests.is_dir() and any(s.name == "ops-list-pagination.spec.ts" for s in specs):
        for helper_rel in PLAYWRIGHT_HELPER_RELPATHS:
            add(web_ele_tests / helper_rel, web_ele_tests)

    return out


def build_playwright_spec_index(repo: Path) -> str:
    specs, note = discover_playwright_ops_specs(repo)
    packed = collect_playwright_automation_files(repo)
    ts_packed = [(p, a) for p, a in packed if p.suffix.lower() == ".ts"]
    lines = [
        "# OPS Playwright E2E Spec 索引（交付包）",
        "",
        f"**生成日**：{date.today():%Y-%m-%d}",
        "",
        note,
        "",
        "## 已扫描目录",
        "",
    ]
    for rel in PLAYWRIGHT_TESTS_ROOTS:
        lines.append(f"- `{rel}`")
    lines.extend(
        [
            "",
            "## 匹配规则",
            "",
            "- `ops-*.spec.ts` · `*ops*.spec.ts` · `*uat*.spec.ts` · `uat-football-ops*.spec.ts`",
            "",
            "## 仓库内 Spec 路径",
            "",
        ]
    )
    if specs:
        for p in specs:
            try:
                rel = p.relative_to(repo)
            except ValueError:
                rel = p
            lines.append(f"- `{rel.as_posix()}`")
    else:
        lines.append("- （无本地文件；常见 SSOT 引用见 `docs/delivery/UAT-FOOTBALL-E2E-20260704.md`）")
        lines.extend(
            [
                "- `football-front/apps/web-ele/tests/uat-football-ops-login.spec.ts`",
                "- `football-front/apps/web-ele/tests/ops-list-pagination.spec.ts`（见 PLAN-ops-list-pagination）",
            ]
        )
    lines.extend(["", "## Zip 内已打包 `.ts` 文件", ""])
    if ts_packed:
        for src, arc in sorted(ts_packed, key=lambda x: x[1]):
            try:
                rel = src.relative_to(repo)
            except ValueError:
                rel = src
            lines.append(f"- `{arc}` ← `{rel.as_posix()}`")
        lines.extend(
            [
                "",
                f"**合计**：{sum(1 for _, a in ts_packed if a.endswith('.spec.ts'))} 个 spec，"
                f"{len(ts_packed)} 个 `.ts` 文件（含 helpers）。",
            ]
        )
    else:
        lines.append("- （无；子模块未检出或未匹配到 spec）")
    lines.append("")
    return "\n".join(lines)


def _glob_md(directory: Path, pattern: str) -> list[Path]:
    if not directory.is_dir():
        return []
    out: list[Path] = []
    for p in sorted(directory.glob(pattern)):
        if p.is_file() and not should_exclude(p):
            out.append(p)
    return out


def resolve_dev_prd() -> tuple[Path, str]:
    """Return (path, note) for root 开发版 PRD."""
    v92 = _repo() / "完整PRD-v9.2-开发版.md"
    v91 = _repo() / "完整PRD-v9.1-开发版.md"
    if v92.is_file():
        return v92, "完整PRD-v9.2-开发版.md（仓库根目录 SSOT）"
    if v91.is_file():
        return v91, "完整PRD-v9.1-开发版.md（v9.2 缺失，已回退 v9.1）"
    raise FileNotFoundError("Neither 完整PRD-v9.2 nor v9.1 found at repo root")


def collect_process_entries() -> list[ProcessEntry]:
    repo = _repo()
    docs = repo / "docs"
    entries: list[ProcessEntry] = []

    dev_prd, _ = resolve_dev_prd()
    entries.append(ProcessEntry(dev_prd, "prd"))

    for p in _glob_md(docs / "product", "PRD-*.md"):
        entries.append(ProcessEntry(p, "prd"))

    for p in _glob_md(docs / "engineering", "API-M*.md"):
        entries.append(ProcessEntry(p, "api"))

    method_names = [
        "PHASE-DEV-METHOD.md",
        "AI-IMPL-GUIDE.md",
        "GLOBAL-CONVENTIONS.md",
        "PROJECT-OVERVIEW.md",
        "TECH-CONSTRAINTS.md",
        "QUALITY-GATES.md",
        "OPS-RBAC-DATA-SCOPE.md",
        "DEV-PLAN.md",
    ]
    eng = docs / "engineering"
    for name in method_names:
        p = eng / name
        if p.is_file() and not should_exclude(p):
            entries.append(ProcessEntry(p, "design/method"))

    for p in _glob_md(docs / "engineering", "STATE-M*.md"):
        entries.append(ProcessEntry(p, "design/state"))

    for p in _glob_md(docs / "adr", "ADR-*.md"):
        entries.append(ProcessEntry(p, "design/adr"))

    for p in _glob_md(docs / "adr", "ADR-M*.md"):
        if not any(e.source == p for e in entries):
            entries.append(ProcessEntry(p, "design/adr"))

    for p in _glob_md(docs / "product", "UX-M*.md"):
        entries.append(ProcessEntry(p, "design/ux"))

    deploy_root = docs / "deploy" / "ops-greenfield-production"
    for p in sorted(deploy_root.rglob("*.md")):
        if should_exclude(p):
            continue
        rel = p.relative_to(deploy_root)
        sub = "database/greenfield" if rel.parent == Path(".") else f"database/greenfield/{rel.parent.as_posix()}"
        entries.append(ProcessEntry(p, sub))

    sql_dir = deploy_root / "sql"
    if sql_dir.is_dir():
        for p in sorted(sql_dir.glob("*.sql")):
            if should_exclude(p):
                continue
            entries.append(ProcessEntry(p, "database/greenfield/sql"))

    wd_schema = docs / "sql" / "wd-schema.sql"
    if wd_schema.is_file():
        entries.append(ProcessEntry(wd_schema, "database/reference"))

    delivery = docs / "delivery"
    for pattern in ("CHECKLIST-M*.md", "TESTCASES-M*.md"):
        for p in _glob_md(delivery, pattern):
            entries.append(ProcessEntry(p, "test/testcases"))

    for p in _glob_md(delivery, "SLICES-M*.md"):
        entries.append(ProcessEntry(p, "delivery"))

    for p in _collect_delivery_report_files(delivery):
        rel = p.relative_to(delivery)
        if rel.parent == Path("."):
            sub = "test/reports"
        else:
            sub = f"test/reports/{rel.parent.as_posix()}"
        entries.append(ProcessEntry(p, sub))

    for name in (
        "OPS-DEV-DEPLOY-GUIDE.md",
        "PLAN-ops-list-pagination.md",
    ):
        p = delivery / name
        if p.is_file() and not should_exclude(p):
            entries.append(ProcessEntry(p, "delivery"))

    index_src = docs / "delivery" / "product-handover" / "OPS过程文档-目录.md"
    if index_src.is_file():
        entries.append(ProcessEntry(index_src, ""))

    return _dedupe_entries(entries)


def _dedupe_entries(entries: list[ProcessEntry]) -> list[ProcessEntry]:
    seen: set[tuple[str, str]] = set()
    out: list[ProcessEntry] = []
    for e in entries:
        arc_key = f"{e.arc_subdir}/{e.source.name}" if e.arc_subdir else e.source.name
        key = (str(e.source.resolve()), arc_key)
        if key in seen:
            continue
        seen.add(key)
        if not _under_repo(e.source):
            continue
        out.append(e)
    return out


def arcname_for(entry: ProcessEntry) -> str:
    if entry.arc_subdir:
        return f"{ARC_PROCESS}/{entry.arc_subdir}/{entry.source.name}"
    return f"{ARC_PROCESS}/{entry.source.name}"


def build_sql_manifest(repo: Path) -> str:
    lines = [
        f"# OPS Greenfield SQL 清单（生成日 {date.today():%Y-%m-%d}）",
        "",
        "完整 DDL/DML 位于 Zip 内 `process-docs/database/greenfield/sql/`。",
        "Flyway 增量以 `football-module-ops` 模块内 `src/main/resources/db/migration` 为运行时 SSOT；",
        "本包 greenfield 脚本用于零基础建库（见 greenfield README）。",
        "",
        "## docs/deploy/ops-greenfield-production/sql/",
        "",
    ]
    sql_dir = repo / "docs" / "deploy" / "ops-greenfield-production" / "sql"
    if sql_dir.is_dir():
        for p in sorted(sql_dir.iterdir()):
            if p.suffix.lower() == ".sql":
                lines.append(f"- `{p.name}` ({p.stat().st_size:,} bytes)")
    lines.extend(
        [
            "",
            "## docs/sql/（参考）",
            "",
            "- `wd-schema.sql` — 历史 wd 库结构参考（Football 合并后身份/字典以 shenyu-system 为准，见 ADR-056）",
            "",
        ]
    )
    return "\n".join(lines)


def summarize_by_subdir(arc_names: list[str]) -> dict[str, int]:
    counts: dict[str, int] = {}
    prefix = f"{ARC_PROCESS}/"
    for name in arc_names:
        if not name.startswith(prefix):
            continue
        rest = name[len(prefix) :]
        top = rest.split("/", 1)[0]
        counts[top] = counts.get(top, 0) + 1
    return counts


def summarize_test_artifacts(arc_names: list[str]) -> dict[str, int]:
    """Counts under process-docs/test/testcases, test/reports, automation/playwright."""
    out = {"testcases": 0, "reports": 0, "playwright_ts": 0, "playwright_specs": 0}
    pw_prefix = f"{ARC_PROCESS}/test/automation/playwright/"
    for name in arc_names:
        if name.startswith(f"{ARC_PROCESS}/test/testcases/"):
            out["testcases"] += 1
        elif name.startswith(f"{ARC_PROCESS}/test/reports/"):
            out["reports"] += 1
        elif name.startswith(pw_prefix) and name.endswith(".ts"):
            out["playwright_ts"] += 1
            if name.endswith(".spec.ts"):
                out["playwright_specs"] += 1
    return out


def validate_minimum_counts(arc_names: list[str]) -> list[str]:
    """Return list of warning messages (empty if OK)."""
    warnings: list[str] = []
    by_top = summarize_by_subdir(arc_names)

    def has_prefix(prefix: str) -> int:
        return sum(1 for n in arc_names if n.startswith(f"{ARC_PROCESS}/{prefix}"))

    if by_top.get("prd", 0) < 10:
        warnings.append(f"prd count low: {by_top.get('prd', 0)}")
    if has_prefix("api/") < 10:
        warnings.append(f"api count low: {has_prefix('api/')}")
    if has_prefix("design/adr/") < 50:
        warnings.append(f"adr count low: {has_prefix('design/adr/')}")
    test = summarize_test_artifacts(arc_names)
    if test["testcases"] < 20:
        warnings.append(f"test/testcases count low: {test['testcases']}")
    if test["reports"] < 40:
        warnings.append(f"test/reports count low: {test['reports']}")
    if by_top.get("delivery", 0) < 10:
        warnings.append(f"delivery (slices/guides) count low: {by_top.get('delivery', 0)}")
    return warnings


def collect_virtual_text_entries(repo: Path) -> list[tuple[str, str]]:
    """(arcname, utf-8 text) generated at pack time."""
    return [
        (f"{ARC_PROCESS}/database/SQL-MANIFEST.txt", build_sql_manifest(repo)),
        (
            f"{ARC_PROCESS}/test/testcases/PLAYWRIGHT-OPS-SPECS-INDEX.md",
            build_playwright_spec_index(repo),
        ),
    ]


def main() -> None:
    import sys as _sys

    repo = Path(__file__).resolve().parents[3]
    global REPO
    REPO = repo
    entries = collect_process_entries()
    by_sub: dict[str, int] = {}
    for e in entries:
        seg = e.arc_subdir.split("/", 1)[0] if e.arc_subdir else "(root)"
        by_sub[seg] = by_sub.get(seg, 0) + 1
    _, prd_note = resolve_dev_prd()
    print(f"REPO={repo}")
    print(f"process entries: {len(entries)}")
    print(f"  {prd_note}")
    for seg, n in sorted(by_sub.items()):
        print(f"  {seg}: {n}")
    arc_names = [arcname_for(e) for e in entries]
    arc_names.extend(a for a, _ in collect_virtual_text_entries(repo))
    print("test artifacts:", summarize_test_artifacts(arc_names))
    warnings = validate_minimum_counts(arc_names)
    if warnings:
        print("WARN:")
        for w in warnings:
            print(f"  {w}")
        _sys.exit(1)
    print("OK")


if __name__ == "__main__":
    main()
