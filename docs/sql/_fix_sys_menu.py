import re
from pathlib import Path
src = Path(r"d:\self\sy\运营数据平台\202606\wd\docs\sql\sys_menu.sql")
out = Path(r"d:\self\sy\运营数据平台\202606\wd\docs\sql\_sys_menu_fixed.sql")

def split_values(body: str) -> list[str]:
    parts = []
    cur = []
    in_str = False
    i = 0
    while i < len(body):
        ch = body[i]
        if in_str:
            cur.append(ch)
            if ch == "'" and (i + 1 >= len(body) or body[i + 1] != "'"):
                in_str = False
            elif ch == "'" and i + 1 < len(body) and body[i + 1] == "'":
                cur.append(body[i + 1])
                i += 1
            i += 1
            continue
        if ch == "'":
            in_str = True
            cur.append(ch)
            i += 1
            continue
        if ch == ",":
            parts.append("".join(cur).strip())
            cur = []
            i += 1
            continue
        cur.append(ch)
        i += 1
    if cur:
        parts.append("".join(cur).strip())
    return parts

cols = "(id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, user_type, creator, create_time, updater, update_time, deleted)"
lines = []
for line in src.read_text(encoding="utf-8").splitlines():
    m = re.search(r"VALUES\s*\((.*)\)\s*;?\s*$", line)
    if not m:
        continue
    parts = split_values(m.group(1))
    if len(parts) != 20:
        raise SystemExit(f"bad count {len(parts)}")
    # dump order: ... status(10), user_type(11), visible(12), keep_alive(13), always_show(14)
    reordered = parts[:11] + [parts[12], parts[13], parts[14], parts[11]] + parts[15:]
    lines.append("INSERT INTO system_menu " + cols + " VALUES (" + ", ".join(reordered) + ");")

out.write_text("SET NAMES utf8mb4;\n" + "\n".join(lines) + "\n", encoding="utf-8")
print("fixed", len(lines))
