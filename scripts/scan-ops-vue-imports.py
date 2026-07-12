#!/usr/bin/env python3
"""Scan ops vue files for Vue API usage without import."""
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "football-front/apps/web-ele/src"
DIRS = [ROOT / "views/ops", ROOT / "components/ops"]
APIS = ["computed", "ref", "reactive", "watch", "onMounted", "onUnmounted", "onBeforeUnmount"]

SCRIPT_RE = re.compile(r"<script[^>]*>([\s\S]*?)</script>")
IMPORT_RE = re.compile(r"import\s*\{([^}]+)\}\s*from\s*['\"]vue['\"]")


def imported_apis(script: str) -> set[str]:
    m = IMPORT_RE.search(script)
    if not m:
        return set()
    return {x.strip() for x in m.group(1).split(",")}


def used_apis(script: str) -> set[str]:
    found = set()
    for api in APIS:
        if re.search(rf"\b{api}\b", script):
            found.add(api)
    return found


def main() -> None:
    issues: list[tuple[str, str]] = []
    for d in DIRS:
        for f in sorted(d.rglob("*.vue")):
            text = f.read_text(encoding="utf-8")
            m = SCRIPT_RE.search(text)
            if not m:
                continue
            script = m.group(1)
            used = used_apis(script)
            imported = imported_apis(script)
            missing = sorted(used - imported)
            if missing:
                issues.append((str(f.relative_to(ROOT)), ", ".join(missing)))

    print(f"Files with missing Vue imports: {len(issues)}")
    for path, missing in issues:
        print(f"  {path}: {missing}")


if __name__ == "__main__":
    main()
