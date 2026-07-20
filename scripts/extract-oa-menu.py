#!/usr/bin/env python3
"""
S2-A: Extract Ops menu/permission map from Layout.vue + router/index.ts.
Outputs:
  - docs/delivery/oa-menu-permission-map.csv
  - scripts/integration-config/seed-oa-system-menu.sql

Usage:
  python scripts/extract-oa-menu.py
"""
from __future__ import annotations

import csv
import re
from dataclasses import dataclass, field
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LAYOUT = ROOT / "ops-platform-ui-vue/src/views/Layout.vue"
ROUTER = ROOT / "ops-platform-ui-vue/src/router/index.ts"
MIGRATIONS = ROOT / "ops-platform-server/ops-platform-module-oa/src/main/resources/db/migration"
CSV_OUT = ROOT / "docs/delivery/oa-menu-permission-map.csv"
SQL_OUT = ROOT / "scripts/integration-config/seed-oa-system-menu.sql"

# M9 identity menus handled by Football system-server (ADR-047 §5.1)
M9_EXCLUDE_PATHS = {"/system-user", "/system-role", "/system-tenant"}

# Layout group index -> title
GROUP_TITLES = {
    "operations": "运营管理",
    "production": "内容生产",
    "performance": "绩效核算",
    "internal": "账号管理",
    "finance": "财务管理",
    "collect": "数据采集",
    "analysis": "数据分析",
    "monitor": "作品监测",
    "config": "配置管理",
    "system": "系统管理(OA)",
}

# Football system_menu directory path slug (unique per group)
GROUP_SLUGS = {
    "首页": "home",
    "运营管理": "operations",
    "内容生产": "production",
    "绩效核算": "performance",
    "账号管理": "internal",
    "财务管理": "finance",
    "数据采集": "collect",
    "数据分析": "analysis",
    "作品监测": "monitor",
    "配置管理": "config",
    "系统管理(OA)": "system-oa",
}

# Explicit route -> oa permission (menu list gate)
ROUTE_PERMISSION: dict[str, str] = {
    "/dashboard": "oa:home:view",
    "/workbench-todos": "oa:home:view",
    "/ip-group": "oa:ip-group:list",
    "/account-analysis": "oa:account-analysis:list",
    "/fans-analysis": "oa:fans-analysis:list",
    "/internal-content": "oa:internal-content:list",
    "/efficiency": "oa:efficiency:list",
    "/sop": "oa:sop:list",
    "/sop/review": "oa:sop:list",
    "/plan": "oa:plan:list",
    "/task": "oa:task:list",
    "/content": "oa:content:list",
    "/content/review": "oa:content:list",
    "/knowledge": "oa:knowledge:list",
    "/layout-template": "oa:layout-template:list",
    "/perf-template": "oa:perf:list",
    "/perf-execution": "oa:perf:list",
    "/perf-result": "oa:perf:list",
    "/order-attribution": "oa:order-attribution:list",
    "/company": "oa:company:list",
    "/realname": "oa:realname:list",
    "/phone": "oa:phone:list",
    "/simcard": "oa:simcard:list",
    "/internal-account": "oa:platform-account:list",
    "/personal-account": "oa:personal-account:list",
    "/triple-rel": "oa:triple-rel:list",
    "/account-cost": "oa:cost:list",
    "/roi-analysis": "oa:roi:list",
    "/collect/task": "oa:collect:task:list",
    "/collect/log": "oa:collect:log:list",
    "/collect/quality": "oa:collect:quality:list",
    "/collect/private-domain-bridge": "oa:collect:bridge:list",
    "/metric": "oa:metric:list",
    "/metric-analysis": "oa:metric-analysis:list",
    "/data-report": "oa:report:list",
    "/financial-analysis": "oa:financial-analysis:list",
    "/funnel-analysis": "oa:funnel-analysis:list",
    "/custom-query": "oa:custom-query:list",
    "/screen": "oa:screen:view",
    "/screen-config": "oa:screen-config:list",
    "/external-account": "oa:external-account:list",
    "/low-score": "oa:low-score:list",
    "/hot-works": "oa:hot-works:list",
    "/high-fans-account": "oa:high-fans:list",
    "/low-fans-account": "oa:low-fans:list",
    "/ip-theme": "oa:ip-theme:list",
    "/config-internal-collect": "oa:config:internal-collect:list",
    "/config-external-collect": "oa:config:external-collect:list",
    "/config-external-data": "oa:config:external-data:list",
    "/config-order-collect": "oa:config:order-collect:list",
    "/config-threshold": "oa:config:threshold:list",
    "/config-ai-model": "oa:config:ai-model:list",
    "/config-ai-prompt": "oa:config:ai-prompt:list",
    "/config-metadata": "oa:metadata:query",
    "/system-param": "oa:param:list",
    "/system-dict": "oa:dict:admin-list",
    "/system-log/operation": "oa:log:operation",
    "/system-log/login": "oa:log:login",
    "/system-message": "oa:message:list",
}

MENU_ID_START = 6100
OPS_ROOT_ID = 6100


@dataclass
class MenuItem:
    route_path: str
    menu_title: str
    parent_group: str
    component_file: str
    permission: str
    route_name: str = ""
    hide_in_menu: bool = False
    in_layout: bool = False
    excluded_m9: bool = False
    football_path: str = ""
    component_name: str = ""


def scan_flyway_permissions() -> dict[str, str]:
    """code -> name from sys_permission inserts."""
    perms: dict[str, str] = {}
    pat = re.compile(r"'(oa:[^']+)',\s*'([^']*)'")
    for sql in sorted(MIGRATIONS.glob("V*.sql")):
        text = sql.read_text(encoding="utf-8")
        if "sys_permission" not in text and "oa:" not in text:
            continue
        for code, name in pat.findall(text):
            if code.startswith("oa:"):
                perms[code] = name
    return perms


def parse_layout() -> dict[str, tuple[str, str]]:
    """path -> (title, parent_group) from Layout.vue menu items."""
    text = LAYOUT.read_text(encoding="utf-8")
    current_group = ""
    items: dict[str, tuple[str, str]] = {}

    # Walk file in order: sub-menu headers set group; menu-item blocks capture index + title.
    pos = 0
    sub_pat = re.compile(r'<el-sub-menu index="([^"]+)"')
    item_pat = re.compile(
        r'<el-menu-item index="([^"]+)"[^>]*>(.*?)</el-menu-item>',
        re.DOTALL,
    )
    title_pat = re.compile(r'<template\s+#title>([^<]+)</template>')

    while pos < len(text):
        sub_m = sub_pat.search(text, pos)
        item_m = item_pat.search(text, pos)
        if sub_m and (not item_m or sub_m.start() < item_m.start()):
            gid = sub_m.group(1)
            current_group = GROUP_TITLES.get(gid, gid)
            pos = sub_m.end()
            continue
        if not item_m:
            break
        path = item_m.group(1)
        body = item_m.group(2)
        title_m = title_pat.search(body)
        title = (title_m.group(1) if title_m else re.sub(r"<[^>]+>", "", body)).strip()
        group = "首页" if path == "/dashboard" else current_group
        items[path] = (title, group)
        pos = item_m.end()
    return items


def parse_router() -> list[dict]:
    """Parse route records from router/index.ts."""
    text = ROUTER.read_text(encoding="utf-8")
    routes: list[dict] = []
    # Match each route block; stop before the next path: to avoid redirect-only blocks
    # (e.g. /data-screen) absorbing a later route's component/meta.
    block_pat = re.compile(
        r"\{\s*path:\s*'([^']+)'(?:,\s*name:\s*'([^']*)')?"
        r"(?:(?!^\s*\{\s*path:).)*?"
        r"component:\s*\(\)\s*=>\s*import\('(@/views/[^']+)'\)"
        r"(?:(?!^\s*\{\s*path:).)*?"
        r"meta:\s*\{([^}]*)\}",
        re.DOTALL | re.MULTILINE,
    )
    for m in block_pat.finditer(text):
        path, name, comp, meta = m.group(1), m.group(2) or "", m.group(3), m.group(4)
        title_m = re.search(r"title:\s*'([^']*)'", meta)
        standalone_m = re.search(r"standalone:\s*true", meta)
        title = title_m.group(1) if title_m else name
        routes.append(
            {
                "path": path,
                "name": name,
                "component": comp,
                "title": title,
                "standalone": bool(standalone_m),
            }
        )
    return routes


def ops_component_path(vue_import: str) -> str:
    """@/views/operations/IpGroup.vue -> ops/operations/IpGroup"""
    p = vue_import.replace("@/views/", "").replace(".vue", "")
    return f"ops/{p}"


def football_route_path(ops_path: str) -> str:
    """Ops /ip-group -> Football /ops/ip-group (hash router)."""
    if ops_path.startswith("/"):
        ops_path = ops_path[1:]
    return f"/ops/{ops_path}" if ops_path else "/ops"


def component_name_from_route(name: str, comp: str) -> str:
    if name:
        return name
    base = Path(comp).stem
    rel = comp.replace("@/views/", "").replace("/", "_").replace(".vue", "")
    return "".join(w.capitalize() for w in re.split(r"[_/]", rel))


def guess_permission(path: str, flyway: dict[str, str]) -> str:
    if path in ROUTE_PERMISSION:
        return ROUTE_PERMISSION[path]
    # try flyway :list suffix match
    slug = path.strip("/").replace("/", "-").replace("_", "-")
    candidate = f"oa:{slug}:list"
    if candidate in flyway:
        return candidate
    # first segment
    seg = path.strip("/").split("/")[0].replace("-", "-")
    candidate2 = f"oa:{seg}:list"
    if candidate2 in flyway:
        return candidate2
    return candidate2 if seg else ""


def build_menu_rows() -> list[MenuItem]:
    layout = parse_layout()
    router_routes = parse_router()
    flyway = scan_flyway_permissions()
    layout_paths = set(layout.keys())

    rows: list[MenuItem] = []
    seen: set[str] = set()

    for r in router_routes:
        path = r["path"]
        if path in seen:
            continue
        seen.add(path)

        if path in M9_EXCLUDE_PATHS:
            rows.append(
                MenuItem(
                    route_path=path,
                    menu_title=layout.get(path, (r["title"], ""))[0],
                    parent_group=layout.get(path, ("", "系统管理(OA)"))[1],
                    component_file=r["component"],
                    permission="",
                    route_name=r["name"],
                    hide_in_menu=True,
                    in_layout=path in layout_paths,
                    excluded_m9=True,
                    football_path=football_route_path(path),
                    component_name=component_name_from_route(r["name"], r["component"]),
                )
            )
            continue

        in_layout = path in layout_paths
        # Layout sidebar items are seeded as visible menus even when routed standalone
        # (e.g. /screen fullscreen page still appears under 数据分析 in Layout.vue).
        hide = not in_layout
        title = layout[path][0] if in_layout else r["title"]
        group = layout[path][1] if in_layout else ""
        perm = guess_permission(path, flyway)

        rows.append(
            MenuItem(
                route_path=path,
                menu_title=title,
                parent_group=group,
                component_file=r["component"],
                permission=perm,
                route_name=r["name"],
                hide_in_menu=bool(hide),
                in_layout=in_layout,
                excluded_m9=False,
                football_path=football_route_path(path),
                component_name=component_name_from_route(r["name"], r["component"]),
            )
        )

    rows.sort(key=lambda x: (x.excluded_m9, x.hide_in_menu, x.parent_group, x.route_path))
    return rows


def write_csv(rows: list[MenuItem]) -> None:
    CSV_OUT.parent.mkdir(parents=True, exist_ok=True)
    with CSV_OUT.open("w", newline="", encoding="utf-8-sig") as f:
        w = csv.writer(f)
        w.writerow(
            [
                "route_path",
                "menu_title",
                "parent_group",
                "component_file",
                "permission",
                "route_name",
                "football_path",
                "football_component",
                "component_name",
                "hide_in_menu",
                "in_layout",
                "excluded_m9",
            ]
        )
        for r in rows:
            w.writerow(
                [
                    r.route_path,
                    r.menu_title,
                    r.parent_group,
                    r.component_file,
                    r.permission,
                    r.route_name,
                    r.football_path,
                    ops_component_path(r.component_file),
                    r.component_name,
                    "Y" if r.hide_in_menu else "N",
                    "Y" if r.in_layout else "N",
                    "Y" if r.excluded_m9 else "N",
                ]
            )


def sql_escape(s: str) -> str:
    return s.replace("'", "''")


def generate_sql(rows: list[MenuItem]) -> str:
    """Generate system_menu + system_role_menu seed (role_id=1 super admin)."""
    menu_rows = [r for r in rows if r.in_layout and not r.excluded_m9 and not r.hide_in_menu]
    group_order: list[str] = []
    for r in menu_rows:
        if r.parent_group and r.parent_group not in group_order:
            group_order.append(r.parent_group)

    lines: list[str] = [
        "-- S2-A: Ops menus for Football system_menu (ADR-047)",
        "-- Generated by scripts/extract-oa-menu.py — idempotent DELETE + INSERT",
        "-- Permission prefix: oa:* ; M9 user/role/tenant excluded",
        "",
        "SET NAMES utf8mb4;",
        "",
        "-- Remove prior Ops menu block (6100-6999)",
        "DELETE FROM system_role_menu WHERE menu_id >= 6100 AND menu_id < 7000;",
        "DELETE FROM system_menu WHERE id >= 6100 AND id < 7000;",
        "",
        "BEGIN;",
        "",
    ]

    next_id = MENU_ID_START
    id_map: dict[str, int] = {}
    role_menu_id = 70000

    # Root directory
    root_id = next_id
    next_id += 1
    lines.append(
        f"INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, "
        f"component, component_name, status, visible, keep_alive, always_show, creator, user_type) VALUES "
        f"({root_id}, '运营数据', '', 1, 5, 0, '/ops', 'ep:data-analysis', NULL, NULL, 0, b'1', b'1', b'1', 'integration', 2);"
    )

    # Group directories under root
    group_ids: dict[str, int] = {}
    for i, group in enumerate(group_order):
        if group == "首页":
            group_ids[group] = root_id
            continue
        gid = next_id
        next_id += 1
        group_ids[group] = gid
        slug = GROUP_SLUGS.get(group) or re.sub(r"[^a-z0-9]+", "-", group.lower())[:30] or f"g{i}"
        lines.append(
            f"INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, "
            f"component, component_name, status, visible, keep_alive, always_show, creator, user_type) VALUES "
            f"({gid}, '{sql_escape(group)}', '', 1, {i + 1}, {root_id}, '{slug}', 'ep:folder', NULL, NULL, "
            f"0, b'1', b'1', b'1', 'integration', 2);"
        )

    # Leaf menus
    sort_counters: dict[int, int] = {}
    menu_ids_for_role: list[int] = [root_id]

    for r in menu_rows:
        parent_group = r.parent_group or "首页"
        parent_id = group_ids.get(parent_group, root_id)
        sort_counters[parent_id] = sort_counters.get(parent_id, 0) + 1
        mid = next_id
        next_id += 1
        id_map[r.route_path] = mid
        menu_ids_for_role.append(mid)

        rel_path = r.route_path.lstrip("/")
        comp = ops_component_path(r.component_file)
        perm = sql_escape(r.permission)
        lines.append(
            f"INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, "
            f"component, component_name, status, visible, keep_alive, always_show, creator, user_type) VALUES "
            f"({mid}, '{sql_escape(r.menu_title)}', '{perm}', 2, {sort_counters[parent_id]}, {parent_id}, "
            f"'{sql_escape(rel_path)}', 'ep:document', '{sql_escape(comp)}', '{sql_escape(r.component_name)}', "
            f"0, b'1', b'1', b'1', 'integration', 2);"
        )

    lines.append("")
    lines.append("-- Grant all Ops menus to super admin (role_id=1, tenant_id=1)")
    for mid in menu_ids_for_role:
        lines.append(
            f"INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id) VALUES "
            f"({role_menu_id}, 1, {mid}, 'integration', 1);"
        )
        role_menu_id += 1

    lines.extend(["", "COMMIT;", ""])
    return "\n".join(lines)


def main() -> None:
    rows = build_menu_rows()
    write_csv(rows)
    SQL_OUT.parent.mkdir(parents=True, exist_ok=True)
    SQL_OUT.write_text(generate_sql(rows), encoding="utf-8")

    menu_count = len([r for r in rows if r.in_layout and not r.excluded_m9 and not r.hide_in_menu])
    excluded = len([r for r in rows if r.excluded_m9])
    hidden = len([r for r in rows if r.hide_in_menu and not r.excluded_m9])
    print(f"CSV: {CSV_OUT} ({len(rows)} routes)")
    print(f"SQL: {SQL_OUT} ({menu_count} visible menus, {hidden} hide-in-menu, {excluded} M9 excluded)")
    print(f"Flyway oa:* permissions scanned: {len(scan_flyway_permissions())}")


if __name__ == "__main__":
    main()
