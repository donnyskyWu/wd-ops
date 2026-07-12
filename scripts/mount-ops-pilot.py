#!/usr/bin/env python3
"""S2-B pilot: copy Ops M1 IP Group views/api/components into football-front with import rewrites."""
from __future__ import annotations

import re
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OPS_UI = ROOT / "ops-platform-ui-vue/src"
FF = ROOT / "football-front/apps/web-ele/src"

REWRITES = [
    (r"from '@/utils/request'", "from '#/api/ops/client'"),
    (r'from "@/utils/request"', 'from "#/api/ops/client"'),
    (r"from '@/api/ip-group'", "from '#/api/ops/ip-group'"),
    (r"from '@/api/dict'", "from '#/api/ops/dict'"),
    (r"from '@/types/ip-group'", "from '#/types/ops/ip-group'"),
    (r"from '@/components/DictSelect.vue'", "from '#/components/ops/DictSelect.vue'"),
    (r"from '@/components/DictLabel.vue'", "from '#/components/ops/DictLabel.vue'"),
    (r"from '@/components/selectors/IpGroupTreeSelect.vue'", "from '#/components/ops/selectors/IpGroupTreeSelect.vue'"),
    (r"from '@/components/selectors/UserSelect.vue'", "from '#/components/ops/selectors/UserSelect.vue'"),
    (r"from '@/components/selectors/AccountSelect.vue'", "from '#/components/ops/selectors/AccountSelect.vue'"),
    (r"from '@/components/selectors/selector-utils'", "from '#/components/ops/selectors/selector-utils'"),
    (r"from '@/api/ip-group'", "from '#/api/ops/ip-group'"),
]


def transform(content: str) -> str:
    for old, new in REWRITES:
        content = content.replace(old, new)
    return content


def copy_transform(src: Path, dst: Path) -> None:
    dst.parent.mkdir(parents=True, exist_ok=True)
    text = src.read_text(encoding="utf-8")
    dst.write_text(transform(text), encoding="utf-8")
    print(f"  {dst.relative_to(ROOT)}")


def main() -> None:
    pairs = [
        (OPS_UI / "views/operations/IpGroup.vue", FF / "views/ops/operations/IpGroup.vue"),
        (OPS_UI / "types/ip-group.ts", FF / "types/ops/ip-group.ts"),
        (OPS_UI / "components/DictSelect.vue", FF / "components/ops/DictSelect.vue"),
        (OPS_UI / "components/DictLabel.vue", FF / "components/ops/DictLabel.vue"),
        (OPS_UI / "components/selectors/IpGroupTreeSelect.vue", FF / "components/ops/selectors/IpGroupTreeSelect.vue"),
        (OPS_UI / "components/selectors/UserSelect.vue", FF / "components/ops/selectors/UserSelect.vue"),
        (OPS_UI / "components/selectors/AccountSelect.vue", FF / "components/ops/selectors/AccountSelect.vue"),
        (OPS_UI / "components/selectors/selector-utils.ts", FF / "components/ops/selectors/selector-utils.ts"),
    ]
    print("Copying pilot files:")
    for src, dst in pairs:
        if not src.exists():
            raise SystemExit(f"Missing source: {src}")
        copy_transform(src, dst)

    # api/ops/ip-group.ts from ops api with requestClient
    ip_api = (OPS_UI / "api/ip-group.ts").read_text(encoding="utf-8")
    ip_api = ip_api.replace("from '@/utils/request'", "from './client'")
    ip_api = ip_api.replace("from '@/types/ip-group'", "from '#/types/ops/ip-group'")
    ip_api = ip_api.replace(
        "import { request } from './client'",
        "import { opsRequest as request } from './client'",
    )
    (FF / "api/ops/ip-group.ts").write_text(ip_api, encoding="utf-8")
    print(f"  {FF.relative_to(ROOT)}/api/ops/ip-group.ts")

    dict_api = (OPS_UI / "api/dict.ts").read_text(encoding="utf-8")
    dict_api = dict_api.replace("from '@/utils/request'", "from './client'")
    dict_api = dict_api.replace(
        "import { request } from './client'",
        "import { opsRequest as request } from './client'",
    )
    (FF / "api/ops/dict.ts").write_text(dict_api, encoding="utf-8")
    print(f"  {FF.relative_to(ROOT)}/api/ops/dict.ts")

    print("Done.")


if __name__ == "__main__":
    main()
