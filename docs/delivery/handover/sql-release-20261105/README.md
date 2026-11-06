# M-Live 投产比 SQL Release · 2026-11-05

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
