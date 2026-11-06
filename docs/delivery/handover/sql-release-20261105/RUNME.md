# 执行清单（投产比 SQL · 2026-11-05）

## M-Live 投产比最小发布（**默认 · 运维交接**）

**不含** `roi_operation_log` / 库内审计（ADR-085）；**不含** Section 15 钉钉 field_map。无需 `log_bin_trust_function_creators`。

**2026-11-06 更新**：`shenyu-ops-roi-minimal.sql` 已含 **01A**（`oa_ip_group*` 空表）、**01B**（`oa_account` / `oa_account_cost`）与 **14B**（`roi_attachment` / `roi_wecom_media` 的 `infra_file_id` / `infra_file_url` 与 `storage_name` 长度）。若已导入旧版 minimal，可另跑 `../sql-release-20261106/shenyu-ops-oa_account_cost-bootstrap.sql`（幂等，与新版重复段可跳过）。

| 顺序 | 文件 | 库 |
|------|------|-----|
| 1 | **`shenyu-ops-roi-minimal.sql`** | `shenyu-ops` |
| 2 | **`shenyu-system.sql`** | `shenyu-system`（原生只读边界 + ROI 维度字典；与 M-Live 媒体委托/字典相关时必跑） |

部署 **ops-server** 后冒烟 `GET /admin-api/ops/roi/bootstrap`。

> 操作日志 Tab / `GET …/audit-logs` **不在 M-Live 范围**（ADR-085；后端已移除）。

**可选（历史库清理）**：若曾安装 Section 04 操作审计（`20261026_roi_operation_logs.sql` / IP 投影 / 组织·企微·作者成本审计触发器），在导入 **`shenyu-ops-roi-minimal.sql`** 之前或之后执行 **`shenyu-ops-drop-audit-routines.sql`**（幂等 DROP；会删除 `roi_operation_log` 数据，请先备份）。`log_bin=ON` 且无 SUPER 时可能 **1419**，须 DBA 先开 `log_bin_trust_function_creators`。

## 平台全量（含 Section 03 等例程 · 给 DBA / 平台组）

| 顺序 | 文件 | 库 |
|------|------|-----|
| 1 | `shenyu-ops-schema.sql` | `shenyu-ops` |
| 2 | 请 DBA 按 **`DBA-1419-一次性设置.md`** 开 `log_bin_trust_function_creators=1` 并校验 |
| 3 | `shenyu-ops-routines.sql` | `shenyu-ops` |
| 4 | `shenyu-system.sql` | `shenyu-system` |

**`shenyu-system.sql`** 单文件即可，**不会**触发 1419。

Section 15 `[OPTIONAL]`（钉钉 field_map）无对接可跳过 — 见 `README.md`。

## 仅当 DBA 已开 trust

一次导入 **`shenyu-ops-full.sql`** 或 **`shenyu-ops.sql`**（与 full 相同），再导入 `shenyu-system.sql`。

## 本机/测试站一键验证

```powershell
# 默认：schema + system（无 1419）
.\scripts\run-handover-sql-test.ps1

# trust 已开或测试 root 可 SET GLOBAL 时，追加 routines / 等同 full
$env:MYSQL_TRUST_CREATORS = "1"
.\scripts\run-handover-sql-test.ps1
```

读 `scripts/integration-config/ops-test-remote.env` 或 `HANDOVER_MYSQL_*` 环境变量。

## 手工 mysql 示例（最小发布）

```bash
mysql --host=HOST --port=3306 --user=OPS_USER --database=shenyu-ops < shenyu-ops-roi-minimal.sql
mysql --host=HOST --port=3306 --user=SYSTEM_USER --database=shenyu-system < shenyu-system.sql
```

## 手工 mysql 示例（平台全量）

```bash
mysql --host=HOST --port=3306 --user=OPS_USER --database=shenyu-ops < shenyu-ops-schema.sql
# DBA 步骤 …
mysql --host=HOST --port=3306 --user=OPS_USER --database=shenyu-ops < shenyu-ops-routines.sql
mysql --host=HOST --port=3306 --user=SYSTEM_USER --database=shenyu-system < shenyu-system.sql
```
