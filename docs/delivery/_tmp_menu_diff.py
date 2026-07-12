# -*- coding: utf-8 -*-
import re
from pathlib import Path
from collections import defaultdict

ROOT = Path(r"d:\self\sy\运营数据平台\202606\wd")

def parse_values_line(line):
    # VALUES form: INSERT INTO `system_menu` VALUES (...)
    m = re.search(r"VALUES\s*\((.+)\)\s*;?\s*$", line)
    if not m:
        return None
    body = m.group(1)
    # split first fields: id, name, permission, type, sort, parent_id, path
    m2 = re.match(
        r"(\d+)\s*,\s*'((?:[^'\\]|\\.)*)'\s*,\s*'((?:[^'\\]|\\.)*)'\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*'((?:[^'\\]|\\.)*)'",
        body,
    )
    if not m2:
        return None
    return dict(
        id=int(m2.group(1)),
        name=m2.group(2).replace("\\'", "'"),
        permission=m2.group(3),
        type=int(m2.group(4)),
        sort=int(m2.group(5)),
        parent_id=int(m2.group(6)),
        path=m2.group(7),
    )

def extract_rows(path):
    rows = {}
    for line in Path(path).read_text(encoding="utf-8", errors="replace").splitlines():
        if "system_menu" not in line or "INSERT" not in line:
            continue
        r = parse_values_line(line)
        if r:
            rows[r["id"]] = r
    return rows

baseline = extract_rows(ROOT / "scripts/integration-config/import-football-system-tables.sql")
latest = extract_rows(ROOT / "docs/sql/shenyu-system0708.sql")

b = {k: v for k, v in baseline.items() if k < 6100}
l = {k: v for k, v in latest.items() if k < 6100}
added_ids = sorted(set(l) - set(b))

print(f"baseline football menus: {len(b)}")
print(f"dump football menus: {len(l)}")
print(f"added (dump - import baseline): {len(added_ids)}")

# roots in latest
roots = {k: v for k, v in l.items() if v["parent_id"] == 0 and v["type"] in (1, 2)}

def root_of(mid):
    cur = mid
    for _ in range(20):
        if cur in roots:
            return cur
        if cur not in l:
            return None
        cur = l[cur]["parent_id"]
        if cur == 0:
            return mid if mid in roots else None
    return None

by_root = defaultdict(list)
for mid in added_ids:
    rid = root_of(mid)
    by_root[rid].append(mid)

# dirs/menus only (type 1 or 2), not buttons (3)
for rid in sorted(by_root, key=lambda r: (-len(by_root[r]), r or 0)):
    root_name = l[rid]["name"] if rid and rid in l else "(orphan)"
    root_path = l[rid]["path"] if rid and rid in l else ""
    all_added = by_root[rid]
    menus = [mid for mid in all_added if l[mid]["type"] in (1, 2)]
    buttons = len(all_added) - len(menus)
    print(f"\n## {root_name} | {root_path} | +{len(all_added)} total ({len(menus)} 目录/菜单, {buttons} 按钮)")
    for mid in sorted(menus, key=lambda x: (l[x]["parent_id"], l[x]["sort"], x))[:12]:
        r = l[mid]
        pname = l[r["parent_id"]]["name"] if r["parent_id"] in l else str(r["parent_id"])
        print(f"   - [{mid}] {r['name']}  ({r['path']}) parent={pname}")
    if len(menus) > 12:
        print(f"   ... 另有 {len(menus)-12} 个目录/菜单项")

# write full added ids for parent
out = ROOT / "docs/delivery/tmp-v137-added-menu-ids.txt"
out.write_text("\n".join(str(i) for i in added_ids), encoding="utf-8")
print(f"\nWrote ids: {out}")
