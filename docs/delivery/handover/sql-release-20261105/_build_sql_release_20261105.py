#!/usr/bin/env python3
"""Assemble M-Live / ROI SQL release from SSOT — one .sql file per database."""

from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]  # wd repo root
OUT = Path(__file__).resolve().parent
UPDATES = ROOT / "football-backend-saas" / "sql" / "mysql" / "updates"
FLYWAY = (
    ROOT
    / "football-backend-saas"
    / "football-module-ops"
    / "football-module-ops-server"
    / "src"
    / "main"
    / "resources"
    / "db"
    / "migration"
)

# Legacy filenames removed on regenerate
# MySQL 8.0 default collations → 5.7 / MariaDB 10.x (handover import targets)
COLLATION_REPLACEMENTS = (
    ("utf8mb4_0900_ai_ci", "utf8mb4_unicode_ci"),
    ("utf8mb4_0900_as_ci", "utf8mb4_unicode_ci"),
    ("utf8mb4_0900_as_cs", "utf8mb4_bin"),
    ("utf8mb4_0900_bin", "utf8mb4_bin"),
)


def normalize_mysql57_collations(text: str) -> str:
    for old, new in COLLATION_REPLACEMENTS:
        text = text.replace(old, new)
    return text


# MySQL 8.0.13+ expression defaults → 5.7-friendly CURRENT_TIMESTAMP (session TZ)
_MYSQL57_DEFAULT_UTC_TS = re.compile(
    r"DEFAULT\s*\(\s*UTC_TIMESTAMP\s*\(\s*(\d+)\s*\)\s*\)",
    re.IGNORECASE,
)


def normalize_mysql57_defaults(text: str) -> str:
    return _MYSQL57_DEFAULT_UTC_TS.sub(r"DEFAULT CURRENT_TIMESTAMP(\1)", text)


_CREATE_ROUTINE_IF_NOT_EXISTS = re.compile(
    r"^(\s*)CREATE\s+(FUNCTION|PROCEDURE)\s+IF\s+NOT\s+EXISTS\s+"
    r"(?:`([A-Za-z0-9_]+)`|([A-Za-z0-9_]+))(.*)$",
    re.IGNORECASE,
)
_CREATE_TRIGGER_IF_NOT_EXISTS = re.compile(
    r"^(\s*)CREATE\s+TRIGGER\s+IF\s+NOT\s+EXISTS\s+"
    r"(?:`([A-Za-z0-9_]+)`|([A-Za-z0-9_]+))(.*)$",
    re.IGNORECASE,
)
_DELIMITER_LINE = re.compile(r"^\s*DELIMITER\s+(\S+)\s*$", re.IGNORECASE)


def _statement_terminator(stmt_term: str) -> str:
    return ";" if stmt_term == ";" else stmt_term


def _normalize_mysql57_delimiter_create_if_not_exists(
    text: str,
    create_pattern: re.Pattern[str],
    drop_sql: str,
    create_sql: str,
) -> str:
    """Rewrite CREATE … IF NOT EXISTS lines inside DELIMITER blocks (5.7 / MariaDB)."""
    lines = text.splitlines(keepends=True)
    out: list[str] = []
    stmt_term = ";"
    for line in lines:
        delim_match = _DELIMITER_LINE.match(line.rstrip("\r\n"))
        if delim_match:
            stmt_term = delim_match.group(1)
            out.append(line)
            continue
        body = line.rstrip("\r\n")
        suffix = line[len(body) :]
        match = create_pattern.match(body)
        if not match:
            out.append(line)
            continue
        indent, bt_name, plain_name, rest = (
            match.group(1),
            match.group(2),
            match.group(3),
            match.group(4),
        )
        name_token = f"`{bt_name}`" if bt_name else plain_name
        drop_end = _statement_terminator(stmt_term)
        out.append(f"{indent}{drop_sql.format(name=name_token)}{drop_end}{suffix}")
        out.append(f"{indent}{create_sql.format(name=name_token, rest=rest)}{suffix}")
    return "".join(out)


def normalize_mysql57_routines(text: str) -> str:
    """MySQL 8.0.29+ CREATE FUNCTION/PROCEDURE IF NOT EXISTS → DROP + CREATE."""
    lines = text.splitlines(keepends=True)
    out: list[str] = []
    stmt_term = ";"
    for line in lines:
        delim_match = _DELIMITER_LINE.match(line.rstrip("\r\n"))
        if delim_match:
            stmt_term = delim_match.group(1)
            out.append(line)
            continue
        body = line.rstrip("\r\n")
        suffix = line[len(body) :]
        match = _CREATE_ROUTINE_IF_NOT_EXISTS.match(body)
        if not match:
            out.append(line)
            continue
        indent, kind, bt_name, plain_name, rest = (
            match.group(1),
            match.group(2).upper(),
            match.group(3),
            match.group(4),
            match.group(5),
        )
        name_token = f"`{bt_name}`" if bt_name else plain_name
        drop_end = _statement_terminator(stmt_term)
        out.append(f"{indent}DROP {kind} IF EXISTS {name_token}{drop_end}{suffix}")
        out.append(f"{indent}CREATE {kind} {name_token}{rest}{suffix}")
    return "".join(out)


def normalize_mysql57_triggers(text: str) -> str:
    """MySQL 8.0.29+ CREATE TRIGGER IF NOT EXISTS → DROP TRIGGER + CREATE TRIGGER."""
    return _normalize_mysql57_delimiter_create_if_not_exists(
        text,
        _CREATE_TRIGGER_IF_NOT_EXISTS,
        drop_sql="DROP TRIGGER IF EXISTS {name}",
        create_sql="CREATE TRIGGER {name}{rest}",
    )


# Post-build verification (must stay 0 in generated .sql)
MYSQL57_VERIFY_PATTERNS: tuple[tuple[str, re.Pattern[str]], ...] = (
    ("utf8mb4_0900", re.compile(r"utf8mb4_0900", re.IGNORECASE)),
    ("DEFAULT (UTC_TIMESTAMP", re.compile(r"DEFAULT\s*\(\s*UTC_TIMESTAMP", re.IGNORECASE)),
    ("CREATE FUNCTION IF NOT EXISTS", re.compile(r"CREATE\s+FUNCTION\s+IF\s+NOT\s+EXISTS", re.IGNORECASE)),
    ("CREATE PROCEDURE IF NOT EXISTS", re.compile(r"CREATE\s+PROCEDURE\s+IF\s+NOT\s+EXISTS", re.IGNORECASE)),
    ("CREATE TRIGGER IF NOT EXISTS", re.compile(r"CREATE\s+TRIGGER\s+IF\s+NOT\s+EXISTS", re.IGNORECASE)),
    ("INVISIBLE column", re.compile(r"\bINVISIBLE\b", re.IGNORECASE)),
    ("RENAME COLUMN", re.compile(r"\bRENAME\s+COLUMN\b", re.IGNORECASE)),
    ("ALGORITHM=INSTANT", re.compile(r"ALGORITHM\s*=\s*INSTANT", re.IGNORECASE)),
    ("JSON_TABLE", re.compile(r"\bJSON_TABLE\s*\(", re.IGNORECASE)),
)


def verify_mysql57_output(path: Path, text: str) -> None:
    for label, pattern in MYSQL57_VERIFY_PATTERNS:
        hits = len(pattern.findall(text))
        if hits:
            raise RuntimeError(f"{path.name}: {hits} hit(s) for {label}")
        print(f"  verify {path.name}: {label} = 0")


def normalize_mysql57_handover_sql(text: str) -> str:
    return normalize_mysql57_triggers(
        normalize_mysql57_routines(
            normalize_mysql57_defaults(normalize_mysql57_collations(text))
        )
    )


_MYSQL57_PREREQ_HEADER = """-- ---------------------------------------------------------------------------
-- MySQL 5.7 / 托管 · 导入前 DBA 须知（本段注释-only，非 DDL）
-- Error 1419: log_bin=ON 且无 SUPER 时，含 CREATE FUNCTION 的例程段会失败。
--   DBA: SET GLOBAL log_bin_trust_function_creators = 1; 或 my.cnf / 云参数组 + 重启
--   校验: SHOW VARIABLES LIKE 'log_bin_trust_function_creators'; → ON/1
-- 本脚本 intentionally 不含 SET GLOBAL（无权限账号误跑会整包失败）。
-- 完整说明: 同目录 00-mysql57-prerequisites.sql 与 README.md「Error 1419」
-- 兼容: utf8mb4_unicode_ci、DROP+CREATE 例程/触发器；5.7 DEFAULT CURRENT_TIMESTAMP 按会话 TZ
-- ---------------------------------------------------------------------------

"""

_BINLOG_TRUST_COMMENT = """-- ---------------------------------------------------------------------------
-- 首个 DELIMITER 块（Section 03 存储过程）起 — 若上一段 1419 未处理，此处开始可能报错
-- ---------------------------------------------------------------------------

"""


def prepend_binlog_trust_comment_before_first_delimiter(text: str) -> str:
    """Comment-only pointer before first DELIMITER block (Section 03+ routines)."""
    lines = text.splitlines(keepends=True)
    for i, line in enumerate(lines):
        if _DELIMITER_LINE.match(line.rstrip("\r\n")):
            return "".join(lines[:i]) + _BINLOG_TRUST_COMMENT + "".join(lines[i:])
    return text


_ROUTINE_COMPANION = re.compile(
    r"^\s*(CALL\s+[A-Za-z0-9_`]+\s*\(\s*\)\s*;|"
    r"DROP\s+PROCEDURE\s+(?:IF\s+EXISTS\s+)?[A-Za-z0-9_`]+\s*;)\s*$",
    re.IGNORECASE,
)
_DROP_PROCEDURE_BEFORE_BLOCK = re.compile(
    r"^\s*DROP\s+PROCEDURE\s+IF\s+EXISTS\s+[A-Za-z0-9_`]+\s*;\s*$",
    re.IGNORECASE,
)


def _is_delimiter_block_opener(line: str) -> bool:
    match = _DELIMITER_LINE.match(line.rstrip("\r\n"))
    return bool(match and match.group(1) != ";")


def _skip_binlog_pointer_comment(lines: list[str], i: int) -> int:
    """Skip prepend_binlog_trust_comment_before_first_delimiter comment block."""
    if i >= len(lines):
        return i
    body = lines[i].rstrip("\r\n")
    if "首个 DELIMITER 块" not in body:
        return i
    while i < len(lines):
        body = lines[i].rstrip("\r\n")
        if body.strip() == "--" or body.startswith("--"):
            i += 1
            if body.startswith("--") and "----" in body and i > 0:
                peek = lines[i].rstrip("\r\n") if i < len(lines) else ""
                if not peek.strip().startswith("--"):
                    break
            continue
        break
    return i


def split_ops_schema_and_routines(full_text: str) -> tuple[str, str]:
    """Schema = full SQL minus DELIMITER blocks and one-shot procedure companions."""
    lines = full_text.splitlines(keepends=True)
    schema_lines: list[str] = []
    routine_lines: list[str] = []
    pending_routine_prefix: list[str] = []
    i = 0
    n = len(lines)
    while i < n:
        body = lines[i].rstrip("\r\n")
        i = _skip_binlog_pointer_comment(lines, i)
        if i >= n:
            break
        body = lines[i].rstrip("\r\n")

        if _DROP_PROCEDURE_BEFORE_BLOCK.match(body):
            pending_routine_prefix.append(lines[i])
            i += 1
            continue

        if _is_delimiter_block_opener(body):
            routine_lines.extend(pending_routine_prefix)
            pending_routine_prefix.clear()
            while i < n:
                routine_lines.append(lines[i])
                term_match = _DELIMITER_LINE.match(lines[i].rstrip("\r\n"))
                if term_match and term_match.group(1) == ";":
                    i += 1
                    break
                i += 1
            while i < n and _ROUTINE_COMPANION.match(lines[i].rstrip("\r\n")):
                routine_lines.append(lines[i])
                i += 1
            continue

        if pending_routine_prefix:
            schema_lines.extend(pending_routine_prefix)
            pending_routine_prefix.clear()

        schema_lines.append(lines[i])
        i += 1

    if pending_routine_prefix:
        schema_lines.extend(pending_routine_prefix)
    return "".join(schema_lines), "".join(routine_lines)


def strip_ops_body_after_header(full_text: str) -> str:
    match = re.search(r"\n-- ={10,}\n-- Section 01:", full_text)
    if not match:
        raise RuntimeError("ops SQL: Section 01 banner not found")
    return full_text[match.start() + 1 :]


# M-Live 投产比运维最小包：不含可选钉钉 Section 15（操作审计已从 SSOT 移除，见 ADR-085）
MLIVE_ROI_MINIMAL_EXCLUDE_SECTIONS: frozenset[str] = frozenset()

# Section 03 列扩展：全量 routines 用存储过程；minimal 用 PREPARE 内联（无 CREATE PROCEDURE）
OA_ACCOUNT_COST_INLINE_ALTERS = """
-- oa_account_cost 扩展列（与 ops_roi_approval_cost_20261026 存储过程等价，无 DELIMITER）
SET @needs_cost_date := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_account_cost' AND COLUMN_NAME = 'cost_date'
);
SET @sql_cost_date := IF(@needs_cost_date = 0,
  'ALTER TABLE oa_account_cost ADD COLUMN cost_date DATE NULL COMMENT ''Recognized expense date; legacy records fall back to pay_date''',
  'SELECT ''skip: oa_account_cost.cost_date exists'' AS notice');
PREPARE stmt_cost_date FROM @sql_cost_date; EXECUTE stmt_cost_date; DEALLOCATE PREPARE stmt_cost_date;

SET @needs_source_type := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_account_cost' AND COLUMN_NAME = 'source_type'
);
SET @sql_source_type := IF(@needs_source_type = 0,
  'ALTER TABLE oa_account_cost ADD COLUMN source_type VARCHAR(16) NOT NULL DEFAULT ''MANUAL''',
  'SELECT ''skip: oa_account_cost.source_type exists'' AS notice');
PREPARE stmt_source_type FROM @sql_source_type; EXECUTE stmt_source_type; DEALLOCATE PREPARE stmt_source_type;

SET @needs_source_key := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_account_cost' AND COLUMN_NAME = 'source_key'
);
SET @sql_source_key := IF(@needs_source_key = 0,
  'ALTER TABLE oa_account_cost ADD COLUMN source_key CHAR(64) NULL',
  'SELECT ''skip: oa_account_cost.source_key exists'' AS notice');
PREPARE stmt_source_key FROM @sql_source_key; EXECUTE stmt_source_key; DEALLOCATE PREPARE stmt_source_key;

SET @needs_source_candidate := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_account_cost' AND COLUMN_NAME = 'source_candidate_id'
);
SET @sql_source_candidate := IF(@needs_source_candidate = 0,
  'ALTER TABLE oa_account_cost ADD COLUMN source_candidate_id CHAR(36) NULL',
  'SELECT ''skip: oa_account_cost.source_candidate_id exists'' AS notice');
PREPARE stmt_source_candidate FROM @sql_source_candidate; EXECUTE stmt_source_candidate; DEALLOCATE PREPARE stmt_source_candidate;

SET @needs_approval_business := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_account_cost' AND COLUMN_NAME = 'approval_business_id'
);
SET @sql_approval_business := IF(@needs_approval_business = 0,
  'ALTER TABLE oa_account_cost ADD COLUMN approval_business_id VARCHAR(128) NULL',
  'SELECT ''skip: oa_account_cost.approval_business_id exists'' AS notice');
PREPARE stmt_approval_business FROM @sql_approval_business; EXECUTE stmt_approval_business; DEALLOCATE PREPARE stmt_approval_business;

SET @needs_approval_instance := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_account_cost' AND COLUMN_NAME = 'approval_instance_id'
);
SET @sql_approval_instance := IF(@needs_approval_instance = 0,
  'ALTER TABLE oa_account_cost ADD COLUMN approval_instance_id VARCHAR(256) NULL',
  'SELECT ''skip: oa_account_cost.approval_instance_id exists'' AS notice');
PREPARE stmt_approval_instance FROM @sql_approval_instance; EXECUTE stmt_approval_instance; DEALLOCATE PREPARE stmt_approval_instance;

SET @needs_author_id := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_account_cost' AND COLUMN_NAME = 'author_id'
);
SET @sql_author_id := IF(@needs_author_id = 0,
  'ALTER TABLE oa_account_cost ADD COLUMN author_id BIGINT NULL',
  'SELECT ''skip: oa_account_cost.author_id exists'' AS notice');
PREPARE stmt_author_id FROM @sql_author_id; EXECUTE stmt_author_id; DEALLOCATE PREPARE stmt_author_id;

SET @needs_attr_ip := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_account_cost' AND COLUMN_NAME = 'attribution_ip_group_id'
);
SET @sql_attr_ip := IF(@needs_attr_ip = 0,
  'ALTER TABLE oa_account_cost ADD COLUMN attribution_ip_group_id BIGINT NULL',
  'SELECT ''skip: oa_account_cost.attribution_ip_group_id exists'' AS notice');
PREPARE stmt_attr_ip FROM @sql_attr_ip; EXECUTE stmt_attr_ip; DEALLOCATE PREPARE stmt_attr_ip;

SET @needs_payment_state := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_account_cost' AND COLUMN_NAME = 'payment_state'
);
SET @sql_payment_state := IF(@needs_payment_state = 0,
  'ALTER TABLE oa_account_cost ADD COLUMN payment_state VARCHAR(16) NOT NULL DEFAULT ''LEGACY''',
  'SELECT ''skip: oa_account_cost.payment_state exists'' AS notice');
PREPARE stmt_payment_state FROM @sql_payment_state; EXECUTE stmt_payment_state; DEALLOCATE PREPARE stmt_payment_state;

SET @needs_paid_ref := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_account_cost' AND COLUMN_NAME = 'paid_reference'
);
SET @sql_paid_ref := IF(@needs_paid_ref = 0,
  'ALTER TABLE oa_account_cost ADD COLUMN paid_reference VARCHAR(200) NULL',
  'SELECT ''skip: oa_account_cost.paid_reference exists'' AS notice');
PREPARE stmt_paid_ref FROM @sql_paid_ref; EXECUTE stmt_paid_ref; DEALLOCATE PREPARE stmt_paid_ref;

SET @needs_payment_note := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_account_cost' AND COLUMN_NAME = 'payment_note'
);
SET @sql_payment_note := IF(@needs_payment_note = 0,
  'ALTER TABLE oa_account_cost ADD COLUMN payment_note TEXT NULL',
  'SELECT ''skip: oa_account_cost.payment_note exists'' AS notice');
PREPARE stmt_payment_note FROM @sql_payment_note; EXECUTE stmt_payment_note; DEALLOCATE PREPARE stmt_payment_note;

SET @needs_pay_date_nullable := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_account_cost'
    AND COLUMN_NAME = 'pay_date' AND IS_NULLABLE = 'NO'
);
SET @sql_pay_date_nullable := IF(@needs_pay_date_nullable > 0,
  'ALTER TABLE oa_account_cost MODIFY COLUMN pay_date DATE NULL COMMENT ''Actual payment date; unknown until cashier registration''',
  'SELECT ''skip: oa_account_cost.pay_date already nullable'' AS notice');
PREPARE stmt_pay_date_nullable FROM @sql_pay_date_nullable; EXECUTE stmt_pay_date_nullable; DEALLOCATE PREPARE stmt_pay_date_nullable;

SET @needs_uq_source := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_account_cost' AND INDEX_NAME = 'uq_oa_cost_source_key'
);
SET @sql_uq_source := IF(@needs_uq_source = 0,
  'ALTER TABLE oa_account_cost ADD UNIQUE KEY uq_oa_cost_source_key(tenant_id,source_key)',
  'SELECT ''skip: uq_oa_cost_source_key exists'' AS notice');
PREPARE stmt_uq_source FROM @sql_uq_source; EXECUTE stmt_uq_source; DEALLOCATE PREPARE stmt_uq_source;

SET @needs_ix_cost_date := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_account_cost' AND INDEX_NAME = 'ix_oa_cost_date'
);
SET @sql_ix_cost_date := IF(@needs_ix_cost_date = 0,
  'ALTER TABLE oa_account_cost ADD KEY ix_oa_cost_date(tenant_id,cost_date,account_id)',
  'SELECT ''skip: ix_oa_cost_date exists'' AS notice');
PREPARE stmt_ix_cost_date FROM @sql_ix_cost_date; EXECUTE stmt_ix_cost_date; DEALLOCATE PREPARE stmt_ix_cost_date;
"""


def patch_section03_inline_alters(schema_text: str) -> str:
    marker = "-- Routine is removed after execution; no credentials or tenant-specific source config."
    if marker not in schema_text:
        raise RuntimeError("minimal ops SQL: Section 03 routine placeholder not found")
    return schema_text.replace(
        marker,
        marker + OA_ACCOUNT_COST_INLINE_ALTERS,
        1,
    )


def roi_minimal_bootstrap_section_specs() -> list[tuple[str, str, str, str]]:
    return [
        (
            "01A",
            "M1 IP 组空表（bootstrap / ROI JOIN）",
            "20261106_minimal_oa_ip_group_for_roi.sql",
            read_updates_ops_body("20261106_minimal_oa_ip_group_for_roi.sql"),
        ),
        (
            "01B",
            "M5 oa_account / oa_account_cost 基表",
            "20261106_minimal_oa_account_bootstrap_for_roi.sql",
            read_updates_ops_body("20261106_minimal_oa_account_bootstrap_for_roi.sql"),
        ),
    ]


def inject_roi_minimal_bootstrap(schema_text: str) -> str:
    """Insert 01A/01B after Section 01, before Section 02 (minimal-only M1/M5 stubs)."""
    needle = f"\n-- {'=' * 77}\n-- Section 02:"
    idx = schema_text.find(needle)
    if idx < 0:
        raise RuntimeError("minimal ops SQL: Section 02 banner not found")
    chunks: list[str] = []
    for num, title, ssot, body in roi_minimal_bootstrap_section_specs():
        chunks.append(section_banner(num, title, ssot))
        chunks.append(normalize_mysql57_handover_sql(body))
        if not body.endswith("\n"):
            chunks.append("\n")
    return schema_text[:idx] + "".join(chunks) + schema_text[idx:]


OPS_ROI_MINIMAL_HEADER = (
    """-- =============================================================================
-- 用途：M-Live 投产比 · 运维最小 SQL（表结构 / 种子 / 条件 ALTER）
-- 目标库：shenyu-ops
-- 范围：Section 01、01A–01B、02–03、05–07、09–14、14B（**不含** Section 15 钉钉；**不含** roi_operation_log / 库内审计）
-- 01A：oa_ip_group* 空表；01B：oa_account / oa_account_cost 基表（20261106 补丁已并入本文件）
-- 14B：roi_attachment / roi_wecom_media 增加 infra_file_* 与 storage_name 长度（FileApi SSOT：20261106_roi_infra_file_storage.sql）
-- 幂等性：是；**无** CREATE FUNCTION / PROCEDURE / TRIGGER / DELIMITER（无需 log_bin_trust_function_creators）
-- 平台全量（含 Section 03 存储过程等例程）：shenyu-ops-schema.sql + shenyu-ops-routines.sql 或 shenyu-ops-full.sql
-- 生成：python docs/delivery/handover/sql-release-20261105/_build_sql_release_20261105.py
-- =============================================================================

SET NAMES utf8mb4;
USE `shenyu-ops`;

"""
)


OPS_SCHEMA_HEADER = (
    """-- =============================================================================
-- 用途：M-Live 投产比（ROI）· shenyu-ops 表结构 / 种子 / 条件 ALTER（无函数·无触发器）
-- 目标库：shenyu-ops
-- 执行顺序：Section 01 → 14、14B（必做）；Section 15 为 [OPTIONAL]（见 README）
-- 幂等性：是（CREATE IF NOT EXISTS、information_schema 条件 ALTER、INSERT IGNORE）
-- 1419：本文件不含 DELIMITER 例程/触发器，无 SUPER 账号可直接导入。
--       审计函数/触发器及 Section 03 oa_account_cost 列扩展 → shenyu-ops-routines.sql
--       （须 DBA 先设 log_bin_trust_function_creators=1，见 DBA-1419-一次性设置.md）
-- 回滚：生产需备份 shenyu-ops（至少 roi_* / ops_oa_* / oa_account_cost）；无自动回滚。
-- 生成：python docs/delivery/handover/sql-release-20261105/_build_sql_release_20261105.py
-- =============================================================================

SET NAMES utf8mb4;
USE `shenyu-ops`;

"""
)

ROUTINES_HEADER = """-- =============================================================================
-- shenyu-ops 例程 / 触发器补充包（DELIMITER 块）
-- 目标库：shenyu-ops
-- 【必须】DBA 一次性设置：log_bin_trust_function_creators = 1
--   详见同目录 DBA-1419-一次性设置.md 与 00-mysql57-prerequisites.sql
--   校验：SHOW VARIABLES LIKE 'log_bin_trust_function_creators'; → ON / 1
-- 执行顺序：在 shenyu-ops-schema.sql 成功导入之后执行本文件
-- 或：DBA 已开 trust 后可直接导入 shenyu-ops-full.sql（= shenyu-ops.sql）整包
-- 生成：python docs/delivery/handover/sql-release-20261105/_build_sql_release_20261105.py
-- =============================================================================

SET NAMES utf8mb4;
USE `shenyu-ops`;

"""


MYSQL57_VERIFY_SCHEMA_PATTERNS: tuple[tuple[str, re.Pattern[str]], ...] = (
    ("DELIMITER block", re.compile(r"^\s*DELIMITER\s+\S+", re.IGNORECASE | re.MULTILINE)),
    ("CREATE FUNCTION", re.compile(r"^\s*CREATE\s+FUNCTION\b", re.IGNORECASE | re.MULTILINE)),
    ("CREATE PROCEDURE", re.compile(r"^\s*CREATE\s+PROCEDURE\b", re.IGNORECASE | re.MULTILINE)),
    ("CREATE TRIGGER", re.compile(r"^\s*CREATE\s+TRIGGER\b", re.IGNORECASE | re.MULTILINE)),
)


def verify_ops_schema_output(path: Path, text: str) -> None:
    for label, pattern in MYSQL57_VERIFY_SCHEMA_PATTERNS:
        hits = len(pattern.findall(text))
        if hits:
            raise RuntimeError(f"{path.name}: {hits} hit(s) for {label}")
        print(f"  verify {path.name}: {label} = 0")


LEGACY_OUTPUTS = (
    "shenyu-ops-ROI-schema-20261105.sql",
    "shenyu-system-ROI-native-read-and-dict-20261105.sql",
    "shenyu-wecom-ROI-audit-triggers-OPTIONAL-20261105.sql",
    "shenyu-ops-ROI-dingtalk-fieldmap-OPTIONAL-20261105.sql",
    "shenyu-pay-NO-CHANGES.md",
    "shenyu-member-NO-CHANGES.md",
    "shenyu-match-NO-CHANGES.md",
    "shenyu-wecom.sql",
    "NO-DDL-databases.md",
)


_ROI_OPERATION_LOG_TABLE = re.compile(
    r"CREATE TABLE IF NOT EXISTS roi_operation_log\b[\s\S]*?;\s*\n?",
    re.IGNORECASE,
)
_ROI_OPERATION_LOG_PREREQ = re.compile(
    r"^.*20261026_roi_operation_logs\.sql.*\n",
    re.MULTILINE,
)
_AUDIT_TRIGGER_NAME = re.compile(r"_audit_v\d", re.IGNORECASE)


def strip_roi_operation_audit(text: str) -> str:
    """M-Live 不交付库内操作审计：去掉 roi_operation_log 表/触发器及对 SSOT 的引用。"""
    text = _ROI_OPERATION_LOG_PREREQ.sub("", text)
    text = _ROI_OPERATION_LOG_TABLE.sub("", text)
    lines = text.splitlines(keepends=True)
    out: list[str] = []
    i = 0
    n = len(lines)
    while i < n:
        delim_match = _DELIMITER_LINE.match(lines[i].rstrip("\r\n"))
        if delim_match and delim_match.group(1) != ";":
            block: list[str] = []
            while i < n:
                block.append(lines[i])
                term_match = _DELIMITER_LINE.match(lines[i].rstrip("\r\n"))
                if term_match and term_match.group(1) == ";":
                    i += 1
                    break
                i += 1
            block_text = "".join(block)
            if (
                "roi_operation_log" in block_text
                or _AUDIT_TRIGGER_NAME.search(block_text)
                or re.search(r"\broi_audit_(leaf|text)_v1\b", block_text, re.IGNORECASE)
            ):
                continue
            out.extend(block)
            continue
        out.append(lines[i])
        i += 1
    text = "".join(out)
    text = re.sub(r"^.*禁止删除审计日志.*\n", "", text, flags=re.MULTILINE)
    return text.strip() + "\n"


def read_updates(name: str) -> str:
    path = UPDATES / name
    if not path.is_file():
        raise FileNotFoundError(f"SSOT missing: {path}")
    return strip_roi_operation_audit(path.read_text(encoding="utf-8"))


def read_updates_ops_body(name: str) -> str:
    """Strip USE/SET NAMES from an updates/*.sql shenyu-ops fragment for handover merge."""
    text = read_updates(name)
    text = re.sub(r"^SET NAMES utf8mb4;\s*", "", text, count=1, flags=re.MULTILINE)
    text = text.replace("USE `shenyu-ops`;", "", 1).strip() + "\n"
    return text


def read_flyway(name: str) -> str:
    return (FLYWAY / name).read_text(encoding="utf-8").strip() + "\n"


def extract_shenyu_ops_from_local_feature() -> str:
    text = read_updates("20261023_01_local_feature_schema.sql")
    marker = "USE `shenyu-ops`;"
    idx = text.find(marker)
    if idx < 0:
        raise RuntimeError("20261023_01: shenyu-ops section not found")
    return text[idx:].strip() + "\n"


def section_banner(num: str, title: str, ssot: str) -> str:
    return (
        f"\n-- {'=' * 77}\n"
        f"-- Section {num}: {title}\n"
        f"-- SSOT: {ssot}\n"
        f"-- {'=' * 77}\n\n"
    )


def optional_section_banner(title: str, ssot: str) -> str:
    return (
        f"\n-- {'=' * 77}\n"
        f"-- [OPTIONAL] {title}\n"
        f"-- SSOT: {ssot}\n"
        f"-- 跳过：非 tenant=1 或未核对钉钉字段时可注释/不执行本段（见 README.md）\n"
        f"-- {'=' * 77}\n\n"
    )


OPS_HEADER = (
    """-- =============================================================================
-- 用途：M-Live 投产比（ROI）· shenyu-ops 增量结构 + 可选钉钉字段映射
-- 目标库：shenyu-ops
-- 执行顺序：Section 01 → 14、14B（必做）；Section 15 为 [OPTIONAL]（见 README）
-- 幂等性：是（CREATE IF NOT EXISTS、information_schema 条件 ALTER、INSERT IGNORE）
-- 回滚：生产需备份 shenyu-ops（至少 roi_* / ops_oa_* / oa_account_cost）；无自动回滚。
--       IP 审计函数回滚见 Section 08 注释（20261026_roi_ip_audit_projection_rollback.sql）。
-- 关联：ADR-084（ROI 运行时统一 ops-server :48094）；Flyway V205/V206 见 Section 12–13
-- 生成：python docs/delivery/handover/sql-release-20261105/_build_sql_release_20261105.py
-- =============================================================================

"""
    + _MYSQL57_PREREQ_HEADER
    + """
SET NAMES utf8mb4;
USE `shenyu-ops`;

"""
)

SYSTEM_HEADER = """-- =============================================================================
-- 用途：M-Live 投产比 · 原生只读边界（ROI 媒体委托读）+ ROI 分析维度字典
-- 目标库：shenyu-system
-- 执行顺序：在 shenyu-ops.sql 之后、ops-server 发版前（见 README.md）
-- 幂等性：是（CREATE IF NOT EXISTS；字典 NOT EXISTS / ON DUPLICATE KEY UPDATE）
-- 回滚：生产需备份；边界表可停用行；字典勿删历史业务引用
-- 关联：ADR-084；native_read_boundary_20261030.md；Flyway V42 dict_roi_dimension
-- 生成：python docs/delivery/handover/sql-release-20261105/_build_sql_release_20261105.py
-- 说明：本库脚本无 CREATE FUNCTION/触发器，无需 log_bin_trust_function_creators（1419）
-- =============================================================================

SET NAMES utf8mb4;
USE `shenyu-system`;

"""

DINGTALK_BODY_HEADER = """-- tenant_id=1 钉钉审批字段 ID 与 ops_oa_template 登记
-- 警告：含本公司 process_code / corp_id；其他租户勿直接执行，需按环境改 WHERE 条件
-- 幂等性：UPDATE 幂等；INSERT … NOT EXISTS
-- 回滚：自备份 roi_process.field_map 与 ops_oa_template 恢复

"""

DICT_ROI_SYSTEM = """
-- Section B: dict_roi_dimension（Flyway V42 → shenyu-system 读路径）
-- DictService @DS("system")；与 ops 库 sys_dict_* 解耦

INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT 'ROI分析维度', 'dict_roi_dimension', 0, 'M-Live/ROI handoff 20261105 from V42', 'sql-release-20261105', NOW(), 'sql-release-20261105', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM system_dict_type WHERE type = 'dict_roi_dimension' AND deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, 'IP组', 'IP_GROUP', 'dict_roi_dimension', 0, '', '', 'V42', 'sql-release-20261105', NOW(), 'sql-release-20261105', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (
  SELECT 1 FROM system_dict_data WHERE dict_type = 'dict_roi_dimension' AND value = 'IP_GROUP' AND deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '账号', 'ACCOUNT', 'dict_roi_dimension', 0, '', '', 'V42', 'sql-release-20261105', NOW(), 'sql-release-20261105', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (
  SELECT 1 FROM system_dict_data WHERE dict_type = 'dict_roi_dimension' AND value = 'ACCOUNT' AND deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '人员', 'PERSON', 'dict_roi_dimension', 0, '', '', 'V42', 'sql-release-20261105', NOW(), 'sql-release-20261105', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (
  SELECT 1 FROM system_dict_data WHERE dict_type = 'dict_roi_dimension' AND value = 'PERSON' AND deleted = b'0'
);
"""


def ops_section_specs() -> list[tuple[str, str, str, str]]:
    return [
        ("01", "ROI + OA 镜像基础表", "20261023_01_local_feature_schema.sql (shenyu-ops 段)", extract_shenyu_ops_from_local_feature()),
        ("02", "OA 费用行归因 ops_oa_cost_link", "20261025_oa_cost_links.sql", read_updates("20261025_oa_cost_links.sql")),
        ("03", "审批成本候选 + oa_account_cost 扩展列", "ops_roi_approval_cost_20261026.sql", read_updates("ops_roi_approval_cost_20261026.sql")),
        ("05", "组织目录 / IP 组范围", "20261026_roi_org_directory.sql", read_updates("20261026_roi_org_directory.sql")),
        ("06", "岗位→角色默认映射（tenant=1）", "20261026_roi_org_role_defaults.sql", read_updates("20261026_roi_org_role_defaults.sql")),
        ("07", "企微管理扩展表", "20261026_roi_wecom_management_restore.sql", read_updates("20261026_roi_wecom_management_restore.sql")),
        ("09", "作者/运营责任 + 费用来源/归属", "roi_author_cost_foundation_20261029.sql", read_updates("roi_author_cost_foundation_20261029.sql")),
        ("10", "费用业务分类 roi_cost_classification", "roi_cost_business_classification_20261030.sql", read_updates("roi_cost_business_classification_20261030.sql")),
        ("11", "媒体只读委托关系", "20261030_roi_media_read_delegation.sql", read_updates("20261030_roi_media_read_delegation.sql")),
        ("12", "Flyway V205 持有人 + M2 发布账号", "V205__m4_holder_m2_publish_account.sql", read_flyway("V205__m4_holder_m2_publish_account.sql")),
        ("13", "Flyway V206 抖快双状态 + 运营人", "V206__oa_account_short_video_live_operator.sql", read_flyway("V206__oa_account_short_video_live_operator.sql")),
        ("14", "oa_account 作者 + 封禁 JSON", "20261102_oa_account_author_ban.sql", read_updates("20261102_oa_account_author_ban.sql")),
        (
            "14B",
            "ROI 附件/企微素材 · infra FileApi 列 + storage_name 长度",
            "20261106_roi_infra_file_storage.sql",
            read_updates_ops_body("20261106_roi_infra_file_storage.sql"),
        ),
    ]


def build_shenyu_ops(
    *,
    exclude_sections: frozenset[str] | None = None,
    include_optional_dingtalk: bool = True,
    header: str = OPS_HEADER,
) -> str:
    skip = exclude_sections or frozenset()
    chunks = [header]
    for num, title, ssot, body in ops_section_specs():
        if num in skip:
            continue
        chunks.append(section_banner(num, title, ssot))
        chunks.append(body)
        if not body.endswith("\n"):
            chunks.append("\n")

    if include_optional_dingtalk:
        dingtalk = read_updates("20261025_roi_approval_field_map.sql")
        dingtalk = dingtalk.replace("SET NAMES utf8mb4;", "", 1)
        dingtalk = dingtalk.replace("USE `shenyu-ops`;", "", 1).strip() + "\n"
        chunks.append(
            optional_section_banner(
                "Section 15: tenant=1 钉钉审批 field_map + ops_oa_template",
                "20261025_roi_approval_field_map.sql",
            )
        )
        chunks.append(DINGTALK_BODY_HEADER)
        chunks.append(dingtalk)
    return "".join(chunks)


def build_shenyu_ops_roi_minimal() -> str:
    ops_core = normalize_mysql57_handover_sql(
        build_shenyu_ops(
            exclude_sections=MLIVE_ROI_MINIMAL_EXCLUDE_SECTIONS,
            include_optional_dingtalk=False,
            header="",
        )
    )
    schema_body, _ = split_ops_schema_and_routines(ops_core)
    schema_body = patch_section03_inline_alters(schema_body)
    schema_body = inject_roi_minimal_bootstrap(schema_body)
    return OPS_ROI_MINIMAL_HEADER + strip_ops_body_after_header(schema_body)


def build_shenyu_system() -> str:
    native = read_updates("native_read_boundary_20261030.sql")
    native = native.replace("USE `shenyu-system`;", "", 1).strip() + "\n"
    chunks = [
        SYSTEM_HEADER,
        section_banner("A", "原生只读边界表", "native_read_boundary_20261030.sql"),
        native,
        section_banner("B", "dict_roi_dimension", "V42__dict_roi_dimension.sql (system 读路径)"),
        DICT_ROI_SYSTEM.strip() + "\n",
    ]
    return "".join(chunks)


def build_readme() -> str:
    return """# M-Live 投产比 SQL Release · 2026-11-05

Handover：**一库一文件**，从 SSOT 合并可重复执行的 ROI/OA 结构变更。勿直连生产；与 [ADR-084](../../../adr/ADR-084-ROI-unify-ops-server.md) **ops-server（:48094）** 同窗口发版。

## M-Live 投产比 · 运维最小发布（默认）

仅表结构/种子/条件 ALTER，**不含** `roi_operation_log` / 库内审计触发器（ADR-085）及 Section 15 钉钉可选段。

**2026-11-06**：`shenyu-ops-roi-minimal.sql` 已含 Section **01A**（`oa_ip_group*` 空表）与 **01B**（`oa_account` / `oa_account_cost` 基表），以及 **14B**（`roi_attachment` / `roi_wecom_media` 的 `infra_file_*` 列与 `storage_name` 长度，SSOT `20261106_roi_infra_file_storage.sql`）。若环境已导入旧版 minimal，可另跑 `../sql-release-20261106/shenyu-ops-oa_account_cost-bootstrap.sql`（幂等；与 01B+Section 03 重叠可跳过）。

| 顺序 | 文件 | 库 |
|---|---|---|
| 1 | **`shenyu-ops-roi-minimal.sql`** | shenyu-ops |
| 2 | **`shenyu-system.sql`** | shenyu-system（原生只读边界 + 字典；与 ops 边界相关时必跑） |

无需 `log_bin_trust_function_creators`。**操作日志 Tab / `GET …/audit-logs` 不在 M-Live 范围（ADR-085）。**

平台全量（含 Section 03 等例程）：`shenyu-ops-schema.sql` + `shenyu-ops-routines.sql`（或 DBA 已开 trust 时 `shenyu-ops-full.sql`）。详见 `RUNME.md`。

## MySQL 5.7 / MariaDB 10.x 兼容性

SSOT 可面向 MySQL 8 编写；`_build_sql_release_20261105.py` 写出前对合并结果做下列变换（生成物 collation 统一为 `utf8mb4_unicode_ci` / `utf8mb4_bin`，非 MySQL 8 默认 `utf8mb4_0900_*`）：

| SSOT（MySQL 8+） | 生成物（5.7 / MariaDB 10.x） |
|---|---|
| `utf8mb4_0900_ai_ci` 等 | `utf8mb4_unicode_ci` 或 `utf8mb4_bin` |
| `DEFAULT (UTC_TIMESTAMP(n))` | `DEFAULT CURRENT_TIMESTAMP(n)` |
| `CREATE FUNCTION` / `PROCEDURE IF NOT EXISTS` | 同 DELIMITER 块内 `DROP … IF EXISTS` + `CREATE …` |
| `CREATE TRIGGER IF NOT EXISTS` | 同 DELIMITER 块内 `DROP TRIGGER IF EXISTS` + `CREATE TRIGGER` |

**语义注意**

- `DEFAULT CURRENT_TIMESTAMP(n)` 在 MySQL 5.7 按**会话时区**写入，非 UTC；触发器/DML 中的 `UTC_TIMESTAMP(3)` 仍为 UTC。生产若要求默认 UTC，导入前 `SET time_zone = '+00:00'` 或统一服务器时区。
- 函数/过程/触发器采用 **DROP + CREATE** 以实现可重复导入；重复执行会短暂移除已有例程，请在低峰或停写窗口执行 `shenyu-ops-routines.sql`。

**未自动变换（DBA 知晓）**

- `CHECK (...)` 约束：MySQL 5.7 接受语法但**不强制**（8.0.16+ 才强制）；MariaDB 10.2+ InnoDB 会强制。本 release 保留 SSOT 的 CHECK 定义。
- 脚本内 `JSON_*` 函数：需 MySQL 5.7.8+ / MariaDB 10.2+（本 release 目标版本已满足）。

重新生成后脚本会自检上述「坏模式」计数为 0（见 `_build_sql_release_20261105.py` 内 `MYSQL57_VERIFY_PATTERNS`）。

### Error 1419（binlog + CREATE FUNCTION）

无 SUPER、`log_bin=ON`、`log_bin_trust_function_creators=OFF` 时，**整包** `shenyu-ops.sql` / `shenyu-ops-full.sql` 会在 Section 03 起失败。**不能**靠改 SQL 内容绕过，须 DBA 开 trust 变量。

| 角色 | 推荐文件 |
|---|---|
| 普通运维（无 GLOBAL 权限） | 先 `shenyu-ops-schema.sql`（表/种子，**不触发 1419**）→ 请 DBA 按 `DBA-1419-一次性设置.md` 开 trust → 再 `shenyu-ops-routines.sql` |
| DBA 已开 trust | 可一次导入 `shenyu-ops-full.sql` 或 `shenyu-ops.sql`（与 full 内容相同） |

| 项 | 说明 |
|---|---|
| 运维 copy-paste | `DBA-1419-一次性设置.md` |
| 前置说明 | `00-mysql57-prerequisites.sql`（**不含** `SET GLOBAL`） |
| DBA 命令 | `SET GLOBAL log_bin_trust_function_creators = 1;` |
| 持久化 | 自建 `my.cnf`；阿里云 RDS / PolarDB / 腾讯云 CDB：**参数组 + 重启** |
| 校验 | `SHOW VARIABLES LIKE 'log_bin_trust_function_creators';` → `ON` / `1` |

`shenyu-system.sql` **无** CREATE FUNCTION/触发器，**不会** 1419。

**本 release 必执行**：`shenyu-ops-schema.sql` +（DBA 后）`shenyu-ops-routines.sql`，以及 `shenyu-system.sql`。`shenyu-pay` / `shenyu-member` / `shenyu-match` 无 DDL。

## 文件

| 文件 | 库 | 说明 |
|---|---|---|
| `00-mysql57-prerequisites.sql` | — | 1419 背景与校验（非必跑 DDL） |
| `DBA-1419-一次性设置.md` | — | **给 DBA/运维** 的参数组与 SET GLOBAL 步骤 |
| **`shenyu-ops-roi-minimal.sql`** | shenyu-ops | **M-Live 运维默认**：Section 01、01A–01B、02–03、05–07、09–14、14B；无例程/触发器 |
| `shenyu-ops-schema.sql` | shenyu-ops | 平台全量第一步：Section 01–03、05–07、09–14、14B 表/种子（无 DELIMITER） |
| `shenyu-ops-routines.sql` | shenyu-ops | 平台第二步（须 trust）：函数/触发器 + Section 03 存储过程版列扩展 |
| `shenyu-ops-full.sql` / `shenyu-ops.sql` | shenyu-ops | 完整单文件（schema + routines）；**仅** DBA 已开 trust 时使用 |
| `shenyu-system.sql` | shenyu-system | 原生只读边界表 + `dict_roi_dimension` |

## 执行顺序

1. **备份** `shenyu-ops`、`shenyu-system`。
2. `mysql … shenyu-ops < shenyu-ops-schema.sql`（无钉钉时跳过 schema 内 **Section 15 `[OPTIONAL]`**）。
3. **DBA** 按 `DBA-1419-一次性设置.md` 设置 `log_bin_trust_function_creators = 1` 并校验。
4. `mysql … shenyu-ops < shenyu-ops-routines.sql`（或步骤 3 已完成时直接 `shenyu-ops-full.sql` 代替 2+4）。
5. `mysql … shenyu-system < shenyu-system.sql`
6. 部署 **ops-server**；冒烟 `GET /admin-api/ops/roi/bootstrap`。

测试：`scripts/run-handover-sql-test.ps1`（默认只跑 schema + system；`MYSQL_TRUST_CREATORS=1` 时追加 routines）。速查 `RUNME.md`。

## Flyway 注意（ops-server · shenyu-ops）

| Flyway | 与本包对应 |
|---|---|
| V205 | `shenyu-ops.sql` Section 12 |
| V206 | Section 13 |
| V42 `dict_roi_dimension` | `shenyu-system.sql` Section B |

若生产 **Flyway 已开启**：V205/V206 与本包 12–13 等价，可先手工 SQL 再发版，或仅发版由 Flyway 执行（勿重复执行同一 DDL）。ROI 主体表无 V207+，SSOT 为 `sql/mysql/updates/` Section 01–11。

## 重新生成

```bash
python docs/delivery/handover/sql-release-20261105/_build_sql_release_20261105.py
```

SSOT：`football-backend-saas/sql/mysql/updates/`、`football-module-ops-server/.../db/migration/V205|V206*.sql`。
"""


def remove_legacy_outputs() -> None:
    for name in LEGACY_OUTPUTS:
        path = OUT / name
        if path.is_file():
            path.unlink()
            print(f"Removed legacy {name}")


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    remove_legacy_outputs()

    ops_full_path = OUT / "shenyu-ops-full.sql"
    ops_path = OUT / "shenyu-ops.sql"
    ops_schema_path = OUT / "shenyu-ops-schema.sql"
    ops_routines_path = OUT / "shenyu-ops-routines.sql"
    ops_minimal_path = OUT / "shenyu-ops-roi-minimal.sql"
    sys_path = OUT / "shenyu-system.sql"
    readme_path = OUT / "README.md"

    ops_core = normalize_mysql57_handover_sql(build_shenyu_ops())
    ops_minimal = build_shenyu_ops_roi_minimal()
    schema_body, routines_body = split_ops_schema_and_routines(ops_core)
    ops_full = prepend_binlog_trust_comment_before_first_delimiter(ops_core)
    ops_schema = OPS_SCHEMA_HEADER + strip_ops_body_after_header(schema_body)
    ops_routines = ROUTINES_HEADER + routines_body.strip() + "\n"

    for path in (ops_full_path, ops_path):
        path.write_text(ops_full, encoding="utf-8", newline="\n")
    ops_schema_path.write_text(ops_schema, encoding="utf-8", newline="\n")
    ops_routines_path.write_text(ops_routines, encoding="utf-8", newline="\n")
    ops_minimal_path.write_text(ops_minimal, encoding="utf-8", newline="\n")
    sys_path.write_text(
        normalize_mysql57_handover_sql(build_shenyu_system()),
        encoding="utf-8",
        newline="\n",
    )
    readme_path.write_text(build_readme(), encoding="utf-8", newline="\n")

    def lc(p: Path) -> int:
        return len(p.read_text(encoding="utf-8").splitlines())

    print(f"Wrote {ops_full_path.name}: {lc(ops_full_path)} lines")
    print(f"Wrote {ops_path.name}: {lc(ops_path)} lines (same as full)")
    print(f"Wrote {ops_schema_path.name}: {lc(ops_schema_path)} lines")
    print(f"Wrote {ops_routines_path.name}: {lc(ops_routines_path)} lines")
    print(f"Wrote {ops_minimal_path.name}: {lc(ops_minimal_path)} lines")
    print(f"Wrote {sys_path.name}: {lc(sys_path)} lines")
    print(f"Wrote {readme_path.name}")
    print("MySQL 5.7 verify:")
    verify_mysql57_output(ops_full_path, ops_full_path.read_text(encoding="utf-8"))
    verify_ops_schema_output(ops_schema_path, ops_schema_path.read_text(encoding="utf-8"))
    verify_ops_schema_output(ops_minimal_path, ops_minimal_path.read_text(encoding="utf-8"))
    verify_mysql57_output(ops_minimal_path, ops_minimal_path.read_text(encoding="utf-8"))
    verify_mysql57_output(sys_path, sys_path.read_text(encoding="utf-8"))


if __name__ == "__main__":
    main()
