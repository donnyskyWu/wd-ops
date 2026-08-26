#!/usr/bin/env python3
"""Generate seed-ops-six-roles-rbac.sql (ADR-064)."""
from pathlib import Path

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


def main() -> None:
    assert 6118 not in ROLES["content_editor"]["menus"]
    assert 6156 not in ROLES["content_editor"]["menus"]
    assert 6175 in ROLES["ip_group_leader"]["menus"]
    assert 6175 in ROLES["ops_manager"]["menus"]
    for code in ("finance", "content_editor", "ops_operator", "data_analyst"):
        assert 6175 not in ROLES[code]["menus"]
        assert 6118 not in ROLES[code]["menus"]
        assert 6134 not in ROLES[code]["menus"]
        assert 6135 not in ROLES[code]["menus"]

    lines: list[str] = [
        "-- ADR-064: OPS six business roles + system_role_menu (exclude super_admin)",
        "-- Apply AFTER seed-oa-system-menu.sql (utf8mb4 stdin via apply-seed-oa-menu.py)",
        "-- Target: Football shenyu-system.system_role / system_role_menu",
        "-- Idempotent re-run: DELETE only ADR-064 menu bindings; preserves work-task role_menu 6194-6196 (V183).",
        "SET NAMES utf8mb4;",
        "",
        "BEGIN;",
        "",
    ]

    for code, r in ROLES.items():
        rid = r["id"]
        lines.append(f"-- ===== {r['name']} ({code}) id={rid} menus={len(r['menus'])} =====")
        lines.extend(
            [
                "INSERT INTO system_role (",
                "    id, name, code, sort, data_scope, data_scope_dept_ids, status, type, remark,",
                "    creator, create_time, updater, update_time, deleted, tenant_id",
                ")",
                "SELECT",
                f"    {rid}, '{r['name']}', '{code}', {r['sort']}, {r['data_scope']}, '', 0, {r['type']},",
                f"    '{r['remark']}',",
                "    'adr-064-seed', NOW(), 'adr-064-seed', NOW(), b'0', 1",
                "FROM DUAL",
                "WHERE NOT EXISTS (",
                f"    SELECT 1 FROM system_role x WHERE x.code = '{code}' AND x.tenant_id = 1 AND x.deleted = b'0'",
                ");",
                "",
                "UPDATE system_role",
                f"SET name = '{r['name']}',",
                f"    sort = {r['sort']},",
                f"    data_scope = {r['data_scope']},",
                f"    type = {r['type']},",
                f"    remark = '{r['remark']}',",
                "    updater = 'adr-064-seed',",
                "    update_time = NOW(),",
                "    deleted = b'0'",
                f"WHERE code = '{code}' AND tenant_id = 1;",
                "",
                f"SET @role_id_{code} := (",
                f"    SELECT id FROM system_role WHERE code = '{code}' AND tenant_id = 1 AND deleted = b'0' LIMIT 1",
                ");",
                "",
                "DELETE FROM system_role_menu",
                f"WHERE role_id = @role_id_{code}",
                "  AND menu_id >= 6100 AND menu_id < 7000",
                "  AND menu_id NOT IN (6194, 6195, 6196);  -- preserve work-task (V183 / 03_work_task_menus_v183)",
                "",
            ]
        )
        base = 71000 + (rid - 160) * 200
        for i, mid in enumerate(r["menus"]):
            rm_id = base + i
            lines.append(
                "INSERT INTO system_role_menu (id, role_id, menu_id, creator, tenant_id, user_type) "
                f"SELECT {rm_id}, @role_id_{code}, {mid}, 'adr-064-seed', 1, 2 FROM DUAL "
                f"WHERE @role_id_{code} IS NOT NULL "
                f"AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE id = {rm_id}) "
                f"AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = @role_id_{code} AND menu_id = {mid});"
            )
        lines.append("")

    lines.extend(
        [
            "COMMIT;",
            "",
            "-- Expected menu counts (ADR-064 §5):",
        ]
    )
    for code, r in ROLES.items():
        lines.append(f"--   {code}: {len(r['menus'])}")

    out = Path(__file__).with_name("seed-ops-six-roles-rbac.sql")
    out.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"wrote {out} ({out.stat().st_size} bytes)")
    for code, r in ROLES.items():
        print(f"  {code}: id={r['id']} menus={len(r['menus'])}")


if __name__ == "__main__":
    main()
