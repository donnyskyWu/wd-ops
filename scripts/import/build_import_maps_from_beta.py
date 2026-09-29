#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Build ip_group_map.json and user_map.json from Beta DB + 0928 Excel labels."""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path

import pymysql

REPO = Path(__file__).resolve().parents[2]
ENV_FILE = REPO / "scripts/integration-config/ops-test-remote.env"
IMPORT_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(IMPORT_DIR))

from xlsx_account_loader import (  # noqa: E402
    normalize_person_name,
    parse_douyin_rows,
    parse_kuaishou_rows,
    load_douyin_df,
    load_kuaishou_df,
    resolve_user_id,
)


def load_env(path: Path) -> dict[str, str]:
    out: dict[str, str] = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        k, v = line.split("=", 1)
        out[k.strip()] = v.strip()
    return out


def mysql_query(
    host: str,
    port: str,
    user: str,
    password: str,
    database: str,
    sql: str,
) -> list[tuple]:
    conn = pymysql.connect(
        host=host,
        port=int(port),
        user=user,
        password=password,
        database=database,
        charset="utf8mb4",
        connect_timeout=30,
        read_timeout=60,
    )
    try:
        with conn.cursor() as cur:
            cur.execute(sql)
            return list(cur.fetchall())
    finally:
        conn.close()


def normalize_ip_label(label: str) -> str:
    s = label.strip()
    s = re.sub(r"(组发视频|组直播|发视频|直播|播)$", "", s)
    s = re.sub(r"组$", "", s)
    return s.strip()


def match_ip_group(label: str, groups: list[tuple[int, str]]) -> int | None:
    if not label or label in {"无", "已分", "新主播"}:
        return None
    raw = label.strip()
    norm = normalize_ip_label(raw)
    candidates: list[tuple[int, str, str]] = [
        (gid, gname, normalize_ip_label(gname)) for gid, gname in groups
    ]
    for gid, gname, gnorm in candidates:
        if raw == gname or norm == gnorm:
            return gid
    for gid, gname, gnorm in candidates:
        if norm and gnorm and (norm in gnorm or gnorm in norm):
            return gid
    for gid, gname, gnorm in candidates:
        if norm and gnorm and len(norm) >= 2 and norm[:2] in gnorm:
            return gid
    alias = {
        "阿豹组": "豹哥",
        "斌哥组": "斌哥",
        "冠希": "冠希组",
        "欣哥": "欣哥组",
        "舒彬": "舒彬组",
    }
    key = norm or raw
    if key in alias:
        target = alias[key]
        for gid, gname, gnorm in candidates:
            if target in gname or target in gnorm:
                return gid
    return None


def _paren_aliases(name: str) -> list[str]:
    aliases: list[str] = []
    for m in re.finditer(r"[（(]([^）)]+)[）)]", name):
        inner = m.group(1).strip()
        if inner and inner not in {"产研", "皮总"}:
            aliases.append(inner)
    return aliases


def match_user(name: str, users: list[tuple[int, str, str]]) -> int | None:
    raw = name.strip().rstrip("？?")
    norm = normalize_person_name(raw)
    if not norm or norm in {"无", "产研", "老板妈妈", "老板娘", "自助餐饭店老板"}:
        return None
    if norm.startswith("*") or norm.startswith("**"):
        return None

    def try_match(candidate: str | None) -> int | None:
        if not candidate:
            return None
        c = candidate.strip().rstrip("？?")
        cn = normalize_person_name(c) or c
        for uid, nick, uname in users:
            for field in (nick, uname):
                if not field:
                    continue
                f = field.strip()
                fn = normalize_person_name(f) or f
                if cn == f or cn == fn:
                    return uid
        for uid, nick, uname in users:
            for field in (nick, uname):
                if not field:
                    continue
                f = normalize_person_name(field.strip()) or field.strip()
                if len(cn) >= 2 and (cn in f or f in cn):
                    return uid
        return None

    for candidate in (norm, raw, *_paren_aliases(name)):
        uid = try_match(candidate)
        if uid is not None:
            return uid
    return None


def _collect_ip_labels_from_df(df) -> set[str]:
    labels: set[str] = set()
    for col in ("分配小组", "分配"):
        if col not in df.columns:
            continue
        for v in df[col].dropna().astype(str).str.strip().tolist():
            if v and v not in {"/", "—", "-"}:
                labels.add(v)
    return labels


def collect_excel_labels(xlsx: Path) -> tuple[set[str], set[str], set[str]]:
    dy_sheet, dy_df = load_douyin_df(xlsx)
    ks_df = load_kuaishou_df(xlsx)
    accounts = parse_douyin_rows(dy_df, dy_sheet) + parse_kuaishou_rows(ks_df)
    ip_labels = _collect_ip_labels_from_df(dy_df) | _collect_ip_labels_from_df(ks_df)
    operators: set[str] = set()
    holders: set[str] = set()
    for p in accounts:
        if p.operator_name:
            operators.add(p.operator_name.strip())
        if p.holder_name:
            holders.add(p.holder_name.strip())
    return ip_labels, operators, holders


def _merge_user_mapping(
    user_map: dict[str, int],
    names: set[str],
    users: list[tuple[int, str, str]],
    unmapped_out: list[str],
) -> None:
    for name in sorted(names):
        uid = match_user(name, users)
        if uid is not None:
            user_map[name] = uid
            norm = normalize_person_name(name)
            if norm and norm not in user_map:
                user_map[norm] = uid
        else:
            unmapped_out.append(name)


def main() -> int:
    env = load_env(ENV_FILE)
    host = env["OPS_TEST_DB_HOST"]
    port = env.get("OPS_TEST_DB_PORT", "3306")
    ops_user = env["OPS_TEST_MASTER_USER"]
    ops_pwd = env["OPS_TEST_MASTER_PASSWORD"]
    ops_db = env.get("OPS_TEST_MASTER_DB", "shenyu-ops")
    sys_user = env["OPS_TEST_SYSTEM_USER"]
    sys_pwd = env["OPS_TEST_SYSTEM_PASSWORD"]
    sys_db = env.get("OPS_TEST_SYSTEM_DB", "shenyu-system")

    xlsx = Path(
        sys.argv[1]
        if len(sys.argv) > 1
        else r"d:\self\sy\文档\抖音号_快手号分配(0928).xlsx"
    )
    ip_labels, operator_names, holder_names = collect_excel_labels(xlsx)

    group_rows = mysql_query(
        host,
        port,
        ops_user,
        ops_pwd,
        ops_db,
        "SELECT id, group_name FROM oa_ip_group WHERE tenant_id=1 ORDER BY id;",
    )
    groups = [(int(r[0]), str(r[1])) for r in group_rows]

    user_rows = mysql_query(
        host,
        port,
        sys_user,
        sys_pwd,
        sys_db,
        "SELECT id, IFNULL(nickname,''), IFNULL(username,'') FROM system_users "
        "WHERE tenant_id=1 AND deleted=0 ORDER BY id;",
    )
    users = [(int(r[0]), str(r[1]), str(r[2])) for r in user_rows]

    ip_map: dict[str, int | str] = {
        "_comment": "Excel 分配小组名 -> oa_ip_group.id（Beta 110.42.49.224 tenant=1）",
        "_source": f"{host}/{ops_db}",
        "_beta_ip_groups": ", ".join(f"{gid}:{gname}" for gid, gname in groups),
    }
    user_map: dict[str, int] = {
        "_comment": "Excel「持有人」「运营人」显示名 -> Football system_users.id（Beta tenant=1）",
        "_source": f"{host}/{sys_db}",
    }

    ip_unmapped: list[str] = []
    for label in sorted(ip_labels):
        gid = match_ip_group(label, groups)
        if gid is not None:
            ip_map[label] = gid
        else:
            ip_unmapped.append(label)

    operator_unmapped: list[str] = []
    holder_unmapped: list[str] = []
    _merge_user_mapping(user_map, operator_names, users, operator_unmapped)
    _merge_user_mapping(user_map, holder_names, users, holder_unmapped)

    data_dir = IMPORT_DIR / "data"
    data_dir.mkdir(parents=True, exist_ok=True)
    (data_dir / "ip_group_map.json").write_text(
        json.dumps(ip_map, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    (data_dir / "user_map.json").write_text(
        json.dumps(user_map, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )

    unmapped_path = data_dir / "unmapped-20260928.txt"
    lines = [
        "# 0928 import unresolved (Beta fuzzy match)",
        f"# IP labels: {len(ip_unmapped)}/{len(ip_labels)} unresolved",
        "",
        "[ip_group_labels]",
        *ip_unmapped,
        "",
        f"# Holders: {len(holder_unmapped)}/{len(holder_names)} unresolved",
        "",
        "[holder_names]",
        *holder_unmapped,
        "",
        f"# Operators: {len(operator_unmapped)}/{len(operator_names)} unresolved",
        "",
        "[operator_names]",
        *operator_unmapped,
        "",
    ]
    unmapped_path.write_text("\n".join(lines), encoding="utf-8")

    dy_sheet, dy_df = load_douyin_df(xlsx)
    ks_df = load_kuaishou_df(xlsx)
    accounts = parse_douyin_rows(dy_df, dy_sheet) + parse_kuaishou_rows(ks_df)
    json_user = {k: v for k, v in user_map.items() if not str(k).startswith("_")}
    holder_resolved_names: set[str] = set()
    op_resolved_names: set[str] = set()
    for p in accounts:
        if p.holder_name and resolve_user_id(p.holder_name, json_user):
            holder_resolved_names.add(p.holder_name)
        if p.operator_name and resolve_user_id(p.operator_name, json_user):
            op_resolved_names.add(p.operator_name)

    report = {
        "beta_host": host,
        "oa_ip_group_count": len(groups),
        "system_users_count": len(users),
        "unique_ip_labels": len(ip_labels),
        "unique_ip_labels_unresolved": len(ip_unmapped),
        "unique_holder_names": len(holder_names),
        "unique_holder_names_unresolved": len(holder_unmapped),
        "unique_operator_names": len(operator_names),
        "unique_operator_names_unresolved": len(operator_unmapped),
        "account_rows_holder_resolved_distinct": len(holder_resolved_names),
        "account_rows_operator_resolved_distinct": len(op_resolved_names),
    }
    print(json.dumps(report, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
