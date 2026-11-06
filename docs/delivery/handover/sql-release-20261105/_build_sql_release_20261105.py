#!/usr/bin/env python3
"""Assemble M-Live / ROI SQL release from SSOT — one .sql file per database."""

from __future__ import annotations

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


def read_updates(name: str) -> str:
    return (UPDATES / name).read_text(encoding="utf-8").strip() + "\n"


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


OPS_HEADER = """-- =============================================================================
-- 用途：M-Live 投产比（ROI）· shenyu-ops 增量结构 + 可选钉钉字段映射
-- 目标库：shenyu-ops
-- 执行顺序：Section 01 → 14（必做）；Section 15 为 [OPTIONAL]（见 README）
-- 幂等性：是（CREATE IF NOT EXISTS、information_schema 条件 ALTER、INSERT IGNORE）
-- 回滚：生产需备份 shenyu-ops（至少 roi_* / ops_oa_* / oa_account_cost）；无自动回滚。
--       IP 审计函数回滚见 Section 08 注释（20261026_roi_ip_audit_projection_rollback.sql）。
-- 关联：ADR-084（ROI 运行时统一 ops-server :48094）；Flyway V205/V206 见 Section 12–13
-- 生成：python docs/delivery/handover/sql-release-20261105/_build_sql_release_20261105.py
-- =============================================================================

SET NAMES utf8mb4;
USE `shenyu-ops`;

"""

SYSTEM_HEADER = """-- =============================================================================
-- 用途：M-Live 投产比 · 原生只读边界（ROI 媒体委托读）+ ROI 分析维度字典
-- 目标库：shenyu-system
-- 执行顺序：在 shenyu-ops.sql 之后、ops-server 发版前（见 README.md）
-- 幂等性：是（CREATE IF NOT EXISTS；字典 NOT EXISTS / ON DUPLICATE KEY UPDATE）
-- 回滚：生产需备份；边界表可停用行；字典勿删历史业务引用
-- 关联：ADR-084；native_read_boundary_20261030.md；Flyway V42 dict_roi_dimension
-- 生成：python docs/delivery/handover/sql-release-20261105/_build_sql_release_20261105.py
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


def build_shenyu_ops() -> str:
    chunks = [OPS_HEADER]
    sections = [
        ("01", "ROI + OA 镜像基础表", "20261023_01_local_feature_schema.sql (shenyu-ops 段)", extract_shenyu_ops_from_local_feature()),
        ("02", "OA 费用行归因 ops_oa_cost_link", "20261025_oa_cost_links.sql", read_updates("20261025_oa_cost_links.sql")),
        ("03", "审批成本候选 + oa_account_cost 扩展列", "ops_roi_approval_cost_20261026.sql", read_updates("ops_roi_approval_cost_20261026.sql")),
        ("04", "ROI 操作审计 roi_operation_log + 触发器", "20261026_roi_operation_logs.sql", read_updates("20261026_roi_operation_logs.sql")),
        ("05", "组织目录 / IP 组范围", "20261026_roi_org_directory.sql", read_updates("20261026_roi_org_directory.sql")),
        ("06", "岗位→角色默认映射（tenant=1）", "20261026_roi_org_role_defaults.sql", read_updates("20261026_roi_org_role_defaults.sql")),
        ("07", "企微管理扩展表", "20261026_roi_wecom_management_restore.sql", read_updates("20261026_roi_wecom_management_restore.sql")),
        ("08", "IP 审计投影函数增量", "20261026_roi_ip_audit_projection.sql", read_updates("20261026_roi_ip_audit_projection.sql")),
        ("09", "作者/运营责任 + 费用来源/归属", "roi_author_cost_foundation_20261029.sql", read_updates("roi_author_cost_foundation_20261029.sql")),
        ("10", "费用业务分类 roi_cost_classification", "roi_cost_business_classification_20261030.sql", read_updates("roi_cost_business_classification_20261030.sql")),
        ("11", "媒体只读委托关系", "20261030_roi_media_read_delegation.sql", read_updates("20261030_roi_media_read_delegation.sql")),
        ("12", "Flyway V205 持有人 + M2 发布账号", "V205__m4_holder_m2_publish_account.sql", read_flyway("V205__m4_holder_m2_publish_account.sql")),
        ("13", "Flyway V206 抖快双状态 + 运营人", "V206__oa_account_short_video_live_operator.sql", read_flyway("V206__oa_account_short_video_live_operator.sql")),
        ("14", "oa_account 作者 + 封禁 JSON", "20261102_oa_account_author_ban.sql", read_updates("20261102_oa_account_author_ban.sql")),
    ]
    for num, title, ssot, body in sections:
        chunks.append(section_banner(num, title, ssot))
        chunks.append(body)
        if not body.endswith("\n"):
            chunks.append("\n")

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

**本 release 仅 2 个必执行 SQL**（`shenyu-ops.sql`、`shenyu-system.sql`）。`shenyu-pay` / `shenyu-member` / `shenyu-match` 无本 release DDL，无需执行。

## 文件

| 文件 | 库 | 说明 |
|---|---|---|
| `shenyu-ops.sql` | shenyu-ops | Section 01–14 必做；**Section 15 `[OPTIONAL]`** 钉钉 field_map（tenant=1）— **无钉钉对接可跳过** |
| `shenyu-system.sql` | shenyu-system | 原生只读边界表 + `dict_roi_dimension` |

## 执行顺序

1. **备份** `shenyu-ops`、`shenyu-system`（至少 `roi_*`、`ops_oa_*`、`oa_account`、`oa_account_cost`）。
2. 若跑 Section 08 IP 审计函数替换，**短暂停写** ROI（见 SSOT 注释）。
3. `mysql … shenyu-ops < shenyu-ops.sql`（无钉钉对接时：跳过或注释 **Section 15 `[OPTIONAL]`** 段）。
4. `mysql … shenyu-system < shenyu-system.sql`
5. 部署 **ops-server** 新 JAR；冒烟 `GET /admin-api/ops/roi/bootstrap`（Gateway 48080 → 48094）。

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

    ops_path = OUT / "shenyu-ops.sql"
    sys_path = OUT / "shenyu-system.sql"
    readme_path = OUT / "README.md"

    ops_path.write_text(build_shenyu_ops(), encoding="utf-8", newline="\n")
    sys_path.write_text(build_shenyu_system(), encoding="utf-8", newline="\n")
    readme_path.write_text(build_readme(), encoding="utf-8", newline="\n")

    def lc(p: Path) -> int:
        return len(p.read_text(encoding="utf-8").splitlines())

    print(f"Wrote {ops_path.name}: {lc(ops_path)} lines")
    print(f"Wrote {sys_path.name}: {lc(sys_path)} lines")
    print(f"Wrote {readme_path.name}")


if __name__ == "__main__":
    main()
