#!/usr/bin/env python3
"""Restore root <template> tags stripped by mount-ops-all ensure_ops_page bug."""
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
VIEWS = ROOT / "football-front/apps/web-ele/src/views/ops"

TEMPLATE_ROOT = re.compile(r"^\s*<template[\s>]")
LEADING_COMMENTS = re.compile(r"^(\s*(?:<!--[\s\S]*?-->\s*)*)")


def needs_fix(content: str) -> bool:
    stripped = LEADING_COMMENTS.sub("", content, count=1)
    return not TEMPLATE_ROOT.match(stripped)


def fix_content(content: str) -> str:
    if not needs_fix(content):
        return content
    m = LEADING_COMMENTS.match(content)
    prefix = m.group(1) if m else ""
    rest = content[len(prefix) :]
    return f"{prefix}<template>\n{rest}"


def main() -> None:
    fixed = 0
    for f in sorted(VIEWS.rglob("*.vue")):
        original = f.read_text(encoding="utf-8")
        updated = fix_content(original)
        if updated != original:
            f.write_text(updated, encoding="utf-8")
            fixed += 1
            print(f"  fixed: {f.relative_to(VIEWS)}")
    print(f"\nDone. Fixed {fixed} vue files.")


if __name__ == "__main__":
    main()
