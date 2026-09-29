#!/usr/bin/env python3
"""Regenerate shenyu-ops-FULL-*.sql from split parts in this directory."""

from __future__ import annotations

from pathlib import Path

DIR = Path(__file__).resolve().parent

PARTS = {
    "A": DIR / "01-shenyu-ops-schema-V205-V206.sql",
    "B": DIR / "02-shenyu-ops-flyway-history-insert.sql",
    "C": DIR / "03-shenyu-ops-preclean-douyin-kuaishou-ONLY.sql",
    "D": DIR / "04-shenyu-ops-data-import-0928-OPTIONAL.sql",
    "E": DIR / "05-shenyu-ops-patch-user-fields-OPTIONAL.sql",
    "F": DIR / "verify-schema-V205-V206.sql",
}

HEADER_PROD = """-- =============================================================================
-- shenyu-ops · 生产一条龙 SQL · 2026-09-28
-- 文件：shenyu-ops-FULL-prod-20260928.sql
--
-- ⚠️ 执行前必须备份 shenyu-ops（全库或 oa_* 相关表）。
-- ⚠️ 默认假设 tenant_id = 1；若生产默认租户不是 1，禁止直接执行 Section D/E。
-- ⚠️ 请勿在本机对生产直连执行；由 DBA 在变更窗口手工执行。
--
-- 分段说明（可按需跳过）：
--   Section A — 架构 V205/V206（幂等 DDL）· **建议始终执行**
--   Section B — flyway_schema_history · **默认注释**；Nacos flyway.enabled=false 时取消注释
--   Section C — 抖快预清理（仅 DOUYIN/KUAISHOU，不删支撑表）· **默认注释**；重导抖快时取消注释
--   Section D — 0928 数据导入（UPSERT）· **默认注释**；业务确认后取消注释
--   Section E — holder/operator/双状态 PATCH · **默认注释**；导入后字段仍 NULL 时取消注释
--   Section F — 只读校验 · 可随时执行
--
-- 生产分步推荐：01 →（可选）03 抖快预清理 → 04 数据 UPSERT。
-- 若需「架构 + 数据」一次跑通（含抖快预清理 + UPSERT），请用：
--   shenyu-ops-FULL-schema-and-data-20260928.sql
--
-- 测试/本地全量替换（含 PRE-IMPORT 删支撑表批次）：
--   scripts/import/import-accounts-shenyu-ops-20260928.sql
--
-- 重新生成本文件：python docs/deploy/prod-sync-20260928/_merge_prod_sync_sql.py
-- =============================================================================

USE `shenyu-ops`;

"""

HEADER_SCHEMA_DATA = """-- =============================================================================
-- shenyu-ops · 生产一条龙 SQL（架构 + 抖快预清理 + 0928 数据）· 2026-09-28
-- 文件：shenyu-ops-FULL-schema-and-data-20260928.sql
--
-- ⚠️ 执行前必须备份 shenyu-ops（全库或 oa_* 相关表）。
-- ⚠️ 默认假设 tenant_id = 1；确认 ID 段（>=100001 等）不与生产冲突。
-- ⚠️ Section C 会删除 tenant=1 **全部** DOUYIN/KUAISHOU（含子表），**不**删公司/实名人/设备/SIM。
-- ⚠️ Section D 为 UPSERT；与 Excel 同 id 或同 (platform, external_id) 时刷新字段，不改 id。
--
-- 分段说明：
--   Section A — 架构 V205/V206（幂等 DDL）
--   Section B — flyway history · **默认注释**（见 UNCOMMENT IF flyway disabled）
--   Section C — 抖快预清理 · **默认已启用**（重新导入抖快时执行；支撑表仍 UPSERT 不批量删）
--   Section D — 0928 数据导入 · **默认已启用**
--   Section E — PATCH · **默认注释**
--   Section F — 只读校验
--
-- 仅架构 / 可选数据请用：shenyu-ops-FULL-prod-20260928.sql
-- =============================================================================

USE `shenyu-ops`;

"""


def read_part(key: str) -> str:
    path = PARTS[key]
    text = path.read_text(encoding="utf-8")
    return text.rstrip() + "\n"


def comment_block(body: str, marker: str) -> str:
    lines = body.splitlines()
    out = [f"-- {marker}", "-- " + "-" * 77]
    for line in lines:
        if line.strip() == "":
            out.append("--")
        else:
            out.append("-- " + line)
    out.append(f"-- END {marker}")
    out.append("")
    return "\n".join(out)


def section_banner(letter: str, title: str) -> str:
    return (
        f"\n-- {'=' * 77}\n"
        f"-- Section {letter}: {title}\n"
        f"-- {'=' * 77}\n\n"
    )


def build(
    *,
    enable_data: bool,
    enable_preclean: bool,
    enable_flyway: bool,
    enable_patch: bool,
) -> str:
    chunks: list[str] = []
    chunks.append(HEADER_SCHEMA_DATA if enable_data else HEADER_PROD)

    chunks.append(section_banner("A", "Schema V205 / V206 (from 01-shenyu-ops-schema-V205-V206.sql)"))
    chunks.append(read_part("A"))

    chunks.append(section_banner("B", "Optional flyway_schema_history (from 02)"))
    if enable_flyway:
        chunks.append(read_part("B"))
    else:
        chunks.append(
            comment_block(
                read_part("B"),
                "UNCOMMENT IF flyway disabled (spring.flyway.enabled=false)",
            )
        )

    chunks.append(
        section_banner(
            "C",
            "Optional DOUYIN/KUAISHOU preclean only (from 03, NO support-table DELETE)",
        )
    )
    if enable_preclean:
        chunks.append(read_part("C"))
    else:
        chunks.append(
            comment_block(
                read_part("C"),
                "UNCOMMENT TO PRECLEAN DOUYIN/KUAISHOU before re-import (then run Section D / 04)",
            )
        )

    chunks.append(section_banner("D", "Optional 0928 data import UPSERT (from 04)"))
    if enable_data:
        chunks.append(read_part("D"))
    else:
        chunks.append(
            comment_block(
                read_part("D"),
                "UNCOMMENT TO IMPORT 0928 DATA (production: confirm tenant_id & ID ranges)",
            )
        )

    chunks.append(section_banner("E", "Optional user-fields patch (from 05)"))
    if enable_patch:
        chunks.append(read_part("E"))
    else:
        chunks.append(
            comment_block(
                read_part("E"),
                "UNCOMMENT IF PATCH holder/operator/dual-status after import",
            )
        )

    chunks.append(section_banner("F", "Verify (read-only, from verify-schema-V205-V206.sql)"))
    chunks.append(read_part("F"))

    return "".join(chunks)


def main() -> None:
    prod_path = DIR / "shenyu-ops-FULL-prod-20260928.sql"
    sd_path = DIR / "shenyu-ops-FULL-schema-and-data-20260928.sql"

    prod_text = build(
        enable_data=False,
        enable_preclean=False,
        enable_flyway=False,
        enable_patch=False,
    )
    sd_text = build(
        enable_data=True,
        enable_preclean=True,
        enable_flyway=False,
        enable_patch=False,
    )

    prod_path.write_text(prod_text, encoding="utf-8", newline="\n")
    sd_path.write_text(sd_text, encoding="utf-8", newline="\n")

    def line_count(p: Path) -> int:
        return len(p.read_text(encoding="utf-8").splitlines())

    print(f"Wrote {prod_path.name}: {line_count(prod_path)} lines")
    print(f"Wrote {sd_path.name}: {line_count(sd_path)} lines")


if __name__ == "__main__":
    main()
