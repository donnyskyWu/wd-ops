#!/usr/bin/env python3
"""Resolve #/ imports in ops vue/ts files and report missing targets."""
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "football-front/apps/web-ele/src"
OPS_DIRS = [
    SRC / "views/ops",
    SRC / "components/ops",
    SRC / "api/ops",
    SRC / "utils/ops",
    SRC / "types/ops",
]

IMPORT_RE = re.compile(
    r"""from\s+['"]#/(views/ops/[^'"]+|components/ops/[^'"]+|api/ops/[^'"]+|utils/ops/[^'"]+|types/ops/[^'"]+)['"]"""
)
IMPORT_RE2 = re.compile(r"""from\s+['"]\./([^'"]+)['"]""")


def resolve_hash(path: str) -> Path | None:
    base = SRC / path
    for ext in ("", ".ts", ".vue", "/index.ts", "/index.vue"):
        candidate = Path(str(base) + ext) if ext else base
        if candidate.exists():
            return candidate
    return None


def resolve_relative(src_file: Path, rel: str) -> Path | None:
    base = (src_file.parent / rel).resolve()
    for ext in ("", ".ts", ".vue"):
        candidate = Path(str(base) + ext)
        if candidate.exists():
            return candidate
    return None


def main() -> None:
    missing: list[str] = []
    checked = 0
    for ops_dir in OPS_DIRS:
        for f in sorted(ops_dir.rglob("*")):
            if f.suffix not in {".vue", ".ts"}:
                continue
            text = f.read_text(encoding="utf-8")
            for m in IMPORT_RE.finditer(text):
                checked += 1
                target = resolve_hash(m.group(1))
                if not target:
                    missing.append(f"{f.relative_to(SRC)} -> #/{m.group(1)}")
            if f.parent.name == "api" and "ops" in str(f.parent):
                for m in IMPORT_RE2.finditer(text):
                    checked += 1
                    target = resolve_relative(f, m.group(1))
                    if not target:
                        missing.append(f"{f.relative_to(SRC)} -> ./{m.group(1)}")
    print(f"Checked {checked} imports")
    if missing:
        print(f"MISSING ({len(missing)}):")
        for x in missing:
            print(f"  {x}")
    else:
        print("PASS all imports resolve")


if __name__ == "__main__":
    main()
