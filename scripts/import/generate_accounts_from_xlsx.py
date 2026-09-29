#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Generate M4 account import SQL from Excel workbook.

用户 SSOT：见 xlsx_account_loader.py 模块头表格。
- oa_account INSERT 含 holder_user_id、realname_id、operator_user_id（持有人/运营人经 user_map.json → Football id）。
- assigned_user_id 仅用于 oa_phone.keeper_id / oa_sim_card.assigned_user_id，与 Excel 持有人/运营人无关。
"""

from __future__ import annotations

import argparse
import base64
import hashlib
import json
import re
from datetime import date, datetime
from pathlib import Path
import sys

import pandas as pd

sys.path.insert(0, str(Path(__file__).resolve().parent))
from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes

from xlsx_account_loader import (
    IMPORT_NO_COLUMNS,
    ParsedAccount,
    load_douyin_df,
    load_json_map,
    load_kuaishou_df,
    load_sim_df,
    parse_douyin_rows,
    parse_kuaishou_rows,
    resolve_user_id,
    sample_mapping,
)

AES_KEY_B64 = "MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI="
TENANT_ID = 1
DEFAULT_USER_ID = 1001  # seed Football user; override via --assigned-user-id
IMPORT_TAG = "xlsx-import-20260928"
REQUIRES_FLYWAY = "V206__oa_account_short_video_live_operator.sql"

ID_COMPANY_BASE = 100_001
ID_REALNAME_BASE = 110_001
ID_PHONE_BASE = 120_001
ID_SIM_BASE = 130_001
ID_ACCOUNT_BASE = 140_001

PLACEHOLDER_ID_CARD = "000000000000000000"
PLACEHOLDER_PHONE = "10000000000"

OPERATOR_MAP = {
    "移动卡": "MOBILE",
    "联通卡": "UNICOM",
    "电信卡": "TELECOM",
    "广电卡": "MOBILE",  # dict_sim_operator 无广电，降级为 MOBILE
    "/": "UNICOM",
}


def encrypt_aes_ecb(plain: str) -> str:
    key = base64.b64decode(AES_KEY_B64)
    data = plain.encode("utf-8")
    pad = 16 - len(data) % 16
    data += bytes([pad]) * pad
    enc = Cipher(algorithms.AES(key), modes.ECB(), backend=default_backend()).encryptor()
    return base64.b64encode(enc.update(data) + enc.finalize()).decode()


def phone_hash(phone: str) -> str:
    return hashlib.sha256(phone.encode()).hexdigest()


def sql_str(value: str | None) -> str:
    if value is None:
        return "NULL"
    return "'" + str(value).replace("\\", "\\\\").replace("'", "''") + "'"


def sql_coalesce_batch_row_id(
    table: str,
    tenant_id: int,
    key_column: str,
    key_value_sql: str,
    batch_id: int | None,
) -> str:
    """FK: uk match, then batch slot id, then literal batch id (partial re-run safe)."""
    by_key = (
        f"(SELECT id FROM {table} WHERE tenant_id = {tenant_id} "
        f"AND {key_column} = {key_value_sql} LIMIT 1)"
    )
    if batch_id is None:
        return by_key
    by_batch_id = (
        f"(SELECT id FROM {table} WHERE tenant_id = {tenant_id} AND id = {batch_id} LIMIT 1)"
    )
    return f"COALESCE({by_key}, {by_batch_id}, {batch_id})"


def emit_upsert_company(lines: list[str], cid: int, name: str, credit: str) -> None:
    tag = sql_str(IMPORT_TAG)
    credit_sql = sql_str(credit)
    set_clause = (
        f"company_name={sql_str(name)}, credit_code={credit_sql}, status='ENABLED', updater={tag}"
    )
    lines.append(
        f"UPDATE oa_company SET {set_clause} "
        f"WHERE tenant_id={TENANT_ID} AND credit_code={credit_sql};"
    )
    lines.append(
        f"UPDATE oa_company SET {set_clause} WHERE tenant_id={TENANT_ID} AND id={cid};"
    )
    lines.append(
        "INSERT INTO oa_company (id, tenant_id, company_name, credit_code, status, creator, updater) "
        f"SELECT {cid}, {TENANT_ID}, {sql_str(name)}, {credit_sql}, 'ENABLED', {tag}, {tag} "
        f"FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM oa_company WHERE tenant_id={TENANT_ID} "
        f"AND credit_code={credit_sql}) "
        f"AND NOT EXISTS (SELECT 1 FROM oa_company WHERE tenant_id={TENANT_ID} AND id={cid});"
    )


def emit_upsert_phone(
    lines: list[str],
    pid: int,
    rn_id: int | None,
    phone_no: str,
    dev: str,
    model: str | None,
    keeper_id: int,
) -> None:
    ph_hash = phone_hash(phone_no)
    enc = sql_str(encrypt_aes_ecb(phone_no))
    hash_sql = sql_str(ph_hash)
    rn_sql = str(rn_id) if rn_id else "NULL"
    model_sql = sql_str(model) if model else "NULL"
    tag = sql_str(IMPORT_TAG)
    dev_sql = sql_str(dev)
    code_sql = sql_str(f"DEV-{dev}")
    set_clause = (
        f"realname_id={rn_sql}, phone_number_encrypted={enc}, phone_code={code_sql}, "
        f"phone_model={model_sql}, device_number={dev_sql}, keeper_id={keeper_id}, "
        f"status='ENABLED', updater={tag}"
    )
    lines.append(
        f"UPDATE oa_phone SET {set_clause} "
        f"WHERE tenant_id={TENANT_ID} AND phone_number_hash={hash_sql};"
    )
    lines.append(
        f"UPDATE oa_phone SET {set_clause} WHERE tenant_id={TENANT_ID} AND id={pid};"
    )
    lines.append(
        "INSERT INTO oa_phone (id, tenant_id, realname_id, phone_number_encrypted, phone_number_hash, "
        "phone_code, phone_model, device_number, keeper_id, status, creator, updater) "
        f"SELECT {pid}, {TENANT_ID}, {rn_sql}, {enc}, {hash_sql}, {code_sql}, {model_sql}, "
        f"{dev_sql}, {keeper_id}, 'ENABLED', {tag}, {tag} "
        f"FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM oa_phone WHERE tenant_id={TENANT_ID} "
        f"AND phone_number_hash={hash_sql}) "
        f"AND NOT EXISTS (SELECT 1 FROM oa_phone WHERE tenant_id={TENANT_ID} AND id={pid});"
    )


def emit_upsert_sim(
    lines: list[str],
    sid: int,
    phone_id_sql: str,
    phone_no: str,
    operator: str,
    is_primary: str,
    package_name: str | None,
    assigned_user_id: int,
) -> None:
    ph_hash = phone_hash(phone_no)
    enc = sql_str(encrypt_aes_ecb(phone_no))
    hash_sql = sql_str(ph_hash)
    pkg_sql = sql_str(package_name) if package_name else "NULL"
    tag = sql_str(IMPORT_TAG)
    set_clause = (
        f"phone_id={phone_id_sql}, phone_number_encrypted={enc}, "
        f"is_primary={sql_str(is_primary)}, operator={sql_str(operator)}, "
        f"assigned_user_id={assigned_user_id}, package_name={pkg_sql}, "
        f"status='ENABLED', updater={tag}"
    )
    lines.append(
        f"UPDATE oa_sim_card SET {set_clause} "
        f"WHERE tenant_id={TENANT_ID} AND phone_number_hash={hash_sql};"
    )
    lines.append(
        f"UPDATE oa_sim_card SET {set_clause} WHERE tenant_id={TENANT_ID} AND id={sid};"
    )
    lines.append(
        "INSERT INTO oa_sim_card (id, tenant_id, phone_id, phone_number_encrypted, phone_number_hash, "
        "is_primary, operator, assigned_user_id, package_name, status, creator, updater) "
        f"SELECT {sid}, {TENANT_ID}, {phone_id_sql}, {enc}, {hash_sql}, {sql_str(is_primary)}, "
        f"{sql_str(operator)}, {assigned_user_id}, {pkg_sql}, 'ENABLED', {tag}, {tag} "
        f"FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM oa_sim_card WHERE tenant_id={TENANT_ID} "
        f"AND phone_number_hash={hash_sql}) "
        f"AND NOT EXISTS (SELECT 1 FROM oa_sim_card WHERE tenant_id={TENANT_ID} AND id={sid});"
    )


def emit_upsert_account(lines: list[str], row: tuple) -> None:
    (
        aid,
        platform,
        account_type,
        account_name,
        ext_id,
        company_id_sql,
        realname_id,
        phone_id_sql,
        sim_id_sql,
        phone_hash_val,
        pwd_enc,
        short_video_status,
        live_status,
        holder_user_id,
        operator_user_id,
    ) = row
    tag = sql_str(IMPORT_TAG)
    plat = sql_str(platform)
    ext = sql_str(ext_id)
    set_clause = (
        f"account_type={sql_str(account_type)}, account_name={sql_str(account_name)}, "
        f"company_id={company_id_sql}, realname_id={realname_id if realname_id else 'NULL'}, "
        f"phone_id={phone_id_sql}, sim_card_id={sim_id_sql}, "
        f"phone_number_hash={sql_str(phone_hash_val) if phone_hash_val else 'NULL'}, "
        f"holder_user_id={holder_user_id if holder_user_id else 'NULL'}, "
        f"operator_user_id={operator_user_id if operator_user_id else 'NULL'}, "
        f"password_encrypted={sql_str(pwd_enc) if pwd_enc else 'NULL'}, "
        f"short_video_status={sql_str(short_video_status) if short_video_status else 'NULL'}, "
        f"live_status={sql_str(live_status) if live_status else 'NULL'}, "
        f"status='NORMAL', updater={tag}"
    )
    lines.append(
        f"UPDATE oa_account SET {set_clause} "
        f"WHERE tenant_id={TENANT_ID} AND platform_type={plat} AND external_account_id={ext};"
    )
    lines.append(
        f"UPDATE oa_account SET {set_clause} WHERE tenant_id={TENANT_ID} AND id={aid};"
    )
    lines.append(
        "INSERT INTO oa_account (id, tenant_id, platform_type, account_type, account_name, external_account_id, "
        "company_id, realname_id, phone_id, sim_card_id, phone_number_hash, ip_group_id, holder_user_id, "
        "operator_user_id, password_encrypted, short_video_status, live_status, status, creator, updater) "
        f"SELECT {aid}, {TENANT_ID}, {plat}, {sql_str(account_type)}, {sql_str(account_name)}, {ext}, "
        f"{company_id_sql}, {realname_id if realname_id else 'NULL'}, "
        f"{phone_id_sql}, {sim_id_sql}, "
        f"{sql_str(phone_hash_val) if phone_hash_val else 'NULL'}, "
        f"NULL, {holder_user_id if holder_user_id else 'NULL'}, {operator_user_id if operator_user_id else 'NULL'}, "
        f"{sql_str(pwd_enc) if pwd_enc else 'NULL'}, "
        f"{sql_str(short_video_status) if short_video_status else 'NULL'}, "
        f"{sql_str(live_status) if live_status else 'NULL'}, "
        f"'NORMAL', {tag}, {tag} "
        f"FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM oa_account WHERE tenant_id={TENANT_ID} "
        f"AND platform_type={plat} AND external_account_id={ext}) "
        f"AND NOT EXISTS (SELECT 1 FROM oa_account WHERE tenant_id={TENANT_ID} AND id={aid});"
    )


def emit_upsert_realname(
    lines: list[str], rid: int, name: str, phone: str, id_card_enc: str
) -> None:
    """无业务唯一键：real_name 对齐 → 批次 id 槽 → INSERT（id 未占槽时）。"""
    name_sql = sql_str(name)
    enc_phone = sql_str(encrypt_aes_ecb(phone))
    id_card_sql = sql_str(id_card_enc)
    tag = sql_str(IMPORT_TAG)
    set_clause = (
        f"real_name={name_sql}, id_type='ID_CARD', id_card_encrypted={id_card_sql}, "
        f"phone_encrypted={enc_phone}, status='ENABLED', updater={tag}"
    )
    lines.append(
        f"UPDATE oa_realname SET {set_clause} "
        f"WHERE tenant_id={TENANT_ID} AND real_name={name_sql};"
    )
    lines.append(
        f"UPDATE oa_realname SET {set_clause} WHERE tenant_id={TENANT_ID} AND id={rid};"
    )
    lines.append(
        "INSERT INTO oa_realname (id, tenant_id, real_name, id_type, id_card_encrypted, phone_encrypted, "
        "status, creator, updater) "
        f"SELECT {rid}, {TENANT_ID}, {name_sql}, 'ID_CARD', {id_card_sql}, {enc_phone}, "
        f"'ENABLED', {tag}, {tag} "
        f"FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM oa_realname WHERE tenant_id={TENANT_ID} "
        f"AND real_name={name_sql}) "
        f"AND NOT EXISTS (SELECT 1 FROM oa_realname WHERE tenant_id={TENANT_ID} AND id={rid});"
    )


def build_patch_user_fields_sql(account_rows: list[tuple]) -> str:
    """UPDATE-only patch for已导入批次：补 holder/operator/双状态，无需 PRE-IMPORT 重跑。"""
    lines = [
        "USE shenyu-ops;",
        "-- =============================================================================",
        f"-- PATCH · {IMPORT_TAG} · holder_user_id / operator_user_id / short_video_status / live_status",
        "-- 适用：已执行 import-accounts-shenyu-ops-20260928.sql 但字段仍为 NULL，或需与 Excel 再对齐",
        f"-- 依赖：{REQUIRES_FLYWAY}",
        "-- 可重复执行（幂等：写入与生成器 INSERT 相同值）",
        "-- =============================================================================",
        "",
        "SET NAMES utf8mb4;",
        "START TRANSACTION;",
        "",
    ]
    patch_count = 0
    for row in account_rows:
        (
            aid,
            platform,
            _account_type,
            _account_name,
            _ext_id,
            _company_id,
            _realname_id,
            _phone_id,
            _sim_id,
            _phone_hash_val,
            _pwd_enc,
            short_video_status,
            live_status,
            holder_user_id,
            operator_user_id,
            *_rest,
        ) = row
        sets: list[str] = []
        if holder_user_id:
            sets.append(f"holder_user_id = {holder_user_id}")
        if operator_user_id:
            sets.append(f"operator_user_id = {operator_user_id}")
        if short_video_status:
            sets.append(f"short_video_status = {sql_str(short_video_status)}")
        if live_status:
            sets.append(f"live_status = {sql_str(live_status)}")
        if not sets:
            continue
        sets.append("updater = 'patch-user-fields-20260928'")
        lines.append(
            f"UPDATE oa_account SET {', '.join(sets)} "
            f"WHERE tenant_id = {TENANT_ID} AND id = {aid} AND platform_type = {sql_str(platform)};"
        )
        patch_count += 1
    lines.extend(
        [
            "",
            f"-- patched_rows={patch_count}",
            "COMMIT;",
            "",
            "-- END",
            "",
        ]
    )
    return "\n".join(lines)


def clean_phone(value) -> str | None:
    if pd.isna(value):
        return None
    s = str(value).strip()
    if s.endswith(".0"):
        s = s[:-2]
    s = re.sub(r"\D", "", s)
    return s if len(s) == 11 else None


def clean_text(value) -> str | None:
    if pd.isna(value):
        return None
    s = str(value).strip()
    return s or None


def clean_device_no(value) -> str | None:
    if pd.isna(value):
        return None
    s = str(value).strip()
    if s.endswith(".0"):
        s = s[:-2]
    return s or None


def credit_code_for(name: str, seq: int) -> str:
    digest = hashlib.md5(name.encode()).hexdigest().upper()[:10]
    return f"91IMPORT{seq:04d}{digest[:4]}"[:18].ljust(18, "0")


def guess_account_type(company_name: str | None) -> str:
    if not company_name:
        return "PERSONAL_ACCOUNT"
    markers = ("公司", "商行", "个体", "经营部", "贸易", "传媒", "有限")
    return "OFFICIAL_ACCOUNT" if any(m in company_name for m in markers) else "PERSONAL_ACCOUNT"


# oa_account 子表：仅随抖音/快手账号删除（保留公众号/视频号/小红书等 E2E 数据）
OA_ACCOUNT_CHILD_TABLES: list[tuple[str, str, str]] = [
    ("oa_platform_account_fan_group", "account_id", "平台账号粉丝群"),
    ("oa_account_cost", "account_id", "账号成本"),
    ("oa_account_status_log", "account_id", "账号状态日志"),
    ("oa_collector_account_bind", "oa_account_id", "采集账号绑定"),
]

DY_KS_PLATFORMS = ("DOUYIN", "KUAISHOU")


def _dy_ks_account_subquery(tenant_id: int) -> str:
    platforms = ", ".join(f"'{p}'" for p in DY_KS_PLATFORMS)
    return (
        f"SELECT id FROM oa_account WHERE tenant_id = {tenant_id} "
        f"AND platform_type IN ({platforms})"
    )


def _import_batch_predicate(id_base: int) -> str:
    return f"(id >= {id_base} OR creator = '{IMPORT_TAG}')"


def _import_batch_account_subquery(tenant_id: int) -> str:
    platforms = ", ".join(f"'{p}'" for p in DY_KS_PLATFORMS)
    batch = _import_batch_predicate(ID_ACCOUNT_BASE)
    return (
        f"SELECT id FROM oa_account WHERE tenant_id = {tenant_id} "
        f"AND platform_type IN ({platforms}) AND {batch}"
    )


def _emit_import_batch_support_cleanup_sql(lines: list[str], tenant_id: int) -> None:
    """Delete Excel import-batch support rows; skip rows still referenced by any account."""
    batch = _import_batch_predicate(ID_REALNAME_BASE)
    lines.append("-- 实名人-中介关联（导入批次实名人）")
    lines.append(
        f"DELETE FROM oa_realname_intermediary WHERE tenant_id = {tenant_id} "
        f"AND realname_id IN (SELECT id FROM oa_realname WHERE tenant_id = {tenant_id} AND {batch});"
    )
    batch = _import_batch_predicate(ID_COMPANY_BASE)
    lines.append("-- 公司扩容记录（导入批次公司）")
    lines.append(
        f"DELETE FROM oa_company_expansion WHERE tenant_id = {tenant_id} "
        f"AND company_id IN (SELECT id FROM oa_company WHERE tenant_id = {tenant_id} AND {batch});"
    )
    batch = _import_batch_predicate(ID_SIM_BASE)
    lines.append("-- 手机卡（导入批次；无剩余账号引用）")
    lines.append(
        f"DELETE FROM oa_sim_card WHERE tenant_id = {tenant_id} AND {batch} "
        f"AND id NOT IN (SELECT sim_card_id FROM oa_account "
        f"WHERE tenant_id = {tenant_id} AND sim_card_id IS NOT NULL);"
    )
    batch = _import_batch_predicate(ID_PHONE_BASE)
    lines.append("-- 手机设备（导入批次；无剩余账号/SIM 引用）")
    lines.append(
        f"DELETE FROM oa_phone WHERE tenant_id = {tenant_id} AND {batch} "
        f"AND id NOT IN (SELECT phone_id FROM oa_account "
        f"WHERE tenant_id = {tenant_id} AND phone_id IS NOT NULL) "
        f"AND id NOT IN (SELECT phone_id FROM oa_sim_card "
        f"WHERE tenant_id = {tenant_id} AND phone_id IS NOT NULL);"
    )
    batch = _import_batch_predicate(ID_REALNAME_BASE)
    lines.append("-- 实名人（导入批次；无剩余账号/设备引用）")
    lines.append(
        f"DELETE FROM oa_realname WHERE tenant_id = {tenant_id} AND {batch} "
        f"AND id NOT IN (SELECT realname_id FROM oa_account "
        f"WHERE tenant_id = {tenant_id} AND realname_id IS NOT NULL) "
        f"AND id NOT IN (SELECT realname_id FROM oa_phone "
        f"WHERE tenant_id = {tenant_id} AND realname_id IS NOT NULL);"
    )
    batch = _import_batch_predicate(ID_COMPANY_BASE)
    lines.append("-- 公司（导入批次；无剩余账号引用）")
    lines.append(
        f"DELETE FROM oa_company WHERE tenant_id = {tenant_id} AND {batch} "
        f"AND id NOT IN (SELECT company_id FROM oa_account "
        f"WHERE tenant_id = {tenant_id} AND company_id IS NOT NULL);"
    )


def emit_pre_import_cleanup_sql(lines: list[str], tenant_id: int) -> None:
    """Emit scoped cleanup: all Douyin/Kuaishou accounts + xlsx import batch support rows."""
    dy_ks = _dy_ks_account_subquery(tenant_id)
    lines.append("-- ========== PRE-IMPORT CLEANUP (test/prod) ==========")
    lines.append(f"-- 策略：删除 tenant_id={tenant_id} 下全部抖音/快手账号后重导；")
    lines.append("--       支撑表仅清 Excel 导入批次（id 段或 creator 标记）；")
    lines.append("--       保留公众号/视频号/小红书/企微/个微等 E2E seed 数据。")
    lines.append("-- 本地 dev 仅重导 Excel（不动已有抖音/快手）时，可注释本段，仅用下方 ROLLBACK 段。")
    lines.append("")
    lines.append("-- --- 1. 抖音/快手账号子表（按 account_id 限定） ---")
    for table, fk_col, label in OA_ACCOUNT_CHILD_TABLES:
        lines.append(f"-- {label}")
        lines.append(
            f"DELETE FROM {table} WHERE tenant_id = {tenant_id} "
            f"AND {fk_col} IN ({dy_ks});"
        )
    lines.append("")
    lines.append("-- --- 2. 平台账号：仅抖音/快手（tenant 全量，含历史手工录入） ---")
    lines.append(
        f"-- 导入会整体替换抖音/快手，故删除 tenant_id={tenant_id} 下全部 DOUYIN/KUAISHOU，"
        "不限 id 段。"
    )
    platforms = ", ".join(f"'{p}'" for p in DY_KS_PLATFORMS)
    lines.append(
        f"DELETE FROM oa_account WHERE tenant_id = {tenant_id} "
        f"AND platform_type IN ({platforms});"
    )
    lines.append("")
    lines.append("-- --- 3. Excel 导入批次支撑表（ID 段 / creator；跳过仍被其他平台账号引用的行） ---")
    _emit_import_batch_support_cleanup_sql(lines, tenant_id)
    lines.append("")


def emit_rollback_cleanup_sql(lines: list[str], tenant_id: int) -> None:
    """Emit ID-range rollback for re-import without wiping low-ID seed rows."""
    import_accounts = _import_batch_account_subquery(tenant_id)
    lines.append("-- ========== ROLLBACK (re-import only) ==========")
    lines.append("-- 按导入 ID 段清理抖音/快手及 Excel 批次支撑数据；dev 重导时可单独使用（注释掉上方 PRE-IMPORT 段）")
    lines.append("-- 不影响公众号/视频号/小红书等其它平台账号。")
    lines.append("")
    lines.append("-- --- 1. 导入批次抖音/快手账号子表 ---")
    for table, fk_col, label in OA_ACCOUNT_CHILD_TABLES:
        lines.append(f"-- {label}")
        lines.append(
            f"DELETE FROM {table} WHERE tenant_id = {tenant_id} "
            f"AND {fk_col} IN ({import_accounts});"
        )
    lines.append("")
    lines.append("-- --- 2. 导入批次抖音/快手账号（id >= 140001 或 creator 标记） ---")
    platforms = ", ".join(f"'{p}'" for p in DY_KS_PLATFORMS)
    batch = _import_batch_predicate(ID_ACCOUNT_BASE)
    lines.append(
        f"DELETE FROM oa_account WHERE tenant_id = {tenant_id} "
        f"AND platform_type IN ({platforms}) AND {batch};"
    )
    lines.append("")
    lines.append("-- --- 3. Excel 导入批次支撑表 ---")
    _emit_import_batch_support_cleanup_sql(lines, tenant_id)
    lines.append("")


def parse_date(value) -> str | None:
    if pd.isna(value):
        return None
    if isinstance(value, (datetime, date)):
        return value.strftime("%Y-%m-%d")
    s = str(value).strip()
    if not s:
        return None
    for fmt in ("%Y-%m-%d", "%Y-%m-%d %H:%M:%S", "%Y/%m/%d"):
        try:
            return datetime.strptime(s[:19], fmt).strftime("%Y-%m-%d")
        except ValueError:
            continue
    return None


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--xlsx",
        default=r"d:\self\sy\文档\抖音号_快手号分配(0928).xlsx",
        help="Source Excel path",
    )
    parser.add_argument(
        "--out-sql",
        default=str(Path(__file__).with_name("import-accounts-from-xlsx-20260928.sql")),
    )
    parser.add_argument(
        "--out-analysis",
        default=str(Path(__file__).with_name("import-accounts-analysis-20260928.md")),
    )
    parser.add_argument("--assigned-user-id", type=int, default=DEFAULT_USER_ID)
    _import_dir = Path(__file__).resolve().parent
    parser.add_argument(
        "--user-map",
        default=str(_import_dir / "data" / "user_map.json"),
        help="JSON: Excel「持有人」「运营人」姓名 -> Football system_users.id（holder_user_id / operator_user_id）",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="统计 + 分析文档；不写 SQL（不执行 PRE-IMPORT）",
    )
    parser.add_argument(
        "--patch-sql-out",
        default=str(_import_dir / "patch-accounts-user-fields-20260928.sql"),
        help="Write UPDATE-only patch SQL (holder/operator/双状态) alongside full import",
    )
    parser.add_argument(
        "--patch-only",
        action="store_true",
        help="Only write --patch-sql-out (no full import SQL / PRE-IMPORT)",
    )
    args = parser.parse_args()

    xlsx = Path(args.xlsx)
    user_map = load_json_map(Path(args.user_map))
    write_sql = not args.dry_run and not args.patch_only

    dy_sheet, dy_df = load_douyin_df(xlsx)
    ks_df = load_kuaishou_df(xlsx)
    sim_df = load_sim_df(xlsx)
    if sim_df is None:
        sim_df = pd.DataFrame(columns=["实名者", "手机号", "存放手机", "机型", "营业厅", "主卡", "卡套餐"])

    parsed_douyin = parse_douyin_rows(dy_df, dy_sheet)
    parsed_kuaishou = parse_kuaishou_rows(ks_df)

    # ---------- build companies ----------
    company_names: list[str] = []
    for col_df in (dy_df,):
        for name in col_df["账号认证主体"].dropna().astype(str).str.strip().tolist():
            if name and name not in company_names:
                company_names.append(name)
    company_id_by_name: dict[str, int] = {}
    company_rows = []
    for idx, name in enumerate(company_names):
        cid = ID_COMPANY_BASE + idx
        company_id_by_name[name] = cid
        company_rows.append(
            (cid, name, credit_code_for(name, idx + 1))
        )

    # ---------- build realnames ----------
    realname_names: list[str] = []
    for series in (sim_df["实名者"], dy_df["持有人"]):
        for name in series.dropna().astype(str).str.strip().tolist():
            if name and name not in realname_names:
                realname_names.append(name)
    for p in parsed_douyin + parsed_kuaishou:
        if p.holder_name and p.holder_name not in realname_names:
            realname_names.append(p.holder_name)
    realname_id_by_name: dict[str, int] = {}
    realname_phone_by_name: dict[str, str] = {}
    for _, row in sim_df.iterrows():
        rn = clean_text(row.get("实名者"))
        ph = clean_phone(row.get("手机号"))
        if rn and ph and rn not in realname_phone_by_name:
            realname_phone_by_name[rn] = ph
    realname_rows = []
    for idx, name in enumerate(realname_names):
        rid = ID_REALNAME_BASE + idx
        realname_id_by_name[name] = rid
        phone = realname_phone_by_name.get(name, PLACEHOLDER_PHONE)
        realname_rows.append((rid, name, phone))

    # ---------- build phones (devices) ----------
    device_rows: dict[str, dict] = {}
    for _, row in sim_df.iterrows():
        dev = clean_device_no(row.get("存放手机"))
        if not dev:
            continue
        phone_no = clean_phone(row.get("手机号"))
        realname = clean_text(row.get("实名者"))
        model = clean_text(row.get("机型"))
        if dev not in device_rows:
            device_rows[dev] = {
                "device_no": dev,
                "phone_no": phone_no,
                "realname": realname,
                "model": model,
            }
        else:
            if not device_rows[dev]["phone_no"] and phone_no:
                device_rows[dev]["phone_no"] = phone_no
            if not device_rows[dev]["realname"] and realname:
                device_rows[dev]["realname"] = realname
            if not device_rows[dev]["model"] and model:
                device_rows[dev]["model"] = model

    # also include Douyin / Kuaishou device numbers
    for p in parsed_douyin + parsed_kuaishou:
        dev = p.device_no
        if not dev or dev in device_rows:
            continue
        preferred_phone = p.sim_card_phone if p.platform == "DOUYIN" else None
        device_rows[dev] = {
            "device_no": dev,
            "phone_no": preferred_phone,
            "realname": p.holder_name,
            "model": None,
        }

    phone_id_by_device: dict[str, int] = {}
    phone_rows = []
    used_phone_numbers: set[str] = set()
    synthetic_seq = 0

    def unique_phone_for_device(dev: str, preferred: str | None) -> str:
        nonlocal synthetic_seq
        if preferred and len(preferred) == 11 and preferred not in used_phone_numbers:
            used_phone_numbers.add(preferred)
            return preferred
        if dev.isdigit():
            candidate = f"199{int(dev):08d}"[:11]
            if candidate not in used_phone_numbers:
                used_phone_numbers.add(candidate)
                return candidate
        while True:
            synthetic_seq += 1
            candidate = f"198{synthetic_seq:08d}"[:11]
            if candidate not in used_phone_numbers:
                used_phone_numbers.add(candidate)
                return candidate

    for idx, (dev, info) in enumerate(sorted(device_rows.items(), key=lambda x: int(x[0]) if x[0].isdigit() else 999999)):
        pid = ID_PHONE_BASE + idx
        phone_id_by_device[dev] = pid
        phone_no = unique_phone_for_device(dev, info["phone_no"])
        rn_id = realname_id_by_name.get(info["realname"]) if info["realname"] else None
        phone_rows.append((pid, rn_id, phone_no, dev, info["model"]))

    # ---------- build sim cards ----------
    sim_seen: set[str] = set()
    sim_rows = []
    sim_id_by_phone: dict[str, int] = {}
    sim_id_by_device: dict[str, int] = {}
    sim_idx = 0
    for _, row in sim_df.iterrows():
        phone_no = clean_phone(row.get("手机号"))
        if not phone_no or phone_no in sim_seen:
            continue
        sim_seen.add(phone_no)
        sid = ID_SIM_BASE + sim_idx
        sim_idx += 1
        sim_id_by_phone[phone_no] = sid
        dev = clean_device_no(row.get("存放手机"))
        if dev and dev not in sim_id_by_device:
            sim_id_by_device[dev] = sid
        phone_id = phone_id_by_device.get(dev) if dev else None
        operator_raw = clean_text(row.get("营业厅")) or "联通卡"
        operator = OPERATOR_MAP.get(operator_raw, "UNICOM")
        is_primary = "YES" if str(row.get("主卡")).strip() in {"1", "1.0", "YES", "是"} else "NO"
        package_name = clean_text(row.get("卡套餐"))
        if package_name and package_name.endswith(".0"):
            package_name = package_name[:-2]
        sim_rows.append((sid, phone_id, phone_no, operator, is_primary, package_name))

    # 抖音「手机卡」列中的 11 位号：无「手机卡」Sheet 时补建 oa_sim_card
    for p in parsed_douyin:
        phone_no = p.sim_card_phone
        if not phone_no or phone_no in sim_seen:
            continue
        sim_seen.add(phone_no)
        sid = ID_SIM_BASE + sim_idx
        sim_idx += 1
        sim_id_by_phone[phone_no] = sid
        dev = p.device_no
        if dev and dev not in sim_id_by_device:
            sim_id_by_device[dev] = sid
        phone_id = phone_id_by_device.get(dev) if dev else None
        sim_rows.append((sid, phone_id, phone_no, "UNICOM", "NO", None))

    def resolve_sim_id(sim_phone: str | None, device_no: str | None) -> tuple[int | None, str | None]:
        """Match sim_card_id: 抖音「手机卡」列手机号，再按设备号回退。"""
        if sim_phone and sim_phone in sim_id_by_phone:
            return sim_id_by_phone[sim_phone], "sim_card_col"
        if device_no and device_no in sim_id_by_device:
            return sim_id_by_device[device_no], "device"
        return None, None

    def account_row_from_parsed(p: ParsedAccount, account_idx: int) -> tuple:
        aid = ID_ACCOUNT_BASE + account_idx
        company_name = p.company_name
        company_id = company_id_by_name.get(company_name) if company_name else None
        realname_id = realname_id_by_name.get(p.holder_name) if p.holder_name else None
        sim_phone = p.sim_card_phone
        dev = p.device_no
        sim_id, sim_match_method = resolve_sim_id(sim_phone, dev)
        if p.platform == "DOUYIN":
            if sim_match_method:
                sim_match_stats[sim_match_method] += 1
            else:
                sim_match_stats["none"] += 1
        phone_id = phone_id_by_device.get(dev) if dev else None
        phone_hash_val = phone_hash(sim_phone) if sim_phone else None
        account_type = (
            guess_account_type(company_name)
            if p.platform == "DOUYIN"
            else "PERSONAL_ACCOUNT"
        )
        pwd_enc = encrypt_aes_ecb(p.password_plain) if p.password_plain else None
        holder_uid = resolve_user_id(p.holder_name, user_map)
        op_uid = resolve_user_id(p.operator_name, user_map)
        return (
            aid,
            p.platform,
            account_type,
            p.account_name,
            p.external_account_id,
            company_id,
            realname_id,
            phone_id,
            sim_id,
            phone_hash_val,
            pwd_enc,
            p.short_video_status,
            p.live_status,
            holder_uid,
            op_uid,
            dev,
            sim_phone,
        )

    # ---------- build accounts ----------
    account_rows = []
    account_idx = 0
    sim_match_stats = {"sim_card_col": 0, "device": 0, "none": 0}
    operator_unresolved: set[str] = set()
    holder_unresolved: set[str] = set()
    douyin_with_sim_phone = sum(1 for p in parsed_douyin if p.sim_card_phone)
    douyin_with_device = sum(1 for p in parsed_douyin if p.device_no)

    for p in parsed_douyin + parsed_kuaishou:
        row = account_row_from_parsed(p, account_idx)
        account_idx += 1
        account_rows.append(row)
        if p.holder_name and row[13] is None:
            holder_unresolved.add(p.holder_name)
        if p.operator_name and row[14] is None:
            operator_unresolved.add(p.operator_name)

    credit_code_by_company_id = {cid: credit for cid, _name, credit in company_rows}
    phone_hash_by_device: dict[str, str] = {
        dev: phone_hash(phone_no) for _pid, _rn, phone_no, dev, _model in phone_rows
    }
    device_by_batch_phone_id = {pid: dev for dev, pid in phone_id_by_device.items()}

    def resolve_company_id_sql(batch_company_id: int | None) -> str:
        if not batch_company_id:
            return "NULL"
        credit = credit_code_by_company_id.get(batch_company_id)
        if not credit:
            return str(batch_company_id)
        return sql_coalesce_batch_row_id(
            "oa_company", TENANT_ID, "credit_code", sql_str(credit), batch_company_id
        )

    def resolve_phone_id_sql(device_no: str | None, batch_phone_id: int | None) -> str:
        if device_no and device_no in phone_hash_by_device:
            h = sql_str(phone_hash_by_device[device_no])
            return sql_coalesce_batch_row_id(
                "oa_phone", TENANT_ID, "phone_number_hash", h, batch_phone_id
            )
        if batch_phone_id:
            return str(batch_phone_id)
        return "NULL"

    def resolve_sim_card_id_sql(sim_phone_no: str | None, batch_sim_id: int | None) -> str:
        if sim_phone_no:
            h = sql_str(phone_hash(sim_phone_no))
            return sql_coalesce_batch_row_id(
                "oa_sim_card", TENANT_ID, "phone_number_hash", h, batch_sim_id
            )
        if batch_sim_id:
            return str(batch_sim_id)
        return "NULL"

    # ---------- write SQL ----------
    lines: list[str] = []
    lines.append("-- =============================================================================")
    lines.append(f"-- M4 账号资产批量导入 SQL · {IMPORT_TAG}")
    lines.append(f"-- 来源: {xlsx.name}")
    lines.append(f"-- 生成时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    lines.append(f"-- tenant_id = {TENANT_ID}")
    lines.append(f"-- phone/sim assigned_user_id / keeper_id = {args.assigned_user_id}（非 Excel 持有人/运营人）")
    lines.append(f"-- 依赖 Flyway: {REQUIRES_FLYWAY}（short_video_status / live_status / operator_user_id · V206 新增运营人列）")
    lines.append("-- holder_user_id：Excel「持有人」/快手「持有」经 user_map.json 写入（与表单「持有人」同字段）")
    lines.append(f"-- 抖音 Sheet: {dy_sheet} · 快手 header=1 双列拆分")
    lines.append(
        "-- 不导入列: "
        + "、".join(
            [
                "接蓝改号手机号",
                "卖家微信名称",
                "截图",
                "运营人费用",
                "登录手机账号/登录手机号",
                "分配小组/分配",
                "备注",
                "购买日期/收号日期",
                "购买价格/价格",
            ]
        )
    )
    lines.append("-- 未映射: 粉丝（可选）；ip_group_id 留空（不导分配小组）")
    lines.append("-- 加密: AES/ECB/PKCS5Padding, key=application ops.aes-key (seed 同款)")
    lines.append("-- 哈希: SHA-256(phone_plain)")
    lines.append("-- =============================================================================")
    lines.append("")
    lines.append("SET NAMES utf8mb4;")
    lines.append("START TRANSACTION;")
    lines.append("")

    if write_sql:
        emit_pre_import_cleanup_sql(lines, TENANT_ID)
        emit_rollback_cleanup_sql(lines, TENANT_ID)

    lines.append(f"-- ========== 1. 公司 oa_company ({len(company_rows)} 条) ==========")
    lines.append(
        "-- UPSERT：credit_code UPDATE → 批次 id UPDATE → INSERT（hash/id 均未占用时）"
    )
    for cid, name, credit in company_rows:
        emit_upsert_company(lines, cid, name, credit)
    lines.append("")

    id_card_enc = encrypt_aes_ecb(PLACEHOLDER_ID_CARD)
    lines.append(f"-- ========== 2. 实名人 oa_realname ({len(realname_rows)} 条) ==========")
    lines.append(f"-- 注意: Excel 无身份证，统一占位加密值 id_card={PLACEHOLDER_ID_CARD}")
    lines.append(
        "-- UPSERT：real_name UPDATE → 批次 id UPDATE → INSERT（姓名与 id 均未占用）"
    )
    lines.append(
        "-- 避免「id=110xxx 已占槽、同名行逻辑键未命中」时仍 INSERT 固定 id → 1062"
    )
    for rid, name, phone in realname_rows:
        emit_upsert_realname(lines, rid, name, phone, id_card_enc)
    lines.append("")

    lines.append(f"-- ========== 3. 手机设备 oa_phone ({len(phone_rows)} 条) ==========")
    lines.append(
        "-- UPSERT：phone_number_hash UPDATE → 批次 id UPDATE → INSERT（hash 与 id 均未占用）"
    )
    lines.append(
        "-- 避免「id=120005 已占槽、Excel 行 hash 为新号」时仅 NOT EXISTS(hash) 仍 INSERT 120005 → 1062"
    )
    for pid, rn_id, phone_no, dev, model in phone_rows:
        emit_upsert_phone(lines, pid, rn_id, phone_no, dev, model, args.assigned_user_id)
    lines.append("")

    lines.append(f"-- ========== 4. 手机卡 oa_sim_card ({len(sim_rows)} 条) ==========")
    lines.append(
        "-- UPSERT：phone_number_hash UPDATE → 批次 id UPDATE → INSERT（hash 与 id 均未占用）"
    )
    for sid, phone_id, phone_no, operator, is_primary, package_name in sim_rows:
        dev = device_by_batch_phone_id.get(phone_id) if phone_id else None
        phone_id_sql = resolve_phone_id_sql(dev, phone_id)
        emit_upsert_sim(
            lines,
            sid,
            phone_id_sql,
            phone_no,
            operator,
            is_primary,
            package_name,
            args.assigned_user_id,
        )
    lines.append("")

    dy_count = sum(1 for r in account_rows if r[1] == "DOUYIN")
    ks_count = sum(1 for r in account_rows if r[1] == "KUAISHOU")
    lines.append(f"-- ========== 5. 平台账号 oa_account (抖音 {dy_count} + 快手 {ks_count}) ==========")
    lines.append(
        "-- UPSERT：platform+ext UPDATE → 批次 id UPDATE → INSERT（uk 与 id 均未占用）"
    )
    lines.append(
        "-- 避免「id=140115 已占槽、uk 为新 external_id」时仅 NOT EXISTS(uk) 仍 INSERT 140115 → 1062"
    )
    lines.append("-- FK=COALESCE(uk, 批次 id 槽, 批次 id 字面量)")
    for row in account_rows:
        (
            aid,
            platform,
            account_type,
            account_name,
            ext_id,
            company_id,
            realname_id,
            phone_id,
            sim_id,
            phone_hash_val,
            pwd_enc,
            short_video_status,
            live_status,
            holder_user_id,
            operator_user_id,
            device_no,
            sim_phone_no,
        ) = row
        sql_row = (
            aid,
            platform,
            account_type,
            account_name,
            ext_id,
            resolve_company_id_sql(company_id),
            realname_id,
            resolve_phone_id_sql(device_no, phone_id),
            resolve_sim_card_id_sql(sim_phone_no, sim_id),
            phone_hash_val,
            pwd_enc,
            short_video_status,
            live_status,
            holder_user_id,
            operator_user_id,
        )
        emit_upsert_account(lines, sql_row)
    lines.append("")
    lines.append("COMMIT;")
    lines.append("")
    lines.append("-- END")

    sql_body = "\n".join(lines) + "\n"
    sql_path = Path(args.out_sql)
    shenyu_ops_path = sql_path.with_name("import-accounts-shenyu-ops-20260928.sql")
    patch_path = Path(args.patch_sql_out)
    patch_body = build_patch_user_fields_sql(account_rows)
    if write_sql:
        sql_path.write_text(sql_body, encoding="utf-8")
        shenyu_ops_path.write_text("USE shenyu-ops;\n" + sql_body, encoding="utf-8")
    if write_sql or args.patch_only:
        patch_path.write_text(patch_body, encoding="utf-8")

    stats = {
        "scope": "slim-20260928",
        "dry_run": not write_sql,
        "douyin_sheet": dy_sheet,
        "douyin_accounts": len(parsed_douyin),
        "kuaishou_accounts": len(parsed_kuaishou),
        "sim_sheet_rows": len(sim_df),
        "douyin_with_device_no": douyin_with_device,
        "douyin_with_sim_card_phone": douyin_with_sim_phone,
        "companies": len(company_rows),
        "realnames": len(realname_rows),
        "phones": len(phone_rows),
        "sim_cards": len(sim_rows),
        "accounts_douyin": dy_count,
        "accounts_kuaishou": ks_count,
        "sim_from_account_column": sum(
            1 for p in parsed_douyin if p.sim_card_phone
        ),
        "sim_skipped_no_phone_in_sheet": int(
            sum(
                1
                for _, row in sim_df.iterrows()
                if not clean_phone(row.get("手机号"))
            )
        )
        if len(sim_df)
        else 0,
        "accounts_missing_company_link": sum(1 for r in account_rows if r[1] == "DOUYIN" and not r[5]),
        "accounts_missing_realname_link": sum(1 for r in account_rows if r[1] == "DOUYIN" and not r[6]),
        "accounts_missing_sim_link": sum(1 for r in account_rows if r[1] == "DOUYIN" and not r[8]),
        "sim_match_by_sim_card_column": sim_match_stats["sim_card_col"],
        "sim_match_by_device": sim_match_stats["device"],
        "sim_match_unresolved_douyin": sim_match_stats["none"],
        "holder_unresolved_names": sorted(holder_unresolved),
        "holder_names_distinct": len(
            {p.holder_name for p in parsed_douyin + parsed_kuaishou if p.holder_name}
        ),
        "holder_names_mapped_distinct": len(
            {
                n
                for n in {p.holder_name for p in parsed_douyin + parsed_kuaishou if p.holder_name}
                if resolve_user_id(n, user_map)
            }
        ),
        "operator_unresolved_names": sorted(operator_unresolved),
        "import_no_columns": list(IMPORT_NO_COLUMNS),
        "with_password": sum(1 for r in account_rows if r[10]),
        "with_short_video_status": sum(1 for r in account_rows if r[11]),
        "with_live_status": sum(1 for r in account_rows if r[12]),
        "sample_mapping": sample_mapping(
            {"douyin_accounts": parsed_douyin, "kuaishou_accounts": parsed_kuaishou}
        ),
    }

    analysis = f"""# M4 账号 Excel 导入分析 · {IMPORT_TAG}（收窄范围）

> **0928 用户确认**：不导入 Set A/B 列；**不映射 ip_group_id**（无分配小组阻塞）；**不用登录手机账号/登录手机号** 做 SIM 关联。

## 0. 用户 SSOT 映射（Excel / 表单 / 导入 · 客户 2026-09-28）

| 来源 | 目标 | 说明 |
|------|------|------|
| Excel **「持有人」**（快手 L/R **「持有」**） | 系统 **持有人** `holder_user_id` / `holderUserId` | 与表单「持有人」同一字段；`user_map.json` |
| Excel **「运营人」** | 系统 **运营人** `operator_user_id` / `operatorUserId` | **V206**；`user_map.json` |
| 同上持有人姓名（合规） | **实名人** `realname_id` | 并行建 `oa_realname`；DOUYIN 仍配合 `company_id` 校验 |

## 1. 仍导入 / 不导入（按 Sheet）

### 1.1 抖音（`{dy_sheet}`，{len(dy_df)} 行 → {len(parsed_douyin)} 账号）

| 仍导入 | 映射 |
|--------|------|
| 抖音ID、抖音名称 | `external_account_id` / `account_name` |
| 账号认证主体 | `oa_company` → `company_id` |
| 持有人 | `holder_user_id` + `oa_realname` → `realname_id` |
| 手机 | `oa_phone.device_number` → `phone_id` |
| 手机卡（列内 **11 位手机号**） | `oa_sim_card` → `sim_card_id`；`phone_number_hash` |
| 短视频状态、直播状态 | `short_video_status` / `live_status` |
| 运营人 | `operator_user_id`（需 `user_map.json`） |
| 抖音密码 | `password_encrypted` |

| 不导入 | 说明 |
|--------|------|
| 接蓝改号手机号、卖家微信名称、截图 | Set A |
| 运营人费用、登录手机账号、分配小组、备注、购买日期、购买价格 | Set B |
| 粉丝 | 系统无专用字段，仍可选人工补录 |
| Unnamed 列 | 忽略 |

### 1.2 快手（header=1，{len(ks_df)} 行 → {len(parsed_kuaishou)} 账号）

| 仍导入 | 映射 |
|--------|------|
| 快手ID / 右侧 ID | `external_account_id`（双列拆分 L/R） |
| 快手账号昵称 / 持有 | `account_name` |
| 持有人 / 持有 | `holder_user_id` + `realname_id`（L/R 块） |
| 手机编号 / 右侧「手机号」列 | `phone_id`（**设备编号**，非登录号） |
| 仅发短视频、短+直播 | 快手状态字段 |
| 密码 / 密码.1 | `password_encrypted` |

| 不导入 | 说明 |
|--------|------|
| 分配小组、分配 | IP 组 |
| 登录手机号 | Set B 同类 |
| 价格、价格.1、收号日期 | 成本/采购 |
| Unnamed 列 | 忽略 |

### 1.3 手机卡 Sheet

| 状态 | 说明 |
|------|------|
| 本文件 **无**「手机卡」Sheet（0 行） | SIM 主数据改由抖音行内「手机卡」列 11 位号补建（{stats.get('douyin_with_sim_card_phone', douyin_with_sim_phone)} 条有号） |

## 2. Excel 概览

| Sheet | 行数 | 说明 |
|-------|------|------|
| 手机卡 | {len(sim_df)} | 有 Sheet 时仍为 SIM 主数据源 |
| 抖音 | {len(dy_df)} | 见 §1.1 |
| 快手 | {len(ks_df)} | 见 §1.2 |

## 3. 导入顺序（依赖链）

```
公司(oa_company) → 实名人(oa_realname) → 手机设备(oa_phone) → 手机卡(oa_sim_card) → 平台账号(oa_account)
```

## 3.1 PRE-IMPORT 清理（test/prod 必读）

SQL 事务开头含两段清理，**顺序不可颠倒**：

| 段落 | 注释标记 | 用途 |
|------|----------|------|
| 定向清理 | `-- PRE-IMPORT CLEANUP (test/prod)` | **test/prod 必用**：删 tenant 下**全部**抖音/快手 + 清 Excel 导入批次支撑数据 |
| ID 段回滚 | `-- ROLLBACK (re-import only)` | dev 仅重导 Excel 时用；**仅**删导入批次抖音/快手（id ≥ 140001）及支撑表，保留低 ID seed |

> 已移除旧版 16 表 tenant 全量 wipe（`DELETE FROM oa_account WHERE tenant_id=1` 等）。

### 删除范围（tenant_id={TENANT_ID}）

| 表 | PRE-IMPORT 条件 | ROLLBACK 条件 |
|----|-----------------|---------------|
| oa_account | `platform_type IN ('DOUYIN','KUAISHOU')`（tenant 全量，含历史手工录入） | 同上 **且** `id >= 140001` 或 `creator = '{IMPORT_TAG}'` |
| oa_platform_account_fan_group | 关联 PRE-IMPORT 抖音/快手 `account_id` | 关联 ROLLBACK 导入批次 `account_id` |
| oa_account_cost | 同上 | 同上 |
| oa_account_status_log | 同上 | 同上 |
| oa_collector_account_bind | 同上 | 同上 |
| oa_sim_card | `id >= 130001` 或 `creator = '{IMPORT_TAG}'`，且无剩余账号引用 | 同 PRE-IMPORT |
| oa_phone | `id >= 120001` 或 `creator = '{IMPORT_TAG}'`，且无剩余账号/SIM 引用 | 同 PRE-IMPORT |
| oa_realname | `id >= 110001` 或 `creator = '{IMPORT_TAG}'`，且无剩余账号/设备引用 | 同 PRE-IMPORT |
| oa_company | `id >= 100001` 或 `creator = '{IMPORT_TAG}'`，且无剩余账号引用 | 同 PRE-IMPORT |
| oa_realname_intermediary | 导入批次实名人 | 同 PRE-IMPORT |
| oa_company_expansion | 导入批次公司 | 同 PRE-IMPORT |

### 保留范围（不删除）

| 对象 | 说明 |
|------|------|
| oa_account（WECHAT_OFFICIAL / WECHAT_VIDEO / XIAOHONGSHU / WEWORK 等） | E2E / seed 其它平台账号（**任意 id**） |
| oa_wework_* / oa_personal_wechat_account | 企微、个微 E2E 数据 |
| oa_account_wechat_video_wework_rel / oa_wechat_official_cert_renewal | 微信三方关联、认证续期 |
| id < 100001 的 company/realname/phone/sim | seed 低 ID 支撑数据 |
| 低 id 抖音/快手（ROLLBACK 段） | 仅 ROLLBACK 时保留；PRE-IMPORT 会删 tenant 下全部抖音/快手 |
| oa_content / oa_order / oa_ip_group 等 | 非 M4 账号资产表（**不在清理范围**） |

## 4. 生成统计

```json
{json.dumps(stats, ensure_ascii=False, indent=2)}
```

## 5. 数据清洗规则

- **手机号**：仅保留 11 位数字；无效行跳过
- **SIM 去重**：按 phone_number_hash 唯一；重复手机号保留首条
- **tenant_id**：固定 `{TENANT_ID}`（dev 默认租户）
- **assigned_user_id / keeper_id**：默认 `{args.assigned_user_id}`（seed 用户，生产前请核对 Football system_users.id）
- **身份证**：Excel 无数据 → 占位 `{PLACEHOLDER_ID_CARD}` AES 加密
- **公司 credit_code**：合成 18 位 `91IMPORTxxxx...`（非真实工商码，生产需人工补录）
- **广电卡**：dict_sim_operator 无枚举，降级为 MOBILE

## 6. 跳过 / 缺失项（收窄后）

- **ip_group_id**：全部 NULL（不导分配小组 → **无 IP 组映射阻塞**）
- 抖音 **{stats['accounts_missing_company_link']}** 条无 company_id · **{stats['accounts_missing_realname_link']}** 条无 realname_id
- 抖音 SIM：`手机卡`列匹配 **{stats['sim_match_by_sim_card_column']}** · 设备回退 **{stats['sim_match_by_device']}** · 仍无 sim **{stats['sim_match_unresolved_douyin']}**（列内非 11 位号如 `226`/运营商简称无法绑卡）
- 抖音有设备号 **{stats['douyin_with_device_no']}** · 有「手机卡」11 位号 **{stats['douyin_with_sim_card_phone']}**
- 持有人未解析 **{len(stats['holder_unresolved_names'])}** 个姓名 · 运营人未解析 **{len(stats['operator_unresolved_names'])}** 个（维护 `user_map.json` 或 UI 补选）
- 持有人去重 **{stats['holder_names_distinct']}** 个姓名 · 已映射 **{stats['holder_names_mapped_distinct']}** 个
- 粉丝、Cookie、ICCID、oa_account_cost：**未导入**

## 6.1 平台账号能力缺口（收窄范围下）

| 能力 | 够用？ | 说明 |
|------|--------|------|
| 外部 ID / 昵称 / 密码 / 双状态 | ✅ | 抖音+快手均覆盖 |
| 持有人 → holderUserId + 实名人 | ✅ | 快手 R「持有」作持有人姓名 |
| 运营人 | ⚠️ | 仅抖音列；依赖 user_map |
| 设备 phone_id | ✅ | 「手机」「手机编号」等 |
| SIM sim_card_id | ⚠️ | 仅抖音「手机卡」11 位 + 可选 SIM Sheet；**不用**登录手机账号 |
| IP 组 | ➖ 刻意不导 | 导入后 UI/任务再绑 |
| 粉丝数 | ❌ | Excel 有列未入库 |
| 采购/成本/备注 | ❌ 刻意不导 | |

## 7. 产出文件

- SQL（通用）: `{sql_path.name}`
- SQL（shenyu-ops 专用，含 `USE shenyu-ops;`）: `{shenyu_ops_path.name}`
- 生成器: `generate_accounts_from_xlsx.py`
- 安全导入器: `apply-import-accounts.py`（**Windows 必用**，避免 PowerShell 管道把中文变成字面量 `?`）

## 8. 字符集与导入（prod / test / local）

### 8.1 根因说明

- 生成器以 **UTF-8** 写 SQL，文件内中文正确。
- 若经 **PowerShell 管道**（`Get-Content | mysql`）导入，Windows 会把无法转码的字节写成 **`?`（HEX 3F）** 落库，UI 显示 `????`，**不是前端问题**。
- 验证：`SELECT company_name, HEX(company_name) FROM oa_company WHERE id >= 100001 LIMIT 3;`
  - 错误：`????` / `3F3F3F3F...`
  - 正确：`湖北枫南邦商贸有限公司` / `E6B996...`（UTF-8 多字节）

### 8.2 推荐导入方式（全部环境）

> ⚠️ SQL 已内置 **PRE-IMPORT CLEANUP**：删除 `tenant_id={TENANT_ID}` 全部抖音/快手账号及 Excel 导入批次支撑数据；**保留**公众号/视频号/小红书等 E2E seed。test/prod 执行前请确认连接的是目标环境。

**方式 A — Python 安全导入（Windows / Linux 通用，推荐）**

```bash
python scripts/import/apply-import-accounts.py
# test/prod
python scripts/import/apply-import-accounts.py --host <host> --user <user> --password <pwd>
```

**方式 B — Bash / cmd 重定向（Linux / macOS / Git Bash）**

```bash
mysql --default-character-set=utf8mb4 -h 127.0.0.1 -P 3306 -u root -proot shenyu-ops < {shenyu_ops_path.name}
```

**方式 C — cmd.exe 原生重定向（Windows，勿用 PowerShell 管道）**

```cmd
chcp 65001
mysql --default-character-set=utf8mb4 -h 127.0.0.1 -P 3306 -u root -proot shenyu-ops < scripts\\import\\{shenyu_ops_path.name}
```

### 8.3 禁止方式

```powershell
# ❌ 禁止：PowerShell 管道会导致中文变 '?'
Get-Content -Path scripts/import/{shenyu_ops_path.name} -Raw -Encoding UTF8 | mysql ...
```

### 8.4 目标库

> **目标库必须是 `shenyu-ops`**（ops-server 实际读取），不是 `wd`。

### 8.5 dev 仅重导（保留 seed）

编辑 SQL，**注释掉** `-- PRE-IMPORT CLEANUP (test/prod)` 整段，仅保留 `-- ROLLBACK (re-import only)` 段后执行。

## 9. 阻塞 / 人工确认项

1. **assigned_user_id={args.assigned_user_id}** 是否为当前环境有效 Football 用户
2. **公司 credit_code** 为合成值，生产环境需替换真实统一社会信用代码
3. **实名人身份证** 全为占位，合规场景需补录真实证件
4. 设备无「手机卡」11 位号时使用合成设备号 `199xxxxxxxx`，需人工核对
5. ~~IP 组映射~~（已移除阻塞）
6. **运营人 user_map** 未覆盖姓名需补映射或导入后编辑
"""

    analysis_path = Path(args.out_analysis)
    analysis_path.write_text(analysis, encoding="utf-8")

    if write_sql:
        print(f"SQL written: {sql_path} ({sql_path.stat().st_size} bytes)")
    else:
        print("dry-run: SQL not written (PRE-IMPORT 未执行)")
    print(f"Analysis written: {analysis_path}")
    print(json.dumps(stats, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
