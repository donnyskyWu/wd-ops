# -*- coding: utf-8 -*-
"""Pack OPS product delivery zip for customer handover."""
from __future__ import annotations

import subprocess
import sys
import zipfile
from collections import Counter
from datetime import date
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]  # docs/
REPO = ROOT.parent
HANDOVER = Path(__file__).resolve().parent
PRODUCT = ROOT / "product"
DELIVERY = PRODUCT / "delivery"
SHOTS = PRODUCT / "delivery-screenshots"
BUILD_HANDOVER_DOCX = PRODUCT / "_build_handover_docx.py"

OUT_NAME = f"OPS产品交付包-{date.today():%Y%m%d}.zip"
OUT_ZIP = HANDOVER / OUT_NAME

# Customer handover SSOT: 需求 + 技术 + 部署运维（md + docx）
HANDOVER_CORE: list[tuple[str, str]] = [
    ("OPS产品交付-需求文档.md", "OPS产品交付-需求文档.docx"),
    ("OPS产品交付-技术文档.md", "OPS产品交付-技术文档.docx"),
    ("OPS产品交付-部署运维手册.md", "OPS产品交付-部署运维手册.docx"),
]

ARC_HANDOVER = "product-handover"
ARC_MANUALS = "product-manuals"
ARC_SHOTS = "delivery-screenshots"

REQUIRED_IN_ZIP = [
    f"{ARC_HANDOVER}/OPS产品交付-需求文档.md",
    f"{ARC_HANDOVER}/OPS产品交付-需求文档.docx",
    f"{ARC_HANDOVER}/OPS产品交付-技术文档.md",
    f"{ARC_HANDOVER}/OPS产品交付-技术文档.docx",
    "process-docs/OPS过程文档-目录.md",
    "process-docs/database/SQL-MANIFEST.txt",
    "process-docs/test/testcases/PLAYWRIGHT-OPS-SPECS-INDEX.md",
]

# Import process-doc collector (same directory)
sys.path.insert(0, str(HANDOVER))
import _pack_process_docs as process_docs  # noqa: E402

process_docs.REPO = REPO


def handover_docx_stale() -> bool:
    for md_name, docx_name in HANDOVER_CORE:
        md_path = HANDOVER / md_name
        docx_path = HANDOVER / docx_name
        if not md_path.exists():
            raise FileNotFoundError(f"Missing handover markdown: {md_path}")
        if not docx_path.exists():
            return True
        if md_path.stat().st_mtime > docx_path.stat().st_mtime:
            return True
    return False


def ensure_handover_docx() -> None:
    if not handover_docx_stale():
        return
    if not BUILD_HANDOVER_DOCX.exists():
        raise FileNotFoundError(f"Handover docx builder not found: {BUILD_HANDOVER_DOCX}")
    print("Regenerating handover docx (missing or older than .md)...")
    subprocess.run(
        [sys.executable, str(BUILD_HANDOVER_DOCX)],
        cwd=str(REPO),
        check=True,
    )


def collect_handover_files() -> list[tuple[Path, str]]:
    out: list[tuple[Path, str]] = []
    seen: set[Path] = set()
    for md_name, docx_name in HANDOVER_CORE:
        for name in (md_name, docx_name):
            path = HANDOVER / name
            if not path.is_file():
                raise FileNotFoundError(f"Required handover file missing: {path}")
            if path not in seen:
                seen.add(path)
                out.append((path, ARC_HANDOVER))
    readme = HANDOVER / "README.md"
    if readme.is_file() and readme not in seen:
        out.append((readme, ARC_HANDOVER))
    index = HANDOVER / "OPS过程文档-目录.md"
    if index.is_file() and index not in seen:
        out.append((index, ARC_HANDOVER))
    return out


def collect_manual_files() -> list[tuple[Path, str]]:
    out: list[tuple[Path, str]] = []
    for p in sorted(DELIVERY.glob("*.docx")):
        out.append((p, ARC_MANUALS))
    for p in sorted(DELIVERY.glob("*.md")):
        if p.name.startswith("_"):
            continue
        out.append((p, ARC_MANUALS))
    return out


def collect_screenshot_files() -> list[tuple[Path, str]]:
    out: list[tuple[Path, str]] = []
    for p in sorted(SHOTS.glob("*.png")):
        out.append((p, ARC_SHOTS))
    readme = SHOTS / "README.md"
    if readme.exists():
        out.append((readme, ARC_SHOTS))
    return out


def top_level_folder(arc: str) -> str:
    return arc.split("/", 1)[0]


def main() -> None:
    ensure_handover_docx()

    files: list[tuple[Path, str | None, str]] = []

    for path, prefix in collect_handover_files():
        files.append((path, None, f"{prefix}/{path.name}"))

    for path, prefix in collect_manual_files():
        files.append((path, None, f"{prefix}/{path.name}"))

    for path, prefix in collect_screenshot_files():
        files.append((path, None, f"{prefix}/{path.name}"))

    seen_arc: set[str] = set()
    for entry in process_docs.collect_process_entries():
        arc = process_docs.arcname_for(entry)
        if arc in seen_arc:
            continue
        seen_arc.add(arc)
        files.append((entry.source, None, arc))

    for arc, text in process_docs.collect_virtual_text_entries(REPO):
        if arc in seen_arc:
            continue
        seen_arc.add(arc)
        files.append((None, text, arc))

    for src, arc in process_docs.collect_playwright_automation_files(REPO):
        if arc in seen_arc:
            continue
        seen_arc.add(arc)
        files.append((src, None, arc))

    if OUT_ZIP.exists():
        OUT_ZIP.unlink()

    arc_names: list[str] = []
    with zipfile.ZipFile(OUT_ZIP, "w", zipfile.ZIP_DEFLATED) as zf:
        for path, inline, arc in files:
            if arc in arc_names:
                continue
            if inline is not None:
                zf.writestr(arc, inline)
            else:
                assert path is not None
                zf.write(path, arcname=arc)
            arc_names.append(arc)

    missing = [r for r in REQUIRED_IN_ZIP if r not in arc_names]
    if missing:
        raise RuntimeError(f"Zip missing required entries: {missing}")

    warnings = process_docs.validate_minimum_counts(arc_names)
    if warnings:
        print("WARN process-docs validation:")
        for w in warnings:
            print(f"  - {w}")

    size = OUT_ZIP.stat().st_size
    top_counts = Counter(top_level_folder(a) for a in arc_names)
    process_sub = process_docs.summarize_by_subdir(arc_names)
    test_stats = process_docs.summarize_test_artifacts(arc_names)
    process_total = sum(1 for a in arc_names if a.startswith("process-docs/"))

    print(f"Wrote {OUT_ZIP}")
    print(f"  size: {size:,} bytes ({size / (1024 * 1024):.2f} MiB)")
    print(f"  files: {len(arc_names)}")
    print(
        f"  process-docs/: {process_total} "
        f"(test/testcases: {test_stats['testcases']}, test/reports: {test_stats['reports']}, "
        f"playwright .ts: {test_stats['playwright_ts']} [{test_stats['playwright_specs']} specs]); "
        f"other: {len(arc_names) - process_total}"
    )
    print("Top-level folders:")
    for folder, count in sorted(top_counts.items()):
        print(f"  {folder}/: {count}")
    print("process-docs breakdown (by first segment under process-docs/):")
    for seg, count in sorted(process_sub.items()):
        print(f"  process-docs/{seg}/: {count}")
    _, prd_note = process_docs.resolve_dev_prd()
    print(f"  dev PRD: {prd_note}")

    print("Required entries:")
    for r in REQUIRED_IN_ZIP:
        mark = "OK" if r in arc_names else "MISSING"
        print(f"  [{mark}] {r}")


if __name__ == "__main__":
    main()
