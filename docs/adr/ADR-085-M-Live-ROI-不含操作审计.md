# ADR-085：M-Live ROI 本期不交付操作日志 / 库内审计

| 项 | 值 |
|---|---|
| 状态 | 已采纳（2026-11-06 用户定向范围裁剪） |
| 关联 | ADR-084 · `docs/delivery/handover/sql-release-20261105/` · PRD-M-Live |

## 背景

ROI 曾规划 `roi_operation_log` 表、MySQL 触发器、`GET /admin-api/ops/roi/audit-logs*` 与前端 `workbench=audit`。M-Live 投产窗口以报表、OCR、场次录入、作者/运营 ROI 等核心能力为主，库内操作审计与独立操作日志工作台不在本期交付范围。

## 决策

1. **后端**：移除 `RoiOperationLogController`、`RoiOperationLogService`；bootstrap 不再返回 `auditRead`；移除 `GET /records/{id}/audit` 与 `audit-logs` 全家桶。
2. **SQL SSOT / handover**：合并脚本不再包含原 Section 04（`20261026_roi_operation_logs.sql`）及 IP 审计投影 Section 08；其余 SSOT 经 `_build_sql_release_20261105.py` 的 `strip_roi_operation_audit` 去掉 `roi_operation_log` 相关触发器。
3. **运维默认包**：仍以 **`shenyu-ops-roi-minimal.sql`** + **`shenyu-system.sql`** 为主；全量 schema/routines 仅保留业务所需例程（如 Section 03 审批成本列扩展），不含操作审计。
4. **保留**：`RoiAuditSupport` 事务内 `@roi_audit_*` 会话变量（兼容历史库若仍有触发器）；`roi_audit` 轻量行级记录（非操作日志 UI 数据源）。

## 后续

若 Phase 2 恢复操作审计，须单独立项：恢复 SSOT、Flyway/ handover Section、API 与 UX，并更新 PRD/测试用例中的 `auditRead` / TC-MLive-GOV-*。
