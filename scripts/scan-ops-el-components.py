#!/usr/bin/env python3
"""List Element Plus kebab components used in ops views."""
from __future__ import annotations

import re
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "football-front/apps/web-ele/src/views/ops"
EL_RE = re.compile(r"<(el-[a-z0-9-]+)")

def main() -> None:
    counts: Counter[str] = Counter()
    for f in ROOT.rglob("*.vue"):
        for m in EL_RE.finditer(f.read_text(encoding="utf-8", errors="replace")):
            counts[m.group(1)] += 1
    for tag, n in counts.most_common():
        print(f"{n:4d}  {tag}")

if __name__ == "__main__":
    main()
