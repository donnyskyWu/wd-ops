#!/usr/bin/env python3
"""S4 batch: copy ALL Ops views/api/components/types into football-front (ADR-047 additive)."""
from __future__ import annotations

import csv
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OPS_UI = ROOT / "ops-platform-ui-vue/src"
FF = ROOT / "football-front/apps/web-ele/src"
CSV_PATH = ROOT / "docs/delivery/oa-menu-permission-map.csv"

COLOR_REWRITES: list[tuple[str, str]] = [
    ("background-color: #ffffff", "background-color: var(--el-bg-color)"),
    ("background: #ffffff", "background: var(--el-bg-color)"),
    ("background-color: #fff", "background-color: var(--el-bg-color)"),
    ("background: #fff", "background: var(--el-bg-color)"),
    ("background-color:#fff", "background-color: var(--el-bg-color)"),
    ("background:#fff", "background: var(--el-bg-color)"),
    ("background-color: white", "background-color: var(--el-bg-color)"),
    ("background: white", "background: var(--el-bg-color)"),
    ("background-color: #f5f7fa", "background-color: var(--el-fill-color-light)"),
    ("background: #f5f7fa", "background: var(--el-fill-color-light)"),
    ("background-color: #fafafa", "background-color: var(--el-fill-color-lighter)"),
    ("background: #fafafa", "background: var(--el-fill-color-lighter)"),
    ("color: #303133", "color: var(--el-text-color-primary)"),
    ("color:#303133", "color: var(--el-text-color-primary)"),
    ("color: #606266", "color: var(--el-text-color-regular)"),
    ("color:#606266", "color: var(--el-text-color-regular)"),
    ("color: #909399", "color: var(--el-text-color-secondary)"),
    ("color:#909399", "color: var(--el-text-color-secondary)"),
    ("border: 1px solid #ebeef5", "border: 1px solid var(--el-border-color-lighter)"),
    ("border-bottom: 1px solid #ebeef5", "border-bottom: 1px solid var(--el-border-color-lighter)"),
    ("border: 1px solid #e4e7ed", "border: 1px solid var(--el-border-color)"),
    ("border-color: #ebeef5", "border-color: var(--el-border-color-lighter)"),
    ("border-color: #e4e7ed", "border-color: var(--el-border-color)"),
    ("background: #f5f9ff", "background: var(--el-color-primary-light-9)"),
    ("background-color: #f5f9ff", "background-color: var(--el-color-primary-light-9)"),
    ("background: #f0fffc", "background: var(--el-color-success-light-9)"),
    ("background-color: #f0fffc", "background-color: var(--el-color-success-light-9)"),
    ("background: #f0f2f5", "background: var(--el-fill-color-light)"),
    ("background-color: #f0f2f5", "background-color: var(--el-fill-color-light)"),
    ("color: #67c23a", "color: var(--el-color-success)"),
    ("color:#67c23a", "color: var(--el-color-success)"),
    ("color: #f56c6c", "color: var(--el-color-danger)"),
    ("color:#f56c6c", "color: var(--el-color-danger)"),
    ("background-color: #1890ff", "background-color: var(--el-color-primary)"),
    ("border-color: #1890ff", "border-color: var(--el-color-primary)"),
    ("background-color: #40a9ff", "background-color: var(--el-color-primary-light-3)"),
    ("border-color: #40a9ff", "border-color: var(--el-color-primary-light-3)"),
]


def _rewrite_legacy_at_imports(content: str) -> str:
    """Rewrite ops-platform @/… imports to football-front #/…/ops/ paths (single + double quotes)."""
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


def transform_imports(content: str) -> str:
    content = _rewrite_legacy_at_imports(content)
    content = content.replace(
        "import { opsRequest } from '#/api/ops/client'",
        "import { request } from '#/api/ops/client'",
    )
    content = content.replace(
        "import { opsRequest as request } from '#/api/ops/client'",
        "import { request } from '#/api/ops/client'",
    )
    return content


# Standalone Ops paths that must become /ops/… in Football shell (safety net for remount).
# Prefer opsRouteTo() / resolveOpsNavUrl() in source; this catches missed string literals.
OPS_ROUTER_PUSH_RE = re.compile(
    r"router\.(push|replace)\((\{[^}]*path:\s*['\"])(/[^'\"]+)(['\"])"
)


def transform_router_paths(content: str) -> str:
    """Rewrite router.push({ path: '/foo' }) → opsRouteTo({ path: '/foo' }) when not already wrapped."""

    def _wrap_object(match: re.Match[str]) -> str:
        method, prefix, path, quote = match.group(1), match.group(2), match.group(3), match.group(4)
        if path.startswith("/ops/") or path == "/ops" or path.startswith("/login"):
            return match.group(0)
        return f"router.{method}(opsRouteTo({prefix}{path}{quote}"

    if "opsRouteTo" not in content and OPS_ROUTER_PUSH_RE.search(content):
        content = re.sub(
            r"(<script[^>]*>)([\s\S]*?)(</script>)",
            lambda m: (
                m.group(1)
                + (
                    m.group(2)
                    if "from '@/utils/ops-route'" in m.group(2)
                    or 'from "@/utils/ops-route"' in m.group(2)
                    or "from '#/utils/ops/ops-route'" in m.group(2)
                    else "import { opsRouteTo } from '@/utils/ops-route'\n" + m.group(2)
                )
                + m.group(3)
            ),
            content,
            count=1,
        )
    content = OPS_ROUTER_PUSH_RE.sub(_wrap_object, content)

    content = re.sub(
        r"router\.(push|replace)\((['\"])(/[^'\"]+)\2\)",
        lambda m: (
            m.group(0)
            if m.group(3).startswith("/ops/")
            or m.group(3) == "/ops"
            or m.group(3).startswith("/login")
            else f"router.{m.group(1)}(opsRouteTo({m.group(2)}{m.group(3)}{m.group(2)}))"
        ),
        content,
    )
    return content


def transform_api(content: str) -> str:
    content = re.sub(r"from ['\"]@/utils/request['\"]", "from './client'", content)
    content = re.sub(r"from ['\"]@/utils/([^'\"]+)['\"]", r"from '#/utils/ops/\1'", content)
    content = re.sub(r"from ['\"]@/utils['\"]", "from '#/utils/ops/index'", content)
    content = re.sub(r"from ['\"]@/types/([^'\"]+)['\"]", r"from '#/types/ops/\1'", content)
    content = re.sub(r"import\(['\"]@/types/([^'\"]+)['\"]\)", r"import('#/types/ops/\1')", content)
    return content


OPS_PAGE_SKIP_FILES = frozenset(
    {
        "ContentEditPanel.vue",
        "ContentEditDialog.vue",
    }
)

OPS_PAGE_SKIP_CLASS_MARKERS = (
    "content-edit-panel",
    "ops-embedded-panel",
)


def should_add_ops_page(path: Path) -> bool:
    rel = path.as_posix()
    if path.name in OPS_PAGE_SKIP_FILES:
        return False
    if "/components/ops/" in rel:
        return False
    return True


def strip_erroneous_ops_page(content: str) -> str:
    """Remove ops-page from embedded panels that mount regression may have tagged."""
    for marker in OPS_PAGE_SKIP_CLASS_MARKERS:
        content = re.sub(
            rf'class="([^"]*\b{re.escape(marker)}\b[^"]*\b)ops-page(\b[^"]*)"',
            r'class="\1\2"',
            content,
        )
        content = re.sub(
            rf'class="([^"]*\b)ops-page(\b[^"]*\b{re.escape(marker)}\b[^"]*)"',
            r'class="\1\2"',
            content,
        )
        content = re.sub(rf'class="{marker} ops-page"', f'class="{marker} ops-embedded-panel"', content)
        content = re.sub(rf'class="ops-page {marker}"', f'class="{marker} ops-embedded-panel"', content)
    return content


def ensure_ops_page(content: str, *, path: Path | None = None) -> str:
    content = strip_erroneous_ops_page(content)
    if path is not None and not should_add_ops_page(path):
        return content
    if "ops-page" in content:
        return content
    m = re.search(r"(<template>\s*\n?\s*<div)(\s+[^>]*class=\"([^\"]*)\"|)", content)
    if m:
        # Preserve <template> — only rewrite the opening <div> (group 1), not the full match.
        if m.group(2) and "class=" in m.group(2):
            existing = m.group(3) or ""
            if any(marker in existing for marker in OPS_PAGE_SKIP_CLASS_MARKERS):
                return content
            new_class = f"{existing} ops-page".strip()
            content = content[: m.start(1)] + f'<div class="{new_class}"' + content[m.end() :]
        else:
            content = content[: m.start(1)] + '<div class="ops-page"' + content[m.end() :]
    return content


def ensure_root_template(content: str) -> str:
    """Add root <template> if mount regression left only </template> closing tag."""
    stripped = re.sub(r"^(\s*(?:<!--[\s\S]*?-->\s*)*)", "", content, count=1)
    if re.match(r"^\s*<template[\s>]", stripped):
        return content
    m = re.match(r"^(\s*(?:<!--[\s\S]*?-->\s*)*)", content)
    prefix = m.group(1) if m else ""
    rest = content[len(prefix) :]
    return f"{prefix}<template>\n{rest}"


SCRIPT_RE = re.compile(r"(<script[^>]*>)([\s\S]*?)(</script>)")
VUE_IMPORT_RE = re.compile(r"import\s*\{([^}]+)\}\s*from\s*['\"]vue['\"]")
VUE_APIS = ["computed", "ref", "reactive", "watch", "onMounted", "onUnmounted", "onBeforeUnmount"]


def ensure_vue_imports(content: str) -> str:
    """Inject missing Vue API imports when script uses them without import."""
    m = SCRIPT_RE.search(content)
    if not m:
        return content
    script = m.group(2)
    needed = {api for api in VUE_APIS if re.search(rf"\b{api}\b", script)}
    if not needed:
        return content
    import_m = VUE_IMPORT_RE.search(script)
    if import_m:
        imported = {x.strip() for x in import_m.group(1).split(",") if x.strip()}
        missing = needed - imported
        if not missing:
            return content
        names = ", ".join(sorted(imported | missing))
        new_script = script[: import_m.start()] + f"import {{ {names} }} from 'vue'" + script[import_m.end() :]
    else:
        names = ", ".join(sorted(needed))
        new_script = f"import {{ {names} }} from 'vue'\n" + script.lstrip("\n")
    return content[: m.start(2)] + new_script + content[m.end(2) :]


def apply_theme_fixes(content: str) -> str:
    for old, new in COLOR_REWRITES:
        content = content.replace(old, new)
    return content


def copy_transform(src: Path, dst: Path, *, api: bool = False, page: bool = False, theme: bool = False) -> None:
    if not src.exists():
        print(f"  SKIP missing source: {src.relative_to(ROOT)}")
        return
    dst.parent.mkdir(parents=True, exist_ok=True)
    text = src.read_text(encoding="utf-8")
    text = transform_api(text) if api else transform_imports(text)
    if page and src.suffix == ".vue":
        text = transform_router_paths(text)
    if page:
        text = ensure_ops_page(text, path=dst)
        text = ensure_root_template(text)
    if page or theme:
        text = apply_theme_fixes(text)
    if src.suffix == ".vue":
        text = ensure_vue_imports(text)
    dst.write_text(text, encoding="utf-8")


def ops_src_view(component_file: str) -> Path:
    rel = component_file.replace("@/views/", "views/")
    return OPS_UI / rel


def load_csv_rows() -> list[dict[str, str]]:
    with CSV_PATH.open(encoding="utf-8") as f:
        return list(csv.DictReader(f))


def copy_all_apis() -> int:
    count = 0
    for src in sorted((OPS_UI / "api").glob("*.ts")):
        dst = FF / "api/ops" / src.name
        copy_transform(src, dst, api=True)
        print(f"  api: {dst.relative_to(ROOT)}")
        count += 1
    return count


def copy_all_types() -> int:
    count = 0
    for src in sorted((OPS_UI / "types").glob("*.ts")):
        dst = FF / "types/ops" / src.name
        copy_transform(src, dst, theme=True)
        print(f"  type: {dst.relative_to(ROOT)}")
        count += 1
    return count


def copy_all_utils() -> int:
    count = 0
    skip = {"request.ts", "mock-helper.ts"}
    for src in sorted((OPS_UI / "utils").glob("*.ts")):
        if src.name in skip or src.name.endswith(".test.ts"):
            continue
        dst = FF / "utils/ops" / src.name
        copy_transform(src, dst, theme=True)
        print(f"  util: {dst.relative_to(ROOT)}")
        count += 1
    return count


def copy_all_constants() -> int:
    count = 0
    const_root = OPS_UI / "constants"
    if not const_root.exists():
        return 0
    for src in sorted(const_root.rglob("*.ts")):
        rel = src.relative_to(const_root)
        dst = FF / "constants/ops" / rel
        copy_transform(src, dst, theme=True)
        print(f"  constant: {dst.relative_to(ROOT)}")
        count += 1
    return count


def copy_all_composables() -> int:
    count = 0
    comp_root = OPS_UI / "composables"
    if not comp_root.exists():
        return 0
    for src in sorted(comp_root.rglob("*.ts")):
        rel = src.relative_to(comp_root)
        dst = FF / "composables/ops" / rel
        copy_transform(src, dst, theme=True)
        print(f"  composable: {dst.relative_to(ROOT)}")
        count += 1
    return count


def copy_all_mocks() -> int:
    count = 0
    mock_root = OPS_UI / "mock"
    if not mock_root.exists():
        return 0
    for src in sorted(mock_root.rglob("*.ts")):
        rel = src.relative_to(mock_root)
        dst = FF / "mock/ops" / rel
        copy_transform(src, dst, theme=True)
        print(f"  mock: {dst.relative_to(ROOT)}")
        count += 1
    return count


def refresh_ops_views() -> int:
    """Re-apply import/theme transforms to all mounted ops vue files."""
    count = 0
    views_root = FF / "views/ops"
    for dst in sorted(views_root.rglob("*.vue")):
        text = dst.read_text(encoding="utf-8")
        updated = transform_imports(text)
        if dst.suffix == ".vue":
            updated = transform_router_paths(updated)
        updated = ensure_ops_page(updated, path=dst)
        updated = ensure_vue_imports(updated)
        updated = apply_theme_fixes(updated)
        if updated != text:
            dst.write_text(updated, encoding="utf-8")
            count += 1
    return count


def copy_all_components() -> int:
    count = 0
    comp_root = OPS_UI / "components"
    for src in sorted(comp_root.rglob("*")):
        if not src.is_file() or src.suffix not in {".vue", ".ts"}:
            continue
        rel = src.relative_to(comp_root)
        dst = FF / "components/ops" / rel
        copy_transform(src, dst, theme=True)
        print(f"  component: {dst.relative_to(ROOT)}")
        count += 1
    return count


def copy_menu_views(rows: list[dict[str, str]]) -> tuple[int, set[str]]:
    seen: set[str] = set()
    count = 0
    for row in rows:
        comp = row.get("football_component", "").strip()
        if not comp or comp in seen:
            continue
        seen.add(comp)
        src = ops_src_view(row["component_file"].strip())
        dst = FF / "views" / f"{comp}.vue"
        copy_transform(src, dst, page=True)
        print(f"  view: {dst.relative_to(ROOT)}")
        count += 1
    return count, seen


def copy_extra_views(seen_components: set[str]) -> int:
    count = 0
    views_root = OPS_UI / "views"
    for src in sorted(views_root.rglob("*.vue")):
        rel_from_views = src.relative_to(views_root)
        key = f"ops/{rel_from_views.with_suffix('')}".replace("\\", "/")
        dst = FF / "views/ops" / rel_from_views
        if key in seen_components and dst.exists():
            continue
        copy_transform(src, dst, page=True)
        print(f"  extra view: {dst.relative_to(ROOT)}")
        count += 1
        seen_components.add(key)
    return count


def generate_ops_routes(rows: list[dict[str, str]]) -> None:
    lines = [
        "import type { RouteRecordRaw } from 'vue-router';",
        "",
        "/** Ops hide-in-menu / detail routes — generated by scripts/mount-ops-all.py */",
        "const routes: RouteRecordRaw[] = [",
    ]
    hidden = 0
    for row in rows:
        if row.get("hide_in_menu", "").upper() != "Y":
            continue
        if row.get("excluded_m9", "").upper() == "Y":
            continue
        path = row.get("football_path", "").strip()
        comp = row.get("football_component", "").strip()
        name = row.get("component_name", "").strip() or row.get("route_name", "").strip()
        if not path or not comp or not name:
            continue
        hidden += 1
        lines.append("  {")
        lines.append(f"    path: '{path}',")
        lines.append(f"    name: '{name}',")
        lines.append(f"    component: () => import('#/views/{comp}.vue'),")
        lines.append("    meta: { hideInMenu: true, title: '' },")
        lines.append("  },")
    lines.extend(["];", "", "export default routes;", ""])
    out = FF / "router/routes/modules/ops.ts"
    out.write_text("\n".join(lines), encoding="utf-8")
    print(f"  routes: {out.relative_to(ROOT)} ({hidden} hidden)")


def main() -> None:
    rows = load_csv_rows()
    print("=== mount-ops-all: APIs ===")
    api_n = copy_all_apis()
    print(f"\n=== mount-ops-all: types ({api_n} apis) ===")
    type_n = copy_all_types()
    print(f"\n=== mount-ops-all: utils ({type_n} types) ===")
    util_n = copy_all_utils()
    print(f"\n=== mount-ops-all: constants ({util_n} utils) ===")
    const_n = copy_all_constants()
    print(f"\n=== mount-ops-all: composables ({const_n} constants) ===")
    compo_n = copy_all_composables()
    print(f"\n=== mount-ops-all: mocks ({compo_n} composables) ===")
    mock_n = copy_all_mocks()
    print(f"\n=== mount-ops-all: components ({mock_n} mocks) ===")
    comp_n = copy_all_components()
    print(f"\n=== mount-ops-all: menu views ({comp_n} components) ===")
    view_n, seen = copy_menu_views(rows)
    print(f"\n=== mount-ops-all: extra views ({view_n} from CSV) ===")
    extra_n = copy_extra_views(seen)
    print(f"\n=== mount-ops-all: refresh ops views (+{extra_n} extra views) ===")
    refresh_n = refresh_ops_views()
    print(f"  refreshed {refresh_n} vue files")
    print(f"\n=== mount-ops-all: ops.ts routes (+{extra_n} extra views) ===")
    generate_ops_routes(rows)

    vue_count = len(list((FF / "views/ops").rglob("*.vue")))
    menu_vue = len(list((FF / "views").glob("ops/**/*.vue"))) + view_n
    print(f"\nDone. views/ops/**/*.vue: {vue_count}; menu-mapped components: {view_n}")


if __name__ == "__main__":
    main()
