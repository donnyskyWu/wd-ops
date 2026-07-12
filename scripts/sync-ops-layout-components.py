#!/usr/bin/env python3
from pathlib import Path
import shutil

ROOT = Path(__file__).resolve().parents[1]
IMPORTS = [
    ("from '@/components/", "from '#/components/ops/"),
    ("from '@/api/", "from '#/api/ops/"),
    ("from '@/types/", "from '#/types/ops/"),
    ("from '@/utils/", "from '#/utils/ops/"),
]
pairs = [
    ("ops-platform-ui-vue/src/components/Pagination.vue", "football-front/apps/web-ele/src/components/ops/Pagination.vue"),
    ("ops-platform-ui-vue/src/components/ContentWrap.vue", "football-front/apps/web-ele/src/components/ops/ContentWrap.vue"),
    ("ops-platform-ui-vue/src/components/TableSearch.vue", "football-front/apps/web-ele/src/components/ops/TableSearch.vue"),
    ("ops-platform-ui-vue/src/views/production/content/ContentEditDialog.vue", "football-front/apps/web-ele/src/views/ops/production/content/ContentEditDialog.vue"),
    ("ops-platform-ui-vue/src/views/production/content/ContentEditPanel.vue", "football-front/apps/web-ele/src/views/ops/production/content/ContentEditPanel.vue"),
]
for src_rel, dst_rel in pairs:
    text = (ROOT / src_rel).read_text(encoding="utf-8")
    for old, new in IMPORTS:
        text = text.replace(old, new)
    dst = ROOT / dst_rel
    dst.parent.mkdir(parents=True, exist_ok=True)
    dst.write_text(text, encoding="utf-8")
    print("synced", dst_rel)
