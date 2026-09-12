#!/usr/bin/env python3
"""Generate ADR-064 six-role RBAC SQL (dict-style: no hardcoded role / role_menu id).

Outputs:
  scripts/integration-config/seed-ops-six-roles-rbac.sql
  scripts/integration-config/ops-greenfield-sources/system/07_ops_six_roles_rbac.sql

Then regenerate 02:
  python scripts/integration-config/gen-ops-greenfield-sql.py --system-seeds-only
"""
from pathlib import Path

INTEG_DIR = Path(__file__).resolve().parent
GREENFIELD_07 = INTEG_DIR / "ops-greenfield-sources" / "system" / "07_ops_six_roles_rbac.sql"
SEED_OUT = INTEG_DIR / "seed-ops-six-roles-rbac.sql"

ROLES = {
    "ip_group_leader": {
        "id": 160,
        "name": "IP组长",
        "sort": 20,
        "data_scope": 5,
        "type": 1,
        "remark": "ADR-064：IP组组长；一级内容审核（本组）",
        "menus": sorted(
            set(
                [
                    6100,
                    6168,
                    6109,
                    6154,
                    6156,
                    6157,
                    6158,
                    6159,
                    6102,
                    6117,
                    6118,
                    6119,
                    6120,
                    6121,
                    6122,
                    6123,
                    6124,
                    6175,
                    6170,
                    6171,
                    6172,
                    6173,
                    6106,
                    6142,
                    6143,
                    6144,
                    6145,
                    6108,
                    6148,
                    6149,
                    6150,
                    6151,
                    6152,
                    6153,
                    6174,
                    6107,
                    6146,
                    6147,
                    6103,
                    6126,
                    6128,
                    6130,
                    6101,
                    6112,
                    6113,
                    6114,
                    6115,
                    6116,
                ]
            )
        ),
    },
    "ops_manager": {
        "id": 161,
        "name": "运营主管",
        "sort": 21,
        "data_scope": 1,
        "type": 2,
        "remark": "ADR-064：运营主管；二级内容审核；租户 ALL",
        "menus": sorted(
            set(
                [
                    6100,
                    6168,
                    6109,
                    6154,
                    6156,
                    6157,
                    6158,
                    6159,
                    6102,
                    6117,
                    6118,
                    6119,
                    6120,
                    6121,
                    6122,
                    6123,
                    6124,
                    6175,
                    6170,
                    6171,
                    6172,
                    6173,
                    6106,
                    6142,
                    6143,
                    6144,
                    6145,
                    6108,
                    6148,
                    6149,
                    6150,
                    6151,
                    6152,
                    6153,
                    6174,
                    6107,
                    6146,
                    6147,
                    6103,
                    6125,
                    6126,
                    6127,
                    6128,
                    6129,
                    6130,
                    6131,
                    6132,
                    6101,
                    6111,
                    6112,
                    6113,
                    6114,
                    6115,
                    6116,
                    6110,
                    6160,
                    6161,
                    6162,
                    6163,
                    6164,
                    6165,
                    6166,
                    6167,
                    6105,
                    6140,
                    6141,
                    6104,
                    6133,
                    6136,
                ]
            )
        ),
    },
    "finance": {
        "id": 162,
        "name": "财务人员",
        "sort": 22,
        "data_scope": 1,
        "type": 2,
        "remark": "ADR-064：财务域；成本/ROI/绩效结果",
        "menus": sorted(
            set(
                [
                    6100,
                    6168,
                    6107,
                    6146,
                    6147,
                    6103,
                    6126,
                    6127,
                    6106,
                    6142,
                    6143,
                    6144,
                    6109,
                    6154,
                    6156,
                    6157,
                    6158,
                    6108,
                    6148,
                    6149,
                    6150,
                    6151,
                    6152,
                    6153,
                    6174,
                    6102,
                    6117,
                    6101,
                    6111,
                    6112,
                    6113,
                    6114,
                    6115,
                    6116,
                ]
            )
        ),
    },
    "content_editor": {
        "id": 163,
        "name": "内容编辑",
        "sort": 23,
        "data_scope": 5,
        "type": 2,
        "remark": "ADR-064：内容编辑；SELF+本组只读；不审（无6118）",
        "menus": sorted(
            set(
                [
                    6100,
                    6168,
                    6102,
                    6117,
                    6119,
                    6120,
                    6121,
                    6124,
                    6109,
                    6154,
                    6157,
                    6158,
                    6103,
                    6125,
                    6128,
                    6108,
                    6148,
                    6149,
                    6150,
                    6151,
                    6152,
                    6153,
                    6174,
                    6101,
                    6112,
                    6113,
                    6114,
                    6115,
                    6116,
                ]
            )
        ),
    },
    "ops_operator": {
        "id": 164,
        "name": "运营",
        "sort": 24,
        "data_scope": 5,
        "type": 2,
        "remark": "ADR-064：运营（含主播/快手）；IP_GROUP+SELF；无审核/无全部任务",
        "menus": sorted(
            set(
                [
                    6100,
                    6168,
                    6109,
                    6154,
                    6156,
                    6157,
                    6158,
                    6102,
                    6117,
                    6119,
                    6120,
                    6121,
                    6122,
                    6124,
                    6106,
                    6143,
                    6144,
                    6108,
                    6148,
                    6149,
                    6150,
                    6151,
                    6152,
                    6153,
                    6174,
                    6101,
                    6112,
                    6113,
                    6114,
                    6115,
                    6116,
                    6107,
                    6146,
                    6147,
                ]
            )
        ),
    },
    "data_analyst": {
        "id": 165,
        "name": "数据分析",
        "sort": 25,
        "data_scope": 1,
        "type": 2,
        "remark": "ADR-064：分析域 ALL；监测/报表 RWD；采集 R；无内容审核",
        "menus": sorted(
            set(
                [
                    6100,
                    6168,
                    6103,
                    6125,
                    6126,
                    6127,
                    6128,
                    6129,
                    6130,
                    6131,
                    6132,
                    6101,
                    6111,
                    6112,
                    6113,
                    6114,
                    6115,
                    6116,
                    6109,
                    6154,
                    6156,
                    6157,
                    6158,
                    6159,
                    6108,
                    6148,
                    6149,
                    6150,
                    6151,
                    6152,
                    6153,
                    6174,
                    6102,
                    6117,
                    6119,
                    6120,
                    6121,
                    6122,
                    6124,
                    6106,
                    6142,
                    6143,
                    6144,
                    6145,
                    6107,
                    6146,
                    6147,
                    6110,
                    6165,
                    6104,
                    6133,
                    6136,
                ]
            )
        ),
    },
}


def _sql_int_list(ids: list[int], indent: str = "    ") -> str:
    chunks: list[str] = []
    for i in range(0, len(ids), 10):
        chunks.append(indent + ", ".join(str(x) for x in ids[i : i + 10]))
    return ",\n".join(chunks)


def _assert_matrix() -> None:
    assert 6118 not in ROLES["content_editor"]["menus"]
    assert 6156 not in ROLES["content_editor"]["menus"]
    assert 6175 in ROLES["ip_group_leader"]["menus"]
    assert 6175 in ROLES["ops_manager"]["menus"]
    for code in ("finance", "content_editor", "ops_operator", "data_analyst"):
        assert 6175 not in ROLES[code]["menus"]
        assert 6118 not in ROLES[code]["menus"]
        assert 6134 not in ROLES[code]["menus"]
        assert 6135 not in ROLES[code]["menus"]


def _role_body() -> list[str]:
    """Dict-style role + role_menu: no hardcoded id; identity = code; bind via JOIN."""
    lines: list[str] = ["BEGIN;", ""]
    for code, r in ROLES.items():
        lines.append(f"-- ===== {r['name']} ({code}) menus={len(r['menus'])} =====")
        lines.extend(
            [
                "INSERT INTO system_role (",
                "    name, code, sort, data_scope, data_scope_dept_ids, status, type, remark,",
                "    creator, create_time, updater, update_time, deleted, tenant_id",
                ")",
                "SELECT",
                f"    '{r['name']}', '{code}', {r['sort']}, {r['data_scope']}, '', 0, {r['type']},",
                f"    '{r['remark']}',",
                "    'adr-064-seed', NOW(), 'adr-064-seed', NOW(), b'0', 1",
                "FROM DUAL",
                "WHERE NOT EXISTS (",
                f"    SELECT 1 FROM system_role x WHERE x.code = '{code}' AND x.tenant_id = 1 AND x.deleted = b'0'",
                ");",
                "",
                "DELETE rm FROM system_role_menu rm",
                "INNER JOIN system_role r ON r.id = rm.role_id",
                f"WHERE r.code = '{code}' AND r.tenant_id = 1 AND r.deleted = b'0'",
                "  AND rm.menu_id >= 6100 AND rm.menu_id < 7000",
                "  AND rm.menu_id NOT IN (6194, 6195, 6196);  -- preserve work-task (03_work_task_menus_v183)",
                "",
                "INSERT INTO system_role_menu (role_id, menu_id, creator, tenant_id, user_type)",
                "SELECT r.id, m.id, 'adr-064-seed', 1, 2",
                "FROM system_role r",
                "INNER JOIN system_menu m ON m.id IN (",
                _sql_int_list(r["menus"]),
                ")",
                f"WHERE r.code = '{code}' AND r.tenant_id = 1 AND r.deleted = b'0'",
                "  AND m.deleted = b'0'",
                "  AND NOT EXISTS (",
                "      SELECT 1 FROM system_role_menu rm",
                "      WHERE rm.role_id = r.id AND rm.menu_id = m.id AND rm.deleted = b'0'",
                "  );",
                "",
            ]
        )
    lines.extend(["COMMIT;", "", "-- Expected menu counts (ADR-064 §5):"])
    for code, r in ROLES.items():
        lines.append(f"--   {code}: {len(r['menus'])}")
    return lines


SEED_HEADER = [
    "-- ADR-064: OPS six business roles + system_role_menu (exclude super_admin)",
    "-- Apply AFTER seed-oa-system-menu.sql (utf8mb4 stdin via apply-seed-oa-menu.py)",
    "-- Target: Football shenyu-system.system_role / system_role_menu",
    "-- Pattern: dict-style INSERT (no id; AUTO_INCREMENT) WHERE NOT EXISTS by code + tenant_id + deleted=0.",
    "-- role_menu: INSERT … SELECT r.id, m.id JOIN by code / menu id; no hardcoded role_id or role_menu.id.",
    "-- Idempotent: skip existing same-code roles (do not overwrite Football); rebuild Ops menu binds;",
    "--             preserves work-task role_menu 6194-6196 (V183 / 03_work_task_menus_v183).",
    "-- Historical: older packs used preferred ids 160–165; re-run binds those rows by code.",
    "SET NAMES utf8mb4;",
    "",
]

GREENFIELD_HEADER = [
    "-- =============================================================================",
    "-- System DB ({{SYSTEM_DB_NAME}}) — ADR-064 Ops 六业务角色 + system_role_menu",
    "-- Generated: by _gen_seed_ops_six_roles.py — do not hand-edit",
    "-- 目标: {{SYSTEM_DB_HOST}}/{{SYSTEM_DB_NAME}}",
    "-- 前置: 01_baseline_ops_menus.sql + 03_work_task_menus_v183.sql",
    "-- Pattern: dict-style — 不写 system_role.id / system_role_menu.id（AUTO_INCREMENT）；",
    "--          角色身份 = code；role_menu 按 r.code JOIN m.id 绑定。",
    "-- 幂等: 同 code 已存在则跳过角色插入（不覆盖 Football）；重建 ADR-064 菜单绑定；",
    "--       保留 6194-6196 工作任务 role_menu（由 03 写入）。",
    "-- 角色: ip_group_leader / ops_manager / finance / content_editor / ops_operator / data_analyst",
    "-- =============================================================================",
    "SET NAMES utf8mb4;",
    "",
    "",
]


def main() -> None:
    _assert_matrix()
    body = _role_body()
    seed_sql = "\n".join(SEED_HEADER + body) + "\n"
    green_sql = "\n".join(GREENFIELD_HEADER + body) + "\n"
    SEED_OUT.write_text(seed_sql, encoding="utf-8")
    GREENFIELD_07.write_text(green_sql, encoding="utf-8")
    print(f"wrote {SEED_OUT} ({SEED_OUT.stat().st_size} bytes)")
    print(f"wrote {GREENFIELD_07} ({GREENFIELD_07.stat().st_size} bytes)")
    for code, r in ROLES.items():
        print(f"  {code}: menus={len(r['menus'])} (legacy preferred id {r['id']} unused)")


if __name__ == "__main__":
    main()
