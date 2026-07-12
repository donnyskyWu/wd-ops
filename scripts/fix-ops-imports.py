#!/usr/bin/env python3
"""Rewrite legacy @/ imports to #/…/ops/ in mounted football-front ops tree."""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
FF = ROOT / "football-front/apps/web-ele/src"

OPS_DIRS = [
    "views/ops",
    "api/ops",
    "components/ops",
    "types/ops",
    "utils/ops",
    "constants/ops",
    "composables/ops",
    "mock/ops",
]


def transform(content: str) -> str:
    content = re.sub(
        r"import router from ['\"]@/router['\"]",
        "import { router } from '#/router'",
        content,
    )
    content = re.sub(
        r"import \{ router \} from ['\"]@/router['\"]",
        "import { router } from '#/router'",
        content,
    )
    content = re.sub(r"from ['\"]@/utils/request['\"]", "from '#/api/ops/client'", content)
    content = re.sub(r"from ['\"]@/utils/([^'\"]+)['\"]", r"from '#/utils/ops/\1'", content)
    content = re.sub(r"from ['\"]@/utils['\"]", "from '#/utils/ops/index'", content)
    content = re.sub(r"from ['\"]@/api/([^'\"]+)['\"]", r"from '#/api/ops/\1'", content)
    content = re.sub(r"from ['\"]@/types/([^'\"]+)['\"]", r"from '#/types/ops/\1'", content)
    content = re.sub(r"from ['\"]@/components/([^'\"]+)['\"]", r"from '#/components/ops/\1'", content)
    content = re.sub(r"from ['\"]@/views/([^'\"]+)['\"]", r"from '#/views/ops/\1'", content)
    content = re.sub(r"from ['\"]@/constants/([^'\"]+)['\"]", r"from '#/constants/ops/\1'", content)
    content = re.sub(r"from ['\"]@/composables/([^'\"]+)['\"]", r"from '#/composables/ops/\1'", content)
    content = re.sub(r"from ['\"]@/mock/([^'\"]+)['\"]", r"from '#/mock/ops/\1'", content)
    content = re.sub(r"import\(['\"]@/types/([^'\"]+)['\"]\)", r"import('#/types/ops/\1')", content)
    return content


def main() -> int:
    changed: list[str] = []
    for rel in OPS_DIRS:
        base = FF / rel
        if not base.exists():
            continue
        for path in sorted(base.rglob("*")):
            if path.suffix not in (".ts", ".vue", ".tsx"):
                continue
            text = path.read_text(encoding="utf-8")
            new_text = transform(text)
            if new_text != text:
                path.write_text(new_text, encoding="utf-8")
                changed.append(str(path.relative_to(ROOT)))
    print(f"Rewritten {len(changed)} files")
    for line in changed:
        print(f"  {line}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
