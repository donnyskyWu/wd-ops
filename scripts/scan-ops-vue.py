#!/usr/bin/env python3
"""Scan football-front ops views for common mount issues."""
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
FF = ROOT / "football-front/apps/web-ele/src"
VIEWS = FF / "views/ops"

missing_root_template = []
at_imports = []

TEMPLATE_ROOT = re.compile(r"^\s*<template[\s>]")

for f in sorted(VIEWS.rglob("*.vue")):
    t = f.read_text(encoding="utf-8")
    rel = str(f.relative_to(VIEWS))
    # strip leading comment blocks
    stripped = re.sub(r"^(\s*<!--[\s\S]*?-->\s*)+", "", t)
    if not TEMPLATE_ROOT.match(stripped):
        missing_root_template.append(rel)
    if "@/" in t:
        at_imports.append(rel)

print(f"Total vue: {len(list(VIEWS.rglob('*.vue')))}")
print(f"Missing root <template>: {len(missing_root_template)}")
for x in missing_root_template:
    print(f"  {x}")
print(f"Contains @/: {len(at_imports)}")
