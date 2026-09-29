#!/usr/bin/env python3
"""Generate prod-safe data SQL under docs/deploy/prod-sync-20260928/."""
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
SRC = REPO / "scripts/import/import-accounts-shenyu-ops-20260928.sql"
PATCH = REPO / "scripts/import/patch-accounts-user-fields-20260928.sql"
OUT = REPO / "docs/deploy/prod-sync-20260928"


def main() -> None:
    text = SRC.read_text(encoding="utf-8")
    insert_start = text.index("-- ========== 1. 公司")
    rb_start = text.index("-- ========== ROLLBACK (re-import only) ==========")
    preclean_start = text.index("-- --- 1. 抖音/快手账号子表")
    preclean_end = text.index("-- --- 3. Excel 导入批次支撑表")
    preclean_body = text[preclean_start:preclean_end].strip()

    header_preclean = """USE `shenyu-ops`;
-- =============================================================================
-- PRODUCTION · 可选预清理 · 仅 tenant=1 抖音/快手账号（含子表）
-- ⚠️ 不删除 oa_company / oa_realname / oa_phone / oa_sim_card 及 expansion/intermediary。
-- ⚠️ 生产「整批重导抖快」时：先本脚本，再 04-shenyu-ops-data-import-0928-OPTIONAL.sql。
-- ⚠️ 保留其它平台账号（公众号/视频号/小红书/企微等）及全部支撑表存量。
-- SSOT 生成器：scripts/import/prod-release/_generate_prod_sync_data_sql.py
-- 测试全量替换（含支撑表清理）仍用 scripts/import/import-accounts-shenyu-ops-20260928.sql PRE-IMPORT 全段。
-- =============================================================================

SET NAMES utf8mb4;
START TRANSACTION;

"""
    (OUT / "03-shenyu-ops-preclean-douyin-kuaishou-ONLY.sql").write_text(
        header_preclean + preclean_body + "\n\nCOMMIT;\n",
        encoding="utf-8",
    )

    header_prod = """USE `shenyu-ops`;
-- =============================================================================
-- PRODUCTION · 数据导入（可选）· xlsx-import-20260928
-- ⚠️ 本文件不含抖快 DELETE；重导抖快请先 03-shenyu-ops-preclean-douyin-kuaishou-ONLY.sql。
-- ⚠️ 数据段为「先按业务唯一键 UPDATE，无则 INSERT … SELECT … WHERE NOT EXISTS」（幂等重跑；保留生产 id）。
-- ⚠️ 支撑表（公司/实名人/设备/SIM）始终 UPSERT，禁止批量 DELETE（见 03 与 04a 范围说明）。
-- ⚠️ 执行前必须：01 架构脚本已跑完；确认 tenant_id=1 与 Excel 批次 ID 段一致。
-- ⚠️ 批次表三步：uk UPDATE → 批次 id 槽 UPDATE → INSERT（uk 与 id 均未占）；FK=COALESCE(uk, 批次 id 槽, 批次 id)。
-- SSOT 生成器：scripts/import/import-accounts-shenyu-ops-20260928.sql（含 PRE-IMPORT 全段，仅测试/全量替换）
-- =============================================================================

SET NAMES utf8mb4;
START TRANSACTION;

"""
    (OUT / "04-shenyu-ops-data-import-0928-OPTIONAL.sql").write_text(
        header_prod + text[insert_start:], encoding="utf-8"
    )

    rollback = """USE `shenyu-ops`;
-- =============================================================================
-- PRODUCTION · 仅清理 xlsx-import-20260928 批次（ROLLBACK 段）
-- 不删除 tenant 下全部 DOUYIN/KUAISHOU；适合「重导同一 Excel 批次」前手工清理。
-- 测试环境「全量替换」请用 scripts/import/import-accounts-shenyu-ops-20260928.sql 的 PRE-IMPORT 段。
-- =============================================================================

SET NAMES utf8mb4;
START TRANSACTION;

""" + text[rb_start:insert_start].strip() + "\n\nCOMMIT;\n"
    (OUT / "04a-shenyu-ops-data-import-0928-ROLLBACK-BATCH.sql").write_text(
        rollback, encoding="utf-8"
    )

    patch = PATCH.read_text(encoding="utf-8")
    patch_hdr = patch.replace(
        "USE shenyu-ops;",
        """USE `shenyu-ops`;
-- =============================================================================
-- PRODUCTION · 可选 PATCH · 对齐 holder/operator/双状态（tenant_id=1, id>=140001）
-- 适用：已导入账号但字段 NULL，或与 Excel 再对齐；依赖 V206 列。
-- SSOT：scripts/import/patch-accounts-user-fields-20260928.sql
-- =============================================================================
""",
        1,
    )
    (OUT / "05-shenyu-ops-patch-user-fields-OPTIONAL.sql").write_text(
        patch_hdr, encoding="utf-8"
    )
    print("OK:", OUT)


if __name__ == "__main__":
    main()
