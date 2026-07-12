#!/usr/bin/env python3
"""Copy Ops API/types/utils needed by perf+finance+account modules."""
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OPS_UI = ROOT / "ops-platform-ui-vue/src"
FF = ROOT / "football-front/apps/web-ele/src"

IMPORT_REWRITES: list[tuple[str, str]] = [
    ("from '@/utils/request'", "from '#/api/ops/client'"),
    ('from "@/utils/request"', 'from "#/api/ops/client"'),
    ("from '@/utils/", "from '#/utils/ops/"),
    ("from '@/api/", "from '#/api/ops/"),
    ("from '@/types/", "from '#/types/ops/"),
    ("from '@/components/", "from '#/components/ops/"),
    ("from '@/views/", "from '#/views/ops/"),
]


def transform_imports(content: str) -> str:
    for old, new in IMPORT_REWRITES:
        content = content.replace(old, new)
    return content.replace(
        "import { opsRequest } from '#/api/ops/client'",
        "import { request } from '#/api/ops/client'",
    )


def transform_api(content: str) -> str:
    content = content.replace("from '@/utils/request'", "from './client'")
    content = content.replace('from "@/utils/request"', 'from "./client"')
    content = re.sub(r"from '@/types/([^']+)'", r"from '#/types/ops/\1'", content)
    content = re.sub(r'from "@/types/([^"]+)"', r'from "#/types/ops/\1"', content)
    return content


def copy_file(src_rel: str, dst_rel: str, *, api: bool = False) -> bool:
    src = OPS_UI / src_rel
    dst = FF / dst_rel
    if not src.exists():
        print(f"SKIP missing {src}")
        return False
    dst.parent.mkdir(parents=True, exist_ok=True)
    text = src.read_text(encoding="utf-8")
    text = transform_api(text) if api else transform_imports(text)
    dst.write_text(text, encoding="utf-8")
    print(f"copied {dst_rel}")
    return True


APIS = [
    "orderAttribution.ts",
    "perfRecord.ts",
    "perfResult.ts",
    "perfTemplate.ts",
    "metric.ts",
    "finance.ts",
    "account.ts",
    "follower.ts",
    "company.ts",
    "platform-account.ts",
    "personal-account.ts",
    "phone.ts",
    "realname.ts",
    "simcard.ts",
    "file.ts",
    "dict.ts",
    "ip-group.ts",
]
TYPES = ["perfExecution.ts", "perfTemplate.ts"]
UTILS = ["index.ts", "fileUrl.ts", "enum-alias.ts", "account-binding-conflict.ts"]


def main() -> None:
    for name in APIS:
        copy_file(f"api/{name}", f"api/ops/{name}", api=True)
    for name in TYPES:
        copy_file(f"types/{name}", f"types/ops/{name}")
    for name in UTILS:
        copy_file(f"utils/{name}", f"utils/ops/{name}")


if __name__ == "__main__":
    main()
