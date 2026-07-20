#!/usr/bin/env python3
"""Sync selected layout/content components from ops-platform-ui-vue into football-front.

Import rewrite (required): ops-platform-ui-vue uses Vite alias @/; football-front uses #/
with ops-prefixed paths (#/utils/ops/, #/composables/ops/, etc.). Copying source
verbatim breaks Vite resolution — this script reuses mount-ops-all._rewrite_legacy_at_imports
so re-sync after upstream edits does not reintroduce @/ imports.
"""
from __future__ import annotations

import importlib.util
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

pairs = [
    ("ops-platform-ui-vue/src/components/Pagination.vue", "football-front/apps/web-ele/src/components/ops/Pagination.vue"),
    ("ops-platform-ui-vue/src/components/ContentWrap.vue", "football-front/apps/web-ele/src/components/ops/ContentWrap.vue"),
    ("ops-platform-ui-vue/src/components/TableSearch.vue", "football-front/apps/web-ele/src/components/ops/TableSearch.vue"),
    ("ops-platform-ui-vue/src/views/production/content/ContentEditDialog.vue", "football-front/apps/web-ele/src/views/ops/production/content/ContentEditDialog.vue"),
    ("ops-platform-ui-vue/src/views/production/content/ContentEditPanel.vue", "football-front/apps/web-ele/src/views/ops/production/content/ContentEditPanel.vue"),
]


def _load_rewrite_imports():
    spec = importlib.util.spec_from_file_location(
        "mount_ops_all",
        ROOT / "scripts" / "mount-ops-all.py",
    )
    mod = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(mod)
    return mod._rewrite_legacy_at_imports


rewrite_imports = _load_rewrite_imports()

for src_rel, dst_rel in pairs:
    text = (ROOT / src_rel).read_text(encoding="utf-8")
    text = rewrite_imports(text)
    dst = ROOT / dst_rel
    dst.parent.mkdir(parents=True, exist_ok=True)
    dst.write_text(text, encoding="utf-8")
    print("synced", dst_rel)
