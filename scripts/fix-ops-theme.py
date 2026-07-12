#!/usr/bin/env python3

"""Batch-fix hardcoded light-theme colors in football-front Ops views/components (S4-fix5)."""

from __future__ import annotations



import re

from pathlib import Path



ROOT = Path(__file__).resolve().parents[1]

TARGET_DIRS = [

    ROOT / "football-front/apps/web-ele/src/views/ops",

    ROOT / "football-front/apps/web-ele/src/components/ops",

]



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



# Order: longer / more specific patterns first

COLOR_REWRITES: list[tuple[str, str]] = [

    ("background-color: #ffffff", "background-color: var(--el-bg-color)"),

    ("background: #ffffff", "background: var(--el-bg-color)"),

    ("background-color: #fff", "background-color: var(--el-bg-color)"),

    ("background: #fff", "background: var(--el-bg-color)"),

    ("background-color: white", "background-color: var(--el-bg-color)"),

    ("background: white", "background: var(--el-bg-color)"),

    ("background-color: #f5f7fa", "background-color: var(--el-fill-color-light)"),

    ("background: #f5f7fa", "background: var(--el-fill-color-light)"),

    ("background-color: #fafafa", "background-color: var(--el-fill-color-lighter)"),

    ("background: #fafafa", "background: var(--el-fill-color-lighter)"),

    ("background-color: #f0f2f5", "background-color: var(--el-fill-color-light)"),

    ("background: #f0f2f5", "background: var(--el-fill-color-light)"),

    ("color: #303133", "color: var(--el-text-color-primary)"),

    ("color: #606266", "color: var(--el-text-color-regular)"),

    ("color: #909399", "color: var(--el-text-color-secondary)"),

    ("color: #c0c4cc", "color: var(--el-text-color-placeholder)"),

    ("border: 1px solid #ebeef5", "border: 1px solid var(--el-border-color-lighter)"),

    ("border-bottom: 1px solid #ebeef5", "border-bottom: 1px solid var(--el-border-color-lighter)"),

    ("border-top: 1px solid #ebeef5", "border-top: 1px solid var(--el-border-color-lighter)"),

    ("border-bottom: 1px solid #f0f2f5", "border-bottom: 1px solid var(--el-border-color-lighter)"),

    ("border: 1px solid #e4e7ed", "border: 1px solid var(--el-border-color)"),

    ("border-color: #ebeef5", "border-color: var(--el-border-color-lighter)"),

    ("border-color: #e4e7ed", "border-color: var(--el-border-color)"),

    ("background-color: #1890ff", "background-color: var(--el-color-primary)"),

    ("border-color: #1890ff", "border-color: var(--el-color-primary)"),

    ("background-color: #40a9ff", "background-color: var(--el-color-primary-light-3)"),

    ("border-color: #40a9ff", "border-color: var(--el-color-primary-light-3)"),

]



INLINE_STYLE_RE = re.compile(

    r'style="float:\s*right;\s*color:\s*#909399;\s*font-size:\s*12px(?:;\s*margin-left:\s*12px)?"'

)





def should_add_ops_page(path: Path) -> bool:

    rel = path.as_posix()

    if path.name in OPS_PAGE_SKIP_FILES:

        return False

    if "/components/ops/" in rel:

        return False

    return True





def strip_erroneous_ops_page(content: str) -> str:

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





def fix_inline_styles(content: str) -> str:

    return INLINE_STYLE_RE.sub('class="ops-option-meta"', content)





def apply_rewrites(content: str) -> tuple[str, int]:

    changes = 0

    for old, new in COLOR_REWRITES:

        if old in content:

            count = content.count(old)

            content = content.replace(old, new)

            changes += count

    before = content

    content = fix_inline_styles(content)

    if content != before:

        changes += 1

    return content, changes





def ensure_ops_page(content: str, path: Path) -> tuple[str, bool]:

    content = strip_erroneous_ops_page(content)

    if not should_add_ops_page(path):

        return content, False

    if "ops-page" in content:

        return content, False

    m = re.search(r"(<template>\s*\n?\s*<div)(\s+[^>]*class=\"([^\"]*)\"|)", content)

    if not m:

        return content, False

    if m.group(2) and "class=" in m.group(2):

        existing = m.group(3) or ""

        if any(marker in existing for marker in OPS_PAGE_SKIP_CLASS_MARKERS):

            return content, False

        new_class = f"{existing} ops-page".strip()

        content = content[: m.start(1)] + f'<div class="{new_class}"' + content[m.end() :]

    else:

        content = content[: m.start(1)] + '<div class="ops-page"' + content[m.end() :]

    return content, True





def main() -> None:

    total_files = 0

    total_changes = 0

    ops_page_added = 0



    for base in TARGET_DIRS:

        if not base.exists():

            print(f"SKIP missing: {base}")

            continue

        for path in sorted(base.rglob("*")):

            if path.suffix not in {".vue", ".scss", ".ts"}:

                continue

            text = path.read_text(encoding="utf-8-sig")

            new_text, changes = apply_rewrites(text)

            if path.suffix == ".vue":

                new_text, added = ensure_ops_page(new_text, path)

                if added:

                    ops_page_added += 1

                    changes += 1

            if new_text != text:

                path.write_text(new_text, encoding="utf-8")

                rel = path.relative_to(ROOT)

                print(f"  fixed ({changes}): {rel}")

                total_files += 1

                total_changes += changes



    print(f"\nDone. files={total_files}, rewrites={total_changes}, ops-page added={ops_page_added}")





if __name__ == "__main__":

    main()

