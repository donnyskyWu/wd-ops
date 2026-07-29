#!/usr/bin/env python3
"""
Generate docs/delivery/OPS-MENU-ROUTE-INDEX.md from:
  - scripts/integration-config/seed-oa-system-menu.sql (menu tree, nested Football path)
  - docs/delivery/oa-menu-permission-map.csv (standalone route_path, football_path, component)

Usage:
  python scripts/generate-ops-menu-route-index.py
"""
from __future__ import annotations

import csv
import re
from dataclasses import dataclass
from datetime import date
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SQL = ROOT / "scripts/integration-config/seed-oa-system-menu.sql"
CSV = ROOT / "docs/delivery/oa-menu-permission-map.csv"
OUT = ROOT / "docs/delivery/OPS-MENU-ROUTE-INDEX.md"

MENU_INSERT = re.compile(
    r"INSERT INTO system_menu \(id, name, permission, type, sort, parent_id, path, icon, "
    r"component, component_name, status, visible, keep_alive, always_show, creator, user_type\) VALUES "
    r"\((\d+), '((?:[^']|'')*)', '((?:[^']|'')*)', (\d+), \d+, (\d+), "
    r"'((?:[^']|'')*)', '[^']*', (?:'((?:[^']|'')*)'|NULL), (?:'((?:[^']|'')*)'|NULL),"
)


@dataclass
class MenuRow:
    id: int
    name: str
    permission: str
    type: int
    parent_id: int
    path: str
    component: str
    component_name: str


@dataclass
class CsvRow:
    route_path: str
    football_path: str
    football_component: str
    permission: str
    menu_title: str
    parent_group: str
    hide_in_menu: bool


def unescape_sql(s: str) -> str:
    return s.replace("''", "'")


def parse_seed_menus() -> dict[int, MenuRow]:
    menus: dict[int, MenuRow] = {}
    for line in SQL.read_text(encoding="utf-8").splitlines():
        if "INSERT INTO system_menu" not in line:
            continue
        m = MENU_INSERT.search(line)
        if not m:
            continue
        mid = int(m.group(1))
        if mid < 6100 or mid >= 7000:
            continue
        comp = unescape_sql(m.group(7)) if m.group(7) else ""
        comp_name = unescape_sql(m.group(8)) if m.group(8) else ""
        menus[mid] = MenuRow(
            id=mid,
            name=unescape_sql(m.group(2)),
            permission=unescape_sql(m.group(3)),
            type=int(m.group(4)),
            parent_id=int(m.group(5)),
            path=unescape_sql(m.group(6)),
            component=comp if comp.upper() != "NULL" else "",
            component_name=comp_name if comp_name.upper() != "NULL" else "",
        )
    return menus


def _csv_row_from_dict(row: dict[str, str]) -> CsvRow:
    return CsvRow(
        route_path=row.get("route_path", "").strip(),
        football_path=row.get("football_path", "").strip(),
        football_component=row.get("football_component", "").strip(),
        permission=row.get("permission", "").strip(),
        menu_title=row.get("menu_title", "").strip(),
        parent_group=row.get("parent_group", "").strip(),
        hide_in_menu=row.get("hide_in_menu", "N").strip().upper() == "Y",
    )


def _csv_row_score(cr: CsvRow, menu: MenuRow | None) -> tuple[int, int, int]:
    """Higher is better: visible sidebar row > static path > shorter path."""
    visible = 0 if cr.hide_in_menu else 1
    static = 1 if ":" not in cr.route_path else 0
    comp_match = 1 if menu and menu.component and cr.football_component == menu.component else 0
    return (visible, static, comp_match)


def parse_csv() -> tuple[dict[int, list[CsvRow]], dict[str, CsvRow]]:
    by_id_lists: dict[int, list[CsvRow]] = {}
    by_component_name: dict[str, CsvRow] = {}
    with CSV.open(encoding="utf-8-sig", newline="") as f:
        for row in csv.DictReader(f):
            cr = _csv_row_from_dict(row)
            menu_id_raw = row.get("menu_id", "").strip()
            if menu_id_raw.isdigit():
                by_id_lists.setdefault(int(menu_id_raw), []).append(cr)
            comp_name = row.get("component_name", "").strip()
            if comp_name:
                by_component_name[comp_name] = cr
    return by_id_lists, by_component_name


def pick_csv_for_menu(menu: MenuRow, by_id_lists: dict[int, list[CsvRow]], by_comp: dict[str, CsvRow]) -> CsvRow | None:
    if menu.component_name and menu.component_name in by_comp:
        return by_comp[menu.component_name]
    candidates = by_id_lists.get(menu.id, [])
    if not candidates:
        return None
    return max(candidates, key=lambda cr: _csv_row_score(cr, menu))


def compute_football_path(menu_id: int, menus: dict[int, MenuRow]) -> str:
    chain: list[MenuRow] = []
    mid = menu_id
    while mid in menus:
        chain.append(menus[mid])
        if menus[mid].parent_id == 0:
            break
        mid = menus[mid].parent_id
    chain.reverse()

    parts: list[str] = []
    for m in chain:
        p = (m.path or "").strip()
        if not p:
            continue
        if m.parent_id == 0 and p.startswith("/"):
            parts = [p.lstrip("/")]
        else:
            parts.extend(p.split("/"))
    return "/" + "/".join(parts) if parts else ""


def module_name(menu_id: int, menus: dict[int, MenuRow]) -> str:
    if menu_id == 6100:
        return "根目录"
    if menu_id == 6168:
        return "首页"
    mid = menu_id
    while mid in menus:
        m = menus[mid]
        if m.parent_id == 6100 and m.type == 1:
            return m.name
        if m.parent_id == 0:
            break
        mid = m.parent_id
    # Walk to nearest type=1 ancestor under 6100
    mid = menu_id
    while mid in menus:
        m = menus[mid]
        if m.parent_id in menus and menus[m.parent_id].parent_id == 6100 and menus[m.parent_id].type == 1:
            return menus[m.parent_id].name
        if m.parent_id == 0:
            break
        mid = m.parent_id
    return "-"


def resolve_csv(
    menu: MenuRow, by_id_lists: dict[int, list[CsvRow]], by_comp: dict[str, CsvRow]
) -> CsvRow | None:
    return pick_csv_for_menu(menu, by_id_lists, by_comp)


def e2e_note(menu: MenuRow, computed: str, csv_row: CsvRow | None, mismatch: bool) -> str:
    if menu.type == 3:
        return "按钮权限，无页面路由"
    if menu.type == 1:
        return "目录节点"
    if mismatch and csv_row:
        return f"⚠️ 待修正 CSV={csv_row.football_path}"
    if mismatch:
        return "⚠️ 待修正"
    return ""


def md_escape(s: str) -> str:
    return s.replace("|", "\\|")


def generate() -> tuple[str, int, int]:
    menus = parse_seed_menus()
    by_id_lists, by_comp = parse_csv()

    rows: list[dict] = []
    mismatch_count = 0

    for mid in sorted(menus):
        menu = menus[mid]
        computed = compute_football_path(mid, menus)
        csv_row = resolve_csv(menu, by_id_lists, by_comp)

        csv_football = csv_row.football_path if csv_row else ""
        standalone = csv_row.route_path if csv_row else ""
        component = menu.component or (csv_row.football_component if csv_row else "")
        permission = menu.permission or (csv_row.permission if csv_row else "")

        mismatch = False
        if menu.type == 2 and csv_football and csv_football != computed:
            mismatch = True
            mismatch_count += 1

        rows.append(
            {
                "menu_id": mid,
                "name": menu.name,
                "module": module_name(mid, menus),
                "seed_path": menu.path,
                "football": computed,
                "standalone": standalone or "-",
                "component": component or "-",
                "permission": permission or "-",
                "e2e": e2e_note(menu, computed, csv_row, mismatch),
            }
        )

    today = date.today().strftime("%Y-%m-%d")
    lines = [
        "# OPS 菜单 → 路由索引（Football :5777）",
        "",
        f"> **生成日期**：{today}  ",
        "> **SSOT 来源**：`scripts/integration-config/seed-oa-system-menu.sql`（菜单树 parent_id + path）· "
        "`docs/delivery/oa-menu-permission-map.csv`（standalone / CSV football_path）  ",
        "> **重生成**：`python scripts/generate-ops-menu-route-index.py`",
        "",
        "## 统计",
        "",
        f"- 菜单条目（6100–6999）：**{len(rows)}**",
        f"- CSV `football_path` 与 seed 嵌套路由不一致（type=2 页面）：**{mismatch_count}**（见 E2E 备注列 ⚠️）",
        "",
        "## 路由计算规则",
        "",
        "从 `6100 运营数据`（path=`/ops`）沿 parent 链向下拼接各节点 `path` 段：",
        "",
        "- 目录 type=1、页面 type=2 均参与拼接",
        "- 示例：`6110 config` + `6165 config-metadata` → `/ops/config/config-metadata`",
        "- **非** CSV 中扁平 `/ops/config-metadata`（除非与 seed 一致）",
        "- Standalone（`:3000`）路由取自 CSV `route_path`，通常为扁平路径（不含分组前缀）",
        "",
        "## 全量索引",
        "",
        "| menu_id | 名称 | 模块 | seed path | Football 完整路由 | standalone 路由 | component | permission | E2E 备注 |",
        "| ---: | --- | --- | --- | --- | --- | --- | --- | --- |",
    ]

    for r in rows:
        lines.append(
            "| {menu_id} | {name} | {module} | `{seed_path}` | `{football}` | `{standalone}` | "
            "`{component}` | `{permission}` | {e2e} |".format(
                menu_id=r["menu_id"],
                name=md_escape(r["name"]),
                module=md_escape(r["module"]),
                seed_path=md_escape(r["seed_path"]),
                football=md_escape(r["football"]),
                standalone=md_escape(r["standalone"]),
                component=md_escape(r["component"]),
                permission=md_escape(r["permission"]),
                e2e=md_escape(r["e2e"]),
            )
        )

    if mismatch_count:
        lines.extend(
            [
                "",
                "## ⚠️ 待修正清单（CSV football_path ≠ seed 嵌套路由）",
                "",
                "| menu_id | 名称 | seed 嵌套路由 | CSV football_path |",
                "| ---: | --- | --- | --- |",
            ]
        )
        for r in rows:
            if "⚠️" in r["e2e"]:
                csv_fp = ""
                cr = resolve_csv(menus[r["menu_id"]], by_id_lists, by_comp)
                if cr:
                    csv_fp = cr.football_path
                lines.append(
                    f"| {r['menu_id']} | {md_escape(r['name'])} | `{r['football']}` | `{csv_fp}` |"
                )

    lines.extend(
        [
            "",
            "## 使用说明（E2E Agent）",
            "",
            "1. **Gate 环境**：Football 壳 `http://localhost:5777/#` + 本表「Football 完整路由」列（非 Standalone `:3000`）。",
            "2. **写 Playwright 导航**：`page.goto('http://localhost:5777/#' + football_route)` 或侧栏点击后断言 URL 含该路径。",
            "3. **权限探针**：同路径可对照 `permission` 列与 Dev Token 角色菜单是否一致。",
            "4. **路径不一致**：E2E 备注含 ⚠️ 时，以 **seed 嵌套路由** 为准写用例；同步提缺陷修正 CSV / Football 路由注册。",
            "5. **隐藏/详情页**：本表仅含 system_menu 6100–6999；子页面（如 `/content/edit`）见 "
            "[`oa-menu-permission-map.csv`](./oa-menu-permission-map.csv) 中 `hide_in_menu=Y` 行。",
            "",
            "## 一级分组速查（6101–6110）",
            "",
            "| menu_id | 分组 | path 段 |",
            "| ---: | --- | --- |",
        ]
    )
    for mid in range(6101, 6111):
        if mid in menus:
            m = menus[mid]
            lines.append(f"| {mid} | {m.name} | `{m.path}` |")

    lines.append("")
    return "\n".join(lines), len(rows), mismatch_count


def main() -> None:
    content, total, mismatches = generate()
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(content, encoding="utf-8")
    print(f"Wrote {OUT}")
    print(f"  menus: {total}")
    print(f"  path mismatches (type=2): {mismatches}")


if __name__ == "__main__":
    main()
