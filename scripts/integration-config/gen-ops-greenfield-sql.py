#!/usr/bin/env python3
"""Regenerate consolidated SQL files for ops-greenfield-production deploy pack.

Run from repo root:
  python scripts/integration-config/gen-ops-greenfield-sql.py

Prerequisites:
  1. Flyway SQL source (184 files) — set FLYWAY_SRC or checkout football-backend-saas submodule
  2. Run gen-ops-flyway-history.py first (generates ops-flyway-record-history.sql)

Outputs (docs/deploy/ops-greenfield-production/sql/):
  01-shenyu-ops-schema.sql
  02-shenyu-system-menus.sql
  03-shenyu-ops-seeds.sql

Sources:
  scripts/integration-config/ops-greenfield-sources/system/
  scripts/integration-config/ops-greenfield-sources/seeds/
"""
from __future__ import annotations

import os
import re
import sys
from datetime import date
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
INTEG_DIR = Path(__file__).resolve().parent
DEPLOY_SQL = REPO_ROOT / "docs/deploy/ops-greenfield-production/sql"
SOURCES = INTEG_DIR / "ops-greenfield-sources"
SYSTEM_DIR = SOURCES / "system"
SEEDS_DIR = SOURCES / "seeds"

ORDER_FILE = INTEG_DIR / "ops-flyway-migration-order.txt"
HISTORY_FILE = INTEG_DIR / "ops-flyway-record-history.sql"

OUT_SCHEMA = DEPLOY_SQL / "01-shenyu-ops-schema.sql"
OUT_SYSTEM = DEPLOY_SQL / "02-shenyu-system-menus.sql"
OUT_SEEDS = DEPLOY_SQL / "03-shenyu-ops-seeds.sql"

DEFAULT_FLYWAY_SRC = (
    REPO_ROOT
    / "football-backend-saas"
    / "football-module-ops"
    / "football-module-ops-server"
    / "src"
    / "main"
    / "resources"
    / "db"
    / "migration"
)

GENERATED_TAG = date.today().isoformat()

SYSTEM_SCRIPTS: list[tuple[str, Path | None, str]] = [
    ("01_baseline_ops_menus.sql", SYSTEM_DIR / "01_baseline_ops_menus.sql", "6100–6168 Ops baseline menus + super_admin role_menu"),
    ("02_menu_supplement.sql", SYSTEM_DIR / "02_menu_supplement.sql", "6175 all-tasks menu, collect path fixes, remove OOS menus"),
    ("05_work_task_dicts_v183.sql", SYSTEM_DIR / "05_work_task_dicts_v183.sql", "Work task 4 dict_type + 11 dict_data"),
    ("06_live_drain_v188.sql", SYSTEM_DIR / "06_live_drain_v188.sql", "LIVE_DRAIN marketing plan dict"),
    ("03_work_task_menus_v183.sql", SYSTEM_DIR / "03_work_task_menus_v183.sql", "6194–6196 work task menus + role_menu"),
    ("07_ops_six_roles_rbac.sql", SYSTEM_DIR / "07_ops_six_roles_rbac.sql", "ADR-064 six Ops roles + role_menu"),
    (
        "04_baseline_dicts.sql",
        None,
        "SKIPPED — Greenfield production has no wd DB; confirm Football dict_* exists",
    ),
]

SEED_SCRIPTS: list[tuple[str, Path, str]] = [
    ("02_ai_prompt_work_task.sql", SEEDS_DIR / "02_ai_prompt_work_task.sql", "WORK_TASK_WIN_PREDICTION AI prompt (V181 §3)"),
    ("03_sys_param_work_task.sql", SEEDS_DIR / "03_sys_param_work_task.sql", "work_task.default_template_id / default_node_id (V181 §4 + V182)"),
]

VERIFY_FILE = SEEDS_DIR / "01_prerequisite_sop_verify.sql"

RE_SET_NAMES = re.compile(r"^SET\s+NAMES\s+utf8mb4\s*;\s*$", re.IGNORECASE)
RE_USE = re.compile(r"^USE\s+`[^`]+`\s*;\s*$", re.IGNORECASE)

# Football system_* SSOT = shenyu-system (V163/V172 drop overlay copies in shenyu-ops).
# 01-shenyu-ops-schema.sql must not execute cross-DB system_* / wd.* writes — use 02-shenyu-system-menus.sql.
OPS_WHOLE_NOOP_MIGRATIONS: dict[str, str] = {
    "V153__system_user_author_data_tables.sql": (
        "Football overlay system_user_author / system_user_data in shenyu-ops. "
        "V163 + V172 DROP; SSOT = shenyu-system. No CREATE on greenfield."
    ),
    "V148__merge_ops_dict_to_football_manual.sql": (
        "wd.sys_dict_* → system_dict_* merge. Greenfield has no wd DB; Football baseline dict_* + 02 §05/06."
    ),
    "V137__sync_shenyu_system_menus.sql": (
        "Football menu baseline sync (~1300 rows). Greenfield: Football seed + 02-shenyu-system-menus.sql."
    ),
    "V145__hide_ops_author_menu.sql": "Hide duplicate OPS author menu — covered by 02 baseline menus.",
    "V146__remove_ops_login_log_menu.sql": "Remove duplicate OPS login-log menu — covered by 02 baseline menus.",
    "V159__split_task_my_and_all_menus.sql": "Split 我的/全部任务 menus — covered by 02_menu_supplement.sql.",
    "V162__repair_collect_menu_paths.sql": "Collect menu path repair — covered by 02_menu_supplement.sql.",
    "V166__rename_permission_oa_to_ops.sql": "oa:* → ops:* permission rename — covered by 02 baseline menus.",
    "V183__m2_work_task_menu_dict_fix.sql": (
        "Work-task menus + dicts on shenyu-system — covered by 02 §03_work_task_menus_v183 + §05_work_task_dicts_v183."
    ),
    "V190__drop_legacy_sys_harness.sql": (
        "Greenfield pack skips CREATE/seed for sys_dict_* / sys_operation_log; tables never exist — DROP is no-op."
    ),
    "V191__drop_legacy_sys_identity_harness.sql": (
        "Greenfield pack skips CREATE/seed for sys_tenant/sys_user*/sys_role*; tables never exist — DROP is no-op."
    ),
}

# V190/V191 终态 DROP — greenfield 01 不应 CREATE/seed 这些表（SSOT = shenyu-system Feign）
V190_V191_DROPPED_TABLES: frozenset[str] = frozenset(
    {
        "sys_tenant",
        "sys_user",
        "sys_user_token",
        "sys_user_role",
        "sys_role",
        "sys_role_permission",
        "sys_permission",
        "sys_dict_type",
        "sys_dict_data",
        "sys_operation_log",
    }
)

RE_SHENYU_SYSTEM_QUALIFIED = re.compile(r"`shenyu-system`\.", re.IGNORECASE)
RE_WD_QUALIFIED = re.compile(r"\bwd\.", re.IGNORECASE)
# Football overlay tables dropped from shenyu-ops by V163/V172 — DML belongs on shenyu-system only.
RE_FOOTBALL_SYSTEM_TABLE = re.compile(
    r"\b(system_menu|system_role_menu|system_dict_type|system_dict_data|"
    r"system_role|system_users|system_user_role|system_oauth2_\w+)\b",
    re.IGNORECASE,
)
RE_SAFE_SYSTEM_DROP = re.compile(
    r"^\s*DROP\s+TABLE\s+IF\s+EXISTS\s+`?(system_\w+|archive_system_\w+)`?\s*;\s*$",
    re.IGNORECASE,
)
RE_SYSTEM_BACKUP = re.compile(r"system_(menu|role_menu)_backup", re.IGNORECASE)
RE_DML_START = re.compile(r"^\s*(INSERT|UPDATE|DELETE|REPLACE)\b", re.IGNORECASE)


def flyway_sql_dir() -> Path:
    env = os.environ.get("FLYWAY_SRC")
    if env:
        return Path(env)
    legacy = REPO_ROOT / "docs/deploy/ops-greenfield-production/01-database-shenyu-ops/flyway-sql"
    if legacy.is_dir():
        return legacy
    return DEFAULT_FLYWAY_SRC


def read_text(path: Path) -> str:
    # utf-8-sig strips UTF-8 BOM (U+FEFF) so concatenated SQL never breaks MySQL near '-- ...'
    return path.read_text(encoding="utf-8-sig")


def write_text(path: Path, content: str) -> int:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")
    return content.count("\n") + (0 if content.endswith("\n") else 1)


def strip_leading_session_directives(content: str) -> str:
    lines = content.splitlines()
    idx = 0
    while idx < len(lines):
        stripped = lines[idx].strip()
        if not stripped or stripped.startswith("--"):
            idx += 1
            continue
        if RE_SET_NAMES.match(stripped) or RE_USE.match(stripped):
            idx += 1
            continue
        break
    body = "\n".join(lines[idx:]).strip("\n")
    return body + "\n" if body else ""


def strip_all_use_directives(content: str) -> str:
    """Remove any USE `...`; line — DB is selected on mysql CLI."""
    kept: list[str] = []
    for line in content.splitlines():
        if RE_USE.match(line.strip()):
            continue
        kept.append(line)
    body = "\n".join(kept).strip("\n")
    return body + "\n" if body else ""


def noop_cross_db_migration_block(filename: str, reason: str) -> str:
    return "\n".join(
        [
            f"-- [SKIPPED in shenyu-ops greenfield pack] {filename}",
            "-- Football system_* / wd.* SSOT = shenyu-system only (V163/V172 drop overlay in shenyu-ops).",
            f"-- Reason: {reason}",
            "-- Apply instead: docs/deploy/ops-greenfield-production/sql/02-shenyu-system-menus.sql",
            "SELECT 1;",
            "",
        ]
    )


def _strip_sql_string_literals(compact: str) -> str:
    """Remove string literal contents so remark text cannot false-trigger table detection."""
    return re.sub(r"'([^']|'')*'", "''", compact)


def _strip_sql_block_comments(compact: str) -> str:
    """Remove block comments so dead /* sys_dict_* seed */ blocks cannot false-trigger skips."""
    return re.sub(r"/\*.*?\*/", " ", compact, flags=re.DOTALL)


def _sql_check_surface(stmt: str) -> str:
    """Compact statement text safe for dropped-table / SSOT identifier detection."""
    compact = " ".join(line.strip() for line in stmt.splitlines() if line.strip() and not line.strip().startswith("--"))
    if not compact:
        return ""
    check = _strip_sql_block_comments(compact)
    return _strip_sql_string_literals(check)


def _statement_targets_shenyu_system_ssot(stmt: str) -> bool:
    """True when a statement must not run on shenyu-ops-only greenfield deploy."""
    check = _sql_check_surface(stmt)
    if not check:
        return False
    if RE_SYSTEM_BACKUP.search(check):
        return False
    if RE_SAFE_SYSTEM_DROP.match(check):
        return False
    if RE_SHENYU_SYSTEM_QUALIFIED.search(check) or RE_WD_QUALIFIED.search(check):
        return True
    if not RE_FOOTBALL_SYSTEM_TABLE.search(check):
        return False
    if RE_DML_START.search(check):
        return True
    if re.search(
        r"\b(FROM|JOIN|INTO)\s+`?(system_menu|system_role_menu|system_dict_\w+|system_role|system_users|system_user_role|system_oauth2_\w+)`?",
        check,
        re.IGNORECASE,
    ):
        return True
    return False


def _sql_scan_line_for_statement_end(line: str, in_block: bool, in_string: bool) -> tuple[bool, bool, bool]:
    """Return (in_block, in_string, statement_ends) after scanning one line."""
    i = 0
    n = len(line)
    statement_ends = False
    while i < n:
        if in_block:
            if i + 1 < n and line[i : i + 2] == "*/":
                in_block = False
                i += 2
                continue
            i += 1
            continue
        if in_string:
            if line[i] == "'":
                if i + 1 < n and line[i + 1] == "'":
                    i += 2
                    continue
                in_string = False
            i += 1
            continue
        if i + 1 < n and line[i : i + 2] == "/*":
            in_block = True
            i += 2
            continue
        if line[i] == "'":
            in_string = True
            i += 1
            continue
        if line[i] == "-" and i + 1 < n and line[i + 1] == "-":
            break
        if line[i] == ";":
            tail = line[i + 1 :].strip()
            if not tail or tail.startswith("--"):
                statement_ends = True
        i += 1
    return in_block, in_string, statement_ends


def split_sql_statements(content: str) -> list[str]:
    """Split on semicolons outside strings and block comments (Flyway files use both)."""
    statements: list[str] = []
    current: list[str] = []
    in_block = False
    in_string = False
    for line in content.splitlines(keepends=False):
        current.append(line)
        in_block, in_string, ends = _sql_scan_line_for_statement_end(line, in_block, in_string)
        if ends and not in_block:
            statements.append("\n".join(current))
            current = []
            in_string = False
    if current:
        statements.append("\n".join(current))
    return statements


def validate_greenfield_sql_landmines(sql: str, label: str) -> None:
    """Fail fast if generated SQL contains executable placeholder / skip lines."""
    issues: list[str] = []
    in_block = False
    for line_no, line in enumerate(sql.splitlines(), 1):
        stripped = line.strip()
        if not stripped:
            continue
        if in_block:
            if "*/" in stripped:
                after = stripped.split("*/", 1)[1].strip()
                in_block = False
                if after.startswith("...") and not after.startswith("--"):
                    issues.append(f"{label}:{line_no}: ellipsis after block comment")
            continue
        if "/*" in stripped and "*/" not in stripped:
            in_block = True
            continue
        if stripped.startswith("...") and not stripped.startswith("--"):
            issues.append(f"{label}:{line_no}: bare ellipsis line")
        if "[greenfield skip]" in line and not stripped.startswith("--"):
            issues.append(f"{label}:{line_no}: greenfield skip not commented")
        if "dict INSERTs removed" in line and not stripped.startswith("--"):
            issues.append(f"{label}:{line_no}: dict placeholder not commented")
    if issues:
        raise ValueError("Greenfield SQL landmines:\n" + "\n".join(issues))


def _statement_targets_v190_v191_dropped(stmt: str) -> bool:
    """True when a statement references V190/V191 dropped legacy sys_* harness tables."""
    check = _sql_check_surface(stmt)
    if not check:
        return False

    # DROP IF EXISTS on legacy harness tables is harmless on greenfield — leave executable.
    if re.match(r"^\s*DROP\s+TABLE\s+IF\s+EXISTS\s+", check, re.IGNORECASE):
        for table in V190_V191_DROPPED_TABLES:
            if re.search(rf"\b{re.escape(table)}\b", check, re.IGNORECASE):
                return False

    # After stripping string literals / block comments, any bare identifier reference
    # (CREATE/INSERT/SET subquery/FROM/JOIN/ALTER/INDEX/…) must be omitted — table never
    # exists on greenfield. COMMENT '… sys_user.id' and flyway script names stay safe.
    for table in V190_V191_DROPPED_TABLES:
        if re.search(rf"\b{re.escape(table)}\b", check, re.IGNORECASE):
            return True
    return False


def sanitize_ops_migration_sql(body: str, filename: str) -> tuple[str, bool]:
    """Strip cross-DB Football system_* / wd.* writes from a Flyway file for the shenyu-ops-only pack."""
    reason = OPS_WHOLE_NOOP_MIGRATIONS.get(filename)
    if reason:
        return noop_cross_db_migration_block(filename, reason), True

    kept: list[str] = []
    harness_skip_count = 0
    ssot_skip_count = 0
    for stmt in split_sql_statements(body):
        if _statement_targets_v190_v191_dropped(stmt):
            harness_skip_count += 1
        elif _statement_targets_shenyu_system_ssot(stmt):
            ssot_skip_count += 1
        else:
            kept.append(stmt)

    skipped_any = harness_skip_count > 0 or ssot_skip_count > 0
    parts: list[str] = []
    if harness_skip_count > 0:
        stmt_word = "statement" if harness_skip_count == 1 else "statements"
        parts.append(
            f"-- [greenfield skip] V190/V191 legacy sys_* omitted ({harness_skip_count} {stmt_word}) "
            "— SSOT = shenyu-system Feign"
        )
    if ssot_skip_count > 0:
        stmt_word = "statement" if ssot_skip_count == 1 else "statements"
        parts.append(
            f"-- [greenfield skip] Football system_* / wd.* / `shenyu-system` omitted "
            f"({ssot_skip_count} {stmt_word}) — apply via 02-shenyu-system-menus.sql"
        )
    parts.extend(kept)

    sanitized = "\n\n".join(part.rstrip("\n") for part in parts if part is not None)
    sanitized = patch_greenfield_migration_sql(sanitized, filename)
    return sanitized + ("\n" if sanitized else ""), skipped_any


RE_V150_SYS_ROLE_WHERE = re.compile(
    r"(INSERT INTO sys_role \(id, tenant_id, code, name, status, data_scope, remark, creator, updater\)\s*"
    r"SELECT 6, 1, 'ip_group_leader', 'IP组长', 'ENABLED', 'SELF',\s*"
    r"'seed · 与 Football system_role\.ip_group_leader 对齐', 'flyway', 'flyway'\s*"
    r"FROM DUAL\s*"
    r"WHERE NOT EXISTS \(\s*"
    r"SELECT 1 FROM sys_role r WHERE r\.code = 'ip_group_leader' AND r\.tenant_id = 1 AND r\.deleted = 0\s*"
    r"\)\s*)"
    r";?\s*"
    r"(?!AND NOT EXISTS \(\s*SELECT 1 FROM sys_role r WHERE r\.id = 6\s*\))",
    re.IGNORECASE | re.DOTALL,
)

V150_SYS_ROLE_ID_GUARD = """\
AND NOT EXISTS (
    SELECT 1 FROM sys_role r WHERE r.id = 6
)"""


def patch_greenfield_migration_sql(body: str, filename: str) -> str:
    """Apply greenfield-only idempotency fixes without changing Flyway JAR source."""
    if filename != "V150__seed_ip_group_leader_role.sql":
        return body

    def _add_id_guard(match: re.Match[str]) -> str:
        prefix = match.group(1).rstrip()
        if prefix.endswith(";"):
            prefix = prefix[:-1].rstrip()
        return (
            "-- [greenfield patch] Skip hardcoded id=6 when V74 DEPT_HEAD already occupies sys_role.id=6; "
            "V154 seeds ip_group_leader without fixed id.\n"
            + prefix
            + "\n"
            + V150_SYS_ROLE_ID_GUARD
            + "\n;\n"
        )

    patched, count = RE_V150_SYS_ROLE_WHERE.subn(_add_id_guard, body, count=1)
    return patched


def section_header(title: str, subtitle: str = "") -> list[str]:
    lines = ["", "-- " + "=" * 77, f"-- {title}"]
    if subtitle:
        lines.append(f"-- {subtitle}")
    lines.append("-- " + "=" * 77)
    lines.append("")
    return lines


def flyway_order_entries() -> list[str]:
    entries: list[str] = []
    for line in read_text(ORDER_FILE).splitlines():
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        if line.endswith(".java"):
            continue
        if not re.match(r"^V\d+__.*\.sql$", line):
            continue
        entries.append(line)
    return entries


def build_flyway_migrations(flyway_dir: Path) -> tuple[str, list[str]]:
    header = [
        "-- =============================================================================",
        "-- shenyu-ops — ALL Flyway SQL migrations (V1–V191, 186 files)",
        f"-- Generated: {GENERATED_TAG} by gen-ops-greenfield-sql.py — do not hand-edit",
        f"-- Source: {flyway_dir}",
        "-- Target DB: pass on mysql CLI, e.g. mysql -h HOST -u USER -p shenyu-ops < sql/01-shenyu-ops-schema.sql",
        "-- Note:   V113 is Java-only — see OPERATIONS-GUIDE.md",
        "-- Note:   V190/V191 targets: legacy sys_* CREATE/seed omitted (not commented); V190/V191 DROP sections are no-op",
        "-- Note:   Final shenyu-ops sys_* = sys_param + sys_message + sys_metadata_* + sys_notification_event only",
        "-- =============================================================================",
        "SET NAMES utf8mb4;",
        "",
    ]
    bodies: list[str] = []
    files_used: list[str] = []
    skipped_migrations: list[str] = []
    for filename in flyway_order_entries():
        path = flyway_dir / filename
        if not path.exists():
            raise FileNotFoundError(f"Missing {path}")
        files_used.append(filename)
        raw_body = strip_leading_session_directives(read_text(path))
        body, skipped = sanitize_ops_migration_sql(raw_body, filename)
        if skipped:
            skipped_migrations.append(filename)
        block = section_header(f"===== {filename} =====")
        block.append(body.rstrip("\n"))
        bodies.extend(block)
    if skipped_migrations:
        note = section_header(
            "Cross-DB system migration skips (informational)",
            f"{len(skipped_migrations)} migration(s) no-op'd or partially stripped in this pack",
        )
        for name in skipped_migrations:
            note.append(f"--   - {name}")
        note.append("")
        bodies = note + bodies
    return "\n".join(header + bodies) + "\n", files_used


def build_flyway_with_history(migrations_sql: str) -> str:
    if not HISTORY_FILE.exists():
        raise FileNotFoundError(f"Missing {HISTORY_FILE} — run gen-ops-flyway-history.py first")

    history_body = strip_leading_session_directives(read_text(HISTORY_FILE))
    header = [
        "-- =============================================================================",
        "-- shenyu-ops — ALL Flyway SQL migrations + flyway_schema_history (recommended DBA path)",
        f"-- Generated: {GENERATED_TAG} by gen-ops-greenfield-sql.py — do not hand-edit",
        "-- Target DB: pass on mysql CLI, e.g. mysql -h HOST -u USER -p shenyu-ops < sql/01-shenyu-ops-schema.sql",
        "-- Includes: 186 migrations + idempotent flyway_schema_history",
        "-- Note:   V113 Java migration excluded from history — JAR first start补跑",
        "-- Note:   V190/V191 legacy sys_* CREATE/seed omitted — see sql/02 + OPERATIONS-GUIDE Step 2",
        "-- Note:   Cross-DB Football system_* / wd.* writes are no-op'd here; run 02-shenyu-system-menus.sql",
        "-- Note:   V190 drops sys_dict_* + sys_operation_log; V191 drops sys_tenant/sys_user*/sys_role* (Feign SSOT)",
        "-- =============================================================================",
        "",
    ]
    history_section = section_header(
        "===== record-flyway-history.sql =====",
        "Idempotent INSERT into flyway_schema_history (186 SQL entries)",
    )
    history_section.append(history_body.rstrip("\n"))

    mig_lines = migrations_sql.splitlines()
    start = 0
    for i, line in enumerate(mig_lines):
        if RE_SET_NAMES.match(line.strip()):
            start = i + 1
            break
    mig_body = "\n".join(mig_lines[start:]).strip("\n")

    return "\n".join(header + ["SET NAMES utf8mb4;", ""] + [mig_body] + history_section) + "\n"


def build_system_scripts() -> str:
    header = [
        "-- =============================================================================",
        "-- shenyu-system — Ops menus / dicts / RBAC (Greenfield execution order)",
        f"-- Generated: {GENERATED_TAG} by gen-ops-greenfield-sql.py — do not hand-edit",
        "-- Schema SSOT: Beta test shenyu-system @ 110.42.49.224 (OPS-TEST-DB.md): menu.user_type, dict_data.value",
        "-- Target DB: pass on mysql CLI, e.g. mysql -h HOST -u USER -p shenyu-system < sql/02-shenyu-system-menus.sql",
        "-- Order:  01 → 02 → 05 → 06 → 03 → 07  (04 skipped on greenfield)",
        "-- =============================================================================",
        "SET NAMES utf8mb4;",
        "",
    ]
    parts: list[str] = list(header)
    for filename, path, description in SYSTEM_SCRIPTS:
        block = section_header(f"===== {filename} =====", description)
        if path is None:
            block.extend([
                "-- SKIPPED on greenfield production.",
                "-- Reason: 04_baseline_dicts.sql merges dict_* from legacy wd DB (V152).",
                "-- Action:  Confirm Football dict_* exists in shenyu-system (see OPERATIONS-GUIDE.md).",
                "",
            ])
        else:
            if not path.exists():
                raise FileNotFoundError(path)
            block.append(strip_all_use_directives(strip_leading_session_directives(read_text(path))).rstrip("\n"))
        parts.extend(block)
    return "\n".join(parts) + "\n"


def build_ops_seeds() -> str:
    if not VERIFY_FILE.exists():
        raise FileNotFoundError(VERIFY_FILE)

    header = [
        "-- =============================================================================",
        "-- shenyu-ops — business seeds (work task AI prompt + sys_param)",
        f"-- Generated: {GENERATED_TAG} by gen-ops-greenfield-sql.py — do not hand-edit",
        "--",
        "-- *** BEFORE RUNNING ***",
        "-- 1. Run prerequisite verify queries (see pointer below) or OPERATIONS-GUIDE.md Step 4",
        "-- 2. Edit {{WORK_TASK_DEFAULT_TEMPLATE_ID}} / {{WORK_TASK_DEFAULT_NODE_ID}}",
        "--",
        "-- Target DB: pass on mysql CLI, e.g. mysql -h HOST -u USER -p shenyu-ops < sql/03-shenyu-ops-seeds.sql",
        "-- =============================================================================",
        "SET NAMES utf8mb4;",
        "",
    ]
    verify_block = section_header(
        "===== 01_prerequisite_sop_verify.sql (reference only — not embedded) =====",
        "Run verification SELECTs from source file before seed writes",
    )
    verify_block.extend(
        [
            "-- Prerequisite verify SQL:",
            "--   scripts/integration-config/ops-greenfield-sources/seeds/01_prerequisite_sop_verify.sql",
            "-- See OPERATIONS-GUIDE.md Step 4 for execution order.",
            "",
        ]
    )

    parts: list[str] = list(header) + verify_block
    for filename, path, description in SEED_SCRIPTS:
        if not path.exists():
            raise FileNotFoundError(path)
        block = section_header(f"===== {filename} =====", description)
        if filename == "03_sys_param_work_task.sql":
            block.extend([
                "-- IMPORTANT: Replace {{WORK_TASK_DEFAULT_TEMPLATE_ID}} and {{WORK_TASK_DEFAULT_NODE_ID}} before executing.",
                "",
            ])
        block.append(strip_all_use_directives(strip_leading_session_directives(read_text(path))).rstrip("\n"))
        parts.extend(block)
    return "\n".join(parts) + "\n"


def main() -> int:
    flyway_dir = flyway_sql_dir()
    if not flyway_dir.is_dir():
        print(f"ERROR: Flyway source not found: {flyway_dir}", file=sys.stderr)
        print("Set FLYWAY_SRC to migration directory.", file=sys.stderr)
        return 1

    migrations, files_used = build_flyway_migrations(flyway_dir)
    if len(files_used) != 186:
        print(f"WARNING: expected 186 SQL migrations, got {len(files_used)}", file=sys.stderr)

    schema_sql = build_flyway_with_history(migrations)
    system_sql = build_system_scripts()
    seeds_sql = build_ops_seeds()

    results = [
        (OUT_SCHEMA, schema_sql),
        (OUT_SYSTEM, system_sql),
        (OUT_SEEDS, seeds_sql),
    ]
    for path, content in results:
        validate_greenfield_sql_landmines(content, path.name)
        lines = write_text(path, content)
        print(f"Wrote {path.relative_to(REPO_ROOT)} ({lines:,} lines)")

    print(f"Flyway migrations concatenated: {len(files_used)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
